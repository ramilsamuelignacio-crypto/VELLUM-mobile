package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.model.Layer
import com.example.model.SketchPoint
import com.example.model.Stroke
import com.example.model.SymmetryMode
import com.example.model.ToolType
import org.json.JSONArray
import org.json.JSONObject

class VellumDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS layers (
                room_id TEXT NOT NULL,
                id TEXT NOT NULL,
                name TEXT NOT NULL,
                visible INTEGER NOT NULL,
                sort_order INTEGER NOT NULL,
                PRIMARY KEY (room_id, id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS strokes (
                room_id TEXT NOT NULL,
                id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                layer_id TEXT NOT NULL,
                tool TEXT NOT NULL,
                color_argb INTEGER NOT NULL,
                size REAL NOT NULL,
                opacity REAL NOT NULL,
                points_json TEXT NOT NULL,
                rotation REAL NOT NULL,
                symmetry TEXT NOT NULL,
                symmetry_center_json TEXT,
                brush TEXT NOT NULL,
                pressure_sensitive INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                PRIMARY KEY (room_id, id)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS strokes")
        db.execSQL("DROP TABLE IF EXISTS layers")
        onCreate(db)
    }

    fun loadLayers(roomId: String): List<Layer> {
        val db = readableDatabase
        val result = mutableListOf<Layer>()
        db.rawQuery(
            "SELECT id, name, visible FROM layers WHERE room_id = ? ORDER BY sort_order ASC",
            arrayOf(roomId)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    Layer(
                        id = cursor.getString(0),
                        name = cursor.getString(1),
                        visible = cursor.getInt(2) == 1
                    )
                )
            }
        }
        return if (result.isEmpty()) {
            val defaultLayer = Layer(id = "layer-1", name = "Layer 1", visible = true)
            saveLayers(roomId, listOf(defaultLayer))
            listOf(defaultLayer)
        } else {
            result
        }
    }

    fun saveLayers(roomId: String, layers: List<Layer>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("layers", "room_id = ?", arrayOf(roomId))
            layers.forEachIndexed { index, layer ->
                val cv = ContentValues().apply {
                    put("room_id", roomId)
                    put("id", layer.id)
                    put("name", layer.name)
                    put("visible", if (layer.visible) 1 else 0)
                    put("sort_order", index)
                }
                db.insertWithOnConflict("layers", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadStrokes(roomId: String): List<Stroke> {
        val db = readableDatabase
        val result = mutableListOf<Stroke>()
        db.rawQuery(
            """
            SELECT id, user_id, layer_id, tool, color_argb, size, opacity, points_json,
                   rotation, symmetry, symmetry_center_json, brush, pressure_sensitive, timestamp
            FROM strokes WHERE room_id = ? ORDER BY timestamp ASC
            """.trimIndent(),
            arrayOf(roomId)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val points = decodePoints(cursor.getString(7))
                val symCenterJson = cursor.getString(10)
                val symCenter = if (!symCenterJson.isNullOrBlank()) decodePoint(symCenterJson) else null
                val tool = runCatching { ToolType.valueOf(cursor.getString(3)) }.getOrDefault(ToolType.PEN)
                val sym = runCatching { SymmetryMode.valueOf(cursor.getString(9)) }.getOrDefault(SymmetryMode.NONE)
                result.add(
                    Stroke(
                        id = cursor.getString(0),
                        userId = cursor.getString(1),
                        layerId = cursor.getString(2),
                        tool = tool,
                        colorArgb = cursor.getInt(4),
                        size = cursor.getFloat(5),
                        opacity = cursor.getFloat(6),
                        points = points,
                        rotation = cursor.getFloat(8),
                        symmetry = sym,
                        symmetryCenter = symCenter,
                        brush = cursor.getString(11),
                        pressureSensitive = cursor.getInt(12) == 1,
                        timestamp = cursor.getLong(13)
                    )
                )
            }
        }
        return result
    }

    fun upsertStroke(roomId: String, stroke: Stroke) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("room_id", roomId)
            put("id", stroke.id)
            put("user_id", stroke.userId)
            put("layer_id", stroke.layerId)
            put("tool", stroke.tool.name)
            put("color_argb", stroke.colorArgb)
            put("size", stroke.size)
            put("opacity", stroke.opacity)
            put("points_json", encodePoints(stroke.points))
            put("rotation", stroke.rotation)
            put("symmetry", stroke.symmetry.name)
            put("symmetry_center_json", stroke.symmetryCenter?.let { encodePoint(it).toString() })
            put("brush", stroke.brush)
            put("pressure_sensitive", if (stroke.pressureSensitive) 1 else 0)
            put("timestamp", stroke.timestamp)
        }
        db.insertWithOnConflict("strokes", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteStroke(roomId: String, strokeId: String) {
        writableDatabase.delete("strokes", "room_id = ? AND id = ?", arrayOf(roomId, strokeId))
    }

    fun clearRoomStrokes(roomId: String) {
        writableDatabase.delete("strokes", "room_id = ?", arrayOf(roomId))
    }

    private fun encodePoints(points: List<SketchPoint>): String {
        val arr = JSONArray()
        for (p in points) {
            arr.put(encodePoint(p))
        }
        return arr.toString()
    }

    private fun encodePoint(p: SketchPoint): JSONObject = JSONObject().apply {
        put("x", p.x.toDouble())
        put("y", p.y.toDouble())
        p.pressure?.let { put("pressure", it.toDouble()) }
        p.tiltX?.let { put("tiltX", it.toDouble()) }
        p.tiltY?.let { put("tiltY", it.toDouble()) }
        p.twist?.let { put("twist", it.toDouble()) }
    }

    private fun decodePoints(json: String): List<SketchPoint> {
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                parsePointObj(obj)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun decodePoint(json: String): SketchPoint? {
        return try {
            parsePointObj(JSONObject(json))
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePointObj(obj: JSONObject): SketchPoint = SketchPoint(
        x = obj.optDouble("x", 0.0).toFloat(),
        y = obj.optDouble("y", 0.0).toFloat(),
        pressure = if (obj.has("pressure")) obj.optDouble("pressure").toFloat() else null,
        tiltX = if (obj.has("tiltX")) obj.optDouble("tiltX").toFloat() else null,
        tiltY = if (obj.has("tiltY")) obj.optDouble("tiltY").toFloat() else null,
        twist = if (obj.has("twist")) obj.optDouble("twist").toFloat() else null
    )

    companion object {
        private const val DATABASE_NAME = "vellum_studio.db"
        private const val DATABASE_VERSION = 1
    }
}
