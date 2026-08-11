package com.eter.undiamas.core.data.api

import kotlinx.serialization.Serializable

/*
 * Contrato de la sección de comunidad.
 *
 * Mismas reglas que el resto de la API: nombres exactos, todo con valor por defecto, y el
 * id del usuario nunca viaja en el cuerpo — el servidor lo saca del token.
 *
 * Regla propia de esta sección: **el servidor no debe devolver nunca el id de usuario del
 * autor**. Solo el alias. Cruzar el alias con el resto de la API permitiría identificar a
 * quien publicó, y aquí la gente cuenta que está en recuperación.
 */

@Serializable
data class HistoriaDto(
    val id: String = "",
    val alias: String = "",
    /** Días de racha del autor al publicar. Congelado por el servidor, no se recalcula. */
    val diasDeRacha: Long = 0,
    /** ALCOHOL, NICOTINA… o null si el autor no quiso decirlo. */
    val objetivo: String? = null,
    val titulo: String = "",
    val cuerpo: String = "",
    val createdAt: String? = null,
    /** PUBLICADA | EN_REVISION | RETIRADA */
    val estado: String = "PUBLICADA",
    val utiles: Int = 0,
    /** Si quien pide ya la marcó como útil. */
    val marcada: Boolean = false,
    /** Si la escribió quien pide. */
    val esMia: Boolean = false,
)

@Serializable
data class CrearHistoriaRequest(
    val titulo: String,
    val cuerpo: String,
    val objetivo: String?,
    /** Si false, el servidor guarda 0 en diasDeRacha y no lo publica. */
    val compartirRacha: Boolean,
)

@Serializable
data class PerfilComunidadDto(
    val alias: String = "",
    val diasDeRacha: Long = 0,
    val puedePublicar: Boolean = false,
    val historiasPublicadas: Int = 0,
    /** Cuántos días le faltan para poder publicar. 0 si ya puede. */
    val diasParaPublicar: Long = 0,
)

@Serializable
data class AliasRequest(val alias: String)

@Serializable
data class ReporteRequest(
    /** APOLOGIA | DATOS_PERSONALES | ODIO | SPAM | OTRO */
    val motivo: String,
    val detalle: String = "",
)

@Serializable
data class UtilRequest(val util: Boolean)

/**
 * Página de historias.
 *
 * Va con cursor y no con número de página: el muro se ordena por racha, y con paginación
 * por número una historia nueva desplazaría a las demás y saldrían repetidas al avanzar.
 */
@Serializable
data class PaginaHistoriasDto(
    val items: List<HistoriaDto> = emptyList(),
    /** Se pasa como `cursor` en la siguiente llamada. null cuando ya no hay más. */
    val siguienteCursor: String? = null,
)
