package com.eter.undiamas.features.perfil.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eter.undiamas.core.domain.model.AddictionType
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.icon
import com.eter.undiamas.core.presentation.theme.AccentOrange
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.theme.BrandPurple
import com.eter.undiamas.core.presentation.theme.HairlineLight
import com.eter.undiamas.core.presentation.theme.InkMuted
import com.eter.undiamas.core.presentation.theme.InkStrong
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Editar perfil.
 *
 * Todo lo que se toca aquí se guarda primero en el teléfono y se envía después, así que el
 * formulario funciona igual sin conexión. El botón dice "Guardar cambios" y eso es lo que
 * pasa: lo guarda. Que el servidor se entere más tarde es un detalle de implementación que
 * no tiene por qué preocupar a quien lo pulsa.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditarPerfilScreen(state: AppState, onVolver: () -> Unit) {
    val perfil = state.profile

    var nombre by remember(perfil.displayName) { mutableStateOf(perfil.displayName) }
    var correo by remember { mutableStateOf(state.session?.email.orEmpty()) }
    var objetivo by remember(perfil.addiction) { mutableStateOf(perfil.addiction) }

    val zona = TimeZone.currentSystemDefault()
    val inicio = perfil.sobrietyStartDate.toLocalDateTime(zona).date
    val fechaTexto = "${dosDigitos(inicio.monthNumber)}/${dosDigitos(inicio.dayOfMonth)}/${inicio.year}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        // Avatar con la inicial. No hay foto de verdad todavía; el botón lo dice al pulsarlo
        // en vez de abrir un selector que no lleva a ninguna parte.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier.size(96.dp).background(BrandPurple, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = nombre.trim().firstOrNull()?.uppercase() ?: "·",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(AccentOrange, CircleShape)
                        .pressable(onClick = { state.notify("Cambiar la foto todavía no está disponible") }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        AppIcons.Editar,
                        contentDescription = "Cambiar foto",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                "Cambiar foto",
                style = MaterialTheme.typography.labelLarge,
                color = BrandPurple,
                modifier = Modifier.pressable(
                    onClick = { state.notify("Cambiar la foto todavía no está disponible") },
                ),
            )
        }

        CampoEtiquetado("Nombre") {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                leadingIcon = { Icon(AppIcons.Usuario, null, tint = InkMuted) },
                placeholder = { Text("¿Cómo quieres que te llame?") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = coloresDeCampo(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        CampoEtiquetado("Correo electrónico") {
            OutlinedTextField(
                value = correo,
                onValueChange = { },
                readOnly = true,
                leadingIcon = { Icon(AppIcons.Correo, null, tint = InkMuted) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = coloresDeCampo(),
                modifier = Modifier.fillMaxWidth(),
            )
            // El correo identifica la cuenta en el servidor: cambiarlo no es editar un
            // campo, es migrar una sesión, y eso no se resuelve en este formulario.
            Text(
                "Tu correo identifica tu cuenta y no se puede cambiar desde aquí.",
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted,
            )
        }

        CampoEtiquetado("Objetivo") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AddictionType.entries.forEach { tipo ->
                    ChipObjetivo(
                        tipo = tipo,
                        seleccionado = objetivo == tipo,
                        onClick = { objetivo = tipo },
                    )
                }
            }
        }

        CampoEtiquetado("Fecha de inicio") {
            OutlinedTextField(
                value = fechaTexto,
                onValueChange = { },
                readOnly = true,
                leadingIcon = { Icon(AppIcons.Calendario, null, tint = InkMuted) },
                trailingIcon = {
                    Icon(
                        AppIcons.Calendario,
                        contentDescription = "Elegir fecha",
                        tint = BrandPurple,
                        modifier = Modifier.pressable(
                            onClick = { state.notify("La fecha de inicio se ajusta desde Sobriedad") },
                        ),
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = coloresDeCampo(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onVolver,
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Text("Cancelar")
            }
            Button(
                onClick = {
                    state.updateProfile { it.copy(displayName = nombre.trim(), addiction = objetivo) }
                    state.notify("Cambios guardados")
                    onVolver()
                },
                enabled = nombre.isNotBlank(),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Icon(AppIcons.Guardar, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Guardar cambios")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CampoEtiquetado(etiqueta: String, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = InkStrong,
            fontWeight = FontWeight.SemiBold,
        )
        contenido()
    }
}

/**
 * Pastilla de objetivo.
 *
 * El seleccionado se marca con fondo azul hielo, borde e icono en púrpura: se distingue por
 * color Y por peso de borde, no solo por color, para que se note también en una pantalla
 * mal calibrada o a plena luz.
 */
@Composable
private fun ChipObjetivo(tipo: AddictionType, seleccionado: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (seleccionado) Color(0xFFEEF2FF) else Color.Transparent,
        modifier = Modifier
            .border(
                width = if (seleccionado) 1.5.dp else 1.dp,
                color = if (seleccionado) BrandPurple else HairlineLight,
                shape = CircleShape,
            )
            .pressable(onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                tipo.icon,
                contentDescription = null,
                tint = if (seleccionado) BrandPurple else InkMuted,
                modifier = Modifier.size(16.dp),
            )
            Text(
                tipo.title,
                style = MaterialTheme.typography.labelLarge,
                color = if (seleccionado) BrandPurple else InkMuted,
            )
        }
    }
}

@Composable
private fun coloresDeCampo() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White,
    focusedBorderColor = BrandPurple,
    unfocusedBorderColor = HairlineLight,
    disabledBorderColor = HairlineLight,
    focusedTextColor = InkStrong,
    unfocusedTextColor = InkStrong,
    disabledTextColor = InkStrong,
)

private fun dosDigitos(valor: Int): String = if (valor < 10) "0$valor" else valor.toString()
