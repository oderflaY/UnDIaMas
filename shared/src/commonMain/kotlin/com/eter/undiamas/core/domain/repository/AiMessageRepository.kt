package com.eter.undiamas.core.domain.repository

import com.eter.undiamas.core.domain.model.AiMessage
import kotlinx.coroutines.flow.Flow

/** Puerto de persistencia del chat con la IA, aislado por uid en `/usuarios/{uid}/mensajesIA`. */
interface AiMessageRepository {
    fun observeRecent(uid: String, limit: Int = 200): Flow<List<AiMessage>>

    suspend fun add(uid: String, message: AiMessage): String

    suspend fun deleteAll(uid: String)
}
