package com.eter.undiamas.core.data.firebase

import kotlinx.serialization.Serializable

/** Forma real de `/usuarios/{uid}/contactosEmergencia[]` en Firestore. */
@Serializable
data class ContactoEmergenciaDoc(
    val nombre: String = "",
    val telefono: String = "",
    val rol: String = "FAMILIAR",
)

/**
 * Forma real del documento `/usuarios/{uid}` en Firestore.
 *
 * Todos los campos llevan valor por defecto a proposito: si el documento se creo a mano
 * en la consola o viene de una version anterior, la deserializacion no revienta por un
 * campo ausente, simplemente cae al default.
 *
 * Las fechas se guardan como epoch en segundos (Long) y no como Timestamp de Firestore.
 * Es deliberado: el dominio usa kotlin.time.Instant, un Long viaja igual en Android e iOS
 * sin serializador propio, y evita que un Timestamp nulo tumbe la lectura. La conversion
 * vive en FirestoreMappers.
 */
@Serializable
data class UsuarioDoc(
    val alias: String = "",
    val fechaInicioSobriedad: Long = 0,
    val recordRachaSegundos: Long = 0,
    val gastoDiarioEstimado: Double = 0.0,
    val contactosEmergencia: List<ContactoEmergenciaDoc> = emptyList(),
    val porQuePersonal: String = "",
    val fcmToken: String? = null,

    // --- Campos de identidad de la cuenta ---
    /** Nombre visible en la app. Es el que la UI muestra. */
    val displayName: String = "",
    /** Nombre completo, si la persona lo aporta. Puede diferir de [displayName]. */
    val fullName: String = "",
    /** Copia del correo de Auth, para poder listarlo sin consultar Auth. */
    val email: String = "",
    /** Copia del uid. Redundante con el id del documento, pero util en consultas. */
    val uid: String = "",
    /** Ultimo acceso, epoch en segundos. 0 si nunca se ha registrado. */
    val lastLogin: Long = 0,
)

/**
 * Forma real de cada documento en `/usuarios/{uid}/alertas`.
 *
 * Solo la Cloud Function las escribe (Admin SDK, que no pasa por las reglas). El cliente
 * las lee, nunca las crea: por eso no hay mapper de escritura.
 */
@Serializable
data class AlertaDoc(
    val tipo: String = "",
    val mensaje: String = "",
    val nivelRiesgo: String = "VERDE",
    val fecha: Long = 0,
    val atendida: Boolean = false,
)

/**
 * Forma real de cada documento en `/logs_ia`, la traza check-in -> riesgo -> respuesta.
 *
 * [userId] no es decorativo: es el campo exacto que la regla compara contra
 * `request.auth.uid` para decidir si puedes leer el documento. Sin el, la lectura falla.
 */
@Serializable
data class LogIaDoc(
    val userId: String = "",
    val checkInId: String = "",
    val nivelRiesgo: String = "VERDE",
    val prompt: String = "",
    val respuesta: String = "",
    val modelo: String = "",
    val fecha: Long = 0,
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
