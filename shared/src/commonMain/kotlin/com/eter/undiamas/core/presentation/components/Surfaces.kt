package com.eter.undiamas.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Tarjeta con fondo degradado, para los bloques destacados de cada pantalla. */
@Composable
fun GradientCard(
    brush: Brush,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Surface(
        shape = shape,
        color = Color.Transparent,
        contentColor = contentColor,
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.pressable(onClick) else it },
    ) {
        Column(
            modifier = Modifier.background(brush, shape).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/** Tarjeta plana sobre SurfaceDark, con presión elástica opcional. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.pressable(onClick) else it },
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Cuadro de acceso rápido con su propio color de acento. */
@Composable
fun ActionTile(
    emoji: String,
    title: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.15f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.pressable(onClick),
    ) {
        Column(
            modifier = Modifier.heightIn(min = 96.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Text(title, style = MaterialTheme.typography.titleMedium, color = accent)
        }
    }
}

/** Métrica compacta: etiqueta pequeña arriba, cifra grande abajo. */
@Composable
fun StatBlock(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = accent)
        OdometerText(value, style = MaterialTheme.typography.headlineSmall)
    }
}
