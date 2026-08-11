package com.eter.undiamas.core.domain.repository

import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.Mood
import com.eter.undiamas.core.domain.model.MoodEntry
import com.eter.undiamas.core.domain.model.RelapseEvent
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.domain.model.UserProfile
import com.eter.undiamas.features.diario.domain.DiaryEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * Perfil de la persona.
 *
 * Se arma con dos rutas del backend (`/v1/users/me` y `/v1/tracker`), pero el resto de la
 * app no tiene por que saberlo: aqui sale un solo [UserProfile].
 */
interface PerfilRepository {
    val profile: StateFlow<UserProfile?>

    /** Racha y ahorro calculados por el servidor, que es quien tiene la fecha de inicio. */
    val streakSeconds: StateFlow<Long>
    val savedAmount: StateFlow<Double>

    suspend fun refresh()

    /** Nombre y "mi por qué": lo unico que el backend deja editar del perfil. */
    suspend fun saveIdentity(displayName: String, personalWhy: String)

    /** El primero de la lista es el contacto de confianza del protocolo de emergencia. */
    suspend fun saveContacts(profile: UserProfile)

    /** Fecha de inicio de la racha y gasto diario previo. */
    suspend fun saveTracker(profile: UserProfile)
}

interface CheckInRepository : RemoteList<CheckInEntry> {
    /**
     * [mood] va aparte porque el cuestionario no lo pregunta; el animo tiene su propia
     * pantalla. Un check-in en ROJO hace que el servidor cree la alerta y mueva el semaforo.
     */
    suspend fun add(entry: CheckInEntry, mood: Mood = Mood.NEUTRAL): CheckInEntry
}

interface DiaryRepository : RemoteList<DiaryEntry> {
    suspend fun add(text: String): DiaryEntry

    suspend fun delete(id: String)
}

interface MoodRepository : RemoteList<MoodEntry> {
    suspend fun add(mood: Mood): MoodEntry
}

/**
 * Chat con el asistente.
 *
 * El historial lo guarda el servidor, no la app: en `/v1/ai/chat` solo viaja el texto. Por
 * eso aqui no hay forma de escribir la conversacion, solo de leerla y de pintar en local lo
 * que acaba de pasar mientras llega la confirmacion.
 */
interface AiMessageRepository : RemoteList<AiMessage> {
    /** false cuando el servidor arranco sin clave de IA: las rutas de chat no existen. */
    val isAvailable: StateFlow<Boolean>

    /** Muestra ya un mensaje en la conversacion; el servidor es quien lo persiste. */
    fun appendLocal(message: AiMessage)
}

interface RelapseRepository : RemoteList<RelapseEvent> {
    /**
     * Registra la recaida. El servidor reinicia la racha y conserva el record historico:
     * ese calculo no se hace en el telefono para que no dependa de su reloj.
     */
    suspend fun register(note: String, triggers: List<String>): RelapseEvent
}

/** Una evaluacion del semaforo guardada en el servidor. */
data class TrafficLightEntry(
    val id: String,
    val status: RiskLevel,
    val reason: String,
    val triggerLevel: Int,
    val suggestedActions: List<String>,
)

/** Alerta del protocolo de emergencia, creada por el servidor. */
data class Alert(
    val id: String,
    val riskLevel: RiskLevel,
    val message: String,
    val handled: Boolean,
)

interface TrafficLightRepository : RemoteList<TrafficLightEntry> {
    val current: StateFlow<RiskLevel>

    /** Devuelve la alerta si el servidor disparo el protocolo (solo ocurre en ROJO). */
    suspend fun save(
        status: RiskLevel,
        reason: String,
        triggerLevel: Int,
        suggestedActions: List<String>,
    ): Alert?
}

interface AlertRepository : RemoteList<Alert> {
    suspend fun markHandled(id: String)
}

/** Recordatorio diario de check-in, que dispara el servidor a la hora elegida. */
data class Reminder(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val timeZone: String,
)

interface ReminderRepository {
    val reminder: StateFlow<Reminder?>

    suspend fun refresh()

    suspend fun save(reminder: Reminder)
}

/** Tendencias que calcula el servidor sobre todo el historial, no solo lo que cargo la app. */
data class RiskTrends(
    val days: Int,
    val totalCheckIns: Int,
    val green: Int,
    val yellow: Int,
    val red: Int,
    val averageCraving: Double,
    val relapses: Int,
    val alerts: Int,
    val topTriggers: List<Pair<String, Int>>,
    val trend: String,
)

interface StatsRepository {
    val trends: StateFlow<RiskTrends?>

    suspend fun refresh(days: Int = 30)
}
