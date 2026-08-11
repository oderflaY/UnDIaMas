package com.eter.undiamas.core.presentation

/**
 * Explicacion util de un fallo de arranque.
 *
 * Decirle "revisa tu internet" a alguien cuya conexion funciona lo manda a buscar el
 * problema donde no esta. Los fallos tipicos al conectar con este backend son de
 * direccion o de configuracion, no del telefono, y conviene nombrarlos por lo que son.
 */
data class StartupDiagnosis(
    val title: String,
    val advice: String,
    val technicalDetail: String,
)

fun diagnoseStartupError(rawMessage: String?): StartupDiagnosis {
    val message = rawMessage.orEmpty()
    val detail = message.ifBlank { "Error desconocido al conectar con el servidor." }

    return when {
        // El error numero uno al empezar: en el emulador, localhost es el propio emulador.
        message.contains("Connection refused", ignoreCase = true) ||
            message.contains("ConnectException", ignoreCase = true) ||
            message.contains("Failed to connect", ignoreCase = true) ->
            StartupDiagnosis(
                title = "El servidor no responde",
                advice = "Comprueba que el backend esté corriendo y que la dirección sea la " +
                    "correcta. Desde el emulador de Android hay que usar 10.0.2.2, no " +
                    "localhost; desde un teléfono, la IP de tu computadora en la wifi. " +
                    "Puedes cambiarla en Configuración → Servidor.",
                technicalDetail = detail,
            )

        // Android bloquea http:// salvo que el manifiesto lo permita.
        message.contains("CLEARTEXT", ignoreCase = true) ->
            StartupDiagnosis(
                title = "Android bloqueó la conexión",
                advice = "El servidor habla http:// sin cifrar y el sistema no lo permite. " +
                    "En desarrollo se habilita en el manifiesto; en producción hay que " +
                    "usar https://.",
                technicalDetail = detail,
            )

        message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("UnknownHost", ignoreCase = true) ->
            StartupDiagnosis(
                title = "No se encuentra el servidor",
                advice = "La dirección del backend no corresponde a ninguna máquina " +
                    "alcanzable desde aquí. Revísala en Configuración → Servidor.",
                technicalDetail = detail,
            )

        message.contains("unauthenticated", ignoreCase = true) ||
            message.contains("sesión expiró", ignoreCase = true) ->
            StartupDiagnosis(
                title = "Tu sesión expiró",
                advice = "Vuelve a entrar con tu correo y contraseña. Tus datos siguen " +
                    "guardados en el servidor.",
                technicalDetail = detail,
            )

        message.contains("network", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("conexión", ignoreCase = true) ->
            StartupDiagnosis(
                title = "Sin conexión",
                advice = "Revisa tu conexión a internet e inténtalo de nuevo.",
                technicalDetail = detail,
            )

        else ->
            StartupDiagnosis(
                title = "No pudimos conectar",
                advice = "Ocurrió un problema al conectar con el servidor. Inténtalo de nuevo.",
                technicalDetail = detail,
            )
    }
}
