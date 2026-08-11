package com.eter.undiamas.core.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StartupDiagnosisTest {

    /**
     * El fallo número uno al conectar por primera vez: en el emulador de Android
     * `localhost` es el propio emulador, no la computadora que lo hospeda. El consejo tiene
     * que decir eso, no "revisa tu internet".
     */
    @Test
    fun `un servidor apagado explica la direccion del emulador`() {
        val d = diagnoseStartupError("java.net.ConnectException: Connection refused")

        assertEquals("El servidor no responde", d.title)
        assertTrue("10.0.2.2" in d.advice)
        assertTrue("internet" !in d.advice.lowercase())
    }

    @Test
    fun `un fallo de conexion del motor HTTP se reconoce igual`() {
        val d = diagnoseStartupError("Failed to connect to /127.0.0.1:8080")

        assertEquals("El servidor no responde", d.title)
    }

    @Test
    fun `http bloqueado por Android se nombra por lo que es`() {
        val d = diagnoseStartupError("CLEARTEXT communication to 10.0.2.2 not permitted by network security policy")

        assertEquals("Android bloqueó la conexión", d.title)
        assertTrue("manifiesto" in d.advice)
    }

    @Test
    fun `una direccion inexistente manda a revisar la configuracion, no el internet`() {
        val d = diagnoseStartupError("Unable to resolve host mi-servidor.local")

        assertEquals("No se encuentra el servidor", d.title)
        assertTrue("Configuración" in d.advice)
    }

    @Test
    fun `una sesion caducada dice que los datos siguen ahi`() {
        val d = diagnoseStartupError("[401 unauthenticated] falta el header Authorization")

        assertEquals("Tu sesión expiró", d.title)
        // Quien ve esta pantalla necesita saber que no perdió su racha.
        assertTrue("siguen" in d.advice)
    }

    @Test
    fun `un fallo de red si manda a revisar la conexion`() {
        val d = diagnoseStartupError("network is unreachable")

        assertEquals("Sin conexión", d.title)
        assertTrue("internet" in d.advice)
    }

    @Test
    fun `un error desconocido no inventa una causa`() {
        val d = diagnoseStartupError("algo raro paso")

        assertEquals("No pudimos conectar", d.title)
        assertEquals("algo raro paso", d.technicalDetail)
    }

    @Test
    fun `un mensaje vacio o nulo sigue dando algo legible`() {
        listOf(null, "").forEach { raw ->
            val d = diagnoseStartupError(raw)
            assertTrue(d.title.isNotBlank())
            assertTrue(d.advice.isNotBlank())
            assertTrue(d.technicalDetail.isNotBlank())
        }
    }
}
