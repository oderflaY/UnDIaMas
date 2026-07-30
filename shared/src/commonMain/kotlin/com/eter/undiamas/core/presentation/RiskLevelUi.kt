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

val RiskLevel.brush: Brush
    get() = when (this) {
        RiskLevel.VERDE -> Brush.linearGradient(listOf(RiskGreen, Color(0xFF4ADE80)))
        RiskLevel.AMARILLO -> Brush.linearGradient(listOf(RiskYellow, Color(0xFFFBBF24)))
        RiskLevel.ROJO -> Brush.linearGradient(listOf(RiskRed, Color(0xFFF87171)))
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
