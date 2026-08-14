package com.eter.undiamas.core.data.api

/**
 * Direccion del backend. Una sola, fija y compilada dentro de la app.
 *
 * No se puede cambiar desde la app a proposito. Dejar que alguien reapunte la app a otro
 * servidor es dejar que le roben la sesion y el historial: bastaria con convencer a una
 * persona de escribir una direccion para que su historial de recaidas y su contrasena
 * acabaran en la maquina de otra.
 *
 * Es https porque la app publicada no acepta trafico sin cifrar (`usesCleartextTraffic`
 * esta en false en el manifiesto).
 */
object ApiConfig {
    const val baseUrl: String = "https://api.undiamas.site"
}
