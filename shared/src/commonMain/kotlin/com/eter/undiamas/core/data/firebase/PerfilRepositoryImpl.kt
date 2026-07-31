package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.model.SupportRole
import com.eter.undiamas.core.domain.model.TrustedContact
import com.eter.undiamas.core.domain.model.UserProfile
import com.eter.undiamas.core.domain.repository.PerfilRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable

/**
 * Perfil sobre el esquema plano real: la identidad vive en `/users/{uid}` y los datos de
 * sobriedad en `/sobriety_trackers/{uid}`, que es como estan guardados en la base.
 *
 * Por eso el perfil del dominio se arma combinando DOS documentos. Se emite en cuanto
 * existe el de identidad; si el tracker aun no se creo, la racha arranca en el momento
 * actual en vez de dejar la pantalla vacia.
 */
class PerfilRepositoryImpl : PerfilRepository {
    private val firestore = Firebase.firestore

    private fun userDoc(uid: String) =
        firestore.collection(Colecciones.USERS).document(uid)

    private fun trackerDoc(uid: String) =
        firestore.collection(Colecciones.SOBRIETY_TRACKERS).document(uid)

    override fun observe(uid: String) =
        combine(userDoc(uid).snapshots, trackerDoc(uid).snapshots) { userSnap, trackerSnap ->
            if (!userSnap.exists) {
                null
            } else {
                val user = userSnap.data<UserDoc>()
                val tracker = if (trackerSnap.exists) trackerSnap.data<SobrietyTrackerDoc>() else null
                val contactos = user.contactosEmergencia.map { it.toDomain() }
                UserProfile(
                    userId = uid,
                    displayName = user.displayName,
                    sobrietyStartDate = tracker?.startDate.toInstant(),
                    previousDailyExpense = tracker?.dailySavingsRate ?: 0.0,
                    recordStreakSeconds = user.recordRachaSegundos,
                    trustedContact = contactos.firstOrNull(),
                    supportNetwork = contactos.drop(1),
                    personalWhy = user.porQuePersonal,
                )
            }
        }

    override suspend fun save(uid: String, profile: UserProfile) {
        // merge: el documento de usuario tambien lleva `role`, `email` y `createdAt`, que
        // este repositorio no debe pisar. `role` ademas lo bloquean las reglas.
        val contactos = listOfNotNull(profile.trustedContact) + profile.supportNetwork
        userDoc(uid).set(
            PerfilEditable(
                uid = uid,
                displayName = profile.displayName,
                contactosEmergencia = contactos.map { it.toDoc() },
                porQuePersonal = profile.personalWhy,
                recordRachaSegundos = profile.recordStreakSeconds,
                lastLoginAt = nowTimestamp(),
            ),
            merge = true,
        )
        trackerDoc(uid).set(
            SobrietyTrackerDoc(
                userId = uid,
                startDate = profile.sobrietyStartDate.toFirestoreTimestamp(),
                dailySavingsRate = profile.previousDailyExpense,
                lastStatusUpdate = nowTimestamp(),
            ),
            merge = true,
        )
    }

    override suspend fun delete(uid: String) {
        userDoc(uid).delete()
        trackerDoc(uid).delete()
    }
}

/**
 * Subconjunto de `/users/{uid}` que la app SI puede escribir. Se serializa con `merge`,
 * asi que los campos ausentes aqui (`role`, `email`, `createdAt`) quedan intactos. `role`
 * ademas lo bloquean las reglas: escribirlo seria un PERMISSION_DENIED.
 */
@Serializable
private data class PerfilEditable(
    val uid: String,
    val displayName: String,
    val contactosEmergencia: List<ContactoEmergenciaDoc>,
    val porQuePersonal: String,
    val recordRachaSegundos: Long,
    val lastLoginAt: Timestamp,
)

private fun TrustedContact.toDoc() =
    ContactoEmergenciaDoc(nombre = name, telefono = phone, rol = role.name)

private fun ContactoEmergenciaDoc.toDomain() = TrustedContact(
    name = nombre,
    phone = telefono,
    role = runCatching { SupportRole.valueOf(rol) }.getOrDefault(SupportRole.FAMILIAR),
)
