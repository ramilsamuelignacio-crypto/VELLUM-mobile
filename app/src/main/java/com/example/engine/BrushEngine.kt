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
import kotlin.math.cos
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

    private val charcoalCache = object : LinkedHashMap<String, Bitmap>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean {
            return size > 32
        }
    }

    private val markerNibCache = object : LinkedHashMap<String, Bitmap>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean {
            return size > 32
        }
    }

    private val watercolorCache = object : LinkedHashMap<String, Bitmap>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean {
            return size > 32
        }
    }

    private val airbrushCache = object : LinkedHashMap<String, Bitmap>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean {
            return size > 32
        }
    }

    private val threadScratch = ThreadLocal.withInitial { RenderScratch() }

    private class RenderScratch {
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val path = Path()
        val matrix = Matrix()
        val rect = RectF()
        val dstOutXfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }

    fun hasVariablePressure(points: List<SketchPoint>): Boolean {
        for (i in points.indices) {
            val p = points[i].pressure
            if (p != null && p > 0f && abs(p - 0.5f) > 0.02f) {
                return true
            }
        }
        return false
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
        val stepDist = max(0.75f, spacing)
        val result = ArrayList<SketchPoint>(points.size * 2)
        result.add(points[0])

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val dist = hypot((p1.x - p0.x).toDouble(), (p1.y - p0.y).toDouble()).toFloat()
            if (dist < 0.05f) continue

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
        val key = "$colorArgb:$size"
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

    @Synchronized
    fun getCachedAirbrushStampBitmap(colorArgb: Int, size: Int = 64): Bitmap {
        val key = "$colorArgb:$size"
        airbrushCache[key]?.let { return it }

        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val stampCanvas = Canvas(bmp)
        val cr = Color.red(colorArgb)
        val cg = Color.green(colorArgb)
        val cb = Color.blue(colorArgb)
        val center = size / 2f
        val shader = RadialGradient(
            center,
            center,
            center,
            intArrayOf(
                Color.argb(255, cr, cg, cb),
                Color.argb(115, cr, cg, cb),
                Color.argb(0, cr, cg, cb)
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.shader = shader
        }
        stampCanvas.drawCircle(center, center, center, p)
        airbrushCache[key] = bmp
        return bmp
    }

    @Synchronized
    fun getCachedCharcoalGrainBitmap(colorArgb: Int, size: Int = 32): Bitmap {
        val key = "$colorArgb:$size"
        charcoalCache[key]?.let { return it }

        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val r = Color.red(colorArgb)
        val g = Color.green(colorArgb)
        val b = Color.blue(colorArgb)
        val center = size / 2f
        val pixels = IntArray(size * size)

        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = (x - center) / center
                val dy = (y - center) / center
                val angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble())
                // Slightly irregular organic perimeter
                val edgeWobble = 0.84f + 0.14f * sin(angle * 3.0 + 0.7).toFloat() +
                    0.08f * cos(angle * 5.0 - 1.1).toFloat()
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat() / edgeWobble
                if (dist > 1f) continue

                // Coarse paper tooth + fine charcoal crumbles
                val coarseX = floor(x * 0.5).toInt()
                val coarseY = floor(y * 0.5).toInt()
                val coarseHash = sin((coarseX * 19.31 + coarseY * 47.17) * 12345.67)
                val coarse = coarseHash - floor(coarseHash)

                val fineHash = sin((x * 12.9898 + y * 78.233) * 43758.5453)
                val fine = fineHash - floor(fineHash)

                val tooth = coarse * 0.55 + fine * 0.45
                val radialFade = (1f - dist * dist).coerceIn(0f, 1f)
                val alphaVal = if (tooth > 0.44) {
                    (radialFade * (165f + (tooth.toFloat() - 0.44f) * 160f)).toInt().coerceIn(0, 255)
                } else {
                    (radialFade * 26f).toInt().coerceIn(0, 255)
                }
                pixels[y * size + x] = Color.argb(alphaVal, r, g, b)
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        charcoalCache[key] = bmp
        return bmp
    }

    @Synchronized
    fun getCachedMarkerNibBitmap(colorArgb: Int, width: Int = 32, height: Int = 12): Bitmap {
        val key = "$colorArgb:${width}x$height"
        markerNibCache[key]?.let { return it }

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val r = Color.red(colorArgb)
        val g = Color.green(colorArgb)
        val b = Color.blue(colorArgb)
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val vEdge = 1f - abs((y - (height - 1) * 0.5f) / (height * 0.5f))
            val yMask = (vEdge * 2.2f).coerceIn(0f, 1f)
            for (x in 0 until width) {
                val hEdge = 1f - abs((x - (width - 1) * 0.5f) / (width * 0.5f))
                val xMask = (hEdge * 4.5f).coerceIn(0f, 1f)
                // Felt-tip fiber grooves along the nib
                val fiberHash = sin(x * 2.35 + 1.1)
                val fiber = 0.86f + 0.14f * fiberHash.toFloat()
                val alphaVal = (255f * xMask * yMask * fiber).toInt().coerceIn(0, 255)
                pixels[y * width + x] = Color.argb(alphaVal, r, g, b)
            }
        }
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        markerNibCache[key] = bmp
        return bmp
    }

    @Synchronized
    fun getCachedWatercolorStampBitmap(colorArgb: Int, size: Int = 64): Bitmap {
        val key = "$colorArgb:$size"
        watercolorCache[key]?.let { return it }

        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val r = Color.red(colorArgb)
        val g = Color.green(colorArgb)
        val b = Color.blue(colorArgb)
        val center = size / 2f
        val pixels = IntArray(size * size)

        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = (x - center) / center
                val dy = (y - center) / center
                val angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble())
                val edgeWobble = 0.86f + 0.09f * sin(angle * 4.0 + 0.4).toFloat() +
                    0.05f * cos(angle * 7.0 - 0.9).toFloat()
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat() / edgeWobble
                if (dist > 1f) continue

                // Watercolor wet bloom: gentle core + slightly richer outer ring before feathered drop-off
                val ring = kotlin.math.exp(-((dist - 0.76f) * (dist - 0.76f)) / 0.025f)
                val core = (1f - dist * 0.42f).coerceIn(0f, 1f) * 0.52f
                val feather = ((1f - dist) * 5.2f).coerceIn(0f, 1f)
                val noiseHash = sin((x * 7.13 + y * 13.71) * 9157.31)
                val granulation = 0.84f + 0.24f * (noiseHash - floor(noiseHash)).toFloat()
                val intensity = ((core + ring * 0.58f) * feather * granulation).coerceIn(0f, 1f)
                val alphaVal = (intensity * 215f).toInt().coerceIn(0, 255)
                pixels[y * size + x] = Color.argb(alphaVal, r, g, b)
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        watercolorCache[key] = bmp
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
        val stamp = getCachedAirbrushStampBitmap(colorArgb, 64)
        val effectiveAlpha = StrokeMath.clamp(opacity * 0.25f, 0.02f, 0.4f)
        val scratch = threadScratch.get()!!
        val paint = scratch.bitmapPaint
        val matrix = scratch.matrix

        paint.alpha = (effectiveAlpha * 255f).toInt().coerceIn(0, 255)
        val scale = (r * 2f) / stamp.width.toFloat()
        matrix.reset()
        matrix.postTranslate(-stamp.width / 2f, -stamp.height / 2f)
        matrix.postScale(scale, scale)
        matrix.postTranslate(x, y)
        canvas.drawBitmap(stamp, matrix, paint)
    }

    private fun sampleSmoothQuadraticPoints(
        points: List<SketchPoint>,
        spacing: Float = 2f
    ): List<SketchPoint> {
        if (points.size <= 2) return interpolateStrokePoints(points, spacing)
        val stepDist = max(1.25f, spacing)
        val sampled = ArrayList<SketchPoint>(points.size * 2)
        sampled.add(points[0].copy(pressure = points[0].pressure ?: 0.5f))

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val prevMidX: Float
            val prevMidY: Float
            val prevMidP: Float
            if (i == 0) {
                prevMidX = p0.x
                prevMidY = p0.y
                prevMidP = p0.pressure ?: 0.5f
            } else {
                val prev = points[i - 1]
                prevMidX = (prev.x + p0.x) * 0.5f
                prevMidY = (prev.y + p0.y) * 0.5f
                prevMidP = ((prev.pressure ?: 0.5f) + (p0.pressure ?: 0.5f)) * 0.5f
            }

            val nextMidX: Float
            val nextMidY: Float
            val nextMidP: Float
            if (i == points.size - 2) {
                nextMidX = p1.x
                nextMidY = p1.y
                nextMidP = p1.pressure ?: 0.5f
            } else {
                nextMidX = (p0.x + p1.x) * 0.5f
                nextMidY = (p0.y + p1.y) * 0.5f
                nextMidP = ((p0.pressure ?: 0.5f) + (p1.pressure ?: 0.5f)) * 0.5f
            }

            val approxLen = hypot((p0.x - prevMidX).toDouble(), (p0.y - prevMidY).toDouble()) +
                hypot((nextMidX - p0.x).toDouble(), (nextMidY - p0.y).toDouble())
            if (approxLen < 0.05) continue

            val steps = max(1, ceil(approxLen / stepDist.toDouble()).toInt())
            val prCtrl = p0.pressure ?: 0.5f

            for (s in 1..steps) {
                val t = s.toFloat() / steps.toFloat()
                val omt = 1f - t
                val omt2 = omt * omt
                val twoOmtT = 2f * omt * t
                val t2 = t * t
                sampled.add(
                    SketchPoint(
                        x = omt2 * prevMidX + twoOmtT * p0.x + t2 * nextMidX,
                        y = omt2 * prevMidY + twoOmtT * p0.y + t2 * nextMidY,
                        pressure = omt2 * prevMidP + twoOmtT * prCtrl + t2 * nextMidP
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
        val sampled = sampleSmoothQuadraticPoints(pts, max(2.2f, baseSize * 0.22f))
        if (sampled.isEmpty()) return

        val scratch = threadScratch.get()!!
        val paint = scratch.strokePaint.apply {
            reset()
            isAntiAlias = true
            style = Paint.Style.FILL
            color = if (isEraser) Color.BLACK else colorArgb
            alpha = (opacity * 255f).toInt().coerceIn(0, 255)
            xfermode = if (isEraser) scratch.dstOutXfermode else null
        }

        if (sampled.size == 1) {
            val r = computePressureStrokeWidth(baseSize, sampled[0].pressure ?: 0.5f, true) * 0.5f
            canvas.drawCircle(sampled[0].x, sampled[0].y, r, paint)
            return
        }

        val count = sampled.size
        val leftX = FloatArray(count)
        val leftY = FloatArray(count)
        val rightX = FloatArray(count)
        val rightY = FloatArray(count)

        for (i in 0 until count) {
            val prev = sampled[max(0, i - 1)]
            val next = sampled[min(count - 1, i + 1)]
            val dx = next.x - prev.x
            val dy = next.y - prev.y
            val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().let { if (it == 0f) 1f else it }
            val nx = -dy / len
            val ny = dx / len
            val r = computePressureStrokeWidth(baseSize, sampled[i].pressure ?: 0.5f, true) * 0.5f
            val sx = sampled[i].x
            val sy = sampled[i].y
            leftX[i] = sx + nx * r
            leftY[i] = sy + ny * r
            rightX[i] = sx - nx * r
            rightY[i] = sy - ny * r
        }

        val path = scratch.path
        path.reset()
        path.moveTo(leftX[0], leftY[0])
        for (i in 1 until count) {
            path.lineTo(leftX[i], leftY[i])
        }
        for (i in count - 1 downTo 0) {
            path.lineTo(rightX[i], rightY[i])
        }
        path.close()
        canvas.drawPath(path, paint)

        val first = sampled[0]
        val last = sampled[count - 1]
        val rFirst = computePressureStrokeWidth(baseSize, first.pressure ?: 0.5f, true) * 0.5f
        val rLast = computePressureStrokeWidth(baseSize, last.pressure ?: 0.5f, true) * 0.5f
        canvas.drawCircle(first.x, first.y, rFirst, paint)
        canvas.drawCircle(last.x, last.y, rLast, paint)
    }

    private fun renderCalligraphyRibbon(
        canvas: Canvas,
        pts: List<SketchPoint>,
        baseSize: Float,
        colorArgb: Int,
        opacity: Float,
        usePressure: Boolean
    ) {
        val sampled = sampleSmoothQuadraticPoints(pts, max(1.5f, baseSize * 0.18f))
        if (sampled.isEmpty()) return

        val scratch = threadScratch.get()!!
        val paint = scratch.strokePaint.apply {
            reset()
            isAntiAlias = true
            style = Paint.Style.FILL
            color = colorArgb
            alpha = (opacity * 255f).toInt().coerceIn(0, 255)
        }

        // Classic -38 degree italic broad-nib vector
        val nibAngleRad = Math.toRadians(-38.0)
        val nibCos = cos(nibAngleRad).toFloat()
        val nibSin = sin(nibAngleRad).toFloat()
        val perpX = -nibSin
        val perpY = nibCos

        if (sampled.size == 1) {
            val pt = sampled[0]
            val p = if (usePressure && (pt.pressure ?: 0f) > 0f) pt.pressure!! else 0.65f
            val w = computePressureStrokeWidth(baseSize, p, usePressure)
            val halfNib = max(1.2f, w * 0.72f)
            val halfThick = max(0.55f, w * 0.14f)
            val path = scratch.path
            path.reset()
            path.moveTo(pt.x - nibCos * halfNib - perpX * halfThick, pt.y - nibSin * halfNib - perpY * halfThick)
            path.lineTo(pt.x + nibCos * halfNib - perpX * halfThick, pt.y + nibSin * halfNib - perpY * halfThick)
            path.lineTo(pt.x + nibCos * halfNib + perpX * halfThick, pt.y + nibSin * halfNib + perpY * halfThick)
            path.lineTo(pt.x - nibCos * halfNib + perpX * halfThick, pt.y - nibSin * halfNib + perpY * halfThick)
            path.close()
            canvas.drawPath(path, paint)
            return
        }

        // Stamp overlapping crisp nib quads along the smooth curve so thick/thin transitions and sharp turns stay watertight
        val path = scratch.path
        for (i in 0 until sampled.size - 1) {
            val a = sampled[i]
            val b = sampled[i + 1]
            val pa = if (usePressure && (a.pressure ?: 0f) > 0f) a.pressure!! else 0.65f
            val pb = if (usePressure && (b.pressure ?: 0f) > 0f) b.pressure!! else 0.65f
            val ha = max(1.0f, computePressureStrokeWidth(baseSize, pa, usePressure) * 0.72f)
            val hb = max(1.0f, computePressureStrokeWidth(baseSize, pb, usePressure) * 0.72f)
            val ta = max(0.45f, ha * 0.16f)
            val tb = max(0.45f, hb * 0.16f)

            val ax1 = a.x - nibCos * ha
            val ay1 = a.y - nibSin * ha
            val ax2 = a.x + nibCos * ha
            val ay2 = a.y + nibSin * ha
            val bx1 = b.x - nibCos * hb
            val by1 = b.y - nibSin * hb
            val bx2 = b.x + nibCos * hb
            val by2 = b.y + nibSin * hb

            path.reset()
            path.moveTo(ax1 - perpX * ta, ay1 - perpY * ta)
            path.lineTo(ax2 + perpX * ta, ay2 + perpY * ta)
            path.lineTo(bx2 + perpX * tb, by2 + perpY * tb)
            path.lineTo(bx1 - perpX * tb, by1 - perpY * tb)
            path.close()
            canvas.drawPath(path, paint)
        }
    }

    private fun renderGeometricOrBasicStroke(canvas: Canvas, stroke: Stroke) {
        val pts = stroke.points
        if (pts.isEmpty()) return

        val scratch = threadScratch.get()!!
        val isEraser = stroke.tool == ToolType.ERASER
        val paint = scratch.strokePaint.apply {
            reset()
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = stroke.size
            style = Paint.Style.STROKE
            color = if (isEraser) Color.BLACK else stroke.colorArgb
            alpha = (stroke.opacity * 255f).toInt().coerceIn(0, 255)
            xfermode = if (isEraser) scratch.dstOutXfermode else null
        }

        when (stroke.tool) {
            ToolType.LINE -> {
                if (pts.size >= 2) {
                    val start = pts[0]
                    val end = pts[pts.size - 1]
                    canvas.drawLine(start.x, start.y, end.x, end.y, paint)
                }
            }
            ToolType.RECT -> {
                if (pts.size >= 2) {
                    val start = pts[0]
                    val end = pts[pts.size - 1]
                    val cx = (start.x + end.x) * 0.5f
                    val cy = (start.y + end.y) * 0.5f
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
                    val start = pts[0]
                    val end = pts[pts.size - 1]
                    val rx = max(1f, abs(end.x - start.x) * 0.5f)
                    val ry = max(1f, abs(end.y - start.y) * 0.5f)
                    val cx = (start.x + end.x) * 0.5f
                    val cy = (start.y + end.y) * 0.5f
                    canvas.save()
                    if (stroke.rotation != 0f) {
                        val deg = Math.toDegrees(stroke.rotation.toDouble()).toFloat()
                        canvas.rotate(deg, cx, cy)
                    }
                    scratch.rect.set(cx - rx, cy - ry, cx + rx, cy + ry)
                    canvas.drawOval(scratch.rect, paint)
                    canvas.restore()
                }
            }
            else -> {
                val first = pts[0]
                var isZeroLength = true
                for (i in 1 until pts.size) {
                    val p = pts[i]
                    if (hypot((p.x - first.x).toDouble(), (p.y - first.y).toDouble()) >= 0.25) {
                        isZeroLength = false
                        break
                    }
                }
                if (pts.size == 1 || isZeroLength) {
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(first.x, first.y, stroke.size * 0.5f, paint)
                } else {
                    val path = scratch.path
                    path.reset()
                    path.moveTo(first.x, first.y)
                    for (i in 1 until pts.size - 1) {
                        val pi = pts[i]
                        val pNext = pts[i + 1]
                        val midX = (pi.x + pNext.x) * 0.5f
                        val midY = (pi.y + pNext.y) * 0.5f
                        path.quadTo(pi.x, pi.y, midX, midY)
                    }
                    val last = pts[pts.size - 1]
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

        if (kind == BrushKind.CALLIGRAPHY) {
            renderCalligraphyRibbon(
                canvas = canvas,
                pts = pts,
                baseSize = stroke.size,
                colorArgb = stroke.colorArgb,
                opacity = stroke.opacity,
                usePressure = pressureAllowed
            )
            return
        }

        if (kind == BrushKind.CHISEL_MARKER) {
            val nib = getCachedMarkerNibBitmap(stroke.colorArgb, 32, 12)
            val spacing = max(1.1f, stroke.size * 0.16f)
            val sampled = sampleSmoothQuadraticPoints(pts, spacing)
            val scratch = threadScratch.get()!!
            val paint = scratch.bitmapPaint
            val matrix = scratch.matrix
            val nibAngleDeg = -32f

            for (i in sampled.indices) {
                val pt = sampled[i]
                val pressure = if (pressureAllowed && (pt.pressure ?: 0f) > 0f) pt.pressure!! else 0.65f
                val nibWidthPx = max(3f, computePressureStrokeWidth(stroke.size, pressure, pressureAllowed) * 1.45f)
                val nibHeightPx = max(1.8f, nibWidthPx * 0.34f)
                // Slight ink pooling on initial touch & lift
                val isEndPool = i < 2 || i >= sampled.size - 2
                val baseAlpha = StrokeMath.clamp(stroke.opacity * (if (isEndPool) 0.28f else 0.20f), 0.03f, 0.42f)
                paint.alpha = (baseAlpha * 255f).toInt().coerceIn(0, 255)
                val scaleX = nibWidthPx / nib.width.toFloat()
                val scaleY = nibHeightPx / nib.height.toFloat()
                matrix.reset()
                matrix.postTranslate(-nib.width * 0.5f, -nib.height * 0.5f)
                matrix.postScale(scaleX, scaleY)
                matrix.postRotate(nibAngleDeg)
                matrix.postTranslate(pt.x, pt.y)
                canvas.drawBitmap(nib, matrix, paint)
            }
            return
        }

        if (kind == BrushKind.CHARCOAL) {
            val grain = getCachedCharcoalGrainBitmap(stroke.colorArgb, 32)
            val spacing = max(1.4f, stroke.size * 0.25f)
            val sampled = interpolateStrokePoints(pts, spacing)
            val scratch = threadScratch.get()!!
            val paint = scratch.bitmapPaint
            val matrix = scratch.matrix

            for (i in sampled.indices) {
                val pt = sampled[i]
                val pressure = if (pressureAllowed && (pt.pressure ?: 0f) > 0f) pt.pressure!! else 0.65f
                val stampSize = max(3f, computePressureStrokeWidth(stroke.size, pressure, pressureAllowed) * 1.42f)
                val angleDeg = Math.toDegrees(((pt.x * 0.23 + pt.y * 0.37 + i * 0.89) % (PI * 2.0))).toFloat()
                val hash = sin((pt.x * 17.13 + pt.y * 59.71 + i) * 31415.92)
                val scatter = ((hash - floor(hash) - 0.5) * stroke.size * 0.32 * (0.45 + pressure)).toFloat()

                paint.alpha = (StrokeMath.clamp(stroke.opacity * (0.28f + pressure * 0.52f), 0.05f, 0.88f) * 255f).toInt()
                val scale = stampSize / grain.width.toFloat()
                matrix.reset()
                matrix.postTranslate(-grain.width * 0.5f, -grain.height * 0.5f)
                matrix.postScale(scale, scale)
                matrix.postRotate(angleDeg)
                matrix.postTranslate(pt.x + scatter, pt.y - scatter * 0.55f)
                canvas.drawBitmap(grain, matrix, paint)
            }
            return
        }

        if (kind == BrushKind.WATERCOLOR) {
            val stamp = getCachedWatercolorStampBitmap(stroke.colorArgb, 64)
            val spacing = max(2.2f, stroke.size * 0.28f)
            val sampled = sampleSmoothQuadraticPoints(pts, spacing)
            val scratch = threadScratch.get()!!
            val paint = scratch.bitmapPaint
            val matrix = scratch.matrix

            for (i in sampled.indices) {
                val pt = sampled[i]
                val pressure = if (pressureAllowed && (pt.pressure ?: 0f) > 0f) pt.pressure!! else 0.65f
                val diameter = max(5f, computePressureStrokeWidth(stroke.size, pressure, pressureAllowed) * 1.85f)
                val angleDeg = Math.toDegrees(((i * 0.77 + pt.x * 0.09) % (PI * 2.0))).toFloat()
                val wobbleHash = sin((pt.x * 9.7 + pt.y * 15.3 + i) * 4375.54)
                val wobble = ((wobbleHash - floor(wobbleHash) - 0.5) * stroke.size * 0.16).toFloat()

                val alpha = StrokeMath.clamp(stroke.opacity * (0.14f + pressure * 0.16f), 0.03f, 0.36f)
                paint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
                val scale = diameter / stamp.width.toFloat()
                matrix.reset()
                matrix.postTranslate(-stamp.width * 0.5f, -stamp.height * 0.5f)
                matrix.postScale(scale, scale)
                matrix.postRotate(angleDeg)
                matrix.postTranslate(pt.x + wobble, pt.y - wobble * 0.7f)
                canvas.drawBitmap(stamp, matrix, paint)
            }
            return
        }

        if (kind == BrushKind.SOFT_AIRBRUSH) {
            val stamp = getCachedAirbrushStampBitmap(stroke.colorArgb, 64)
            val spacing = max(2f, stroke.size * 0.35f)
            val sampled = interpolateStrokePoints(pts, spacing)
            val scratch = threadScratch.get()!!
            val paint = scratch.bitmapPaint
            val matrix = scratch.matrix

            for (i in sampled.indices) {
                val pt = sampled[i]
                val pressure = if (pressureAllowed && (pt.pressure ?: 0f) > 0f) pt.pressure!! else 0.65f
                val radius = max(1.5f, stroke.size * (0.45f + pressure * 1.15f))
                val effectiveAlpha = StrokeMath.clamp(stroke.opacity * (0.35f + pressure * 0.5f) * 0.25f, 0.02f, 0.4f)
                paint.alpha = (effectiveAlpha * 255f).toInt().coerceIn(0, 255)
                val scale = (radius * 2f) / stamp.width.toFloat()
                matrix.reset()
                matrix.postTranslate(-stamp.width * 0.5f, -stamp.height * 0.5f)
                matrix.postScale(scale, scale)
                matrix.postTranslate(pt.x, pt.y)
                canvas.drawBitmap(stamp, matrix, paint)
            }
            return
        }

        if (kind == BrushKind.HB_PENCIL) {
            val grain = getCachedPencilGrainBitmap(stroke.colorArgb, 24)
            val spacing = max(1.25f, stroke.size * 0.24f)
            val sampled = interpolateStrokePoints(pts, spacing)
            val scratch = threadScratch.get()!!
            val paint = scratch.bitmapPaint
            val matrix = scratch.matrix

            for (i in sampled.indices) {
                val pt = sampled[i]
                val pressure = if (pressureAllowed && (pt.pressure ?: 0f) > 0f) pt.pressure!! else 0.65f
                val stampSize = max(1.5f, computePressureStrokeWidth(stroke.size, pressure, pressureAllowed))
                val angleDeg = Math.toDegrees(((pt.x * 0.17 + pt.y * 0.29 + i * 0.61) % (PI * 2.0))).toFloat()
                val hash = sin((pt.x * 12.9898 + pt.y * 78.233 + i) * 43758.5453)
                val scatter = ((hash - floor(hash) - 0.5) * stroke.size * 0.22 * (0.4 + pressure)).toFloat()

                paint.alpha = (StrokeMath.clamp(stroke.opacity * (0.3f + pressure * 0.55f), 0.04f, 0.92f) * 255f).toInt()
                val scale = stampSize / grain.width.toFloat()
                matrix.reset()
                matrix.postTranslate(-grain.width * 0.5f, -grain.height * 0.5f)
                matrix.postScale(scale, scale)
                matrix.postRotate(angleDeg)
                matrix.postTranslate(pt.x + scatter, pt.y - scatter * 0.6f)
                canvas.drawBitmap(grain, matrix, paint)
            }
            return
        }

        // Hard Round ('Ink')
        val first = pts[0]
        var isZeroLength = true
        for (i in 1 until pts.size) {
            val p = pts[i]
            if (hypot((p.x - first.x).toDouble(), (p.y - first.y).toDouble()) >= 0.25) {
                isZeroLength = false
                break
            }
        }
        if (pts.size == 1 || isZeroLength) {
            val pressure = if (pressureAllowed && first.pressure != null && first.pressure > 0f && first.pressure != 0.5f) {
                first.pressure
            } else {
                0.6f
            }
            val radius = max(0.5f, computePressureStrokeWidth(stroke.size, pressure, pressureAllowed) * 0.5f)
            val scratch = threadScratch.get()!!
            val paint = scratch.strokePaint.apply {
                reset()
                isAntiAlias = true
                style = Paint.Style.FILL
                color = stroke.colorArgb
                alpha = (stroke.opacity * 255f).toInt().coerceIn(0, 255)
            }
            canvas.drawCircle(first.x, first.y, radius, paint)
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
        colorArgb: Int,
        strokeSize: Float = 4f,
        opacity: Float = 1f
    ) {
        val kind = BrushKind.fromId(brushId)
        val pts = generateThumbnailStrokePoints(width, height, 28)
        val previewScale = StrokeMath.clamp(0.45f + (strokeSize / 10f), 0.65f, 2.8f)
        val clampedOpacity = StrokeMath.clamp(opacity, 0.08f, 1f)

        when (kind) {
            BrushKind.CALLIGRAPHY -> {
                renderCalligraphyRibbon(
                    canvas = canvas,
                    pts = pts,
                    baseSize = 2.6f * previewScale,
                    colorArgb = colorArgb,
                    opacity = clampedOpacity,
                    usePressure = true
                )
            }
            BrushKind.CHISEL_MARKER -> {
                val nib = getCachedMarkerNibBitmap(colorArgb, 32, 12)
                val scratch = threadScratch.get()!!
                val paint = scratch.bitmapPaint
                val matrix = scratch.matrix
                for (i in pts.indices) {
                    val p = pts[i]
                    val pressure = p.pressure ?: 0.7f
                    val nibW = (4.2f + pressure * 2.6f) * previewScale
                    val nibH = nibW * 0.34f
                    paint.alpha = (StrokeMath.clamp(0.24f * clampedOpacity, 0.04f, 0.42f) * 255f).toInt()
                    matrix.reset()
                    matrix.postTranslate(-nib.width * 0.5f, -nib.height * 0.5f)
                    matrix.postScale(nibW / nib.width.toFloat(), nibH / nib.height.toFloat())
                    matrix.postRotate(-32f)
                    matrix.postTranslate(p.x, p.y)
                    canvas.drawBitmap(nib, matrix, paint)
                }
            }
            BrushKind.CHARCOAL -> {
                val grain = getCachedCharcoalGrainBitmap(colorArgb, 32)
                val scratch = threadScratch.get()!!
                val paint = scratch.bitmapPaint
                val matrix = scratch.matrix
                for (i in pts.indices) {
                    val p = pts[i]
                    val pressure = p.pressure ?: 0.7f
                    val stampSize = (3.6f + pressure * 2.4f) * previewScale
                    val angleDeg = Math.toDegrees((i * 0.92) % (PI * 2.0)).toFloat()
                    val scatter = ((i % 3) - 1) * 0.65f * previewScale.coerceAtMost(1.8f)
                    paint.alpha = (StrokeMath.clamp((0.36f + pressure * 0.45f) * clampedOpacity, 0.06f, 0.9f) * 255f).toInt()
                    val scale = stampSize / grain.width.toFloat()
                    matrix.reset()
                    matrix.postTranslate(-grain.width * 0.5f, -grain.height * 0.5f)
                    matrix.postScale(scale, scale)
                    matrix.postRotate(angleDeg)
                    matrix.postTranslate(p.x + scatter, p.y - scatter * 0.55f)
                    canvas.drawBitmap(grain, matrix, paint)
                }
            }
            BrushKind.WATERCOLOR -> {
                val stamp = getCachedWatercolorStampBitmap(colorArgb, 64)
                val scratch = threadScratch.get()!!
                val paint = scratch.bitmapPaint
                val matrix = scratch.matrix
                for (i in pts.indices) {
                    val p = pts[i]
                    val pressure = p.pressure ?: 0.7f
                    val diameter = (5.4f + pressure * 3.8f) * previewScale
                    val angleDeg = Math.toDegrees((i * 0.75) % (PI * 2.0)).toFloat()
                    paint.alpha = (StrokeMath.clamp(0.24f * clampedOpacity, 0.04f, 0.36f) * 255f).toInt()
                    val scale = diameter / stamp.width.toFloat()
                    matrix.reset()
                    matrix.postTranslate(-stamp.width * 0.5f, -stamp.height * 0.5f)
                    matrix.postScale(scale, scale)
                    matrix.postRotate(angleDeg)
                    matrix.postTranslate(p.x, p.y)
                    canvas.drawBitmap(stamp, matrix, paint)
                }
            }
            BrushKind.SOFT_AIRBRUSH -> {
                for (p in pts) {
                    val pressure = p.pressure ?: 0.7f
                    val radius = (3.0f + pressure * 3.2f) * previewScale
                    val alpha = (0.55f + pressure * 0.3f) * clampedOpacity
                    renderAirbrushStamp(canvas, p.x, p.y, radius, colorArgb, alpha)
                }
            }
            BrushKind.HB_PENCIL -> {
                val grain = getCachedPencilGrainBitmap(colorArgb, 24)
                val scratch = threadScratch.get()!!
                val paint = scratch.bitmapPaint
                val matrix = scratch.matrix
                for (i in pts.indices) {
                    val p = pts[i]
                    val pressure = p.pressure ?: 0.7f
                    val stampSize = (2.4f + pressure * 1.4f) * previewScale
                    val angleDeg = Math.toDegrees((i * 0.65) % (PI * 2.0)).toFloat()
                    val scatter = ((i % 3) - 1) * 0.45f * previewScale.coerceAtMost(1.8f)
                    paint.alpha = (StrokeMath.clamp((0.35f + pressure * 0.45f) * clampedOpacity, 0.05f, 0.88f) * 255f).toInt()
                    val scale = stampSize / grain.width.toFloat()
                    matrix.reset()
                    matrix.postTranslate(-grain.width * 0.5f, -grain.height * 0.5f)
                    matrix.postScale(scale, scale)
                    matrix.postRotate(angleDeg)
                    matrix.postTranslate(p.x + scatter, p.y - scatter * 0.6f)
                    canvas.drawBitmap(grain, matrix, paint)
                }
            }
            BrushKind.HARD_ROUND -> {
                val scratch = threadScratch.get()!!
                val paint = scratch.strokePaint.apply {
                    reset()
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    color = colorArgb
                }
                for (i in 0 until pts.size - 1) {
                    val p0 = pts[i]
                    val p1 = pts[i + 1]
                    val avgPressure = ((p0.pressure ?: 0.7f) + (p1.pressure ?: 0.7f)) * 0.5f
                    paint.strokeWidth = (1.2f + avgPressure * 2.4f) * previewScale
                    paint.alpha = (StrokeMath.clamp((0.8f + avgPressure * 0.2f) * clampedOpacity, 0.08f, 1f) * 255f).toInt()
                    canvas.drawLine(p0.x, p0.y, p1.x, p1.y, paint)
                }
            }
        }
    }
}
