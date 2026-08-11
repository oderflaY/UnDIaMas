package com.eter.undiamas.features.sobriedad.domain

import com.eter.undiamas.core.domain.model.UserProfile
import kotlin.time.Instant

class SobrietyCounter {
    /** Nunca negativa: una fecha de inicio en el futuro es un dato mal metido, no una deuda. */
    fun currentStreakSeconds(profile: UserProfile, now: Instant): Long =
        (now.epochSeconds - profile.sobrietyStartDate.epochSeconds).coerceAtLeast(0)

    /**
     * Récord histórico.
     *
     * Si la racha de ahora ya superó al récord guardado, el récord **es** la racha de ahora:
     * no hay que esperar a una recaída para reconocerlo. Enseñar "42 días · mejor racha 30"
     * sería absurdo, y en esta app además desalentador.
     */
    fun recordStreakSeconds(profile: UserProfile, now: Instant): Long =
        maxOf(profile.recordStreakSeconds, currentStreakSeconds(profile, now))

    /** true cuando la racha actual es la mejor que ha tenido esta persona. */
    fun isPersonalBest(profile: UserProfile, now: Instant): Boolean =
        profile.recordStreakSeconds > 0 &&
            currentStreakSeconds(profile, now) >= profile.recordStreakSeconds

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
