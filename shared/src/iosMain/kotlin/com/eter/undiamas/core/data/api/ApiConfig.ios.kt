package com.eter.undiamas.core.data.api

/**
 * Reserva, no la direccion real.
 *
 * En iOS la fija el punto de entrada: `MainViewController(apiBaseUrl = …)`, que lee la
 * configuracion del target de Xcode. Igual que en Android, aqui no puede haber una IP de
 * red local, porque este valor viaja dentro de la app publicada.
 */
actual fun defaultApiBaseUrl(): String = "https://api.undiamas.mx"
