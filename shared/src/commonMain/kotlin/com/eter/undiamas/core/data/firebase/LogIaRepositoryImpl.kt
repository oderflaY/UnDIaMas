package com.eter.undiamas.core.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

private const val COLECCION_LOGS_IA = "logs_ia"
private const val CAMPO_USER_ID = "userId"

/** Entrada de la traza check-in -> nivel de riesgo -> respuesta de la IA. */
data class LogIa(
    val id: String,
    val userId: String,
    val checkInId: String,
    val nivelRiesgo: String,
    val prompt: String,
    val respuesta: String,
    val modelo: String,
    val epochSeconds: Long,
)

/**
 * Lectura de `/logs_ia`, la coleccion raiz de trazabilidad de la IA.
 *
 * Dos cosas la hacen distinta del resto de repositorios:
 *
 * 1. Es una coleccion raiz, no una subcoleccion del usuario, asi que el aislamiento no
 *    sale de la ruta sino del campo `userId`. La regla compara ese campo contra
 *    `request.auth.uid`, por lo que la consulta DEBE filtrar por el: sin
 *    `whereEqualTo(userId, uid)` Firestore intenta leer documentos ajenos y rechaza la
 *    consulta entera con PERMISSION_DENIED, aunque el usuario tenga logs propios.
 *
 * 2. Solo lectura: las escribe la Cloud Function via Admin SDK (`allow write: if false`).
 */
class LogIaRepositoryImpl {
    private val firestore = Firebase.firestore

    /** Logs del usuario actual, del mas reciente al mas antiguo. */
    suspend fun list(limit: Int = 50): Result<List<LogIa>> = withUid { uid ->
        firestore.collection(COLECCION_LOGS_IA)
            .where { CAMPO_USER_ID equalTo uid }
            .get()
            .documents
            .map { doc ->
                val data = doc.data<LogIaDoc>()
                LogIa(
                    id = doc.id,
                    userId = data.userId,
                    checkInId = data.checkInId,
                    nivelRiesgo = data.nivelRiesgo,
                    prompt = data.prompt,
                    respuesta = data.respuesta,
                    modelo = data.modelo,
                    epochSeconds = data.fecha,
                )
            }
            .sortedByDescending { it.epochSeconds }
            .take(limit)
    }
}
