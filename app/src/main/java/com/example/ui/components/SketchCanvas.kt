package com.example.ui.components

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
import androidx.compose.runtime.getValue
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
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin

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
    snapGuides: SnapGuides,
    isExpanded: Boolean,
    onCanvasResize: (width: Float, height: Float) -> Unit,
    onPanZoom: (pan: SketchPoint, zoom: Float) -> Unit,
    onZoomStep: (Float) -> Unit,
    onResetView: () -> Unit,
    onSelectStroke: (String?) -> Unit,
    onSnapGuides: (SnapGuides) -> Unit,
    onStartStroke: (Stroke) -> Unit,
    onAppendPoints: (strokeId: String, points: List<SketchPoint>) -> Unit,
    onEndStroke: (Stroke) -> Unit,
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
    val currentStabilization by rememberUpdatedState(stabilization)
    val currentPressureEnabled by rememberUpdatedState(pressureEnabled)
    val currentZoom by rememberUpdatedState(zoom)
    val currentPan by rememberUpdatedState(pan)
    val currentSymmetryMode by rememberUpdatedState(symmetryMode)
    val currentSymmetryCenter by rememberUpdatedState(symmetryCenter)
    val currentTouchPanMode by rememberUpdatedState(touchPanMode)

    val visibleStrokes = remember(strokes, layers) {
        StrokeMath.getVisibleStrokesInLayerOrder(strokes, layers)
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
                    var activeStroke: Stroke? = null
                    var isPanning = false
                    var isPinching = false
                    var panStartOrigin = SketchPoint(0f, 0f)
                    var panStartPointer = SketchPoint(0f, 0f)

                    fun extractPressure(rawPressure: Float, type: PointerType): Float {
                        if (!currentPressureEnabled) return 0.5f
                        if (rawPressure.isNaN()) return 0.5f
                        if (type == PointerType.Touch && rawPressure == 0f) return 0.5f
                        if (type == PointerType.Mouse && kotlin.math.abs(rawPressure - 0.5f) < 1e-4f) return 0.5f
                        return StrokeMath.clamp(rawPressure, 0.05f, 1f)
                    }

                    fun toWorldPoint(pos: Offset, pressure: Float, type: PointerType): SketchPoint {
                        val safeZoom = if (currentZoom > 0f) currentZoom else 1f
                        return SketchPoint(
                            x = round(((pos.x - currentPan.x) / safeZoom) * 10f) / 10f,
                            y = round(((pos.y - currentPan.y) / safeZoom) * 10f) / 10f,
                            pressure = extractPressure(pressure, type)
                        )
                    }

                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        activePointers.clear()
                        activeStroke = null
                        isPanning = false
                        isPinching = false

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
                            val stroke = Stroke(
                                id = "s_${System.currentTimeMillis()}_${(1000..9999).random()}",
                                userId = currentLocalUserId,
                                layerId = currentActiveLayerId,
                                tool = currentTool,
                                colorArgb = currentColor,
                                size = effectiveSize,
                                opacity = if (currentTool == ToolType.HIGHLIGHTER) 0.35f else 1f,
                                points = listOf(startPt),
                                symmetry = currentSymmetryMode,
                                symmetryCenter = if (currentSymmetryMode != SymmetryMode.NONE) currentSymmetryCenter else null,
                                brush = currentBrush,
                                pressureSensitive = currentPressureEnabled,
                                timestamp = System.currentTimeMillis()
                            )
                            activeStroke = stroke
                            onStartStroke(stroke)
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                if (change.pressed) {
                                    if (!activePointers.containsKey(change.id)) {
                                        activePointers[change.id] = SketchPoint(change.position.x, change.position.y)
                                    }
                                }
                            }

                            if (activePointers.size >= 2) {
                                // Transition cleanly to 2-finger pinch & pan
                                activeStroke?.let { pending ->
                                    activeStroke = null
                                    onSnapGuides(SnapGuides())
                                    onEndStroke(pending)
                                }
                                isPanning = false
                                isPinching = true

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
                                    currentPan = currentPan,
                                    currentZoom = currentZoom
                                )
                                onPanZoom(nextTransform.pan, nextTransform.zoom)
                                event.changes.forEach { it.consume() }
                            } else if (!isPinching) {
                                val primary = event.changes.firstOrNull()
                                if (primary != null && primary.pressed) {
                                    activePointers[primary.id] = SketchPoint(primary.position.x, primary.position.y)
                                    if (isPanning) {
                                        val dx = primary.position.x - panStartPointer.x
                                        val dy = primary.position.y - panStartPointer.y
                                        onPanZoom(
                                            SketchPoint(panStartOrigin.x + dx, panStartOrigin.y + dy),
                                            currentZoom
                                        )
                                        primary.consume()
                                    } else {
                                        val current = activeStroke
                                        if (current != null) {
                                            val pt = toWorldPoint(primary.position, primary.pressure, primary.type)
                                            when (current.tool) {
                                                ToolType.RECT, ToolType.CIRCLE -> {
                                                    val snapped = StrokeMath.snapPointToGridOrObjects(
                                                        pt,
                                                        currentStrokes,
                                                        excludeStrokeId = current.id
                                                    )
                                                    onSnapGuides(snapped.guides)
                                                    val updated = current.copy(points = listOf(current.points.first(), snapped.point))
                                                    activeStroke = updated
                                                    onEndStroke(updated)
                                                }
                                                ToolType.LINE -> {
                                                    val updated = current.copy(points = listOf(current.points.first(), pt))
                                                    activeStroke = updated
                                                    onEndStroke(updated)
                                                }
                                                else -> {
                                                    val ptsToAdd = mutableListOf<SketchPoint>()
                                                    for (hist in primary.historical) {
                                                        ptsToAdd.add(toWorldPoint(hist.position, primary.pressure, primary.type))
                                                    }
                                                    ptsToAdd.add(pt)

                                                    val stabilizedBatch = ArrayList<SketchPoint>(ptsToAdd.size)
                                                    val mutableList = current.points.toMutableList()
                                                    for (samplePt in ptsToAdd) {
                                                        val prevPt = mutableList.lastOrNull()
                                                        val nextPt = if (prevPt != null) {
                                                            StrokeMath.stabilizePoint(prevPt, samplePt, currentStabilization)
                                                        } else {
                                                            samplePt
                                                        }
                                                        val last = mutableList.lastOrNull()
                                                        if (last == null || last.x != nextPt.x || last.y != nextPt.y) {
                                                            mutableList.add(nextPt)
                                                            stabilizedBatch.add(nextPt)
                                                        }
                                                    }
                                                    if (stabilizedBatch.isNotEmpty()) {
                                                        onAppendPoints(current.id, stabilizedBatch)
                                                    }
                                                    activeStroke = current.copy(points = mutableList)
                                                }
                                            }
                                            primary.consume()
                                        }
                                    }
                                }
                            }

                            for (change in event.changes) {
                                if (!change.pressed) {
                                    activePointers.remove(change.id)
                                }
                            }
                            if (activePointers.size < 2) {
                                isPinching = false
                            }
                            if (activePointers.isEmpty()) {
                                activeStroke?.let { done ->
                                    activeStroke = null
                                    onSnapGuides(SnapGuides())
                                    onEndStroke(done)
                                }
                                break
                            }
                        }
                    }
                }
        ) {
            // 1. Dot Grid
            if (showGrid) {
                drawDotGrid(pan = pan, zoom = zoom)
            }

            // 2. Strokes inside offscreen saveLayer so Eraser DST_OUT preserves paper & grid
            val nc = drawContext.canvas.nativeCanvas
            val layerSave = nc.saveLayer(0f, 0f, size.width, size.height, null)
            nc.save()
            nc.translate(pan.x, pan.y)
            nc.scale(zoom, zoom)
            for (stroke in visibleStrokes) {
                BrushEngine.renderBrushStrokeToCanvas(nc, stroke, symmetryCenter)
            }
            nc.restore()
            nc.restoreToCount(layerSave)

            // 3. Subtle Paper Grain overlay
            if (showPaperGrain) {
                drawPaperGrain()
            }

            // 4. Symmetry Axes & Guides
            if (symmetryMode != SymmetryMode.NONE || guideMode != GuideMode.NONE) {
                drawSymmetryAndGuides(
                    symmetryMode = symmetryMode,
                    symmetryCenter = symmetryCenter,
                    guideMode = guideMode,
                    pan = pan,
                    zoom = zoom
                )
            }

            // 5. Alignment Snapping Guides
            snapGuides.x?.let { gx ->
                val sx = gx * zoom + pan.x
                drawLine(
                    color = VellumColors.CobaltBlue.copy(alpha = 0.6f),
                    start = Offset(sx, 0f),
                    end = Offset(sx, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            snapGuides.y?.let { gy ->
                val sy = gy * zoom + pan.y
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
                                onEndStroke(s.copy(rotation = rot))
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
        }
    }
}

private fun DrawScope.drawDotGrid(pan: SketchPoint, zoom: Float) {
    val step = 24f * zoom
    if (step < 6f) return
    val startX = ((pan.x % step) + step) % step
    val startY = ((pan.y % step) + step) % step
    val radius = 1f.coerceAtLeast(0.8f * zoom.coerceAtMost(1.4f))

    var y = startY
    while (y < size.height) {
        var x = startX
        while (x < size.width) {
            drawCircle(
                color = VellumColors.GridDot,
                radius = radius,
                center = Offset(x, y)
            )
            x += step
        }
        y += step
    }
}

private fun DrawScope.drawPaperGrain() {
    val step = 18.dp.toPx()
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) 0f else step / 2f
        while (x < size.width) {
            drawCircle(
                color = VellumColors.InkCharcoal.copy(alpha = 0.03f),
                radius = 0.7f,
                center = Offset(x, y)
            )
            x += step
        }
        y += step
        row++
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
