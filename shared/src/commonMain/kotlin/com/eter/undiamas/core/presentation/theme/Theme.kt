package com.eter.undiamas.core.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = PrimaryVioletStart,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryVioletEnd,
    onPrimaryContainer = TextPrimary,
    secondary = RiskGreen,
    onSecondary = BackgroundDark,
    secondaryContainer = SurfaceDark,
    onSecondaryContainer = TextPrimary,
    tertiary = RiskYellow,
    onTertiary = BackgroundDark,
    tertiaryContainer = SurfaceDark,
    onTertiaryContainer = TextPrimary,
    error = RiskRed,
    onError = TextPrimary,
    errorContainer = EmergencyCoralEnd,
    onErrorContainer = TextPrimary,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = BackgroundDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondary,
    outline = TextSecondary,
)

private val LightColors = lightColorScheme(
    primary = PrimaryVioletEnd,
    onPrimary = SurfaceLight,
    secondary = RiskGreen,
    onSecondary = SurfaceLight,
    tertiary = RiskYellow,
    onTertiary = InkLight,
    error = RiskRed,
    onError = SurfaceLight,
    background = SurfaceLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceLightDim,
    onSurfaceVariant = InkLightSecondary,
    outline = InkLightSecondary,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * El sistema de diseño está pensado en oscuro, así que ese es el valor por defecto.
 * El interruptor de Configuración es quien decide en tiempo de ejecución.
 */
@Composable
fun UnDiaMasTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
