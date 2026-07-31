package com.eter.undiamas.core.data.firebase

import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/**
 * Modelos que calcan la estructura REAL de Firestore (colecciones planas en la raiz),
 * no la de subcolecciones que usaba la version anterior.
 *
 * Dos decisiones que se repiten en todos:
 *
 * 1. Todo campo tiene valor por defecto. Los documentos existentes se crearon en momentos
 *    distintos y no todos traen los mismos campos (unos tienen `lastLogin`, otros
 *    `lastLoginAt`); sin defaults, un campo ausente revienta la deserializacion entera.
 *
 * 2. Las fechas son [Timestamp] de Firestore, no Long, porque asi estan guardadas ya en
 *    la base. Son nullable: un documento a medio crear puede no tenerlas todavia.
 */

/** Contacto de la red de apoyo, tal como se guarda dentro de `/users/{uid}`. */
@Serializable
data class ContactoEmergenciaDoc(
    val nombre: String = "",
    val telefono: String = "",
    val rol: String = "FAMILIAR",
)

/** Rol de la cuenta. Determina que puede leer, asi que nunca lo escribe el cliente. */
object UserRoles {
    const val PATIENT = "patient"
    const val THERAPIST = "therapist"
}

/** Documento de `/users/{docId}`. */
@Serializable
data class UserDoc(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = UserRoles.PATIENT,
    val authProvider: String = "",
    val status: String = "active",
    val isEmailVerified: Boolean = false,
    /** Los documentos antiguos usan `lastLogin`; los nuevos, `lastLoginAt`. Se leen ambos. */
    val lastLogin: Timestamp? = null,
    val lastLoginAt: Timestamp? = null,
    val createdAt: Timestamp? = null,

    // --- Datos de recuperacion que viven en el mismo documento de usuario ---
    /**
     * Red de apoyo. El primero es el contacto de confianza que sugiere el protocolo de
     * emergencia: la Cloud Function `onCheckInCreated` lee justamente este campo, asi
     * que perderlo dejaria el semaforo rojo sin a quien llamar.
     */
    val contactosEmergencia: List<ContactoEmergenciaDoc> = emptyList(),
    /** "Mi por que": el motivo personal que la persona escribio. */
    val porQuePersonal: String = "",
    /** Racha mas larga alcanzada, en segundos. Se conserva aunque haya recaida. */
    val recordRachaSegundos: Long = 0,
    /** Token de FCM del dispositivo, para la notificacion de emergencia. */
    val fcmToken: String? = null,
) {
    /** Ultimo acceso, venga del campo que venga. */
    val ultimoAcceso: Timestamp? get() = lastLoginAt ?: lastLogin

    val esTerapeuta: Boolean get() = role == UserRoles.THERAPIST
}

/** Documento de `/sobriety_trackers/{userId}`: el contador de sobriedad del paciente. */
@Serializable
data class SobrietyTrackerDoc(
    val userId: String = "",
    val startDate: Timestamp? = null,
    val dailySavingsRate: Double = 0.0,
    val currency: String = "MXN",
    /** GREEN | YELLOW | RED, el semaforo vigente. */
    val trafficLightStatus: String = "GREEN",
    val lastStatusUpdate: Timestamp? = null,
)

/** Documento de `/traffic_light_logs/{id}`: cada cambio del semaforo con su porque. */
@Serializable
data class TrafficLightLogDoc(
    val userId: String = "",
    val status: String = "GREEN",
    val reason: String = "",
    /** 1..5, que tan fuerte fue el detonante. */
    val triggerLevel: Int = 0,
    val suggestedActions: List<String> = emptyList(),
    val timestamp: Timestamp? = null,
)

/** Documento de `/check_ins/{id}`. */
@Serializable
data class CheckInFlatDoc(
    val userId: String = "",
    val riskLevel: String = "GREEN",
    val cravingLevel: Int = 0,
    val mood: String = "",
    val triggers: List<String> = emptyList(),
    val note: String = "",
    val answers: Map<String, String> = emptyMap(),
    val timestamp: Timestamp? = null,
)

/** Documento de `/relapse_events/{id}`: dato clinico sensible, se guarda sin juicio. */
@Serializable
data class RelapseEventFlatDoc(
    val userId: String = "",
    val note: String = "",
    val triggers: List<String> = emptyList(),
    /** Racha en segundos que se perdio, para conservar el record historico. */
    val previousStreakSeconds: Long = 0,
    val timestamp: Timestamp? = null,
)

/** Documento de `/sessions/{id}`: sesion entre terapeuta y paciente. */
@Serializable
data class SessionDoc(
    val patientId: String = "",
    val therapistId: String = "",
    val status: String = "scheduled",
    val scheduledAt: Timestamp? = null,
    val notes: String = "",
)

/** Documento de `/clinical_notes/{id}`: nota clinica escrita por el terapeuta. */
@Serializable
data class ClinicalNoteDoc(
    val patientId: String = "",
    val therapistId: String = "",
    val content: String = "",
    val createdAt: Timestamp? = null,
)

/**
 * Documento de `/ai_logs/{id}`: traza check-in -> riesgo -> respuesta de la IA.
 * Calca exactamente lo que escribe `onAiMessageCreated` en Cloud Functions.
 */
@Serializable
data class AiLogDoc(
    val userId: String = "",
    val checkInId: String = "",
    val aiMessageId: String = "",
    val riskLevel: String = "GREEN",
    val createdAt: Timestamp? = null,
)

/** Documento de `/sobriety_logs/{id}`: instantanea diaria de la racha. */
@Serializable
data class SobrietyLogDoc(
    val userId: String = "",
    val streakSeconds: Long = 0,
    val savedAmount: Double = 0.0,
    val timestamp: Timestamp? = null,
)

/**
 * Documento de `/journal_entries/{id}`. Esta coleccion no venia en el volcado inicial
 * porque nadie habia escrito en el diario todavia, pero sigue el mismo patron plano:
 * `userId` dentro del documento y fecha como Timestamp.
 */
@Serializable
data class JournalEntryDoc(
    val userId: String = "",
    val content: String = "",
    val createdAt: Timestamp? = null,
)

/** Documento de `/mood_logs/{id}`: como se sintio la persona en un momento dado. */
@Serializable
data class MoodLogDoc(
    val userId: String = "",
    val mood: String = "NEUTRAL",
    val timestamp: Timestamp? = null,
)

/**
 * Documento de `/ai_messages/{id}`: cada turno de la conversacion con el asistente.
 * Es el dato mas intimo de la app; las reglas no lo abren ni al terapeuta.
 */
@Serializable
data class AiMessageFlatDoc(
    val userId: String = "",
    /** USUARIO | ASISTENTE. */
    val role: String = "ASISTENTE",
    val content: String = "",
    /** Semaforo vigente cuando se envio el mensaje, para la trazabilidad de `ai_logs`. */
    val riskLevelContext: String? = null,
    val timestamp: Timestamp? = null,
)

/**
 * Documento de `/alerts/{id}`: alerta generada por el agente de IA en Cloud Functions.
 * Solo de lectura para la app; la escribe el Admin SDK.
 */
@Serializable
data class AlertDoc(
    val userId: String = "",
    val riskLevel: String = "VERDE",
    val message: String = "",
    val handled: Boolean = false,
    val timestamp: Timestamp? = null,
)
