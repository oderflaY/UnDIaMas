package com.eter.undiamas.features.avisos

import com.eter.undiamas.features.avisos.domain.Aviso
import com.eter.undiamas.features.avisos.domain.AvisoProgramado
import com.eter.undiamas.features.avisos.domain.Notificador
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * Notificaciones locales de iOS.
 *
 * Las programa el sistema con `UNCalendarNotificationTrigger`, así que llegan aunque la app
 * esté cerrada, igual que las alarmas de Android.
 */
@OptIn(ExperimentalForeignApi::class)
class NotificadorIos : Notificador {

    private val centro = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun tienePermiso(): Boolean = suspendCancellableCoroutine { cont ->
        centro.getNotificationSettingsWithCompletionHandler { ajustes ->
            val estado = ajustes?.authorizationStatus
            cont.resume(
                estado == UNAuthorizationStatusAuthorized ||
                    estado == UNAuthorizationStatusProvisional,
            )
        }
    }

    override suspend fun pedirPermiso(): Boolean = suspendCancellableCoroutine { cont ->
        val opciones = UNAuthorizationOptionAlert or
            UNAuthorizationOptionSound or
            UNAuthorizationOptionBadge
        centro.requestAuthorizationWithOptions(opciones) { concedido, _ ->
            cont.resume(concedido)
        }
    }

    override suspend fun mostrarAhora(aviso: Aviso) {
        // Un segundo en vez de cero: iOS descarta los disparadores de intervalo nulo.
        centro.addNotificationRequest(
            UNNotificationRequest.requestWithIdentifier(
                identifier = aviso.id,
                content = contenidoDe(aviso),
                trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, false),
            ),
        ) { }
    }

    override suspend fun programar(plan: List<AvisoProgramado>) {
        cancelarTodo()
        plan.forEach { programado ->
            val cuando = NSDateComponents().apply {
                hour = programado.hora.toLong()
                minute = programado.minuto.toLong()
            }
            centro.addNotificationRequest(
                UNNotificationRequest.requestWithIdentifier(
                    identifier = programado.aviso.id,
                    content = contenidoDe(programado.aviso),
                    // repeats = true: el plan vale mientras el semáforo no cambie, y al
                    // cambiar se reprograma entero.
                    trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                        dateComponents = cuando,
                        repeats = true,
                    ),
                ),
            ) { }
        }
    }

    override suspend fun cancelarTodo() {
        centro.removeAllPendingNotificationRequests()
    }

    private fun contenidoDe(aviso: Aviso) = UNMutableNotificationContent().apply {
        setTitle(aviso.titulo)
        setBody(aviso.cuerpo)
        // Solo los avisos de crisis suenan: el resto llega en silencio para no agobiar.
        if (aviso.urgente) setSound(UNNotificationSound.defaultSound())
    }
}
