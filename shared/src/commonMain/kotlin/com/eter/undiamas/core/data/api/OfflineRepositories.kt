package com.eter.undiamas.core.data.api

import com.eter.undiamas.core.data.local.BorradoPendiente
import com.eter.undiamas.core.data.local.LocalStore
import com.eter.undiamas.core.data.local.Outbox
import com.eter.undiamas.core.data.local.TipoPendiente
import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.Mood
import com.eter.undiamas.core.domain.model.MoodEntry
import com.eter.undiamas.core.domain.model.RelapseEvent
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.domain.model.UserProfile
import com.eter.undiamas.core.domain.repository.AiMessageRepository
import com.eter.undiamas.core.domain.repository.Alert
import com.eter.undiamas.core.domain.repository.AlertRepository
import com.eter.undiamas.core.domain.repository.CheckInRepository
import com.eter.undiamas.core.domain.repository.DiaryRepository
import com.eter.undiamas.core.domain.repository.MoodRepository
import com.eter.undiamas.core.domain.repository.PerfilRepository
import com.eter.undiamas.core.domain.repository.RelapseRepository
import com.eter.undiamas.core.domain.repository.Reminder
import com.eter.undiamas.core.domain.repository.ReminderRepository
import com.eter.undiamas.core.domain.repository.TrafficLightEntry
import com.eter.undiamas.core.domain.repository.TrafficLightRepository
import com.eter.undiamas.features.diario.domain.DiaryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.Instant

/*
 * Repositorios offline-first.
 *
 * El patrón es siempre el mismo y conviene entenderlo una vez:
 *
 *   escribir → guardar en SQLite (marcado como pendiente) → encolar → intentar enviar
 *   leer     → SQLite, siempre; la red solo actualiza esa copia
 *
 * Lo importante es el orden. Se guarda **antes** de intentar la red, así que si el envío
 * falla —o si la app se cierra a mitad— el dato ya está en el teléfono. La alternativa
 * habitual (intentar la red y guardar en local solo si falla) pierde datos justo en el
 * momento en que peor sienta perderlos.
 *
 * De ahí sale otra consecuencia: la escritura nunca lanza por falta de red. Para quien usa
 * la app, guardar un check-in en el metro y guardarlo con wifi son la misma acción.
 */

/** Id provisional de una fila que todavía no existe en el servidor. */
private fun idLocal(prefijo: String): String =
    "local-$prefijo-${Clock.System.now().toEpochMilliseconds()}"

class OfflineCheckInRepository(
    private val api: UnDiaMasApi,
    private val local: LocalStore,
    private val outbox: Outbox,
    private val alSincronizar: suspend () -> Unit,
) : CheckInRepository {

    override val items: StateFlow<List<CheckInEntry>> = local.checkIns

    override suspend fun refresh() {
        val remotos = api.checkIns().map { dto ->
            dto.toDomain("") to dto.mood.toMood()
        }
        local.reemplazarCheckIns(remotos)
    }

    override suspend fun add(entry: CheckInEntry, mood: Mood): CheckInEntry {
        val id = entry.id.ifBlank { idLocal("checkin") }
        val guardado = entry.copy(id = id)
        local.guardarCheckIn(guardado, mood, pendiente = true)
        outbox.encolar(
            tipo = TipoPendiente.CHECK_IN,
            carga = apiJson.encodeToString(guardado.toRequest(mood)),
            idLocal = id,
        )
        alSincronizar()
        return guardado
    }
}

class OfflineDiaryRepository(
    private val api: UnDiaMasApi,
    private val local: LocalStore,
    private val outbox: Outbox,
    private val alSincronizar: suspend () -> Unit,
) : DiaryRepository {

    override val items: StateFlow<List<DiaryEntry>> = local.diario

    override suspend fun refresh() {
        local.reemplazarDiario(api.journal().map { it.toDomain("") })
    }

    override suspend fun add(text: String): DiaryEntry {
        val entrada = DiaryEntry(
            id = idLocal("diario"),
            userId = "",
            createdAt = Clock.System.now(),
            text = text,
        )
        local.guardarEntradaDiario(entrada, pendiente = true)
        outbox.encolar(
            tipo = TipoPendiente.DIARIO_CREAR,
            carga = apiJson.encodeToString(CreateJournalRequest(text)),
            idLocal = entrada.id,
        )
        alSincronizar()
        return entrada
    }

    /**
     * Borra la entrada.
     *
     * Si todavía no había salido del teléfono, se quita también de la cola: mandar al
     * servidor un "crea esto" seguido de un "bórralo" es trabajo inútil, y deja rastro de
     * algo que la persona decidió que no existiera.
     */
    override suspend fun delete(id: String) {
        val eraLocal = outbox.descartarPorIdLocal(id)
        local.borrarEntradaDiario(id)
        if (eraLocal) return
        outbox.encolar(
            tipo = TipoPendiente.DIARIO_BORRAR,
            carga = apiJson.encodeToString(BorradoPendiente(id)),
        )
        alSincronizar()
    }
}

class OfflineMoodRepository(
    private val api: UnDiaMasApi,
    private val local: LocalStore,
    private val outbox: Outbox,
    private val alSincronizar: suspend () -> Unit,
) : MoodRepository {

    override val items: StateFlow<List<MoodEntry>> = local.animos

    override suspend fun refresh() {
        local.reemplazarAnimos(api.moodLogs().map { it.toDomain("") })
    }

    override suspend fun add(mood: Mood): MoodEntry {
        val entrada = MoodEntry(
            id = idLocal("animo"),
            userId = "",
            mood = mood,
            registeredAt = Clock.System.now(),
        )
        local.guardarAnimo(entrada, pendiente = true)
        outbox.encolar(
            tipo = TipoPendiente.ANIMO,
            carga = apiJson.encodeToString(CreateMoodRequest(mood.name)),
            idLocal = entrada.id,
        )
        alSincronizar()
        return entrada
    }
}

class OfflineRelapseRepository(
    private val api: UnDiaMasApi,
    private val local: LocalStore,
    private val outbox: Outbox,
    private val alSincronizar: suspend () -> Unit,
) : RelapseRepository {

    override val items: StateFlow<List<RelapseEvent>> = local.recaidas

    override suspend fun refresh() {
        local.reemplazarRecaidas(api.relapses().map { it.toDomain("") })
    }

    override suspend fun register(note: String, triggers: List<String>): RelapseEvent {
        val evento = RelapseEvent(
            id = idLocal("recaida"),
            userId = "",
            occurredAt = Clock.System.now(),
            notes = note.takeIf { it.isNotBlank() },
        )
        local.guardarRecaida(evento, triggers, pendiente = true)
        outbox.encolar(
            tipo = TipoPendiente.RECAIDA,
            carga = apiJson.encodeToString(CreateRelapseRequest(note, triggers)),
            idLocal = evento.id,
        )
        alSincronizar()
        return evento
    }
}

class OfflineTrafficLightRepository(
    private val api: UnDiaMasApi,
    private val local: LocalStore,
    private val outbox: Outbox,
    private val alSincronizar: suspend () -> Unit,
) : TrafficLightRepository {

    override val items: StateFlow<List<TrafficLightEntry>> = local.semaforo

    private val _current = MutableStateFlow(RiskLevel.VERDE)
    override val current: StateFlow<RiskLevel> = _current.asStateFlow()

    override suspend fun refresh() {
        val estado = api.trafficLight()
        _current.value = estado.current.toRiskLevel()
        local.reemplazarSemaforo(
            estado.items.map { dto ->
                TrafficLightEntry(
                    id = dto.id,
                    status = dto.status.toRiskLevel(),
                    reason = dto.reason,
                    triggerLevel = dto.triggerLevel,
                    suggestedActions = dto.suggestedActions,
                ) to (dto.createdAt.toInstantOrNow().toEpochMilliseconds())
            },
        )
    }

    /**
     * Guarda la evaluación.
     *
     * Sin conexión devuelve null aunque el nivel sea ROJO: la alerta la crea el servidor, y
     * decir que se disparó el protocolo cuando nadie lo ha recibido sería la peor mentira
     * que esta app puede contar. Quien esté en rojo sin red necesita ver el botón de
     * emergencia y su contacto de confianza, que funcionan sin servidor.
     */
    override suspend fun save(
        status: RiskLevel,
        reason: String,
        triggerLevel: Int,
        suggestedActions: List<String>,
    ): Alert? {
        val id = idLocal("semaforo")
        _current.value = status
        local.guardarSemaforo(
            TrafficLightEntry(id, status, reason, triggerLevel, suggestedActions),
            creadoEn = Clock.System.now().toEpochMilliseconds(),
            pendiente = true,
        )
        outbox.encolar(
            tipo = TipoPendiente.SEMAFORO,
            carga = apiJson.encodeToString(
                CreateTrafficLightRequest(status.name, reason, triggerLevel, suggestedActions),
            ),
            idLocal = id,
        )
        alSincronizar()
        return null
    }
}

class OfflineAlertRepository(
    private val api: UnDiaMasApi,
    private val local: LocalStore,
    private val outbox: Outbox,
) : AlertRepository {

    override val items: StateFlow<List<Alert>> = local.alertas

    override suspend fun refresh() {
        local.reemplazarAlertas(
            api.alerts().map { dto ->
                dto.toDomain() to dto.createdAt.toInstantOrNow().toEpochMilliseconds()
            },
        )
    }

    override suspend fun markHandled(id: String) {
        local.marcarAlertaAtendida(id)
        outbox.encolar(
            tipo = TipoPendiente.ALERTA_ATENDIDA,
            carga = apiJson.encodeToString(BorradoPendiente(id)),
        )
    }

    /** Alerta recién llegada por el canal de eventos. */
    suspend fun onPushed(alerta: Alert) {
        local.guardarAlerta(alerta, Clock.System.now().toEpochMilliseconds())
    }
}

class OfflinePerfilRepository(
    private val api: UnDiaMasApi,
    private val local: LocalStore,
    private val outbox: Outbox,
    private val alSincronizar: suspend () -> Unit,
) : PerfilRepository {

    override val profile: StateFlow<UserProfile?> = local.perfil
    override val streakSeconds: StateFlow<Long> = local.rachaSegundos
    override val savedAmount: StateFlow<Double> = local.ahorro

    override suspend fun refresh() {
        val usuario = api.me()
        val tracker = api.tracker()
        local.guardarPerfil(
            buildProfile(usuario, tracker, previous = local.perfil.value),
            rachaSegundos = tracker.rachaSegundos,
            ahorro = tracker.ahorroAcumulado,
        )
    }

    /**
     * Guarda nombre y "mi por qué".
     *
     * Si todavía no hay perfil local se crea uno: es lo que pasa cuando alguien se registra
     * y completa el cuestionario inicial sin conexión. Antes se descartaba en silencio, y la
     * persona veía cómo su nombre desaparecía sin que nada le explicara por qué.
     */
    override suspend fun saveIdentity(displayName: String, personalWhy: String) {
        val actual = local.perfil.value ?: UserProfile(
            userId = "",
            displayName = displayName,
            sobrietyStartDate = Clock.System.now(),
        )
        guardarLocal(actual.copy(displayName = displayName, personalWhy = personalWhy))
        outbox.encolar(
            tipo = TipoPendiente.PERFIL_IDENTIDAD,
            carga = apiJson.encodeToString(UpdateUserRequest(displayName, personalWhy)),
        )
        alSincronizar()
    }

    override suspend fun saveContacts(profile: UserProfile) {
        guardarLocal(profile)
        outbox.encolar(
            tipo = TipoPendiente.PERFIL_CONTACTOS,
            carga = apiJson.encodeToString(EmergencyContactsRequest(profile.toContactDtos())),
        )
        alSincronizar()
    }

    /**
     * Guarda la fecha de inicio y el gasto diario.
     *
     * La racha se recalcula en local con el reloj del teléfono mientras no haya servidor.
     * Es una aproximación: en cuanto vuelve la red, el número que manda es el del backend.
     */
    override suspend fun saveTracker(profile: UserProfile) {
        val ahora = Clock.System.now().epochSeconds
        val racha = (ahora - profile.sobrietyStartDate.epochSeconds).coerceAtLeast(0)
        local.guardarPerfil(
            profile,
            rachaSegundos = racha,
            ahorro = profile.previousDailyExpense * racha / 86_400.0,
        )
        outbox.encolar(
            tipo = TipoPendiente.PERFIL_TRACKER,
            carga = apiJson.encodeToString(
                UpdateTrackerRequest(
                    startDate = profile.sobrietyStartDate.toApiString(),
                    dailySavingsRate = profile.previousDailyExpense,
                ),
            ),
        )
        alSincronizar()
    }

    /** Crea el perfil local la primera vez, para que el onboarding sin red no se pierda. */
    suspend fun sembrar(profile: UserProfile) = guardarLocal(profile)

    private suspend fun guardarLocal(profile: UserProfile) {
        local.guardarPerfil(profile, local.rachaSegundos.value, local.ahorro.value)
    }
}

class OfflineReminderRepository(
    private val api: UnDiaMasApi,
    private val outbox: Outbox,
    private val alSincronizar: suspend () -> Unit,
) : ReminderRepository {

    private val _reminder = MutableStateFlow<Reminder?>(null)
    override val reminder: StateFlow<Reminder?> = _reminder.asStateFlow()

    override suspend fun refresh() {
        val dto = api.reminder()
        _reminder.value = Reminder(dto.enabled, dto.hora, dto.minuto, dto.zona)
    }

    override suspend fun save(reminder: Reminder) {
        _reminder.value = reminder
        outbox.encolar(
            tipo = TipoPendiente.RECORDATORIO,
            carga = apiJson.encodeToString(
                ReminderDto(reminder.enabled, reminder.hour, reminder.minute, reminder.timeZone),
            ),
        )
        alSincronizar()
    }
}

/**
 * Chat con el asistente. **No pasa por la cola**, a propósito.
 *
 * Encolar un mensaje al asistente significaría que alguien escribe "tengo muchas ganas de
 * tomar" en el metro, no pasa nada, y tres horas después le llega una respuesta a algo que
 * ya se resolvió como pudo. Es mejor decir de frente que hace falta conexión y señalar el
 * botón de emergencia, que sí funciona sin red.
 */
class OfflineAiMessageRepository(
    private val api: UnDiaMasApi,
) : AiMessageRepository {

    private val _items = MutableStateFlow<List<AiMessage>>(emptyList())
    override val items: StateFlow<List<AiMessage>> = _items.asStateFlow()

    private val _isAvailable = MutableStateFlow(true)
    override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    override suspend fun refresh() {
        try {
            _items.value = api.aiMessages().map { it.toDomain("") }.sortedBy { it.sentAt }
            _isAvailable.value = true
        } catch (error: ApiException) {
            if (error.status == 404) {
                _isAvailable.value = false
                _items.value = emptyList()
            } else {
                throw error
            }
        }
    }

    override fun appendLocal(message: AiMessage) {
        _items.value = _items.value + message
    }
}
