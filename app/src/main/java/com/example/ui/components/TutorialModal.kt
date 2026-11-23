package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.theme.VellumColors

private data class TutorialHighlight(val label: String, val detail: String)

private data class TutorialStep(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val highlights: List<TutorialHighlight>
)

private val TUTORIAL_STEPS = listOf(
    TutorialStep(
        id = "brushes",
        title = "Draw with Expressive Brushes",
        subtitle = "Switch tools, stabilize lines, and pick tactile brush engines from the quick bar or Tools sheet.",
        icon = Icons.Filled.Brush,
        highlights = listOf(
            TutorialHighlight(
                "3 Brush Engines",
                "Choose Ink (Hard Round), textured HB Pencil, or Soft Airbrush with live stroke previews."
            ),
            TutorialHighlight(
                "Tools & Shapes",
                "Draw freehand with Pen or Highlighter, erase cleanly, snap Lines/Rects/Circles, or Select & rotate."
            )
        )
    ),
    TutorialStep(
        id = "navigation",
        title = "Move & Zoom the Canvas Freely",
        subtitle = "Glide across an infinite workspace without interrupting your drawing flow.",
        icon = Icons.Filled.TouchApp,
        highlights = listOf(
            TutorialHighlight(
                "Pinch & Pan Anytime",
                "Use two fingers to pinch-zoom (25% – 400%) and pan, or toggle Pan mode in the bottom control bar."
            ),
            TutorialHighlight(
                "Pressure & Stabilization",
                "Fine-tune line smoothing and stylus pressure responsiveness from the Tools panel."
            )
        )
    ),
    TutorialStep(
        id = "symmetry",
        title = "Symmetry, Guides & References",
        subtitle = "Align sketches effortlessly with real-time mirror axes and visual overlays.",
        icon = Icons.Filled.Explore,
        highlights = listOf(
            TutorialHighlight(
                "Mirror Modes",
                "Pick Horizontal, Vertical, Radial, or Kaleidoscope from Canvas settings, and tap Recenter Axis anytime."
            ),
            TutorialHighlight(
                "Guides & Reference",
                "Enable Perspective lines, Rulers, dot grid, or pin a draggable floating Reference Image."
            )
        )
    ),
    TutorialStep(
        id = "collaborate",
        title = "Layers, Export & Live Rooms",
        subtitle = "Organize strokes by layer, export crisp PNGs, and share rooms effortlessly.",
        icon = Icons.Filled.Layers,
        highlights = listOf(
            TutorialHighlight(
                "Layer Stack & Undo",
                "Add, hide, or reorder layers cleanly. Undo your last stroke anytime from the control bar."
            ),
            TutorialHighlight(
                "Share & Export",
                "Tap Share to generate a scannable Room QR code, join by code, or Export PNG to share your artwork."
            )
        )
    )
)

@Composable
fun TutorialModal(
    onClose: () -> Unit
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val step = TUTORIAL_STEPS[stepIndex]
    val isLast = stepIndex == TUTORIAL_STEPS.lastIndex

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = VellumColors.PaperWhite,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 16.dp,
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 512.dp)
                .fillMaxWidth()
                .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(20.dp))
                .testTag("tutorial_modal")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(VellumColors.CobaltTint)
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = step.title,
                                tint = VellumColors.CobaltBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "VELLUM GUIDE · STEP ${stepIndex + 1} OF ${TUTORIAL_STEPS.size}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = VellumColors.SlateMuted
                            )
                            Text(
                                text = step.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = VellumColors.InkCharcoal
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("tutorial_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close guide",
                            tint = VellumColors.SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = VellumColors.HairlineBorder)

                // Scrollable Body
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    TutorialStepVisual(stepId = step.id)

                    Text(
                        text = step.subtitle,
                        fontSize = 13.sp,
                        color = VellumColors.SlateSecondary
                    )

                    step.highlights.forEach { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(VellumColors.Subsurface)
                                .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = item.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VellumColors.InkCharcoal
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.detail,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = VellumColors.SlateSecondary
                            )
                        }
                    }
                }

                HorizontalDivider(color = VellumColors.HairlineBorder)

                // Footer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VellumColors.HoverSurface.copy(alpha = 0.6f))
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Dots
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TUTORIAL_STEPS.forEachIndexed { idx, _ ->
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(if (idx == stepIndex) 20.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (idx == stepIndex) VellumColors.CobaltBlue else VellumColors.GridDot
                                    )
                                    .clickable { stepIndex = idx }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (stepIndex > 0) {
                            OutlinedButton(
                                onClick = { stepIndex = (stepIndex - 1).coerceAtLeast(0) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back", fontSize = 12.sp)
                            }
                        } else {
                            TextButton(
                                onClick = onClose,
                                modifier = Modifier.testTag("tutorial_skip_button")
                            ) {
                                Text("Skip", fontSize = 12.sp, color = VellumColors.SlateSecondary)
                            }
                        }

                        if (isLast) {
                            Button(
                                onClick = onClose,
                                colors = ButtonDefaults.buttonColors(containerColor = VellumColors.CobaltBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("tutorial_finish_button")
                            ) {
                                Text("Start Sketching", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        } else {
                            Button(
                                onClick = { stepIndex = (stepIndex + 1).coerceAtMost(TUTORIAL_STEPS.lastIndex) },
                                colors = ButtonDefaults.buttonColors(containerColor = VellumColors.CobaltBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("tutorial_next_button")
                            ) {
                                Text("Next", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next",
                                    modifier = Modifier.size(14.dp)
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
private fun TutorialStepVisual(stepId: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(VellumColors.Subsurface)
            .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp)
    ) {
        when (stepId) {
            "brushes" -> {
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "hard-round" to "Hard Round",
                        "hb-pencil" to "HB Pencil",
                        "soft-airbrush" to "Soft Airbrush"
                    ).forEach { (id, label) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BrushPreviewCanvas(
                                brushId = id,
                                colorArgb = if (id == "soft-airbrush") 0xFF2563EB.toInt() else 0xFF18181B.toInt(),
                                width = 68.dp,
                                height = 26.dp
                            )
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = VellumColors.InkCharcoal)
                        }
                    }
                }
            }
            "navigation" -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(VellumColors.PaperWhite)
                            .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "-   100%   +",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = VellumColors.InkCharcoal
                        )
                    }
                    Column {
                        Text("2-Finger Pinch & Pan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = VellumColors.InkCharcoal)
                        Text("Zoom smoothly 25% – 400%", fontSize = 11.sp, color = VellumColors.SlateSecondary)
                    }
                }
            }
            "symmetry" -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(width = 100.dp, height = 72.dp)) {
                        val cx = size.width / 2f
                        drawLine(
                            color = VellumColors.CobaltBlue,
                            start = Offset(cx, 4.dp.toPx()),
                            end = Offset(cx, size.height - 4.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.5.dp.toPx(),
                            center = Offset(cx, size.height / 2f)
                        )
                        drawCircle(
                            color = VellumColors.CobaltBlue,
                            radius = 3.5.dp.toPx(),
                            center = Offset(cx, size.height / 2f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        val leftPath = Path().apply {
                            moveTo(cx - 12.dp.toPx(), 14.dp.toPx())
                            cubicTo(cx - 32.dp.toPx(), 22.dp.toPx(), cx - 30.dp.toPx(), 48.dp.toPx(), cx - 10.dp.toPx(), 56.dp.toPx())
                        }
                        val rightPath = Path().apply {
                            moveTo(cx + 12.dp.toPx(), 14.dp.toPx())
                            cubicTo(cx + 32.dp.toPx(), 22.dp.toPx(), cx + 30.dp.toPx(), 48.dp.toPx(), cx + 10.dp.toPx(), 56.dp.toPx())
                        }
                        drawPath(leftPath, VellumColors.InkCharcoal, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                        drawPath(rightPath, VellumColors.InkCharcoal, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Column {
                        Text("Exact World-Space Symmetry", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = VellumColors.InkCharcoal)
                        Text("Horizontal · Vertical · Radial · Kaleidoscope", fontSize = 11.sp, color = VellumColors.SlateSecondary)
                    }
                }
            }
            else -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.width(120.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(VellumColors.PaperWhite)
                                .border(1.dp, VellumColors.CobaltBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text("Layer 2", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = VellumColors.InkCharcoal)
                            Text("Active", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = VellumColors.CobaltBlue)
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(VellumColors.PaperWhite)
                                .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text("Layer 1", fontSize = 11.sp, color = VellumColors.SlateSecondary)
                            Text("Visible", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = VellumColors.SlateMuted)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(VellumColors.CobaltBlue)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("Share Live Room", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        }
                        Text("QR room invite & PNG export", fontSize = 11.sp, color = VellumColors.SlateSecondary)
                    }
                }
            }
        }
    }
}
