package com.eter.undiamas.core.domain.biometrics

import kotlinx.serialization.Serializable

/**
 * Umbral de frecuencia cardíaca que dispara el aviso de posible episodio de abstinencia.
 *
 * Está deliberadamente bajo. El reposo normal ronda 60–100 bpm, así que caminar o subir
 * escaleras basta para cruzarlo: el aviso saltará a menudo por motivos que no tienen nada
 * que ver con la abstinencia. Se asume ese coste para no dejar pasar un episodio real.
 *
 * Si el aviso resulta tan frecuente que se empieza a ignorar, subir este número (100–110
 * es un rango razonable para "taquicardia en reposo") es todo lo que hay que tocar.
 */
const val WITHDRAWAL_BPM_THRESHOLD: Long = 80

/** Tramo continuo de pulsaciones en o por encima del umbral, con su pico y su duración. */
@Serializable
data class WithdrawalAlert(
    val startEpochSeconds: Long,
    val endEpochSeconds: Long,
    val peakBpm: Long,
    val sampleCount: Int,
)

/**
 * Aviso de posible episodio de abstinencia a partir de la frecuencia cardíaca.
 *
 * A diferencia de [SpikeDetector], que compara contra la línea base del propio día, aquí
 * el criterio es un umbral fijo: "el pulso subió a [thresholdBpm]". Son complementarios y
 * conviven a propósito — uno detecta lo raro para esa persona, el otro lo alto en términos
 * absolutos.
 *
 * Las lecturas contiguas por encima del umbral se agrupan en UN episodio en vez de generar
 * un aviso por lectura: avisar cinco veces del mismo momento es la forma más rápida de que
 * la persona deje de leer los avisos, y entonces se pierde el que sí importaba.
 *
 * No diagnostica nada. Señala "tu cuerpo está acelerado, mira cómo estás".
 */
class WithdrawalAlertDetector(
    private val thresholdBpm: Long = WITHDRAWAL_BPM_THRESHOLD,
) {
    fun detect(samples: List<HeartRateSample>): List<WithdrawalAlert> {
        if (samples.isEmpty()) return emptyList()

        // Health Connect no garantiza orden; sin ordenar, un episodio se partiría en varios.
        val ordered = samples.sortedBy { it.epochSeconds }
        val alerts = mutableListOf<WithdrawalAlert>()
        var run = mutableListOf<HeartRateSample>()

        fun closeRun() {
            if (run.isNotEmpty()) {
                alerts.add(
                    WithdrawalAlert(
                        startEpochSeconds = run.first().epochSeconds,
                        endEpochSeconds = run.last().epochSeconds,
                        peakBpm = run.maxOf { it.bpm },
                        sampleCount = run.size,
                    ),
                )
                run = mutableListOf()
            }
        }

        ordered.forEach { sample ->
            if (sample.bpm >= thresholdBpm) run.add(sample) else closeRun()
        }
        closeRun()
        return alerts
    }

    /**
     * true si la lectura más reciente sigue en o por encima del umbral, es decir, si el
     * episodio está pasando ahora mismo. Es lo que decide si la app avisa en el momento o
     * si solo lo muestra como algo que ya ocurrió hoy.
     */
    fun isActive(samples: List<HeartRateSample>): Boolean =
        samples.maxByOrNull { it.epochSeconds }?.let { it.bpm >= thresholdBpm } ?: false
}
