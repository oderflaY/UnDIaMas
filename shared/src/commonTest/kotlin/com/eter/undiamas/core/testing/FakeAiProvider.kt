package com.eter.undiamas.core.testing

import com.eter.undiamas.core.domain.ai.AiProvider
import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.AiMessageRole
import com.eter.undiamas.core.domain.model.RiskLevel
import kotlinx.datetime.Instant

class FakeAiProvider(
    private val responsesByRiskLevel: Map<RiskLevel, String> = mapOf(
        RiskLevel.VERDE to "¡Vas muy bien, sigue así!",
        RiskLevel.AMARILLO to "Notemos qué detonante apareció y busquemos una alternativa.",
        RiskLevel.ROJO to "Activemos tu protocolo de emergencia ahora mismo.",
    ),
    private val now: Instant = Instant.fromEpochSeconds(0),
) : AiProvider {
    var lastPrompt: String? = null
        private set

    override suspend fun generateResponse(
        prompt: String,
        riskLevel: RiskLevel,
        history: List<AiMessage>,
    ): AiMessage {
        lastPrompt = prompt
        return AiMessage(
            id = "fake-${history.size}",
            userId = "fake-user",
            role = AiMessageRole.ASISTENTE,
            content = responsesByRiskLevel.getValue(riskLevel),
            riskLevelContext = riskLevel,
            sentAt = now,
        )
    }
}
