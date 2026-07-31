package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.Trigger
import com.eter.undiamas.core.domain.repository.CheckInRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.map

/**
 * Check-ins sobre la coleccion plana `/check_ins`.
 *
 * El aislamiento sale del campo `userId`, no de la ruta, asi que el filtro es obligatorio:
 * sin el, la regla rechaza la consulta entera aunque el usuario tenga check-ins propios.
 */
class CheckInRepositoryImpl : CheckInRepository {
    private val firestore = Firebase.firestore

    private fun consultaDe(uid: String) =
        firestore.collection(Colecciones.CHECK_INS).where { "userId" equalTo uid }

    override fun observeRecent(uid: String, limit: Int) =
        consultaDe(uid).snapshots.map { snapshot ->
            snapshot.documents
                .map { doc -> doc.data<CheckInFlatDoc>().toCheckInEntry(doc.id) }
                .sortedByDescending { it.answeredAt }
                .take(limit)
        }

    override suspend fun add(uid: String, entry: CheckInEntry): String {
        val ref = firestore.collection(Colecciones.CHECK_INS).add(entry.toFlatDoc(uid))
        return ref.id
    }

    override suspend fun deleteAll(uid: String) {
        consultaDe(uid).get().documents.forEach { it.reference.delete() }
    }
}

private fun CheckInEntry.toFlatDoc(uid: String) = CheckInFlatDoc(
    userId = uid,
    riskLevel = riskLevel.toFirestoreCode(),
    cravingLevel = urgeIntensity,
    mood = answers["estado_animo"].orEmpty(),
    triggers = triggers.map { it.name },
    note = note,
    answers = answers,
    timestamp = answeredAt.toFirestoreTimestamp(),
)

private fun CheckInFlatDoc.toCheckInEntry(id: String) = CheckInEntry(
    id = id,
    userId = userId,
    answeredAt = timestamp.toInstant(),
    answers = answers,
    riskLevel = riskLevel.toRiskLevelOrGreen(),
    // Un detonante que no reconozcamos (dato viejo, o escrito por otra version) no debe
    // tumbar la lectura completa del historial: se descarta solo ese valor.
    triggers = triggers.mapNotNull { name ->
        runCatching { Trigger.valueOf(name) }.getOrNull()
    }.toSet(),
    urgeIntensity = cravingLevel,
    note = note,
)
