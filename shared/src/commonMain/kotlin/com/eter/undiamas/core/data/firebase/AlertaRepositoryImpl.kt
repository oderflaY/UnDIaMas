package com.eter.undiamas.core.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Alerta ya resuelta a modelo de dominio, con el id del documento. */
data class Alerta(
    val id: String,
    val mensaje: String,
    val nivelRiesgo: String,
    val epochSeconds: Long,
    val atendida: Boolean,
)

/**
 * Lectura de la coleccion plana `/alerts`, filtrada por `userId`.
 *
 * Deliberadamente sin metodos de escritura: las reglas solo permiten leer, porque estas
 * alertas las genera la Cloud Function `agentChat` via Admin SDK. Exponer un `guardar()`
 * aqui seria ofrecer una operacion condenada a PERMISSION_DENIED.
 */
class AlertaRepositoryImpl {
    private val firestore = Firebase.firestore

    private fun consultaDe(uid: String) =
        firestore.collection(Colecciones.ALERTS).where { "userId" equalTo uid }

    /** Alertas del usuario en vivo. Ante un fallo emite lista vacia en vez de romper la UI. */
    fun observe(uid: String): Flow<List<Alerta>> =
        consultaDe(uid).snapshots
            .map { snapshot -> snapshot.documents.map { it.data<AlertDoc>().toAlerta(it.id) } }
            .map { alertas -> alertas.sortedByDescending { it.epochSeconds } }
            .catch { emit(emptyList()) }

    /** Lectura puntual. Result para que un fallo de permisos no se propague como crash. */
    suspend fun list(): Result<List<Alerta>> = withUid { uid ->
        consultaDe(uid).get().documents
            .map { it.data<AlertDoc>().toAlerta(it.id) }
            .sortedByDescending { it.epochSeconds }
    }
}

private fun AlertDoc.toAlerta(id: String) = Alerta(
    id = id,
    mensaje = message,
    nivelRiesgo = riskLevel,
    epochSeconds = timestamp?.seconds ?: 0,
    atendida = handled,
)
