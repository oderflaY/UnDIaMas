package com.eter.undiamas.core.data.api

import kotlinx.serialization.Serializable

/*
 * Espejo exacto del JSON del backend, verificado ruta por ruta contra el servidor.
 *
 * Nada de renombrar campos "para que se lean mejor": el backend rechaza con 400 cualquier
 * clave que no conozca, asi que estos nombres son parte del contrato. La mezcla de ingles
 * y español (`riskLevel` junto a `porQuePersonal`) es la del servidor, no un descuido.
 *
 * Todos los campos llevan valor por defecto para que añadir uno en el backend no rompa
 * una version vieja de la app.
 */

// ---- Auth ----------------------------------------------------------------------

@Serializable
data class AuthRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
)

/**
 * Peticion de codigo de recuperacion.
 *
 * El servidor responde igual exista o no la cuenta: contestar distinto convertiria esta
 * ruta en una forma de averiguar quien tiene cuenta en una app de adicciones.
 */
@Serializable
data class ForgotPasswordRequest(
    val email: String,
)

/** Cambio de contraseña con el codigo que llego por correo. */
@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val password: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class AuthUserDto(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "patient",
)

@Serializable
data class AuthResponse(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresIn: Long = 0,
    val user: AuthUserDto = AuthUserDto(),
)

// ---- Perfil --------------------------------------------------------------------

@Serializable
data class ContactDto(
    val nombre: String = "",
    val telefono: String = "",
    val rol: String = "FAMILIAR",
)

@Serializable
data class UserDto(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "patient",
    val porQuePersonal: String = "",
    val recordRachaSegundos: Long = 0,
    val contactosEmergencia: List<ContactDto> = emptyList(),
    val createdAt: String? = null,
)

/** Solo estos dos campos son editables: el resto los calcula o los protege el servidor. */
@Serializable
data class UpdateUserRequest(
    val displayName: String,
    val porQuePersonal: String,
)

@Serializable
data class EmergencyContactsRequest(
    val contactosEmergencia: List<ContactDto>,
)

// ---- Tracker (racha y ahorro) --------------------------------------------------

@Serializable
data class TrackerDto(
    val startDate: String? = null,
    val dailySavingsRate: Double = 0.0,
    val currency: String = "MXN",
    val trafficLightStatus: String = "VERDE",
    val lastStatusUpdate: String? = null,
    val rachaSegundos: Long = 0,
    val recordRachaSegundos: Long = 0,
    val ahorroAcumulado: Double = 0.0,
)

@Serializable
data class UpdateTrackerRequest(
    val startDate: String,
    val dailySavingsRate: Double,
    val currency: String = "MXN",
)

// ---- Check-ins -----------------------------------------------------------------

@Serializable
data class CheckInDto(
    val id: String = "",
    val riskLevel: String = "VERDE",
    val cravingLevel: Int = 0,
    val mood: String = "NEUTRAL",
    val triggers: List<String> = emptyList(),
    val note: String = "",
    val answers: Map<String, String> = emptyMap(),
    val createdAt: String? = null,
)

@Serializable
data class CreateCheckInRequest(
    val riskLevel: String,
    val cravingLevel: Int,
    val mood: String,
    val triggers: List<String>,
    val note: String,
    val answers: Map<String, String>,
)

// ---- Recaidas ------------------------------------------------------------------

@Serializable
data class RelapseDto(
    val id: String = "",
    val note: String = "",
    val triggers: List<String> = emptyList(),
    /** Racha que se perdio. El servidor la calcula y conserva el record historico aparte. */
    val previousStreakSeconds: Long = 0,
    val createdAt: String? = null,
)

@Serializable
data class CreateRelapseRequest(
    val note: String,
    val triggers: List<String>,
)

// ---- Semaforo ------------------------------------------------------------------

@Serializable
data class TrafficLightDto(
    val id: String = "",
    val status: String = "VERDE",
    val reason: String = "",
    val triggerLevel: Int = 0,
    val suggestedActions: List<String> = emptyList(),
    val createdAt: String? = null,
)

@Serializable
data class CreateTrafficLightRequest(
    val status: String,
    val reason: String,
    val triggerLevel: Int,
    val suggestedActions: List<String>,
)

/** Un semaforo en rojo devuelve [alert] no nula: es el disparo del protocolo de emergencia. */
@Serializable
data class TrafficLightResult(
    val alert: AlertDto? = null,
    val entry: TrafficLightDto = TrafficLightDto(),
)

@Serializable
data class TrafficLightStateDto(
    val current: String = "VERDE",
    val items: List<TrafficLightDto> = emptyList(),
    val since: String? = null,
)

// ---- Diario y animo ------------------------------------------------------------

@Serializable
data class JournalDto(
    val id: String = "",
    val content: String = "",
    val createdAt: String? = null,
)

@Serializable
data class CreateJournalRequest(val content: String)

@Serializable
data class MoodDto(
    val id: String = "",
    val mood: String = "NEUTRAL",
    val createdAt: String? = null,
)

@Serializable
data class CreateMoodRequest(val mood: String)

// ---- Alertas -------------------------------------------------------------------

@Serializable
data class AlertDto(
    val id: String = "",
    val riskLevel: String = "VERDE",
    val message: String = "",
    val handled: Boolean = false,
    val createdAt: String? = null,
)

@Serializable
data class HandleAlertRequest(val handled: Boolean)

// ---- Recordatorio diario -------------------------------------------------------

@Serializable
data class ReminderDto(
    val enabled: Boolean = false,
    val hora: Int = 21,
    val minuto: Int = 0,
    val zona: String = "America/Mexico_City",
    val ultimoEnvio: String? = null,
)

// ---- Estadisticas --------------------------------------------------------------

@Serializable
data class CountedDto(
    val valor: String = "",
    val total: Int = 0,
)

@Serializable
data class DailyPointDto(
    val fecha: String = "",
    val total: Int = 0,
    val verdes: Int = 0,
    val amarillos: Int = 0,
    val rojos: Int = 0,
)

@Serializable
data class RiskTrendsDto(
    val dias: Int = 30,
    val desde: String? = null,
    val hasta: String? = null,
    val zona: String = "",
    val totalCheckIns: Int = 0,
    val verdes: Int = 0,
    val amarillos: Int = 0,
    val rojos: Int = 0,
    val promedioCraving: Double = 0.0,
    val detonantesFrecuentes: List<CountedDto> = emptyList(),
    val animosFrecuentes: List<CountedDto> = emptyList(),
    val recaidas: Int = 0,
    val alertas: Int = 0,
    val serieDiaria: List<DailyPointDto> = emptyList(),
    val tendencia: String = "sin-datos",
)

// ---- IA ------------------------------------------------------------------------




// ---- Terapeuta -----------------------------------------------------------------

@Serializable
data class TherapistDto(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
)

@Serializable
data class SessionDto(
    val id: String = "",
    val therapistId: String = "",
    val status: String = "scheduled",
    val scheduledAt: String? = null,
    val notes: String = "",
)

// ---- Envoltorio de listas ------------------------------------------------------

/** Todas las rutas de listado responden `{"items":[...]}`, nunca un array pelado. */
@Serializable
data class ItemsDto<T>(val items: List<T> = emptyList())
