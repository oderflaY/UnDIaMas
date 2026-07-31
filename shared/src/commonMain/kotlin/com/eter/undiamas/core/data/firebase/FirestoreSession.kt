package com.eter.undiamas.core.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

/**
 * No hay sesion activa cuando se intento tocar Firestore.
 *
 * Es un tipo propio y no una excepcion generica para que quien llama pueda distinguir
 * "falta iniciar sesion" de "fallo la red", que se resuelven de forma muy distinta.
 */
class NoAuthenticatedUserException :
    IllegalStateException("No hay sesión activa. Inicia sesión antes de leer o escribir datos.")

/**
 * uid de la sesion actual, o excepcion controlada si no hay ninguna.
 *
 * Toda ruta de Firestore de esta app se construye a partir de este uid. Las reglas exigen
 * `request.auth.uid == uid`, asi que pedirlo aqui garantiza que jamas se arme una ruta
 * apuntando a los datos de otra persona: el peor fallo posible en una app con historial
 * de recaidas y conversaciones con la IA.
 */
fun requireUid(): String =
    Firebase.auth.currentUser?.uid ?: throw NoAuthenticatedUserException()

/** uid actual sin lanzar, para decidir en la UI si mostrar algo o no. */
fun currentUidOrNull(): String? = Firebase.auth.currentUser?.uid

/**
 * Ejecuta una operacion de Firestore garantizando sesion y sin dejar escapar excepciones.
 *
 * Devuelve [Result] en vez de lanzar: un fallo de red o de permisos no debe tumbar la app
 * de alguien que quiza esta en crisis. Quien llama decide si reintenta o degrada.
 */
suspend fun <T> withUid(block: suspend (uid: String) -> T): Result<T> =
    runCatching { block(requireUid()) }
