package com.eter.undiamas.core.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StartupDiagnosisTest {

    @Test
    fun `auth sin habilitar no culpa a la conexion del usuario`() {
        val d = diagnoseStartupError("An internal error has occurred. [ CONFIGURATION_NOT_FOUND ]")

        assertEquals("Falta configurar el servidor", d.title)
        assertTrue("Firebase" in d.advice)
        // El fallo es del backend: mandar a revisar el internet seria mandar a buscar mal.
        assertTrue("internet" !in d.advice.lowercase() || "conexión está bien" in d.advice)
    }

    @Test
    fun `operacion restringida por admin se trata como falta de configuracion`() {
        val d = diagnoseStartupError("ADMIN_RESTRICTED_OPERATION")

        assertEquals("Falta configurar el servidor", d.title)
    }

    @Test
    fun `permisos de firestore apuntan a las reglas`() {
        val d = diagnoseStartupError("PERMISSION_DENIED: Missing or insufficient permissions")

        assertEquals("Sin permisos para leer tus datos", d.title)
        assertTrue("reglas" in d.advice)
    }

    @Test
    fun `un fallo de red si manda a revisar la conexion`() {
        val d = diagnoseStartupError("Unable to resolve host firebaseio.com")

        assertEquals("Sin conexión", d.title)
        assertTrue("internet" in d.advice)
    }

    @Test
    fun `una clave invalida senala el archivo de configuracion`() {
        val d = diagnoseStartupError("API key not valid. Please pass a valid API key.")

        assertEquals("Configuración inválida", d.title)
        assertTrue("google-services.json" in d.advice)
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
