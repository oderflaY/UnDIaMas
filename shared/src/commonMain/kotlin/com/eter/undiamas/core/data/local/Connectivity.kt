package com.eter.undiamas.core.data.local

import kotlinx.coroutines.flow.Flow

/**
 * Avisa cuando el teléfono gana o pierde conexión.
 *
 * Sirve para empujar la bandeja de salida en cuanto vuelve la red, sin esperar a que la
 * persona toque nada. Aun así el sincronizador no se fía solo de esto: "hay red" no
 * significa "el servidor responde" — el wifi de una cafetería que exige iniciar sesión
 * cuenta como conectado y no lleva a ninguna parte. Por eso también se reintenta con
 * temporizador y al abrir la app.
 */
expect class ConnectivityMonitor() {
    val estaEnLinea: Flow<Boolean>
}
