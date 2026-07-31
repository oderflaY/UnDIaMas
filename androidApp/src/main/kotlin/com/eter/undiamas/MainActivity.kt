package com.eter.undiamas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.health.connect.client.PermissionController
import com.eter.undiamas.core.data.PREFERENCES_FILE
import com.eter.undiamas.core.data.UserPreferences
import com.eter.undiamas.core.data.initPreferencesPath
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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Permite que closeApp() cierre esta Activity y la quite de recientes.
        registerActivityForClose(this)

        // La ruta del archivo de preferencias necesita Context, así que se inyecta aquí.
        initPreferencesPath(applicationContext.filesDir.resolve(PREFERENCES_FILE).absolutePath)
        val preferences = UserPreferences()

        val biometrics = HealthDataExtractor(applicationContext) { permissions ->
            CompletableDeferred<Set<String>>().also { deferred ->
                pendingPermissions = deferred
                healthPermissionLauncher.launch(permissions)
            }.await()
        }

        setContent {
            App(biometrics = biometrics, preferences = preferences)
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
