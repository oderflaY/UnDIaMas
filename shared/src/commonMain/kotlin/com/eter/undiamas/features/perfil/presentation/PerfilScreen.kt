package com.eter.undiamas.features.perfil.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.TrustedContact
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.theme.AccentPerfil
import com.eter.undiamas.core.presentation.theme.HeroBrush

@Composable
fun PerfilScreen(state: AppState, navigator: Navigator) {
    var name by remember { mutableStateOf(state.profile.displayName) }
    var contactName by remember { mutableStateOf(state.profile.trustedContact?.name.orEmpty()) }
    var contactPhone by remember { mutableStateOf(state.profile.trustedContact?.phone.orEmpty()) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(96.dp).background(HeroBrush, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                state.profile.displayName.take(1).uppercase(),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
        }
        Text(state.profile.displayName, style = MaterialTheme.typography.headlineSmall)

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DATOS PERSONALES", style = MaterialTheme.typography.labelMedium, color = AccentPerfil)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tu nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Contacto de confianza") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text("Teléfono de contacto") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPerfil),
                    onClick = {
                        state.updateProfile { profile ->
                            profile.copy(
                                displayName = name,
                                trustedContact = if (contactName.isBlank()) null else TrustedContact(contactName, contactPhone),
                            )
                        }
                        state.notify("Perfil actualizado ✨")
                    },
                ) { Text("Guardar cambios") }
            }
        }

        OutlinedButton(onClick = { navigator.goTo(Screen.Configuracion) }, modifier = Modifier.fillMaxWidth()) {
            Text("⚙️  Configuración")
        }
    }
}
