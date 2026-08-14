package com.eter.undiamas.core.domain.repository

/** Quien esta usando la app ahora mismo. */
data class Session(
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String = "patient",
) {
    val isTherapist: Boolean get() = role == "therapist"
}

/**
 * Puerto de autenticacion contra el backend propio.
 *
 * Ya no hay sesion anonima: el servidor solo emite tokens a cuentas con correo y
 * contraseña. A cambio, los datos dejan de estar atados a la instalacion — quien cambia de
 * telefono recupera su historial, que en una app de recuperacion pesa mas que ahorrarse un
 * paso en el alta.
 */
interface AuthRepository {
    /** Recupera la sesion guardada al abrir la app, o null si toca pedir credenciales. */
    suspend fun restore(): Session?

    suspend fun register(email: String, password: String, displayName: String): Session

    suspend fun login(email: String, password: String): Session

    /** Cierra la sesion en todos los dispositivos e invalida los tokens guardados. */
    suspend fun logout()

    /**
     * Pide un codigo de recuperacion al correo indicado.
     *
     * Lanza [com.eter.undiamas.core.data.api.ApiException] con `isServiceMissing` si el
     * servidor no tiene correo configurado, que es un caso normal y no un fallo.
     */
    suspend fun requestPasswordReset(email: String)

    /** Cambia la contraseña con el codigo recibido por correo. */
    suspend fun resetPassword(email: String, code: String, newPassword: String)
}
