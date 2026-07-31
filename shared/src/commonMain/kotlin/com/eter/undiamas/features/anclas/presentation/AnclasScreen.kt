package com.eter.undiamas.features.anclas.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.GuardedBack
import com.eter.undiamas.core.presentation.components.InterceptBack
import com.eter.undiamas.core.presentation.components.SectionHeaderLarge
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.icon
import com.eter.undiamas.core.presentation.theme.AccentAsistente
import com.eter.undiamas.core.presentation.theme.AccentPerfil
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.theme.PrimaryVioletStart
import com.eter.undiamas.core.presentation.theme.RiskGreen
import com.eter.undiamas.core.presentation.theme.SavingsGoldEnd
import com.eter.undiamas.features.anclas.domain.Anchor
import com.eter.undiamas.features.anclas.domain.AnchorKind

private val anchorColors = listOf(PrimaryVioletStart, RiskGreen, SavingsGoldEnd, AccentPerfil, AccentAsistente)

/** Alturas alternadas para que el mosaico no quede uniforme. */
private val tileHeights = listOf(150, 200, 170, 230, 185)

private fun colorFor(anchor: Anchor): Color = anchorColors[anchor.tileSeed % anchorColors.size]
private fun heightFor(anchor: Anchor): Int = tileHeights[anchor.tileSeed % tileHeights.size]

@Composable
fun AnclasScreen(state: AppState) {
    var creating by remember { mutableStateOf(false) }
    var fullScreen by remember { mutableStateOf<Anchor?>(null) }

    // Atrás en pantalla completa vuelve al mosaico; no sale de la sección.
    fullScreen?.let { anchor ->
        InterceptBack { fullScreen = null }
        AnchorFullScreen(
            anchor = anchor,
            onClose = { fullScreen = null },
            onDelete = {
                state.removeAnchor(anchor.id)
                fullScreen = null
                state.notify("Ancla eliminada")
            },
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeaderLarge(AppIcons.Ancla, "Muro de anclas")
        Text(
            "Lo que no quieres perder. Míralo cuando el impulso apriete.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = { creating = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPerfil),
        ) {
            Icon(AppIcons.Ancla, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  Agregar un ancla")
        }

        if (state.anchors.isEmpty()) {
            EmptyAnchors()
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(state.anchors, key = { it.id }) { anchor ->
                AnchorTile(anchor) { fullScreen = anchor }
            }
        }
    }

    if (creating) {
        AnchorCreator(
            onDismiss = { creating = false },
            onSave = { title, note, kind ->
                state.addAnchor(title, note, kind)
                state.notify("Ancla agregada")
                creating = false
            },
        )
    }
}

@Composable
private fun AnchorTile(anchor: Anchor, onClick: () -> Unit) {
    val accent = colorFor(anchor)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightFor(anchor).dp)
            .background(
                Brush.linearGradient(listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.45f))),
                RoundedCornerShape(20.dp),
            )
            .pressable(onClick),
    ) {
        // Velo oscuro inferior: garantiza que el texto blanco se lea sobre cualquier color.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))),
                    RoundedCornerShape(20.dp),
                ),
        )
        Icon(
            anchor.kind.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(16.dp).size(26.dp).align(Alignment.TopStart),
        )
        Column(
            modifier = Modifier.padding(16.dp).align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(anchor.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                anchor.kind.label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun AnchorFullScreen(anchor: Anchor, onClose: () -> Unit, onDelete: () -> Unit) {
    val accent = colorFor(anchor)
    var confirmDelete by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.9f), Color.Black))),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    anchor.kind.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    anchor.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                Text(
                    anchor.note,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp).align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                    Text("Volver", color = Color.White)
                }
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) {
                    Text("Eliminar", color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar esta ancla?") },
            text = { Text("Dejará de aparecer en tu muro.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun EmptyAnchors() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            AppIcons.Ancla,
            contentDescription = null,
            tint = AccentPerfil,
            modifier = Modifier.size(48.dp),
        )
        Text("Tu muro está vacío", style = MaterialTheme.typography.titleMedium)
        Text(
            "Agrega a las personas, metas o lugares por los que empezaste este camino.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AnchorCreator(onDismiss: () -> Unit, onSave: (String, String, AnchorKind) -> Unit) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(AnchorKind.PERSONA) }
    val hasContent = title.isNotBlank() || note.isNotBlank()

    GuardedBack(
        enabled = hasContent,
        title = "¿Salir sin guardar?",
        message = "Perderás el ancla que estabas creando.",
        confirmLabel = "Descartar",
        onConfirmExit = onDismiss,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva ancla") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("¿Quién o qué es?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("¿Por qué te sostiene?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    minLines = 3,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnchorKind.entries.take(3).forEach { option ->
                        KindChip(option, option == kind) { kind = option }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnchorKind.entries.drop(3).forEach { option ->
                        KindChip(option, option == kind) { kind = option }
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = title.isNotBlank(), onClick = { onSave(title, note, kind) }) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun KindChip(kind: AnchorKind, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) AccentPerfil.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.pressable(onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(kind.icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(kind.label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
