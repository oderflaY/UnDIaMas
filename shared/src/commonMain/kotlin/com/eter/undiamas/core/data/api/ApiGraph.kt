package com.eter.undiamas.core.data.api

import com.eter.undiamas.core.data.UserPreferences
import com.eter.undiamas.core.data.local.ConnectivityMonitor
import com.eter.undiamas.core.data.local.LocalStore
import com.eter.undiamas.core.data.local.Outbox
import com.eter.undiamas.core.data.local.SyncManager
import com.eter.undiamas.core.data.local.UndiamasDatabase
import com.eter.undiamas.core.data.local.crearBaseDeDatos
import com.eter.undiamas.core.domain.repository.AuthRepository
import io.ktor.client.engine.HttpClientEngine

/**
 * Arma la capa de datos completa: base local, cliente HTTP, API y repositorios.
 *
 * Existe para que [com.eter.undiamas.core.presentation.AppState] no tenga que conocer a
 * Ktor ni a SQLite ni el orden en que se construye cada pieza, y para poder cambiar el
 * motor HTTP o la base por unos de prueba sin tocar nada más.
 */
class ApiGraph(
    preferences: UserPreferences? = null,
    /**
     * Beta sin backend: todo se guarda en el telefono y nada sale de el.
     *
     * No es un "modo sin conexion" —eso ya existe y guarda para enviar despues— sino la
     * ausencia total de servidor: no hay sesion que pedir, ni cola que vaciar, ni muro de
     * comunidad. Lo elige la variante de compilacion, no la persona que usa la app.
     */
    val modoLocal: Boolean = false,
    baseUrl: () -> String = { ApiConfig.baseUrl },
    engine: HttpClientEngine? = null,
    database: UndiamasDatabase = crearBaseDeDatos(),
) {
    /** Sin preferencias (previews, tests) la sesion no sobrevive al proceso. */
    val tokenStore: TokenStore =
        preferences?.let { PreferencesTokenStore(it) } ?: InMemoryTokenStore()

    /** Quien inicio sesion, guardado en el telefono para poder abrir la app sin red. */
    val sessionCache: SessionCache =
        preferences?.let { PreferencesSessionCache(it) } ?: InMemorySessionCache()

    /**
     * Id de la sesion actual.
     *
     * Solo sirve para etiquetar los objetos de dominio que ya estan en memoria: al servidor
     * no se le manda nunca, lo saca del token. Lo escribe AppState al entrar.
     */
    var currentUserId: String = ""

    val http = createApiHttpClient(tokenStore, baseUrl, engine)

    val api = UnDiaMasApi(http, tokenStore)

    val local = LocalStore(database, marcarPendientes = !modoLocal)
    val outbox = Outbox(database, activa = !modoLocal)
    val sync = SyncManager(api, local, outbox)
    val connectivity = ConnectivityMonitor()

    /**
     * Cada escritura pide sincronizar en cuanto termina de guardarse.
     *
     * Si no hay red el intento falla y no pasa nada: el dato ya esta en SQLite y la cola lo
     * reintentara. Por eso se traga el error aqui en vez de propagarlo a la pantalla.
     */
    private val alSincronizar: suspend () -> Unit =
        if (modoLocal) ({ }) else ({ runCatching { sync.sincronizar() }; Unit })

    val auth: AuthRepository =
        if (modoLocal) LocalAuthRepository(sessionCache) else ApiAuthRepository(api, tokenStore, sessionCache)
    val perfil = OfflinePerfilRepository(api, local, outbox, alSincronizar)
    val checkIns = OfflineCheckInRepository(api, local, outbox, alSincronizar)
    val diary = OfflineDiaryRepository(api, local, outbox, alSincronizar)
    val moods = OfflineMoodRepository(api, local, outbox, alSincronizar)
    val relapses = OfflineRelapseRepository(api, local, outbox, alSincronizar)
    val trafficLight = OfflineTrafficLightRepository(api, local, outbox, alSincronizar)
    val alerts = OfflineAlertRepository(api, local, outbox)
    val reminders = OfflineReminderRepository(api, outbox, alSincronizar)
    val stats = ApiStatsRepository(api)
    val comunidad = ApiComunidadRepository(api)
    val events = EventStream(api)
}
