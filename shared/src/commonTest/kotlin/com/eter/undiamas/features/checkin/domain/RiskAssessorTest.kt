package com.eter.undiamas.features.checkin.domain

import com.eter.undiamas.core.domain.model.RiskLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class RiskAssessorTest {
    private val assessor = RiskAssessor()

    @Test
    fun `respuestas sin detonantes producen semaforo verde`() {
        val result = assessor.assess(mapOf("detonante_presente" to "no", "estado_animo" to "bien"))

        assertEquals(RiskLevel.VERDE, result.riskLevel)
    }

    @Test
    fun `un detonante detectado sin impulso de consumo produce semaforo amarillo`() {
        val result = assessor.assess(mapOf("detonante_presente" to "si", "impulso_consumo" to "no"))

        assertEquals(RiskLevel.AMARILLO, result.riskLevel)
    }

    @Test
    fun `impulso de consumo activo produce semaforo rojo`() {
        val result = assessor.assess(mapOf("detonante_presente" to "si", "impulso_consumo" to "si"))

        assertEquals(RiskLevel.ROJO, result.riskLevel)
    }
}
