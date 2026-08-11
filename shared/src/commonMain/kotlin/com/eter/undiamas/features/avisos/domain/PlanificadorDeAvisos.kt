package com.eter.undiamas.features.avisos.domain

import com.eter.undiamas.core.domain.model.RiskLevel

/**
 * Cuántos avisos y cada cuánto, según el semáforo.
 *
 * La idea de fondo: en verde la app estorba lo mínimo, y cuanto peor va el día, más
 * presente se pone. En rojo hay uno cada hora y media porque el impulso viene en oleadas y
 * lo que sirve es que alguien aparezca **durante** la oleada, no dos horas después.
 */
data class Cadencia(
    val avisosPorDia: Int,
    val primeraHora: Int,
    val ultimaHora: Int,
) {
    companion object {
        /** Dos al día: uno al despertar y otro al cerrar. Nada más. */
        val VERDE = Cadencia(avisosPorDia = 2, primeraHora = 9, ultimaHora = 21)

        /** Cinco: el día pide atención, pero todavía no es una crisis. */
        val AMARILLO = Cadencia(avisosPorDia = 5, primeraHora = 8, ultimaHora = 22)

        /** Once, uno cada hora y media. Es el nivel en el que la app se pone pesada a propósito. */
        val ROJO = Cadencia(avisosPorDia = 11, primeraHora = 7, ultimaHora = 23)

        fun para(nivel: RiskLevel): Cadencia = when (nivel) {
            RiskLevel.VERDE -> VERDE
            RiskLevel.AMARILLO -> AMARILLO
            RiskLevel.ROJO -> ROJO
        }
    }
}

/**
 * Arma el plan de avisos de un día.
 *
 * Es determinista: con el mismo contexto y la misma semilla sale el mismo plan. Eso permite
 * probarlo de verdad, y evita que dos ejecuciones del programador el mismo día generen
 * avisos distintos y acaben duplicándose en la barra de notificaciones.
 */
class PlanificadorDeAvisos(
    private val plantillas: List<PlantillaAviso> = PLANTILLAS,
) {

    /**
     * Plantillas que se pueden usar con este contexto y este nivel.
     *
     * Filtrar por [PlantillaAviso.disponible] es lo que evita mandar "Has ahorrado $0.00" a
     * quien nunca declaró un gasto, o hablar de un contacto de confianza que no existe.
     */
    fun disponibles(contexto: ContextoAviso): List<PlantillaAviso> =
        plantillas.filter { contexto.nivel in it.aplicaA && it.disponible(contexto) }

    /**
     * El plan del día, con la hora de cada aviso.
     *
     * Los avisos se reparten en franjas iguales entre la primera y la última hora, y se
     * eligen alternando categoría para que no lleguen tres de ahorro seguidos. Si hay menos
     * plantillas que huecos, se reutilizan: es preferible repetir un mensaje útil a dejar de
     * acompañar a alguien que está en rojo.
     */
    fun planDelDia(contexto: ContextoAviso, semilla: Int = 0): List<AvisoProgramado> {
        val cadencia = Cadencia.para(contexto.nivel)
        val candidatas = disponibles(contexto)
        if (candidatas.isEmpty()) return emptyList()

        val elegidas = repartirPorCategoria(candidatas, cadencia.avisosPorDia, semilla)

        val inicio = cadencia.primeraHora * 60
        val fin = cadencia.ultimaHora * 60
        val huecos = elegidas.size
        val paso = if (huecos > 1) (fin - inicio) / (huecos - 1) else 0

        return elegidas.mapIndexed { indice, plantilla ->
            AvisoProgramado(
                aviso = redactar(plantilla, contexto, indice),
                minutosDelDia = (inicio + paso * indice).coerceIn(0, 23 * 60 + 59),
            )
        }
    }

    /** Redacta una plantilla concreta. [ordinal] hace único el id dentro del día. */
    fun redactar(plantilla: PlantillaAviso, contexto: ContextoAviso, ordinal: Int = 0): Aviso = Aviso(
        id = "${plantilla.id}_$ordinal",
        categoria = plantilla.categoria,
        titulo = plantilla.titulo(contexto),
        cuerpo = plantilla.cuerpo(contexto),
        urgente = plantilla.urgente,
    )

    /**
     * Un aviso suelto, para mostrar en el momento en que el semáforo cambia.
     *
     * En rojo devuelve siempre uno de ayuda: es lo único que sirve justo cuando alguien
     * acaba de registrar que está mal.
     */
    fun avisoInmediato(contexto: ContextoAviso, semilla: Int = 0): Aviso? {
        val todas = disponibles(contexto)
        val candidatas = if (contexto.nivel == RiskLevel.ROJO) {
            // Las urgentes primero: el instante en que alguien registra rojo es cuando peor
            // sienta un mensaje tibio. Si no hubiera ninguna, valen las de ayuda normales.
            todas.filter { it.urgente }
                .ifEmpty { todas.filter { it.categoria == CategoriaAviso.AYUDA } }
                .ifEmpty { todas }
        } else {
            todas
        }
        if (candidatas.isEmpty()) return null
        val indice = ((semilla % candidatas.size) + candidatas.size) % candidatas.size
        return redactar(candidatas[indice], contexto, ordinal = semilla)
    }

    /**
     * Reparte los huecos entre categorías dando la vuelta en round-robin.
     *
     * Sin esto, un contexto con muchas anclas llenaría el día entero de anclas y ninguna de
     * ayuda, que es justo la que hace falta cuando el semáforo está en rojo.
     */
    private fun repartirPorCategoria(
        candidatas: List<PlantillaAviso>,
        cuantos: Int,
        semilla: Int,
    ): List<PlantillaAviso> {
        val porCategoria = candidatas.groupBy { it.categoria }
        val categorias = ordenarCategorias(porCategoria.keys)
        val usadasPorCategoria = mutableMapOf<CategoriaAviso, Int>()

        return (0 until cuantos).map { hueco ->
            val categoria = categorias[(hueco + semilla) % categorias.size]
            val opciones = porCategoria.getValue(categoria)
            val yaUsadas = usadasPorCategoria.getOrElse(categoria) { 0 }
            usadasPorCategoria[categoria] = yaUsadas + 1
            opciones[(yaUsadas + semilla) % opciones.size]
        }
    }

    /** En rojo, la ayuda va primero: es la categoría que toca el primer hueco del día. */
    private fun ordenarCategorias(categorias: Set<CategoriaAviso>): List<CategoriaAviso> {
        val prioridad = listOf(
            CategoriaAviso.AYUDA,
            CategoriaAviso.ANCLA,
            CategoriaAviso.RACHA,
            CategoriaAviso.CHECKIN,
            CategoriaAviso.AHORRO,
        )
        return prioridad.filter { it in categorias }
    }
}
