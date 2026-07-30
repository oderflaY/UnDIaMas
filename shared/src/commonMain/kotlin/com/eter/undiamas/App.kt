package com.eter.undiamas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.theme.UnDiaMasTheme
import com.eter.undiamas.features.calculadora.presentation.CalculadoraScreen
import com.eter.undiamas.features.checkin.presentation.CheckInScreen
import com.eter.undiamas.features.configuracion.presentation.ConfiguracionScreen
import com.eter.undiamas.features.diario.presentation.DiarioScreen
import com.eter.undiamas.features.emergencia.presentation.EmergenciaScreen
import com.eter.undiamas.features.estadisticas.presentation.EstadisticasScreen
import com.eter.undiamas.features.ia.presentation.IaScreen
import com.eter.undiamas.features.inicio.presentation.InicioScreen
import com.eter.undiamas.features.perfil.presentation.PerfilScreen
import com.eter.undiamas.features.sobriedad.presentation.SobrietyScreen
import kotlinx.coroutines.launch

private val bottomTabs = listOf(Screen.Inicio, Screen.CheckIn, Screen.Diario, Screen.Estadisticas, Screen.Perfil)

@Composable
@Preview
fun App() {
    val state = remember { AppState() }
    val navigator = remember { Navigator() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    state.onNotify = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }

    UnDiaMasTheme {
        val isTopLevel = bottomTabs.any { it == navigator.current }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(navigator.current.label, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        if (!isTopLevel) {
                            TextButton(onClick = { navigator.back() }) { Text("← Atrás") }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(),
                )
            },
            bottomBar = {
                if (isTopLevel) {
                    HorizontalDivider()
                    NavigationBar {
                        bottomTabs.forEach { screen ->
                            NavigationBarItem(
                                selected = navigator.current == screen,
                                onClick = { navigator.goTo(screen) },
                                icon = {},
                                label = { Text(screen.label) },
                            )
                        }
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(shape = MaterialTheme.shapes.medium) {
                        Text(data.visuals.message)
                    }
                }
            },
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (navigator.current) {
                    Screen.Inicio -> InicioScreen(state, navigator)
                    Screen.Sobriedad -> SobrietyScreen(state)
                    Screen.CheckIn -> CheckInScreen(state, navigator)
                    Screen.Ia -> IaScreen(state)
                    Screen.Diario -> DiarioScreen(state)
                    Screen.Estadisticas -> EstadisticasScreen(state)
                    Screen.Calculadora -> CalculadoraScreen(state)
                    Screen.Emergencia -> EmergenciaScreen(state)
                    Screen.Perfil -> PerfilScreen(state, navigator)
                    Screen.Configuracion -> ConfiguracionScreen()
                }
            }
        }
    }
}
