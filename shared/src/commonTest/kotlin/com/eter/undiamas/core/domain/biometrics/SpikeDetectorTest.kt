package com.eter.undiamas.core.domain.biometrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpikeDetectorTest {
    private val detector = SpikeDetector()

    private fun samples(vararg bpms: Long) =
        bpms.mapIndexed { index, bpm -> HeartRateSample(epochSeconds = index * 60L, bpm = bpm) }

    @Test
    fun `sin muestras no hay picos`() {
        assertTrue(detector.detect(emptyList()).isEmpty())
    }

    @Test
    fun `una frecuencia constante no produce picos`() {
        assertTrue(detector.detect(samples(72, 72, 72, 72, 72, 72, 72, 72)).isEmpty())
    }

    @Test
    fun `con muy pocas muestras no se afirma nada`() {
        // Tres lecturas no son señal suficiente para hablar de picos.
        assertTrue(detector.detect(samples(70, 71, 140)).isEmpty())
    }

    @Test
    fun `detecta un pico claro sobre una base estable`() {
        val spikes = detector.detect(samples(70, 71, 69, 70, 72, 71, 70, 135, 70, 71))

        assertEquals(1, spikes.size)
        assertEquals(135, spikes.first().peakBpm)
    }

    @Test
    fun `muestras altas consecutivas forman un solo pico con su rango`() {
        val spikes = detector.detect(samples(70, 71, 70, 72, 130, 142, 138, 70, 71, 70))

        assertEquals(1, spikes.size)
        with(spikes.first()) {
            assertEquals(142, peakBpm)
            assertEquals(3, sampleCount)
            assertEquals(4 * 60L, startEpochSeconds)
            assertEquals(6 * 60L, endEpochSeconds)
        }
    }

    @Test
    fun `picos separados por lecturas normales se reportan por separado`() {
        val spikes = detector.detect(samples(70, 70, 71, 135, 70, 71, 70, 140, 71, 70))

        assertEquals(2, spikes.size)
        assertEquals(135, spikes[0].peakBpm)
        assertEquals(140, spikes[1].peakBpm)
    }

    @Test
    fun `el JSON del analisis incluye los picos detectados`() {
        val snapshot = BiometricsSnapshot(
            generatedAtEpochSeconds = 0,
            totalSteps = 1_000,
            heartRate = samples(70, 71, 69, 70, 72, 71, 70, 135, 70, 71),
        )

        val json = snapshot.toAnalysisJson()

        assertTrue("heartRateSpikes" in json)
        assertTrue("peakBpm" in json)
    }
}
