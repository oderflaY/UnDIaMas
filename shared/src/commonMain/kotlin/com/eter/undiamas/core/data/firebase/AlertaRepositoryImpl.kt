package com.eter.undiamas.core.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val COLECCION_USUARIOS = "usuarios"
private const val SUBCOLECCION_ALERTAS = "alertas"

/** Alerta ya resuelta a modelo de dominio, con el id del documento. */
data class Alerta(
    val id: String,
    val tipo: String,
    val mensaje: String,
    val nivelRiesgo: String,
    val epochSeconds: Long,
    val atendida: Boolean,
)

/**
 * Lectura de `/usuarios/{uid}/alertas`.
 *
 * Deliberadamente sin metodos de escritura: las reglas declaran `allow write: if false`
 * porque estas alertas solo las genera la Cloud Function `agentChat` via Admin SDK.
 * Exponer un `guardar()` aqui seria ofrecer una operacion condenada a PERMISSION_DENIED.
 */
class AlertaRepositoryImpl {
    private val firestore = Firebase.firestore

    private fun alertasDe(uid: String) =
        firestore.collection(COLECCION_USUARIOS).document(uid).collection(SUBCOLECCION_ALERTAS)

    /** Alertas del usuario en vivo. Ante un fallo emite lista vacia en vez de romper la UI. */
    fun observe(uid: String): Flow<List<Alerta>> =
        alertasDe(uid).snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    val data = doc.data<AlertaDoc>()
                    Alerta(
                        id = doc.id,
                        tipo = data.tipo,
                        mensaje = data.mensaje,
                        nivelRiesgo = data.nivelRiesgo,
                        epochSeconds = data.fecha,
                        atendida = data.atendida,
                    )
                }.sortedByDescending { it.epochSeconds }
            }
            .catch { emit(emptyList()) }

    /** Lectura puntual. Result para que un fallo de permisos no se propague como crash. */
    suspend fun list(): Result<List<Alerta>> = withUid { uid ->
        alertasDe(uid).get().documents.map { doc ->
            val data = doc.data<AlertaDoc>()
            Alerta(
                id = doc.id,
                tipo = data.tipo,
                mensaje = data.mensaje,
                nivelRiesgo = data.nivelRiesgo,
                epochSeconds = data.fecha,
                atendida = data.atendida,
            )
        }.sortedByDescending { it.epochSeconds }
    }
}
