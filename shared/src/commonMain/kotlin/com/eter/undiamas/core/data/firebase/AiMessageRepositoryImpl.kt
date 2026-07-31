package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.AiMessageRole
import com.eter.undiamas.core.domain.repository.AiMessageRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.map

/**
 * Conversacion con el asistente sobre la coleccion plana `/ai_messages`.
 *
 * Cada documento creado aqui dispara `onAiMessageCreated` en Cloud Functions para la
 * trazabilidad de `ai_logs`.
 */
class AiMessageRepositoryImpl : AiMessageRepository {
    private val firestore = Firebase.firestore

    private fun consultaDe(uid: String) =
        firestore.collection(Colecciones.AI_MESSAGES).where { "userId" equalTo uid }

    override fun observeRecent(uid: String, limit: Int) =
        consultaDe(uid).snapshots.map { snapshot ->
            snapshot.documents
                .map { doc ->
                    val data = doc.data<AiMessageFlatDoc>()
                    AiMessage(
                        id = doc.id,
                        userId = data.userId,
                        role = runCatching { AiMessageRole.valueOf(data.role) }
                            .getOrDefault(AiMessageRole.ASISTENTE),
                        content = data.content,
                        riskLevelContext = data.riskLevelContext?.toRiskLevelOrGreen(),
                        sentAt = data.timestamp.toInstant(),
                    )
                }
                .sortedByDescending { it.sentAt }
                .take(limit)
        }

    override suspend fun add(uid: String, message: AiMessage): String {
        val ref = firestore.collection(Colecciones.AI_MESSAGES).add(
            AiMessageFlatDoc(
                userId = uid,
                role = message.role.name,
                content = message.content,
                riskLevelContext = message.riskLevelContext?.toFirestoreCode(),
                timestamp = message.sentAt.toFirestoreTimestamp(),
            ),
        )
        return ref.id
    }

    override suspend fun deleteAll(uid: String) {
        consultaDe(uid).get().documents.forEach { it.reference.delete() }
    }
}
