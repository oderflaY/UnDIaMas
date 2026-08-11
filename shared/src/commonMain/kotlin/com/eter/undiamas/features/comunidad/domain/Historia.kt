package com.eter.undiamas.features.comunidad.domain

import com.eter.undiamas.core.domain.model.AddictionType
import kotlin.time.Instant

/**
 * Días de racha mínimos para poder publicar.
 *
 * La sección es "quien lleva más tiempo cuenta cómo lo hizo", así que hace falta un umbral.
 * 30 días es suficiente para tener algo que contar y lo bastante bajo para no convertir el
 * muro en un club de veteranos: alguien con dos meses le habla mucho mejor a quien lleva
 * dos días que alguien con cinco años.
 */
const val DIAS_MINIMOS_PARA_PUBLICAR = 30

/** Límites de texto. Los mismos que debe validar el backend, para fallar aquí y no allá. */
const val TITULO_MAXIMO = 80
const val CUERPO_MINIMO = 120
const val CUERPO_MAXIMO = 4000

/**
 * En qué estado está una historia.
 *
 * La moderación no es opcional: una tienda de apps exige poder revisar y retirar contenido
 * de usuarios, y aquí además se habla de recaídas y consumo. [EN_REVISION] existe para que
 * el backend pueda retener una publicación sin borrarla mientras alguien la mira.
 */
enum class EstadoHistoria {
    PUBLICADA,
    EN_REVISION,
    RETIRADA,
}

/** Cómo se ordena el muro. */
enum class OrdenHistorias {
    /** Por días de racha del autor. Es el orden por defecto de la sección. */
    RACHA,

    /** Lo último publicado. */
    RECIENTE,

    /** Lo que más gente marcó como útil. */
    UTILES,
}

/** Motivo por el que alguien reporta una historia. */
enum class MotivoReporte(val etiqueta: String) {
    APOLOGIA("Anima a consumir"),
    DATOS_PERSONALES("Expone datos personales"),
    ODIO("Agrede o discrimina"),
    SPAM("Spam o publicidad"),
    OTRO("Otro motivo"),
}

/**
 * Una experiencia publicada en el muro.
 *
 * El autor va como [alias], nunca con su nombre real ni su correo: quien comparte que está
 * en recuperación no puede quedar identificado por hacerlo. El servidor no debe devolver
 * jamás el id de usuario del autor a otros usuarios.
 */
data class Historia(
    val id: String,
    val alias: String,
    /** Días de racha del autor en el momento de publicar. Congelado, no se recalcula. */
    val diasDeRacha: Long,
    /** Qué está dejando. Null si el autor prefirió no decirlo. */
    val objetivo: AddictionType?,
    val titulo: String,
    val cuerpo: String,
    val publicadaEn: Instant,
    val estado: EstadoHistoria = EstadoHistoria.PUBLICADA,
    /** Cuánta gente la marcó como "me ayudó". */
    val utiles: Int = 0,
    /** Si quien está mirando ya la marcó. */
    val marcada: Boolean = false,
    /** Si la escribió quien está mirando, para poder ofrecerle borrarla. */
    val esMia: Boolean = false,
)

/** Lo que la app sabe del usuario respecto a la comunidad. */
data class PerfilDeComunidad(
    /** Alias público. Vacío si todavía no eligió uno. */
    val alias: String = "",
    /** Días de racha que el servidor le reconoce. */
    val diasDeRacha: Long = 0,
    /** Si el servidor le permite publicar ahora mismo. */
    val puedePublicar: Boolean = false,
    /** Cuántas historias ha publicado. */
    val historiasPublicadas: Int = 0,
)

/** Borrador de una historia, antes de mandarla. */
data class BorradorDeHistoria(
    val titulo: String = "",
    val cuerpo: String = "",
    val objetivo: AddictionType? = null,
    /** Si acepta que se muestren sus días de racha junto al texto. */
    val compartirRacha: Boolean = true,
)
