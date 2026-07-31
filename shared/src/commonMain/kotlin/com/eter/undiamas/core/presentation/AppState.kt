package com.eter.undiamas.core.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eter.undiamas.core.data.firebase.AiMessageRepositoryImpl
import com.eter.undiamas.core.data.firebase.AuthRepositoryImpl
import com.eter.undiamas.core.data.firebase.CheckInRepositoryImpl
import com.eter.undiamas.core.data.firebase.DiaryRepositoryImpl
import com.eter.undiamas.core.data.firebase.MoodRepositoryImpl
import com.eter.undiamas.core.data.firebase.PerfilRepositoryImpl
import com.eter.undiamas.core.data.firebase.configureFirebaseEmulatorsIfNeeded
import com.eter.undiamas.core.domain.ai.AiProvider
import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.Mood
import com.eter.undiamas.core.domain.model.MoodEntry
import com.eter.undiamas.core.domain.model.TrustedContact
import com.eter.undiamas.core.domain.model.UserProfile
import com.eter.undiamas.core.domain.model.AddictionType
import com.eter.undiamas.core.domain.biometrics.BiometricsProvider
import com.eter.undiamas.core.data.UserPreferences
import com.eter.undiamas.features.anclas.domain.Anchor
import com.eter.undiamas.features.anclas.domain.AnchorKind
import com.eter.undiamas.features.capsulas.domain.TimeCapsule
import com.eter.undiamas.features.capsulas.domain.TimeCapsuleVault
import com.eter.undiamas.features.habitos.domain.Habit
import com.eter.undiamas.features.habitos.domain.HabitCompletion
import com.eter.undiamas.features.habitos.domain.HabitTracker
import com.eter.undiamas.features.estadisticas.domain.RiskPatternDetector
import kotlinx.datetime.LocalDate
import com.eter.undiamas.core.domain.repository.AiMessageRepository
import com.eter.undiamas.core.domain.repository.AuthRepository
import com.eter.undiamas.core.domain.repository.CheckInRepository
import com.eter.undiamas.core.domain.repository.DiaryRepository
import com.eter.undiamas.core.domain.repository.MoodRepository
import com.eter.undiamas.core.domain.repository.PerfilRepository
import com.eter.undiamas.features.calculadora.domain.SavingsCalculator
import com.eter.undiamas.features.checkin.domain.CheckInHistory
import com.eter.undiamas.features.checkin.domain.RiskAssessor
import com.eter.undiamas.features.diario.domain.DiaryEntry
import com.eter.undiamas.features.diario.domain.SentimentAnalyzer
import com.eter.undiamas.features.estadisticas.domain.RiskInsights
import com.eter.undiamas.features.ia.data.CloudFunctionsAiProvider
import com.eter.undiamas.features.ia.domain.AiConversationService
import com.eter.undiamas.features.sobriedad.domain.Milestones
import com.eter.undiamas.features.sobriedad.domain.SobrietyCounter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

private const val SECONDS_PER_DAY = 60L * 60 * 24

/** Preferencias de la pantalla de Configuración. */
data class AppSettings(
    val dailyReminders: Boolean = true,
    val reminderHour: Int = 21,
    val weeklySummary: Boolean = false,
    val darkTheme: Boolean = true,
    val diaryLocked: Boolean = false,
    val stealthMode: Boolean = false,
)

/**
 * Estado compartido entre pantallas. Perfil, check-ins, diario, animos y mensajes de IA
 * son un espejo reactivo (Observer/Flow) de Firestore: [start] abre la sesion y suscribe
 * los listeners; los metodos `register*`/`add*` escriben al repositorio real y dejan que
 * el propio listener actualice el estado (fuente de verdad unica).
 */
class AppState(
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val perfilRepository: PerfilRepository = PerfilRepositoryImpl(),
    private val checkInRepository: CheckInRepository = CheckInRepositoryImpl(),
    private val diaryRepository: DiaryRepository = DiaryRepositoryImpl(),
    private val moodRepository: MoodRepository = MoodRepositoryImpl(),
    private val aiMessageRepository: AiMessageRepository = AiMessageRepositoryImpl(),
    aiProvider: AiProvider = CloudFunctionsAiProvider(),
    /** Lectura de la pulsera (Android). null en iOS o en previews. */
    val biometricsProvider: BiometricsProvider? = null,
    /** Preferencias locales; solo guardan lo justo para no repetir el onboarding. */
    private val preferences: UserPreferences? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** uid de la sesion actual (anonima o con correo); null hasta que [start] termine de autenticar. */
    var uid: String? by mutableStateOf(null)
        private set

    /** true mientras se autentica y se espera la primera lectura de Firestore. */
    var isLoading: Boolean by mutableStateOf(true)
        private set

    /** Error legible de la ultima operacion de login/registro con correo, o null si no hay. */
    var authError: String? by mutableStateOf(null)
        private set

    /** Fallo al arrancar la sesion (sin red, Firebase mal configurado). null si todo fue bien. */
    var startupError: String? by mutableStateOf(null)
        private set

    /**
     * Aviso de que alguna coleccion no se pudo cargar, sin ser fatal.
     * La app sigue usable con lo que si cargo; esto solo lo hace visible.
     */
    var dataWarning: String? by mutableStateOf(null)
        private set

    fun clearDataWarning() {
        dataWarning = null
    }

    /** Sesiones del bunker de 15 minutos completadas hasta el final. */
    var urgeSessionsCompleted: Int by mutableStateOf(0)
        private set

    private var nextCapsuleId = 0
    private var nextHabitId = 0
    private var nextAnchorId = 0

    private var profileJob: Job? = null
    private var checkInsJob: Job? = null
    private var diaryJob: Job? = null
    private var moodJob: Job? = null
    private var aiMessagesJob: Job? = null

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

    /** Mientras sea false la app muestra el cuestionario inicial en vez del resto de pantallas. */
    var isOnboarded: Boolean by mutableStateOf(false)
        private set

    val checkIns = mutableStateListOf<CheckInEntry>()
    val diaryEntries = mutableStateListOf<DiaryEntry>()
    val aiMessages = mutableStateListOf<AiMessage>()
    val moodEntries = mutableStateListOf<MoodEntry>()

    // Estas cuatro aun no tienen coleccion en Firestore, asi que viven en memoria y se
    // pierden al cerrar la app. Migrarlas es el siguiente paso de la Fase 3.
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
    val aiConversationService = AiConversationService(aiProvider)

    /** La pantalla raíz reemplaza esto por una función que muestra un snackbar real. */
    var onNotify: (String) -> Unit = {}

    fun notify(message: String) = onNotify(message)

    /** Autentica anonimamente y suscribe perfil + check-ins reales de Firestore. Llamar una sola vez. */
    fun start() {
        scope.launch {
            startupError = null
            // Sin runCatching, cualquier fallo de red o de configuracion de Firebase se
            // propaga como excepcion no capturada y tumba la app al abrirla. En una app de
            // recuperacion eso es inaceptable: es preferible entrar en modo degradado y
            // ofrecer reintentar.
            runCatching {
                configureFirebaseEmulatorsIfNeeded()
                authRepository.signInAnonymously()
            }.onSuccess { newUid ->
                subscribeToUid(newUid)
            }.onFailure { error ->
                startupError = error.message ?: "No se pudo conectar con el servidor."
                isLoading = false
            }
        }
    }

    /** Reintento manual del arranque tras un fallo de conexion. */
    fun retryStart() {
        isLoading = true
        start()
    }

    /**
     * Vincula la sesion anonima actual (con todos sus datos) a un correo/contraseña.
     * El uid no cambia, asi que no hace falta resuscribir los listeners de perfil/check-ins.
     */
    fun linkAccountWithEmail(email: String, password: String) {
        scope.launch {
            runCatching { authRepository.linkAnonymousWithEmail(email, password) }
                .onSuccess {
                    authError = null
                    notify("Cuenta vinculada. Tus datos seguirán contigo si cambias de dispositivo.")
                }
                .onFailure { authError = it.message ?: "No se pudo vincular la cuenta." }
        }
    }

    /** Inicia sesion con correo/contraseña y migra la sesion a ese uid. */
    fun signInWithEmail(email: String, password: String) {
        scope.launch {
            runCatching { authRepository.signInWithEmail(email, password) }
                .onSuccess { newUid ->
                    authError = null
                    isOnboarded = false
                    subscribeToUid(newUid)
                }
                .onFailure { authError = it.message ?: "No se pudo iniciar sesión." }
        }
    }

    /** Cierra la sesion actual y vuelve a abrir una sesion anonima nueva. */
    fun signOut() {
        scope.launch {
            authRepository.signOut()
            isOnboarded = false
            subscribeToUid(authRepository.signInAnonymously())
        }
    }

    private fun subscribeToUid(newUid: String) {
        profileJob?.cancel()
        checkInsJob?.cancel()
        diaryJob?.cancel()
        moodJob?.cancel()
        aiMessagesJob?.cancel()
        checkIns.clear()
        diaryEntries.clear()
        moodEntries.clear()
        aiMessages.clear()
        isLoading = true
        uid = newUid
        profile = profile.copy(userId = newUid)

        // Cada listener va con .catch: un fallo de permisos o de red en CUALQUIERA de ellos
        // se propagaria como excepcion no capturada dentro de la corrutina y mataria el
        // proceso. Que falle el diario no puede tumbar el protocolo de emergencia.
        profileJob = scope.launch {
            perfilRepository.observe(newUid)
                .catch { error ->
                    dataWarning = "No se pudo cargar tu perfil: ${error.message}"
                    // Sin esto la app se queda girando para siempre si el perfil falla.
                    isLoading = false
                }
                .collect { loaded ->
                    if (loaded != null) {
                        profile = loaded
                        isOnboarded = true
                    }
                    isLoading = false
                }
        }
        checkInsJob = scope.launch {
            checkInRepository.observeRecent(newUid)
                .catch { error -> dataWarning = "No se pudieron cargar tus check-ins: ${error.message}" }
                .collect { entries ->
                    checkIns.clear()
                    checkIns.addAll(entries)
                }
        }
        diaryJob = scope.launch {
            diaryRepository.observeRecent(newUid)
                .catch { error -> dataWarning = "No se pudo cargar tu diario: ${error.message}" }
                .collect { entries ->
                    diaryEntries.clear()
                    diaryEntries.addAll(entries)
                }
        }
        moodJob = scope.launch {
            moodRepository.observeRecent(newUid)
                .catch { error -> dataWarning = "No se pudieron cargar tus animos: ${error.message}" }
                .collect { entries ->
                    moodEntries.clear()
                    moodEntries.addAll(entries)
                }
        }
        aiMessagesJob = scope.launch {
            aiMessageRepository.observeRecent(newUid)
                .catch { error -> dataWarning = "No se pudo cargar la conversacion: ${error.message}" }
                .collect { entries ->
                    aiMessages.clear()
                    aiMessages.addAll(entries)
                }
        }
    }

    fun updateProfile(update: (UserProfile) -> UserProfile) {
        profile = update(profile)
        persistProfile()
    }

    fun updateSettings(update: (AppSettings) -> AppSettings) {
        settings = update(settings)
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
    ) {
        val now = Clock.System.now()
        profile = profile.copy(
            displayName = displayName.ifBlank { "Amigo/a" },
            sobrietyStartDate = Instant.fromEpochSeconds(now.epochSeconds - daysSober * SECONDS_PER_DAY),
            recordStreakSeconds = recordDays * SECONDS_PER_DAY,
            previousDailyExpense = previousDailyExpense,
            trustedContact = if (contactName.isBlank()) null else TrustedContact(contactName, contactPhone),
            addiction = addiction,
        )
        isOnboarded = true
        persistProfile()
    }

    /**
     * Restaura lo guardado localmente para pintar algo util antes de que Firestore responda.
     * Cuando llega el perfil remoto, el listener lo sobreescribe: Firestore manda.
     */
    fun restoreFrom(completed: Boolean, savedName: String, savedAddiction: AddictionType?) {
        if (!completed) return
        profile = profile.copy(
            displayName = savedName.ifBlank { profile.displayName },
            addiction = savedAddiction ?: profile.addiction,
        )
        isOnboarded = true
    }

    private fun persistProfile() {
        val currentUid = uid ?: return
        scope.launch { perfilRepository.save(currentUid, profile) }
    }

    /** Escribe el check-in en Firestore; [checkIns] se actualiza solo via el listener de [start]. */
    fun registerCheckIn(entry: CheckInEntry) {
        val currentUid = uid ?: return
        scope.launch { checkInRepository.add(currentUid, entry) }
    }

    /** Escribe la entrada en Firestore; [diaryEntries] se actualiza solo via el listener de [subscribeToUid]. */
    fun addDiaryEntry(entry: DiaryEntry) {
        val currentUid = uid ?: return
        scope.launch { diaryRepository.add(currentUid, entry) }
    }

    /** Escribe el animo en Firestore; [moodEntries] se actualiza solo via el listener de [subscribeToUid]. */
    fun registerMood(mood: Mood) {
        val currentUid = uid ?: return
        val entry = MoodEntry(id = "", userId = currentUid, mood = mood, registeredAt = Clock.System.now())
        scope.launch { moodRepository.add(currentUid, entry) }
    }

    /** Escribe el mensaje en Firestore; [aiMessages] se actualiza solo via el listener de [subscribeToUid]. */
    fun registerAiMessage(message: AiMessage) {
        val currentUid = uid ?: return
        scope.launch { aiMessageRepository.add(currentUid, message) }
    }

    /** Derecho al olvido: borra todos los datos reales del usuario y vuelve al cuestionario inicial. */
    fun purgeAllData() {
        val currentUid = uid
        diaryEntries.clear()
        aiMessages.clear()
        moodEntries.clear()
        profile = UserProfile(
            userId = currentUid.orEmpty(),
            displayName = "",
            sobrietyStartDate = Clock.System.now(),
        )
        settings = AppSettings()
        isOnboarded = false
        if (currentUid != null) {
            scope.launch {
                perfilRepository.delete(currentUid)
                checkInRepository.deleteAll(currentUid)
                diaryRepository.deleteAll(currentUid)
                moodRepository.deleteAll(currentUid)
                aiMessageRepository.deleteAll(currentUid)
            }
        }
    }

    // ---- Funciones locales (aun sin coleccion en Firestore) -------------------------

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

    /** Alterna el cumplimiento de un habito ese dia, sin duplicar registros. */
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
    }

    fun removeAnchor(anchorId: String) {
        anchors.removeAll { it.id == anchorId }
    }

    /** Persiste localmente lo minimo para no repetir el cuestionario inicial. */
    suspend fun persistOnboarding() {
        preferences?.saveOnboarding(profile.displayName, profile.addiction)
    }

    suspend fun clearPersisted() {
        preferences?.clear()
    }

}
