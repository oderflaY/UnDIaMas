package com.eter.undiamas.core.data

/**
 * La ruta se inyecta desde la Activity al arrancar: resolverla necesita un Context,
 * y una `expect fun` no puede recibirlo sin ensuciar la firma común.
 */
internal var androidPreferencesPath: String? = null

fun initPreferencesPath(path: String) {
    androidPreferencesPath = path
}

actual fun preferencesFilePath(): String =
    androidPreferencesPath ?: error("Llama a initPreferencesPath() antes de usar UserPreferences")
