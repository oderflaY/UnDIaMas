package com.eter.undiamas.core.data.local

import com.eter.undiamas.core.data.api.ApiErrorCode
import com.eter.undiamas.core.data.api.ApiException
import com.eter.undiamas.core.data.api.CreateCheckInRequest
import com.eter.undiamas.core.data.api.CreateJournalRequest
import com.eter.undiamas.core.data.api.CreateMoodRequest
import com.eter.undiamas.core.data.api.CreateRelapseRequest
import com.eter.undiamas.core.data.api.CreateTrafficLightRequest
import com.eter.undiamas.core.data.api.EmergencyContactsRequest
import com.eter.undiamas.core.data.api.ReminderDto
import com.eter.undiamas.core.data.api.UnDiaMasApi
import com.eter.undiamas.core.data.api.UpdateTrackerRequest
import com.eter.undiamas.core.data.api.UpdateUserRequest
import com.eter.undiamas.core.data.api.apiJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlin.time.Clock

/** Cuerpo de la operación de borrar una entrada del diario, que va por ruta y no por JSON. */
@Serializable
data class BorradoPendiente(val id: String)

/** En qué punto está la sincronización, para poder contarlo en pantalla sin mentir. */
enum class EstadoSync {
    AL_DIA,
    SIN_CONEXION,
    ENVIANDO,
    CON_ERRORES,
}

/**
 * Empuja al servidor lo que se guardó sin conexión.
 *
 * La regla que ordena todo lo demás: **una operación solo sale de la cola cuando el
 * servidor la confirma, o cuando la rechaza de una forma que no se arregla reintentando**.
 * Un fallo de red deja la fila donde está; un 400 por datos inválidos la descarta, porque
 * si no bloquearía para siempre todo lo que venga detrás.
 *
 * Se envía en orden estricto y de una en una. Es más lento que mandarlas en paralelo, y es
 * lo correcto: el servidor calcula la racha a partir de la secuencia de recaídas y
 * check-ins, y desordenarlas le haría reconstruir una historia que no pasó.
 */
class SyncManager(
    private val api: UnDiaMasApi,
    private val local: LocalStore,
    private val outbox: Outbox,
) {
    private val _estado = MutableStateFlow(EstadoSync.AL_DIA)
    val estado: StateFlow<EstadoSync> = _estado.asStateFlow()

    private val _ultimaSincronizacion = MutableStateFlow<Long?>(null)
    val ultimaSincronizacion: StateFlow<Long?> = _ultimaSincronizacion.asStateFlow()

    /**
     * Solo puede haber un envío a la vez.
     *
     * Sin esto habría duplicados: cada escritura pide sincronizar, y además lo piden el
     * aviso de "volvió la red" y el temporizador de reintento. Comprobar un flag no basta
     * —dos corrutinas pasan la comprobación antes de que ninguna lo cambie— y el resultado
     * es la misma fila de la cola enviada dos veces, o sea dos check-ins idénticos en el
     * historial de alguien.
     */
    private val enviando = Mutex()

    /**
     * Vacía la cola.
     *
     * Devuelve true si quedó vacía. En cuanto una operación falla por red se corta: seguir
     * con las siguientes las mandaría en desorden, y además no van a ir mejor.
     *
     * Si ya hay un envío en marcha se devuelve false sin hacer nada: el que está corriendo
     * sigue hasta vaciar la cola, así que también se llevará lo que se acaba de encolar.
     */
    suspend fun sincronizar(): Boolean {
        if (!enviando.tryLock()) return false
        _estado.value = EstadoSync.ENVIANDO
        var todoBien = true

        try {
            while (true) {
                val pendiente = outbox.siguientes(limite = 1).firstOrNull() ?: break
                val resultado = enviar(pendiente)
                if (resultado == Resultado.REINTENTAR) {
                    todoBien = false
                    break
                }
                outbox.completar(pendiente.seq)
            }
        } finally {
            _estado.value = when {
                !todoBien -> EstadoSync.SIN_CONEXION
                outbox.pendientes.value > 0 -> EstadoSync.CON_ERRORES
                else -> EstadoSync.AL_DIA
            }
            if (todoBien) _ultimaSincronizacion.value = Clock.System.now().toEpochMilliseconds()
            enviando.unlock()
        }
        return todoBien
    }

    private enum class Resultado { HECHO, REINTENTAR }

    private suspend fun enviar(pendiente: Pendiente): Resultado = try {
        ejecutar(pendiente)
        Resultado.HECHO
    } catch (error: ApiException) {
        if (esDefinitivo(error)) {
            // No se arregla reintentando. Se anota y se descarta para no bloquear la cola;
            // el dato local se queda, porque es lo que la persona escribió.
            outbox.anotarFallo(pendiente.seq, error.debugMessage)
            Resultado.HECHO
        } else {
            outbox.anotarFallo(pendiente.seq, error.debugMessage)
            Resultado.REINTENTAR
        }
    } catch (error: Throwable) {
        // Casi siempre, sin red. Se queda en la cola tal cual.
        outbox.anotarFallo(pendiente.seq, error.message ?: "sin conexión")
        Resultado.REINTENTAR
    }

    /**
     * Un 401 no es definitivo: se resuelve al renovar el token o al volver a entrar, y la
     * operación debe sobrevivir a eso. Los demás errores de cliente sí lo son.
     */
    private fun esDefinitivo(error: ApiException): Boolean = when (error.code) {
        ApiErrorCode.INVALID_ARGUMENT, ApiErrorCode.FORBIDDEN, ApiErrorCode.NOT_FOUND -> true
        else -> error.status in 400..499 && error.status != 401 && error.status != 429
    }

    private suspend fun ejecutar(pendiente: Pendiente) {
        when (pendiente.tipo) {
            TipoPendiente.CHECK_IN -> {
                val cuerpo = apiJson.decodeFromString<CreateCheckInRequest>(pendiente.carga)
                val creado = api.createCheckIn(cuerpo)
                pendiente.idLocal?.let { local.confirmarCheckIn(it, creado.id) }
            }

            TipoPendiente.DIARIO_CREAR -> {
                val cuerpo = apiJson.decodeFromString<CreateJournalRequest>(pendiente.carga)
                val creado = api.createJournal(cuerpo.content)
                pendiente.idLocal?.let { local.confirmarEntradaDiario(it, creado.id) }
            }

            TipoPendiente.DIARIO_BORRAR -> {
                val cuerpo = apiJson.decodeFromString<BorradoPendiente>(pendiente.carga)
                api.deleteJournal(cuerpo.id)
            }

            TipoPendiente.ANIMO -> {
                val cuerpo = apiJson.decodeFromString<CreateMoodRequest>(pendiente.carga)
                val creado = api.createMood(cuerpo.mood)
                pendiente.idLocal?.let { local.confirmarAnimo(it, creado.id) }
            }

            TipoPendiente.RECAIDA -> {
                val cuerpo = apiJson.decodeFromString<CreateRelapseRequest>(pendiente.carga)
                val creado = api.createRelapse(cuerpo.note, cuerpo.triggers)
                pendiente.idLocal?.let { local.confirmarRecaida(it, creado.id) }
            }

            TipoPendiente.SEMAFORO -> {
                val cuerpo = apiJson.decodeFromString<CreateTrafficLightRequest>(pendiente.carga)
                val resultado = api.saveTrafficLight(cuerpo)
                pendiente.idLocal?.let { local.confirmarSemaforo(it, resultado.entry.id) }
            }

            TipoPendiente.PERFIL_IDENTIDAD -> {
                val cuerpo = apiJson.decodeFromString<UpdateUserRequest>(pendiente.carga)
                api.updateMe(cuerpo.displayName, cuerpo.porQuePersonal)
            }

            TipoPendiente.PERFIL_CONTACTOS -> {
                val cuerpo = apiJson.decodeFromString<EmergencyContactsRequest>(pendiente.carga)
                api.updateEmergencyContacts(cuerpo.contactosEmergencia)
            }

            TipoPendiente.PERFIL_TRACKER -> {
                val cuerpo = apiJson.decodeFromString<UpdateTrackerRequest>(pendiente.carga)
                api.updateTracker(cuerpo.startDate, cuerpo.dailySavingsRate, cuerpo.currency)
            }

            TipoPendiente.RECORDATORIO -> {
                val cuerpo = apiJson.decodeFromString<ReminderDto>(pendiente.carga)
                api.updateReminder(cuerpo)
            }

            TipoPendiente.ALERTA_ATENDIDA -> {
                val cuerpo = apiJson.decodeFromString<BorradoPendiente>(pendiente.carga)
                api.markAlertHandled(cuerpo.id)
            }
        }
    }

    fun marcarSinConexion() {
        if (_estado.value != EstadoSync.ENVIANDO) _estado.value = EstadoSync.SIN_CONEXION
    }

    fun marcarAlDia() {
        if (outbox.pendientes.value == 0) _estado.value = EstadoSync.AL_DIA
    }
}
