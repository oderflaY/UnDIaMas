package com.eter.undiamas.core.presentation.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Degradado de marca a 45°, usado en las tarjetas hero de racha e inicio. */
val PrimaryVioletBrush: Brush
    get() = Brush.linearGradient(
        colors = listOf(PrimaryVioletStart, PrimaryVioletEnd),
        start = Offset.Zero,
        end = Offset.Infinite, // diagonal ≈ 45°
    )

val SavingsBrush: Brush
    get() = Brush.linearGradient(listOf(SavingsGoldStart, SavingsGoldEnd))

val StatsBrush: Brush
    get() = Brush.linearGradient(listOf(AccentStats, PrimaryVioletStart))

val EmergencyBrush: Brush
    get() = Brush.linearGradient(listOf(EmergencyCoralStart, EmergencyCoralEnd))

val AssistantBrush: Brush
    get() = Brush.linearGradient(listOf(AssistantMagentaStart, AssistantMagentaEnd))

/** Degradado azul→verde de la barra de progreso del check-in. */
val CheckInProgressBrush: Brush
    get() = Brush.horizontalGradient(listOf(AnswerBlue, RiskGreen))

fun accentBrush(accent: Color): Brush =
    Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.68f)))

fun glowBrush(accent: Color): Brush =
    Brush.radialGradient(listOf(accent.copy(alpha = 0.28f), Color.Transparent))
