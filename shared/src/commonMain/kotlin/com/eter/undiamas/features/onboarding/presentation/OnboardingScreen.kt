package com.eter.undiamas.features.onboarding.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.GradientCard
import com.eter.undiamas.core.presentation.theme.PrimaryVioletBrush
import com.eter.undiamas.core.presentation.theme.PrimaryVioletStart
import com.eter.undiamas.core.presentation.theme.AppIcons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import com.eter.undiamas.core.domain.model.AddictionType
import com.eter.undiamas.core.domain.model.SupportRole
import com.eter.undiamas.core.domain.model.Trigger
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.icon

/**
 * Las cinco primeras preguntas son las que el contador necesita para existir; el resto
 * afinan el acompañamiento y se pueden saltar.
 *
 * La division importa. Quien crea la cuenta suele estar en un mal momento, y un
 * cuestionario de diez pantallas obligatorias es una forma eficaz de que cierre la app
 * antes de llegar al final. Lo que no se conteste ahora se puede llenar despues desde
 * Perfil, y la app funciona igual sin ello.
 */
private const val PASOS_ESENCIALES = 5
private const val TOTAL_STEPS = 10

@Composable
fun OnboardingScreen(state: AppState, onFinishOnboarding: () -> Unit = {}) {
    var step by remember { mutableStateOf(0) }

    var name by remember { mutableStateOf("") }
    var daysSober by remember { mutableStateOf("0") }
    var recordDays by remember { mutableStateOf("0") }
    var dailyExpense by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var addiction by remember { mutableStateOf<AddictionType?>(null) }
    var porQue by remember { mutableStateOf("") }
    var metaTitulo by remember { mutableStateOf("") }
    var metaMonto by remember { mutableStateOf("") }
    var detonantes by remember { mutableStateOf(emptySet<Trigger>()) }
    var contactRole by remember { mutableStateOf(SupportRole.FAMILIAR) }
    var horaRecordatorio by remember { mutableStateOf(21) }
    var quiereRecordatorio by remember { mutableStateOf(true) }

    // Solo las esenciales bloquean. Las demas se pueden dejar en blanco y seguir.
    val canAdvance = when (step) {
        0 -> addiction != null
        1 -> name.isNotBlank()
        2 -> daysSober.toLongOrNull() != null
        3 -> recordDays.toLongOrNull() != null
        4 -> dailyExpense.toDoubleOrNull() != null
        else -> true
    }
    val esOpcional = step >= PASOS_ESENCIALES

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        GradientCard(brush = PrimaryVioletBrush) {
            Text("BIENVENIDO/A A UN DÍA MÁS", style = MaterialTheme.typography.labelMedium)
            Text(
                "Cuéntanos un poco de ti para acompañarte mejor",
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        // Progreso por segmentos, uno por pregunta.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(TOTAL_STEPS) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(
                            if (index <= step) PrimaryVioletStart else PrimaryVioletStart.copy(alpha = 0.18f),
                            RoundedCornerShape(3.dp),
                        ),
                )
            }
        }
        Text("PASO ${step + 1} DE $TOTAL_STEPS", style = MaterialTheme.typography.labelMedium, color = PrimaryVioletStart)

        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding-step",
        ) { current ->
            when (current) {
                0 -> QuestionStep(
                    icon = AppIcons.Escudo,
                    question = "¿Qué estás intentando dejar?",
                    hint = "Desliza para ver las opciones. Esto personaliza tus mensajes y tus recordatorios.",
                ) {
                    AddictionCarousel(selected = addiction, onSelect = { addiction = it })
                }

                1 -> QuestionStep(
                    icon = AppIcons.Perfil,
                    question = "¿Cómo quieres que te llamemos?",
                    hint = "Puede ser tu nombre o un apodo.",
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tu nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                }

                2 -> QuestionStep(
                    icon = AppIcons.Racha,
                    question = "¿Cuántos días llevas sobrio/a?",
                    hint = "Si empiezas hoy, deja 0. El contador arranca desde ahí.",
                ) {
                    NumberField(daysSober, { daysSober = it }, "Días")
                }

                3 -> QuestionStep(
                    icon = AppIcons.Record,
                    question = "¿Cuál es tu récord anterior?",
                    hint = "Tu mejor racha hasta hoy, en días. Si es la primera vez, deja 0.",
                ) {
                    NumberField(recordDays, { recordDays = it }, "Días de récord")
                }

                4 -> QuestionStep(
                    icon = AppIcons.Ahorro,
                    question = "¿Cuánto gastabas al día?",
                    hint = "Nos sirve para calcular cuánto llevas ahorrado.",
                ) {
                    NumberField(dailyExpense, { dailyExpense = it }, "Gasto diario (MXN)", decimal = true)
                }

                5 -> QuestionStep(
                    icon = AppIcons.Ancla,
                    question = "¿Por qué lo estás haciendo?",
                    hint = "Escríbelo con tus palabras. Es lo que te vamos a recordar los días difíciles, así que no lo hagas bonito: hazlo tuyo.",
                ) {
                    OutlinedTextField(
                        value = porQue,
                        onValueChange = { porQue = it },
                        label = { Text("Mi por qué") },
                        placeholder = { Text("Por mi hija. Para volver a dormir bien.") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                }

                6 -> QuestionStep(
                    icon = AppIcons.Ahorro,
                    question = "¿Para qué quieres ese dinero?",
                    hint = "Ponerle nombre a lo que estás ahorrando lo vuelve algo que se acerca, en vez de una cifra que sube.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = metaTitulo,
                            onValueChange = { metaTitulo = it },
                            label = { Text("Mi meta") },
                            placeholder = { Text("Un viaje con mi hermana") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        )
                        NumberField(metaMonto, { metaMonto = it }, "Cuánto cuesta (MXN)", decimal = true)
                    }
                }

                7 -> QuestionStep(
                    icon = AppIcons.Semaforo,
                    question = "¿Qué suele ponerte en riesgo?",
                    hint = "Marca los que reconozcas. Aparecerán primero cuando hagas un check-in, para que no tengas que buscarlos justo cuando peor estás.",
                ) {
                    FichasDeDetonante(
                        seleccionados = detonantes,
                        onCambiar = { detonantes = it },
                    )
                }

                8 -> QuestionStep(
                    icon = AppIcons.Red,
                    question = "¿A quién llamamos si estás en riesgo?",
                    hint = "Tu contacto de confianza aparecerá en el protocolo de emergencia. Puedes dejarlo en blanco y agregarlo después.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Nombre del contacto") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        )
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Teléfono") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        )
                        Text(
                            "¿Quién es para ti?",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FichasDeRol(seleccionado = contactRole, onSeleccionar = { contactRole = it })
                    }
                }

                else -> QuestionStep(
                    icon = AppIcons.Notificaciones,
                    question = "¿A qué hora te viene bien que te escribamos?",
                    hint = "Un recordatorio al día para registrar cómo vas. Si tu semáforo se pone en amarillo o rojo, te escribiremos más seguido. Al terminar, tu teléfono te pedirá permiso para avisarte.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SelectorDeHora(
                            seleccionada = horaRecordatorio,
                            activo = quiereRecordatorio,
                            onSeleccionar = { horaRecordatorio = it; quiereRecordatorio = true },
                        )
                        TextButton(
                            onClick = { quiereRecordatorio = !quiereRecordatorio },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (quiereRecordatorio) {
                                    "Prefiero que no me escriban a diario"
                                } else {
                                    "Sí quiero un recordatorio diario"
                                },
                            )
                        }
                    }
                }
            }
        }

        Button(
            enabled = canAdvance,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (step < TOTAL_STEPS - 1) {
                    step += 1
                } else {
                    state.completeOnboarding(
                        displayName = name,
                        daysSober = daysSober.toLongOrNull() ?: 0,
                        recordDays = recordDays.toLongOrNull() ?: 0,
                        previousDailyExpense = dailyExpense.toDoubleOrNull() ?: 0.0,
                        contactName = contactName,
                        contactPhone = contactPhone,
                        contactRole = contactRole,
                        addiction = addiction,
                        personalWhy = porQue,
                        savingsGoalTitle = metaTitulo,
                        savingsGoalAmount = metaMonto.toDoubleOrNull(),
                        habitualTriggers = detonantes.toList(),
                        wantsDailyReminder = quiereRecordatorio,
                        reminderHour = horaRecordatorio,
                    )
                    onFinishOnboarding()
                }
            },
        ) {
            Text(if (step < TOTAL_STEPS - 1) "Continuar" else "Empezar mi primer día")
        }

        // Salir del cuestionario sin contestar lo opcional tiene que ser una opción visible,
        // no algo que se descubra dejando campos vacíos.
        if (esOpcional && step < TOTAL_STEPS - 1) {
            TextButton(
                onClick = { step = TOTAL_STEPS - 1 },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Prefiero contestar esto después") }
        }

        if (step > 0) {
            TextButton(onClick = { step -= 1 }, modifier = Modifier.fillMaxWidth()) {
                Text("Atrás")
            }
        }
    }
}

/** Ficha redonda seleccionable, el patrón que ya usan el check-in y el diálogo de recaída. */
@Composable
private fun Ficha(
    etiqueta: String,
    activa: Boolean,
    icono: ImageVector? = null,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (activa) {
            PrimaryVioletStart.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (activa) 1.5.dp else 1.dp,
            color = if (activa) PrimaryVioletStart else MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.pressable(onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icono != null) {
                Icon(
                    icono,
                    contentDescription = null,
                    tint = if (activa) PrimaryVioletStart else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelLarge,
                color = if (activa) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FichasDeDetonante(seleccionados: Set<Trigger>, onCambiar: (Set<Trigger>) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Trigger.entries.forEach { detonante ->
            val activa = detonante in seleccionados
            Ficha(
                etiqueta = detonante.label,
                activa = activa,
                icono = detonante.icon,
                onClick = {
                    onCambiar(
                        if (activa) seleccionados - detonante else seleccionados + detonante,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FichasDeRol(seleccionado: SupportRole, onSeleccionar: (SupportRole) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        SupportRole.entries.forEach { rol ->
            Ficha(
                etiqueta = rol.label,
                activa = rol == seleccionado,
                onClick = { onSeleccionar(rol) },
            )
        }
    }
}

/**
 * Horas en bloques de tres, las mismas que ofrece Configuracion.
 *
 * Un reloj completo seria mas preciso y peor: aqui la pregunta es "por la mañana o por la
 * noche", y elegir entre seis opciones se hace de un vistazo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorDeHora(seleccionada: Int, activo: Boolean, onSeleccionar: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        listOf(6, 9, 12, 15, 18, 21).forEach { hora ->
            Ficha(
                etiqueta = if (hora < 10) "0$hora:00" else "$hora:00",
                activa = activo && hora == seleccionada,
                onClick = { onSeleccionar(hora) },
            )
        }
    }
}

@Composable
private fun QuestionStep(
    icon: ImageVector,
    question: String,
    hint: String,
    field: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            icon,
            contentDescription = null,
            tint = PrimaryVioletStart,
            modifier = Modifier.size(44.dp),
        )
        Text(question, style = MaterialTheme.typography.headlineSmall)
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        field()
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    decimal: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Solo dígitos (y un punto cuando aplica) para que el campo no acepte basura.
            val filtered = input.filter { it.isDigit() || (decimal && it == '.') }
            onValueChange(filtered)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    )
}
