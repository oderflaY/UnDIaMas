package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.model.RiskLevel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * El dominio nombra el semaforo en espanol y Firestore lo guarda en ingles. Esa
 * traduccion es el punto donde un descuido silencioso convertiria un ROJO en un VERDE,
 * asi que se prueba en ambos sentidos.
 */
class FirestoreMappersTest {
    @Test
    fun `cada nivel de riesgo sobrevive el viaje de ida y vuelta a Firestore`() {
        RiskLevel.entries.forEach { nivel ->
            assertEquals(nivel, nivel.toFirestoreCode().toRiskLevelOrGreen())
        }
    }

    @Test
    fun `los codigos en ingles de la base se leen como el nivel correcto`() {
        assertEquals(RiskLevel.VERDE, "GREEN".toRiskLevelOrGreen())
        assertEquals(RiskLevel.AMARILLO, "YELLOW".toRiskLevelOrGreen())
        assertEquals(RiskLevel.ROJO, "RED".toRiskLevelOrGreen())
    }

    @Test
    fun `los codigos en espanol de datos antiguos siguen leyendose`() {
        assertEquals(RiskLevel.AMARILLO, "AMARILLO".toRiskLevelOrGreen())
        assertEquals(RiskLevel.ROJO, "rojo".toRiskLevelOrGreen())
    }

    @Test
    fun `un codigo desconocido cae a verde y no inventa una emergencia`() {
        // Un dato corrupto no debe activar el protocolo de emergencia de nadie.
        assertEquals(RiskLevel.VERDE, "".toRiskLevelOrGreen())
        assertEquals(RiskLevel.VERDE, "MORADO".toRiskLevelOrGreen())
    }
}
