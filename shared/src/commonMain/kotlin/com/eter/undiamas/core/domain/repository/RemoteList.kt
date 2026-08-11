package com.eter.undiamas.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Lista que vive en el servidor y la app espeja en memoria.
 *
 * Con Firestore cada coleccion empujaba sus cambios sola. Contra un backend REST no hay
 * tal cosa: alguien tiene que pedir los datos. Por eso el patron es siempre el mismo —
 * [items] es lo que pintan las pantallas, y [refresh] es la unica forma de moverlo.
 *
 * [items] nunca lanza. Un fallo de red sale por [refresh], que es quien sabe reintentar;
 * si el flujo pudiera fallar, un diario que no carga tumbaria tambien el semaforo.
 */
interface RemoteList<T> {
    val items: StateFlow<List<T>>

    suspend fun refresh()
}
