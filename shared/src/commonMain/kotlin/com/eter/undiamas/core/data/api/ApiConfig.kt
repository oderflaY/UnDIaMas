package com.eter.undiamas.core.data.api

/**
 * Direccion de reserva del backend.
 *
 * Es https a proposito y **no** una direccion de desarrollo: este valor acaba compilado
 * dentro de la app publicada, y una IP de la red de quien programa no pinta nada ahi. La
 * direccion de verdad la inyecta cada plataforma al arrancar — en Android desde el
 * buildType, que es quien sabe si esto es una build de pruebas o la de la tienda.
 */
expect fun defaultApiBaseUrl(): String

object ApiConfig {
    var baseUrl: String = defaultApiBaseUrl()

    /**
     * Si se puede apuntar la app a otro servidor desde Configuracion.
     *
     * Solo en debug. En la app publicada esto seria un agujero serio: bastaria con
     * convencer a alguien de escribir una direccion para que su historial de recaidas y su
     * contrasena acabaran en el servidor de otra persona.
     */
    var permiteCambiarServidor: Boolean = false

    /**
     * Fija la direccion y si se permite cambiarla. Se llama una vez, al arrancar.
     *
     * [url] se ignora si viene en blanco, para que un valor mal configurado en la
     * plataforma no deje la app apuntando a la nada.
     */
    fun configurar(url: String, permiteCambiar: Boolean) {
        if (url.isNotBlank()) baseUrl = url
        permiteCambiarServidor = permiteCambiar
    }
}
