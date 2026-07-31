package com.eter.undiamas.core.domain.repository

import com.eter.undiamas.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** Puerto de persistencia del perfil, aislado por uid en `/usuarios/{uid}`. */
interface PerfilRepository {
    fun observe(uid: String): Flow<UserProfile?>

    suspend fun save(uid: String, profile: UserProfile)

    /** Derecho al olvido: borra el documento de perfil del usuario. */
    suspend fun delete(uid: String)
}
