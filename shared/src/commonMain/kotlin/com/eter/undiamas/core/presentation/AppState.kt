package com.eter.undiamas.core.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eter.undiamas.core.data.UserPreferences
import com.eter.undiamas.core.data.api.ApiGraph
import com.eter.undiamas.core.data.local.EstadoSync
import com.eter.undiamas.core.data.api.asAlert
import com.eter.undiamas.core.data.api.toDomain
import com.eter.undiamas.core.data.api.toUserMessage
import com.eter.undiamas.core.domain.model.AddictionType
import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.Mood
import com.eter.undiamas.core.domain.model.MoodEntry
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.domain.model.TrustedContact
import com.eter.undiamas.core.domain.model.UserProfile
import com.eter.undiamas.core.domain.repository.Alert
import com.eter.undiamas.core.data.api.ApiException
import com.eter.undiamas.core.domain.model.SavingsGoal
import com.eter.undiamas.core.domain.model.SupportRole
import com.eter.undiamas.core.domain.model.Trigger
import com.eter.undiamas.core.domain.repository.Reminder
import kotlinx.datetime.TimeZone
import com.eter.undiamas.core.domain.repository.Session
import com.eter.undiamas.features.anclas.domain.Anchor
import com.eter.undiamas.features.anclas.domain.AnchorKind
import com.eter.undiamas.features.avisos.domain.ContextoAviso
import com.eter.undiamas.features.avisos.domain.Notificador
import com.eter.undiamas.features.avisos.domain.NotificadorInactivo
import com.eter.undiamas.features.avisos.domain.PlanificadorDeAvisos
import com.eter.undiamas.features.calculadora.domain.SavingsCalculator
import com.eter.undiamas.features.comunidad.domain.BorradorDeHistoria
import com.eter.undiamas.features.comunidad.domain.Historia
import com.eter.undiamas.features.comunidad.domain.MotivoReporte
import com.eter.undiamas.features.comunidad.domain.OrdenHistorias
import com.eter.undiamas.features.capsulas.domain.TimeCapsule
import com.eter.undiamas.features.capsulas.domain.TimeCapsuleVault
import com.eter.undiamas.features.checkin.domain.CheckInHistory
import com.eter.undiamas.features.checkin.domain.RiskAssessor
import com.eter.undiamas.features.diario.domain.DiaryEntry
import com.eter.undiamas.features.diario.domain.SentimentAnalyzer
import com.eter.undiamas.features.estadisticas.domain.RiskInsights
import com.eter.undiamas.features.estadisticas.domain.RiskPatternDetector
import com.eter.undiamas.features.habitos.domain.Habit
import com.eter.undiamas.features.habitos.domain.HabitCompletion
import com.eter.undiamas.features.habitos.domain.HabitTracker
import com.eter.undiamas.features.sobriedad.domain.Milestones
import com.eter.undiamas.features.sobriedad.domain.SobrietyCounter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.Instant

private const val SECONDS_PER_DAY = 60L * 60 * 24

/**
 * Cada cuánto se reintenta enviar lo pendiente aunque nadie avise de un cambio de red.
 *
 * Cinco minutos: lo bastante seguido para que nada se quede horas atascado, lo bastante
 * espaciado para no despertar la radio del teléfono y gastarle la batería a alguien.
 */
private const val REINTENTO_SYNC_MILLIS = 5 * 60 * 1000L

/**
 * Cómo decide la app si va en claro u oscuro.
 *
 * [SISTEMA] es el valor por defecto: el teléfono ya sabe si es de noche o si la persona
 * prefiere el modo oscuro siempre, y respetarlo evita que esta app sea la única que
 * deslumbra a las tres de la mañana.
 */
enum class ThemeMode {
    SISTEMA,
    CLARO,
    OSCURO,
}

/** Preferencias de la pantalla de Configuración. */
data class AppSettings(
    val dailyReminders: Boolean = true,
    val reminderHour: Int = 21,
    val weeklySummary: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SISTEMA,
    val diaryLocked: Boolean = false,
    val stealthMode: Boolean = false,
)

/**
 * Estado compartido entre pantallas.
 *
 * El servidor es la fuente de verdad; esta clase es su espejo en memoria. Cada lista
 * (`checkIns`, `diaryEntries`...) refleja el StateFlow de su repositorio, y los métodos
 * `register*`/`add*` escriben contra el backend y dejan que el repositorio actualice el
 * espejo. Ninguna pantalla habla con la red directamente.
 *
 * A diferencia de Firestore, aquí no hay listeners: los datos llegan cuando alguien los
 * pide ([refreshAll]) o cuando el servidor avisa por el canal de eventos.
 */
class AppState(
    /**
     * Beta sin backend: todo vive en el telefono.
     *
     * Se expone para que las pantallas puedan decirlo en vez de fingir que hay servidor:
     * la comunidad no existe y los datos no tienen copia en ningun sitio, y callarse
     * cualquiera de las dos cosas seria engañar a quien prueba la app.
     */
    val modoLocal: Boolean = false,
    /** Preferencias locales: tokens de sesión y lo justo para no repetir el onboarding. */
    private val preferences: UserPreferences? = null,
    private val graph: ApiGraph = ApiGraph(preferences, modoLocal),
    /** Notificaciones locales del sistema. Sin implementación de plataforma, no hace nada. */
    private val notificador: Notificador = NotificadorInactivo(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val authRepository = graph.auth
    private val perfilRepository = graph.perfil
    private val checkInRepository = graph.checkIns
    private val diaryRepository = graph.diary
    private val moodRepository = graph.moods
    private val relapseRepository = graph.relapses
    private val trafficLightRepository = graph.trafficLight
    private val alertRepository = graph.alerts
    private val reminderRepository = graph.reminders
    private val statsRepository = graph.stats
    private val comunidadRepository = graph.comunidad

    /** id de la sesion actual; null mientras no haya nadie dentro. */
    var uid: String? by mutableStateOf(null)
        private set

    var session: Session? by mutableStateOf(null)
        private set

    /**
     * true cuando hace falta entrar o crear una cuenta.
     *
     * El backend no emite tokens anónimos, así que sin credenciales no hay datos: es la
     * pantalla de entrada o nada.
     */
    var needsAuth: Boolean by mutableStateOf(false)
        private set

    /** true mientras se recupera la sesion y se piden los primeros datos. */
    var isLoading: Boolean by mutableStateOf(true)
        private set

    /** true mientras se envia un login o un registro, para bloquear el boton. */
    var isAuthenticating: Boolean by mutableStateOf(false)
        private set

    /** Error legible del ultimo login/registro, o null. */
    var authError: String? by mutableStateOf(null)
        private set

    /** Fallo al arrancar (sin red, servidor apagado). null si todo fue bien. */
    var startupError: String? by mutableStateOf(null)
        private set

    /**
     * Aviso de que algo no se pudo cargar, sin ser fatal.
     * La app sigue usable con lo que sí cargó; esto solo lo hace visible.
     */
    var dataWarning: String? by mutableStateOf(null)
        private set

    fun clearDataWarning() {
        dataWarning = null
    }

    fun clearAuthError() {
        authError = null
    }

    /** Semáforo vigente según el servidor, no según lo último que calculó esta pantalla. */
    var currentRiskLevel: RiskLevel by mutableStateOf(RiskLevel.VERDE)
        private set

    /** Racha y ahorro los calcula el backend: no dependen del reloj del teléfono. */
    var streakSeconds: Long by mutableStateOf(0)
        private set

    var savedAmount: Double by mutableStateOf(0.0)
        private set

    /** Hay red según el sistema. No garantiza que el servidor responda. */
    var isOnline: Boolean by mutableStateOf(true)
        private set

    /** En qué punto está el envío de lo que se guardó sin conexión. */
    var syncState: EstadoSync by mutableStateOf(EstadoSync.AL_DIA)
        private set

    /** Cuántos cambios esperan a que vuelva la red. */
    var pendingChanges: Int by mutableStateOf(0)
        private set

    /** Ids de las filas guardadas en el teléfono que el servidor todavía no confirmó. */
    var unsyncedIds: Set<String> by mutableStateOf(emptySet())
        private set

    /** Sesiones del búnker de 15 minutos completadas hasta el final. */
    var urgeSessionsCompleted: Int by mutableStateOf(0)
        private set

    private var nextCapsuleId = 0
    private var nextHabitId = 0
    private var nextAnchorId = 0

    private val mirrorJobs = mutableListOf<Job>()
    private var eventsJob: Job? = null

    var profile: UserProfile by mutableStateOf(
        UserProfile(
            userId = "",
            displayName = "",
            sobrietyStartDate = Clock.System.now(),
        ),
    )
        private set

    var settings: AppSettings by mutableStateOf(AppSettings())
        private set

    /**
     * Mientras sea false la app muestra el cuestionario inicial.
     *
     * Se enciende por un hecho explicito y nunca se deduce del perfil: haber contestado el
     * cuestionario y tener nombre son cosas distintas, y confundirlas hacia que el
     * cuestionario se cerrara solo a media pregunta en cuanto llegaba el perfil del
     * servidor —el registro ya pide el nombre—.
     *
     * Los tres hechos que lo encienden: la marca guardada en el telefono, terminar el
     * cuestionario, y que el servidor diga que esta cuenta ya tiene fecha de inicio.
     */
    var isOnboarded: Boolean by mutableStateOf(false)
        private set

    val checkIns = mutableStateListOf<CheckInEntry>()
    val diaryEntries = mutableStateListOf<DiaryEntry>()
    val moodEntries = mutableStateListOf<MoodEntry>()

    /** Alertas del protocolo de emergencia que creó el servidor. */
    val alerts = mutableStateListOf<Alert>()

    // Estas cuatro no tienen ruta en el backend todavía, así que viven en memoria y se
    // pierden al cerrar la app. Es la deuda que queda de esta migración.
    val capsules = mutableStateListOf<TimeCapsule>()
    val habits = mutableStateListOf<Habit>()
    val habitCompletions = mutableStateListOf<HabitCompletion>()
    val anchors = mutableStateListOf<Anchor>()

    val sobrietyCounter = SobrietyCounter()
    val savingsCalculator = SavingsCalculator()
    val riskAssessor = RiskAssessor()
    val checkInHistory = CheckInHistory()
    val riskInsights = RiskInsights()
    val milestones = Milestones()
    val riskPatternDetector = RiskPatternDetector()
    val capsuleVault = TimeCapsuleVault()
    val habitTracker = HabitTracker()
    val sentimentAnalyzer = SentimentAnalyzer()
    val planificadorDeAvisos = PlanificadorDeAvisos()

    // ---- Comunidad ---------------------------------------------------------------
    //
    // Se exponen los StateFlow tal cual, sin espejo en listas de Compose: el muro es
    // contenido remoto y paginado, no algo que la app mantenga sincronizado.

    val comunidadHistorias = comunidadRepository.historias.collectAsMutableState()
    val comunidadPerfil = comunidadRepository.perfil.collectAsMutableState()
    val comunidadCargando = comunidadRepository.cargando.collectAsMutableState()
    val comunidadHayMas = comunidadRepository.hayMas.collectAsMutableState()

    /** La pantalla raíz reemplaza esto por una función que muestra un snackbar real. */
    var onNotify: (String) -> Unit = {}

    fun notify(message: String) = onNotify(message)

    // ---- Sesion ------------------------------------------------------------------

    /**
     * Recupera la sesión guardada y carga los datos. Llamar una sola vez al abrir la app.
     *
     * Sin `runCatching`, un servidor apagado tumbaría la app al abrirla. En una app de
     * recuperación eso es inaceptable: es preferible entrar en modo degradado y ofrecer
     * reintentar.
     */
    fun start() {
        scope.launch {
            startupError = null
            runCatching { authRepository.restore() }
                .onSuccess { restored ->
                    if (restored == null) {
                        needsAuth = true
                        isLoading = false
                    } else {
                        onSignedIn(restored)
                    }
                }
                .onFailure { error ->
                    startupError = error.toUserMessage()
                    isLoading = false
                }
        }
    }

    /** Reintento manual del arranque tras un fallo de conexión. */
    fun retryStart() {
        isLoading = true
        start()
    }

    // ---- Recuperacion de contraseña ----------------------------------------------

    /** Paso en el que va la recuperacion; null cuando la pantalla no esta abierta. */
    var recuperacion: EstadoDeRecuperacion? by mutableStateOf(null)
        private set

    fun abrirRecuperacion(email: String) {
        recuperacion = EstadoDeRecuperacion(email = email)
    }

    fun cerrarRecuperacion() {
        recuperacion = null
    }

    /**
     * Pide el codigo por correo.
     *
     * Se avanza al paso del codigo pase lo que pase con cuentas inexistentes, porque el
     * servidor responde igual exista o no: decir "ese correo no tiene cuenta" convertiria
     * esta pantalla en una forma de averiguar quien usa una app de adicciones.
     */
    fun pedirCodigoDeRecuperacion() {
        val actual = recuperacion ?: return
        if (actual.enviando) return
        recuperacion = actual.copy(enviando = true, error = null)
        scope.launch {
            runCatching { authRepository.requestPasswordReset(actual.email) }
                .onSuccess {
                    recuperacion = recuperacion?.copy(enviando = false, codigoEnviado = true)
                }
                .onFailure { error ->
                    recuperacion = recuperacion?.copy(
                        enviando = false,
                        // Que al servidor le falte el correo no es culpa de quien lo pulsa,
                        // y merece una explicacion distinta de "algo salio mal".
                        sinCorreoEnElServidor = error is ApiException && error.isServiceMissing,
                        error = error.toUserMessage(),
                    )
                }
        }
    }

    fun cambiarContrasena(codigo: String, nueva: String) {
        val actual = recuperacion ?: return
        if (actual.enviando) return
        recuperacion = actual.copy(enviando = true, error = null)
        scope.launch {
            runCatching { authRepository.resetPassword(actual.email, codigo, nueva) }
                .onSuccess {
                    recuperacion = null
                    // Se entra sola: pedirle la contraseña que acaba de escribir seria
                    // hacerle repetir un paso que la app ya conoce.
                    authenticate { authRepository.login(actual.email, nueva) }
                    notify("Contraseña cambiada")
                }
                .onFailure { error ->
                    recuperacion = recuperacion?.copy(enviando = false, error = error.toUserMessage())
                }
        }
    }

    fun register(email: String, password: String, displayName: String) {
        authenticate { authRepository.register(email, password, displayName) }
    }

    fun signInWithEmail(email: String, password: String) {
        authenticate { authRepository.login(email, password) }
    }

    private fun authenticate(block: suspend () -> Session) {
        if (isAuthenticating) return
        scope.launch {
            isAuthenticating = true
            authError = null
            runCatching { block() }
                .onSuccess { newSession ->
                    isLoading = true
                    onSignedIn(newSession)
                }
                .onFailure { authError = it.toUserMessage() }
            isAuthenticating = false
        }
    }

    /** Cierra la sesión en todos los dispositivos y vuelve a la pantalla de entrada. */
    fun signOut() {
        scope.launch {
            runCatching { authRepository.logout() }
            runCatching { notificador.cancelarTodo() }
            stopMirrors()
            clearMirroredData()
            uid = null
            session = null
            isOnboarded = false
            // Sin servidor no hay pantalla de entrada a la que volver: se abre una cuenta
            // local nueva, que es lo que "empezar de cero" significa en la beta.
            needsAuth = !modoLocal
            isLoading = modoLocal
            profile = UserProfile(userId = "", displayName = "", sobrietyStartDate = Clock.System.now())
            if (modoLocal) start()
        }
    }

    private suspend fun onSignedIn(newSession: Session) {
        stopMirrors()
        clearMirroredData()
        graph.currentUserId = newSession.userId
        session = newSession
        uid = newSession.userId
        needsAuth = false
        authError = null
        profile = profile.copy(userId = newSession.userId, displayName = newSession.displayName)

        // Lo primero es la base del teléfono: la pantalla se pinta con lo que ya hay,
        // aunque no haya red. Si el teléfono venía de otra cuenta, se borra su caché.
        graph.local.abrirSesion(newSession.userId)
        graph.outbox.refrescarConteo()
        isLoading = false

        startMirrors()

        // En la beta local no hay a quien preguntar ni quien avise: la pantalla ya tiene
        // todo lo que existe, que es lo que hay en SQLite.
        if (modoLocal) {
            reprogramarAvisos()
            return
        }

        observarConexion()
        refreshAll()
        listenToServerEvents()
    }

    /**
     * Empuja la cola en cuanto vuelve la red.
     *
     * También se reintenta cada pocos minutos: "hay wifi" no es lo mismo que "el servidor
     * responde", y el caso del wifi de cafetería que pide iniciar sesión se resuelve solo
     * volviendo a probar.
     */
    private fun observarConexion() {
        mirrorJobs += scope.launch {
            graph.connectivity.estaEnLinea.collect { enLinea ->
                isOnline = enLinea
                if (enLinea) {
                    if (graph.sync.sincronizar()) refreshAllNow()
                } else {
                    graph.sync.marcarSinConexion()
                }
            }
        }
        mirrorJobs += scope.launch {
            while (true) {
                delay(REINTENTO_SYNC_MILLIS)
                if (graph.outbox.pendientes.value > 0) graph.sync.sincronizar()
            }
        }
    }

    /** Reintento manual, para quien no quiere esperar a que la app lo haga sola. */
    fun sincronizarAhora() {
        scope.launch {
            val exito = graph.sync.sincronizar()
            if (exito) {
                refreshAllNow()
                notify("Todo sincronizado")
            } else {
                notify("Sigue sin haber conexión. Tus cambios están guardados.")
            }
        }
    }

    // ---- Espejo de los repositorios ----------------------------------------------

    private fun startMirrors() {
        mirrorJobs += scope.launch {
            perfilRepository.profile.collect { loaded ->
                if (loaded != null) {
                    // El servidor no guarda el tipo de adicción ni la meta de ahorro: se
                    // conserva lo que ya había en pantalla en vez de borrarlo.
                    profile = loaded.copy(
                        addiction = loaded.addiction ?: profile.addiction,
                        savingsGoal = loaded.savingsGoal ?: profile.savingsGoal,
                    )
                }
            }
        }
        mirrorJobs += scope.launch {
            perfilRepository.streakSeconds.collect { segundos ->
                streakSeconds = segundos
                // Si la racha en curso ya superó al récord guardado, el récord es esta
                // racha. Se anota en el perfil para que sobreviva a la próxima recaída:
                // es lo único que a alguien le queda de un tramo que costó meses.
                if (segundos > profile.recordStreakSeconds) {
                    profile = profile.copy(recordStreakSeconds = segundos)
                }
            }
        }
        mirrorJobs += scope.launch { perfilRepository.savedAmount.collect { savedAmount = it } }
        // Solo puede encender la bandera, nunca apagarla: si el servidor tarda en
        // contestar mientras alguien esta a medio cuestionario, la respuesta que llega no
        // puede cerrarselo en la cara.
        mirrorJobs += scope.launch {
            perfilRepository.onboardingCompleto.collect { completo ->
                if (completo) isOnboarded = true
            }
        }
        mirrorJobs += scope.launch { mirror(checkInRepository.items, checkIns) }
        mirrorJobs += scope.launch { mirror(diaryRepository.items, diaryEntries) }
        mirrorJobs += scope.launch { mirror(moodRepository.items, moodEntries) }
        mirrorJobs += scope.launch { mirror(alertRepository.items, alerts) }
        mirrorJobs += scope.launch {
            trafficLightRepository.current.collect { nivel ->
                val cambio = nivel != currentRiskLevel
                currentRiskLevel = nivel
                // El plan de avisos depende del semáforo, así que se rehace en cuanto cambia.
                if (cambio) reprogramarAvisos(avisarAhora = nivel == RiskLevel.ROJO)
            }
        }
        mirrorJobs += scope.launch { graph.sync.estado.collect { syncState = it } }
        mirrorJobs += scope.launch { graph.outbox.pendientes.collect { pendingChanges = it } }
        mirrorJobs += scope.launch { graph.local.sinEnviar.collect { unsyncedIds = it } }
        mirrorJobs += scope.launch {
            reminderRepository.reminder.collect { remote ->
                if (remote != null) {
                    settings = settings.copy(
                        dailyReminders = remote.enabled,
                        reminderHour = remote.hour,
                    )
                }
            }
        }
    }

    private suspend fun <T> mirror(
        source: kotlinx.coroutines.flow.StateFlow<List<T>>,
        target: androidx.compose.runtime.snapshots.SnapshotStateList<T>,
    ) {
        source.collect { values ->
            target.clear()
            target.addAll(values)
        }
    }

    private fun stopMirrors() {
        mirrorJobs.forEach { it.cancel() }
        mirrorJobs.clear()
        eventsJob?.cancel()
        eventsJob = null
    }

    private fun clearMirroredData() {
        checkIns.clear()
        diaryEntries.clear()
        moodEntries.clear()
        alerts.clear()
    }

    /**
     * Pide todos los datos.
     *
     * Cada bloque va por separado a propósito: que falle el diario no puede dejar sin
     * cargar el semáforo ni las alertas, que son la parte crítica.
     */
    fun refreshAll() {
        scope.launch { refreshAllNow() }
    }

    private suspend fun refreshAllNow() {
        // Sin conexión no se intenta: la pantalla ya tiene los datos del teléfono, y lanzar
        // diez peticiones condenadas solo serviría para llenarla de avisos de error.
        if (!isOnline) {
            isLoading = false
            return
        }
        loadOrWarn("tu perfil") { perfilRepository.refresh() }
        isLoading = false
        loadOrWarn("tus check-ins") { checkInRepository.refresh() }
        loadOrWarn("el semáforo") { trafficLightRepository.refresh() }
        // Al volver de estar sin conexión, aquí está lo que pasó mientras tanto.
        loadOrWarn("tus alertas") { alertRepository.refresh() }
        loadOrWarn("tu diario") { diaryRepository.refresh() }
        loadOrWarn("tus ánimos") { moodRepository.refresh() }
        loadOrWarn("tu historial de recaídas") { relapseRepository.refresh() }
        loadOrWarn("tus recordatorios") { reminderRepository.refresh() }
        loadOrWarn("tus estadísticas") { statsRepository.refresh() }
        // Con los datos ya cargados, el plan se arma con la racha y el ahorro de verdad.
        // También repone las alarmas que el sistema pierde al reiniciar el teléfono.
        reprogramarAvisos()
    }

    private suspend fun loadOrWarn(what: String, block: suspend () -> Unit) {
        runCatching { block() }.onFailure { error ->
            dataWarning = "No se pudo cargar $what: ${error.toUserMessage()}"
        }
    }

    // ---- Avisos en tiempo real ---------------------------------------------------

    /**
     * Escucha el canal abierto del servidor.
     *
     * Sustituye a las notificaciones push: con la app abierta, un semáforo en rojo llega en
     * el momento. Con la app cerrada no llega nada, pero nada se pierde — queda en
     * `/v1/alerts`, que es justo lo que [refreshAll] vuelve a leer al abrir.
     */
    private fun listenToServerEvents() {
        eventsJob = scope.launch {
            runCatching {
                graph.events.events().collect { event ->
                    event.asAlert()?.let { payload ->
                        val alert = payload.alert.toDomain()
                        alertRepository.onPushed(alert)
                        val contact = payload.trustedContact?.nombre
                        notify(
                            if (contact != null) {
                                "${alert.message}. Puedes llamar a $contact."
                            } else {
                                alert.message
                            },
                        )
                    }
                    if (event.type == "traffic_light") {
                        runCatching { trafficLightRepository.refresh() }
                    }
                    if (event.type == "check_in_reminder") {
                        notify("Es tu hora de check-in. ¿Cómo vas hoy?")
                    }
                }
            }
        }
    }

    // ---- Avisos ------------------------------------------------------------------

    /**
     * Todo lo que las plantillas necesitan para hablar con datos de verdad.
     *
     * Se arma desde la copia local, así que funciona sin conexión: los avisos siguen
     * llegando aunque el servidor lleve días caído.
     */
    private fun contextoDeAvisos(): ContextoAviso {
        val ahora = Clock.System.now()
        val dias = sobrietyCounter.currentStreakSeconds(profile, ahora) / SECONDS_PER_DAY
        val record = sobrietyCounter.recordStreakSeconds(profile, ahora) / SECONDS_PER_DAY
        val ahorro = savedAmount.takeIf { it > 0 }
            ?: savingsCalculator.totalSavings(
                profile.previousDailyExpense,
                sobrietyCounter.currentStreakSeconds(profile, ahora),
            )
        return ContextoAviso(
            nombre = profile.displayName,
            nivel = currentRiskLevel,
            dias = dias,
            recordDias = record,
            ahorro = ahorro,
            anclas = anchors.toList(),
            porQue = profile.personalWhy,
            contacto = profile.trustedContact,
            checkInHecho = checkIns.any {
                it.answeredAt.epochSeconds > ahora.epochSeconds - SECONDS_PER_DAY
            },
        )
    }

    /**
     * Rehace el plan del día.
     *
     * [avisarAhora] muestra además uno en el momento: al pasar a rojo, esperar a la
     * siguiente franja sería llegar tarde a lo único que importa.
     */
    fun reprogramarAvisos(avisarAhora: Boolean = false) {
        if (!settings.dailyReminders) {
            scope.launch { notificador.cancelarTodo() }
            return
        }
        scope.launch {
            if (!notificador.tienePermiso()) return@launch
            val contexto = contextoDeAvisos()
            val semilla = Clock.System.now().epochSeconds.toInt() / 86_400
            notificador.programar(planificadorDeAvisos.planDelDia(contexto, semilla))
            if (avisarAhora) {
                planificadorDeAvisos.avisoInmediato(contexto, semilla)?.let {
                    notificador.mostrarAhora(it)
                }
            }
        }
    }

    /** Pide el permiso de notificaciones y, si lo dan, deja el plan puesto. */
    fun activarAvisos() {
        scope.launch {
            if (notificador.pedirPermiso()) {
                reprogramarAvisos()
                notify("Listo. Te acompañaremos según cómo vaya tu día.")
            } else {
                notify("Sin permiso de notificaciones no podemos recordarte nada.")
            }
        }
    }

    /** Para probar los textos sin esperar a que llegue la hora. */
    fun avisoDePrueba() {
        scope.launch {
            if (!notificador.pedirPermiso()) {
                notify("Sin permiso de notificaciones no podemos recordarte nada.")
                return@launch
            }
            val aviso = planificadorDeAvisos.avisoInmediato(
                contextoDeAvisos(),
                Clock.System.now().epochSeconds.toInt(),
            )
            if (aviso == null) notify("Todavía no hay datos para armar un aviso.")
            else notificador.mostrarAhora(aviso)
        }
    }

    // ---- Acciones de comunidad ----------------------------------------------------

    fun cargarComunidad(orden: OrdenHistorias) {
        scope.launch {
            runCatching { comunidadRepository.refrescar(orden) }
                .onFailure { dataWarning = "No se pudo cargar la comunidad: ${it.toUserMessage()}" }
            runCatching { comunidadRepository.refrescarPerfil() }
        }
    }

    fun cargarMasComunidad(orden: OrdenHistorias) {
        scope.launch { runCatching { comunidadRepository.cargarMas(orden) } }
    }

    fun cargarPerfilDeComunidad() {
        scope.launch { runCatching { comunidadRepository.refrescarPerfil() } }
    }

    /** Guarda el alias si cambió y publica. [onListo] solo corre si el servidor aceptó. */
    fun publicarHistoria(borrador: BorradorDeHistoria, alias: String, onListo: () -> Unit) {
        scope.launch {
            runCatching {
                if (alias.isNotBlank() && alias != comunidadRepository.perfil.value.alias) {
                    comunidadRepository.guardarAlias(alias)
                }
                comunidadRepository.publicar(borrador)
            }.onSuccess {
                notify("Publicada. Gracias por contarlo.")
                onListo()
            }.onFailure {
                dataWarning = "No se pudo publicar: ${it.toUserMessage()}"
            }
        }
    }

    fun marcarHistoriaUtil(historia: Historia) {
        scope.launch {
            runCatching { comunidadRepository.marcarUtil(historia.id, !historia.marcada) }
        }
    }

    fun reportarHistoria(historia: Historia, motivo: MotivoReporte, detalle: String = "") {
        scope.launch {
            runCatching { comunidadRepository.reportar(historia.id, motivo, detalle) }
                .onSuccess { notify("Gracias. Lo vamos a revisar.") }
        }
    }

    fun bloquearAutor(historia: Historia) {
        scope.launch {
            runCatching { comunidadRepository.bloquearAutor(historia.id) }
                .onSuccess { notify("No volverás a ver historias de esa persona.") }
        }
    }

    fun borrarHistoria(historia: Historia) {
        scope.launch {
            runCatching { comunidadRepository.borrar(historia.id) }
                .onSuccess { notify("Historia borrada.") }
        }
    }

    /** Convierte un StateFlow en algo que Compose puede leer sin recolectar en cada pantalla. */
    private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsMutableState():
        androidx.compose.runtime.State<T> {
        val estado = mutableStateOf(value)
        scope.launch { collect { estado.value = it } }
        return estado
    }

    // ---- Perfil ------------------------------------------------------------------

    /**
     * Cambia el perfil en pantalla y lo guarda.
     *
     * Se pinta antes de que el servidor conteste para que el formulario no se sienta
     * trabado; si la escritura falla, el aviso lo dice y el siguiente refresco corrige.
     */
    fun updateProfile(update: (UserProfile) -> UserProfile) {
        val before = profile
        val updated = update(profile)
        profile = updated
        val currentUid = uid ?: return
        if (currentUid.isBlank()) return
        scope.launch {
            runCatching {
                if (updated.displayName != before.displayName || updated.personalWhy != before.personalWhy) {
                    perfilRepository.saveIdentity(updated.displayName, updated.personalWhy)
                }
                if (updated.trustedContact != before.trustedContact ||
                    updated.supportNetwork != before.supportNetwork
                ) {
                    perfilRepository.saveContacts(updated)
                }
                if (updated.sobrietyStartDate != before.sobrietyStartDate ||
                    updated.previousDailyExpense != before.previousDailyExpense
                ) {
                    perfilRepository.saveTracker(updated)
                }
            }.onFailure { dataWarning = "No se pudo guardar tu perfil: ${it.toUserMessage()}" }
        }
    }

    fun updateSettings(update: (AppSettings) -> AppSettings) {
        val before = settings
        val updated = update(settings)
        settings = updated
        // Apagar los recordatorios cancela lo pendiente; encenderlos rehace el plan.
        if (updated.dailyReminders != before.dailyReminders) reprogramarAvisos()
        if (updated.dailyReminders == before.dailyReminders && updated.reminderHour == before.reminderHour) return
        if (uid == null) return
        scope.launch {
            runCatching {
                reminderRepository.save(
                    Reminder(
                        enabled = updated.dailyReminders,
                        hour = updated.reminderHour,
                        minute = 0,
                        timeZone = reminderRepository.reminder.value?.timeZone ?: "America/Mexico_City",
                    ),
                )
            }.onFailure { dataWarning = "No se pudo guardar el recordatorio: ${it.toUserMessage()}" }
        }
    }

    /** Cierra el cuestionario inicial construyendo el perfil con lo que respondió la persona. */
    fun completeOnboarding(
        displayName: String,
        daysSober: Long,
        recordDays: Long,
        previousDailyExpense: Double,
        contactName: String,
        contactPhone: String,
        addiction: AddictionType?,
        contactRole: SupportRole = SupportRole.FAMILIAR,
        /** El "por qué" personal. Es el único campo de texto libre que guarda el servidor. */
        personalWhy: String = "",
        savingsGoalTitle: String = "",
        savingsGoalAmount: Double? = null,
        habitualTriggers: List<Trigger> = emptyList(),
        wantsDailyReminder: Boolean = true,
        reminderHour: Int = 21,
    ) {
        val now = Clock.System.now()
        profile = profile.copy(
            displayName = displayName.ifBlank { "Amigo/a" },
            sobrietyStartDate = Instant.fromEpochSeconds(now.epochSeconds - daysSober * SECONDS_PER_DAY),
            recordStreakSeconds = recordDays * SECONDS_PER_DAY,
            previousDailyExpense = previousDailyExpense,
            trustedContact = if (contactName.isBlank()) {
                null
            } else {
                TrustedContact(contactName, contactPhone, contactRole)
            },
            addiction = addiction,
            personalWhy = personalWhy.trim(),
            // Una meta sin monto no se puede dibujar como progreso, así que hacen falta las dos.
            savingsGoal = if (savingsGoalTitle.isBlank() || savingsGoalAmount == null) {
                null
            } else {
                SavingsGoal(savingsGoalTitle.trim(), savingsGoalAmount)
            },
            habitualTriggers = habitualTriggers,
        )
        isOnboarded = true

        // El recordatorio se guarda por su propia ruta, no con el perfil. Se hace aquí y no
        // en la pantalla para que la elección de la persona valga desde el primer día en
        // vez de esperar a que alguien entre en Configuración.
        settings = settings.copy(dailyReminders = wantsDailyReminder, reminderHour = reminderHour)
        val onboarded = profile
        scope.launch {
            // Primero al teléfono, y solo después se intenta el servidor: quien completa
            // esto sin conexión no puede perder lo que acaba de escribir.
            runCatching { perfilRepository.sembrar(onboarded) }
            runCatching {
                perfilRepository.saveIdentity(onboarded.displayName, onboarded.personalWhy)
                perfilRepository.saveTracker(onboarded)
                if (onboarded.trustedContact != null) perfilRepository.saveContacts(onboarded)
            }.onFailure { dataWarning = "No se pudo guardar tu perfil: ${it.toUserMessage()}" }
            runCatching {
                reminderRepository.save(
                    Reminder(
                        enabled = wantsDailyReminder,
                        hour = reminderHour,
                        minute = 0,
                        timeZone = TimeZone.currentSystemDefault().id,
                    ),
                )
            }

            // El permiso se pide aquí, al terminar el cuestionario, y no escondido en
            // Configuración. Los avisos son la mitad del acompañamiento: una app que nunca
            // los pide solo sirve los días en que la persona se acuerda de abrirla, que son
            // justo los días en los que menos falta hace.
            //
            // Se pide después de haber explicado para qué son y de que la persona haya
            // elegido su hora, no nada más abrir: así el cuadro del sistema aparece cuando
            // ya se sabe qué se está aceptando.
            if (wantsDailyReminder) {
                if (notificador.pedirPermiso()) {
                    reprogramarAvisos()
                } else {
                    dataWarning = "Sin permiso de notificaciones no podremos recordarte " +
                        "nada. Puedes darlo luego desde Configuración."
                }
            } else {
                runCatching { notificador.cancelarTodo() }
            }
            if (isOnline) runCatching { perfilRepository.refresh() }
        }
    }

    /**
     * Restaura lo guardado localmente para pintar algo útil antes de que el servidor
     * responda. Cuando llega el perfil remoto lo sobrescribe: el servidor manda.
     */
    fun restoreFrom(completed: Boolean, savedName: String, savedAddiction: AddictionType?) {
        if (!completed) return
        profile = profile.copy(
            displayName = savedName.ifBlank { profile.displayName },
            addiction = savedAddiction ?: profile.addiction,
        )
        isOnboarded = true
    }

    // ---- Escrituras --------------------------------------------------------------

    /**
     * Guarda el check-in y, si sale ROJO, deja constancia en el semáforo.
     *
     * Los dos avisos los crea el servidor, no la app: un check-in en rojo genera su alerta
     * aunque el teléfono se apague justo después de enviarlo.
     */
    fun registerCheckIn(entry: CheckInEntry) {
        if (uid == null) return
        scope.launch {
            runCatching {
                checkInRepository.add(entry)
                trafficLightRepository.save(
                    status = entry.riskLevel,
                    reason = entry.note.ifBlank { "Check-in" },
                    triggerLevel = entry.urgeIntensity,
                    suggestedActions = emptyList(),
                )
                perfilRepository.refresh()
            }.onFailure { dataWarning = "No se pudo guardar el check-in: ${it.toUserMessage()}" }
        }
    }

    fun addDiaryEntry(entry: DiaryEntry) {
        if (uid == null) return
        scope.launch {
            runCatching { diaryRepository.add(entry.text) }
                .onFailure { dataWarning = "No se pudo guardar la entrada: ${it.toUserMessage()}" }
        }
    }

    fun deleteDiaryEntry(id: String) {
        scope.launch {
            runCatching { diaryRepository.delete(id) }
                .onFailure { dataWarning = "No se pudo borrar la entrada: ${it.toUserMessage()}" }
        }
    }

    fun registerMood(mood: Mood) {
        if (uid == null) return
        scope.launch {
            runCatching { moodRepository.add(mood) }
                .onFailure { dataWarning = "No se pudo guardar tu ánimo: ${it.toUserMessage()}" }
        }
    }

    /**
     * Registra la recaída en el servidor, que reinicia la racha y conserva el récord.
     *
     * Ese cálculo no se hace en el teléfono a propósito: el récord histórico es lo único
     * que queda intacto tras una recaída, y no puede depender de la hora del dispositivo.
     */
    fun registerRelapse(note: String = "", triggers: List<String> = emptyList()) {
        if (uid == null) return
        scope.launch {
            runCatching {
                relapseRepository.register(note, triggers)
                perfilRepository.refresh()
            }.onFailure { dataWarning = "No se pudo registrar la recaída: ${it.toUserMessage()}" }
        }
    }

    fun markAlertHandled(id: String) {
        scope.launch { runCatching { alertRepository.markHandled(id) } }
    }

    /**
     * Derecho al olvido, hasta donde llega el backend.
     *
     * El servidor solo permite borrar entradas del diario; no hay ninguna ruta que elimine
     * check-ins, ánimos, recaídas ni la cuenta. Se borra lo que sí se puede, se cierra la
     * sesión y se avisa de lo que queda — decir "todo borrado" cuando no lo está sería
     * mentirle a alguien sobre sus propios datos de recaídas.
     */
    fun purgeAllData() {
        val borrables = diaryEntries.map { it.id }
        scope.launch {
            runCatching { borrables.forEach { diaryRepository.delete(it) } }
            // La copia local se va entera, aunque el servidor conserve el historial: quien
            // pulsa esto suele querer que no quede nada en ESTE teléfono.
            runCatching { graph.local.borrarTodo() }
            preferences?.clear()
            notify(
                if (modoLocal) {
                    "Se borró todo lo que había en este teléfono."
                } else {
                    "Se borró tu diario y se cerró la sesión. Tu historial de check-ins " +
                        "sigue en el servidor: para eliminarlo hay que pedirlo directamente."
                },
            )
            signOut()
        }
    }

    // ---- Funciones locales (aún sin ruta en el backend) --------------------------

    fun registerUrgeOvercome() {
        urgeSessionsCompleted += 1
    }

    fun addCapsule(title: String, message: String, createdOn: LocalDate, unlockOn: LocalDate) {
        capsules.add(
            0,
            TimeCapsule(
                id = (nextCapsuleId++).toString(),
                userId = uid.orEmpty(),
                title = title,
                message = message,
                createdOn = createdOn,
                unlockOn = unlockOn,
            ),
        )
    }

    fun addHabit(name: String) {
        habits.add(Habit(id = (nextHabitId++).toString(), userId = uid.orEmpty(), name = name))
    }

    fun removeHabit(habitId: String) {
        habits.removeAll { it.id == habitId }
        habitCompletions.removeAll { it.habitId == habitId }
    }

    /** Alterna el cumplimiento de un hábito ese día, sin duplicar registros. */
    fun toggleHabit(habitId: String, date: LocalDate) {
        val existing = habitCompletions.firstOrNull { it.habitId == habitId && it.date == date }
        if (existing != null) habitCompletions.remove(existing) else habitCompletions.add(HabitCompletion(habitId, date))
    }

    fun addAnchor(title: String, note: String, kind: AnchorKind) {
        val nuevoId = nextAnchorId++
        anchors.add(
            0,
            Anchor(
                id = nuevoId.toString(),
                userId = uid.orEmpty(),
                title = title,
                note = note,
                kind = kind,
                tileSeed = nuevoId,
            ),
        )
        // Ahora hay un motivo más que recordarle: el plan lo puede usar desde hoy.
        reprogramarAvisos()
    }

    fun removeAnchor(anchorId: String) {
        anchors.removeAll { it.id == anchorId }
        reprogramarAvisos()
    }

    /** Persiste localmente lo mínimo para no repetir el cuestionario inicial. */
    suspend fun persistOnboarding() {
        preferences?.saveOnboarding(profile.displayName, profile.addiction)
    }

    suspend fun clearPersisted() {
        preferences?.clear()
    }
}
