package com.eter.undiamas.core.domain.ai

import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.RiskLevel

/**
 * Puerto del proveedor de IA conversacional. La implementación real (Firebase AI Logic u otro
 * proveedor configurable) se resuelve mediante Factory/Facade en la capa data de Fase 2/3.
 */
interface AiProvider {
    suspend fun generateResponse(
        prompt: String,
        riskLevel: RiskLevel,
        history: List<AiMessage>,
    ): AiMessage
}
