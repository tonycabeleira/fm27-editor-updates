package com.cabincrew.academy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand palette
val Navy = Color(0xFF0B1F3A)
val NavyLight = Color(0xFF1C3A63)
val NavyDeep = Color(0xFF06111F)
val Gold = Color(0xFFC9A227)
val GoldLight = Color(0xFFE6C766)
val Sky = Color(0xFF3B7BBF)
val Success = Color(0xFF1E8E5A)
val SuccessDark = Color(0xFF4CC38A)
val Danger = Color(0xFFC0392B)
val DangerDark = Color(0xFFFF7A6B)
val Warning = Color(0xFFE08E0B)

@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val gold: Color,
    val warning: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val cardBorder: Color,
    val chartTrack: Color,
)

private val LightExtended = ExtendedColors(
    success = Success, onSuccess = Color.White, successContainer = Color(0xFFDDF3E7),
    danger = Danger, dangerContainer = Color(0xFFFADEDA),
    gold = Gold, warning = Warning,
    heroStart = Navy, heroEnd = NavyLight,
    cardBorder = Color(0xFFE3E8EF), chartTrack = Color(0xFFE6EBF2),
)

private val DarkExtended = ExtendedColors(
    success = SuccessDark, onSuccess = Color(0xFF00210F), successContainer = Color(0xFF173A2A),
    danger = DangerDark, dangerContainer = Color(0xFF4A1C17),
    gold = GoldLight, warning = Color(0xFFFFB84D),
    heroStart = Color(0xFF0E2547), heroEnd = Color(0xFF1B3B68),
    cardBorder = Color(0xFF1F2F48), chartTrack = Color(0xFF1C2A40),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6F5),
    onPrimaryContainer = Navy,
    secondary = Color(0xFFA6801A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7ECCB),
    onSecondaryContainer = Color(0xFF3B2C00),
    tertiary = Sky,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9E8F8),
    onTertiaryContainer = Color(0xFF0A2A4A),
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF0E1726),
    surface = Color(0xFFF4F6F9),
    onSurface = Color(0xFF0E1726),
    surfaceVariant = Color(0xFFE8ECF2),
    onSurfaceVariant = Color(0xFF4F5B6D),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0F3F7),
    surfaceContainerHighest = Color(0xFFE8ECF2),
    outline = Color(0xFFC3CBD6),
    outlineVariant = Color(0xFFE1E6ED),
    error = Danger,
    onError = Color.White,
    errorContainer = Color(0xFFFADEDA),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4CBEF),
    onPrimary = Navy,
    primaryContainer = NavyLight,
    onPrimaryContainer = Color(0xFFDCE6F5),
    secondary = GoldLight,
    onSecondary = Color(0xFF2B2000),
    secondaryContainer = Color(0xFF4A3B0C),
    onSecondaryContainer = Color(0xFFF7ECCB),
    tertiary = Color(0xFF8FC1F2),
    onTertiary = Color(0xFF00213F),
    tertiaryContainer = Color(0xFF14365A),
    onTertiaryContainer = Color(0xFFD9E8F8),
    background = Color(0xFF070F1D),
    onBackground = Color(0xFFE3E8F0),
    surface = Color(0xFF070F1D),
    onSurface = Color(0xFFE3E8F0),
    surfaceVariant = Color(0xFF172741),
    onSurfaceVariant = Color(0xFFA9B6C8),
    surfaceContainerLowest = Color(0xFF050B16),
    surfaceContainerLow = Color(0xFF0D182A),
    surfaceContainer = Color(0xFF0F1C31),
    surfaceContainerHigh = Color(0xFF15243C),
    surfaceContainerHighest = Color(0xFF1B2C47),
    outline = Color(0xFF3A4C66),
    outlineVariant = Color(0xFF22324B),
    error = DangerDark,
    onError = Color(0xFF410002),
    errorContainer = Color(0xFF4A1C17),
    onErrorContainer = Color(0xFFFFDAD4),
)

private val base = Typography()
private val Sans = FontFamily.SansSerif

private val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = Sans, fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Sans, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Sans, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Sans, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = Sans, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = Sans, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = Sans, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = Sans, lineHeight = 26.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = Sans),
    labelLarge = base.labelLarge.copy(fontFamily = Sans, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = Sans, letterSpacing = 0.6.sp),
)

/** Small all-caps label used for section eyebrows ("YOUR PROGRESS"). */
val EyebrowStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CabinCrewTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtendedColors provides if (darkTheme) DarkExtended else LightExtended) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object AppTheme {
    val extended: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
