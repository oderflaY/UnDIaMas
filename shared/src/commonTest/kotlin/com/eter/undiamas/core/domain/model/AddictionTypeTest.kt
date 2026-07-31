package com.eter.undiamas.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddictionTypeTest {

    @Test
    fun `las sustancias quedan marcadas como tales`() {
        // Es lo que decide si el aviso menciona abstinencia y atención médica.
        assertTrue(AddictionType.ALCOHOL.substance)
        assertTrue(AddictionType.NICOTINA.substance)
        assertTrue(AddictionType.OPIOIDES.substance)
        assertTrue(AddictionType.ESTIMULANTES.substance)
        assertTrue(AddictionType.CANNABIS.substance)
    }

    @Test
    fun `las conductas no se tratan como sustancia`() {
        assertFalse(AddictionType.JUEGO.substance)
        assertFalse(AddictionType.PANTALLAS.substance)
        assertFalse(AddictionType.COMPRAS.substance)
        assertFalse(AddictionType.OTRA.substance)
    }

    @Test
    fun `toda opcion tiene titulo y descripcion legibles para el carrusel`() {
        AddictionType.entries.forEach { option ->
            assertTrue(option.title.isNotBlank(), "Sin título: ${option.name}")
            assertTrue(option.description.isNotBlank(), "Sin descripción: ${option.name}")
        }
    }

    @Test
    fun `el nombre guardado en preferencias resuelve de vuelta al mismo tipo`() {
        // UserPreferences guarda enum.name; si eso deja de resolver, se pierde la selección.
        AddictionType.entries.forEach { option ->
            val restored = AddictionType.entries.firstOrNull { it.name == option.name }
            assertEquals(option, restored)
        }
    }
}
