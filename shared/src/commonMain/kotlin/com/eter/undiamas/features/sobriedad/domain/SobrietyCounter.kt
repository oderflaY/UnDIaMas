package com.eter.undiamas.features.sobriedad.domain

import com.eter.undiamas.core.domain.model.UserProfile
import kotlinx.datetime.Instant

class SobrietyCounter {
    fun currentStreakSeconds(profile: UserProfile, now: Instant): Long =
        now.epochSeconds - profile.sobrietyStartDate.epochSeconds

    fun registerRelapse(profile: UserProfile, relapseAt: Instant): UserProfile {
        val streakAtRelapse = relapseAt.epochSeconds - profile.sobrietyStartDate.epochSeconds
        return profile.copy(
            sobrietyStartDate = relapseAt,
            recordStreakSeconds = maxOf(profile.recordStreakSeconds, streakAtRelapse),
        )
    }

    fun motivationalMessage(currentStreakSeconds: Long, recordStreakSeconds: Long): String =
        if (recordStreakSeconds > 0 && currentStreakSeconds >= recordStreakSeconds) {
            "¡Nuevo récord personal! Sigue así, un día a la vez."
        } else {
            "Cada día cuenta. Vas por buen camino hacia tu récord."
        }
}
