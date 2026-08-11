package com.eter.undiamas

import androidx.compose.ui.window.ComposeUIViewController
import com.eter.undiamas.core.data.api.ApiConfig

/**
 * Punto de entrada de iOS.
 *
 * [apiBaseUrl] se pasa desde Swift con la direccion del backend que corresponda al target
 * (desarrollo o produccion); en blanco se queda la de reserva. [permiteCambiarServidor]
 * solo debe ir a true en builds de prueba: en la app publicada, dejar reapuntar el servidor
 * es dejar que le roben la sesion a alguien.
 */
fun MainViewController(
    apiBaseUrl: String = "",
    permiteCambiarServidor: Boolean = false,
) = ComposeUIViewController {
    ApiConfig.configurar(apiBaseUrl, permiteCambiarServidor)
    App()
}
