package com.eter.undiamas.core.data.api

import com.eter.undiamas.core.data.UserPreferences

/**
 * Par de tokens de la sesion.
 *
 * El [accessToken] dura 15 minutos y viaja en cada peticion; el [refreshToken] dura 30 dias
 * y solo sirve para pedir otro par. El backend rota el refresh en cada uso: guardar el
 * nuevo no es opcional, si se guarda el viejo la sesion muere en la siguiente renovacion.
 */
data class Tokens(
    val accessToken: String,
    val refreshToken: String,
)

interface TokenStore {
    suspend fun read(): Tokens?
    suspend fun save(tokens: Tokens)
    suspend fun clear()
}

/** Guarda los tokens en DataStore, para que la sesion sobreviva a cerrar la app. */
class PreferencesTokenStore(
    private val preferences: UserPreferences,
) : TokenStore {
    override suspend fun read(): Tokens? = preferences.readTokens()

    override suspend fun save(tokens: Tokens) =
        preferences.saveTokens(tokens.accessToken, tokens.refreshToken)

    override suspend fun clear() = preferences.clearTokens()
}

/**
 * Copia local de quien inicio sesion.
 *
 * Existe para poder abrir la app sin conexion. Sin esto, arrancar en modo avion obligaba a
 * preguntarle al servidor quien eres, fallar, y enseñar una pantalla de error a alguien que
 * tiene todos sus datos guardados en el telefono.
 */
interface SessionCache {
    suspend fun read(): CachedSession?
    suspend fun save(session: CachedSession)
    suspend fun clear()
}

data class CachedSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String,
)

class PreferencesSessionCache(
    private val preferences: UserPreferences,
) : SessionCache {
    override suspend fun read(): CachedSession? = preferences.readSession()

    override suspend fun save(session: CachedSession) = preferences.saveSession(session)

    override suspend fun clear() = preferences.clearSession()
}

class InMemorySessionCache(private var session: CachedSession? = null) : SessionCache {
    override suspend fun read(): CachedSession? = session

    override suspend fun save(session: CachedSession) {
        this.session = session
    }

    override suspend fun clear() {
        session = null
    }
}

/** Para tests y previews: la sesion no sobrevive al proceso. */
class InMemoryTokenStore(private var tokens: Tokens? = null) : TokenStore {
    override suspend fun read(): Tokens? = tokens

    override suspend fun save(tokens: Tokens) {
        this.tokens = tokens
    }

    override suspend fun clear() {
        tokens = null
    }
}
