package com.eter.undiamas.core.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse

/**
 * Todas las rutas del backend, en un solo sitio.
 *
 * Ninguna funcion recibe el id del usuario: el servidor lo saca del token y rechaza con
 * 400 cualquier `userId` que llegue en el cuerpo. Es justo lo que impide que un fallo de
 * la app deje a alguien leyendo el historial de recaidas de otra persona.
 */
class UnDiaMasApi(
    val http: HttpClient,
    private val tokenStore: TokenStore,
) {

    // ---- Sesion ----------------------------------------------------------------

    /** Registro. No lleva token: es la unica forma de conseguir el primero. */
    suspend fun register(email: String, password: String, displayName: String): AuthResponse =
        http.post("/v1/auth/register") {
            setBody(RegisterRequest(email.trim(), password, displayName.trim()))
        }.body<AuthResponse>().also { it.persist() }

    suspend fun login(email: String, password: String): AuthResponse =
        http.post("/v1/auth/login") {
            setBody(AuthRequest(email.trim(), password))
        }.body<AuthResponse>().also { it.persist() }

    /**
     * Cierra la sesion en todos los dispositivos.
     *
     * Los tokens locales se borran pase lo que pase: si el servidor no contesta, dejar el
     * par guardado significaria que "cerrar sesion" no cerro nada en este telefono, que es
     * exactamente lo que alguien espera cuando lo pulsa por privacidad.
     */
    suspend fun logout() {
        runCatching { http.post("/v1/auth/logout") }
        tokenStore.clear()
        http.forgetCachedToken()
    }

    /** Guarda el par recien emitido y descarta el que el cliente tuviera cacheado. */
    private suspend fun AuthResponse.persist() {
        tokenStore.save(Tokens(accessToken, refreshToken))
        http.forgetCachedToken()
    }

    // ---- Perfil ----------------------------------------------------------------

    suspend fun me(): UserDto = http.get("/v1/users/me").body()

    suspend fun updateMe(displayName: String, personalWhy: String): UserDto =
        http.patch("/v1/users/me") {
            setBody(UpdateUserRequest(displayName, personalWhy))
        }.body()

    suspend fun updateEmergencyContacts(contacts: List<ContactDto>): UserDto =
        http.put("/v1/users/me/emergency-contacts") {
            setBody(EmergencyContactsRequest(contacts))
        }.body()

    // ---- Racha y ahorro --------------------------------------------------------

    suspend fun tracker(): TrackerDto = http.get("/v1/tracker").body()

    suspend fun updateTracker(
        startDateIso: String,
        dailySavingsRate: Double,
        currency: String = "MXN",
    ): TrackerDto = http.patch("/v1/tracker") {
        setBody(UpdateTrackerRequest(startDateIso, dailySavingsRate, currency))
    }.body()

    // ---- Check-ins -------------------------------------------------------------

    suspend fun checkIns(limit: Int = 100): List<CheckInDto> =
        http.get("/v1/check-ins") { parameter("limit", limit) }
            .body<ItemsDto<CheckInDto>>().items

    suspend fun createCheckIn(request: CreateCheckInRequest): CheckInDto =
        http.post("/v1/check-ins") { setBody(request) }.body()

    // ---- Recaidas --------------------------------------------------------------

    suspend fun relapses(): List<RelapseDto> =
        http.get("/v1/relapses").body<ItemsDto<RelapseDto>>().items

    suspend fun createRelapse(note: String, triggers: List<String>): RelapseDto =
        http.post("/v1/relapses") { setBody(CreateRelapseRequest(note, triggers)) }.body()

    // ---- Semaforo --------------------------------------------------------------

    suspend fun trafficLight(): TrafficLightStateDto = http.get("/v1/traffic-light").body()

    suspend fun saveTrafficLight(request: CreateTrafficLightRequest): TrafficLightResult =
        http.post("/v1/traffic-light") { setBody(request) }.body()

    // ---- Diario y animo --------------------------------------------------------

    suspend fun journal(): List<JournalDto> =
        http.get("/v1/journal").body<ItemsDto<JournalDto>>().items

    suspend fun createJournal(content: String): JournalDto =
        http.post("/v1/journal") { setBody(CreateJournalRequest(content)) }.body()

    suspend fun deleteJournal(id: String): HttpResponse = http.delete("/v1/journal/$id")

    suspend fun moodLogs(): List<MoodDto> =
        http.get("/v1/mood-logs").body<ItemsDto<MoodDto>>().items

    suspend fun createMood(mood: String): MoodDto =
        http.post("/v1/mood-logs") { setBody(CreateMoodRequest(mood)) }.body()

    // ---- Alertas ---------------------------------------------------------------

    suspend fun alerts(): List<AlertDto> =
        http.get("/v1/alerts").body<ItemsDto<AlertDto>>().items

    suspend fun markAlertHandled(id: String, handled: Boolean = true): AlertDto =
        http.patch("/v1/alerts/$id") { setBody(HandleAlertRequest(handled)) }.body()

    // ---- Recordatorio ----------------------------------------------------------

    suspend fun reminder(): ReminderDto = http.get("/v1/reminders").body()

    suspend fun updateReminder(reminder: ReminderDto): ReminderDto =
        http.put("/v1/reminders") { setBody(reminder) }.body()

    // ---- Estadisticas ----------------------------------------------------------

    suspend fun riskTrends(days: Int = 30): RiskTrendsDto =
        http.get("/v1/stats/risk-trends") { parameter("days", days) }.body()

    // ---- IA --------------------------------------------------------------------

    /**
     * Solo se manda el texto. El historial y el nivel de riesgo los lee el servidor de la
     * base con el id del token, asi que la conversacion sobrevive a reinstalar la app.
     */
    suspend fun chat(prompt: String): ChatResponse =
        http.post("/v1/ai/chat") { setBody(ChatRequest(prompt)) }.body()

    suspend fun aiMessages(): List<AiMessageDto> =
        http.get("/v1/ai/messages").body<ItemsDto<AiMessageDto>>().items

    // ---- Comunidad -------------------------------------------------------------

    /**
     * Muro de historias.
     *
     * [orden] es `racha` (por defecto), `reciente` o `utiles`. [cursor] viene de la página
     * anterior; null para pedir la primera.
     */
    suspend fun historias(
        orden: String = "racha",
        limite: Int = 20,
        cursor: String? = null,
    ): PaginaHistoriasDto = http.get("/v1/community/stories") {
        parameter("sort", orden)
        parameter("limit", limite)
        if (cursor != null) parameter("cursor", cursor)
    }.body()

    suspend fun crearHistoria(request: CrearHistoriaRequest): HistoriaDto =
        http.post("/v1/community/stories") { setBody(request) }.body()

    suspend fun borrarHistoria(id: String): HttpResponse =
        http.delete("/v1/community/stories/$id")

    suspend fun marcarHistoriaUtil(id: String, util: Boolean): HistoriaDto =
        http.put("/v1/community/stories/$id/useful") { setBody(UtilRequest(util)) }.body()

    suspend fun reportarHistoria(id: String, request: ReporteRequest): HttpResponse =
        http.post("/v1/community/stories/$id/reports") { setBody(request) }

    /** Oculta al autor de esa historia para quien llama. */
    suspend fun bloquearAutorDe(id: String): HttpResponse =
        http.post("/v1/community/stories/$id/block-author")

    suspend fun perfilDeComunidad(): PerfilComunidadDto =
        http.get("/v1/community/me").body()

    suspend fun guardarAlias(alias: String): PerfilComunidadDto =
        http.put("/v1/community/me") { setBody(AliasRequest(alias)) }.body()

    // ---- Terapeuta -------------------------------------------------------------

    suspend fun therapists(): List<TherapistDto> =
        http.get("/v1/me/therapists").body<ItemsDto<TherapistDto>>().items

    suspend fun sessions(): List<SessionDto> =
        http.get("/v1/me/sessions").body<ItemsDto<SessionDto>>().items
}
