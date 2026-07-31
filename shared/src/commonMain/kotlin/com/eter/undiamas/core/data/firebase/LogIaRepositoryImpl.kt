package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.model.RiskLevel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlin.time.Instant

/** Entrada de la traza check-in -> nivel de riesgo -> respuesta de la IA. */
data class LogIa(
    val id: String,
    val userId: String,
    val checkInId: String,
    val aiMessageId: String,
    val nivelRiesgo: RiskLevel,
    val creadoEn: Instant,
)

/**
 * Lectura de `/ai_logs`, la coleccion raiz de trazabilidad de la IA.
 *
 * Dos cosas la hacen distinta del resto de repositorios:
 *
 * 1. El aislamiento no sale de la ruta sino del campo `userId`. La regla compara ese
 *    campo contra `request.auth.uid`, por lo que la consulta DEBE filtrar por el: sin el
 *    filtro, Firestore intenta leer documentos ajenos y rechaza la consulta entera con
 *    PERMISSION_DENIED, aunque el usuario tenga logs propios.
 *
 * 2. Solo lectura: las escribe la Cloud Function via Admin SDK (`allow write: if false`).
 */
class LogIaRepositoryImpl {
    private val firestore = Firebase.firestore

    /** Logs del usuario actual, del mas reciente al mas antiguo. */
    suspend fun list(limit: Int = 50): Result<List<LogIa>> = withUid { uid ->
        firestore.collection(Colecciones.AI_LOGS)
            .where { "userId" equalTo uid }
            .get()
            .documents
            .map { doc ->
                val data = doc.data<AiLogDoc>()
                LogIa(
                    id = doc.id,
                    userId = data.userId,
                    checkInId = data.checkInId,
                    aiMessageId = data.aiMessageId,
                    nivelRiesgo = data.riskLevel.toRiskLevelOrGreen(),
                    creadoEn = data.createdAt.toInstant(),
                )
            }
            .sortedByDescending { it.creadoEn }
            .take(limit)
    }
}
