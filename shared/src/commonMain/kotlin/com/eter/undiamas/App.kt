package com.eter.undiamas

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.theme.UnDiaMasTheme
import com.eter.undiamas.core.presentation.theme.screenTransition
import com.eter.undiamas.features.calculadora.presentation.CalculadoraScreen
import com.eter.undiamas.features.checkin.presentation.CheckInScreen
import com.eter.undiamas.features.configuracion.presentation.ConfiguracionScreen
import com.eter.undiamas.features.diario.presentation.DiarioScreen
import com.eter.undiamas.features.anclas.presentation.AnclasScreen
import com.eter.undiamas.features.capsulas.presentation.CapsulasScreen
import com.eter.undiamas.features.emergencia.presentation.EmergenciaScreen
import com.eter.undiamas.features.habitos.presentation.HabitosScreen
import com.eter.undiamas.features.emergencia.presentation.UrgeSurfingScreen
import com.eter.undiamas.features.estadisticas.presentation.EstadisticasScreen
import com.eter.undiamas.features.ia.presentation.IaScreen
import com.eter.undiamas.features.inicio.presentation.InicioScreen
import com.eter.undiamas.features.onboarding.presentation.OnboardingScreen
import com.eter.undiamas.features.perfil.presentation.PerfilScreen
import com.eter.undiamas.features.sobriedad.presentation.SobrietyScreen
import kotlinx.coroutines.launch
import com.eter.undiamas.features.biometria.presentation.BiometriaScreen
import com.eter.undiamas.core.domain.biometrics.BiometricsProvider

private val bottomTabs = listOf(
    Screen.Inicio to AppIcons.Inicio,
    Screen.CheckIn to AppIcons.CheckIn,
    Screen.Diario to AppIcons.Diario,
    Screen.Estadisticas to AppIcons.Estadisticas,
    Screen.Perfil to AppIcons.Perfil,
)

@Composable
@Preview
fun App(biometrics: BiometricsProvider? = null) {
    val state = remember { AppState(biometricsProvider = biometrics) }
    val navigator = remember { Navigator() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    state.onNotify = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }

    UnDiaMasTheme(darkTheme = state.settings.darkTheme) {
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
                            IconButton(onClick = { navigator.back() }) {
                                Icon(AppIcons.Atras, contentDescription = "Atrás")
                            }
                        },
                    )
                }
            },
            bottomBar = {
                if (isTopLevel) {
                    NavigationBar(tonalElevation = 0.dp) {
                        bottomTabs.forEach { (screen, icon) ->
                            NavigationBarItem(
                                selected = navigator.current == screen,
                                onClick = { navigator.goTo(screen) },
                                icon = { Icon(icon, contentDescription = screen.label) },
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
                        Screen.Estadisticas -> EstadisticasScreen(state, navigator)
                        Screen.Calculadora -> CalculadoraScreen(state)
                        Screen.Emergencia -> EmergenciaScreen(state, navigator)
                        Screen.UrgeSurfing -> UrgeSurfingScreen(state, navigator)
                        Screen.Capsulas -> CapsulasScreen(state)
                        Screen.Habitos -> HabitosScreen(state)
                        Screen.Anclas -> AnclasScreen(state)
                        Screen.Biometria -> BiometriaScreen(state)
                        Screen.Perfil -> PerfilScreen(state, navigator)
                        Screen.Configuracion -> ConfiguracionScreen(state)
                    }
                }
            }
        }
    }
}
