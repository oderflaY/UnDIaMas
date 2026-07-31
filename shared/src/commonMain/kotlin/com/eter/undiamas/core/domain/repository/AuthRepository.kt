package com.eter.undiamas.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Puerto de autenticacion. El modo por defecto de esta app es la sesion anonima
 * (Fase 3 · 07): cada instalacion obtiene un uid propio sin pedir registro.
 */
interface AuthRepository {
    val currentUserId: Flow<String?>

    suspend fun signInAnonymously(): String

    /**
     * Vincula la sesion anonima actual a un correo/contraseña (registro), conservando el
     * mismo uid y por tanto todos los datos ya guardados. Si no hay sesion anonima activa
     * se comporta como un registro nuevo.
     */
    suspend fun linkAnonymousWithEmail(email: String, password: String): String

    /** Inicia sesion con correo/contraseña. Devuelve el uid resultante. */
    suspend fun signInWithEmail(email: String, password: String): String

    suspend fun signOut()
}
