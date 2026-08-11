package com.eter.undiamas.features.auth.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.GradientCard
import com.eter.undiamas.core.presentation.theme.PrimaryVioletBrush
import com.eter.undiamas.core.presentation.theme.RiskRed

private const val MIN_PASSWORD_LENGTH = 8

/**
 * Entrada a la app: crear cuenta o iniciar sesión.
 *
 * Aparece porque el backend no emite sesiones anónimas. El texto lo explica en positivo —
 * la cuenta es lo que hace que el historial siga a la persona si cambia de teléfono, no un
 * trámite — y no pide más datos de los necesarios: aquí no se pregunta por la adicción ni
 * por nada sensible, eso viene después y dentro de la app.
 */
@Composable
fun AuthScreen(state: AppState) {
    var isRegistering by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val emailLooksValid = email.contains("@") && email.contains(".")
    val passwordLongEnough = password.length >= MIN_PASSWORD_LENGTH
    val canSubmit = emailLooksValid && passwordLongEnough &&
        (!isRegistering || name.isNotBlank()) && !state.isAuthenticating

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        GradientCard(brush = PrimaryVioletBrush) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Un día más", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Tu cuenta guarda tu racha y tu historial en el servidor, para que sigan " +
                        "contigo aunque cambies de teléfono.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        AnimatedContent(targetState = isRegistering) { registering ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (registering) "Crear cuenta" else "Iniciar sesión",
                    style = MaterialTheme.typography.titleLarge,
                )

                if (registering) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("¿Cómo quieres que te llame?") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = { Text("Correo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    supportingText = {
                        if (registering) Text("Mínimo $MIN_PASSWORD_LENGTH caracteres")
                    },
                    isError = password.isNotEmpty() && !passwordLongEnough,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                )
            }
        }

        state.authError?.let { error ->
            Text(error, style = MaterialTheme.typography.labelLarge, color = RiskRed)
        }

        Button(
            onClick = {
                if (isRegistering) {
                    state.register(email, password, name)
                } else {
                    state.signInWithEmail(email, password)
                }
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isAuthenticating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(if (isRegistering) "Crear cuenta" else "Entrar")
            }
        }

        TextButton(
            onClick = {
                isRegistering = !isRegistering
                state.clearAuthError()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (isRegistering) "Ya tengo cuenta" else "Quiero crear una cuenta",
            )
        }
    }
}
