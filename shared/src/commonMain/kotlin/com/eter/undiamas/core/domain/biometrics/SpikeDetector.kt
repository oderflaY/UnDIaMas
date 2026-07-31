package com.eter.undiamas.core.domain.biometrics

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max

/** Tramo de lecturas de FC anómalamente altas, con su ventana temporal y su máximo. */
@Serializable
data class HeartRateSpike(
    val startEpochSeconds: Long,
    val endEpochSeconds: Long,
    val peakBpm: Long,
    val sampleCount: Int,
)

/** Factor que convierte la MAD en un equivalente de desviación estándar. */
private const val MAD_TO_SIGMA = 1.4826

/**
 * Detección de picos de frecuencia cardíaca sobre línea base robusta.
 *
 * Se usa mediana + MAD en lugar de media + desviación estándar porque el propio pico
 * contamina su línea base: un tramo de 140 bpm sobre base de 70 infla la media y la σ
 * hasta el punto de esconderse a sí mismo. La mediana y la MAD resisten esos atípicos.
 *
 * El umbral es mediana + max(sigmaFactor · σ_robusta, minDeltaBpm): el delta mínimo evita
 * que un día de FC casi plana (MAD ≈ 0) marque como pico cualquier variación trivial.
 *
 * Es deliberadamente estadística y no clínica: señala "esto es raro respecto a TU día",
 * sin pretender diagnosticar nada. Con menos de [minSamples] lecturas no afirma nada.
 */
class SpikeDetector(
    private val sigmaFactor: Double = 2.0,
    private val minDeltaBpm: Double = 15.0,
    private val minSamples: Int = 5,
) {
    fun detect(samples: List<HeartRateSample>): List<HeartRateSpike> {
        if (samples.size < minSamples) return emptyList()

        val ordered = samples.sortedBy { it.epochSeconds }
        val median = medianOf(ordered.map { it.bpm.toDouble() })
        val mad = medianOf(ordered.map { abs(it.bpm - median) })
        val robustSigma = MAD_TO_SIGMA * mad
        val threshold = median + max(sigmaFactor * robustSigma, minDeltaBpm)

        val spikes = mutableListOf<HeartRateSpike>()
        var run = mutableListOf<HeartRateSample>()

        fun closeRun() {
            if (run.isNotEmpty()) {
                spikes.add(
                    HeartRateSpike(
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
            if (sample.bpm > threshold) run.add(sample) else closeRun()
        }
        closeRun()
        return spikes
    }

    private fun medianOf(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
    }
}
