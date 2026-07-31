package com.eter.undiamas.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.eter.undiamas.R
import com.eter.undiamas.core.domain.model.AddictionType

private const val CHANNEL_ID = "biometric_alerts"
private const val NOTIFICATION_ID = 1001

/**
 * Avisos de alteración biométrica.
 *
 * Usa las APIs de plataforma directamente en vez de androidx.core: con minSdk 29 todo
 * lo necesario está disponible, y evita arrastrar una dependencia que exige subir el
 * compileSdk del proyecto.
 *
 * El canal usa importancia DEFAULT y no HIGH a propósito: un aviso de este tipo no debe
 * irrumpir a pantalla completa sobre alguien que ya está alterado. Y el texto nunca
 * afirma que haya una recaída, solo que el cuerpo cambió y que respirar ayuda.
 */
class NotificationHelper(private val context: Context) {

    private val manager: NotificationManager? =
        context.getSystemService(NotificationManager::class.java)

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas biométricas",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisos cuando tu pulsera detecta una alteración en tu pulso"
        }
        manager?.createNotificationChannel(channel)
    }

    /**
     * Mensaje del aviso. Para sustancias se menciona la abstinencia y se remite a atención
     * médica, porque ahí los síntomas físicos pueden ser reales; para conductas se usa un
     * texto más neutro que no patologiza una subida de pulso cualquiera.
     */
    fun buildMessage(addiction: AddictionType?): String = when {
        addiction == null ->
            "Hemos detectado una alteración en tu pulso. Respira profundo; estamos contigo."

        addiction.substance ->
            "Hemos detectado una alteración en tu pulso. Respira profundo, podría ser un " +
                "síntoma de abstinencia de ${addiction.title}. Si te sientes mal, busca atención médica."

        else ->
            "Hemos detectado una alteración en tu pulso. Respira profundo, podría ser un " +
                "momento de impulso relacionado con ${addiction.title}."
    }

    /** True si el sistema permite publicar avisos (en Android 13+ depende del permiso). */
    fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Publica el aviso. Devuelve false si no se pudo en vez de lanzar excepción:
     * quedarse sin notificación nunca debe tumbar la app del paciente.
     */
    fun notifyBiometricAlert(addiction: AddictionType?): Boolean {
        if (!canNotify()) return false
        val message = buildMessage(addiction)

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Tu cuerpo está pidiendo una pausa")
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()

        return try {
            manager?.notify(NOTIFICATION_ID, notification) != null
        } catch (_: SecurityException) {
            false
        }
    }
}
