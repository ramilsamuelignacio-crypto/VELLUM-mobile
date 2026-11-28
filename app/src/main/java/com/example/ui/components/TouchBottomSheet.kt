package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrushKind
import com.example.model.GuideMode
import com.example.model.Layer
import com.example.model.SymmetryMode
import com.example.model.ToolType
import com.example.model.TouchSheetTab
import com.example.theme.VellumColors
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TouchBottomSheet(
    activeSheet: TouchSheetTab?,
    onClose: () -> Unit,
    activeTool: ToolType,
    onSelectTool: (ToolType) -> Unit,
    colorArgb: Int,
    customColors: List<Int>,
    onSelectColor: (Int) -> Unit,
    onOpenCustomColor: () -> Unit,
    strokeSize: Float,
    onChangeSize: (Float) -> Unit,
    brushOpacity: Float,
    onChangeOpacity: (Float) -> Unit,
    onOpenBrushSettings: () -> Unit,
    stabilization: Float,
    onChangeStabilization: (Float) -> Unit,
    pressureEnabled: Boolean,
    onTogglePressure: () -> Unit,
    activeBrush: String,
    onSelectBrush: (String) -> Unit,
    symmetryMode: SymmetryMode,
    onChangeSymmetryMode: (SymmetryMode) -> Unit,
    onRecenterSymmetry: () -> Unit,
    guideMode: GuideMode,
    onChangeGuideMode: (GuideMode) -> Unit,
    showGrid: Boolean,
    onToggleGrid: () -> Unit,
    zoom: Float,
    onZoomStep: (Float) -> Unit,
    onResetView: () -> Unit,
    onPickReference: () -> Unit,
    onExportPng: () -> Unit,
    onClearCanvas: () -> Unit,
    layers: List<Layer>,
    activeLayerId: String,
    onSelectLayer: (String) -> Unit,
    onAddLayer: () -> Unit,
    onToggleLayerVisibility: (String) -> Unit,
    onMoveLayer: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = activeSheet != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x5909090B))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        ) {
            AnimatedVisibility(
                visible = activeSheet != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                val title = when (activeSheet) {
                    TouchSheetTab.TOOLS -> "Tools, Brushes & Color"
                    TouchSheetTab.CANVAS -> "Canvas, Symmetry & Zoom"
                    TouchSheetTab.LAYERS -> "Layers"
                    null -> ""
                }

                Surface(
                    color = VellumColors.PaperWhite,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .testTag("touch_bottom_sheet")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Drag handle & header
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, start = 20.dp, end = 12.dp, bottom = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(VellumColors.GridDot)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VellumColors.InkCharcoal
                                )
                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("sheet_close_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close touch panel",
                                        tint = VellumColors.SlateSecondary
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = VellumColors.HairlineBorder)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            when (activeSheet) {
                                TouchSheetTab.TOOLS -> {
                                    // Drawing Tools 4-col grid
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SheetSectionLabel("Drawing Tool")
                                        ToolType.entries.chunked(4).forEach { rowTools ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                rowTools.forEach { tool ->
                                                    val isActive = activeTool == tool
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(min = 52.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(
                                                                if (isActive) VellumColors.CobaltBlue else VellumColors.Subsurface
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (isActive) VellumColors.CobaltBlue else VellumColors.HairlineBorder,
                                                                RoundedCornerShape(12.dp)
                                                            )
                                                            .clickable { onSelectTool(tool) }
                                                            .padding(vertical = 6.dp)
                                                            .testTag("sheet_tool_${tool.name.lowercase()}")
                                                    ) {
                                                        Icon(
                                                            imageVector = toolIcon(tool),
                                                            contentDescription = tool.label,
                                                            tint = if (isActive) Color.White else VellumColors.InkCharcoal,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = tool.label,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (isActive) Color.White else VellumColors.InkCharcoal,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                                repeat(4 - rowTools.size) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }

                                    // Brush Engine 3-col grid
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val activeKind = BrushKind.fromId(activeBrush)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            SheetSectionLabel("Brush Engine")
                                            Text(
                                                text = activeKind.description,
                                                fontSize = 11.sp,
                                                color = VellumColors.SlateSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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
                                                        isActive -> android.graphics.Color.WHITE
                                                        Color(colorArgb).luminance() > 0.85f -> 0xFF18181B.toInt()
                                                        else -> colorArgb
                                                    }
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(min = 58.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(if (isActive) VellumColors.InkCharcoal else VellumColors.PaperWhite)
                                                            .border(
                                                                1.dp,
                                                                if (isActive) VellumColors.InkCharcoal else VellumColors.HairlineBorder,
                                                                RoundedCornerShape(12.dp)
                                                            )
                                                            .clickable { onSelectBrush(brush.id) }
                                                            .padding(horizontal = 6.dp, vertical = 8.dp)
                                                            .testTag("sheet_brush_${brush.id}")
                                                    ) {
                                                        BrushPreviewCanvas(
                                                            brushId = brush.id,
                                                            colorArgb = previewInk,
                                                            width = 48.dp,
                                                            height = 22.dp
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

                                    // Ink Color Palette
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            SheetSectionLabel("Color Palette")
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(VellumColors.CobaltTint)
                                                    .border(1.dp, VellumColors.CobaltBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        onClose()
                                                        onOpenBrushSettings()
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                                    .testTag("sheet_open_brush_menu")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Tune,
                                                    contentDescription = "Open Brush Studio",
                                                    tint = VellumColors.CobaltBlue,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "Brush Menu",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = VellumColors.CobaltBlue
                                                )
                                            }
                                        }
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            (customColors + VellumColors.ExtendedPalette).distinct().forEach { swatch ->
                                                val isSelected = colorArgb == swatch && activeTool != ToolType.ERASER
                                                val swatchColor = Color(swatch)
                                                val checkTint = if (swatchColor.luminance() > 0.65f) VellumColors.InkCharcoal else Color.White
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(swatchColor)
                                                        .border(
                                                            width = if (isSelected) 3.dp else 1.dp,
                                                            color = if (isSelected) VellumColors.CobaltBlue else VellumColors.HairlineBorder,
                                                            shape = CircleShape
                                                        )
                                                        .clickable { onSelectColor(swatch) }
                                                        .testTag("sheet_swatch_$swatch")
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = "Selected color",
                                                            tint = checkTint,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
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
                                                    .testTag("sheet_custom_color")
                                            )
                                        }
                                    }

                                    // Sliders
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Column {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Stroke Size", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = VellumColors.InkCharcoal)
                                                Text("${strokeSize.roundToInt()}px", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = VellumColors.SlateSecondary)
                                            }
                                            Slider(
                                                value = strokeSize,
                                                onValueChange = onChangeSize,
                                                valueRange = 1f..48f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = VellumColors.CobaltBlue,
                                                    activeTrackColor = VellumColors.CobaltBlue
                                                ),
                                                modifier = Modifier.testTag("sheet_size_slider")
                                            )
                                        }
                                        Column {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Opacity", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = VellumColors.InkCharcoal)
                                                Text("${(brushOpacity * 100f).roundToInt()}%", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = VellumColors.SlateSecondary)
                                            }
                                            Slider(
                                                value = brushOpacity * 100f,
                                                onValueChange = { onChangeOpacity(it / 100f) },
                                                valueRange = 10f..100f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = VellumColors.CobaltBlue,
                                                    activeTrackColor = VellumColors.CobaltBlue
                                                ),
                                                modifier = Modifier.testTag("sheet_opacity_slider")
                                            )
                                        }
                                        Column {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Stabilization", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = VellumColors.InkCharcoal)
                                                Text("${(stabilization * 100f).roundToInt()}%", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = VellumColors.SlateSecondary)
                                            }
                                            Slider(
                                                value = stabilization * 100f,
                                                onValueChange = { onChangeStabilization(it / 100f) },
                                                valueRange = 0f..85f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = VellumColors.CobaltBlue,
                                                    activeTrackColor = VellumColors.CobaltBlue
                                                )
                                            )
                                        }
                                    }

                                    // Pressure Toggle
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
                                            .testTag("sheet_pressure_toggle")
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

                                TouchSheetTab.CANVAS -> {
                                    // Symmetry
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            SheetSectionLabel("Symmetry Mode")
                                            if (symmetryMode != SymmetryMode.NONE) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(VellumColors.CobaltTint)
                                                        .clickable(onClick = onRecenterSymmetry)
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                        .testTag("sheet_recenter_symmetry")
                                                ) {
                                                    Text(
                                                        text = "Recenter Axis",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = VellumColors.CobaltBlue
                                                    )
                                                }
                                            }
                                        }
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SymmetryMode.entries.forEach { opt ->
                                                val isActive = symmetryMode == opt
                                                SheetChip(
                                                    label = opt.label,
                                                    selected = isActive,
                                                    selectedBg = VellumColors.CobaltBlue,
                                                    onClick = { onChangeSymmetryMode(opt) },
                                                    testTag = "sheet_symmetry_${opt.name.lowercase()}"
                                                )
                                            }
                                        }
                                    }

                                    // Guides & Grid
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SheetSectionLabel("Guides & Grid")
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            GuideMode.entries.forEach { opt ->
                                                SheetChip(
                                                    label = opt.label,
                                                    selected = guideMode == opt,
                                                    selectedBg = VellumColors.InkCharcoal,
                                                    onClick = { onChangeGuideMode(opt) },
                                                    testTag = "sheet_guide_${opt.name.lowercase()}"
                                                )
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier
                                                    .heightIn(min = 44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (showGrid) VellumColors.InkCharcoal else VellumColors.Subsurface)
                                                    .border(
                                                        1.dp,
                                                        if (showGrid) VellumColors.InkCharcoal else VellumColors.HairlineBorder,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable(onClick = onToggleGrid)
                                                    .padding(horizontal = 14.dp)
                                                    .testTag("sheet_grid_toggle")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.GridOn,
                                                    contentDescription = "Dot Grid",
                                                    tint = if (showGrid) Color.White else VellumColors.InkCharcoal,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Dot Grid",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (showGrid) Color.White else VellumColors.InkCharcoal
                                                )
                                            }
                                        }
                                    }

                                    // Zoom Controls
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SheetSectionLabel("Zoom & Touch Navigation (Pinch with 2 fingers or use Pan mode)")
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(VellumColors.Subsurface)
                                                    .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(12.dp))
                                                    .clickable { onZoomStep(-0.25f) }
                                                    .testTag("sheet_zoom_out")
                                            ) {
                                                Text("-25%", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = VellumColors.InkCharcoal)
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(VellumColors.PaperWhite)
                                                    .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(12.dp))
                                                    .clickable(onClick = onResetView)
                                                    .testTag("sheet_zoom_reset")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Refresh,
                                                    contentDescription = "Reset zoom",
                                                    tint = VellumColors.InkCharcoal,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${(zoom * 100f).roundToInt()}%",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    color = VellumColors.InkCharcoal
                                                )
                                            }
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(VellumColors.Subsurface)
                                                    .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(12.dp))
                                                    .clickable { onZoomStep(0.25f) }
                                                    .testTag("sheet_zoom_in")
                                            ) {
                                                Text("+25%", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = VellumColors.InkCharcoal)
                                            }
                                        }
                                    }

                                    // Reference, Export, Clear
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CanvasActionTile(
                                            label = "Reference",
                                            icon = Icons.Filled.AddPhotoAlternate,
                                            bg = VellumColors.PaperWhite,
                                            border = VellumColors.HairlineBorder,
                                            fg = VellumColors.InkCharcoal,
                                            onClick = {
                                                onPickReference()
                                                onClose()
                                            },
                                            testTag = "sheet_reference_button",
                                            modifier = Modifier.weight(1f)
                                        )
                                        CanvasActionTile(
                                            label = "Export",
                                            icon = Icons.Filled.Download,
                                            bg = VellumColors.PaperWhite,
                                            border = VellumColors.HairlineBorder,
                                            fg = VellumColors.InkCharcoal,
                                            onClick = {
                                                onExportPng()
                                                onClose()
                                            },
                                            testTag = "sheet_export_button",
                                            modifier = Modifier.weight(1f)
                                        )
                                        CanvasActionTile(
                                            label = "Clear",
                                            icon = Icons.Filled.DeleteOutline,
                                            bg = VellumColors.DangerBg,
                                            border = VellumColors.DangerBorder,
                                            fg = VellumColors.DangerRed,
                                            onClick = {
                                                onClearCanvas()
                                                onClose()
                                            },
                                            testTag = "sheet_clear_button",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                TouchSheetTab.LAYERS -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        SheetSectionLabel("Tap a layer to make it active")
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(VellumColors.CobaltBlue)
                                                .clickable(onClick = onAddLayer)
                                                .padding(horizontal = 14.dp)
                                                .testTag("sheet_add_layer")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = "Add Layer",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Add Layer",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    val displayLayers = layers.reversed()
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        displayLayers.forEachIndexed { displayIdx, layer ->
                                            val actualIdx = layers.size - 1 - displayIdx
                                            val isActive = layer.id == activeLayerId
                                            val canMoveUp = actualIdx < layers.size - 1
                                            val canMoveDown = actualIdx > 0

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 56.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(if (isActive) VellumColors.CobaltTint else VellumColors.Subsurface)
                                                    .border(
                                                        1.dp,
                                                        if (isActive) VellumColors.CobaltBorder else VellumColors.HairlineBorder,
                                                        RoundedCornerShape(16.dp)
                                                    )
                                                    .clickable { onSelectLayer(layer.id) }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    .testTag("sheet_layer_${layer.id}")
                                            ) {
                                                IconButton(
                                                    onClick = { onToggleLayerVisibility(layer.id) },
                                                    modifier = Modifier.size(44.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (layer.visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                                        contentDescription = "Toggle visibility for ${layer.name}",
                                                        tint = if (layer.visible) VellumColors.InkCharcoal else VellumColors.SlateMuted,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Text(
                                                    text = layer.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = VellumColors.InkCharcoal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = { onMoveLayer(actualIdx, actualIdx + 1) },
                                                    enabled = canMoveUp,
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .alpha(if (canMoveUp) 1f else 0.25f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.KeyboardArrowUp,
                                                        contentDescription = "Move ${layer.name} up",
                                                        tint = VellumColors.InkCharcoal
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onMoveLayer(actualIdx, actualIdx - 1) },
                                                    enabled = canMoveDown,
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .alpha(if (canMoveDown) 1f else 0.25f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                                        contentDescription = "Move ${layer.name} down",
                                                        tint = VellumColors.InkCharcoal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                null -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = VellumColors.SlateSecondary
    )
}

@Composable
private fun SheetChip(
    label: String,
    selected: Boolean,
    selectedBg: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) selectedBg else VellumColors.Subsurface)
            .border(
                1.dp,
                if (selected) selectedBg else VellumColors.HairlineBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else VellumColors.InkCharcoal
        )
    }
}

@Composable
private fun CanvasActionTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    border: Color,
    fg: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
            .testTag(testTag)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}
