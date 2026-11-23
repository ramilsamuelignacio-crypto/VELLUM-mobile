package com.example.math

import com.example.model.Layer
import com.example.model.SketchPoint
import com.example.model.Stroke
import com.example.model.SymmetryMode
import com.example.model.ToolType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.sin

data class ShapeBounds(
    val cx: Float,
    val cy: Float,
    val width: Float,
    val height: Float
)

data class SnapGuides(
    val x: Float? = null,
    val y: Float? = null
)

data class SnapResult(
    val point: SketchPoint,
    val guides: SnapGuides
)

object StrokeMath {
    fun clamp(value: Float, min: Float, max: Float): Float =
        value.coerceIn(min, max)

    fun reorderLayers(layers: List<Layer>, fromIndex: Int, toIndex: Int): List<Layer> {
        if (fromIndex < 0 || fromIndex >= layers.size ||
            toIndex < 0 || toIndex >= layers.size ||
            fromIndex == toIndex
        ) {
            return layers
        }
        val next = layers.toMutableList()
        val moved = next.removeAt(fromIndex)
        next.add(toIndex, moved)
        return next
    }

    fun toggleLayerVisibility(layers: List<Layer>, layerId: String): List<Layer> =
        layers.map { if (it.id == layerId) it.copy(visible = !it.visible) else it }

    fun getVisibleStrokesInLayerOrder(strokes: List<Stroke>, layers: List<Layer>): List<Stroke> {
        if (layers.isEmpty()) return strokes
        val visibleIds = layers.filter { it.visible }.map { it.id }.toSet()
        val orderMap = layers.withIndex().associate { it.value.id to it.index }
        return strokes
            .filter { it.layerId.isEmpty() || it.layerId in visibleIds }
            .sortedWith(
                compareBy<Stroke>(
                    { orderMap[it.layerId] ?: 0 },
                    { it.timestamp }
                )
            )
    }

    fun addStrokeIdempotent(strokes: List<Stroke>, incoming: Stroke): List<Stroke> {
        val exists = strokes.any { it.id == incoming.id }
        return if (exists) {
            strokes.map { if (it.id == incoming.id) incoming else it }
        } else {
            strokes + incoming
        }
    }

    fun appendPointToStroke(
        strokes: List<Stroke>,
        strokeId: String,
        point: SketchPoint
    ): List<Stroke> {
        return strokes.map { stroke ->
            if (stroke.id != strokeId) {
                stroke
            } else {
                val last = stroke.points.lastOrNull()
                if (last != null && last.x == point.x && last.y == point.y) {
                    stroke
                } else {
                    stroke.copy(points = stroke.points + point)
                }
            }
        }
    }

    fun appendPointsToStroke(
        strokes: List<Stroke>,
        strokeId: String,
        points: List<SketchPoint>
    ): List<Stroke> {
        if (points.isEmpty()) return strokes
        return strokes.map { stroke ->
            if (stroke.id != strokeId) {
                stroke
            } else {
                val merged = stroke.points.toMutableList()
                for (pt in points) {
                    val last = merged.lastOrNull()
                    if (last == null || last.x != pt.x || last.y != pt.y) {
                        merged.add(pt)
                    }
                }
                if (merged.size == stroke.points.size) stroke else stroke.copy(points = merged)
            }
        }
    }

    fun removeStrokeById(strokes: List<Stroke>, strokeId: String): List<Stroke> =
        strokes.filter { it.id != strokeId }

    fun getLastUserStrokeId(strokes: List<Stroke>, userId: String): String? {
        for (i in strokes.indices.reversed()) {
            if (strokes[i].userId == userId) {
                return strokes[i].id
            }
        }
        return null
    }

    fun generateRoomCode(): String {
        val chars = "abcdefghjkmnpqrstuvwxyz23456789"
        return buildString(6) {
            repeat(6) {
                append(chars.random())
            }
        }
    }

    fun screenToWorld(screen: SketchPoint, pan: SketchPoint, zoom: Float): SketchPoint {
        val safeZoom = if (zoom > 0f) zoom else 1f
        return SketchPoint(
            x = round(((screen.x - pan.x) / safeZoom) * 10f) / 10f,
            y = round(((screen.y - pan.y) / safeZoom) * 10f) / 10f
        )
    }

    fun worldToScreen(world: SketchPoint, pan: SketchPoint, zoom: Float): SketchPoint {
        return SketchPoint(
            x = world.x * zoom + pan.x,
            y = world.y * zoom + pan.y
        )
    }

    fun getShapeBounds(stroke: Stroke): ShapeBounds? {
        if ((stroke.tool != ToolType.RECT && stroke.tool != ToolType.CIRCLE) || stroke.points.size < 2) {
            return null
        }
        val start = stroke.points.first()
        val end = stroke.points.last()
        return ShapeBounds(
            cx = (start.x + end.x) / 2f,
            cy = (start.y + end.y) / 2f,
            width = abs(end.x - start.x),
            height = abs(end.y - start.y)
        )
    }

    fun hitTestShape(strokes: List<Stroke>, point: SketchPoint): Stroke? {
        for (i in strokes.indices.reversed()) {
            val stroke = strokes[i]
            val bounds = getShapeBounds(stroke) ?: continue
            val angle = -stroke.rotation.toDouble()
            val dx = (point.x - bounds.cx).toDouble()
            val dy = (point.y - bounds.cy).toDouble()
            val localX = dx * cos(angle) - dy * sin(angle)
            val localY = dx * sin(angle) + dy * cos(angle)
            val pad = maxOf(14f, stroke.size / 2f)
            if (abs(localX) <= bounds.width / 2f + pad &&
                abs(localY) <= bounds.height / 2f + pad
            ) {
                return stroke
            }
        }
        return null
    }

    fun calculateRotationAngle(cx: Float, cy: Float, pointer: SketchPoint): Float {
        return (atan2((pointer.y - cy).toDouble(), (pointer.x - cx).toDouble()) + PI / 2.0).toFloat()
    }

    fun stabilizePoint(previous: SketchPoint, raw: SketchPoint, factor: Float): SketchPoint {
        val f = clamp(factor, 0f, 0.9f)
        val alpha = 1f - f
        return raw.copy(
            x = round((previous.x + (raw.x - previous.x) * alpha) * 10f) / 10f,
            y = round((previous.y + (raw.y - previous.y) * alpha) * 10f) / 10f
        )
    }

    fun getSymmetryTransforms(
        point: SketchPoint,
        mode: SymmetryMode,
        center: SketchPoint,
        radialSegments: Int = 6
    ): List<SketchPoint> {
        return when (mode) {
            SymmetryMode.NONE -> listOf(point)
            SymmetryMode.HORIZONTAL -> listOf(
                point,
                point.copy(x = 2f * center.x - point.x, y = point.y)
            )
            SymmetryMode.VERTICAL -> listOf(
                point,
                point.copy(x = point.x, y = 2f * center.y - point.y)
            )
            SymmetryMode.RADIAL, SymmetryMode.KALEIDOSCOPE -> {
                val dx = (point.x - center.x).toDouble()
                val dy = (point.y - center.y).toDouble()
                val results = mutableListOf<SketchPoint>()
                val step = (2.0 * PI) / radialSegments
                for (i in 0 until radialSegments) {
                    val angle = i * step
                    val c = cos(angle)
                    val s = sin(angle)
                    results.add(
                        point.copy(
                            x = (center.x + dx * c - dy * s).toFloat(),
                            y = (center.y + dx * s + dy * c).toFloat()
                        )
                    )
                    if (mode == SymmetryMode.KALEIDOSCOPE) {
                        results.add(
                            point.copy(
                                x = (center.x + dx * c + dy * s).toFloat(),
                                y = (center.y + dx * s - dy * c).toFloat()
                            )
                        )
                    }
                }
                results
            }
        }
    }

    fun snapPointToGridOrObjects(
        point: SketchPoint,
        strokes: List<Stroke>,
        gridSize: Float = 24f,
        threshold: Float = 8f,
        excludeStrokeId: String? = null
    ): SnapResult {
        var snappedX = point.x
        var snappedY = point.y
        var guideX: Float? = null
        var guideY: Float? = null
        var bestDx = threshold + 1f
        var bestDy = threshold + 1f

        for (s in strokes) {
            if (s.id == excludeStrokeId) continue
            val bounds = getShapeBounds(s) ?: continue
            val xTargets = listOf(
                bounds.cx - bounds.width / 2f,
                bounds.cx,
                bounds.cx + bounds.width / 2f
            )
            for (tx in xTargets) {
                val dx = abs(point.x - tx)
                if (dx < bestDx) {
                    bestDx = dx
                    snappedX = round(tx * 10f) / 10f
                    guideX = snappedX
                }
            }
            val yTargets = listOf(
                bounds.cy - bounds.height / 2f,
                bounds.cy,
                bounds.cy + bounds.height / 2f
            )
            for (ty in yTargets) {
                val dy = abs(point.y - ty)
                if (dy < bestDy) {
                    bestDy = dy
                    snappedY = round(ty * 10f) / 10f
                    guideY = snappedY
                }
            }
        }

        if (guideX == null) {
            val nearestGridX = round(point.x / gridSize) * gridSize
            if (abs(point.x - nearestGridX) <= threshold) {
                snappedX = nearestGridX
            }
        }
        if (guideY == null) {
            val nearestGridY = round(point.y / gridSize) * gridSize
            if (abs(point.y - nearestGridY) <= threshold) {
                snappedY = nearestGridY
            }
        }

        return SnapResult(
            point = point.copy(x = snappedX, y = snappedY),
            guides = SnapGuides(x = guideX, y = guideY)
        )
    }

    fun distance(a: SketchPoint, b: SketchPoint): Float =
        hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
}
