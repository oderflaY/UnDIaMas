package com.eter.undiamas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.health.connect.client.PermissionController
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

        val biometrics = HealthDataExtractor(applicationContext) { permissions ->
            CompletableDeferred<Set<String>>().also { deferred ->
                pendingPermissions = deferred
                healthPermissionLauncher.launch(permissions)
            }.await()
        }

        setContent {
            App(biometrics = biometrics)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
