package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.repository.DiaryRepository
import com.eter.undiamas.features.diario.domain.DiaryEntry
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.map

/**
 * Diario sobre la coleccion plana `/journal_entries`.
 *
 * El filtro por `userId` no es opcional: es lo unico que aisla el diario de una persona
 * del de otra, tanto en la consulta como en la regla de seguridad.
 */
class DiaryRepositoryImpl : DiaryRepository {
    private val firestore = Firebase.firestore

    private fun consultaDe(uid: String) =
        firestore.collection(Colecciones.JOURNAL_ENTRIES).where { "userId" equalTo uid }

    override fun observeRecent(uid: String, limit: Int) =
        consultaDe(uid).snapshots.map { snapshot ->
            snapshot.documents
                .map { doc ->
                    val data = doc.data<JournalEntryDoc>()
                    DiaryEntry(
                        id = doc.id,
                        userId = data.userId,
                        createdAt = data.createdAt.toInstant(),
                        text = data.content,
                    )
                }
                .sortedByDescending { it.createdAt }
                .take(limit)
        }

    override suspend fun add(uid: String, entry: DiaryEntry): String {
        val ref = firestore.collection(Colecciones.JOURNAL_ENTRIES).add(
            JournalEntryDoc(
                userId = uid,
                content = entry.text,
                createdAt = entry.createdAt.toFirestoreTimestamp(),
            ),
        )
        return ref.id
    }

    override suspend fun deleteAll(uid: String) {
        consultaDe(uid).get().documents.forEach { it.reference.delete() }
    }
}
