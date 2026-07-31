package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.repository.CheckInRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.map

private const val COLECCION_USUARIOS = "usuarios"
private const val SUBCOLECCION_CHECKINS = "checkins"

/**
 * Adaptador real sobre Cloud Firestore (Fase 3 · 07): `/usuarios/{uid}/checkins`.
 * Todas las consultas cuelgan del documento del uid recibido: no hay forma de leer
 * check-ins de otro usuario desde este repositorio.
 */
class CheckInRepositoryImpl : CheckInRepository {
    private val firestore = Firebase.firestore

    private fun checkInsOf(uid: String) =
        firestore.collection(COLECCION_USUARIOS).document(uid).collection(SUBCOLECCION_CHECKINS)

    override fun observeRecent(uid: String, limit: Int) =
        checkInsOf(uid)
            .orderBy("fechaHora", Direction.DESCENDING)
            .limit(limit)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc -> doc.data<CheckInDoc>().toCheckInEntry(id = doc.id, uid = uid) }
            }

    override suspend fun add(uid: String, entry: CheckInEntry): String {
        val ref = checkInsOf(uid).add(entry.toCheckInDoc())
        return ref.id
    }

    override suspend fun deleteAll(uid: String) {
        val docs = checkInsOf(uid).get().documents
        docs.forEach { it.reference.delete() }
    }
}
