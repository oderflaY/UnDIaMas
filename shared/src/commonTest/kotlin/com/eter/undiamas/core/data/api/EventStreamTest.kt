package com.eter.undiamas.core.data.api

import com.eter.undiamas.core.domain.model.RiskLevel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * El canal de eventos sustituye a las notificaciones push, así que su lectura tiene que
 * aguantar exactamente lo que manda el servidor: latidos, líneas en blanco y el `ready`
 * inicial. Un fallo aquí es un aviso de semáforo en rojo que no llega.
 */
class EventStreamTest {

    private fun streamOf(body: String): EventStream {
        val engine = MockEngine {
            respond(body, headers = headersOf(HttpHeaders.ContentType, "text/event-stream"))
        }
        val store = InMemoryTokenStore(Tokens("t", "r"))
        return EventStream(UnDiaMasApi(createApiHttpClient(store, { "http://test.local" }, engine), store))
    }

    @Test
    fun `ignora el ready y los latidos, y entrega los eventos reales`() = runTest {
        val body = buildString {
            appendLine("event: ready")
            appendLine("data: {}")
            appendLine()
            appendLine(": ping")
            appendLine()
            appendLine("""data: {"type":"traffic_light","payload":{"id":"t1","status":"ROJO"},"createdAt":"2026-08-03T11:45:54-06:00"}""")
            appendLine()
            appendLine(": ping")
            appendLine()
            appendLine("""data: {"type":"alert","payload":{"alert":{"id":"a1","riskLevel":"ROJO","message":"Semáforo en rojo","handled":false},"deliveredOnline":true,"trustedContact":{"nombre":"Luis","telefono":"555","rol":"PADRINO"}}}""")
            appendLine()
        }

        val events = streamOf(body).events().take(2).toList()

        assertEquals(listOf("traffic_light", "alert"), events.map { it.type })
    }

    @Test
    fun `lee la alerta con su contacto de confianza`() = runTest {
        val body = """data: {"type":"alert","payload":{"alert":{"id":"a1","riskLevel":"ROJO","message":"Semáforo en rojo","handled":false},"deliveredOnline":true,"trustedContact":{"nombre":"Luis","telefono":"555-1","rol":"PADRINO"}}}""" + "\n\n"

        val event = streamOf(body).events().take(1).toList().single()
        val payload = event.asAlert()

        assertNotNull(payload)
        assertEquals("a1", payload.alert.id)
        assertEquals("ROJO", payload.alert.riskLevel)
        // El contacto sugerido es lo que la app ofrece marcar en el protocolo de emergencia.
        assertEquals("Luis", payload.trustedContact?.nombre)
        assertEquals("555-1", payload.trustedContact?.telefono)
    }

    @Test
    fun `lee el evento de semaforo`() = runTest {
        val body = """data: {"type":"traffic_light","payload":{"id":"t1","status":"AMARILLO","reason":"check-in","triggerLevel":3,"suggestedActions":["respirar"]}}""" + "\n\n"

        val entry = streamOf(body).events().take(1).toList().single().asTrafficLight()

        assertNotNull(entry)
        assertEquals(RiskLevel.AMARILLO, entry.status.toRiskLevel())
        assertEquals(listOf("respirar"), entry.suggestedActions)
    }

    @Test
    fun `un evento de otro tipo no se lee como alerta`() = runTest {
        val body = """data: {"type":"check_in_reminder","payload":{}}""" + "\n\n"

        val event = streamOf(body).events().take(1).toList().single()

        assertEquals(ServerEventType.CHECK_IN_REMINDER, event.type)
        assertNull(event.asAlert())
        assertNull(event.asTrafficLight())
    }

    @Test
    fun `un evento con json roto no tumba el canal`() = runTest {
        val body = buildString {
            appendLine("data: {esto no es json}")
            appendLine()
            appendLine("""data: {"type":"alert","payload":{"alert":{"id":"a2","riskLevel":"ROJO","message":"ok","handled":false}}}""")
            appendLine()
        }

        val events = streamOf(body).events().take(1).toList()

        assertEquals("alert", events.single().type)
    }
}
