package com.eter.undiamas

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.theme.UnDiaMasTheme
import com.eter.undiamas.core.presentation.theme.screenTransition
import com.eter.undiamas.features.calculadora.presentation.CalculadoraScreen
import com.eter.undiamas.features.checkin.presentation.CheckInScreen
import com.eter.undiamas.features.configuracion.presentation.ConfiguracionScreen
import com.eter.undiamas.features.diario.presentation.DiarioScreen
import com.eter.undiamas.features.emergencia.presentation.EmergenciaScreen
import com.eter.undiamas.features.estadisticas.presentation.EstadisticasScreen
import com.eter.undiamas.features.ia.presentation.IaScreen
import com.eter.undiamas.features.inicio.presentation.InicioScreen
import com.eter.undiamas.features.onboarding.presentation.OnboardingScreen
import com.eter.undiamas.features.perfil.presentation.PerfilScreen
import com.eter.undiamas.features.sobriedad.presentation.SobrietyScreen
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import com.eter.undiamas.core.presentation.diagnoseStartupError

private val bottomTabs = listOf(
    Screen.Inicio to "🏠",
    Screen.CheckIn to "✅",
    Screen.Diario to "📓",
    Screen.Estadisticas to "📊",
    Screen.Perfil to "🙂",
)

@Composable
@Preview
fun App() {
    val state = remember { AppState() }
    val navigator = remember { Navigator() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    state.onNotify = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }

    LaunchedEffect(Unit) { state.start() }

    UnDiaMasTheme(darkTheme = state.settings.darkTheme) {
        // Un fallo de conexion no debe dejar la app en blanco ni tumbarla: se explica y se reintenta.
        state.startupError?.let { error ->
            val diagnosis = diagnoseStartupError(error)
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(diagnosis.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        diagnosis.advice,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        diagnosis.technicalDetail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = { state.retryStart() }) { Text("Reintentar") }
                }
            }
            return@UnDiaMasTheme
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@UnDiaMasTheme
        }

        if (!state.isOnboarded) {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
                Box(modifier = Modifier.padding(padding)) { OnboardingScreen(state) }
            }
            return@UnDiaMasTheme
        }

        val isTopLevel = bottomTabs.any { it.first == navigator.current }

        Scaffold(
            topBar = {
                if (!isTopLevel) {
                    TopAppBar(
                        title = { Text(navigator.current.label) },
                        navigationIcon = {
                            TextButton(onClick = { navigator.back() }) { Text("←  Atrás") }
                        },
                    )
                }
            },
            bottomBar = {
                if (isTopLevel) {
                    NavigationBar(tonalElevation = 0.dp) {
                        bottomTabs.forEach { (screen, emoji) ->
                            NavigationBarItem(
                                selected = navigator.current == screen,
                                onClick = { navigator.goTo(screen) },
                                icon = { Text(emoji, style = MaterialTheme.typography.titleMedium) },
                                label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) { Text(data.visuals.message) }
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = navigator.current,
                transitionSpec = { screenTransition() },
                modifier = Modifier.padding(padding),
                label = "screen",
            ) { screen ->
                Box {
                    when (screen) {
                        Screen.Inicio -> InicioScreen(state, navigator)
                        Screen.Sobriedad -> SobrietyScreen(state)
                        Screen.CheckIn -> CheckInScreen(state, navigator)
                        Screen.Ia -> IaScreen(state)
                        Screen.Diario -> DiarioScreen(state)
                        Screen.Estadisticas -> EstadisticasScreen(state)
                        Screen.Calculadora -> CalculadoraScreen(state)
                        Screen.Emergencia -> EmergenciaScreen(state)
                        Screen.Perfil -> PerfilScreen(state, navigator)
                        Screen.Configuracion -> ConfiguracionScreen(state)
                    }
                }
            }
        }
    }
}
