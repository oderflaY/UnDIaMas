package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.repository.AiMessageRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.map

private const val COLECCION_USUARIOS = "usuarios"
private const val SUBCOLECCION_MENSAJES_IA = "mensajesIA"

/**
 * Adaptador real sobre Cloud Firestore: `/usuarios/{uid}/mensajesIA`, acotado siempre al
 * uid recibido. Cada documento creado aqui dispara `onAiMessageCreated` en Cloud Functions
 * para la trazabilidad de `logs_ia`.
 */
class AiMessageRepositoryImpl : AiMessageRepository {
    private val firestore = Firebase.firestore

    private fun mensajesDe(uid: String) =
        firestore.collection(COLECCION_USUARIOS).document(uid).collection(SUBCOLECCION_MENSAJES_IA)

    override fun observeRecent(uid: String, limit: Int) =
        mensajesDe(uid)
            .orderBy("fecha", Direction.DESCENDING)
            .limit(limit)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc -> doc.data<AiMessageDoc>().toAiMessage(id = doc.id, uid = uid) }
            }

    override suspend fun add(uid: String, message: AiMessage): String {
        val ref = mensajesDe(uid).add(message.toAiMessageDoc())
        return ref.id
    }

    override suspend fun deleteAll(uid: String) {
        mensajesDe(uid).get().documents.forEach { it.reference.delete() }
    }
}
