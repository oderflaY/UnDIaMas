package com.eter.undiamas.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eter.undiamas.core.data.api.CachedSession
import com.eter.undiamas.core.data.api.Tokens
import com.eter.undiamas.core.domain.model.AddictionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.IOException
import okio.Path.Companion.toPath

/** Nombre del archivo de preferencias; la ruta la resuelve cada plataforma. */
const val PREFERENCES_FILE = "undiamas.preferences_pb"

/** Ruta del archivo de DataStore, distinta en Android y en iOS. */
expect fun preferencesFilePath(): String

/**
 * Preferencias locales que sobreviven al cierre de la app.
 *
 * Guarda tres cosas: los tokens de la sesión (para no pedir la contraseña cada vez que se
 * abre la app), lo mínimo que evita repetir el cuestionario inicial, y el tipo de adicción,
 * que el backend todavía no modela. El historial real vive en el servidor.
 */
class UserPreferences(
    private val store: DataStore<Preferences> = defaultStore(),
) {
    private object Keys {
        val onboardingCompleted = booleanPreferencesKey("ONBOARDING_COMPLETED")
        val userAddiction = stringPreferencesKey("USER_ADDICTION")
        val displayName = stringPreferencesKey("USER_DISPLAY_NAME")
        val accessToken = stringPreferencesKey("AUTH_ACCESS_TOKEN")
        val refreshToken = stringPreferencesKey("AUTH_REFRESH_TOKEN")
        val serverUrl = stringPreferencesKey("SERVER_BASE_URL")
        val sessionUserId = stringPreferencesKey("SESSION_USER_ID")
        val sessionEmail = stringPreferencesKey("SESSION_EMAIL")
        val sessionName = stringPreferencesKey("SESSION_NAME")
        val sessionRole = stringPreferencesKey("SESSION_ROLE")
    }

    /** Un archivo corrupto o ilegible no debe impedir abrir la app: se cae a vacío. */
    private val preferences: Flow<Preferences> = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val onboardingCompleted: Flow<Boolean> =
        preferences.map { it[Keys.onboardingCompleted] ?: false }

    val displayName: Flow<String> =
        preferences.map { it[Keys.displayName].orEmpty() }

    /** Null si aún no eligió, o si el valor guardado ya no corresponde a un tipo conocido. */
    val userAddiction: Flow<AddictionType?> = preferences.map { prefs ->
        prefs[Keys.userAddiction]?.let { saved ->
            AddictionType.entries.firstOrNull { it.name == saved }
        }
    }

    suspend fun saveOnboarding(displayName: String, addiction: AddictionType?) {
        store.edit { prefs ->
            prefs[Keys.onboardingCompleted] = true
            prefs[Keys.displayName] = displayName
            if (addiction != null) prefs[Keys.userAddiction] = addiction.name
        }
    }

    suspend fun saveAddiction(addiction: AddictionType) {
        store.edit { prefs -> prefs[Keys.userAddiction] = addiction.name }
    }

    /** Null si nunca hubo sesión, o si se cerró: en ambos casos toca pedir credenciales. */
    suspend fun readTokens(): Tokens? {
        val prefs = preferences.first()
        val access = prefs[Keys.accessToken]
        val refresh = prefs[Keys.refreshToken]
        return if (access.isNullOrBlank() || refresh.isNullOrBlank()) null else Tokens(access, refresh)
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        store.edit { prefs ->
            prefs[Keys.accessToken] = accessToken
            prefs[Keys.refreshToken] = refreshToken
        }
    }

    /**
     * Dirección del backend escrita a mano, o null para usar la de fábrica.
     *
     * Se guarda aparte de los tokens porque no es un dato de la persona: es a qué máquina
     * apunta esta instalación, y tiene que sobrevivir a cerrar sesión.
     */
    suspend fun readServerUrl(): String? =
        preferences.first()[Keys.serverUrl]?.takeIf { it.isNotBlank() }

    suspend fun saveServerUrl(url: String) {
        store.edit { prefs -> prefs[Keys.serverUrl] = url }
    }

    /**
     * Quién inició sesión la última vez.
     *
     * Se guarda para poder abrir la app sin conexión: con esto y el refresh token basta
     * para entrar y ver lo que hay en el teléfono, sin preguntarle nada al servidor.
     */
    suspend fun readSession(): CachedSession? {
        val prefs = preferences.first()
        val id = prefs[Keys.sessionUserId]
        return if (id.isNullOrBlank()) {
            null
        } else {
            CachedSession(
                userId = id,
                email = prefs[Keys.sessionEmail].orEmpty(),
                displayName = prefs[Keys.sessionName].orEmpty(),
                role = prefs[Keys.sessionRole] ?: "patient",
            )
        }
    }

    suspend fun saveSession(session: CachedSession) {
        store.edit { prefs ->
            prefs[Keys.sessionUserId] = session.userId
            prefs[Keys.sessionEmail] = session.email
            prefs[Keys.sessionName] = session.displayName
            prefs[Keys.sessionRole] = session.role
        }
    }

    suspend fun clearSession() {
        store.edit { prefs ->
            prefs.remove(Keys.sessionUserId)
            prefs.remove(Keys.sessionEmail)
            prefs.remove(Keys.sessionName)
            prefs.remove(Keys.sessionRole)
        }
    }

    /** Cierre de sesión: los tokens se van, pero el onboarding local se queda. */
    suspend fun clearTokens() {
        store.edit { prefs ->
            prefs.remove(Keys.accessToken)
            prefs.remove(Keys.refreshToken)
        }
    }

    /** Derecho al olvido: borra también lo persistido, no solo lo que hay en memoria. */
    suspend fun clear() {
        store.edit { it.clear() }
    }
}

private fun defaultStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath { preferencesFilePath().toPath() }
