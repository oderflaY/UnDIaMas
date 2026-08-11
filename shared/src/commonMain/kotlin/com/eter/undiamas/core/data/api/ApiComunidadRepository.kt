package com.eter.undiamas.core.data.api

import com.eter.undiamas.core.domain.model.AddictionType
import com.eter.undiamas.features.comunidad.domain.BorradorDeHistoria
import com.eter.undiamas.features.comunidad.domain.ComunidadRepository
import com.eter.undiamas.features.comunidad.domain.EstadoHistoria
import com.eter.undiamas.features.comunidad.domain.Historia
import com.eter.undiamas.features.comunidad.domain.MotivoReporte
import com.eter.undiamas.features.comunidad.domain.OrdenHistorias
import com.eter.undiamas.features.comunidad.domain.PerfilDeComunidad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Muro de la comunidad contra el backend.
 *
 * Las historias se cachean en memoria, no en SQLite: son contenido de otras personas que
 * cambia y se modera, y guardarlo en el teléfono significaría conservar textos que quizá ya
 * fueron retirados. Lo que sí sobrevive sin conexión es lo propio de la persona, que está
 * en el resto de la app.
 */
class ApiComunidadRepository(
    private val api: UnDiaMasApi,
) : ComunidadRepository {

    private val _historias = MutableStateFlow<List<Historia>>(emptyList())
    override val historias: StateFlow<List<Historia>> = _historias.asStateFlow()

    private val _perfil = MutableStateFlow(PerfilDeComunidad())
    override val perfil: StateFlow<PerfilDeComunidad> = _perfil.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    override val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _hayMas = MutableStateFlow(false)
    override val hayMas: StateFlow<Boolean> = _hayMas.asStateFlow()

    private var cursor: String? = null

    override suspend fun refrescar(orden: OrdenHistorias) {
        _cargando.value = true
        try {
            val pagina = api.historias(orden = orden.paraApi(), cursor = null)
            _historias.value = pagina.items.map { it.toDomain() }
            cursor = pagina.siguienteCursor
            _hayMas.value = pagina.siguienteCursor != null
        } finally {
            _cargando.value = false
        }
    }

    override suspend fun cargarMas(orden: OrdenHistorias) {
        val siguiente = cursor ?: return
        if (_cargando.value) return
        _cargando.value = true
        try {
            val pagina = api.historias(orden = orden.paraApi(), cursor = siguiente)
            // Se filtran los ids repetidos: si alguien publicó mientras se leía la página
            // anterior, el servidor puede devolver una historia que ya está en pantalla.
            val nuevos = pagina.items.map { it.toDomain() }
                .filter { nueva -> _historias.value.none { it.id == nueva.id } }
            _historias.value = _historias.value + nuevos
            cursor = pagina.siguienteCursor
            _hayMas.value = pagina.siguienteCursor != null
        } finally {
            _cargando.value = false
        }
    }

    override suspend fun refrescarPerfil() {
        _perfil.value = api.perfilDeComunidad().toDomain()
    }

    override suspend fun guardarAlias(alias: String): PerfilDeComunidad =
        api.guardarAlias(alias.trim()).toDomain().also { _perfil.value = it }

    override suspend fun publicar(borrador: BorradorDeHistoria): Historia {
        val creada = api.crearHistoria(
            CrearHistoriaRequest(
                titulo = borrador.titulo.trim(),
                cuerpo = borrador.cuerpo.trim(),
                objetivo = borrador.objetivo?.name,
                compartirRacha = borrador.compartirRacha,
            ),
        ).toDomain()
        // Al frente aunque el muro ordene por racha: quien acaba de publicar tiene que ver
        // su historia, no buscarla entre las de gente con más días.
        _historias.value = listOf(creada) + _historias.value
        _perfil.value = _perfil.value.copy(
            historiasPublicadas = _perfil.value.historiasPublicadas + 1,
        )
        return creada
    }

    override suspend fun borrar(historiaId: String) {
        api.borrarHistoria(historiaId)
        _historias.value = _historias.value.filterNot { it.id == historiaId }
    }

    override suspend fun marcarUtil(historiaId: String, util: Boolean) {
        val actualizada = api.marcarHistoriaUtil(historiaId, util).toDomain()
        _historias.value = _historias.value.map { if (it.id == historiaId) actualizada else it }
    }

    override suspend fun reportar(historiaId: String, motivo: MotivoReporte, detalle: String) {
        api.reportarHistoria(historiaId, ReporteRequest(motivo.name, detalle.trim()))
        // Se quita de la vista al instante: quien acaba de reportar algo no debería seguir
        // teniéndolo delante mientras alguien lo revisa.
        _historias.value = _historias.value.filterNot { it.id == historiaId }
    }

    override suspend fun bloquearAutor(historiaId: String) {
        api.bloquearAutorDe(historiaId)
        // El servidor ya no se lo mandará; en pantalla se va todo lo suyo ahora mismo.
        val alias = _historias.value.firstOrNull { it.id == historiaId }?.alias
        _historias.value = _historias.value.filterNot { it.alias == alias }
    }

    private fun OrdenHistorias.paraApi(): String = when (this) {
        OrdenHistorias.RACHA -> "racha"
        OrdenHistorias.RECIENTE -> "reciente"
        OrdenHistorias.UTILES -> "utiles"
    }
}

fun HistoriaDto.toDomain(): Historia = Historia(
    id = id,
    alias = alias,
    diasDeRacha = diasDeRacha,
    objetivo = objetivo?.let { valor ->
        AddictionType.entries.firstOrNull { it.name.equals(valor, ignoreCase = true) }
    },
    titulo = titulo,
    cuerpo = cuerpo,
    publicadaEn = createdAt.toInstantOrNow(),
    estado = EstadoHistoria.entries.firstOrNull { it.name.equals(estado, ignoreCase = true) }
        ?: EstadoHistoria.PUBLICADA,
    utiles = utiles,
    marcada = marcada,
    esMia = esMia,
)

fun PerfilComunidadDto.toDomain(): PerfilDeComunidad = PerfilDeComunidad(
    alias = alias,
    diasDeRacha = diasDeRacha,
    puedePublicar = puedePublicar,
    historiasPublicadas = historiasPublicadas,
)
