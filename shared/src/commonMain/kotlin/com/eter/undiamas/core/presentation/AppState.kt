package com.eter.undiamas.core.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eter.undiamas.core.domain.ai.AiProvider
import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.TrustedContact
import com.eter.undiamas.core.domain.model.UserProfile
import com.eter.undiamas.features.calculadora.domain.SavingsCalculator
import com.eter.undiamas.features.checkin.domain.CheckInHistory
import com.eter.undiamas.features.checkin.domain.RiskAssessor
import com.eter.undiamas.features.diario.domain.DiaryEntry
import com.eter.undiamas.features.ia.data.MockAiProvider
import com.eter.undiamas.features.ia.domain.AiConversationService
import com.eter.undiamas.features.sobriedad.domain.SobrietyCounter
import kotlin.time.Clock
import kotlin.time.Instant

private const val SECONDS_PER_DAY = 60L * 60 * 24

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

    /** Mientras sea false la app muestra el cuestionario inicial en vez del resto de pantallas. */
    var isOnboarded: Boolean by mutableStateOf(false)
        private set

    val checkIns = mutableStateListOf<CheckInEntry>()
    val diaryEntries = mutableStateListOf<DiaryEntry>()
    val aiMessages = mutableStateListOf<AiMessage>()

    val sobrietyCounter = SobrietyCounter()
    val savingsCalculator = SavingsCalculator()
    val riskAssessor = RiskAssessor()
    val checkInHistory = CheckInHistory()
    val aiConversationService = AiConversationService(aiProvider)

    private var nextCheckInId = 0
    private var nextDiaryId = 0

    /** La pantalla raíz reemplaza esto por una función que muestra un snackbar real. */
    var onNotify: (String) -> Unit = {}

    fun notify(message: String) = onNotify(message)

    fun updateProfile(update: (UserProfile) -> UserProfile) {
        profile = update(profile)
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
}
