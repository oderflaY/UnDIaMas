package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.model.MoodEntry
import com.eter.undiamas.core.domain.repository.MoodRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.map

private const val COLECCION_USUARIOS = "usuarios"
private const val SUBCOLECCION_ANIMOS = "animos"

/** Adaptador real sobre Cloud Firestore: `/usuarios/{uid}/animos`, acotado siempre al uid recibido. */
class MoodRepositoryImpl : MoodRepository {
    private val firestore = Firebase.firestore

    private fun animosDe(uid: String) =
        firestore.collection(COLECCION_USUARIOS).document(uid).collection(SUBCOLECCION_ANIMOS)

    override fun observeRecent(uid: String, limit: Int) =
        animosDe(uid)
            .orderBy("fecha", Direction.DESCENDING)
            .limit(limit)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc -> doc.data<MoodEntryDoc>().toMoodEntry(id = doc.id, uid = uid) }
            }

    override suspend fun add(uid: String, entry: MoodEntry): String {
        val ref = animosDe(uid).add(entry.toMoodEntryDoc())
        return ref.id
    }

    override suspend fun deleteAll(uid: String) {
        animosDe(uid).get().documents.forEach { it.reference.delete() }
    }
}
