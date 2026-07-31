package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.repository.DiaryRepository
import com.eter.undiamas.features.diario.domain.DiaryEntry
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.map

private const val COLECCION_USUARIOS = "usuarios"
private const val SUBCOLECCION_DIARIO = "diario"

/** Adaptador real sobre Cloud Firestore: `/usuarios/{uid}/diario`, acotado siempre al uid recibido. */
class DiaryRepositoryImpl : DiaryRepository {
    private val firestore = Firebase.firestore

    private fun diarioDe(uid: String) =
        firestore.collection(COLECCION_USUARIOS).document(uid).collection(SUBCOLECCION_DIARIO)

    override fun observeRecent(uid: String, limit: Int) =
        diarioDe(uid)
            .orderBy("fecha", Direction.DESCENDING)
            .limit(limit)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc -> doc.data<DiaryEntryDoc>().toDiaryEntry(id = doc.id, uid = uid) }
            }

    override suspend fun add(uid: String, entry: DiaryEntry): String {
        val ref = diarioDe(uid).add(entry.toDiaryEntryDoc())
        return ref.id
    }

    override suspend fun deleteAll(uid: String) {
        diarioDe(uid).get().documents.forEach { it.reference.delete() }
    }
}
