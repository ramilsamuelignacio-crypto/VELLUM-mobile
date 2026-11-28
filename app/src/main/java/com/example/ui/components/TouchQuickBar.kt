package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrushKind
import com.example.model.ToolType
import com.example.theme.VellumColors
import kotlin.math.roundToInt

private val QUICK_TOOLS = listOf(
    ToolType.PEN,
    ToolType.HIGHLIGHTER,
    ToolType.ERASER,
    ToolType.LINE,
    ToolType.RECT,
    ToolType.CIRCLE,
    ToolType.SELECT
)

@Composable
fun TouchQuickBar(
    activeTool: ToolType,
    onSelectTool: (ToolType) -> Unit,
    activeBrush: String,
    onSelectBrush: (String) -> Unit,
    colorArgb: Int,
    onSelectColor: (Int) -> Unit,
    strokeSize: Float,
    onChangeSize: (Float) -> Unit,
    brushOpacity: Float,
    onOpenBrushSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = VellumColors.PaperWhite.copy(alpha = 0.96f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("touch_quick_tools")
            ) {
                // Tools
                QUICK_TOOLS.forEach { tool ->
                    val isActive = activeTool == tool
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) VellumColors.CobaltBlue else VellumColors.Subsurface)
                            .clickable { onSelectTool(tool) }
                            .testTag("quick_tool_${tool.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = toolIcon(tool),
                            contentDescription = tool.label,
                            tint = if (isActive) Color.White else VellumColors.InkCharcoal,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                QuickDivider()

                // Brush presets
                BrushKind.entries.forEach { brush ->
                    val isActive = activeBrush == brush.id
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) VellumColors.InkCharcoal else VellumColors.Subsurface)
                            .clickable { onSelectBrush(brush.id) }
                            .padding(horizontal = 10.dp)
                            .testTag("quick_brush_${brush.id}")
                    ) {
                        Text(
                            text = brush.shortLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isActive) Color.White else VellumColors.InkCharcoal
                        )
                    }
                }

                QuickDivider()

                // Color swatches
                VellumColors.Swatches.forEach { swatch ->
                    val isSelected = colorArgb == swatch && activeTool != ToolType.ERASER
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(swatch))
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, VellumColors.CobaltBlue, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onSelectColor(swatch) }
                            .testTag("quick_swatch_$swatch")
                    )
                }

                QuickDivider()

                // Size steppers & brush settings trigger
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 32.dp, height = 36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(VellumColors.Subsurface)
                        .clickable { onChangeSize((strokeSize - 2f).coerceAtLeast(1f)) }
                        .testTag("quick_size_decrease")
                ) {
                    Text("-", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = VellumColors.InkCharcoal)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(VellumColors.CobaltTint)
                        .border(1.dp, VellumColors.CobaltBorder, RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenBrushSettings)
                        .padding(horizontal = 9.dp)
                        .testTag("quick_brush_settings")
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(colorArgb).copy(alpha = brushOpacity))
                            .border(1.dp, VellumColors.HairlineBorder, CircleShape)
                    )
                    Text(
                        text = "${strokeSize.roundToInt()}px · ${(brushOpacity * 100f).roundToInt()}%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VellumColors.CobaltBlue
                    )
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Open brush settings",
                        tint = VellumColors.CobaltBlue,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 32.dp, height = 36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(VellumColors.Subsurface)
                        .clickable { onChangeSize((strokeSize + 2f).coerceAtMost(48f)) }
                        .testTag("quick_size_increase")
                ) {
                    Text("+", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = VellumColors.InkCharcoal)
                }
            }
            HorizontalDivider(thickness = 1.dp, color = VellumColors.HairlineBorder)
        }
    }
}

@Composable
private fun QuickDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(VellumColors.HairlineBorder)
    )
}
