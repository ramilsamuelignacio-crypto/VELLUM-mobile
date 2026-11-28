package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrushKind
import com.example.model.ToolType
import com.example.theme.VellumColors
import kotlin.math.roundToInt

@Composable
fun ToolDock(
    activeTool: ToolType,
    onSelectTool: (ToolType) -> Unit,
    colorArgb: Int,
    onSelectColor: (Int) -> Unit,
    onOpenCustomColor: () -> Unit,
    strokeSize: Float,
    onChangeSize: (Float) -> Unit,
    brushOpacity: Float,
    onOpenBrushSettings: () -> Unit,
    stabilization: Float,
    onChangeStabilization: (Float) -> Unit,
    pressureEnabled: Boolean,
    onTogglePressure: () -> Unit,
    activeBrush: String,
    onSelectBrush: (String) -> Unit,
    showGrid: Boolean,
    onToggleGrid: () -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onPickReference: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = VellumColors.PaperWhite,
        modifier = modifier
            .width(76.dp)
            .fillMaxHeight()
    ) {
        Row(modifier = Modifier.fillMaxHeight()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 10.dp, horizontal = 6.dp)
            ) {
                // 2-column Drawing Tools grid
                val toolRows = ToolType.entries.chunked(2)
                toolRows.forEach { rowTools ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowTools.forEach { tool ->
                            val isActive = activeTool == tool
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) VellumColors.CobaltBlue else Color.Transparent)
                                    .clickable { onSelectTool(tool) }
                                    .testTag("dock_tool_${tool.name.lowercase()}")
                            ) {
                                Icon(
                                    imageVector = toolIcon(tool),
                                    contentDescription = tool.label,
                                    tint = if (isActive) Color.White else VellumColors.SlateSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (rowTools.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                HorizontalDivider(color = VellumColors.HairlineBorder, modifier = Modifier.padding(horizontal = 6.dp))

                // Color swatches 2-col
                VellumColors.Swatches.chunked(2).forEach { pair ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        pair.forEach { swatch ->
                            val isSelected = colorArgb == swatch && activeTool != ToolType.ERASER
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
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
                                    .testTag("dock_swatch_${swatch}")
                            )
                        }
                    }
                }

                // Custom color picker button
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFFF43F5E),
                                    Color(0xFFF59E0B),
                                    Color(0xFF10B981),
                                    Color(0xFF3B82F6),
                                    Color(0xFF8B5CF6),
                                    Color(0xFFF43F5E)
                                )
                            )
                        )
                        .border(1.dp, VellumColors.HairlineBorder, CircleShape)
                        .clickable(onClick = onOpenCustomColor)
                        .testTag("dock_custom_color")
                )

                HorizontalDivider(color = VellumColors.HairlineBorder, modifier = Modifier.padding(horizontal = 6.dp))

                // Brush Settings Menu Trigger (Size, Opacity, Studio Palette)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(VellumColors.CobaltTint)
                        .border(1.dp, VellumColors.CobaltBorder, RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenBrushSettings)
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                        .testTag("dock_brush_settings")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(colorArgb).copy(alpha = brushOpacity))
                                .border(1.dp, VellumColors.HairlineBorder, CircleShape)
                        )
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Open brush settings",
                            tint = VellumColors.CobaltBlue,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = "${strokeSize.roundToInt()}px·${(brushOpacity * 100f).roundToInt()}%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VellumColors.CobaltBlue,
                        maxLines = 1
                    )
                }

                // Stroke size
                Text(
                    text = "${strokeSize.roundToInt()}px",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = VellumColors.SlateSecondary
                )
                Slider(
                    value = strokeSize,
                    onValueChange = onChangeSize,
                    valueRange = 1f..48f,
                    colors = SliderDefaults.colors(
                        thumbColor = VellumColors.CobaltBlue,
                        activeTrackColor = VellumColors.CobaltBlue
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .testTag("dock_size_slider")
                )

                // Stabilization
                Text(
                    text = "${(stabilization * 100f).roundToInt()}%",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = VellumColors.SlateSecondary
                )
                Slider(
                    value = stabilization * 100f,
                    onValueChange = { onChangeStabilization(it / 100f) },
                    valueRange = 0f..85f,
                    colors = SliderDefaults.colors(
                        thumbColor = VellumColors.CobaltBlue,
                        activeTrackColor = VellumColors.CobaltBlue
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .testTag("dock_stabilization_slider")
                )

                // Brush Presets
                Text(
                    text = "Brush",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = VellumColors.SlateMuted
                )
                BrushKind.entries.forEach { brush ->
                    val isActive = activeBrush == brush.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) VellumColors.InkCharcoal else Color.Transparent)
                            .clickable { onSelectBrush(brush.id) }
                            .padding(vertical = 4.dp)
                            .testTag("dock_brush_${brush.id}")
                    ) {
                        val previewInk = when {
                            isActive -> android.graphics.Color.WHITE
                            Color(colorArgb).luminance() > 0.85f -> 0xFF18181B.toInt()
                            else -> colorArgb
                        }
                        BrushPreviewCanvas(
                            brushId = brush.id,
                            colorArgb = previewInk,
                            width = 44.dp,
                            height = 18.dp
                        )
                        Text(
                            text = brush.shortLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isActive) Color.White else VellumColors.SlateSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f, fill = true))

                HorizontalDivider(color = VellumColors.HairlineBorder, modifier = Modifier.padding(horizontal = 4.dp))

                // Bottom utility toggles
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    DockUtilityButton(
                        active = pressureEnabled,
                        activeBg = VellumColors.CobaltBlue,
                        icon = Icons.Filled.Edit,
                        description = "Toggle pressure sensitivity",
                        onClick = onTogglePressure,
                        testTag = "dock_pressure_toggle",
                        modifier = Modifier.weight(1f)
                    )
                    DockUtilityButton(
                        active = showGrid,
                        activeBg = VellumColors.InkCharcoal,
                        icon = Icons.Filled.GridOn,
                        description = "Toggle dot grid",
                        onClick = onToggleGrid,
                        testTag = "dock_grid_toggle",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    DockUtilityButton(
                        active = false,
                        activeBg = VellumColors.HoverSurface,
                        icon = Icons.Filled.AddPhotoAlternate,
                        description = "Pin reference image",
                        onClick = onPickReference,
                        testTag = "dock_reference_button",
                        modifier = Modifier.weight(1f)
                    )
                    DockUtilityButton(
                        active = false,
                        activeBg = VellumColors.HoverSurface,
                        icon = Icons.AutoMirrored.Filled.Undo,
                        description = "Undo last stroke",
                        onClick = onUndo,
                        enabled = canUndo,
                        testTag = "dock_undo_button",
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClear)
                        .testTag("dock_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Clear canvas",
                        tint = VellumColors.DangerRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(VellumColors.HairlineBorder)
            )
        }
    }
}

@Composable
private fun DockUtilityButton(
    active: Boolean,
    activeBg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(30.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) activeBg else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) Color.White else VellumColors.SlateSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}
