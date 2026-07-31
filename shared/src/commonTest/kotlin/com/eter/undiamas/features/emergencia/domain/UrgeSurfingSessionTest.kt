package com.eter.undiamas.features.emergencia.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UrgeSurfingSessionTest {
    private val session = UrgeSurfingSession()

    @Test
    fun `los primeros cinco minutos son de respiracion`() {
        assertEquals(UrgeStage.RESPIRACION, session.stageAt(elapsedSeconds = 0))
        assertEquals(UrgeStage.RESPIRACION, session.stageAt(elapsedSeconds = 299))
    }

    @Test
    fun `del minuto cinco al diez toca reestructuracion cognitiva`() {
        assertEquals(UrgeStage.REENCUADRE, session.stageAt(elapsedSeconds = 300))
        assertEquals(UrgeStage.REENCUADRE, session.stageAt(elapsedSeconds = 599))
    }

    @Test
    fun `los ultimos cinco minutos son de distraccion tactil`() {
        assertEquals(UrgeStage.DISTRACCION, session.stageAt(elapsedSeconds = 600))
        assertEquals(UrgeStage.DISTRACCION, session.stageAt(elapsedSeconds = 899))
    }

    @Test
    fun `a los quince minutos la sesion se considera completa`() {
        assertFalse(session.isComplete(elapsedSeconds = 899))
        assertTrue(session.isComplete(elapsedSeconds = 900))
        assertTrue(session.isComplete(elapsedSeconds = 1_000))
    }

    @Test
    fun `pasado el total la etapa se mantiene en la ultima y no se desborda`() {
        assertEquals(UrgeStage.DISTRACCION, session.stageAt(elapsedSeconds = 5_000))
    }

    @Test
    fun `el tiempo restante nunca es negativo`() {
        assertEquals(900, session.remainingSeconds(elapsedSeconds = 0))
        assertEquals(60, session.remainingSeconds(elapsedSeconds = 840))
        assertEquals(0, session.remainingSeconds(elapsedSeconds = 2_000))
    }

    @Test
    fun `el progreso va de cero a uno sin pasarse`() {
        assertEquals(0f, session.progress(elapsedSeconds = 0))
        assertEquals(0.5f, session.progress(elapsedSeconds = 450))
        assertEquals(1f, session.progress(elapsedSeconds = 3_000))
    }

    @Test
    fun `un tiempo negativo por reloj desfasado se trata como el inicio`() {
        assertEquals(UrgeStage.RESPIRACION, session.stageAt(elapsedSeconds = -10))
        assertEquals(0f, session.progress(elapsedSeconds = -10))
        assertEquals(900, session.remainingSeconds(elapsedSeconds = -10))
    }

    @Test
    fun `cada etapa ofrece al menos una pregunta de reencuadre`() {
        assertTrue(session.reframingPrompts.isNotEmpty())
        assertTrue(session.reframingPrompts.all { it.question.isNotBlank() })
    }
}
