package com.eter.undiamas.core.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

// Nombres reales de las colecciones en Firestore.
object Colecciones {
    const val USERS = "users"
    const val SOBRIETY_TRACKERS = "sobriety_trackers"
    const val TRAFFIC_LIGHT_LOGS = "traffic_light_logs"
    const val CHECK_INS = "check_ins"
    const val RELAPSE_EVENTS = "relapse_events"
    const val SESSIONS = "sessions"
    const val CLINICAL_NOTES = "clinical_notes"
    const val AI_LOGS = "ai_logs"
    const val SOBRIETY_LOGS = "sobriety_logs"
    const val JOURNAL_ENTRIES = "journal_entries"
    const val MOOD_LOGS = "mood_logs"
    const val AI_MESSAGES = "ai_messages"
    const val ALERTS = "alerts"
}

private const val CAMPO_UID = "uid"
private const val CAMPO_USER_ID = "userId"

/** Convierte un Timestamp de Firestore al Instant del dominio; null se trata como epoch 0. */
fun Timestamp?.toInstant(): Instant =
    this?.let { Instant.fromEpochSeconds(it.seconds, it.nanoseconds) } ?: Instant.fromEpochSeconds(0)

/** Instante actual como Timestamp de Firestore, para escribir fechas. */
fun nowTimestamp(): Timestamp = Timestamp.now()

/** Camino inverso: del Instant del dominio al Timestamp que espera Firestore. */
fun Instant.toFirestoreTimestamp(): Timestamp =
    Timestamp(seconds = epochSeconds, nanoseconds = nanosecondsOfSecond)

/**
 * Cuentas de `/users`.
 *
 * Busca por el CAMPO `uid`, no por el id del documento, a proposito: en la base actual
 * conviven documentos cuyo id es el uid (`user_demo_02`) con otros de id autogenerado
 * (`Y244hY4WftK5ztrOZhU0`) que llevan el uid dentro. Asumir que el id es el uid perderia
 * esos ultimos en silencio.
 */
class UsersRepository {
    private val firestore = Firebase.firestore

    /** Documento del usuario autenticado, o null si aun no existe. */
    suspend fun currentUser(): Result<UserDoc?> = withUid { uid ->
        firestore.collection(Colecciones.USERS)
            .where { CAMPO_UID equalTo uid }
            .get()
            .documents
            .firstOrNull()
            ?.data<UserDoc>()
    }

    /** true si la cuenta autenticada tiene rol de terapeuta. */
    suspend fun isTherapist(): Boolean =
        currentUser().getOrNull()?.esTerapeuta == true

    /**
     * Crea o actualiza el documento del usuario.
     *
     * No escribe `role` a proposito: el rol decide que datos ajenos puede leer la cuenta,
     * asi que dejarlo en manos del cliente permitiria que cualquiera se ascendiera a
     * terapeuta. Lo asigna un administrador o una Cloud Function.
     */
    suspend fun upsertCurrentUser(displayName: String, email: String): Result<Unit> = withUid { uid ->
        val docs = firestore.collection(Colecciones.USERS)
            .where { CAMPO_UID equalTo uid }
            .get()
            .documents

        val payload = mapOf(
            CAMPO_UID to uid,
            "displayName" to displayName,
            "email" to email,
            "lastLoginAt" to nowTimestamp(),
        )

        val existing = docs.firstOrNull()
        if (existing != null) {
            existing.reference.set(payload, merge = true)
        } else {
            // El id del documento pasa a ser el uid: consultas mas simples de aqui en adelante.
            firestore.collection(Colecciones.USERS).document(uid)
                .set(payload + ("createdAt" to nowTimestamp()) + ("role" to UserRoles.PATIENT))
        }
    }

    /** Pacientes visibles para un terapeuta. Las reglas rechazan la consulta si no lo es. */
    suspend fun listPatients(): Result<List<UserDoc>> = withUid {
        firestore.collection(Colecciones.USERS)
            .where { "role" equalTo UserRoles.PATIENT }
            .get()
            .documents
            .map { it.data<UserDoc>() }
    }
}

/** Contador de sobriedad en `/sobriety_trackers`, un documento por paciente. */
class SobrietyTrackerRepository {
    private val firestore = Firebase.firestore

    fun observe(userId: String): Flow<SobrietyTrackerDoc?> =
        firestore.collection(Colecciones.SOBRIETY_TRACKERS).document(userId).snapshots
            .map { snapshot -> if (snapshot.exists) snapshot.data<SobrietyTrackerDoc>() else null }
            .catch { emit(null) }

    suspend fun save(startDate: Timestamp, dailySavingsRate: Double, currency: String): Result<Unit> =
        withUid { uid ->
            firestore.collection(Colecciones.SOBRIETY_TRACKERS).document(uid).set(
                SobrietyTrackerDoc(
                    userId = uid,
                    startDate = startDate,
                    dailySavingsRate = dailySavingsRate,
                    currency = currency,
                    lastStatusUpdate = nowTimestamp(),
                ),
                merge = true,
            )
        }
}

/** Historial del semaforo en `/traffic_light_logs`, filtrado siempre por userId. */
class TrafficLightLogRepository {
    private val firestore = Firebase.firestore

    fun observe(userId: String): Flow<List<TrafficLightLogDoc>> =
        firestore.collection(Colecciones.TRAFFIC_LIGHT_LOGS)
            .where { CAMPO_USER_ID equalTo userId }
            .snapshots
            .map { snapshot -> snapshot.documents.map { it.data<TrafficLightLogDoc>() } }
            .catch { emit(emptyList()) }

    suspend fun add(status: String, reason: String, triggerLevel: Int, actions: List<String>): Result<Unit> =
        withUid { uid ->
            firestore.collection(Colecciones.TRAFFIC_LIGHT_LOGS).add(
                TrafficLightLogDoc(
                    userId = uid,
                    status = status,
                    reason = reason,
                    triggerLevel = triggerLevel,
                    suggestedActions = actions,
                    timestamp = nowTimestamp(),
                ),
            )
            Unit
        }
}

/** Check-ins en `/check_ins`, coleccion plana filtrada por userId. */
class CheckInFlatRepository {
    private val firestore = Firebase.firestore

    fun observe(userId: String): Flow<List<CheckInFlatDoc>> =
        firestore.collection(Colecciones.CHECK_INS)
            .where { CAMPO_USER_ID equalTo userId }
            .snapshots
            .map { snapshot -> snapshot.documents.map { it.data<CheckInFlatDoc>() } }
            .catch { emit(emptyList()) }

    suspend fun add(doc: CheckInFlatDoc): Result<Unit> = withUid { uid ->
        firestore.collection(Colecciones.CHECK_INS)
            .add(doc.copy(userId = uid, timestamp = doc.timestamp ?: nowTimestamp()))
        Unit
    }
}

/**
 * Trazas de IA en `/ai_logs`. Solo lectura: las escribe la Cloud Function con Admin SDK.
 * El filtro por userId es obligatorio; sin el, la regla rechaza la consulta completa.
 */
class AiLogRepository {
    private val firestore = Firebase.firestore

    suspend fun list(limit: Int = 50): Result<List<AiLogDoc>> = withUid { uid ->
        firestore.collection(Colecciones.AI_LOGS)
            .where { CAMPO_USER_ID equalTo uid }
            .get()
            .documents
            .map { it.data<AiLogDoc>() }
            .take(limit)
    }
}
