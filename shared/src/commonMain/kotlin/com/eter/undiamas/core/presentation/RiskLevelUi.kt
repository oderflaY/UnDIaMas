package com.eter.undiamas.core.presentation

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.presentation.theme.RiskGreen
import com.eter.undiamas.core.presentation.theme.RiskRed
import com.eter.undiamas.core.presentation.theme.RiskYellow

val RiskLevel.color: Color
    get() = when (this) {
        RiskLevel.VERDE -> RiskGreen
        RiskLevel.AMARILLO -> RiskYellow
        RiskLevel.ROJO -> RiskRed
    }

/** Degradado de fondo del resultado: calma, precaución y crisis. */
val RiskLevel.brush: Brush
    get() = when (this) {
        RiskLevel.VERDE -> Brush.linearGradient(listOf(RiskGreen, Color(0xFF059669)))
        RiskLevel.AMARILLO -> Brush.linearGradient(listOf(RiskYellow, Color(0xFFD97706)))
        RiskLevel.ROJO -> Brush.linearGradient(listOf(RiskRed, Color(0xFFB91C1C)))
    }

val RiskLevel.label: String
    get() = when (this) {
        RiskLevel.VERDE -> "Todo tranquilo"
        RiskLevel.AMARILLO -> "Hay un detonante"
        RiskLevel.ROJO -> "Protocolo de emergencia"
    }

val RiskLevel.emoji: String
    get() = when (this) {
        RiskLevel.VERDE -> "🌱"
        RiskLevel.AMARILLO -> "⚠️"
        RiskLevel.ROJO -> "🆘"
    }
