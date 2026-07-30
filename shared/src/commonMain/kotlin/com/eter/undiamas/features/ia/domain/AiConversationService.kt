package com.eter.undiamas.features.ia.domain

import com.eter.undiamas.core.domain.ai.AiProvider
import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.RiskLevel

class AiConversationService(private val aiProvider: AiProvider) {
    suspend fun respond(prompt: String, riskLevel: RiskLevel, history: List<AiMessage>): AiMessage =
        aiProvider.generateResponse(prompt, riskLevel, history)
}
