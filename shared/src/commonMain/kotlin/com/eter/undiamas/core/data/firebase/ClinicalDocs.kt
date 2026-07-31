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

/** Documento de `/ai_logs/{id}`: traza check-in -> riesgo -> respuesta de la IA. */
@Serializable
data class AiLogDoc(
    val userId: String = "",
    val checkInId: String = "",
    val riskLevel: String = "GREEN",
    val prompt: String = "",
    val response: String = "",
    val model: String = "",
    val timestamp: Timestamp? = null,
)

/** Documento de `/sobriety_logs/{id}`: instantanea diaria de la racha. */
@Serializable
data class SobrietyLogDoc(
    val userId: String = "",
    val streakSeconds: Long = 0,
    val savedAmount: Double = 0.0,
    val timestamp: Timestamp? = null,
)
