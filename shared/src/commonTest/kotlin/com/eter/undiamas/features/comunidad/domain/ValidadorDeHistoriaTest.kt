package com.eter.undiamas.features.comunidad.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidadorDeHistoriaTest {

    private val validador = ValidadorDeHistoria()
    private val cuerpoValido = "a".repeat(CUERPO_MINIMO)
    private val perfilConPermiso = PerfilDeComunidad(alias = "Ana R.", diasDeRacha = 90, puedePublicar = true)

    private fun borrador(titulo: String = "Lo que me funcionó", cuerpo: String = cuerpoValido) =
        BorradorDeHistoria(titulo = titulo, cuerpo = cuerpo)

    @Test
    fun `un borrador completo se puede enviar`() {
        assertTrue(validador.puedeEnviar(borrador(), perfilConPermiso))
    }

    @Test
    fun `sin la racha minima no se puede publicar`() {
        val sinPermiso = perfilConPermiso.copy(diasDeRacha = 12, puedePublicar = false)

        val errores = validador.errores(borrador(), sinPermiso)

        assertTrue(ProblemaDeHistoria.RachaInsuficiente in errores)
    }

    @Test
    fun `sin alias no se publica`() {
        val errores = validador.errores(borrador(), perfilConPermiso.copy(alias = ""))

        assertTrue(ProblemaDeHistoria.SinAlias in errores)
    }

    @Test
    fun `un texto demasiado corto no cuenta una experiencia`() {
        val errores = validador.errores(borrador(cuerpo = "Me costó."), perfilConPermiso)

        assertTrue(ProblemaDeHistoria.CuerpoCorto in errores)
    }

    @Test
    fun `respeta los limites de longitud`() {
        val largo = validador.errores(
            borrador(titulo = "t".repeat(TITULO_MAXIMO + 1), cuerpo = "c".repeat(CUERPO_MAXIMO + 1)),
            perfilConPermiso,
        )

        assertTrue(ProblemaDeHistoria.TituloLargo in largo)
        assertTrue(ProblemaDeHistoria.CuerpoLargo in largo)
    }

    @Test
    fun `el titulo vacio se detecta aunque tenga espacios`() {
        val errores = validador.errores(borrador(titulo = "    "), perfilConPermiso)

        assertTrue(ProblemaDeHistoria.TituloVacio in errores)
    }

    // ---- Datos personales: avisan, no bloquean ------------------------------------

    @Test
    fun `avisa si escribio un correo`() {
        val avisos = validador.avisos(borrador(cuerpo = "$cuerpoValido escríbeme a ana@correo.mx"))

        assertEquals(1, avisos.size)
        assertTrue("correo" in avisos.first().mensaje)
    }

    @Test
    fun `avisa si escribio un telefono`() {
        val avisos = validador.avisos(borrador(cuerpo = "$cuerpoValido mi cel 618 123 4567"))

        assertTrue(avisos.any { "teléfono" in it.mensaje })
    }

    @Test
    fun `avisa si escribio un enlace`() {
        val avisos = validador.avisos(borrador(cuerpo = "$cuerpoValido mira https://ejemplo.mx/algo"))

        assertTrue(avisos.any { "enlace" in it.mensaje })
    }

    /**
     * El aviso no bloquea: alguien puede querer dejar su contacto a propósito, y esa
     * decisión es suya. Lo que no puede es publicarlo sin darse cuenta.
     */
    @Test
    fun `un dato personal avisa pero deja publicar`() {
        val con = borrador(cuerpo = "$cuerpoValido ana@correo.mx")

        assertTrue(validador.avisos(con).isNotEmpty())
        assertTrue(validador.puedeEnviar(con, perfilConPermiso))
    }

    @Test
    fun `un texto limpio no genera avisos`() {
        assertTrue(validador.avisos(borrador()).isEmpty())
    }

    @Test
    fun `una fecha no se confunde con un telefono`() {
        val avisos = validador.avisos(borrador(cuerpo = "$cuerpoValido empecé el 3 de agosto"))

        assertTrue(avisos.isEmpty(), "falso positivo: $avisos")
    }
}
