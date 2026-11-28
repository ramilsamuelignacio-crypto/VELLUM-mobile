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

    data class PaletteGroup(
        val name: String,
        val colors: List<Int>
    )

    val PaletteGroups: List<PaletteGroup> = listOf(
        PaletteGroup(
            name = "Inks & Neutrals",
            colors = listOf(
                0xFF18181B.toInt(), // Charcoal Ink
                0xFF3F3F46.toInt(), // Graphite
                0xFF71717A.toInt(), // Warm Slate
                0xFF78716C.toInt(), // Stone
                0xFFD4D4D8.toInt(), // Silver Mist
                0xFFFFFFFF.toInt()  // Paper Highlight White
            )
        ),
        PaletteGroup(
            name = "Warm Pigments",
            colors = listOf(
                0xFFDC2626.toInt(), // Vermilion
                0xFFF43F5E.toInt(), // Rose Coral
                0xFFEA580C.toInt(), // Burnt Orange
                0xFFD97706.toInt(), // Ochre
                0xFFEAB308.toInt(), // Marigold
                0xFFDB2777.toInt()  // Magenta
            )
        ),
        PaletteGroup(
            name = "Earth & Botanical",
            colors = listOf(
                0xFF92400E.toInt(), // Raw Umber
                0xFF65A30D.toInt(), // Moss
                0xFF16A34A.toInt(), // Leaf Green
                0xFF059669.toInt(), // Viridian
                0xFF0D9488.toInt(), // Deep Teal
                0xFF06B6D4.toInt()  // Aqua
            )
        ),
        PaletteGroup(
            name = "Sky & Violet",
            colors = listOf(
                0xFF0284C7.toInt(), // Cerulean
                0xFF2563EB.toInt(), // Cobalt
                0xFF4F46E5.toInt(), // Indigo
                0xFF7C3AED.toInt(), // Violet
                0xFF9333EA.toInt(), // Plum
                0xFFA855F7.toInt()  // Lavender Ink
            )
        )
    )

    val ExtendedPalette: List<Int> = PaletteGroups.flatMap { it.colors }
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
