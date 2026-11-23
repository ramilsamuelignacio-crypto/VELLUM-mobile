package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.example.math.StrokeMath
import com.example.model.BrushKind
import com.example.model.SketchPoint
import com.example.model.Stroke
import com.example.model.SymmetryMode
import com.example.model.ToolType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

object BrushEngine {
    private val grainCache = object : LinkedHashMap<String, Bitmap>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean {
            return size > 32
        }
    }

    fun hasVariablePressure(points: List<SketchPoint>): Boolean {
        return points.any { pt ->
            val p = pt.pressure
            p != null && p > 0f && abs(p - 0.5f) > 0.02f
        }
    }

    fun computePressureStrokeWidth(
        baseSize: Float,
        pressure: Float,
        usePressure: Boolean = true
    ): Float {
        if (!usePressure) return max(0.75f, baseSize)
        val p = StrokeMath.clamp(pressure, 0.05f, 1f)
        val scale = 0.18f + 1.42f * p.toDouble().pow(1.05).toFloat()
        return max(0.75f, baseSize * scale)
    }

    fun interpolateStrokePoints(points: List<SketchPoint>, spacing: Float = 2f): List<SketchPoint> {
        if (points.size <= 1) return points
        val stepDist = max(0.5f, spacing)
        val result = mutableListOf(points.first())

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val dist = hypot((p1.x - p0.x).toDouble(), (p1.y - p0.y).toDouble()).toFloat()
            if (dist < 0.01f) continue

            val steps = max(1, ceil((dist / stepDist).toDouble()).toInt())
            val pr0 = p0.pressure ?: 0.5f
            val pr1 = p1.pressure ?: 0.5f

            for (s in 1..steps) {
                val t = s.toFloat() / steps.toFloat()
                result.add(
                    SketchPoint(
                        x = p0.x + (p1.x - p0.x) * t,
                        y = p0.y + (p1.y - p0.y) * t,
                        pressure = pr0 + (pr1 - pr0) * t
                    )
                )
            }
        }
        return result
    }

    fun generateThumbnailStrokePoints(
        width: Float = 44f,
        height: Float = 20f,
        sampleCount: Int = 28
    ): List<SketchPoint> {
        val padX = max(4f, width * 0.14f)
        val usableW = max(4f, width - padX * 2f)
        val midY = height / 2f
        val amp = max(2f, height * 0.28f)
        val count = max(4, sampleCount)
        return List(count) { i ->
            val t = i.toFloat() / (count - 1).toFloat()
            val x = padX + usableW * t
            val y = midY - sin(t * PI * 2.0).toFloat() * amp
            val pressure = StrokeMath.clamp(0.25f + 0.75f * sin(t * PI).toFloat(), 0.2f, 1f)
            SketchPoint(x = x, y = y, pressure = pressure)
        }
    }

    @Synchronized
    fun getCachedPencilGrainBitmap(colorArgb: Int, size: Int = 24): Bitmap {
        val key = "${colorArgb}:$size"
        grainCache[key]?.let { return it }

        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val r = Color.red(colorArgb)
        val g = Color.green(colorArgb)
        val b = Color.blue(colorArgb)
        val center = size / 2f
        val pixels = IntArray(size * size)

        for (y in 0 until size) {
            for (x in 0 until size) {
                val dist = hypot((x - center).toDouble(), (y - center).toDouble()).toFloat() / center
                if (dist > 1f) continue
                val hash = sin((x * 12.9898 + y * 78.233) * 43758.5453)
                val noise = hash - floor(hash)
                val falloff = ((1f - dist) * (if (noise > 0.32) 255f else 55f)).toInt().coerceIn(0, 255)
                pixels[y * size + x] = Color.argb(falloff, r, g, b)
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        grainCache[key] = bmp
        return bmp
    }

    fun renderAirbrushStamp(
        canvas: Canvas,
        x: Float,
        y: Float,
        radius: Float,
        colorArgb: Int,
        opacity: Float
    ) {
        val r = max(1f, radius)
        val cr = Color.red(colorArgb)
        val cg = Color.green(colorArgb)
        val cb = Color.blue(colorArgb)
        val effectiveAlpha = StrokeMath.clamp(opacity * 0.25f, 0.02f, 0.4f)

        val shader = RadialGradient(
            x, y, r,
            intArrayOf(
                Color.argb(255, cr, cg, cb),
                Color.argb(115, cr, cg, cb),
                Color.argb(0, cr, cg, cb)
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            alpha = (effectiveAlpha * 255f).toInt().coerceIn(0, 255)
            this.shader = shader
        }
        canvas.drawCircle(x, y, r, paint)
    }

    private fun sampleSmoothQuadraticPoints(
        points: List<SketchPoint>,
        spacing: Float = 2f
    ): List<SketchPoint> {
        if (points.size <= 2) return interpolateStrokePoints(points, spacing)
        val stepDist = max(0.75f, spacing)
        val sampled = mutableListOf(points.first().copy(pressure = points.first().pressure ?: 0.5f))

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val prevMid = if (i == 0) {
                p0
            } else {
                val prev = points[i - 1]
                SketchPoint(
                    x = (prev.x + p0.x) / 2f,
                    y = (prev.y + p0.y) / 2f,
                    pressure = ((prev.pressure ?: 0.5f) + (p0.pressure ?: 0.5f)) / 2f
                )
            }
            val nextMid = if (i == points.size - 2) {
                p1
            } else {
                SketchPoint(
                    x = (p0.x + p1.x) / 2f,
                    y = (p0.y + p1.y) / 2f,
                    pressure = ((p0.pressure ?: 0.5f) + (p1.pressure ?: 0.5f)) / 2f
                )
            }

            val approxLen = hypot((p0.x - prevMid.x).toDouble(), (p0.y - prevMid.y).toDouble()) +
                hypot((nextMid.x - p0.x).toDouble(), (nextMid.y - p0.y).toDouble())
            if (approxLen < 0.01) continue

            val steps = max(1, ceil(approxLen / stepDist.toDouble()).toInt())
            val prStart = prevMid.pressure ?: 0.5f
            val prCtrl = p0.pressure ?: 0.5f
            val prEnd = nextMid.pressure ?: 0.5f

            for (s in 1..steps) {
                val t = s.toFloat() / steps.toFloat()
                val omt = 1f - t
                sampled.add(
                    SketchPoint(
                        x = omt * omt * prevMid.x + 2f * omt * t * p0.x + t * t * nextMid.x,
                        y = omt * omt * prevMid.y + 2f * omt * t * p0.y + t * t * nextMid.y,
                        pressure = omt * omt * prStart + 2f * omt * t * prCtrl + t * t * prEnd
                    )
                )
            }
        }
        return sampled
    }

    private fun renderVariableWidthRibbon(
        canvas: Canvas,
        pts: List<SketchPoint>,
        baseSize: Float,
        colorArgb: Int,
        opacity: Float,
        isEraser: Boolean
    ) {
        val sampled = sampleSmoothQuadraticPoints(pts, max(1.5f, baseSize * 0.16f))
        if (sampled.isEmpty()) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (isEraser) Color.BLACK else colorArgb
            alpha = (opacity * 255f).toInt().coerceIn(0, 255)
            if (isEraser) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            }
        }

        if (sampled.size == 1) {
            val r = computePressureStrokeWidth(baseSize, sampled[0].pressure ?: 0.5f, true) / 2f
            canvas.drawCircle(sampled[0].x, sampled[0].y, r, paint)
            return
        }

        val leftEdge = ArrayList<SketchPoint>(sampled.size)
        val rightEdge = ArrayList<SketchPoint>(sampled.size)

        for (i in sampled.indices) {
            val prev = sampled[max(0, i - 1)]
            val next = sampled[min(sampled.size - 1, i + 1)]
            val dx = next.x - prev.x
            val dy = next.y - prev.y
            val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().let { if (it == 0f) 1f else it }
            val nx = -dy / len
            val ny = dx / len
            val r = computePressureStrokeWidth(baseSize, sampled[i].pressure ?: 0.5f, true) / 2f
            leftEdge.add(SketchPoint(sampled[i].x + nx * r, sampled[i].y + ny * r))
            rightEdge.add(SketchPoint(sampled[i].x - nx * r, sampled[i].y - ny * r))
        }

        val path = Path()
        path.moveTo(leftEdge[0].x, leftEdge[0].y)
        for (i in 1 until leftEdge.size) {
            path.lineTo(leftEdge[i].x, leftEdge[i].y)
        }
        for (i in rightEdge.indices.reversed()) {
            path.lineTo(rightEdge[i].x, rightEdge[i].y)
        }
        path.close()
        canvas.drawPath(path, paint)

        val first = sampled.first()
        val last = sampled.last()
        val rFirst = computePressureStrokeWidth(baseSize, first.pressure ?: 0.5f, true) / 2f
        val rLast = computePressureStrokeWidth(baseSize, last.pressure ?: 0.5f, true) / 2f
        canvas.drawCircle(first.x, first.y, rFirst, paint)
        canvas.drawCircle(last.x, last.y, rLast, paint)
    }

    private fun renderGeometricOrBasicStroke(canvas: Canvas, stroke: Stroke) {
        val pts = stroke.points
        if (pts.isEmpty()) return

        val isEraser = stroke.tool == ToolType.ERASER
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = stroke.size
            style = Paint.Style.STROKE
            color = if (isEraser) Color.BLACK else stroke.colorArgb
            alpha = (stroke.opacity * 255f).toInt().coerceIn(0, 255)
            if (isEraser) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            }
        }

        when (stroke.tool) {
            ToolType.LINE -> {
                if (pts.size >= 2) {
                    val start = pts.first()
                    val end = pts.last()
                    canvas.drawLine(start.x, start.y, end.x, end.y, paint)
                }
            }
            ToolType.RECT -> {
                if (pts.size >= 2) {
                    val start = pts.first()
                    val end = pts.last()
                    val cx = (start.x + end.x) / 2f
                    val cy = (start.y + end.y) / 2f
                    canvas.save()
                    if (stroke.rotation != 0f) {
                        val deg = Math.toDegrees(stroke.rotation.toDouble()).toFloat()
                        canvas.rotate(deg, cx, cy)
                    }
                    val left = min(start.x, end.x)
                    val top = min(start.y, end.y)
                    val right = max(start.x, end.x)
                    val bottom = max(start.y, end.y)
                    canvas.drawRect(left, top, right, bottom, paint)
                    canvas.restore()
                }
            }
            ToolType.CIRCLE -> {
                if (pts.size >= 2) {
                    val start = pts.first()
                    val end = pts.last()
                    val rx = max(1f, abs(end.x - start.x) / 2f)
                    val ry = max(1f, abs(end.y - start.y) / 2f)
                    val cx = (start.x + end.x) / 2f
                    val cy = (start.y + end.y) / 2f
                    canvas.save()
                    if (stroke.rotation != 0f) {
                        val deg = Math.toDegrees(stroke.rotation.toDouble()).toFloat()
                        canvas.rotate(deg, cx, cy)
                    }
                    val rect = RectF(cx - rx, cy - ry, cx + rx, cy + ry)
                    canvas.drawOval(rect, paint)
                    canvas.restore()
                }
            }
            else -> {
                val isZeroLength = pts.all {
                    hypot((it.x - pts[0].x).toDouble(), (it.y - pts[0].y).toDouble()) < 0.25
                }
                if (pts.size == 1 || isZeroLength) {
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(pts[0].x, pts[0].y, stroke.size / 2f, paint)
                } else {
                    val path = Path()
                    path.moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size - 1) {
                        val midX = (pts[i].x + pts[i + 1].x) / 2f
                        val midY = (pts[i].y + pts[i + 1].y) / 2f
                        path.quadTo(pts[i].x, pts[i].y, midX, midY)
                    }
                    val last = pts.last()
                    path.lineTo(last.x, last.y)
                    canvas.drawPath(path, paint)
                }
            }
        }
    }

    private fun renderSingleBrushPath(canvas: Canvas, stroke: Stroke) {
        val pts = stroke.points
        if (pts.isEmpty()) return

        val pressureAllowed = stroke.pressureSensitive
        val hasStylusPressure = pressureAllowed && hasVariablePressure(pts)

        if (stroke.tool == ToolType.LINE || stroke.tool == ToolType.RECT || stroke.tool == ToolType.CIRCLE) {
            renderGeometricOrBasicStroke(canvas, stroke.copy(symmetry = SymmetryMode.NONE))
            return
        }

        if (stroke.tool == ToolType.HIGHLIGHTER || stroke.tool == ToolType.ERASER) {
            if (!hasStylusPressure) {
                renderGeometricOrBasicStroke(canvas, stroke.copy(symmetry = SymmetryMode.NONE))
                return
            }
            val isEraser = stroke.tool == ToolType.ERASER
            renderVariableWidthRibbon(
                canvas = canvas,
                pts = pts,
                baseSize = stroke.size,
                colorArgb = if (isEraser) Color.BLACK else stroke.colorArgb,
                opacity = stroke.opacity,
                isEraser = isEraser
            )
            return
        }

        val kind = BrushKind.fromId(stroke.brush)

        if (kind == BrushKind.SOFT_AIRBRUSH) {
            val spacing = max(1.5f, stroke.size * 0.35f)
            val sampled = interpolateStrokePoints(pts, spacing)
            for (pt in sampled) {
                val pressure = if (pressureAllowed && (pt.pressure ?: 0f) > 0f) pt.pressure!! else 0.65f
                val radius = max(1.5f, stroke.size * (0.45f + pressure * 1.15f))
                renderAirbrushStamp(
                    canvas = canvas,
                    x = pt.x,
                    y = pt.y,
                    radius = radius,
                    colorArgb = stroke.colorArgb,
                    opacity = stroke.opacity * (0.35f + pressure * 0.5f)
                )
            }
            return
        }

        if (kind == BrushKind.HB_PENCIL) {
            val grain = getCachedPencilGrainBitmap(stroke.colorArgb, 24)
            val spacing = max(1f, stroke.size * 0.22f)
            val sampled = interpolateStrokePoints(pts, spacing)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val matrix = Matrix()

            for (i in sampled.indices) {
                val pt = sampled[i]
                val pressure = if (pressureAllowed && (pt.pressure ?: 0f) > 0f) pt.pressure!! else 0.65f
                val stampSize = max(1.5f, computePressureStrokeWidth(stroke.size, pressure, pressureAllowed))
                val angleDeg = Math.toDegrees(((pt.x * 0.17 + pt.y * 0.29 + i * 0.61) % (PI * 2.0))).toFloat()
                val hash = sin((pt.x * 12.9898 + pt.y * 78.233 + i) * 43758.5453)
                val scatter = ((hash - floor(hash) - 0.5) * stroke.size * 0.22 * (0.4 + pressure)).toFloat()

                paint.alpha = (StrokeMath.clamp(stroke.opacity * (0.3f + pressure * 0.55f), 0.15f, 0.92f) * 255f).toInt()
                val scale = stampSize / grain.width.toFloat()
                matrix.reset()
                matrix.postTranslate(-grain.width / 2f, -grain.height / 2f)
                matrix.postScale(scale, scale)
                matrix.postRotate(angleDeg)
                matrix.postTranslate(pt.x + scatter, pt.y - scatter * 0.6f)
                canvas.drawBitmap(grain, matrix, paint)
            }
            return
        }

        // Hard Round ('Ink')
        val isZeroLength = pts.all {
            hypot((it.x - pts[0].x).toDouble(), (it.y - pts[0].y).toDouble()) < 0.25
        }
        if (pts.size == 1 || isZeroLength) {
            val p = pts[0]
            val pressure = if (pressureAllowed && p.pressure != null && p.pressure > 0f && p.pressure != 0.5f) {
                p.pressure
            } else {
                0.6f
            }
            val radius = max(0.5f, computePressureStrokeWidth(stroke.size, pressure, pressureAllowed) / 2f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = stroke.colorArgb
                alpha = (stroke.opacity * 255f).toInt().coerceIn(0, 255)
            }
            canvas.drawCircle(p.x, p.y, radius, paint)
            return
        }

        if (!hasStylusPressure) {
            renderGeometricOrBasicStroke(canvas, stroke.copy(symmetry = SymmetryMode.NONE))
            return
        }

        renderVariableWidthRibbon(
            canvas = canvas,
            pts = pts,
            baseSize = stroke.size,
            colorArgb = stroke.colorArgb,
            opacity = stroke.opacity,
            isEraser = false
        )
    }

    fun renderBrushStrokeToCanvas(
        canvas: Canvas,
        stroke: Stroke,
        defaultSymmetryCenter: SketchPoint = SketchPoint(600f, 380f)
    ) {
        if (stroke.points.isEmpty()) return
        val sym = stroke.symmetry
        if (sym != SymmetryMode.NONE) {
            val activeCenter = stroke.symmetryCenter ?: defaultSymmetryCenter
            val transformedPaths = mutableListOf<MutableList<SketchPoint>>()
            for (pt in stroke.points) {
                val copies = StrokeMath.getSymmetryTransforms(pt, sym, activeCenter)
                copies.forEachIndexed { idx, copy ->
                    while (transformedPaths.size <= idx) {
                        transformedPaths.add(mutableListOf())
                    }
                    transformedPaths[idx].add(copy)
                }
            }
            for (pathPts in transformedPaths) {
                renderSingleBrushPath(canvas, stroke.copy(points = pathPts, symmetry = SymmetryMode.NONE))
            }
            return
        }
        renderSingleBrushPath(canvas, stroke)
    }

    fun renderBrushPreviewToCanvas(
        canvas: Canvas,
        brushId: String,
        width: Float,
        height: Float,
        colorArgb: Int
    ) {
        val kind = BrushKind.fromId(brushId)
        val pts = generateThumbnailStrokePoints(width, height, 28)

        when (kind) {
            BrushKind.SOFT_AIRBRUSH -> {
                for (p in pts) {
                    val pressure = p.pressure ?: 0.7f
                    val radius = 3.5f + pressure * 3.5f
                    renderAirbrushStamp(canvas, p.x, p.y, radius, colorArgb, 0.55f + pressure * 0.3f)
                }
            }
            BrushKind.HB_PENCIL -> {
                val grain = getCachedPencilGrainBitmap(colorArgb, 16)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                val matrix = Matrix()
                for (i in pts.indices) {
                    val p = pts[i]
                    val pressure = p.pressure ?: 0.7f
                    val stampSize = 2.6f + pressure * 1.4f
                    val angleDeg = Math.toDegrees((i * 0.65) % (PI * 2.0)).toFloat()
                    val scatter = ((i % 3) - 1) * 0.45f
                    paint.alpha = (StrokeMath.clamp(0.35f + pressure * 0.45f, 0.2f, 0.85f) * 255f).toInt()
                    val scale = stampSize / grain.width.toFloat()
                    matrix.reset()
                    matrix.postTranslate(-grain.width / 2f, -grain.height / 2f)
                    matrix.postScale(scale, scale)
                    matrix.postRotate(angleDeg)
                    matrix.postTranslate(p.x + scatter, p.y - scatter * 0.6f)
                    canvas.drawBitmap(grain, matrix, paint)
                }
            }
            BrushKind.HARD_ROUND -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    color = colorArgb
                }
                for (i in 0 until pts.size - 1) {
                    val p0 = pts[i]
                    val p1 = pts[i + 1]
                    val avgPressure = ((p0.pressure ?: 0.7f) + (p1.pressure ?: 0.7f)) / 2f
                    paint.strokeWidth = 1.2f + avgPressure * 2.6f
                    paint.alpha = (StrokeMath.clamp(0.8f + avgPressure * 0.2f, 0.75f, 1f) * 255f).toInt()
                    canvas.drawLine(p0.x, p0.y, p1.x, p1.y, paint)
                }
            }
        }
    }
}
