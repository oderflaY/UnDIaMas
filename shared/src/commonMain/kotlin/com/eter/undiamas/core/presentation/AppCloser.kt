package com.eter.undiamas.core.presentation

/**
 * Cierra la aplicacion de inmediato.
 *
 * Existe para el modo camuflaje / borrado de emergencia: cuando alguien exige ver el
 * telefono, la persona necesita que la app desaparezca de un toque, incluida su entrada
 * en la lista de apps recientes.
 *
 * Comportamiento por plataforma:
 *  - Android: cierra la Activity y la quita de recientes.
 *  - iOS: termina el proceso. Apple prohibe el cierre programatico en sus guias de
 *    revision (2.5.x) y lo reporta como crash, asi que una build para la App Store
 *    deberia dejar este boton fuera del target de iOS.
 */
expect fun closeApp()
