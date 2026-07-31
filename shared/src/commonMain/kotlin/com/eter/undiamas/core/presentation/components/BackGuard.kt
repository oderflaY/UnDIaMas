package com.eter.undiamas.core.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

/**
 * Intercepta el gesto/botón de retroceso del sistema.
 *
 * Envuelve la API experimental de Compose Multiplatform en un único punto, para que el
 * opt-in no se repita por toda la app y para poder cambiar la implementación si cambia.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InterceptBack(enabled: Boolean = true, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

/**
 * Retroceso protegido por confirmación: el botón atrás no sale de inmediato, sino que
 * abre un diálogo. Se usa donde salir por accidente cuesta caro (una crisis en curso,
 * un registro a medio escribir).
 */
@Composable
fun GuardedBack(
    enabled: Boolean = true,
    title: String,
    message: String,
    confirmLabel: String = "Salir",
    dismissLabel: String = "Quedarme",
    onConfirmExit: () -> Unit,
) {
    var asking by remember { mutableStateOf(false) }

    InterceptBack(enabled = enabled) { asking = true }

    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        asking = false
                        onConfirmExit()
                    },
                ) { Text(confirmLabel) }
            },
            dismissButton = { TextButton(onClick = { asking = false }) { Text(dismissLabel) } },
        )
    }
}
