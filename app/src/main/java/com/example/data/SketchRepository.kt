package com.example.data

import android.content.Context
import com.example.math.StrokeMath
import com.example.model.Collaborator
import com.example.model.Layer
import com.example.model.SketchPoint
import com.example.model.Stroke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SketchRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dbHelper = VellumDatabaseHelper(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val localUser: Collaborator = loadOrCreateLocalUser(appContext)

    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes.asStateFlow()

    private val _layers = MutableStateFlow(listOf(Layer(id = "layer-1", name = "Layer 1", visible = true)))
    val layers: StateFlow<List<Layer>> = _layers.asStateFlow()

    private val _collaborators = MutableStateFlow(listOf(localUser))
    val collaborators: StateFlow<List<Collaborator>> = _collaborators.asStateFlow()

    private val _connected = MutableStateFlow(true)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private var currentRoomId: String = "studio-1"

    fun joinRoom(roomId: String) {
        currentRoomId = roomId
        _collaborators.value = listOf(localUser)
        _connected.value = true
        scope.launch {
            val loadedLayers = dbHelper.loadLayers(roomId)
            val loadedStrokes = dbHelper.loadStrokes(roomId)
            _layers.value = loadedLayers
            _strokes.value = loadedStrokes
        }
    }

    fun commitStroke(stroke: Stroke) {
        val roomId = currentRoomId
        _strokes.update { StrokeMath.addStrokeIdempotent(it, stroke) }
        scope.launch {
            dbHelper.upsertStroke(roomId, stroke)
        }
    }

    fun updateStrokeInMemory(stroke: Stroke) {
        _strokes.update { StrokeMath.addStrokeIdempotent(it, stroke) }
    }

    fun startStroke(stroke: Stroke) {
        val roomId = currentRoomId
        _strokes.update { StrokeMath.addStrokeIdempotent(it, stroke) }
        scope.launch {
            dbHelper.upsertStroke(roomId, stroke)
        }
    }

    fun appendPoints(strokeId: String, points: List<SketchPoint>) {
        if (points.isEmpty()) return
        _strokes.update { StrokeMath.appendPointsToStroke(it, strokeId, points) }
    }

    fun endStroke(stroke: Stroke) {
        commitStroke(stroke)
    }

    fun undoStroke(strokeId: String) {
        val roomId = currentRoomId
        _strokes.update { StrokeMath.removeStrokeById(it, strokeId) }
        scope.launch {
            dbHelper.deleteStroke(roomId, strokeId)
        }
    }

    fun clearCanvas() {
        val roomId = currentRoomId
        _strokes.value = emptyList()
        scope.launch {
            dbHelper.clearRoomStrokes(roomId)
        }
    }

    fun createLayer(layer: Layer) {
        val roomId = currentRoomId
        val next = if (_layers.value.any { it.id == layer.id }) {
            _layers.value
        } else {
            _layers.value + layer
        }
        _layers.value = next
        scope.launch {
            dbHelper.saveLayers(roomId, next)
        }
    }

    fun toggleLayer(layerId: String) {
        val roomId = currentRoomId
        val next = StrokeMath.toggleLayerVisibility(_layers.value, layerId)
        _layers.value = next
        scope.launch {
            dbHelper.saveLayers(roomId, next)
        }
    }

    fun moveLayer(fromIndex: Int, toIndex: Int) {
        val roomId = currentRoomId
        val next = StrokeMath.reorderLayers(_layers.value, fromIndex, toIndex)
        if (next == _layers.value) return
        _layers.value = next
        scope.launch {
            dbHelper.saveLayers(roomId, next)
        }
    }

    companion object {
        private const val PREFS_NAME = "vellum_user_prefs"
        private const val KEY_USER_ID = "local_user_id"
        private const val KEY_USER_NAME = "local_user_name"
        private const val KEY_USER_COLOR = "local_user_color"

        private val USER_COLORS = listOf(
            0xFF2563EB.toInt(),
            0xFFD97706.toInt(),
            0xFF059669.toInt(),
            0xFFDC2626.toInt(),
            0xFF7C3AED.toInt(),
            0xFFDB2777.toInt()
        )
        private val ADJECTIVES = listOf("Swift", "Calm", "Bright", "Bold", "Quiet", "Sharp")
        private val NOUNS = listOf("Finch", "Fox", "Crane", "Lark", "Wren", "Owl")

        private fun loadOrCreateLocalUser(context: Context): Collaborator {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existingId = prefs.getString(KEY_USER_ID, null)
            val existingName = prefs.getString(KEY_USER_NAME, null)
            if (!existingId.isNullOrBlank() && !existingName.isNullOrBlank()) {
                val color = prefs.getInt(KEY_USER_COLOR, USER_COLORS.first())
                return Collaborator(id = existingId, name = existingName, colorArgb = color, cursor = null)
            }
            val suffix = (100000..999999).random().toString(36)
            val id = "u_$suffix"
            val name = "${ADJECTIVES.random()} ${NOUNS.random()}"
            val color = USER_COLORS.random()
            prefs.edit()
                .putString(KEY_USER_ID, id)
                .putString(KEY_USER_NAME, name)
                .putInt(KEY_USER_COLOR, color)
                .apply()
            return Collaborator(id = id, name = name, colorArgb = color, cursor = null)
        }
    }
}
