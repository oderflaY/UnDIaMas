package com.eter.undiamas.core.data.local

/**
 * La ruta se inyecta desde la Activity al arrancar: resolverla necesita un Context, y una
 * `expect fun` no puede recibirlo sin ensuciar la firma común. Mismo patrón que usa
 * [com.eter.undiamas.core.data.preferencesFilePath].
 */
internal var androidDatabasePath: String? = null

fun initDatabasePath(path: String) {
    androidDatabasePath = path
}

actual fun databaseFilePath(): String =
    androidDatabasePath ?: error("Llama a initDatabasePath() antes de usar la base de datos")
