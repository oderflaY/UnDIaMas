package com.eter.undiamas.core.data.firebase

import kotlinx.serialization.Serializable

/** Forma real de `/usuarios/{uid}/contactosEmergencia[]` en Firestore. */
@Serializable
data class ContactoEmergenciaDoc(
    val nombre: String = "",
    val telefono: String = "",
    val rol: String = "FAMILIAR",
)

/** Forma real del documento `/usuarios/{uid}` en Firestore. */
@Serializable
data class UsuarioDoc(
    val alias: String = "",
    val fechaInicioSobriedad: Long = 0,
    val recordRachaSegundos: Long = 0,
    val gastoDiarioEstimado: Double = 0.0,
    val contactosEmergencia: List<ContactoEmergenciaDoc> = emptyList(),
    val porQuePersonal: String = "",
    val fcmToken: String? = null,
)

/** Forma real de cada documento en la subcoleccion `/usuarios/{uid}/checkins`. */
@Serializable
data class CheckInDoc(
    val nivelCraving: Int = 0,
    val estadoAnimo: String = "",
    val gatillos: List<String> = emptyList(),
    val nivelRiesgo: String = "VERDE",
    val fechaHora: Long = 0,
    val nota: String = "",
    /** Respuestas crudas del cuestionario adaptativo (Fase 2 · 05), para no perder fidelidad. */
    val respuestas: Map<String, String> = emptyMap(),
)

/** Forma real de cada documento en la subcoleccion `/usuarios/{uid}/diario`. */
@Serializable
data class DiaryEntryDoc(
    val contenido: String = "",
    val fecha: Long = 0,
)

/** Forma real de cada documento en la subcoleccion `/usuarios/{uid}/animos`. */
@Serializable
data class MoodEntryDoc(
    val animo: String = "NEUTRAL",
    val fecha: Long = 0,
)

/** Forma real de cada documento en la subcoleccion `/usuarios/{uid}/mensajesIA`. */
@Serializable
data class AiMessageDoc(
    val rol: String = "ASISTENTE",
    val contenido: String = "",
    val nivelRiesgoContexto: String? = null,
    val fecha: Long = 0,
)
