package com.eter.undiamas.features.checkin.domain

import com.eter.undiamas.core.domain.model.RiskAssessment
import com.eter.undiamas.core.domain.model.RiskLevel

class RiskAssessor {
    fun assess(answers: Map<String, String>): RiskAssessment {
        val hayImpulso = answers["impulso_consumo"] == "si"
        val hayDetonante = answers["detonante_presente"] == "si"

        val riskLevel = when {
            hayImpulso -> RiskLevel.ROJO
            hayDetonante -> RiskLevel.AMARILLO
            else -> RiskLevel.VERDE
        }

        val recommendation = when (riskLevel) {
            RiskLevel.VERDE -> "Sigue así, refuerza tus hábitos positivos de hoy."
            RiskLevel.AMARILLO -> "Identifiquemos el detonante y busquemos una alternativa antes de que crezca."
            RiskLevel.ROJO -> "Activa tu protocolo de emergencia y contacta a tu persona de confianza."
        }

        return RiskAssessment(riskLevel = riskLevel, recommendation = recommendation)
    }
}
