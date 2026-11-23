package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ToolType
import com.example.model.TouchSheetTab
import com.example.theme.VellumColors

@Composable
fun TouchControlBar(
    activeSheet: TouchSheetTab?,
    onToggleSheet: (TouchSheetTab) -> Unit,
    touchPanMode: Boolean,
    onToggleTouchPan: () -> Unit,
    activeTool: ToolType,
    colorArgb: Int,
    canUndo: Boolean,
    onUndo: () -> Unit,
    layerCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = VellumColors.PaperWhite.copy(alpha = 0.96f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("touch_control_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = VellumColors.HairlineBorder)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                ControlBarItem(
                    label = "Undo",
                    icon = Icons.AutoMirrored.Filled.Undo,
                    selected = false,
                    enabled = canUndo,
                    selectedBg = VellumColors.HoverSurface,
                    selectedFg = VellumColors.InkCharcoal,
                    onClick = onUndo,
                    testTag = "control_undo",
                    modifier = Modifier.weight(1f)
                )

                ControlBarItem(
                    label = if (touchPanMode) "Pan" else "Draw",
                    icon = if (touchPanMode) Icons.Filled.PanTool else Icons.Filled.Create,
                    selected = touchPanMode,
                    enabled = true,
                    selectedBg = VellumColors.CobaltTint,
                    selectedFg = VellumColors.CobaltBlue,
                    onClick = onToggleTouchPan,
                    testTag = "control_pan_toggle",
                    modifier = Modifier.weight(1f)
                )

                // Tools tab with active color indicator dot
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (activeSheet == TouchSheetTab.TOOLS) VellumColors.InkCharcoal else Color.Transparent)
                        .clickable { onToggleSheet(TouchSheetTab.TOOLS) }
                        .padding(vertical = 4.dp)
                        .testTag("control_tools_tab")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = Icons.Filled.Brush,
                                contentDescription = "Open tools and brushes",
                                tint = if (activeSheet == TouchSheetTab.TOOLS) Color.White else VellumColors.InkCharcoal,
                                modifier = Modifier.size(20.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .offset(x = 4.dp, y = (-2).dp)
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (activeTool == ToolType.ERASER) VellumColors.SlateMuted else Color(colorArgb)
                                    )
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }
                        Text(
                            text = "Tools",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (activeSheet == TouchSheetTab.TOOLS) Color.White else VellumColors.InkCharcoal
                        )
                    }
                }

                ControlBarItem(
                    label = "Canvas",
                    icon = Icons.Filled.Explore,
                    selected = activeSheet == TouchSheetTab.CANVAS,
                    enabled = true,
                    selectedBg = VellumColors.InkCharcoal,
                    selectedFg = Color.White,
                    onClick = { onToggleSheet(TouchSheetTab.CANVAS) },
                    testTag = "control_canvas_tab",
                    modifier = Modifier.weight(1f)
                )

                ControlBarItem(
                    label = "Layers ($layerCount)",
                    icon = Icons.Filled.Layers,
                    selected = activeSheet == TouchSheetTab.LAYERS,
                    enabled = true,
                    selectedBg = VellumColors.InkCharcoal,
                    selectedFg = Color.White,
                    onClick = { onToggleSheet(TouchSheetTab.LAYERS) },
                    testTag = "control_layers_tab",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ControlBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    selectedBg: Color,
    selectedFg: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val fg = if (selected) selectedFg else VellumColors.InkCharcoal
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) selectedBg else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = fg,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = fg,
            maxLines = 1
        )
    }
}
