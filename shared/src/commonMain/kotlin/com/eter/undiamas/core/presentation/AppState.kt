package com.eter.undiamas.core.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eter.undiamas.core.domain.ai.AiProvider
import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.Mood
import com.eter.undiamas.core.domain.model.MoodEntry
import com.eter.undiamas.core.domain.model.TrustedContact
import com.eter.undiamas.core.domain.model.UserProfile
import com.eter.undiamas.features.calculadora.domain.SavingsCalculator
import com.eter.undiamas.features.checkin.domain.CheckInHistory
import com.eter.undiamas.features.checkin.domain.RiskAssessor
import com.eter.undiamas.features.diario.domain.DiaryEntry
import com.eter.undiamas.features.diario.domain.SentimentAnalyzer
import com.eter.undiamas.features.estadisticas.domain.RiskInsights
import com.eter.undiamas.features.ia.data.MockAiProvider
import com.eter.undiamas.features.ia.domain.AiConversationService
import com.eter.undiamas.features.sobriedad.domain.Milestones
import com.eter.undiamas.features.sobriedad.domain.SobrietyCounter
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
 * Estado en memoria compartido entre pantallas mientras Fase 3 conecta Auth/Firestore.
 * No persiste entre reinicios; se reemplazará por repositorios reales sobre Firebase.
 */
class AppState(aiProvider: AiProvider = MockAiProvider()) {
    var profile: UserProfile by mutableStateOf(
        UserProfile(
            userId = "demo-user",
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

    val sobrietyCounter = SobrietyCounter()
    val savingsCalculator = SavingsCalculator()
    val riskAssessor = RiskAssessor()
    val checkInHistory = CheckInHistory()
    val riskInsights = RiskInsights()
    val milestones = Milestones()
    val sentimentAnalyzer = SentimentAnalyzer()
    val aiConversationService = AiConversationService(aiProvider)

    private var nextCheckInId = 0
    private var nextDiaryId = 0
    private var nextMoodId = 0

    /** La pantalla raíz reemplaza esto por una función que muestra un snackbar real. */
    var onNotify: (String) -> Unit = {}

    fun notify(message: String) = onNotify(message)

    fun updateProfile(update: (UserProfile) -> UserProfile) {
        profile = update(profile)
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
    ) {
        val now = Clock.System.now()
        profile = profile.copy(
            displayName = displayName.ifBlank { "Amigo/a" },
            sobrietyStartDate = Instant.fromEpochSeconds(now.epochSeconds - daysSober * SECONDS_PER_DAY),
            recordStreakSeconds = recordDays * SECONDS_PER_DAY,
            previousDailyExpense = previousDailyExpense,
            trustedContact = if (contactName.isBlank()) null else TrustedContact(contactName, contactPhone),
        )
        isOnboarded = true
    }

    fun registerCheckIn(entry: CheckInEntry) {
        checkIns.add(0, entry.copy(id = (nextCheckInId++).toString()))
    }

    fun addDiaryEntry(entry: DiaryEntry) {
        diaryEntries.add(0, entry.copy(id = (nextDiaryId++).toString()))
    }

    fun registerMood(mood: Mood) {
        moodEntries.add(
            0,
            MoodEntry(
                id = (nextMoodId++).toString(),
                userId = profile.userId,
                mood = mood,
                registeredAt = Clock.System.now(),
            ),
        )
    }

    /** Derecho al olvido: borra todo rastro local y devuelve la app al cuestionario inicial. */
    fun purgeAllData() {
        checkIns.clear()
        diaryEntries.clear()
        aiMessages.clear()
        moodEntries.clear()
        nextCheckInId = 0
        nextDiaryId = 0
        nextMoodId = 0
        profile = UserProfile(
            userId = "demo-user",
            displayName = "",
            sobrietyStartDate = Clock.System.now(),
        )
        settings = AppSettings()
        isOnboarded = false
    }
}
