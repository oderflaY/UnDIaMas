package com.eter.undiamas.features.onboarding.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.AddictionType
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.icon
import com.eter.undiamas.core.presentation.theme.PrimaryVioletEnd
import com.eter.undiamas.core.presentation.theme.PrimaryVioletStart
import kotlin.math.absoluteValue

/**
 * Carrusel de selección de adicción: una tarjeta grande por opción, con icono vectorial,
 * y las tarjetas vecinas encogidas para que se lea de un vistazo que hay más deslizando.
 */
@Composable
fun AddictionCarousel(
    selected: AddictionType?,
    onSelect: (AddictionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = AddictionType.entries
    val pagerState = rememberPagerState(
        initialPage = selected?.ordinal ?: 0,
        pageCount = { options.size },
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            // El padding lateral deja asomar las tarjetas vecinas: la pista visual de que se desliza.
            contentPadding = PaddingValues(horizontal = 48.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val option = options[page]
            val distance = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction)
                .absoluteValue
                .coerceIn(0f, 1f)
            val scale by animateFloatAsState(
                targetValue = 1f - distance * 0.12f,
                animationSpec = tween(220),
                label = "card-scale",
            )

            AddictionCard(
                option = option,
                isSelected = option == selected,
                modifier = Modifier.scale(scale),
                onSelect = { onSelect(option) },
            )
        }

        PageIndicator(
            pageCount = options.size,
            currentPage = pagerState.currentPage,
        )
    }

    // Si ya había una selección previa, el carrusel abre en ella.
    LaunchedEffect(selected) {
        selected?.let { if (pagerState.currentPage != it.ordinal) pagerState.animateScrollToPage(it.ordinal) }
    }
}

@Composable
private fun AddictionCard(
    option: AddictionType,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth().height(340.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isSelected) {
                        Modifier.background(
                            Brush.linearGradient(listOf(PrimaryVioletStart, PrimaryVioletEnd)),
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.18f) else PrimaryVioletStart.copy(alpha = 0.14f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    option.icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else PrimaryVioletStart,
                    modifier = Modifier.size(60.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    option.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    option.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) {
                        Color.White.copy(alpha = 0.85f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color.White else PrimaryVioletStart,
                    contentColor = if (isSelected) PrimaryVioletEnd else Color.White,
                ),
            ) {
                Text(if (isSelected) "Seleccionada" else "Seleccionar esta")
            }
        }
    }
}

/** Puntos bajo el carrusel; el activo se ensancha en vez de solo cambiar de color. */
@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(pageCount) { index ->
            val active = index == currentPage
            val width by animateFloatAsState(
                targetValue = if (active) 22f else 8f,
                animationSpec = tween(220),
                label = "dot-width",
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .size(width = width.dp, height = 8.dp)
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                        CircleShape,
                    ),
            )
        }
    }
}
