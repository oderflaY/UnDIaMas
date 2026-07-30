package com.eter.undiamas.features.sobriedad.domain

import com.eter.undiamas.core.domain.model.UserProfile
import kotlinx.datetime.Instant

class SobrietyCounter {
    fun currentStreakSeconds(profile: UserProfile, now: Instant): Long = TODO()

    fun registerRelapse(profile: UserProfile, relapseAt: Instant): UserProfile = TODO()

    fun motivationalMessage(currentStreakSeconds: Long, recordStreakSeconds: Long): String = TODO()
}
