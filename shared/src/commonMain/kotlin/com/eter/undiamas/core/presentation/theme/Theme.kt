package com.eter.undiamas.core.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Tema oscuro.
 *
 * Es la misma marca, no otra app: el púrpura se aclara para que siga siendo legible sobre
 * fondo oscuro, en vez de cambiarlo por otro color. `surface` queda un punto por encima de
 * `background` para que las tarjetas se distingan igual que en claro.
 */
private val DarkColors = darkColorScheme(
    primary = BrandPurpleLight,
    onPrimary = Color.White,
    primaryContainer = BrandPurpleDeep,
    onPrimaryContainer = Color.White,
    secondary = BrandPurpleLight,
    onSecondary = Color.White,
    tertiary = AccentOrange,
    onTertiary = InkStrong,
    tertiaryContainer = CanvasDarkElevated,
    onTertiaryContainer = TextPrimary,
    error = RiskRed,
    onError = Color.White,
    errorContainer = EmergencyCoralEnd,
    onErrorContainer = Color.White,
    background = CanvasDark,
    onBackground = TextPrimary,
    surface = CanvasDarkElevated,
    onSurface = TextPrimary,
    surfaceVariant = CanvasDarkElevated,
    onSurfaceVariant = InkMutedDark,
    outline = HairlineDark,
    outlineVariant = HairlineDark,
)

/**
 * Tema claro, el principal.
 *
 * `surface` es blanco puro y `background` el gris #F8F9FA: esa diferencia mínima es lo que
 * hace que las tarjetas blancas se despeguen del fondo sin necesidad de bordes.
 */
private val LightColors = lightColorScheme(
    primary = BrandPurple,
    onPrimary = Color.White,
    primaryContainer = BrandPurpleLight,
    onPrimaryContainer = Color.White,
    secondary = BrandPurpleDeep,
    onSecondary = Color.White,
    tertiary = AccentOrange,
    onTertiary = Color.White,
    tertiaryContainer = AccentCream,
    onTertiaryContainer = InkStrong,
    error = RiskRed,
    onError = Color.White,
    background = CanvasLight,
    onBackground = InkStrong,
    surface = Color.White,
    onSurface = InkStrong,
    surfaceVariant = CanvasLight,
    onSurfaceVariant = InkMuted,
    outline = HairlineLight,
    outlineVariant = HairlineLight,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Por defecto sigue al teléfono.
 *
 * Quien tiene el modo oscuro puesto en el sistema lo tiene puesto por algo — de noche, por
 * la vista, por batería — y no debería tener que repetírselo a cada app.
 */
@Composable
fun UnDiaMasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
