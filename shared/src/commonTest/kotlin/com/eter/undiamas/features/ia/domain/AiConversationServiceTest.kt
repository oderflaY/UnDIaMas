package com.eter.undiamas.features.ia.domain

import com.eter.undiamas.core.domain.model.AiMessageRole
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.testing.FakeAiProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AiConversationServiceTest {
    @Test
    fun `el tono de la respuesta cambia segun el nivel de riesgo`() = runTest {
        val provider = FakeAiProvider()
        val service = AiConversationService(provider)

        val response = service.respond(prompt = "hola", riskLevel = RiskLevel.ROJO, history = emptyList())

        assertEquals(AiMessageRole.ASISTENTE, response.role)
        assertEquals(RiskLevel.ROJO, response.riskLevelContext)
    }

    @Test
    fun `el servicio reenvia el prompt original al proveedor`() = runTest {
        val provider = FakeAiProvider()
        val service = AiConversationService(provider)

        service.respond(prompt = "me siento en riesgo", riskLevel = RiskLevel.AMARILLO, history = emptyList())

        assertEquals("me siento en riesgo", provider.lastPrompt)
    }
}
