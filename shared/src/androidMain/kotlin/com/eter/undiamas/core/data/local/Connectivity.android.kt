package com.eter.undiamas.core.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

/**
 * El Context se inyecta desde la Activity, igual que la ruta de la base de datos: una
 * `expect class` no puede recibirlo sin ensuciar la firma común.
 */
internal var androidAppContext: Context? = null

fun initConnectivityContext(context: Context) {
    androidAppContext = context.applicationContext
}

actual class ConnectivityMonitor actual constructor() {

    /**
     * Emite el estado actual y cada cambio.
     *
     * Sin Context (previews, tests) se asume que hay conexión: es preferible intentar
     * enviar y fallar que quedarse callado guardando cosas que nunca salen.
     */
    actual val estaEnLinea: Flow<Boolean> = androidAppContext?.let { context ->
        callbackFlow {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            fun hayInternet(): Boolean {
                val capacidades = manager.getNetworkCapabilities(manager.activeNetwork)
                return capacidades?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            }

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(true)
                }

                override fun onLost(network: Network) {
                    // Puede quedar otra red activa (se cae el wifi y entra la de datos).
                    trySend(hayInternet())
                }

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    trySend(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                }
            }

            trySend(hayInternet())
            val peticion = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            manager.registerNetworkCallback(peticion, callback)

            awaitClose { manager.unregisterNetworkCallback(callback) }
        }.distinctUntilChanged()
    } ?: flowOf(true)
}
