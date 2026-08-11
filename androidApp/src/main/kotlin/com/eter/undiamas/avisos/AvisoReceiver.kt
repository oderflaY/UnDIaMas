package com.eter.undiamas.avisos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.eter.undiamas.MainActivity
import com.eter.undiamas.R

/** Canal normal: los recordatorios de racha, anclas y ahorro. */
const val CANAL_ACOMPANAMIENTO = "acompanamiento"

/**
 * Canal aparte para los avisos de crisis.
 *
 * Van separados para que alguien pueda silenciar los recordatorios del día a día sin
 * silenciar también el protocolo de emergencia. Si compartieran canal, apagar el ruido
 * apagaría justo lo que hace falta cuando el semáforo está en rojo.
 */
const val CANAL_EMERGENCIA = "emergencia"

const val EXTRA_TITULO = "titulo"
const val EXTRA_CUERPO = "cuerpo"
const val EXTRA_URGENTE = "urgente"
const val EXTRA_ID = "id_aviso"

/**
 * Recibe la alarma programada y publica la notificación.
 *
 * Se hace con `BroadcastReceiver` + `AlarmManager` en vez de WorkManager porque estos
 * avisos tienen hora exacta: uno de "respira conmigo" que llega cuarenta minutos tarde ya
 * no acompaña nada.
 */
class AvisoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val titulo = intent.getStringExtra(EXTRA_TITULO) ?: return
        val cuerpo = intent.getStringExtra(EXTRA_CUERPO).orEmpty()
        val urgente = intent.getBooleanExtra(EXTRA_URGENTE, false)
        val id = intent.getStringExtra(EXTRA_ID).orEmpty()

        crearCanales(context)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Al tocar la notificación se abre la app, no una pantalla suelta: el aviso invita
        // a entrar, y desde dentro la persona decide qué necesita.
        val abrir = PendingIntent.getActivity(
            context,
            id.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notificacion = NotificationCompat.Builder(
            context,
            if (urgente) CANAL_EMERGENCIA else CANAL_ACOMPANAMIENTO,
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            // El texto largo se ve entero al desplegar: varios cuerpos no caben en una línea.
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(if (urgente) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (urgente) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .build()

        NotificationManagerCompat.from(context).notify(id.hashCode(), notificacion)
    }

    companion object {
        fun crearCanales(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return

            manager.createNotificationChannel(
                NotificationChannel(
                    CANAL_ACOMPANAMIENTO,
                    "Acompañamiento diario",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Tu racha, tus motivos y lo que llevas ahorrado."
                },
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CANAL_EMERGENCIA,
                    "Apoyo en momentos difíciles",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Avisos cuando tu semáforo está en rojo."
                    enableVibration(true)
                },
            )
        }
    }
}
