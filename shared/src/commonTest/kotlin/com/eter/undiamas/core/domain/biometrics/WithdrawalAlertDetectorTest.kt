package com.eter.undiamas.core.domain.biometrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * La alerta de posible episodio de abstinencia es de las cosas que más pesan en esta app:
 * si falla por defecto deja sola a la persona, y si falla por exceso la acostumbra a
 * ignorar avisos. Por eso ambos lados están cubiertos.
 */
class WithdrawalAlertDetectorTest {

    private fun serie(vararg bpm: Long) =
        bpm.mapIndexed { index, valor -> HeartRateSample(epochSeconds = index * 60L, bpm = valor) }

    @Test
    fun `una lectura en el umbral ya dispara la alerta`() {
        // 80 exacto cuenta: el umbral es "sube a 80", no "supera 80".
        val alertas = WithdrawalAlertDetector().detect(serie(65, 70, 80, 72))

        assertEquals(1, alertas.size)
        assertEquals(80, alertas.first().peakBpm)
    }

    @Test
    fun `un pulso siempre por debajo del umbral no genera nada`() {
        assertTrue(WithdrawalAlertDetector().detect(serie(60, 65, 72, 79)).isEmpty())
    }

    @Test
    fun `lecturas altas seguidas son un solo episodio y no una alerta por lectura`() {
        // Avisar cuatro veces del mismo episodio es la forma más rápida de que dejen de leerse.
        val alertas = WithdrawalAlertDetector().detect(serie(70, 88, 95, 91, 84, 68))

        assertEquals(1, alertas.size)
        val alerta = alertas.first()
        assertEquals(95, alerta.peakBpm)
        assertEquals(4, alerta.sampleCount)
        assertEquals(60, alerta.startEpochSeconds)
        assertEquals(240, alerta.endEpochSeconds)
    }

    @Test
    fun `dos episodios separados por un tramo en calma se cuentan aparte`() {
        val alertas = WithdrawalAlertDetector().detect(serie(85, 90, 65, 62, 83, 88))

        assertEquals(2, alertas.size)
        assertEquals(90, alertas[0].peakBpm)
        assertEquals(88, alertas[1].peakBpm)
    }

    @Test
    fun `sin lecturas no se inventa una alerta`() {
        assertTrue(WithdrawalAlertDetector().detect(emptyList()).isEmpty())
    }

    @Test
    fun `las lecturas desordenadas se ordenan antes de agrupar`() {
        // Health Connect no garantiza orden; agrupar sin ordenar partiría un episodio en dos.
        val desordenadas = listOf(
            HeartRateSample(epochSeconds = 180, bpm = 90),
            HeartRateSample(epochSeconds = 60, bpm = 85),
            HeartRateSample(epochSeconds = 120, bpm = 88),
        )

        val alertas = WithdrawalAlertDetector().detect(desordenadas)

        assertEquals(1, alertas.size)
        assertEquals(3, alertas.first().sampleCount)
    }

    @Test
    fun `el episodio esta activo si la ultima lectura sigue alta`() {
        assertTrue(WithdrawalAlertDetector().isActive(serie(65, 70, 92)))
        assertFalse(WithdrawalAlertDetector().isActive(serie(92, 70, 65)))
        assertFalse(WithdrawalAlertDetector().isActive(emptyList()))
    }

    @Test
    fun `el umbral se puede subir sin tocar el resto de la logica`() {
        // 80 es agresivo a proposito; subirlo debe ser cambiar un numero, no reescribir esto.
        val estricto = WithdrawalAlertDetector(thresholdBpm = 110)

        assertTrue(estricto.detect(serie(85, 90, 95)).isEmpty())
        assertEquals(1, estricto.detect(serie(85, 115)).size)
    }
}
