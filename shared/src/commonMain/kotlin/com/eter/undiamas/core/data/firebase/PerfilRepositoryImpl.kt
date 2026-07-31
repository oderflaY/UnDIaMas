package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.model.UserProfile
import com.eter.undiamas.core.domain.repository.PerfilRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.map

private const val COLECCION_USUARIOS = "usuarios"

/**
 * Adaptador real sobre Cloud Firestore (Fase 3 · 07): `/usuarios/{uid}`.
 * Cada consulta esta acotada al uid recibido, nunca a un uid distinto al de la sesion.
 */
class PerfilRepositoryImpl : PerfilRepository {
    private val firestore = Firebase.firestore

    override fun observe(uid: String) =
        firestore.collection(COLECCION_USUARIOS).document(uid).snapshots.map { snapshot ->
            if (!snapshot.exists) null else snapshot.data<UsuarioDoc>().toUserProfile(uid)
        }

    override suspend fun save(uid: String, profile: UserProfile) {
        firestore.collection(COLECCION_USUARIOS).document(uid).set(profile.toUsuarioDoc())
    }

    override suspend fun delete(uid: String) {
        firestore.collection(COLECCION_USUARIOS).document(uid).delete()
    }
}
