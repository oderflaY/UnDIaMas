package com.eter.undiamas.core.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberLinkOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                // Sin navegador no hay nada que hacer, pero tampoco hay motivo para
                // tumbar Configuracion: el resto de los ajustes siguen funcionando.
            }
        }
    }
}
