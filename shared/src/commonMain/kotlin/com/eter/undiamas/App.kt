package com.eter.undiamas

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import com.eter.undiamas.core.presentation.ThemeMode
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.theme.UnDiaMasTheme
import com.eter.undiamas.core.presentation.theme.screenTransition
import com.eter.undiamas.features.auth.presentation.AuthScreen
import com.eter.undiamas.features.avisos.domain.Notificador
import com.eter.undiamas.features.avisos.domain.NotificadorInactivo
import com.eter.undiamas.features.calculadora.presentation.CalculadoraScreen
import com.eter.undiamas.features.checkin.presentation.CheckInScreen
import com.eter.undiamas.features.comunidad.presentation.ComunidadScreen
import com.eter.undiamas.features.comunidad.presentation.PublicarHistoriaScreen
import com.eter.undiamas.features.configuracion.presentation.ConfiguracionScreen
import com.eter.undiamas.features.diario.presentation.DiarioScreen
import com.eter.undiamas.features.anclas.presentation.AnclasScreen
import com.eter.undiamas.features.capsulas.presentation.CapsulasScreen
import com.eter.undiamas.features.emergencia.presentation.EmergenciaScreen
import com.eter.undiamas.features.habitos.presentation.HabitosScreen
import com.eter.undiamas.features.emergencia.presentation.UrgeSurfingScreen
import com.eter.undiamas.features.estadisticas.presentation.EstadisticasScreen
import com.eter.undiamas.features.ia.presentation.IaScreen
import com.eter.undiamas.features.inicio.presentation.DashboardScreen
import com.eter.undiamas.features.onboarding.presentation.OnboardingScreen
import com.eter.undiamas.features.onboarding.presentation.IntroSlides
import com.eter.undiamas.features.perfil.presentation.EditarPerfilScreen
import com.eter.undiamas.features.perfil.presentation.PerfilScreen
import com.eter.undiamas.features.splash.presentation.SplashScreen
import com.eter.undiamas.features.sobriedad.presentation.SobrietyScreen
import kotlinx.coroutines.launch
import com.eter.undiamas.features.biometria.presentation.BiometriaScreen
import com.eter.undiamas.core.domain.biometrics.BiometricsProvider
import kotlinx.coroutines.flow.first
import com.eter.undiamas.core.data.UserPreferences
import com.eter.undiamas.core.data.api.ApiConfig
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import com.eter.undiamas.core.presentation.components.SyncBanner
import com.eter.undiamas.core.presentation.diagnoseStartupError

/** Las cinco secciones de siempre: inicio, check-in, diario, estadísticas y perfil. */
private val bottomTabs = listOf(
    Screen.Inicio to AppIcons.Inicio,
    Screen.CheckIn to AppIcons.CheckIn,
    Screen.Diario to AppIcons.Diario,
    Screen.Estadisticas to AppIcons.Estadisticas,
    Screen.Perfil to AppIcons.Perfil,
)

@Composable
@Preview
fun App(
    biometrics: BiometricsProvider? = null,
    preferences: UserPreferences? = null,
    notificador: Notificador = NotificadorInactivo(),
) {
    val state = remember {
        AppState(
            biometricsProvider = biometrics,
            preferences = preferences,
            notificador = notificador,
        )
    }
    var restored by remember { mutableStateOf(preferences == null) }
    var splashListo by remember { mutableStateOf(false) }
    var introVista by remember { mutableStateOf(false) }

    // Restaura la sesión previa antes de decidir si mostrar el cuestionario inicial;
    // sin esta espera se vería el onboarding un instante aunque ya estuviera completo.
    LaunchedEffect(preferences) {
        if (preferences == null) return@LaunchedEffect
        // Antes que nada: a qué servidor apunta esta instalación. Va aquí porque el cliente
        // HTTP lee la dirección en cada petición, y la primera es la de recuperar la sesión.
        // En release se ignora lo guardado: la dirección la fija la compilación y punto.
        if (ApiConfig.permiteCambiarServidor) {
            preferences.readServerUrl()?.let { ApiConfig.baseUrl = it }
        }
        state.restoreFrom(
            completed = preferences.onboardingCompleted.first(),
            savedName = preferences.displayName.first(),
            savedAddiction = preferences.userAddiction.first(),
        )
        restored = true
    }
    val navigator = remember { Navigator() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    state.onNotify = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }

    // Se espera a `restored` para no arrancar la sesión contra la dirección de fábrica
    // cuando hay otra guardada: la primera petición ya debe salir al servidor correcto.
    LaunchedEffect(restored) { if (restored) state.start() }

    // El teléfono decide, salvo que la persona haya elegido explícitamente en Configuración.
    val oscuro = when (state.settings.themeMode) {
        ThemeMode.SISTEMA -> isSystemInDarkTheme()
        ThemeMode.CLARO -> false
        ThemeMode.OSCURO -> true
    }

    UnDiaMasTheme(darkTheme = oscuro) {
        // El splash tapa el arranque: recuperar la sesión y abrir la base local. Va lo
        // primero para que nadie vea una pantalla a medio montar.
        if (!splashListo) {
            SplashScreen(onTerminar = { splashListo = true })
            return@UnDiaMasTheme
        }

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

        // Sin sesion no hay datos: el backend no emite tokens anonimos. Va antes que el
        // indicador de carga porque esperar a nada seria quedarse girando para siempre.
        if (state.needsAuth) {
            // Las diapositivas de bienvenida van antes del formulario: explican de qué va
            // la app a quien todavía no tiene motivo para darle su correo.
            if (!introVista) {
                IntroSlides(onTerminar = { introVista = true })
                return@UnDiaMasTheme
            }
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    AuthScreen(state)
                }
            }
            return@UnDiaMasTheme
        }

        // Mientras se recupera la sesion, o mientras se restauran las preferencias locales,
        // se muestra el mismo indicador: para quien usa la app es una sola espera.
        if (state.isLoading || !restored) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@UnDiaMasTheme
        }

        if (!state.isOnboarded) {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    OnboardingScreen(state, onFinishOnboarding = { scope.launch { state.persistOnboarding() } })
                }
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
                // Una coleccion que no cargo se avisa sin bloquear el resto de la app.
                state.dataWarning?.let { warning ->
                    LaunchedEffect(warning) {
                        snackbarHostState.showSnackbar(warning)
                        state.clearDataWarning()
                    }
                }
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) { Text(data.visuals.message) }
                }
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // Encima de todo: si algo quedó sin enviar, se dice en cualquier pantalla,
                // no solo en la que se estaba usando al perder la conexión.
                SyncBanner(state, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))

                AnimatedContent(
                    targetState = navigator.current,
                    transitionSpec = { screenTransition() },
                    label = "screen",
                ) { screen ->
                    Box {
                        when (screen) {
                            Screen.Inicio -> DashboardScreen(state, navigator)
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
                            Screen.Biometria -> BiometriaScreen(state, navigator)
                            Screen.Perfil -> PerfilScreen(state, navigator)
                            Screen.EditarPerfil -> EditarPerfilScreen(state, onVolver = { navigator.back() })
                            Screen.Comunidad -> ComunidadScreen(state, navigator)
                            Screen.PublicarHistoria -> PublicarHistoriaScreen(state, onVolver = { navigator.back() })
                            Screen.Configuracion -> ConfiguracionScreen(state)
                        }
                    }
                }
            }
        }
    }
}
