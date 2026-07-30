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
    primary = Violet40,
    onPrimary = Neutral99,
    primaryContainer = Violet90,
    onPrimaryContainer = Violet20,
    secondary = Teal40,
    onSecondary = Neutral99,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal20,
    tertiary = Amber40,
    onTertiary = Neutral99,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber20,
    error = Rose40,
    onError = Neutral99,
    errorContainer = Rose90,
    onErrorContainer = Rose20,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Neutral95,
    onSurfaceVariant = Neutral30,
)

private val DarkColors = darkColorScheme(
    primary = Violet90,
    onPrimary = Violet20,
    primaryContainer = Violet40,
    onPrimaryContainer = Violet90,
    secondary = Teal90,
    onSecondary = Teal20,
    secondaryContainer = Teal40,
    onSecondaryContainer = Teal90,
    tertiary = Amber90,
    onTertiary = Amber20,
    tertiaryContainer = Amber40,
    onTertiaryContainer = Amber90,
    error = Rose90,
    onError = Rose20,
    errorContainer = Rose40,
    onErrorContainer = Rose90,
    background = Neutral10,
    onBackground = Neutral95,
    surface = Neutral10,
    onSurface = Neutral95,
    surfaceVariant = Neutral30,
    onSurfaceVariant = Neutral95,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun UnDiaMasTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        content = content,
    )
}
