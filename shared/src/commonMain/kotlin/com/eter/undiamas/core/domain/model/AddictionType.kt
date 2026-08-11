package com.eter.undiamas.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Qué está intentando dejar la persona. Personaliza los mensajes de la app y el texto
 * de las alertas biométricas.
 *
 * [substance] separa las sustancias de las conductas: en las primeras la abstinencia
 * puede tener síntomas físicos reales, y por eso el aviso remite a atención médica.
 */
@Serializable
enum class AddictionType(
    val title: String,
    val description: String,
    val substance: Boolean,
) {
    ALCOHOL(
        title = "Alcohol",
        description = "Cerveza, vino o destilados",
        substance = true,
    ),
    NICOTINA(
        title = "Nicotina",
        description = "Cigarro, vapeo o tabaco",
        substance = true,
    ),
    OPIOIDES(
        title = "Opioides",
        description = "Analgésicos fuertes u opiáceos",
        substance = true,
    ),
    ESTIMULANTES(
        title = "Estimulantes",
        description = "Cocaína, anfetaminas o similares",
        substance = true,
    ),
    CANNABIS(
        title = "Cannabis",
        description = "Marihuana en cualquier forma",
        substance = true,
    ),
    JUEGO(
        title = "Juego",
        description = "Apuestas, casino o loterías",
        substance = false,
    ),
    PANTALLAS(
        title = "Pantallas",
        description = "Redes, videojuegos o navegación",
        substance = false,
    ),
    COMPRAS(
        title = "Compras",
        description = "Gasto impulsivo o compulsivo",
        substance = false,
    ),
    /**
     * No es una adicción, y por eso [substance] es false: la abstinencia no tiene síntomas
     * físicos que remitan a urgencias. Está aquí porque muchas personas usan la app para
     * llevar la cuenta de días sin hacerlo, y el contador y la red de apoyo sirven igual.
     */
    AUTOLESIONES(
        title = "Autolesiones",
        description = "Días sin hacerme daño",
        substance = false,
    ),
    ANSIEDAD(
        title = "Ansiedad",
        description = "Manejo de crisis y rumiación",
        substance = false,
    ),
    OTRA(
        title = "Otra",
        description = "Prefiero no especificarla",
        substance = false,
    ),
}
