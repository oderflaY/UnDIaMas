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
import com.eter.undiamas.core.data.PREFERENCES_FILE
import com.eter.undiamas.core.data.UserPreferences
import com.eter.undiamas.core.data.initPreferencesPath
import com.eter.undiamas.core.data.local.DATABASE_FILE
import com.eter.undiamas.core.data.local.initConnectivityContext
import com.eter.undiamas.core.data.local.initDatabasePath
import com.eter.undiamas.core.presentation.registerActivityForClose
import com.eter.undiamas.core.presentation.unregisterActivityForClose
import kotlinx.coroutines.CompletableDeferred

class MainActivity : ComponentActivity() {

    // Puente entre el ActivityResultContract (callback) y el mundo suspend: en
    // Android 13+ hay que pedir el permiso de notificaciones.
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

        setContent {
            // Lo decide la variante de compilación, no un ajuste de la app: una beta que
            // se pudiera reconectar al servidor a mitad tendría los datos en dos sitios.
            App(
                preferences = preferences,
                notificador = notificador,
                modoLocal = BuildConfig.MODO_LOCAL,
            )
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
