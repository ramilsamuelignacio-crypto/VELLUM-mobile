package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BrushKind
import com.example.model.ToolType
import com.example.theme.VellumColors
import kotlin.math.roundToInt

private val SIZE_PRESETS = listOf(2, 6, 12, 24, 40)
private val OPACITY_PRESETS = listOf(25, 50, 75, 100)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrushSettingsMenu(
    activeTool: ToolType,
    activeBrush: String,
    onSelectBrush: (String) -> Unit,
    strokeSize: Float,
    onChangeSize: (Float) -> Unit,
    brushOpacity: Float,
    onChangeOpacity: (Float) -> Unit,
    colorArgb: Int,
    customColors: List<Int>,
    onSelectColor: (Int) -> Unit,
    onOpenCustomColor: () -> Unit,
    stabilization: Float,
    onChangeStabilization: (Float) -> Unit,
    pressureEnabled: Boolean,
    onTogglePressure: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeBrushKind = BrushKind.fromId(activeBrush)
    val hexString = String.format(
        "#%02X%02X%02X",
        AndroidColor.red(colorArgb),
        AndroidColor.green(colorArgb),
        AndroidColor.blue(colorArgb)
    )
    val opacityPercent = (brushOpacity * 100f).roundToInt()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Surface(
                color = VellumColors.PaperWhite,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(20.dp))
                    .testTag("brush_settings_menu")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(VellumColors.CobaltTint)
                                    .border(1.dp, VellumColors.CobaltBorder, RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Brush,
                                    contentDescription = "Brush settings",
                                    tint = VellumColors.CobaltBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Brush Settings",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VellumColors.InkCharcoal
                                )
                                Text(
                                    text = "${activeBrushKind.label} · ${activeTool.label}",
                                    fontSize = 12.sp,
                                    color = VellumColors.SlateSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("brush_menu_close")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close brush settings",
                                tint = VellumColors.SlateSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = VellumColors.HairlineBorder)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        // Live Brush Preview Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(VellumColors.Subsurface)
                                .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(14.dp))
                        ) {
                            // Subtle checkerboard background to make opacity adjustments immediately visible
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cell = 10.dp.toPx()
                                val cols = (size.width / cell).toInt() + 1
                                val rows = (size.height / cell).toInt() + 1
                                for (r in 0 until rows) {
                                    for (c in 0 until cols) {
                                        if ((r + c) % 2 == 0) {
                                            drawRect(
                                                color = Color(0x0A18181B),
                                                topLeft = Offset(c * cell, r * cell),
                                                size = Size(cell, cell)
                                            )
                                        }
                                    }
                                }
                            }

                            BrushPreviewCanvas(
                                brushId = activeBrush,
                                colorArgb = colorArgb,
                                width = 280.dp,
                                height = 56.dp,
                                strokeSize = strokeSize,
                                opacity = brushOpacity,
                                modifier = Modifier.align(Alignment.Center)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VellumColors.PaperWhite.copy(alpha = 0.92f))
                                    .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorArgb).copy(alpha = brushOpacity))
                                        .border(1.dp, VellumColors.HairlineBorder, CircleShape)
                                )
                                Text(
                                    text = "${strokeSize.roundToInt()}px · $opacityPercent% · $hexString",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = VellumColors.InkCharcoal
                                )
                            }
                        }

                        // Brush Engine Selector (3-column grid)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                BrushMenuSectionTitle("Brush Engine")
                                Text(
                                    text = activeBrushKind.description,
                                    fontSize = 11.sp,
                                    color = VellumColors.SlateSecondary,
                                    maxLines = 1
                                )
                            }
                            BrushKind.entries.chunked(3).forEach { rowBrushes ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowBrushes.forEach { brush ->
                                        val isActive = activeBrush == brush.id
                                        val previewInk = when {
                                            isActive -> AndroidColor.WHITE
                                            Color(colorArgb).luminance() > 0.85f -> 0xFF18181B.toInt()
                                            else -> colorArgb
                                        }
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 60.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isActive) VellumColors.InkCharcoal else VellumColors.PaperWhite)
                                                .border(
                                                    1.dp,
                                                    if (isActive) VellumColors.InkCharcoal else VellumColors.HairlineBorder,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { onSelectBrush(brush.id) }
                                                .padding(horizontal = 6.dp, vertical = 8.dp)
                                                .testTag("brush_menu_engine_${brush.id}")
                                        ) {
                                            BrushPreviewCanvas(
                                                brushId = brush.id,
                                                colorArgb = previewInk,
                                                width = 48.dp,
                                                height = 20.dp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = brush.label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isActive) Color.White else VellumColors.InkCharcoal,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    repeat(3 - rowBrushes.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        // Brush Size Controls
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                BrushMenuSectionTitle("Brush Size")
                                ValueReadoutChip("${strokeSize.roundToInt()} px")
                            }
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
                                    .testTag("brush_menu_size_slider")
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SIZE_PRESETS.forEach { preset ->
                                    val selected = strokeSize.roundToInt() == preset
                                    PresetPill(
                                        label = "${preset}px",
                                        selected = selected,
                                        onClick = { onChangeSize(preset.toFloat()) },
                                        testTag = "brush_menu_size_preset_$preset",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Opacity Controls
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                BrushMenuSectionTitle("Opacity")
                                ValueReadoutChip("$opacityPercent%")
                            }
                            Slider(
                                value = brushOpacity * 100f,
                                onValueChange = { onChangeOpacity(it / 100f) },
                                valueRange = 10f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = VellumColors.CobaltBlue,
                                    activeTrackColor = VellumColors.CobaltBlue
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("brush_menu_opacity_slider")
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OPACITY_PRESETS.forEach { preset ->
                                    val selected = opacityPercent == preset
                                    PresetPill(
                                        label = "$preset%",
                                        selected = selected,
                                        onClick = { onChangeOpacity(preset / 100f) },
                                        testTag = "brush_menu_opacity_preset_$preset",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Color Palette Section
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Palette,
                                        contentDescription = "Color palette",
                                        tint = VellumColors.SlateSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    BrushMenuSectionTitle("Color Palette")
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(VellumColors.Subsurface)
                                        .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(10.dp))
                                        .clickable(onClick = onOpenCustomColor)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("brush_menu_custom_color")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
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
                                    )
                                    Text(
                                        text = "Custom RGB",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = VellumColors.InkCharcoal
                                    )
                                }
                            }

                            if (customColors.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Recent Custom",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = VellumColors.SlateSecondary
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        customColors.forEach { swatch ->
                                            PaletteSwatchCircle(
                                                swatchArgb = swatch,
                                                selected = colorArgb == swatch && activeTool != ToolType.ERASER,
                                                onClick = { onSelectColor(swatch) },
                                                testTag = "brush_menu_custom_swatch_$swatch"
                                            )
                                        }
                                    }
                                }
                            }

                            VellumColors.PaletteGroups.forEach { group ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = group.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = VellumColors.SlateSecondary
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        group.colors.forEach { swatch ->
                                            PaletteSwatchCircle(
                                                swatchArgb = swatch,
                                                selected = colorArgb == swatch && activeTool != ToolType.ERASER,
                                                onClick = { onSelectColor(swatch) },
                                                testTag = "brush_menu_color_$swatch",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = VellumColors.HairlineBorder)

                        // Stabilization & Stylus Pressure
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                BrushMenuSectionTitle("Stroke Stabilization")
                                ValueReadoutChip("${(stabilization * 100f).roundToInt()}%")
                            }
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
                                    .testTag("brush_menu_stabilization_slider")
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (pressureEnabled) VellumColors.CobaltTint else VellumColors.Subsurface)
                                    .border(
                                        1.dp,
                                        if (pressureEnabled) VellumColors.CobaltBorder else VellumColors.HairlineBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable(onClick = onTogglePressure)
                                    .padding(horizontal = 14.dp)
                                    .testTag("brush_menu_pressure_toggle")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Pressure sensitivity",
                                        tint = if (pressureEnabled) VellumColors.CobaltBlue else VellumColors.InkCharcoal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Stylus & Pen Pressure Sensitivity",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (pressureEnabled) VellumColors.CobaltBlue else VellumColors.InkCharcoal
                                    )
                                }
                                Text(
                                    text = if (pressureEnabled) "On" else "Off",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (pressureEnabled) VellumColors.CobaltBlue else VellumColors.SlateSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrushMenuSectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = VellumColors.SlateMuted
    )
}

@Composable
private fun ValueReadoutChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(VellumColors.Subsurface)
            .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = VellumColors.InkCharcoal
        )
    }
}

@Composable
private fun PresetPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) VellumColors.CobaltTint else VellumColors.Subsurface)
            .border(
                1.dp,
                if (selected) VellumColors.CobaltBlue else VellumColors.HairlineBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) VellumColors.CobaltBlue else VellumColors.InkCharcoal
        )
    }
}

@Composable
private fun PaletteSwatchCircle(
    swatchArgb: Int,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val swatchColor = Color(swatchArgb)
    val checkTint = if (swatchColor.luminance() > 0.65f) VellumColors.InkCharcoal else Color.White
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(swatchColor)
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) VellumColors.CobaltBlue else VellumColors.HairlineBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected color",
                tint = checkTint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
