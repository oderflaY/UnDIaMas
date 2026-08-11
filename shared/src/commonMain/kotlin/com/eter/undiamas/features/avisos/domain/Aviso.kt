package com.eter.undiamas.features.avisos.domain

import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.domain.model.TrustedContact
import com.eter.undiamas.features.anclas.domain.Anchor

/** De qué habla el aviso. Sirve para no repetir dos del mismo tipo seguidos. */
enum class CategoriaAviso {
    /** "Llevas 42 días": el contador y el récord. */
    RACHA,

    /** Recuerda por qué empezó: sus anclas y su "por qué" personal. */
    ANCLA,

    /** Lo que lleva ahorrado y qué podría hacer con ello. */
    AHORRO,

    /** Ofrece una salida concreta: respirar, llamar, abrir el búnker. */
    AYUDA,

    /** Le pide que registre el día. */
    CHECKIN,
}

/**
 * Todo lo que hace falta para redactar un aviso con datos de verdad.
 *
 * Se pasa entero en vez de ir pidiendo cosas sueltas porque las plantillas eligen qué usar:
 * una de ahorro necesita el importe, una de ancla necesita las anclas, y ninguna debería
 * poder consultar nada que no esté aquí.
 */
data class ContextoAviso(
    val nombre: String = "",
    val nivel: RiskLevel = RiskLevel.VERDE,
    val dias: Long = 0,
    val recordDias: Long = 0,
    val ahorro: Double = 0.0,
    val moneda: String = "MXN",
    val anclas: List<Anchor> = emptyList(),
    val porQue: String = "",
    val contacto: TrustedContact? = null,
    val checkInHecho: Boolean = false,
) {
    /** Nombre utilizable en un texto, sin dejar huecos raros si no lo puso. */
    val nombreCorto: String get() = nombre.trim().takeIf { it.isNotBlank() } ?: ""

    val faltaParaRecord: Long get() = (recordDias - dias).coerceAtLeast(0)
}

/** Un aviso ya redactado, listo para que la plataforma lo muestre. */
data class Aviso(
    val id: String,
    val categoria: CategoriaAviso,
    val titulo: String,
    val cuerpo: String,
    /**
     * true solo para los avisos de ayuda en rojo.
     *
     * Es lo único que puede sonar de noche y saltarse el silencio: si alguien está en rojo
     * a las tres de la mañana, ese es exactamente el momento en el que hace falta.
     */
    val urgente: Boolean = false,
)

/** Aviso con la hora a la que toca mostrarlo. [minutosDelDia] va de 0 a 1439. */
data class AvisoProgramado(
    val aviso: Aviso,
    val minutosDelDia: Int,
) {
    val hora: Int get() = minutosDelDia / 60
    val minuto: Int get() = minutosDelDia % 60
}
