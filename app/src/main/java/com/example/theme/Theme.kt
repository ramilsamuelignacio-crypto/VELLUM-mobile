package com.example.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object VellumColors {
    val StudioDesk = Color(0xFFF4F3EF)
    val Subsurface = Color(0xFFF8F7F4)
    val PaperWhite = Color(0xFFFFFFFF)
    val InkCharcoal = Color(0xFF18181B)
    val SlateSecondary = Color(0xFF71717A)
    val SlateMuted = Color(0xFFA1A1AA)
    val CobaltBlue = Color(0xFF2563EB)
    val CobaltTint = Color(0xFFEFF6FF)
    val CobaltBorder = Color(0xFFBFDBFE)
    val HairlineBorder = Color(0xFFE4E4E7)
    val GridDot = Color(0xFFD4D4D8)
    val HoverSurface = Color(0xFFF4F4F5)
    val DangerRed = Color(0xFFDC2626)
    val DangerBg = Color(0xFFFEF2F2)
    val DangerBorder = Color(0xFFFECACA)
    val EmeraldGreen = Color(0xFF059669)
    val AmberWarn = Color(0xFFD97706)

    val Swatches: List<Int> = listOf(
        0xFF18181B.toInt(), // Charcoal Ink
        0xFFDC2626.toInt(), // Vermilion
        0xFFD97706.toInt(), // Ochre
        0xFF059669.toInt(), // Viridian
        0xFF2563EB.toInt(), // Cobalt
        0xFF7C3AED.toInt()  // Violet
    )
}

private val VellumLightColorScheme = lightColorScheme(
    primary = VellumColors.CobaltBlue,
    onPrimary = Color.White,
    primaryContainer = VellumColors.CobaltTint,
    onPrimaryContainer = VellumColors.CobaltBlue,
    secondary = VellumColors.InkCharcoal,
    onSecondary = Color.White,
    background = VellumColors.StudioDesk,
    onBackground = VellumColors.InkCharcoal,
    surface = VellumColors.PaperWhite,
    onSurface = VellumColors.InkCharcoal,
    surfaceVariant = VellumColors.Subsurface,
    onSurfaceVariant = VellumColors.SlateSecondary,
    outline = VellumColors.HairlineBorder,
    error = VellumColors.DangerRed,
    onError = Color.White
)

val VellumTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

@Composable
fun VellumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VellumLightColorScheme,
        typography = VellumTypography,
        content = content
    )
}
