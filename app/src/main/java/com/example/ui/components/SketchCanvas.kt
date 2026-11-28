package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint as AndroidPaint
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as ComposeStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BrushEngine
import com.example.math.SnapGuides
import com.example.math.StrokeMath
import com.example.math.TouchMath
import com.example.model.GuideMode
import com.example.model.Layer
import com.example.model.SketchPoint
import com.example.model.Stroke
import com.example.model.SymmetryMode
import com.example.model.ToolType
import com.example.theme.VellumColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin

private class LiveStrokeHolder {
    var activeStroke: Stroke? = null
    val activePoints = ArrayList<SketchPoint>(256)
    var pendingCommitStroke: Stroke? = null
    var activeSnapGuides: SnapGuides = SnapGuides()
    var totalMoveDist: Float = 0f
    var isGestureNavigating: Boolean = false
    var gesturePan: SketchPoint = SketchPoint(0f, 0f)
    var gestureZoom: Float = 1f
}

private class CommittedCanvasCache {
    var bitmap: Bitmap? = null
    var canvas: android.graphics.Canvas? = null
    var lastWidth: Int = 0
    var lastHeight: Int = 0
    var lastPan: SketchPoint? = null
    var lastZoom: Float = -1f
    var lastSymmetryCenter: SketchPoint? = null
    var lastStrokes: List<Stroke>? = null
    private val previewMatrix = Matrix()
    private val previewPaint = AndroidPaint(AndroidPaint.FILTER_BITMAP_FLAG)

    fun drawTransformedPreview(
        targetCanvas: android.graphics.Canvas,
        currentPan: SketchPoint,
        currentZoom: Float
    ): Boolean {
        val bmp = bitmap ?: return false
        val bakedZoom = lastZoom
        val bakedPan = lastPan ?: return false
        if (bakedZoom <= 0f || currentZoom <= 0f) return false
        val relScale = currentZoom / bakedZoom
        val relTx = currentPan.x - bakedPan.x * relScale
        val relTy = currentPan.y - bakedPan.y * relScale
        previewMatrix.reset()
        previewMatrix.postScale(relScale, relScale)
        previewMatrix.postTranslate(relTx, relTy)
        targetCanvas.drawBitmap(bmp, previewMatrix, previewPaint)
        return true
    }

    fun ensureAndBake(
        width: Int,
        height: Int,
        visibleStrokes: List<Stroke>,
        pan: SketchPoint,
        zoom: Float,
        symmetryCenter: SketchPoint
    ): Bitmap? {
        if (width <= 0 || height <= 0) return null

        val sizeChanged = bitmap == null || lastWidth != width || lastHeight != height
        if (sizeChanged) {
            bitmap?.recycle()
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            canvas = android.graphics.Canvas(bitmap!!)
            lastWidth = width
            lastHeight = height
            lastStrokes = null
        }

        val viewUnchanged = !sizeChanged &&
            lastPan == pan &&
            lastZoom == zoom &&
            lastSymmetryCenter == symmetryCenter

        val prev = lastStrokes
        if (viewUnchanged && prev === visibleStrokes) {
            return bitmap
        }

        val targetCanvas = canvas ?: return bitmap

        val canIncrementalAppend = viewUnchanged &&
            prev != null &&
            visibleStrokes.size == prev.size + 1 &&
            visibleStrokes.last().tool != ToolType.ERASER &&
            (0 until prev.size).all { visibleStrokes[it] === prev[it] }

        if (canIncrementalAppend) {
            targetCanvas.save()
            targetCanvas.translate(pan.x, pan.y)
            targetCanvas.scale(zoom, zoom)
            BrushEngine.renderBrushStrokeToCanvas(targetCanvas, visibleStrokes.last(), symmetryCenter)
            targetCanvas.restore()
        } else {
            bitmap!!.eraseColor(AndroidColor.TRANSPARENT)
            if (visibleStrokes.isNotEmpty()) {
                targetCanvas.save()
                targetCanvas.translate(pan.x, pan.y)
                targetCanvas.scale(zoom, zoom)
                for (i in visibleStrokes.indices) {
                    BrushEngine.renderBrushStrokeToCanvas(targetCanvas, visibleStrokes[i], symmetryCenter)
                }
                targetCanvas.restore()
            }
        }

        lastPan = pan
        lastZoom = zoom
        lastSymmetryCenter = symmetryCenter
        lastStrokes = visibleStrokes
        return bitmap
    }

    fun recycle() {
        bitmap?.recycle()
        bitmap = null
        canvas = null
        lastStrokes = null
    }
}

private class DotGridBuffer {
    var points = FloatArray(4096)
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        style = AndroidPaint.Style.STROKE
        strokeCap = AndroidPaint.Cap.ROUND
    }
}

private class PaperGrainShaderCache {
    private var tileBitmap: Bitmap? = null
    private var paint: AndroidPaint? = null
    private var lastTileSize: Int = 0
    private var lastColor: Int = 0

    fun getPaint(stepPx: Float, colorArgb: Int): AndroidPaint {
        val tileSize = (stepPx * 4f).roundToInt().coerceIn(32, 256)
        val existing = paint
        if (existing != null && lastTileSize == tileSize && lastColor == colorArgb) {
            return existing
        }
        tileBitmap?.recycle()
        val bmp = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)
        val tileCanvas = android.graphics.Canvas(bmp)
        val dotPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            style = AndroidPaint.Style.FILL
            color = colorArgb
            alpha = 8
        }
        val step = tileSize / 4f
        var row = 0
        var y = step * 0.5f
        while (y < tileSize) {
            var x = if (row % 2 == 0) step * 0.25f else step * 0.75f
            while (x < tileSize) {
                tileCanvas.drawCircle(x, y, 0.75f, dotPaint)
                x += step
            }
            y += step
            row++
        }
        val shader = BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        val newPaint = AndroidPaint().apply {
            this.shader = shader
        }
        tileBitmap = bmp
        paint = newPaint
        lastTileSize = tileSize
        lastColor = colorArgb
        return newPaint
    }

    fun recycle() {
        tileBitmap?.recycle()
        tileBitmap = null
        paint = null
    }
}

@Composable
fun SketchCanvas(
    strokes: List<Stroke>,
    layers: List<Layer>,
    activeLayerId: String,
    localUserId: String,
    activeTool: ToolType,
    activeBrush: String,
    colorArgb: Int,
    strokeSize: Float,
    brushOpacity: Float = 1f,
    stabilization: Float,
    pressureEnabled: Boolean,
    showGrid: Boolean,
    showPaperGrain: Boolean,
    zoom: Float,
    pan: SketchPoint,
    symmetryMode: SymmetryMode,
    symmetryCenter: SketchPoint,
    guideMode: GuideMode,
    touchPanMode: Boolean,
    selectedStrokeId: String?,
    isExpanded: Boolean,
    onCanvasResize: (width: Float, height: Float) -> Unit,
    onPanZoom: (pan: SketchPoint, zoom: Float) -> Unit,
    onZoomStep: (Float) -> Unit,
    onResetView: () -> Unit,
    onSelectStroke: (String?) -> Unit,
    onCommitStroke: (Stroke) -> Unit,
    onUpdateStrokePreview: (Stroke) -> Unit,
    onChangeSymmetryMode: (SymmetryMode) -> Unit,
    onRecenterSymmetry: () -> Unit,
    onChangeGuideMode: (GuideMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStrokes by rememberUpdatedState(strokes)
    val currentLayers by rememberUpdatedState(layers)
    val currentActiveLayerId by rememberUpdatedState(activeLayerId)
    val currentLocalUserId by rememberUpdatedState(localUserId)
    val currentTool by rememberUpdatedState(activeTool)
    val currentBrush by rememberUpdatedState(activeBrush)
    val currentColor by rememberUpdatedState(colorArgb)
    val currentStrokeSize by rememberUpdatedState(strokeSize)
    val currentBrushOpacity by rememberUpdatedState(brushOpacity)
    val currentStabilization by rememberUpdatedState(stabilization)
    val currentPressureEnabled by rememberUpdatedState(pressureEnabled)
    val currentZoom by rememberUpdatedState(zoom)
    val currentPan by rememberUpdatedState(pan)
    val currentSymmetryMode by rememberUpdatedState(symmetryMode)
    val currentSymmetryCenter by rememberUpdatedState(symmetryCenter)
    val currentTouchPanMode by rememberUpdatedState(touchPanMode)

    val liveHolder = remember { LiveStrokeHolder() }
    var activeDrawTick by remember { mutableIntStateOf(0) }
    val committedCache = remember { CommittedCanvasCache() }
    val dotGridBuffer = remember { DotGridBuffer() }
    val paperGrainCache = remember { PaperGrainShaderCache() }

    DisposableEffect(Unit) {
        onDispose {
            committedCache.recycle()
            paperGrainCache.recycle()
        }
    }

    // Pre-warm active brush stamp bitmaps before the user touches the canvas
    LaunchedEffect(colorArgb, activeBrush) {
        BrushEngine.getCachedPencilGrainBitmap(colorArgb, 24)
        BrushEngine.getCachedAirbrushStampBitmap(colorArgb, 64)
        BrushEngine.getCachedCharcoalGrainBitmap(colorArgb, 32)
        BrushEngine.getCachedMarkerNibBitmap(colorArgb, 32, 12)
        BrushEngine.getCachedWatercolorStampBitmap(colorArgb, 64)
    }

    val visibleStrokes = remember(strokes, layers) {
        StrokeMath.getVisibleStrokesInLayerOrder(strokes, layers)
    }

    // Clear pendingCommitStroke only after visibleStrokes reflects it (prevents 1-frame blink)
    LaunchedEffect(visibleStrokes) {
        val pending = liveHolder.pendingCommitStroke
        if (pending != null && visibleStrokes.any { it.id == pending.id }) {
            liveHolder.pendingCommitStroke = null
            activeDrawTick++
        }
    }

    val selectedStroke = remember(activeTool, selectedStrokeId, visibleStrokes) {
        if (activeTool == ToolType.SELECT && selectedStrokeId != null) {
            visibleStrokes.find { it.id == selectedStrokeId }
        } else {
            null
        }
    }
    val selectedBounds = remember(selectedStroke) {
        selectedStroke?.let { StrokeMath.getShapeBounds(it) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VellumColors.PaperWhite)
            .clipToBounds()
            .onSizeChanged { size ->
                onCanvasResize(size.width.toFloat(), size.height.toFloat())
            }
            .testTag("sketch_canvas_container")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("sketch_canvas")
                .pointerInput(Unit) {
                    val activePointers = LinkedHashMap<PointerId, SketchPoint>()
                    var isPanning = false
                    var hasPinchedInGesture = false
                    var panStartOrigin = SketchPoint(0f, 0f)
                    var panStartPointer = SketchPoint(0f, 0f)

                    fun extractPressure(rawPressure: Float, type: PointerType): Float {
                        if (!currentPressureEnabled) return 0.5f
                        if (rawPressure.isNaN()) return 0.5f
                        if (type == PointerType.Touch && rawPressure == 0f) return 0.5f
                        if (type == PointerType.Mouse && abs(rawPressure - 0.5f) < 1e-4f) return 0.5f
                        return StrokeMath.clamp(rawPressure, 0.05f, 1f)
                    }

                    fun toWorldPoint(pos: Offset, pressure: Float, type: PointerType): SketchPoint {
                        val activePan = if (liveHolder.isGestureNavigating) liveHolder.gesturePan else currentPan
                        val activeZoom = if (liveHolder.isGestureNavigating) liveHolder.gestureZoom else currentZoom
                        val safeZoom = if (activeZoom > 0f) activeZoom else 1f
                        return SketchPoint(
                            x = round(((pos.x - activePan.x) / safeZoom) * 100f) / 100f,
                            y = round(((pos.y - activePan.y) / safeZoom) * 100f) / 100f,
                            pressure = extractPressure(pressure, type)
                        )
                    }

                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        activePointers.clear()
                        liveHolder.activeStroke = null
                        liveHolder.activePoints.clear()
                        liveHolder.activeSnapGuides = SnapGuides()
                        liveHolder.totalMoveDist = 0f
                        liveHolder.isGestureNavigating = false
                        liveHolder.gesturePan = currentPan
                        liveHolder.gestureZoom = currentZoom
                        isPanning = false
                        hasPinchedInGesture = false

                        activePointers[firstDown.id] = SketchPoint(firstDown.position.x, firstDown.position.y)

                        if (currentTouchPanMode) {
                            isPanning = true
                            panStartOrigin = currentPan
                            panStartPointer = SketchPoint(firstDown.position.x, firstDown.position.y)
                        } else if (currentTool == ToolType.SELECT) {
                            val pt = toWorldPoint(firstDown.position, firstDown.pressure, firstDown.type)
                            val vis = StrokeMath.getVisibleStrokesInLayerOrder(currentStrokes, currentLayers)
                            val hit = StrokeMath.hitTestShape(vis, pt)
                            onSelectStroke(hit?.id)
                        } else {
                            val pt = toWorldPoint(firstDown.position, firstDown.pressure, firstDown.type)
                            val startPt = if (currentTool == ToolType.RECT || currentTool == ToolType.CIRCLE) {
                                StrokeMath.snapPointToGridOrObjects(pt, currentStrokes).point
                            } else {
                                pt
                            }
                            val effectiveSize = if (currentTool == ToolType.HIGHLIGHTER) {
                                maxOf(12f, currentStrokeSize * 2f)
                            } else {
                                currentStrokeSize
                            }
                            val effectiveOpacity = when (currentTool) {
                                ToolType.ERASER -> 1f
                                ToolType.HIGHLIGHTER -> StrokeMath.clamp(0.35f * currentBrushOpacity, 0.08f, 0.65f)
                                else -> StrokeMath.clamp(currentBrushOpacity, 0.1f, 1f)
                            }
                            val stroke = Stroke(
                                id = "s_${System.currentTimeMillis()}_${(1000..9999).random()}",
                                userId = currentLocalUserId,
                                layerId = currentActiveLayerId,
                                tool = currentTool,
                                colorArgb = currentColor,
                                size = effectiveSize,
                                opacity = effectiveOpacity,
                                points = emptyList(),
                                symmetry = currentSymmetryMode,
                                symmetryCenter = if (currentSymmetryMode != SymmetryMode.NONE) currentSymmetryCenter else null,
                                brush = currentBrush,
                                pressureSensitive = currentPressureEnabled,
                                timestamp = System.currentTimeMillis()
                            )
                            liveHolder.activePoints.add(startPt)
                            liveHolder.activeStroke = stroke
                            activeDrawTick++
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                if (!change.pressed) {
                                    activePointers.remove(change.id)
                                } else if (!activePointers.containsKey(change.id)) {
                                    activePointers[change.id] = SketchPoint(change.position.x, change.position.y)
                                }
                            }

                            if (activePointers.isEmpty()) {
                                liveHolder.activeStroke?.let { pending ->
                                    val done = pending.copy(points = liveHolder.activePoints.toList())
                                    liveHolder.activeStroke = null
                                    liveHolder.activePoints.clear()
                                    liveHolder.activeSnapGuides = SnapGuides()
                                    if (done.points.isNotEmpty()) {
                                        liveHolder.pendingCommitStroke = done
                                        onCommitStroke(done)
                                    }
                                    activeDrawTick++
                                }
                                if (liveHolder.isGestureNavigating) {
                                    liveHolder.isGestureNavigating = false
                                    onPanZoom(liveHolder.gesturePan, liveHolder.gestureZoom)
                                    activeDrawTick++
                                }
                                break
                            }

                            if (activePointers.size >= 2) {
                                // Transition cleanly to 2-finger pinch & pan
                                liveHolder.activeStroke?.let { pending ->
                                    val isTinyPinchDot =
                                        liveHolder.activePoints.size <= 3 && liveHolder.totalMoveDist < 12f
                                    liveHolder.activeStroke = null
                                    liveHolder.activeSnapGuides = SnapGuides()
                                    if (!isTinyPinchDot && liveHolder.activePoints.isNotEmpty()) {
                                        val done = pending.copy(points = liveHolder.activePoints.toList())
                                        liveHolder.pendingCommitStroke = done
                                        onCommitStroke(done)
                                    }
                                    liveHolder.activePoints.clear()
                                }
                                hasPinchedInGesture = true
                                isPanning = false
                                liveHolder.isGestureNavigating = true

                                val entries = activePointers.entries.toList()
                                val id1 = entries[0].key
                                val p1Prev = entries[0].value
                                val id2 = entries[1].key
                                val p2Prev = entries[1].value

                                val c1 = event.changes.find { it.id == id1 }
                                val c2 = event.changes.find { it.id == id2 }
                                val p1Next = c1?.let { SketchPoint(it.position.x, it.position.y) } ?: p1Prev
                                val p2Next = c2?.let { SketchPoint(it.position.x, it.position.y) } ?: p2Prev

                                activePointers[id1] = p1Next
                                activePointers[id2] = p2Next

                                val nextTransform = TouchMath.computePinchTransform(
                                    p1Prev = p1Prev,
                                    p2Prev = p2Prev,
                                    p1Next = p1Next,
                                    p2Next = p2Next,
                                    currentPan = liveHolder.gesturePan,
                                    currentZoom = liveHolder.gestureZoom
                                )
                                liveHolder.gesturePan = nextTransform.pan
                                liveHolder.gestureZoom = nextTransform.zoom
                                activeDrawTick++
                                onPanZoom(nextTransform.pan, nextTransform.zoom)
                                event.changes.forEach { it.consume() }
                            } else if (!hasPinchedInGesture) {
                                val primary = event.changes.firstOrNull { it.pressed }
                                if (primary != null) {
                                    activePointers[primary.id] = SketchPoint(primary.position.x, primary.position.y)
                                    if (isPanning) {
                                        val dx = primary.position.x - panStartPointer.x
                                        val dy = primary.position.y - panStartPointer.y
                                        val nextPan = SketchPoint(panStartOrigin.x + dx, panStartOrigin.y + dy)
                                        liveHolder.isGestureNavigating = true
                                        liveHolder.gesturePan = nextPan
                                        liveHolder.gestureZoom = currentZoom
                                        activeDrawTick++
                                        onPanZoom(nextPan, currentZoom)
                                        primary.consume()
                                    } else {
                                        val current = liveHolder.activeStroke
                                        if (current != null && liveHolder.activePoints.isNotEmpty()) {
                                            val pt = toWorldPoint(primary.position, primary.pressure, primary.type)
                                            when (current.tool) {
                                                ToolType.RECT, ToolType.CIRCLE -> {
                                                    val snapped = StrokeMath.snapPointToGridOrObjects(
                                                        pt,
                                                        currentStrokes,
                                                        excludeStrokeId = current.id
                                                    )
                                                    val start = liveHolder.activePoints[0]
                                                    liveHolder.totalMoveDist = StrokeMath.distance(start, snapped.point)
                                                    liveHolder.activeSnapGuides = snapped.guides
                                                    liveHolder.activePoints.clear()
                                                    liveHolder.activePoints.add(start)
                                                    liveHolder.activePoints.add(snapped.point)
                                                    activeDrawTick++
                                                }
                                                ToolType.LINE -> {
                                                    val start = liveHolder.activePoints[0]
                                                    liveHolder.totalMoveDist = StrokeMath.distance(start, pt)
                                                    liveHolder.activePoints.clear()
                                                    liveHolder.activePoints.add(start)
                                                    liveHolder.activePoints.add(pt)
                                                    activeDrawTick++
                                                }
                                                else -> {
                                                    var addedAny = false
                                                    val histList = primary.historical
                                                    for (hIdx in histList.indices) {
                                                        val histPt = toWorldPoint(
                                                            histList[hIdx].position,
                                                            primary.pressure,
                                                            primary.type
                                                        )
                                                        val prevPt = liveHolder.activePoints.last()
                                                        val nextPt = StrokeMath.stabilizePoint(
                                                            prevPt,
                                                            histPt,
                                                            currentStabilization
                                                        )
                                                        if (prevPt.x != nextPt.x || prevPt.y != nextPt.y) {
                                                            liveHolder.totalMoveDist += StrokeMath.distance(prevPt, nextPt)
                                                            liveHolder.activePoints.add(nextPt)
                                                            addedAny = true
                                                        }
                                                    }
                                                    val prevPt = liveHolder.activePoints.last()
                                                    val nextPt = StrokeMath.stabilizePoint(
                                                        prevPt,
                                                        pt,
                                                        currentStabilization
                                                    )
                                                    if (prevPt.x != nextPt.x || prevPt.y != nextPt.y) {
                                                        liveHolder.totalMoveDist += StrokeMath.distance(prevPt, nextPt)
                                                        liveHolder.activePoints.add(nextPt)
                                                        addedAny = true
                                                    }
                                                    if (addedAny) {
                                                        activeDrawTick++
                                                    }
                                                }
                                            }
                                            primary.consume()
                                        }
                                    }
                                }
                            } else {
                                // After pinching with 2 fingers, keep position updated for remaining finger without stray marks
                                for (change in event.changes) {
                                    if (change.pressed) {
                                        activePointers[change.id] = SketchPoint(change.position.x, change.position.y)
                                        change.consume()
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            // Reading activeDrawTick ensures only DrawScope invalidates during active drawing/pinching (0 Compose recompositions)
            @Suppress("UNUSED_VARIABLE")
            val tick = activeDrawTick

            val activePan = if (liveHolder.isGestureNavigating) liveHolder.gesturePan else pan
            val activeZoom = if (liveHolder.isGestureNavigating) liveHolder.gestureZoom else zoom

            // 1. Batched Dot Grid (single drawPoints call)
            if (showGrid) {
                drawDotGrid(pan = activePan, zoom = activeZoom, buffer = dotGridBuffer)
            }

            // 2. Baked offscreen committed strokes + live active stroke
            val wPx = size.width.roundToInt()
            val hPx = size.height.roundToInt()
            val nc = drawContext.canvas.nativeCanvas

            val pendingCommit = liveHolder.pendingCommitStroke
                ?.takeIf { p -> visibleStrokes.none { it.id == p.id } }
            val activeStroke = liveHolder.activeStroke
            val activeRenderStroke = if (activeStroke != null && liveHolder.activePoints.isNotEmpty()) {
                activeStroke.copy(points = liveHolder.activePoints)
            } else {
                null
            }

            val needsEraserLayer = activeRenderStroke?.tool == ToolType.ERASER ||
                pendingCommit?.tool == ToolType.ERASER

            val layerSave = if (needsEraserLayer) {
                nc.saveLayer(0f, 0f, size.width, size.height, null)
            } else {
                -1
            }

            val drewLivePreview = if (liveHolder.isGestureNavigating) {
                committedCache.drawTransformedPreview(nc, activePan, activeZoom)
            } else {
                false
            }
            if (!drewLivePreview) {
                val bakedBitmap = committedCache.ensureAndBake(
                    width = wPx,
                    height = hPx,
                    visibleStrokes = visibleStrokes,
                    pan = activePan,
                    zoom = activeZoom,
                    symmetryCenter = symmetryCenter
                )
                if (bakedBitmap != null) {
                    nc.drawBitmap(bakedBitmap, 0f, 0f, null)
                }
            }

            if (pendingCommit != null || activeRenderStroke != null) {
                nc.save()
                nc.translate(activePan.x, activePan.y)
                nc.scale(activeZoom, activeZoom)
                if (pendingCommit != null) {
                    BrushEngine.renderBrushStrokeToCanvas(nc, pendingCommit, symmetryCenter)
                }
                if (activeRenderStroke != null) {
                    BrushEngine.renderBrushStrokeToCanvas(nc, activeRenderStroke, symmetryCenter)
                }
                nc.restore()
            }

            if (layerSave >= 0) {
                nc.restoreToCount(layerSave)
            }

            // 3. Cached tiled Paper Grain overlay (single drawRect call)
            if (showPaperGrain) {
                val grainPaint = paperGrainCache.getPaint(18.dp.toPx(), VellumColors.InkCharcoal.toArgb())
                nc.drawRect(0f, 0f, size.width, size.height, grainPaint)
            }

            // 4. Symmetry Axes & Guides
            if (symmetryMode != SymmetryMode.NONE || guideMode != GuideMode.NONE) {
                drawSymmetryAndGuides(
                    symmetryMode = symmetryMode,
                    symmetryCenter = symmetryCenter,
                    guideMode = guideMode,
                    pan = activePan,
                    zoom = activeZoom
                )
            }

            // 5. Alignment Snapping Guides (local to active drag)
            val guides = liveHolder.activeSnapGuides
            guides.x?.let { gx ->
                val sx = gx * activeZoom + activePan.x
                drawLine(
                    color = VellumColors.CobaltBlue.copy(alpha = 0.6f),
                    start = Offset(sx, 0f),
                    end = Offset(sx, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            guides.y?.let { gy ->
                val sy = gy * activeZoom + activePan.y
                drawLine(
                    color = VellumColors.CobaltBlue.copy(alpha = 0.6f),
                    start = Offset(0f, sy),
                    end = Offset(size.width, sy),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        // Selection Bounding Box & Screen-Space Rotation Handle
        if (selectedStroke != null && selectedBounds != null) {
            val density = LocalDensity.current
            val boxWidthPx = (selectedBounds.width * zoom).coerceAtLeast(with(density) { 12.dp.toPx() })
            val boxHeightPx = (selectedBounds.height * zoom).coerceAtLeast(with(density) { 12.dp.toPx() })
            val cxPx = selectedBounds.cx * zoom + pan.x
            val cyPx = selectedBounds.cy * zoom + pan.y
            val leftPx = cxPx - boxWidthPx / 2f
            val topPx = cyPx - boxHeightPx / 2f
            val boxW = with(density) { boxWidthPx.toDp() }
            val boxH = with(density) { boxHeightPx.toDp() }
            val deg = Math.toDegrees(selectedStroke.rotation.toDouble()).toFloat()

            // Rotated bounding box outline + stem (visual only)
            Box(
                modifier = Modifier
                    .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                    .size(boxW, boxH)
                    .graphicsLayer { rotationZ = deg }
                    .border(1.5.dp, VellumColors.CobaltBlue.copy(alpha = 0.85f))
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-28).dp)
                        .width(1.5.dp)
                        .height(28.dp)
                        .background(VellumColors.CobaltBlue.copy(alpha = 0.85f))
                )
            }

            // Unrotated handle placed at exact rotated world-to-screen handle center
            val stemOffsetPx = boxHeightPx / 2f + with(density) { 28.dp.toPx() }
            val rad = selectedStroke.rotation.toDouble()
            val handleCenterX = cxPx + (stemOffsetPx * sin(rad)).toFloat()
            val handleCenterY = cyPx - (stemOffsetPx * cos(rad)).toFloat()
            val handleHalfPx = with(density) { 24.dp.toPx() }
            var dragScreenX = handleCenterX
            var dragScreenY = handleCenterY

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (handleCenterX - handleHalfPx).roundToInt(),
                            (handleCenterY - handleHalfPx).roundToInt()
                        )
                    }
                    .size(48.dp)
                    .pointerInput(selectedStroke.id) {
                        detectDragGestures(
                            onDragStart = {
                                val s = currentStrokes.find { it.id == selectedStroke.id } ?: selectedStroke
                                val b = StrokeMath.getShapeBounds(s) ?: selectedBounds
                                val scx = b.cx * currentZoom + currentPan.x
                                val scy = b.cy * currentZoom + currentPan.y
                                val bh = (b.height * currentZoom).coerceAtLeast(with(density) { 12.dp.toPx() })
                                val dist = bh / 2f + with(density) { 28.dp.toPx() }
                                dragScreenX = scx + (dist * sin(s.rotation.toDouble())).toFloat()
                                dragScreenY = scy - (dist * cos(s.rotation.toDouble())).toFloat()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragScreenX += dragAmount.x
                                dragScreenY += dragAmount.y
                                val s = currentStrokes.find { it.id == selectedStroke.id } ?: selectedStroke
                                val b = StrokeMath.getShapeBounds(s) ?: selectedBounds
                                val worldPtr = StrokeMath.screenToWorld(
                                    SketchPoint(dragScreenX, dragScreenY),
                                    currentPan,
                                    currentZoom
                                )
                                val rot = StrokeMath.calculateRotationAngle(b.cx, b.cy, worldPtr)
                                onUpdateStrokePreview(s.copy(rotation = rot))
                            },
                            onDragEnd = {
                                val s = currentStrokes.find { it.id == selectedStroke.id }
                                if (s != null) {
                                    onCommitStroke(s)
                                }
                            },
                            onDragCancel = {
                                val s = currentStrokes.find { it.id == selectedStroke.id }
                                if (s != null) {
                                    onCommitStroke(s)
                                }
                            }
                        )
                    }
                    .testTag("rotation_handle")
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, VellumColors.CobaltBlue, CircleShape)
                )
            }
        }

        // Tablet / Expanded HUDs: Top-left Symmetry & Guides, Bottom-right Zoom
        if (isExpanded) {
            TabletSymmetryGuidesHud(
                symmetryMode = symmetryMode,
                guideMode = guideMode,
                onChangeSymmetryMode = onChangeSymmetryMode,
                onRecenterSymmetry = onRecenterSymmetry,
                onChangeGuideMode = onChangeGuideMode,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            )

            TabletZoomHud(
                zoom = zoom,
                onZoomOut = { onZoomStep(-0.25f) },
                onResetView = onResetView,
                onZoomIn = { onZoomStep(0.25f) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        } else if (abs(zoom - 1f) > 0.01f || abs(pan.x) > 4f || abs(pan.y) > 4f) {
            TabletZoomHud(
                zoom = zoom,
                onZoomOut = { onZoomStep(-0.25f) },
                onResetView = onResetView,
                onZoomIn = { onZoomStep(0.25f) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .testTag("compact_zoom_hud")
            )
        }
    }
}

private fun DrawScope.drawDotGrid(pan: SketchPoint, zoom: Float, buffer: DotGridBuffer) {
    val step = 24f * zoom
    if (step < 6f) return
    val startX = ((pan.x % step) + step) % step
    val startY = ((pan.y % step) + step) % step
    val radius = 1f.coerceAtLeast(0.8f * zoom.coerceAtMost(1.4f))

    val cols = ceil(((size.width - startX) / step).toDouble()).toInt().coerceAtLeast(0) + 1
    val rows = ceil(((size.height - startY) / step).toDouble()).toInt().coerceAtLeast(0) + 1
    val needed = cols * rows * 2
    if (needed <= 0) return

    if (buffer.points.size < needed) {
        buffer.points = FloatArray(needed)
    }
    val pts = buffer.points
    var idx = 0
    var y = startY
    while (y < size.height) {
        var x = startX
        while (x < size.width) {
            if (idx + 1 >= pts.size) break
            pts[idx++] = x
            pts[idx++] = y
            x += step
        }
        y += step
    }
    if (idx >= 2) {
        buffer.paint.color = VellumColors.GridDot.toArgb()
        buffer.paint.strokeWidth = radius * 2f
        drawContext.canvas.nativeCanvas.drawPoints(pts, 0, idx, buffer.paint)
    }
}

private fun DrawScope.drawSymmetryAndGuides(
    symmetryMode: SymmetryMode,
    symmetryCenter: SketchPoint,
    guideMode: GuideMode,
    pan: SketchPoint,
    zoom: Float
) {
    val screenSym = StrokeMath.worldToScreen(symmetryCenter, pan, zoom)
    val span = hypot(size.width.toDouble(), size.height.toDouble()).toFloat() + 120f
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))

    when (symmetryMode) {
        SymmetryMode.HORIZONTAL -> {
            drawLine(
                color = VellumColors.CobaltBlue.copy(alpha = 0.45f),
                start = Offset(screenSym.x, 0f),
                end = Offset(screenSym.x, size.height),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = dashEffect
            )
        }
        SymmetryMode.VERTICAL -> {
            drawLine(
                color = VellumColors.CobaltBlue.copy(alpha = 0.45f),
                start = Offset(0f, screenSym.y),
                end = Offset(size.width, screenSym.y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = dashEffect
            )
        }
        SymmetryMode.RADIAL -> {
            for (i in 0 until 6) {
                val angle = i * PI / 3.0
                drawLine(
                    color = VellumColors.CobaltBlue.copy(alpha = 0.35f),
                    start = Offset(screenSym.x, screenSym.y),
                    end = Offset(
                        screenSym.x + (cos(angle) * span).toFloat(),
                        screenSym.y + (sin(angle) * span).toFloat()
                    ),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = dashEffect
                )
            }
        }
        SymmetryMode.KALEIDOSCOPE -> {
            for (i in 0 until 12) {
                val angle = i * PI / 6.0
                drawLine(
                    color = VellumColors.CobaltBlue.copy(alpha = if (i % 2 == 0) 0.4f else 0.25f),
                    start = Offset(screenSym.x, screenSym.y),
                    end = Offset(
                        screenSym.x + (cos(angle) * span).toFloat(),
                        screenSym.y + (sin(angle) * span).toFloat()
                    ),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = dashEffect
                )
            }
        }
        SymmetryMode.NONE -> {}
    }

    if (symmetryMode != SymmetryMode.NONE) {
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = Offset(screenSym.x, screenSym.y)
        )
        drawCircle(
            color = VellumColors.CobaltBlue,
            radius = 4.dp.toPx(),
            center = Offset(screenSym.x, screenSym.y),
            style = ComposeStroke(width = 1.5.dp.toPx())
        )
    }

    if (guideMode == GuideMode.PERSPECTIVE) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val guideColor = VellumColors.SlateSecondary.copy(alpha = 0.28f)
        drawLine(guideColor, Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1.dp.toPx())
        for (i in 0 until 12) {
            val angle = i * PI * 2.0 / 12.0
            drawLine(
                color = guideColor,
                start = Offset(cx, cy),
                end = Offset(cx + (cos(angle) * span).toFloat(), cy + (sin(angle) * span).toFloat()),
                strokeWidth = 1.dp.toPx()
            )
        }
    }

    if (guideMode == GuideMode.RULER) {
        val step = 24f * zoom
        if (step >= 6f) {
            val guideColor = VellumColors.SlateSecondary.copy(alpha = 0.45f)
            val cols = ceil((size.width / step).toDouble()).toInt() + 2
            val rows = ceil((size.height / step).toDouble()).toInt() + 2
            val offsetX = pan.x % step
            val offsetY = pan.y % step
            for (i in 0..cols) {
                val rx = i * step + offsetX
                val tickH = if (i % 4 == 0) 14.dp.toPx() else 7.dp.toPx()
                drawLine(guideColor, Offset(rx, 0f), Offset(rx, tickH), strokeWidth = 1.dp.toPx())
            }
            for (i in 0..rows) {
                val ry = i * step + offsetY
                val tickW = if (i % 4 == 0) 14.dp.toPx() else 7.dp.toPx()
                drawLine(guideColor, Offset(0f, ry), Offset(tickW, ry), strokeWidth = 1.dp.toPx())
            }
        }
    }
}

@Composable
private fun TabletSymmetryGuidesHud(
    symmetryMode: SymmetryMode,
    guideMode: GuideMode,
    onChangeSymmetryMode: (SymmetryMode) -> Unit,
    onRecenterSymmetry: () -> Unit,
    onChangeGuideMode: (GuideMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var symMenuOpen by remember { mutableStateOf(false) }
    var guideMenuOpen by remember { mutableStateOf(false) }

    Surface(
        color = VellumColors.PaperWhite.copy(alpha = 0.95f),
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 2.dp,
        modifier = modifier.border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(10.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("Symmetry", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = VellumColors.SlateSecondary)
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(VellumColors.HoverSurface)
                        .clickable { symMenuOpen = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(symmetryMode.label, fontSize = 11.sp, color = VellumColors.InkCharcoal)
                }
                DropdownMenu(expanded = symMenuOpen, onDismissRequest = { symMenuOpen = false }) {
                    SymmetryMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label, fontSize = 12.sp) },
                            onClick = {
                                onChangeSymmetryMode(mode)
                                symMenuOpen = false
                            }
                        )
                    }
                }
            }

            if (symmetryMode != SymmetryMode.NONE) {
                Text(
                    text = "Center",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = VellumColors.CobaltBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onRecenterSymmetry)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            Text("|", color = VellumColors.GridDot, fontSize = 12.sp)

            Text("Guides", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = VellumColors.SlateSecondary)
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(VellumColors.HoverSurface)
                        .clickable { guideMenuOpen = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(guideMode.label, fontSize = 11.sp, color = VellumColors.InkCharcoal)
                }
                DropdownMenu(expanded = guideMenuOpen, onDismissRequest = { guideMenuOpen = false }) {
                    GuideMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label, fontSize = 12.sp) },
                            onClick = {
                                onChangeGuideMode(mode)
                                guideMenuOpen = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletZoomHud(
    zoom: Float,
    onZoomOut: () -> Unit,
    onResetView: () -> Unit,
    onZoomIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = VellumColors.PaperWhite.copy(alpha = 0.95f),
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 2.dp,
        modifier = modifier.border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(10.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onZoomOut)
            ) {
                Text("-", fontSize = 13.sp, color = VellumColors.InkCharcoal)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onResetView)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${(zoom * 100f).roundToInt()}%",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = VellumColors.InkCharcoal
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onZoomIn)
            ) {
                Text("+", fontSize = 13.sp, color = VellumColors.InkCharcoal)
            }
        }
    }
}
