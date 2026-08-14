package com.eter.undiamas.core.data.api

import com.eter.undiamas.core.domain.repository.AuthRepository
import com.eter.undiamas.core.domain.repository.Session
import kotlin.random.Random
import kotlin.time.Clock

/**
 * Sesion de la beta local: sin servidor, sin correo y sin contraseña.
 *
 * Existe para poder probar la app entera con el backend apagado. No hay nada que
 * autenticar porque no hay nadie al otro lado: el "usuario" es solo una etiqueta con la
 * que marcar las filas de SQLite, y vive en este telefono y en ningun otro sitio.
 *
 * Consecuencias que conviene decir en voz alta, porque no son obvias:
 *
 * - **No pide credenciales.** Pedir un correo y una contraseña que no se comprueban contra
 *   nada seria teatro, y ademas invitaria a reutilizar una contraseña de verdad.
 * - **Los datos no salen del telefono.** Si se desinstala la app o se borran sus datos, no
 *   hay copia en ningun servidor. Es la pantalla de Configuracion la que tiene que decirlo
 *   claro: en una app de recuperacion, perder meses de diario sin aviso es grave.
 */
class LocalAuthRepository(
    private val sessionCache: SessionCache,
) : AuthRepository {

    /**
     * Devuelve la sesion de este telefono, creandola la primera vez.
     *
     * Nunca devuelve null: en la beta local no existe el caso "toca pedir credenciales",
     * asi que quien abre la app entra directo.
     */
    override suspend fun restore(): Session =
        sessionCache.read()?.toSession() ?: crear()

    /**
     * Alta y entrada son lo mismo aqui, y ninguna de las dos mira lo que se le pasa.
     *
     * Estan implementadas —en vez de lanzar— porque el flujo de la app puede llamarlas, y
     * en una beta es preferible que entre a que se quede en un error sin salida.
     */
    override suspend fun register(email: String, password: String, displayName: String): Session =
        restore().let { sesion ->
            if (displayName.isBlank()) {
                sesion
            } else {
                sesion.copy(displayName = displayName).also { it.guardar() }
            }
        }

    override suspend fun login(email: String, password: String): Session = restore()

    /**
     * Empieza de cero.
     *
     * Sin servidor no hay "cerrar sesion" en el sentido normal: lo unico que puede hacer es
     * olvidar quien era esta instalacion. La siguiente apertura crea un usuario nuevo, y la
     * cache local del anterior se descarta al detectar que cambio la cuenta.
     */
    override suspend fun logout() {
        sessionCache.clear()
    }

    /**
     * No hay contraseña que recuperar: en la beta local nunca se pidio ninguna.
     *
     * Se lanza el mismo error que cuando al servidor le falta el SMTP, para que la pantalla
     * de recuperacion no tenga que saber en que modo esta corriendo la app.
     */
    override suspend fun requestPasswordReset(email: String): Unit =
        throw ApiException(ApiErrorCode.UNAVAILABLE, 503, "sin servidor en la beta local")

    override suspend fun resetPassword(email: String, code: String, newPassword: String): Unit =
        throw ApiException(ApiErrorCode.UNAVAILABLE, 503, "sin servidor en la beta local")

    private suspend fun crear(): Session {
        // Aleatorio y con marca de tiempo para que dos instalaciones no compartan id: si
        // coincidieran, la cache local de una podria abrirse creyendo que es la de la otra.
        val id = "local-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(100_000)}"
        return Session(userId = id, email = "", displayName = "").also { it.guardar() }
    }

    private suspend fun Session.guardar() = sessionCache.save(
        CachedSession(userId = userId, email = email, displayName = displayName, role = role),
    )

    private fun CachedSession.toSession() = Session(userId, email, displayName, role)
}
