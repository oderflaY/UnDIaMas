package com.eter.undiamas.features.inicio.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eter.undiamas.core.domain.model.Mood
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.components.shake
import com.eter.undiamas.core.presentation.icon
import com.eter.undiamas.core.presentation.formatClock
import com.eter.undiamas.core.presentation.greetingForHour
import com.eter.undiamas.core.presentation.rememberNow
import com.eter.undiamas.core.presentation.theme.AccentAsistente
import com.eter.undiamas.core.presentation.theme.AccentCream
import com.eter.undiamas.core.presentation.theme.AccentDiario
import com.eter.undiamas.core.presentation.theme.AccentPerfil
import com.eter.undiamas.core.presentation.theme.AccentOrange
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.theme.BrandPurple
import com.eter.undiamas.core.presentation.theme.HairlineLight
import com.eter.undiamas.core.presentation.theme.InkMuted
import com.eter.undiamas.core.presentation.theme.InkStrong
import com.eter.undiamas.core.presentation.theme.PrimaryVioletBrush
import com.eter.undiamas.core.presentation.theme.RiskGreen
import com.eter.undiamas.core.presentation.theme.RiskRed
import com.eter.undiamas.features.sobriedad.presentation.EnlaceDeRecaida
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val SEGUNDOS_POR_DIA = 86_400L

/** Frases del día. Cortas y sin promesas: nadie quiere un sermón al abrir la app. */
private val FRASES = listOf(
    "El progreso, por pequeño que sea, siempre vale la pena.",
    "Hoy no tienes que resolverlo todo. Solo hoy.",
    "Lo difícil de sostener también cuenta como avanzar.",
    "Volver a empezar no borra lo que ya aprendiste.",
)

/**
 * Pantalla de inicio.
 *
 * Se lee de arriba abajo en orden de urgencia: primero la racha, que es el dato por el que
 * la mayoría abre la app; después el botón de completar el día, que es la única acción
 * importante; y al final el contexto. Todo sale de la copia local, así que se ve igual con
 * o sin conexión.
 */
@Composable
fun DashboardScreen(state: AppState, navigator: Navigator) {
    val now by rememberNow()
    val zona = TimeZone.currentSystemDefault()
    val hoy = now.toLocalDateTime(zona).date

    // La racha la calcula el servidor cuando hay conexión; sin ella se cae al cálculo local
    // sobre la fecha de inicio. El local avanza cada segundo con `rememberNow`, así que el
    // contador está vivo en los dos casos.
    val rachaLocal = state.sobrietyCounter.currentStreakSeconds(state.profile, now)
    val rachaSegundos = maxOf(state.streakSeconds, rachaLocal)
    val recordSegundos = maxOf(
        state.sobrietyCounter.recordStreakSeconds(state.profile, now),
        rachaSegundos,
    )
    val dias = rachaSegundos / SEGUNDOS_POR_DIA
    val record = recordSegundos / SEGUNDOS_POR_DIA
    val esRecord = rachaSegundos >= recordSegundos && recordSegundos > 0
    // Cuánto falta para batir el récord. Con récord ya superado, el anillo se llena entero.
    val avanceHaciaRecord = if (recordSegundos > 0) {
        (rachaSegundos.toFloat() / recordSegundos).coerceIn(0f, 1f)
    } else {
        0f
    }

    val porDia = state.checkInHistory.byDay(state.checkIns, zona)
    val completadosHoy = porDia.containsKey(hoy)
    val nivelDeHoy = porDia[hoy]
    // El banner de emergencia solo llama la atención cuando el semáforo de HOY lo justifica.
    val enRiesgo = nivelDeHoy == RiskLevel.AMARILLO || nivelDeHoy == RiskLevel.ROJO
    val animoDeHoy = state.moodEntries.firstOrNull {
        it.registeredAt.toLocalDateTime(zona).date == hoy
    }?.mood
    val totalCompletados = porDia.size
    val frase = FRASES[(hoy.toEpochDays() % FRASES.size).toInt()]

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { TarjetaPrincipal(state, rachaSegundos, dias, record, esRecord, avanceHaciaRecord) }

        item {
            SemanaActual(
                state = state,
                zona = zona,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        item {
            Button(
                onClick = { navigator.goTo(Screen.CheckIn) },
                enabled = !completadosHoy,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(AppIcons.Hecho, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (completadosHoy) "Hoy ya está completado" else "Hoy completé mi día",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        // Justo debajo de "completé mi día": las dos cosas que puede haber pasado hoy,
        // una al lado de la otra, sin que ninguna esté escondida.
        item {
            EnlaceDeRecaida(state, modifier = Modifier.padding(horizontal = 20.dp))
        }

        item {
            SelectorDeAnimo(
                seleccionado = animoDeHoy,
                modifier = Modifier.padding(horizontal = 20.dp),
                onSelect = { animo ->
                    state.registerMood(animo)
                    state.notify("Ánimo registrado: ${animo.label}")
                },
            )
        }

        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = AccentCream,
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            AppIcons.Frase,
                            contentDescription = null,
                            tint = AccentOrange,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            "Frase del día",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentOrange,
                        )
                    }
                    Text(
                        frase,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = InkStrong,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                WidgetDato(
                    icono = AppIcons.Calendario,
                    tinte = BrandPurple,
                    valor = totalCompletados.toString(),
                    etiqueta = "Completados",
                    modifier = Modifier.weight(1f),
                    onClick = { navigator.goTo(Screen.Estadisticas) },
                )
                WidgetDato(
                    icono = AppIcons.Record,
                    tinte = AccentOrange,
                    valor = record.toString(),
                    etiqueta = "días · Mejor racha",
                    modifier = Modifier.weight(1f),
                    onClick = { navigator.goTo(Screen.Sobriedad) },
                )
            }
        }

        item {
            Text(
                "TUS HERRAMIENTAS",
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        item { RejillaDeHerramientas(navigator, Modifier.padding(horizontal = 20.dp)) }

        item {
            BannerDeEmergencia(
                enRiesgo = enRiesgo,
                modifier = Modifier.padding(horizontal = 20.dp),
                onClick = { navigator.goTo(Screen.Emergencia) },
            )
        }
    }
}

/**
 * Registro rápido de ánimo.
 *
 * Cinco toques posibles y ninguna pregunta: es la forma más barata de dejar constancia de
 * un mal día para quien no tiene fuerzas de rellenar el check-in completo.
 */
@Composable
private fun SelectorDeAnimo(
    seleccionado: Mood?,
    modifier: Modifier = Modifier,
    onSelect: (Mood) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "¿CÓMO ESTÁS HOY?",
            style = MaterialTheme.typography.labelSmall,
            color = InkMuted,
            letterSpacing = 1.2.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Mood.entries.forEach { animo ->
                val activo = animo == seleccionado
                Surface(
                    shape = MaterialTheme.shapes.medium,
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
                    modifier = Modifier.pressable({ onSelect(animo) }),
                ) {
                    Icon(
                        animo.icon,
                        contentDescription = animo.label,
                        tint = if (activo) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            InkMuted
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp).size(26.dp),
                    )
                }
            }
        }
    }
}

/**
 * Todas las herramientas de la app en un sitio.
 *
 * Están aquí porque el inicio es la única pantalla por la que pasa todo el mundo: sin esta
 * rejilla, las anclas o las cápsulas existirían en el código y no en la app de nadie.
 */
@Composable
private fun RejillaDeHerramientas(navigator: Navigator, modifier: Modifier = Modifier) {
    val herramientas = listOf(
        Herramienta(AppIcons.Record, "Mi racha", AccentOrange, Screen.Sobriedad),
        Herramienta(AppIcons.Ahorro, "Mi ahorro", RiskGreen, Screen.Calculadora),
        Herramienta(AppIcons.Asistente, "Asistente", AccentAsistente, Screen.Ia),
        Herramienta(AppIcons.Habitos, "Hábitos", RiskGreen, Screen.Habitos),
        Herramienta(AppIcons.Ancla, "Mis anclas", AccentPerfil, Screen.Anclas),
        Herramienta(AppIcons.Capsula, "Cápsulas", BrandPurple, Screen.Capsulas),
        Herramienta(AppIcons.Diario, "Diario", AccentDiario, Screen.Diario),
        Herramienta(AppIcons.Corazon, "Biometría", RiskRed, Screen.Biometria),
        Herramienta(AppIcons.Grupo, "Comunidad", AccentAsistente, Screen.Comunidad),
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        herramientas.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                fila.forEach { herramienta ->
                    FichaDeHerramienta(
                        herramienta = herramienta,
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.goTo(herramienta.destino) },
                    )
                }
                // Si la última fila queda coja, el hueco se rellena para que la ficha
                // suelta no se estire al doble de ancho que las demás.
                if (fila.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class Herramienta(
    val icono: androidx.compose.ui.graphics.vector.ImageVector,
    val titulo: String,
    val acento: Color,
    val destino: Screen,
)

@Composable
private fun FichaDeHerramienta(
    herramienta: Herramienta,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier.pressable(onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(herramienta.acento.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    herramienta.icono,
                    contentDescription = null,
                    tint = herramienta.acento,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                herramienta.titulo,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Acceso al protocolo de emergencia.
 *
 * Siempre visible, no solo en rojo: quien lo necesita no está para buscarlo en un menú.
 * Cuando el semáforo del día está en amarillo o rojo, además tiembla al aparecer.
 */
@Composable
private fun BannerDeEmergencia(
    enRiesgo: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = RiskRed.copy(alpha = 0.10f),
        border = BorderStroke(1.5.dp, RiskRed.copy(alpha = if (enRiesgo) 0.85f else 0.4f)),
        modifier = modifier.fillMaxWidth().shake(enRiesgo).pressable(onClick),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                AppIcons.Emergencia,
                contentDescription = null,
                tint = RiskRed,
                modifier = Modifier.size(28.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Necesito ayuda ahora",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (enRiesgo) {
                        "Tu semáforo de hoy pide atención. Estamos aquí."
                    } else {
                        "Respiración guiada, anclas y tu contacto de confianza."
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                )
            }
        }
    }
}

/**
 * Tarjeta superior con degradado.
 *
 * Ocupa la parte alta y se curva solo por abajo: así se lee como una continuación de la
 * barra de estado y no como una tarjeta suelta.
 */
@Composable
private fun TarjetaPrincipal(
    state: AppState,
    rachaSegundos: Long,
    dias: Long,
    record: Long,
    esRecord: Boolean,
    avanceHaciaRecord: Float,
) {
    val ahora by rememberNow()
    val hora = ahora.toLocalDateTime(TimeZone.currentSystemDefault()).hour
    val nombre = state.profile.displayName.ifBlank { "qué bueno verte" }
    val inicial = state.profile.displayName.trim().firstOrNull()?.uppercase() ?: "·"

    // El anillo se anima al cambiar en vez de saltar: el progreso hacia un récord de meses
    // avanza tan despacio que sin transición parecería roto.
    val avance by animateFloatAsState(
        targetValue = avanceHaciaRecord,
        animationSpec = tween(durationMillis = 900),
        label = "avance",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = PrimaryVioletBrush,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            )
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${greetingForHour(hora)}, $nombre",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.24f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        inicial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Anillo con los días dentro: el número grande y su avance hacia el récord
                // en la misma figura, para no tener que leer dos sitios.
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(132.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val grosor = 10.dp.toPx()
                        val diametro = size.minDimension - grosor
                        val esquina = Offset(
                            (size.width - diametro) / 2f,
                            (size.height - diametro) / 2f,
                        )
                        drawArc(
                            color = Color.White.copy(alpha = 0.22f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = esquina,
                            size = Size(diametro, diametro),
                            style = Stroke(width = grosor, cap = StrokeCap.Round),
                        )
                        if (avance > 0f) {
                            drawArc(
                                color = Color.White,
                                startAngle = -90f,
                                sweepAngle = 360f * avance,
                                useCenter = false,
                                topLeft = esquina,
                                size = Size(diametro, diametro),
                                style = Stroke(width = grosor, cap = StrokeCap.Round),
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            dias.toString(),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            if (dias == 1L) "día" else "días",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Reloj vivo: avanza cada segundo. Es lo que hace que la racha se sienta
                    // en marcha y no como un número que alguien escribió una vez.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            AppIcons.Racha,
                            contentDescription = null,
                            tint = AccentOrange,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            formatClock(rachaSegundos),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            AppIcons.Record,
                            contentDescription = null,
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                "$record ${if (record == 1L) "día" else "días"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            Text(
                                if (esRecord) "Tu mejor racha, ahora mismo" else "Mejor racha",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Metrica(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    tinteIcono: Color,
    valor: String,
    etiqueta: String,
    tamanoValor: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icono, contentDescription = null, tint = tinteIcono, modifier = Modifier.size(26.dp))
        Text(
            valor,
            fontSize = tamanoValor,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.82f),
        )
    }
}

/**
 * Los siete días de la semana en curso.
 *
 * Un día se marca cuando hay check-in registrado. Los futuros salen vacíos con su inicial:
 * no son fallos, es que todavía no han pasado, y marcarlos en rojo sería una regañina por
 * algo que nadie ha hecho.
 */
@Composable
private fun SemanaActual(state: AppState, zona: TimeZone, modifier: Modifier = Modifier) {
    val now by rememberNow()
    val hoy = now.toLocalDateTime(zona).date
    val porDia = state.checkInHistory.byDay(state.checkIns, zona)

    // Lunes de esta semana: `isoDayNumber` va de 1 (lunes) a 7 (domingo).
    val lunes = hoy.minus(
        DatePeriod(days = hoy.dayOfWeek.isoDayNumber - 1),
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "ESTA SEMANA",
            style = MaterialTheme.typography.labelSmall,
            color = InkMuted,
            letterSpacing = 1.2.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            repeat(7) { indice ->
                val dia = lunes.plus(DatePeriod(days = indice))
                val completado = porDia.containsKey(dia)
                val futuro = dia > hoy

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = if (completado) BrandPurple else Color.White,
                                shape = CircleShape,
                            )
                            .border(
                                width = if (completado) 0.dp else 1.dp,
                                color = if (completado) Color.Transparent else HairlineLight,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (completado) {
                            Icon(
                                AppIcons.Hecho,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        } else {
                            Text(
                                inicialDe(dia.dayOfWeek),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (futuro) InkMuted.copy(alpha = 0.6f) else InkMuted,
                            )
                        }
                    }
                    Text(
                        inicialDe(dia.dayOfWeek),
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetDato(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    tinte: Color,
    valor: String,
    etiqueta: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = modifier.pressable(onClick),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icono, contentDescription = null, tint = tinte, modifier = Modifier.size(24.dp))
            Text(
                valor,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = InkStrong,
            )
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
                textAlign = TextAlign.Start,
            )
        }
    }
}

private fun inicialDe(dia: DayOfWeek): String = when (dia) {
    DayOfWeek.MONDAY -> "L"
    DayOfWeek.TUESDAY -> "M"
    DayOfWeek.WEDNESDAY -> "X"
    DayOfWeek.THURSDAY -> "J"
    DayOfWeek.FRIDAY -> "V"
    DayOfWeek.SATURDAY -> "S"
    else -> "D"
}
