package com.eter.undiamas.avisos

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.eter.undiamas.features.avisos.domain.Aviso
import com.eter.undiamas.features.avisos.domain.AvisoProgramado
import com.eter.undiamas.features.avisos.domain.Notificador
import kotlinx.coroutines.CompletableDeferred
import java.util.Calendar

/**
 * Notificador de Android.
 *
 * Programa una alarma por aviso. El sistema las conserva aunque la app se cierre, que es
 * justo lo que hace falta: los recordatorios sirven precisamente cuando nadie tiene la app
 * abierta.
 */
class NotificadorAndroid(
    private val context: Context,
    /** La Activity lo rellena para poder pedir el permiso; sin ella solo se puede consultar. */
    var solicitarPermiso: (suspend () -> Boolean)? = null,
) : Notificador {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Ids de las alarmas puestas, para poder cancelarlas al reprogramar. */
    private val programados = mutableSetOf<String>()

    init {
        AvisoReceiver.crearCanales(context)
    }

    override suspend fun tienePermiso(): Boolean {
        // Antes de Android 13 no existe el permiso: basta con que el usuario no haya
        // silenciado la app desde los ajustes del sistema.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    override suspend fun pedirPermiso(): Boolean {
        if (tienePermiso()) return true
        return solicitarPermiso?.invoke() ?: false
    }

    override suspend fun mostrarAhora(aviso: Aviso) {
        if (!tienePermiso()) return
        context.sendBroadcast(intentDe(aviso))
    }

    override suspend fun programar(plan: List<AvisoProgramado>) {
        cancelarTodo()
        if (!tienePermiso()) return

        val ahora = Calendar.getInstance()
        plan.forEach { programado ->
            val momento = (ahora.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, programado.hora)
                set(Calendar.MINUTE, programado.minuto)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // Si esa hora ya pasó hoy, va para mañana: mejor que dispararla de golpe al
                // programar y llenar la barra de notificaciones atrasadas.
                if (before(ahora)) add(Calendar.DAY_OF_YEAR, 1)
            }

            val pending = PendingIntent.getBroadcast(
                context,
                programado.aviso.id.hashCode(),
                intentDe(programado.aviso),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            // `setExactAndAllowWhileIdle` solo para los urgentes: son los únicos que
            // justifican despertar un teléfono en reposo. El resto puede esperar un poco.
            if (programado.aviso.urgente && puedeProgramarExactas()) {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    momento.timeInMillis,
                    pending,
                )
            } else {
                alarmManager?.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    momento.timeInMillis,
                    VENTANA_MILLIS,
                    pending,
                )
            }
            programados += programado.aviso.id
        }
    }

    override suspend fun cancelarTodo() {
        programados.forEach { id ->
            val pending = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                Intent(context, AvisoReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
            )
            if (pending != null) {
                alarmManager?.cancel(pending)
                pending.cancel()
            }
        }
        programados.clear()
    }

    private fun intentDe(aviso: Aviso) = Intent(context, AvisoReceiver::class.java).apply {
        putExtra(EXTRA_ID, aviso.id)
        putExtra(EXTRA_TITULO, aviso.titulo)
        putExtra(EXTRA_CUERPO, aviso.cuerpo)
        putExtra(EXTRA_URGENTE, aviso.urgente)
    }

    /** Android 12+ exige permiso aparte para alarmas exactas; sin él se usa una ventana. */
    private fun puedeProgramarExactas(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager?.canScheduleExactAlarms() == true

    private companion object {
        /** Margen que se le da al sistema para agrupar alarmas y no gastar batería. */
        const val VENTANA_MILLIS = 10 * 60 * 1000L
    }
}

/** Puente entre el lanzador de permisos de la Activity y el mundo de corrutinas. */
class PermisoPendiente {
    var esperando: CompletableDeferred<Boolean>? = null
}
