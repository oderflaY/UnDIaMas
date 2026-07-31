package com.eter.undiamas.core.presentation

import kotlin.system.exitProcess

/**
 * iOS no ofrece una forma legitima de cerrar la app.
 *
 * `exitProcess(0)` funciona en tiempo de ejecucion, pero conviene saber dos cosas antes
 * de enviarlo a produccion:
 *  - Las guias de revision de Apple (2.5.x) prohiben terminar la app por codigo y es
 *    motivo habitual de rechazo.
 *  - Para el usuario y para los reportes de crashes es indistinguible de un cierre
 *    inesperado, no de una accion intencional.
 *
 * Para una build de App Store, lo razonable es ocultar el boton en iOS y dejar que la
 * persona use el gesto del sistema.
 */
actual fun closeApp() {
    exitProcess(0)
}
