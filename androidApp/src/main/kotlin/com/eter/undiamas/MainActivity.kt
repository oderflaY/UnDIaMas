package com.eter.undiamas

import android.os.Bundle
import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.eter.undiamas.avisos.NotificadorAndroid
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.health.connect.client.PermissionController
import com.eter.undiamas.core.data.PREFERENCES_FILE
import com.eter.undiamas.core.data.UserPreferences
import com.eter.undiamas.core.data.api.ApiConfig
import com.eter.undiamas.core.data.initPreferencesPath
import com.eter.undiamas.core.data.local.DATABASE_FILE
import com.eter.undiamas.core.data.local.initConnectivityContext
import com.eter.undiamas.core.data.local.initDatabasePath
import com.eter.undiamas.core.presentation.registerActivityForClose
import com.eter.undiamas.core.presentation.unregisterActivityForClose
import com.eter.undiamas.health.HealthDataExtractor
import kotlinx.coroutines.CompletableDeferred

class MainActivity : ComponentActivity() {

    // Puente entre el ActivityResultContract (callback) y el mundo suspend del extractor.
    private var pendingPermissions: CompletableDeferred<Set<String>>? = null

    private val healthPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted -> pendingPermissions?.complete(granted) }

    // Mismo puente para el permiso de notificaciones, que en Android 13+ hay que pedir.
    private var permisoAvisos: CompletableDeferred<Boolean>? = null

    private val lanzadorPermisoAvisos = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido -> permisoAvisos?.complete(concedido) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Permite que closeApp() cierre esta Activity y la quite de recientes.
        registerActivityForClose(this)

        // Estas tres necesitan Context, así que se inyectan aquí en vez de ensuciar las
        // firmas comunes. Van antes de construir la app: la base de datos se abre al crear
        // el AppState, y sin ruta caería a una base en memoria que se pierde al cerrar.
        initPreferencesPath(applicationContext.filesDir.resolve(PREFERENCES_FILE).absolutePath)
        initDatabasePath(applicationContext.filesDir.resolve(DATABASE_FILE).absolutePath)
        initConnectivityContext(applicationContext)

        // La dirección del backend la fija el buildType: en debug la de la wifi local, en
        // release el dominio https de producción. Nunca queda escrita en el código común.
        ApiConfig.configurar(
            url = BuildConfig.API_BASE_URL,
            permiteCambiar = BuildConfig.PERMITE_CAMBIAR_SERVIDOR,
        )

        val preferences = UserPreferences()

        // Notificaciones locales: los recordatorios según el semáforo. Se le pasa la forma
        // de pedir el permiso, que solo una Activity puede hacer.
        val notificador = NotificadorAndroid(applicationContext).apply {
            solicitarPermiso = {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    true
                } else {
                    CompletableDeferred<Boolean>().also { esperando ->
                        permisoAvisos = esperando
                        lanzadorPermisoAvisos.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }.await()
                }
            }
        }

        val biometrics = HealthDataExtractor(applicationContext) { permissions ->
            CompletableDeferred<Set<String>>().also { deferred ->
                pendingPermissions = deferred
                healthPermissionLauncher.launch(permissions)
            }.await()
        }

        setContent {
            App(biometrics = biometrics, preferences = preferences, notificador = notificador)
        }
    }

    override fun onDestroy() {
        unregisterActivityForClose()
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
