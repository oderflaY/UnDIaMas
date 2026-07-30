package com.eter.undiamas.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Instante actual que se refresca cada segundo, para que los contadores en pantalla
 * avancen en vivo en vez de quedarse congelados en el valor de la primera composición.
 */
@Composable
fun rememberNow(intervalMillis: Long = 1_000): State<Instant> =
    produceState(initialValue = Clock.System.now()) {
        while (true) {
            delay(intervalMillis)
            value = Clock.System.now()
        }
    }
