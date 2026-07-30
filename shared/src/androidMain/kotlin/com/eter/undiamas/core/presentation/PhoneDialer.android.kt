package com.eter.undiamas.core.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPhoneDialer(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { rawNumber ->
            val number = sanitizePhoneNumber(rawNumber)
            if (number.isNotBlank()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    // Un dispositivo sin app de teléfono (tablet, emulador) no debe tumbar
                    // la pantalla de emergencia: el número sigue visible para marcarlo a mano.
                }
            }
        }
    }
}
