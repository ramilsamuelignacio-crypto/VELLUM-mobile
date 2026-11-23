package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theme.VellumColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun ReferenceOverlay(
    imageUri: Uri,
    containerWidthPx: Float,
    containerHeightPx: Float,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val initialX = with(density) { 24.dp.toPx() }
    val initialY = with(density) { 80.dp.toPx() }
    var offsetX by remember(imageUri) { mutableFloatStateOf(initialX) }
    var offsetY by remember(imageUri) { mutableFloatStateOf(initialY) }
    var opacity by remember { mutableFloatStateOf(0.85f) }

    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, imageUri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                }
                val maxEdge = 1024
                var inSampleSize = 1
                val srcW = boundsOpts.outWidth
                val srcH = boundsOpts.outHeight
                if (srcW > maxEdge || srcH > maxEdge) {
                    val halfW = srcW / 2
                    val halfH = srcH / 2
                    while ((halfW / inSampleSize) >= maxEdge || (halfH / inSampleSize) >= maxEdge) {
                        inSampleSize *= 2
                    }
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    val cardWidthPx = with(density) { 256.dp.toPx() }
    val minY = with(density) { 48.dp.toPx() }

    Surface(
        color = VellumColors.PaperWhite,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 8.dp,
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(256.dp)
            .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(14.dp))
            .testTag("reference_overlay")
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(VellumColors.Subsurface)
                    .pointerInput(containerWidthPx, containerHeightPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val maxX = (containerWidthPx - cardWidthPx).coerceAtLeast(0f)
                            val maxY = (containerHeightPx - 160f).coerceAtLeast(minY)
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxX)
                            offsetY = (offsetY + dragAmount.y).coerceIn(minY, maxY)
                        }
                    }
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragIndicator,
                        contentDescription = "Drag reference",
                        tint = VellumColors.SlateMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Reference",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = VellumColors.InkCharcoal
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Slider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        valueRange = 0.15f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = VellumColors.CobaltBlue,
                            activeTrackColor = VellumColors.CobaltBlue
                        ),
                        modifier = Modifier
                            .width(72.dp)
                            .height(24.dp)
                            .testTag("reference_opacity_slider")
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("reference_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close reference image",
                            tint = VellumColors.SlateSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = VellumColors.HairlineBorder)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VellumColors.HoverSurface)
                    .padding(8.dp)
            ) {
                val bmp = imageBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = "Pinned reference",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .alpha(opacity)
                    )
                } else {
                    Text(
                        text = "Loading reference...",
                        fontSize = 11.sp,
                        color = VellumColors.SlateMuted,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }
        }
    }
}
