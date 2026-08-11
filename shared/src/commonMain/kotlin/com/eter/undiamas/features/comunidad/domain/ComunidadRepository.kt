package com.eter.undiamas.features.comunidad.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Puerto del muro de la comunidad.
 *
 * A diferencia del resto de la app, esto **no** es offline-first para escribir: publicar una
 * historia sin conexión y que salga sola horas después sería raro para quien la escribe y
 * peor para moderarla. Se lee de caché, se escribe solo con red.
 */
interface ComunidadRepository {
    val historias: StateFlow<List<Historia>>

    /** Alias, racha reconocida y si puede publicar. */
    val perfil: StateFlow<PerfilDeComunidad>

    /** true mientras se pide una página. */
    val cargando: StateFlow<Boolean>

    /** Cursor de la siguiente página, o null si ya no hay más. */
    val hayMas: StateFlow<Boolean>

    suspend fun refrescar(orden: OrdenHistorias = OrdenHistorias.RACHA)

    suspend fun cargarMas(orden: OrdenHistorias = OrdenHistorias.RACHA)

    suspend fun refrescarPerfil()

    /** Elige o cambia el alias público. Devuelve el perfil actualizado. */
    suspend fun guardarAlias(alias: String): PerfilDeComunidad

    suspend fun publicar(borrador: BorradorDeHistoria): Historia

    suspend fun borrar(historiaId: String)

    /** Marca o desmarca "me ayudó". */
    suspend fun marcarUtil(historiaId: String, util: Boolean)

    suspend fun reportar(historiaId: String, motivo: MotivoReporte, detalle: String)

    /**
     * Oculta a un autor para quien está mirando.
     *
     * Va aparte del reporte: reportar es "esto está mal y que alguien lo revise", bloquear
     * es "no quiero volver a leer a esta persona". La tienda exige las dos por separado.
     */
    suspend fun bloquearAutor(historiaId: String)
}
