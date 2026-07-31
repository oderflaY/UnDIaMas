package com.eter.undiamas.features.anclas.domain

import kotlinx.serialization.Serializable

/** Categoría del ancla, que define su color e icono en el muro. */
@Serializable
enum class AnchorKind(val label: String) {
    PERSONA("Persona"),
    MASCOTA("Mascota"),
    META("Meta"),
    LUGAR("Lugar"),
    RECUERDO("Recuerdo"),
}

/**
 * Aquello que la persona no quiere perder. En un impulso fuerte el cerebro racional no
 * lee bien, así que el muro funciona con estímulos cortos y muy visuales.
 */
@Serializable
data class Anchor(
    val id: String,
    val userId: String,
    val title: String,
    val note: String,
    val kind: AnchorKind,
    /** Índice que determina la altura de la tarjeta en el mosaico, para que no quede uniforme. */
    val tileSeed: Int = 0,
)
