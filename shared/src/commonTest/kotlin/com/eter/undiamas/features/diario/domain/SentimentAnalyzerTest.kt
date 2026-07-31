package com.eter.undiamas.features.diario.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class SentimentAnalyzerTest {
    private val analyzer = SentimentAnalyzer()

    @Test
    fun `un texto con palabras de logro se clasifica como positivo`() {
        assertEquals(Sentiment.POSITIVO, analyzer.analyze("Hoy me sentí tranquilo y orgulloso de mi avance"))
    }

    @Test
    fun `un texto con senales de riesgo se clasifica como vulnerable`() {
        assertEquals(Sentiment.VULNERABLE, analyzer.analyze("Estuve ansioso y con muchas ganas de recaer"))
    }

    @Test
    fun `un texto sin carga emocional queda neutral`() {
        assertEquals(Sentiment.NEUTRAL, analyzer.analyze("Fui al super y luego regresé a casa"))
    }

    @Test
    fun `la deteccion ignora mayusculas y acentos`() {
        assertEquals(Sentiment.VULNERABLE, analyzer.analyze("ESTOY MUY ANSIOSO"))
        assertEquals(Sentiment.POSITIVO, analyzer.analyze("Que dia tan TRANQUILO"))
    }

    @Test
    fun `cuando hay senales mezcladas gana la vulnerable para no restarle importancia`() {
        assertEquals(
            Sentiment.VULNERABLE,
            analyzer.analyze("Me sentí feliz en la mañana pero en la noche tuve ganas de consumir"),
        )
    }

    @Test
    fun `un texto vacio queda neutral`() {
        assertEquals(Sentiment.NEUTRAL, analyzer.analyze(""))
    }
}
