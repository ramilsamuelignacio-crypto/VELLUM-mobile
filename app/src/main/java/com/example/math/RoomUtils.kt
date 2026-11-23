package com.example.math

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object RoomUtils {
    private val TOKEN_REGEX = Regex("[^a-z0-9-_]")

    fun normalizeRoomCode(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        if (trimmed.contains("://") || trimmed.contains("?room=") || trimmed.contains("&room=")) {
            try {
                val uri = when {
                    trimmed.contains("://") -> Uri.parse(trimmed)
                    trimmed.startsWith("?") -> Uri.parse("https://vellum.local/$trimmed")
                    else -> Uri.parse("https://vellum.local/?$trimmed")
                }
                val roomParam = uri.getQueryParameter("room")
                if (!roomParam.isNullOrBlank()) {
                    return sanitizeRoomToken(roomParam)
                }
                if (uri.scheme == "vellum" && uri.host == "room") {
                    val seg = uri.lastPathSegment
                    if (!seg.isNullOrBlank()) {
                        return sanitizeRoomToken(seg)
                    }
                }
            } catch (_: Exception) {
                // Fall back to direct token sanitization
            }
        }
        return sanitizeRoomToken(trimmed)
    }

    private fun sanitizeRoomToken(raw: String): String {
        val cleaned = raw.trim().lowercase().replace(TOKEN_REGEX, "")
        return if (cleaned.length in 2..48) cleaned else ""
    }

    fun buildRoomShareUrl(roomId: String): String {
        val safeRoom = sanitizeRoomToken(roomId).ifEmpty { "studio-1" }
        return "https://vellum.studio/?room=$safeRoom"
    }

    fun generateQrBitmap(content: String, sizePx: Int = 448): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 2,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height)
            val dark = Color.rgb(24, 24, 27)
            val light = Color.WHITE
            for (y in 0 until height) {
                val rowOffset = y * width
                for (x in 0 until width) {
                    pixels[rowOffset + x] = if (matrix.get(x, y)) dark else light
                }
            }
            Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        } catch (_: Exception) {
            null
        }
    }
}
