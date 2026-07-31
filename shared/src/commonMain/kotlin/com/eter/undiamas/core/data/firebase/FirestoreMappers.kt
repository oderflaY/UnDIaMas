package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.AiMessageRole
import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.Mood
import com.eter.undiamas.core.domain.model.MoodEntry
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.domain.model.SupportRole
import com.eter.undiamas.core.domain.model.Trigger
import com.eter.undiamas.core.domain.model.TrustedContact
import com.eter.undiamas.core.domain.model.UserProfile
import com.eter.undiamas.features.diario.domain.DiaryEntry
import kotlin.time.Instant

fun UserProfile.toUsuarioDoc(): UsuarioDoc {
    val contactos = listOfNotNull(trustedContact) + supportNetwork
    return UsuarioDoc(
        alias = displayName,
        fechaInicioSobriedad = sobrietyStartDate.epochSeconds,
        recordRachaSegundos = recordStreakSeconds,
        gastoDiarioEstimado = previousDailyExpense,
        contactosEmergencia = contactos.map { it.toDoc() },
        porQuePersonal = personalWhy,
    )
}

fun UsuarioDoc.toUserProfile(uid: String, existing: UserProfile? = null): UserProfile {
    val contactos = contactosEmergencia.map { it.toDomain() }
    return UserProfile(
        userId = uid,
        displayName = alias,
        sobrietyStartDate = Instant.fromEpochSeconds(fechaInicioSobriedad),
        recordStreakSeconds = recordRachaSegundos,
        previousDailyExpense = gastoDiarioEstimado,
        trustedContact = contactos.firstOrNull(),
        supportNetwork = contactos.drop(1),
        personalWhy = porQuePersonal,
        savingsGoal = existing?.savingsGoal,
    )
}

private fun TrustedContact.toDoc() = ContactoEmergenciaDoc(nombre = name, telefono = phone, rol = role.name)

private fun ContactoEmergenciaDoc.toDomain() = TrustedContact(
    name = nombre,
    phone = telefono,
    role = runCatching { SupportRole.valueOf(rol) }.getOrDefault(SupportRole.FAMILIAR),
)

fun CheckInEntry.toCheckInDoc(): CheckInDoc = CheckInDoc(
    nivelCraving = urgeIntensity,
    estadoAnimo = answers["estado_animo"] ?: "",
    gatillos = triggers.map { it.name },
    nivelRiesgo = riskLevel.name,
    fechaHora = answeredAt.epochSeconds,
    nota = note,
    respuestas = answers,
)

fun CheckInDoc.toCheckInEntry(id: String, uid: String): CheckInEntry = CheckInEntry(
    id = id,
    userId = uid,
    answeredAt = Instant.fromEpochSeconds(fechaHora),
    answers = respuestas.ifEmpty { if (estadoAnimo.isBlank()) emptyMap() else mapOf("estado_animo" to estadoAnimo) },
    riskLevel = runCatching { RiskLevel.valueOf(nivelRiesgo) }.getOrDefault(RiskLevel.VERDE),
    triggers = gatillos.mapNotNull { runCatching { Trigger.valueOf(it) }.getOrNull() }.toSet(),
    urgeIntensity = nivelCraving,
    note = nota,
)

fun DiaryEntry.toDiaryEntryDoc(): DiaryEntryDoc = DiaryEntryDoc(contenido = text, fecha = createdAt.epochSeconds)

fun DiaryEntryDoc.toDiaryEntry(id: String, uid: String): DiaryEntry = DiaryEntry(
    id = id,
    userId = uid,
    createdAt = Instant.fromEpochSeconds(fecha),
    text = contenido,
)

fun MoodEntry.toMoodEntryDoc(): MoodEntryDoc = MoodEntryDoc(animo = mood.name, fecha = registeredAt.epochSeconds)

fun MoodEntryDoc.toMoodEntry(id: String, uid: String): MoodEntry = MoodEntry(
    id = id,
    userId = uid,
    mood = runCatching { Mood.valueOf(animo) }.getOrDefault(Mood.NEUTRAL),
    registeredAt = Instant.fromEpochSeconds(fecha),
)

fun AiMessage.toAiMessageDoc(): AiMessageDoc = AiMessageDoc(
    rol = role.name,
    contenido = content,
    nivelRiesgoContexto = riskLevelContext?.name,
    fecha = sentAt.epochSeconds,
)

fun AiMessageDoc.toAiMessage(id: String, uid: String): AiMessage = AiMessage(
    id = id,
    userId = uid,
    role = runCatching { AiMessageRole.valueOf(rol) }.getOrDefault(AiMessageRole.ASISTENTE),
    content = contenido,
    riskLevelContext = nivelRiesgoContexto?.let { runCatching { RiskLevel.valueOf(it) }.getOrNull() },
    sentAt = Instant.fromEpochSeconds(fecha),
)
