package com.eter.undiamas.core.data.api

import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.Mood
import com.eter.undiamas.core.domain.model.MoodEntry
import com.eter.undiamas.core.domain.model.RelapseEvent
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.domain.model.SavingsGoal
import com.eter.undiamas.core.domain.model.SupportRole
import com.eter.undiamas.core.domain.model.Trigger
import com.eter.undiamas.core.domain.model.TrustedContact
import com.eter.undiamas.core.domain.model.UserProfile
import com.eter.undiamas.features.diario.domain.DiaryEntry
import kotlin.time.Clock
import kotlin.time.Instant

/*
 * Traduccion entre el JSON del backend y el modelo de dominio.
 *
 * El vocabulario coincide: el servidor ya habla VERDE/AMARILLO/ROJO, MUY_MAL..MUY_BIEN y
 * ESTRES..TRABAJO, los mismos nombres que los enums de Kotlin. Aun asi nada se convierte a
 * ciegas: un valor desconocido cae al mas benigno en vez de reventar. Inventar un ROJO por
 * un dato corrupto dispararia el protocolo de emergencia de alguien sin motivo.
 */

// ---- Fechas ---------------------------------------------------------------------

/** El backend manda RFC3339 con offset ("2026-08-03T11:44:48.898645-06:00"). */
fun String?.toInstantOrNull(): Instant? =
    this?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }

fun String?.toInstantOrNow(): Instant = toInstantOrNull() ?: Clock.System.now()

fun Instant.toApiString(): String = toString()

// ---- Enumerados -----------------------------------------------------------------

fun String?.toRiskLevel(): RiskLevel =
    RiskLevel.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: RiskLevel.VERDE

fun String?.toMood(): Mood =
    Mood.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: Mood.NEUTRAL

fun List<String>.toTriggers(): Set<Trigger> =
    mapNotNull { raw -> Trigger.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } }
        .toSet()

fun Set<Trigger>.toApiTriggers(): List<String> = map { it.name }

fun String?.toSupportRole(): SupportRole =
    SupportRole.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: SupportRole.FAMILIAR


// ---- Check-ins ------------------------------------------------------------------

fun CheckInDto.toDomain(userId: String): CheckInEntry = CheckInEntry(
    id = id,
    userId = userId,
    answeredAt = createdAt.toInstantOrNow(),
    answers = answers,
    riskLevel = riskLevel.toRiskLevel(),
    triggers = triggers.toTriggers(),
    urgeIntensity = cravingLevel,
    note = note,
)

/**
 * [mood] va aparte porque el cuestionario de la app no lo pregunta: el animo se registra
 * en su propia pantalla. Se manda NEUTRAL en vez de deducirlo del nivel de riesgo, que
 * seria inventar un dato que la persona no dio.
 */
fun CheckInEntry.toRequest(mood: Mood = Mood.NEUTRAL): CreateCheckInRequest = CreateCheckInRequest(
    riskLevel = riskLevel.name,
    cravingLevel = urgeIntensity,
    mood = mood.name,
    triggers = triggers.toApiTriggers(),
    note = note,
    answers = answers,
)

// ---- Diario, animo, IA, recaidas ------------------------------------------------

fun JournalDto.toDomain(userId: String): DiaryEntry = DiaryEntry(
    id = id,
    userId = userId,
    createdAt = createdAt.toInstantOrNow(),
    text = content,
)

fun MoodDto.toDomain(userId: String): MoodEntry = MoodEntry(
    id = id,
    userId = userId,
    mood = mood.toMood(),
    registeredAt = createdAt.toInstantOrNow(),
)

fun RelapseDto.toDomain(userId: String): RelapseEvent = RelapseEvent(
    id = id,
    userId = userId,
    occurredAt = createdAt.toInstantOrNow(),
    notes = note.takeIf { it.isNotBlank() },
)

// ---- Contactos y perfil ---------------------------------------------------------

fun ContactDto.toDomain(): TrustedContact = TrustedContact(
    name = nombre,
    phone = telefono,
    role = rol.toSupportRole(),
)

fun TrustedContact.toDto(): ContactDto = ContactDto(
    nombre = name,
    telefono = phone,
    rol = role.name,
)

/**
 * El perfil de la app se arma con DOS rutas: `/v1/users/me` (nombre, por que, contactos,
 * record) y `/v1/tracker` (fecha de inicio y gasto diario). Se juntan aqui para que el
 * resto de la app siga viendo un solo [UserProfile].
 *
 * `addiction` y `savingsGoal` no existen todavia en el backend: se conservan de lo que ya
 * hubiera en memoria o en preferencias locales, en vez de borrarlos en cada refresco.
 */
fun buildProfile(
    user: UserDto,
    tracker: TrackerDto,
    previous: UserProfile? = null,
): UserProfile {
    val contacts = user.contactosEmergencia.map { it.toDomain() }
    return UserProfile(
        userId = user.id,
        displayName = user.displayName,
        // Sin fecha en el servidor, la racha empieza ahora: es lo que el propio backend
        // reporta como rachaSegundos = 0, no un valor inventado por la app.
        sobrietyStartDate = tracker.startDate.toInstantOrNow(),
        recordStreakSeconds = user.recordRachaSegundos,
        previousDailyExpense = tracker.dailySavingsRate,
        // El primero de la lista es el contacto de confianza: es el que usa el protocolo
        // de emergencia, y el orden es significativo para el backend.
        trustedContact = contacts.firstOrNull(),
        supportNetwork = contacts.drop(1),
        personalWhy = user.porQuePersonal,
        savingsGoal = previous?.savingsGoal,
        addiction = previous?.addiction,
    )
}

/** Contactos en el orden que espera el backend: el de confianza siempre primero. */
fun UserProfile.toContactDtos(): List<ContactDto> =
    (listOfNotNull(trustedContact) + supportNetwork).map { it.toDto() }

/** El objetivo de ahorro es local por ahora; se deja explicito para no olvidarlo. */
fun UserProfile.withSavingsGoal(goal: SavingsGoal?): UserProfile = copy(savingsGoal = goal)
