package com.eter.undiamas.core.presentation

import android.app.Activity
import java.lang.ref.WeakReference
import kotlin.system.exitProcess

/**
 * Referencia debil a la Activity actual.
 *
 * Debil a proposito: una referencia fuerte a una Activity en una variable estatica la
 * mantendria viva tras destruirse y filtraria toda su jerarquia de vistas.
 */
private var activityRef: WeakReference<Activity>? = null

/** Llamar desde `onCreate` de la Activity que aloja la UI de Compose. */
fun registerActivityForClose(activity: Activity) {
    activityRef = WeakReference(activity)
}

/** Llamar desde `onDestroy` para no retener una Activity que ya se fue. */
fun unregisterActivityForClose() {
    activityRef = null
}

actual fun closeApp() {
    val activity = activityRef?.get()
    if (activity != null) {
        // finishAndRemoveTask tambien borra la app de la lista de recientes, que es lo
        // que de verdad importa cuando alguien esta a punto de revisar el telefono.
        activity.finishAndRemoveTask()
    } else {
        // Sin Activity viva no queda otra que terminar el proceso.
        exitProcess(0)
    }
}
