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
import com.eter.undiamas.features.checkin.domain.RiskAssessor
import com.eter.undiamas.features.diario.domain.DiaryEntry
import com.eter.undiamas.features.ia.data.MockAiProvider
import com.eter.undiamas.features.ia.domain.AiConversationService
import com.eter.undiamas.features.sobriedad.domain.SobrietyCounter
import kotlinx.datetime.Clock

/**
 * Estado en memoria compartido entre pantallas mientras Fase 3 conecta Auth/Firestore.
 * No persiste entre reinicios; se reemplazará por repositorios reales sobre Firebase.
 */
class AppState(aiProvider: AiProvider = MockAiProvider()) {
    var profile: UserProfile by mutableStateOf(
        UserProfile(
            userId = "demo-user",
            displayName = "Alex",
            sobrietyStartDate = Clock.System.now(),
            previousDailyExpense = 80.0,
            trustedContact = TrustedContact(name = "Mar", phone = "55 0000 0000"),
        ),
    )
        private set

    val checkIns = mutableStateListOf<CheckInEntry>()
    val diaryEntries = mutableStateListOf<DiaryEntry>()
    val aiMessages = mutableStateListOf<AiMessage>()

    val sobrietyCounter = SobrietyCounter()
    val savingsCalculator = SavingsCalculator()
    val riskAssessor = RiskAssessor()
    val aiConversationService = AiConversationService(aiProvider)

    private var nextCheckInId = 0
    private var nextDiaryId = 0

    fun updateProfile(update: (UserProfile) -> UserProfile) {
        profile = update(profile)
    }

    fun registerCheckIn(entry: CheckInEntry) {
        checkIns.add(0, entry.copy(id = (nextCheckInId++).toString()))
    }

    fun addDiaryEntry(entry: DiaryEntry) {
        diaryEntries.add(0, entry.copy(id = (nextDiaryId++).toString()))
    }
}
