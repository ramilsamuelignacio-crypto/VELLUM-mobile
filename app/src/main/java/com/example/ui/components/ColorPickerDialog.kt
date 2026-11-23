package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.theme.VellumColors
import kotlin.math.roundToInt

@Composable
fun ColorPickerDialog(
    initialColorArgb: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var red by remember { mutableIntStateOf(AndroidColor.red(initialColorArgb)) }
    var green by remember { mutableIntStateOf(AndroidColor.green(initialColorArgb)) }
    var blue by remember { mutableIntStateOf(AndroidColor.blue(initialColorArgb)) }
    val previewColor = AndroidColor.rgb(red, green, blue)
    val hexString = String.format("#%02X%02X%02X", red, green, blue)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = VellumColors.PaperWhite,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(20.dp))
                .testTag("custom_color_dialog")
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Custom Ink Color",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VellumColors.InkCharcoal
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = hexString,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = VellumColors.SlateSecondary
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(previewColor))
                                .border(1.dp, VellumColors.HairlineBorder, CircleShape)
                        )
                    }
                }

                ColorChannelSlider("Red", red, Color(0xFFDC2626)) { red = it }
                ColorChannelSlider("Green", green, Color(0xFF059669)) { green = it }
                ColorChannelSlider("Blue", blue, Color(0xFF2563EB)) { blue = it }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = VellumColors.SlateSecondary)
                    }
                    Button(
                        onClick = { onConfirm(previewColor) },
                        colors = ButtonDefaults.buttonColors(containerColor = VellumColors.CobaltBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("custom_color_apply")
                    ) {
                        Text("Use Color", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Int,
    trackColor: Color,
    onChange: (Int) -> Unit
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = VellumColors.InkCharcoal)
            Text(value.toString(), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = VellumColors.SlateSecondary)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = trackColor,
                activeTrackColor = trackColor
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
