package com.eter.undiamas.features.avisos.domain

import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.domain.model.SupportRole
import com.eter.undiamas.core.domain.model.TrustedContact
import com.eter.undiamas.features.anclas.domain.Anchor
import com.eter.undiamas.features.anclas.domain.AnchorKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlanificadorDeAvisosTest {

    private val planificador = PlanificadorDeAvisos()

    private fun contexto(
        nivel: RiskLevel = RiskLevel.VERDE,
        dias: Long = 42,
        record: Long = 47,
        ahorro: Double = 1250.5,
        anclas: List<Anchor> = listOf(
            Anchor("a1", "u1", "Mi hija Sofía", "Que me vea entero", AnchorKind.PERSONA),
            Anchor("a2", "u1", "Volver a correr", "", AnchorKind.META),
        ),
        porQue: String = "Quiero estar presente",
        contacto: TrustedContact? = TrustedContact("Luis", "555-1", SupportRole.PADRINO),
    ) = ContextoAviso(
        nombre = "Ana",
        nivel = nivel,
        dias = dias,
        recordDias = record,
        ahorro = ahorro,
        anclas = anclas,
        porQue = porQue,
        contacto = contacto,
    )

    // ---- Cantidad según el semáforo ---------------------------------------------

    @Test
    fun `en verde la app estorba lo minimo`() {
        val plan = planificador.planDelDia(contexto(RiskLevel.VERDE))

        assertEquals(2, plan.size)
    }

    @Test
    fun `en amarillo aparece mas veces`() {
        val plan = planificador.planDelDia(contexto(RiskLevel.AMARILLO))

        assertEquals(5, plan.size)
    }

    @Test
    fun `en rojo se pone presente todo el dia`() {
        val plan = planificador.planDelDia(contexto(RiskLevel.ROJO))

        assertEquals(11, plan.size)
    }

    @Test
    fun `mas riesgo, mas avisos`() {
        val verde = planificador.planDelDia(contexto(RiskLevel.VERDE)).size
        val amarillo = planificador.planDelDia(contexto(RiskLevel.AMARILLO)).size
        val rojo = planificador.planDelDia(contexto(RiskLevel.ROJO)).size

        assertTrue(verde < amarillo && amarillo < rojo, "$verde < $amarillo < $rojo")
    }

    // ---- Horarios ----------------------------------------------------------------

    @Test
    fun `los avisos caen dentro de la franja del dia y en orden`() {
        val plan = planificador.planDelDia(contexto(RiskLevel.ROJO))
        val cadencia = Cadencia.ROJO

        plan.forEach { programado ->
            assertTrue(
                programado.hora in cadencia.primeraHora..cadencia.ultimaHora,
                "aviso a las ${programado.hora}h, fuera de la franja",
            )
        }
        // Nada de notificaciones de madrugada: en rojo el primero es a las 7.
        assertEquals(cadencia.primeraHora, plan.first().hora)
        assertTrue(plan.map { it.minutosDelDia } == plan.map { it.minutosDelDia }.sorted())
    }

    @Test
    fun `no se amontonan dos a la misma hora`() {
        val plan = planificador.planDelDia(contexto(RiskLevel.ROJO))

        assertEquals(plan.size, plan.map { it.minutosDelDia }.distinct().size)
    }

    // ---- Elección de contenido ---------------------------------------------------

    /**
     * Lo más importante del reparto: en rojo tiene que haber ayuda de verdad, no solo
     * ánimos. Un día entero de "llevas 42 días" a alguien en crisis no ofrece ninguna salida.
     */
    @Test
    fun `en rojo hay avisos de ayuda y el primero lo es`() {
        val plan = planificador.planDelDia(contexto(RiskLevel.ROJO))

        val ayudas = plan.count { it.aviso.categoria == CategoriaAviso.AYUDA }
        assertTrue(ayudas >= 2, "solo $ayudas avisos de ayuda en rojo")
        assertEquals(CategoriaAviso.AYUDA, plan.first().aviso.categoria)
    }

    @Test
    fun `en verde no se cuelan los avisos de crisis`() {
        val plan = planificador.planDelDia(contexto(RiskLevel.VERDE))

        val urgentes = plan.filter { it.aviso.urgente }
        assertTrue(urgentes.isEmpty(), "no debería haber avisos urgentes en verde: $urgentes")
    }

    @Test
    fun `reparte entre categorias en vez de repetir una sola`() {
        val plan = planificador.planDelDia(contexto(RiskLevel.ROJO))

        val categorias = plan.map { it.aviso.categoria }.distinct()
        assertTrue(categorias.size >= 3, "solo salieron $categorias")
    }

    @Test
    fun `el mismo contexto da siempre el mismo plan`() {
        val uno = planificador.planDelDia(contexto(RiskLevel.AMARILLO), semilla = 3)
        val otro = planificador.planDelDia(contexto(RiskLevel.AMARILLO), semilla = 3)

        assertEquals(uno, otro)
    }

    // ---- Datos que faltan ---------------------------------------------------------

    /**
     * Sin gasto declarado, "Has ahorrado $0.00" es peor que no mandar nada: subraya que la
     * persona no configuró algo, en vez de acompañarla.
     */
    @Test
    fun `sin ahorro no se manda ningun aviso de dinero`() {
        val plan = planificador.planDelDia(contexto(RiskLevel.ROJO, ahorro = 0.0))

        assertTrue(plan.none { it.aviso.categoria == CategoriaAviso.AHORRO })
    }

    @Test
    fun `sin anclas no se habla de anclas`() {
        val plan = planificador.planDelDia(
            contexto(RiskLevel.AMARILLO, anclas = emptyList(), porQue = ""),
        )

        assertTrue(plan.none { it.aviso.categoria == CategoriaAviso.ANCLA })
    }

    @Test
    fun `sin contacto de confianza no se le nombra`() {
        val plan = planificador.planDelDia(contexto(RiskLevel.ROJO, contacto = null))

        assertTrue(plan.none { it.aviso.id.startsWith("rojo_llama_contacto") })
    }

    @Test
    fun `un contexto vacio no revienta ni manda textos a medias`() {
        val plan = planificador.planDelDia(ContextoAviso(nivel = RiskLevel.ROJO))

        assertTrue(plan.isNotEmpty())
        plan.forEach {
            assertTrue(it.aviso.titulo.isNotBlank(), "título vacío en ${it.aviso.id}")
            assertTrue(it.aviso.cuerpo.isNotBlank(), "cuerpo vacío en ${it.aviso.id}")
        }
    }

    // ---- Contenido de los textos ---------------------------------------------------

    @Test
    fun `los avisos usan los datos reales de la persona`() {
        val ctx = contexto(RiskLevel.ROJO)
        val textos = planificador.disponibles(ctx)
            .map { planificador.redactar(it, ctx) }
            .joinToString(" ") { "${it.titulo} ${it.cuerpo}" }

        assertTrue("42 días" in textos, "debería nombrar la racha")
        assertTrue("Mi hija Sofía" in textos, "debería nombrar el ancla")
        assertTrue("Luis" in textos, "debería nombrar al contacto de confianza")
        assertTrue("1250.50" in textos, "debería nombrar el ahorro")
        assertTrue("Quiero estar presente" in textos, "debería nombrar su por qué")
    }

    /**
     * Ni una plantilla puede culpar. Alguien a punto de consumir que lee un reproche se
     * aleja de la app justo cuando más la necesita.
     */
    @Test
    fun `ninguna plantilla culpa a la persona`() {
        val prohibidas = listOf(
            "fallaste", "fracaso", "recaíste", "otra vez", "deberías",
            "no puedes", "mal hecho", "decepcion",
        )
        val ctx = contexto(RiskLevel.ROJO)

        PLANTILLAS.forEach { plantilla ->
            val texto = "${plantilla.titulo(ctx)} ${plantilla.cuerpo(ctx)}".lowercase()
            prohibidas.forEach { palabra ->
                assertTrue(palabra !in texto, "«$palabra» en la plantilla ${plantilla.id}: $texto")
            }
        }
    }

    @Test
    fun `solo los avisos de rojo pueden ser urgentes`() {
        PLANTILLAS.filter { it.urgente }.forEach { plantilla ->
            assertEquals(
                setOf(RiskLevel.ROJO),
                plantilla.aplicaA,
                "${plantilla.id} es urgente pero no es exclusiva de rojo",
            )
        }
    }

    @Test
    fun `hay plantillas de sobra para llenar un dia en rojo`() {
        val disponibles = planificador.disponibles(contexto(RiskLevel.ROJO))

        assertTrue(disponibles.size >= Cadencia.ROJO.avisosPorDia, "solo ${disponibles.size}")
    }

    @Test
    fun `no hay dos plantillas con el mismo id`() {
        assertEquals(PLANTILLAS.size, PLANTILLAS.map { it.id }.distinct().size)
    }

    // ---- Aviso inmediato ------------------------------------------------------------

    @Test
    fun `al ponerse en rojo el aviso inmediato ofrece ayuda`() {
        val aviso = assertNotNull(planificador.avisoInmediato(contexto(RiskLevel.ROJO)))

        assertEquals(CategoriaAviso.AYUDA, aviso.categoria)
        assertTrue(aviso.urgente)
    }

    @Test
    fun `el importe se formatea con dos decimales`() {
        assertEquals("$1250.50 MXN", dinero(1250.5, "MXN"))
        assertEquals("$0.99 MXN", dinero(0.99, "MXN"))
        assertEquals("$100.00 MXN", dinero(100.0, "MXN"))
    }
}
