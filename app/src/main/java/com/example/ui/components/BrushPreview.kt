package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.engine.BrushEngine
import com.example.model.ToolType

fun toolIcon(tool: ToolType): ImageVector = when (tool) {
    ToolType.SELECT -> Icons.Filled.NearMe
    ToolType.PEN -> Icons.Filled.Create
    ToolType.HIGHLIGHTER -> Icons.Filled.BorderColor
    ToolType.ERASER -> Icons.Filled.CleaningServices
    ToolType.LINE -> Icons.Filled.HorizontalRule
    ToolType.RECT -> Icons.Filled.CheckBoxOutlineBlank
    ToolType.CIRCLE -> Icons.Filled.RadioButtonUnchecked
}

@Composable
fun BrushPreviewCanvas(
    brushId: String,
    colorArgb: Int,
    width: Dp = 44.dp,
    height: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width, height)) {
        drawContext.canvas.nativeCanvas.let { nativeCanvas ->
            val scaleX = size.width / width.value
            val scaleY = size.height / height.value
            nativeCanvas.save()
            nativeCanvas.scale(scaleX, scaleY)
            BrushEngine.renderBrushPreviewToCanvas(
                canvas = nativeCanvas,
                brushId = brushId,
                width = width.value,
                height = height.value,
                colorArgb = colorArgb
            )
            nativeCanvas.restore()
        }
    }
}
