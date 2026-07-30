package com.eter.undiamas.core.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Degradado de marca para las tarjetas "hero". */
val HeroBrush: Brush
    get() = Brush.linearGradient(listOf(Violet60, Color(0xFF9333EA), Mint60))

val SavingsBrush: Brush
    get() = Brush.linearGradient(listOf(AccentAhorro, Mint60))

val StatsBrush: Brush
    get() = Brush.linearGradient(listOf(AccentStats, Violet80))

val EmergencyBrush: Brush
    get() = Brush.linearGradient(listOf(Coral60, Color(0xFFFF8A5B)))

/** Degradado suave partiendo de un acento, para tarjetas de contenido. */
fun accentBrush(accent: Color): Brush =
    Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.68f)))

/** Halo translúcido para fondos detrás de contenido. */
fun glowBrush(accent: Color): Brush =
    Brush.radialGradient(listOf(accent.copy(alpha = 0.28f), Color.Transparent))
