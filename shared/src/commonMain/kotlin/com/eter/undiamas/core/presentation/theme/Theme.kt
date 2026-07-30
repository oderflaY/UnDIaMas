package com.eter.undiamas.core.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Violet60,
    onPrimary = Surface99,
    primaryContainer = Violet95,
    onPrimaryContainer = Violet15,
    secondary = Mint40,
    onSecondary = Surface99,
    secondaryContainer = Mint95,
    onSecondaryContainer = Mint15,
    tertiary = Amber40,
    onTertiary = Surface99,
    tertiaryContainer = Amber95,
    onTertiaryContainer = Amber15,
    error = Coral40,
    onError = Surface99,
    errorContainer = Coral95,
    onErrorContainer = Coral15,
    background = Surface99,
    onBackground = Ink10,
    surface = Surface99,
    onSurface = Ink10,
    surfaceVariant = SurfaceDim,
    onSurfaceVariant = Ink30,
)

private val DarkColors = darkColorScheme(
    primary = Violet80,
    onPrimary = Violet15,
    primaryContainer = Violet40,
    onPrimaryContainer = Violet95,
    secondary = Mint60,
    onSecondary = Mint15,
    secondaryContainer = Mint40,
    onSecondaryContainer = Mint95,
    tertiary = Amber60,
    onTertiary = Amber15,
    tertiaryContainer = Amber40,
    onTertiaryContainer = Amber95,
    error = Coral60,
    onError = Coral15,
    errorContainer = Coral40,
    onErrorContainer = Coral95,
    background = DarkSurface,
    onBackground = DarkInk90,
    surface = DarkSurface,
    onSurface = DarkInk90,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkInk90,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun UnDiaMasTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
