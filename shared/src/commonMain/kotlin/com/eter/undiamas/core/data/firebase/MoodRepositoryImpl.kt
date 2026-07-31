package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.model.Mood
import com.eter.undiamas.core.domain.model.MoodEntry
import com.eter.undiamas.core.domain.repository.MoodRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.map

/** Registro de animo sobre la coleccion plana `/mood_logs`, filtrada por `userId`. */
class MoodRepositoryImpl : MoodRepository {
    private val firestore = Firebase.firestore

    private fun consultaDe(uid: String) =
        firestore.collection(Colecciones.MOOD_LOGS).where { "userId" equalTo uid }

    override fun observeRecent(uid: String, limit: Int) =
        consultaDe(uid).snapshots.map { snapshot ->
            snapshot.documents
                .map { doc ->
                    val data = doc.data<MoodLogDoc>()
                    MoodEntry(
                        id = doc.id,
                        userId = data.userId,
                        // Un animo desconocido cae a NEUTRAL en lugar de romper la lectura.
                        mood = runCatching { Mood.valueOf(data.mood) }.getOrDefault(Mood.NEUTRAL),
                        registeredAt = data.timestamp.toInstant(),
                    )
                }
                .sortedByDescending { it.registeredAt }
                .take(limit)
        }

    override suspend fun add(uid: String, entry: MoodEntry): String {
        val ref = firestore.collection(Colecciones.MOOD_LOGS).add(
            MoodLogDoc(
                userId = uid,
                mood = entry.mood.name,
                timestamp = entry.registeredAt.toFirestoreTimestamp(),
            ),
        )
        return ref.id
    }

    override suspend fun deleteAll(uid: String) {
        consultaDe(uid).get().documents.forEach { it.reference.delete() }
    }
}
