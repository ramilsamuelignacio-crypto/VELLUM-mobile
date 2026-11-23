package com.example.math

import com.example.model.SketchPoint
import kotlin.math.hypot
import kotlin.math.round

data class PinchTransformResult(
    val pan: SketchPoint,
    val zoom: Float
)

object TouchMath {
    fun computePinchTransform(
        p1Prev: SketchPoint,
        p2Prev: SketchPoint,
        p1Next: SketchPoint,
        p2Next: SketchPoint,
        currentPan: SketchPoint,
        currentZoom: Float,
        minZoom: Float = 0.25f,
        maxZoom: Float = 4f
    ): PinchTransformResult {
        val prevDist = hypot((p2Prev.x - p1Prev.x).toDouble(), (p2Prev.y - p1Prev.y).toDouble()).toFloat()
        val nextDist = hypot((p2Next.x - p1Next.x).toDouble(), (p2Next.y - p1Next.y).toDouble()).toFloat()

        val prevMid = SketchPoint(
            x = (p1Prev.x + p2Prev.x) / 2f,
            y = (p1Prev.y + p2Prev.y) / 2f
        )
        val nextMid = SketchPoint(
            x = (p1Next.x + p2Next.x) / 2f,
            y = (p1Next.y + p2Next.y) / 2f
        )

        if (prevDist < 1f || nextDist < 1f || currentZoom <= 0f) {
            return PinchTransformResult(
                zoom = StrokeMath.clamp(if (currentZoom > 0f) currentZoom else 1f, minZoom, maxZoom),
                pan = SketchPoint(
                    x = currentPan.x + (nextMid.x - prevMid.x),
                    y = currentPan.y + (nextMid.y - prevMid.y)
                )
            )
        }

        val scaleFactor = nextDist / prevDist
        val nextZoom = StrokeMath.clamp(
            round(currentZoom * scaleFactor * 1000f) / 1000f,
            minZoom,
            maxZoom
        )

        val worldX = (prevMid.x - currentPan.x) / currentZoom
        val worldY = (prevMid.y - currentPan.y) / currentZoom

        return PinchTransformResult(
            zoom = nextZoom,
            pan = SketchPoint(
                x = nextMid.x - worldX * nextZoom,
                y = nextMid.y - worldY * nextZoom
            )
        )
    }
}
