package com.eter.undiamas.core.data.api

import kotlinx.serialization.Serializable

/** Cuerpo de error del backend: siempre estas dos claves, en todas las rutas. */
@Serializable
data class ApiErrorBody(
    val error: String = "unknown",
    val message: String = "",
)

/**
 * Codigos estables del campo `error`. Se decide con estos, nunca con el texto de `message`,
 * que el backend deja claro que es para depurar y no para enseñar a la persona.
 */
object ApiErrorCode {
    const val INVALID_ARGUMENT = "invalid-argument"
    const val UNAUTHENTICATED = "unauthenticated"
    const val FORBIDDEN = "forbidden"
    const val NOT_FOUND = "not-found"
    const val EMAIL_TAKEN = "email-taken"
    const val RATE_LIMITED = "rate-limited"
    /** El servidor esta levantado pero le falta una pieza: hoy, el SMTP de recuperacion. */
    const val UNAVAILABLE = "unavailable"
    const val NETWORK = "network"
}

/**
 * Fallo de una llamada al backend.
 *
 * [code] es lo que se mira para decidir; [userMessage] es lo unico que puede llegar a la
 * pantalla. En una app que acompaña a alguien en recuperacion, un volcado tecnico en
 * mitad de una crisis es ruido en el peor momento posible.
 */
class ApiException(
    val code: String,
    val status: Int,
    val debugMessage: String,
) : Exception("[$status $code] $debugMessage") {

    val isAuthExpired: Boolean get() = code == ApiErrorCode.UNAUTHENTICATED

    /**
     * La ruta existe en la app pero no en este servidor.
     *
     * Pasa de verdad: la recuperacion por correo solo se monta si hay SMTP configurado, y
     * el chat solo si hay clave de Gemini. La app tiene que saber distinguir "no existe
     * esto" de "fallo la llamada", porque lo que le dice a la persona es distinto.
     */
    val isServiceMissing: Boolean get() = status == 404 || status == 503

    val userMessage: String
        get() = when (code) {
            ApiErrorCode.UNAUTHENTICATED -> "Tu sesión expiró. Vuelve a entrar."
            ApiErrorCode.FORBIDDEN -> "Esta cuenta no puede hacer eso."
            ApiErrorCode.NOT_FOUND -> "Eso ya no está disponible."
            ApiErrorCode.EMAIL_TAKEN -> "Ese correo ya tiene una cuenta."
            ApiErrorCode.RATE_LIMITED -> "Vas muy rápido. Espera un momento."
            ApiErrorCode.UNAVAILABLE ->
                "Ese servicio no está disponible todavía. Escríbenos y te ayudamos a mano."
            ApiErrorCode.INVALID_ARGUMENT -> "Revisa los datos e inténtalo otra vez."
            ApiErrorCode.NETWORK -> "No hay conexión con el servidor."
            else -> "Algo salió mal. Inténtalo otra vez."
        }
}

/** Traduce el error tecnico del cliente HTTP a algo que la app pueda mostrar. */
fun Throwable.toUserMessage(): String = when (this) {
    is ApiException -> userMessage
    else -> "No hay conexión con el servidor."
}
