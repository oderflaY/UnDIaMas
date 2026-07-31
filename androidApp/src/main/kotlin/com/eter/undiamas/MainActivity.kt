
package com.eter.undiamas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.eter.undiamas.core.presentation.unregisterActivityForClose
import com.eter.undiamas.core.presentation.registerActivityForClose

class MainActivity : ComponentActivity() {

    override fun onDestroy() {
        unregisterActivityForClose()
        super.onDestroy()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Permite que closeApp() cierre esta Activity y la quite de recientes.
        registerActivityForClose(this)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}