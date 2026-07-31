package com.eter.undiamas.core.presentation

/**
 * Explicacion util de un fallo de arranque.
 *
 * Decirle "revisa tu internet" a alguien cuya conexion funciona lo manda a buscar el
 * problema donde no esta. Estos casos son de configuracion del backend, no del telefono,
 * y conviene nombrarlos por lo que son.
 */
data class StartupDiagnosis(
    val title: String,
    val advice: String,
    val technicalDetail: String,
)

fun diagnoseStartupError(rawMessage: String?): StartupDiagnosis {
    val message = rawMessage.orEmpty()
    val detail = message.ifBlank { "Error desconocido al iniciar sesión." }

    return when {
        // Auth no esta habilitado/provisionado en el proyecto de Firebase.
        message.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
            message.contains("ADMIN_RESTRICTED_OPERATION", ignoreCase = true) ->
            StartupDiagnosis(
                title = "Falta configurar el servidor",
                advice = "Tu conexión está bien. Falta habilitar el inicio de sesión anónimo " +
                    "en la consola de Firebase (Authentication → Sign-in method → Anónimo).",
                technicalDetail = detail,
            )

        message.contains("PERMISSION_DENIED", ignoreCase = true) ||
            message.contains("Missing or insufficient permissions", ignoreCase = true) ->
            StartupDiagnosis(
                title = "Sin permisos para leer tus datos",
                advice = "Faltan desplegar las reglas de Firestore del proyecto.",
                technicalDetail = detail,
            )

        message.contains("API key not valid", ignoreCase = true) ||
            message.contains("API_KEY", ignoreCase = true) ->
            StartupDiagnosis(
                title = "Configuración inválida",
                advice = "El archivo google-services.json no corresponde a este proyecto.",
                technicalDetail = detail,
            )

        message.contains("UNAVAILABLE", ignoreCase = true) ||
            message.contains("network", ignoreCase = true) ||
            message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ->
            StartupDiagnosis(
                title = "Sin conexión",
                advice = "Revisa tu conexión a internet e inténtalo de nuevo.",
                technicalDetail = detail,
            )

        else ->
            StartupDiagnosis(
                title = "No pudimos conectar",
                advice = "Ocurrió un problema al iniciar tu sesión. Inténtalo de nuevo.",
                technicalDetail = detail,
            )
    }
}
