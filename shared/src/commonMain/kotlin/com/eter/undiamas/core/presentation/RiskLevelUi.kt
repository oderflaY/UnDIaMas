package com.eter.undiamas.core.presentation

import androidx.compose.ui.graphics.Color
import com.eter.undiamas.core.domain.model.RiskLevel

val RiskLevel.color: Color
    get() = when (this) {
        RiskLevel.VERDE -> Color(0xFF2E7D32)
        RiskLevel.AMARILLO -> Color(0xFFF9A825)
        RiskLevel.ROJO -> Color(0xFFC62828)
    }

val RiskLevel.label: String
    get() = when (this) {
        RiskLevel.VERDE -> "Verde · Todo tranquilo"
        RiskLevel.AMARILLO -> "Amarillo · Hay un detonante"
        RiskLevel.ROJO -> "Rojo · Protocolo de emergencia"
    }
