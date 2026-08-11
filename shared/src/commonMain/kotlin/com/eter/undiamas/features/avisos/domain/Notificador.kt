package com.eter.undiamas.features.avisos.domain

/**
 * Puerto de notificaciones locales del sistema.
 *
 * "Locales" es la palabra clave: las programa el propio teléfono y llegan aunque la app
 * esté cerrada y aunque no haya conexión. Es lo contrario del canal de eventos del
 * servidor, que solo funciona con la app abierta.
 */
interface Notificador {

    /** Si el sistema tiene permiso concedido para mostrar notificaciones. */
    suspend fun tienePermiso(): Boolean

    /** Pide el permiso. Devuelve si quedó concedido. */
    suspend fun pedirPermiso(): Boolean

    /** Muestra un aviso ahora mismo. */
    suspend fun mostrarAhora(aviso: Aviso)

    /**
     * Deja programado el plan del día.
     *
     * Reemplaza cualquier plan anterior: si el semáforo cambió de verde a rojo, los dos
     * avisos tranquilos que quedaban pendientes ya no valen, y dejarlos mezclados con los
     * once nuevos daría un mensaje contradictorio.
     */
    suspend fun programar(plan: List<AvisoProgramado>)

    /** Cancela todo lo pendiente. Al cerrar sesión, o si se apagan los avisos. */
    suspend fun cancelarTodo()
}

/** Notificador que no hace nada. Para previews, tests y plataformas sin implementar. */
class NotificadorInactivo : Notificador {
    override suspend fun tienePermiso(): Boolean = false
    override suspend fun pedirPermiso(): Boolean = false
    override suspend fun mostrarAhora(aviso: Aviso) = Unit
    override suspend fun programar(plan: List<AvisoProgramado>) = Unit
    override suspend fun cancelarTodo() = Unit
}
