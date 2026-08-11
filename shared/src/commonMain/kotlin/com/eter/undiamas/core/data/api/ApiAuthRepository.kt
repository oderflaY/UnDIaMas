package com.eter.undiamas.core.data.api

import com.eter.undiamas.core.domain.repository.AuthRepository
import com.eter.undiamas.core.domain.repository.Session

/**
 * Sesion contra el backend propio, con copia local.
 *
 * La app nunca guarda ni ve el id del usuario para mandarlo: viaja dentro del token y el
 * servidor lo saca de ahi. [Session.userId] existe solo para que la UI pueda etiquetar lo
 * que ya tiene en memoria.
 */
class ApiAuthRepository(
    private val api: UnDiaMasApi,
    private val tokenStore: TokenStore,
    private val sessionCache: SessionCache = InMemorySessionCache(),
) : AuthRepository {

    /**
     * Reabre la sesion guardada.
     *
     * Tres caminos, y la diferencia entre ellos importa mucho:
     *
     * - **Sin tokens**: no hay sesion, toca pedir credenciales.
     * - **El servidor dice 401**: la sesion murio de verdad (caducada o cerrada desde otro
     *   dispositivo). Se borra todo y a la pantalla de entrada.
     * - **No se pudo preguntar** (sin red, servidor caido): se entra con la copia local. Es
     *   lo que permite abrir la app en el metro y ver la racha, el diario y el "por que"
     *   personal. Echar a alguien de su propia app porque el wifi no va seria absurdo, y en
     *   una app de recuperacion, cruel.
     */
    override suspend fun restore(): Session? {
        if (tokenStore.read() == null) return null

        return runCatching { api.me() }
            .map { user ->
                Session(
                    userId = user.id,
                    email = user.email,
                    displayName = user.displayName,
                    role = user.role,
                ).also { it.cachear() }
            }
            .getOrElse { error ->
                if (error is ApiException && error.isAuthExpired) {
                    cerrarLocalmente()
                    null
                } else {
                    sessionCache.read()?.toSession()
                        // Sin copia local no se puede saber de quien son los datos, y sin
                        // eso no se puede abrir la caché de nadie sin riesgo de mezclarlas.
                        ?: throw error
                }
            }
    }

    override suspend fun register(email: String, password: String, displayName: String): Session =
        api.register(email, password, displayName).toSession().also { it.cachear() }

    override suspend fun login(email: String, password: String): Session =
        api.login(email, password).toSession().also { it.cachear() }

    override suspend fun logout() {
        api.logout()
        sessionCache.clear()
    }

    private suspend fun cerrarLocalmente() {
        tokenStore.clear()
        api.http.forgetCachedToken()
        sessionCache.clear()
    }

    private suspend fun Session.cachear() = sessionCache.save(
        CachedSession(userId = userId, email = email, displayName = displayName, role = role),
    )

    private fun CachedSession.toSession() = Session(userId, email, displayName, role)

    private fun AuthResponse.toSession() = Session(
        userId = user.id,
        email = user.email,
        displayName = user.displayName,
        role = user.role,
    )
}
