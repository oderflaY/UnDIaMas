package com.eter.undiamas.core.domain.biometrics

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BiometricsTest {

    private fun snapshot(bpms: List<Long>) = BiometricsSnapshot(
        generatedAtEpochSeconds = 1_700_000_000,
        totalSteps = 8_432,
        heartRate = bpms.mapIndexed { index, bpm -> HeartRateSample(1_700_000_000L + index * 60, bpm) },
    )

    @Test
    fun `calcula minimo maximo y media de la frecuencia cardiaca`() {
        val s = snapshot(listOf(62, 118, 75))

        assertEquals(62, s.minBpm)
        assertEquals(118, s.maxBpm)
        assertEquals(85, s.avgBpm) // (62+118+75)/3 = 85
    }

    @Test
    fun `sin lecturas de FC los agregados son nulos y no se divide entre cero`() {
        val s = snapshot(emptyList())

        assertNull(s.minBpm)
        assertNull(s.maxBpm)
        assertNull(s.avgBpm)
    }

    @Test
    fun `el JSON del analisis conserva todos los datos al ir y volver`() {
        val original = snapshot(listOf(70, 95))

        val json = original.toAnalysisJson()
        val restored = Json.decodeFromString<AnalysisPayload>(json)

        assertEquals(original, restored.snapshot)
    }

    @Test
    fun `el JSON expone los campos que el algoritmo espera`() {
        val json = snapshot(listOf(70)).toAnalysisJson()

        assertTrue("totalSteps" in json)
        assertTrue("heartRate" in json)
        assertTrue("bpm" in json)
        assertTrue("windowHours" in json)
    }
}
