package com.eter.undiamas.core.data.local

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La cola es donde vive lo que alguien ya considera guardado. Estas pruebas cuidan las tres
 * reglas que la hacen fiable: orden, nada se borra sin confirmación, y nada se pierde.
 */
class OutboxTest {

    private fun outbox() = Outbox(UndiamasDatabase(":memory:"))

    @Test
    fun `entrega las operaciones en el orden en que ocurrieron`() = runTest {
        val outbox = outbox()

        outbox.encolar(TipoPendiente.RECAIDA, """{"note":"","triggers":[]}""")
        outbox.encolar(TipoPendiente.CHECK_IN, """{"riskLevel":"VERDE"}""")
        outbox.encolar(TipoPendiente.ANIMO, """{"mood":"BIEN"}""")

        val pendientes = outbox.siguientes()

        // El servidor calcula la racha a partir de esta secuencia: desordenarla le haría
        // reconstruir una historia que no pasó.
        assertEquals(
            listOf(TipoPendiente.RECAIDA, TipoPendiente.CHECK_IN, TipoPendiente.ANIMO),
            pendientes.map { it.tipo },
        )
        assertTrue(pendientes[0].seq < pendientes[1].seq)
    }

    @Test
    fun `cuenta lo que falta por enviar`() = runTest {
        val outbox = outbox()
        outbox.refrescarConteo()
        assertEquals(0, outbox.pendientes.value)

        outbox.encolar(TipoPendiente.ANIMO, "{}")
        outbox.encolar(TipoPendiente.ANIMO, "{}")
        assertEquals(2, outbox.pendientes.value)

        outbox.completar(outbox.siguientes().first().seq)
        assertEquals(1, outbox.pendientes.value)
    }

    @Test
    fun `un fallo no borra la operacion, solo lo anota`() = runTest {
        val outbox = outbox()
        val seq = outbox.encolar(TipoPendiente.CHECK_IN, """{"riskLevel":"ROJO"}""")

        outbox.anotarFallo(seq, "sin conexión")

        // Perder un check-in es mucho peor que mandarlo dos veces.
        val pendiente = outbox.siguientes().single()
        assertEquals(1, pendiente.intentos)
        assertEquals("sin conexión", pendiente.ultimoError)
        assertEquals(1, outbox.pendientes.value)
    }

    @Test
    fun `los intentos se acumulan`() = runTest {
        val outbox = outbox()
        val seq = outbox.encolar(TipoPendiente.ANIMO, "{}")

        repeat(3) { outbox.anotarFallo(seq, "sin conexión") }

        assertEquals(3, outbox.siguientes().single().intentos)
    }

    @Test
    fun `descartar por id local anula las operaciones de una fila`() = runTest {
        val outbox = outbox()
        outbox.encolar(TipoPendiente.DIARIO_CREAR, """{"content":"x"}""", idLocal = "local-1")
        outbox.encolar(TipoPendiente.ANIMO, """{"mood":"BIEN"}""", idLocal = "local-2")

        val habia = outbox.descartarPorIdLocal("local-1")

        assertTrue(habia)
        assertEquals(1, outbox.pendientes.value)
        assertEquals(TipoPendiente.ANIMO, outbox.siguientes().single().tipo)
    }

    @Test
    fun `descartar algo que ya se envio no rompe nada`() = runTest {
        val outbox = outbox()

        assertTrue(!outbox.descartarPorIdLocal("no-existe"))
        assertEquals(0, outbox.pendientes.value)
    }

    @Test
    fun `vaciar deja la cola limpia al cambiar de cuenta`() = runTest {
        val outbox = outbox()
        outbox.encolar(TipoPendiente.CHECK_IN, "{}")
        outbox.encolar(TipoPendiente.DIARIO_CREAR, "{}")

        outbox.vaciar()

        // Mandar la cola de una persona con el token de otra sería lo peor que puede pasar
        // en un teléfono compartido.
        assertEquals(0, outbox.pendientes.value)
        assertTrue(outbox.siguientes().isEmpty())
    }

    @Test
    fun `la carga viaja intacta`() = runTest {
        val outbox = outbox()
        val json = """{"riskLevel":"ROJO","cravingLevel":9,"note":"día muy difícil"}"""

        outbox.encolar(TipoPendiente.CHECK_IN, json, idLocal = "local-checkin-1")

        val pendiente = outbox.siguientes().single()
        assertEquals(json, pendiente.carga)
        assertEquals("local-checkin-1", pendiente.idLocal)
    }
}
