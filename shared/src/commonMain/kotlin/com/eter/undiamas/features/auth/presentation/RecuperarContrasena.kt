package com.eter.undiamas.features.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.theme.RiskRed

/**
 * Recuperar la contraseña, en un diálogo de dos pasos.
 *
 * Existe porque sin esto la única salida para quien olvida su contraseña es crear otra
 * cuenta, y crear otra cuenta aquí significa empezar la racha de cero. Perder ocho meses de
 * sobriedad registrados por no acordarse de una contraseña sería la peor forma posible de
 * fallarle a alguien.
 *
 * El servidor solo monta estas rutas si tiene SMTP configurado. Cuando no lo tiene contesta
 * 503, y entonces esto lo dice claramente en vez de fingir que el correo va en camino.
 */
@Composable
fun DialogoDeRecuperacion(state: AppState) {
    val estado = state.recuperacion ?: return
    var codigo by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }

    val nuevaValida = nueva.length >= MIN_PASSWORD_LENGTH

    AlertDialog(
        onDismissRequest = { state.cerrarRecuperacion() },
        title = {
            Text(
                when {
                    estado.sinCorreoEnElServidor -> "Todavía no podemos hacerlo"
                    estado.codigoEnviado -> "Revisa tu correo"
                    else -> "Recuperar mi contraseña"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    // Sin SMTP no hay nada que reintentar: se dice y se ofrece la salida real.
                    estado.sinCorreoEnElServidor -> Text(
                        "El servidor todavía no puede enviar correos, así que no hay forma " +
                            "automática de recuperar tu contraseña. Escríbenos y lo " +
                            "resolvemos a mano: tu historial sigue guardado y no se pierde.",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    estado.codigoEnviado -> {
                        Text(
                            "Te enviamos un código a ${estado.email}. Si no lo ves, mira en " +
                                "correo no deseado.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedTextField(
                            value = codigo,
                            onValueChange = { codigo = it.trim() },
                            label = { Text("Código") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        )
                        OutlinedTextField(
                            value = nueva,
                            onValueChange = { nueva = it },
                            label = { Text("Nueva contraseña") },
                            supportingText = { Text("Mínimo $MIN_PASSWORD_LENGTH caracteres") },
                            isError = nueva.isNotEmpty() && !nuevaValida,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        )
                    }

                    else -> Text(
                        "Te mandamos un código a ${estado.email} para que puedas poner una " +
                            "contraseña nueva sin perder nada de lo que llevas.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                estado.error?.takeIf { !estado.sinCorreoEnElServidor }?.let { error ->
                    Text(error, style = MaterialTheme.typography.labelLarge, color = RiskRed)
                }
            }
        },
        confirmButton = {
            if (estado.sinCorreoEnElServidor) {
                TextButton(onClick = { state.cerrarRecuperacion() }) { Text("Entendido") }
            } else {
                Button(
                    enabled = !estado.enviando && (!estado.codigoEnviado || (codigo.isNotBlank() && nuevaValida)),
                    onClick = {
                        if (estado.codigoEnviado) {
                            state.cambiarContrasena(codigo, nueva)
                        } else {
                            state.pedirCodigoDeRecuperacion()
                        }
                    },
                ) {
                    if (estado.enviando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(if (estado.codigoEnviado) "Cambiar contraseña" else "Enviar código")
                    }
                }
            }
        },
        dismissButton = {
            if (!estado.sinCorreoEnElServidor) {
                TextButton(onClick = { state.cerrarRecuperacion() }) { Text("Cancelar") }
            }
        },
    )
}
