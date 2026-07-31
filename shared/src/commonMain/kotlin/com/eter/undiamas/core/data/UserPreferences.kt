package com.eter.undiamas.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eter.undiamas.core.domain.model.AddictionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
 * Guarda lo mínimo que evita repetir el cuestionario inicial: si ya se completó, el
 * nombre y qué adicción eligió la persona. El historial completo (check-ins, diario)
 * sigue en memoria hasta que Fase 3 conecte Firestore, que es donde corresponde.
 */
class UserPreferences(
    private val store: DataStore<Preferences> = defaultStore(),
) {
    private object Keys {
        val onboardingCompleted = booleanPreferencesKey("ONBOARDING_COMPLETED")
        val userAddiction = stringPreferencesKey("USER_ADDICTION")
        val displayName = stringPreferencesKey("USER_DISPLAY_NAME")
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

    /** Derecho al olvido: borra también lo persistido, no solo lo que hay en memoria. */
    suspend fun clear() {
        store.edit { it.clear() }
    }
}

private fun defaultStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath { preferencesFilePath().toPath() }
