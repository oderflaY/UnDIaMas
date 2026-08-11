package com.eter.undiamas.features.comunidad.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.icon
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.theme.InkMuted
import com.eter.undiamas.features.comunidad.domain.DIAS_MINIMOS_PARA_PUBLICAR
import com.eter.undiamas.features.comunidad.domain.Historia
import com.eter.undiamas.features.comunidad.domain.MotivoReporte
import com.eter.undiamas.features.comunidad.domain.OrdenHistorias

/**
 * Muro de la comunidad.
 *
 * Quien lleva más tiempo cuenta cómo lo hizo. El orden por defecto es por racha porque esa
 * es la promesa de la sección; los otros dos órdenes están para que no se convierta en un
 * podio fijo donde siempre se lee a la misma gente.
 */
@Composable
fun ComunidadScreen(state: AppState, navigator: Navigator) {
    val historias by state.comunidadHistorias
    val perfil by state.comunidadPerfil
    val cargando by state.comunidadCargando
    var orden by remember { mutableStateOf(OrdenHistorias.RACHA) }
    var historiaAReportar by remember { mutableStateOf<Historia?>(null) }

    LaunchedEffect(orden) { state.cargarComunidad(orden) }

    Column(modifier = Modifier.fillMaxWidth()) {
        FiltrosDeOrden(
            actual = orden,
            onCambiar = { orden = it },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )

        if (historias.isEmpty() && !cargando) {
            MuroVacio(
                puedePublicar = perfil.puedePublicar,
                diasDeRacha = perfil.diasDeRacha,
                onPublicar = { navigator.goTo(Screen.PublicarHistoria) },
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                TarjetaDeInvitacion(
                    perfil.puedePublicar,
                    perfil.diasDeRacha,
                    onPublicar = { navigator.goTo(Screen.PublicarHistoria) },
                )
            }

            items(historias, key = { it.id }) { historia ->
                TarjetaDeHistoria(
                    historia = historia,
                    onUtil = { state.marcarHistoriaUtil(historia) },
                    onReportar = { historiaAReportar = historia },
                    onBloquear = { state.bloquearAutor(historia) },
                    onBorrar = { state.borrarHistoria(historia) },
                )
            }

            if (cargando) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    }
                }
            } else if (state.comunidadHayMas.value) {
                item {
                    OutlinedButton(
                        onClick = { state.cargarMasComunidad(orden) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Ver más historias") }
                }
            }
        }
    }

    historiaAReportar?.let { historia ->
        DialogoDeReporte(
            onCerrar = { historiaAReportar = null },
            onReportar = { motivo ->
                state.reportarHistoria(historia, motivo)
                historiaAReportar = null
            },
        )
    }
}

@Composable
private fun FiltrosDeOrden(
    actual: OrdenHistorias,
    onCambiar: (OrdenHistorias) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OrdenHistorias.entries.forEach { opcion ->
            val activo = opcion == actual
            Surface(
                shape = CircleShape,
                color = if (activo) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = if (activo) 1.5.dp else 1.dp,
                    color = if (activo) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                ),
                modifier = Modifier.pressable({ onCambiar(opcion) }),
            ) {
                Text(
                    text = when (opcion) {
                        OrdenHistorias.RACHA -> "Más tiempo"
                        OrdenHistorias.RECIENTE -> "Recientes"
                        OrdenHistorias.UTILES -> "Más útiles"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (activo) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        InkMuted
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
        }
    }
}

@Composable
private fun TarjetaDeInvitacion(
    puedePublicar: Boolean,
    diasDeRacha: Long,
    onPublicar: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (puedePublicar) "Cuenta cómo lo estás haciendo" else "Tu historia también cuenta",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (puedePublicar) {
                    "Alguien que empieza hoy necesita leer justo lo que tú ya viviste."
                } else {
                    // Se dice cuánto falta, no solo que no puede: un "no" sin número se
                    // siente como un rechazo y no como un camino.
                    "Con $DIAS_MINIMOS_PARA_PUBLICAR días podrás publicar. " +
                        "Llevas $diasDeRacha."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (puedePublicar) {
                Button(onClick = onPublicar, modifier = Modifier.fillMaxWidth()) {
                    Icon(AppIcons.Editar, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Escribir mi historia")
                }
            }
        }
    }
}

@Composable
private fun TarjetaDeHistoria(
    historia: Historia,
    onUtil: () -> Unit,
    onReportar: () -> Unit,
    onBloquear: () -> Unit,
    onBorrar: () -> Unit,
) {
    var desplegada by remember { mutableStateOf(false) }
    var menuAbierto by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        historia.alias.trim().firstOrNull()?.uppercase() ?: "·",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(historia.alias, style = MaterialTheme.typography.titleSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // La racha solo aparece si el autor aceptó compartirla.
                        if (historia.diasDeRacha > 0) {
                            Icon(
                                AppIcons.Racha,
                                contentDescription = null,
                                tint = InkMuted,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                "${historia.diasDeRacha} días",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkMuted,
                            )
                        }
                        historia.objetivo?.let { objetivo ->
                            Icon(
                                objetivo.icon,
                                contentDescription = null,
                                tint = InkMuted,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                objetivo.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = InkMuted,
                            )
                        }
                    }
                }
                Icon(
                    AppIcons.Configuracion,
                    contentDescription = "Opciones",
                    tint = InkMuted,
                    modifier = Modifier.size(20.dp).pressable({ menuAbierto = !menuAbierto }),
                )
            }

            Text(
                historia.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                historia.cuerpo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (desplegada) Int.MAX_VALUE else 6,
            )
            if (!desplegada && historia.cuerpo.length > 300) {
                Text(
                    "Leer todo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.pressable({ desplegada = true }),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (historia.marcada) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.pressable(onUtil),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            AppIcons.PorQue,
                            contentDescription = null,
                            tint = if (historia.marcada) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                InkMuted
                            },
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            if (historia.utiles > 0) "Me ayudó · ${historia.utiles}" else "Me ayudó",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (historia.marcada) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                InkMuted
                            },
                        )
                    }
                }
            }

            if (menuAbierto) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (historia.esMia) {
                        TextButton(onClick = onBorrar) { Text("Borrar mi historia") }
                    } else {
                        TextButton(onClick = onReportar) { Text("Reportar") }
                        TextButton(onClick = onBloquear) { Text("No ver a esta persona") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MuroVacio(puedePublicar: Boolean, diasDeRacha: Long, onPublicar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            AppIcons.Grupo,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            "Todavía no hay historias",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            if (puedePublicar) {
                "Puedes ser quien empiece. Alguien que abre esto mañana te lo va a agradecer."
            } else {
                "Cuando alguien comparta su experiencia, la verás aquí. " +
                    "Con $DIAS_MINIMOS_PARA_PUBLICAR días tú también podrás escribir; llevas $diasDeRacha."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (puedePublicar) {
            Button(onClick = onPublicar) { Text("Escribir la primera") }
        }
    }
}

@Composable
private fun DialogoDeReporte(onCerrar: () -> Unit, onReportar: (MotivoReporte) -> Unit) {
    var motivo by remember { mutableStateOf<MotivoReporte?>(null) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("¿Qué pasa con esta historia?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "La revisaremos. Mientras tanto dejarás de verla.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MotivoReporte.entries.forEach { opcion ->
                    val activo = motivo == opcion
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (activo) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = if (activo) 1.5.dp else 1.dp,
                            color = if (activo) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        ),
                        modifier = Modifier.fillMaxWidth().pressable({ motivo = opcion }),
                    ) {
                        Text(
                            opcion.etiqueta,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { motivo?.let(onReportar) },
                enabled = motivo != null,
            ) { Text("Reportar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}
