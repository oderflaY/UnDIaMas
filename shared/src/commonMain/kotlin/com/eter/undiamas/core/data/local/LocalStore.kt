package com.eter.undiamas.core.data.local

import androidx.sqlite.execSQL
import com.eter.undiamas.core.data.api.apiJson
import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.Mood
import com.eter.undiamas.core.domain.model.MoodEntry
import com.eter.undiamas.core.domain.model.RelapseEvent
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.domain.model.Trigger
import com.eter.undiamas.core.domain.model.UserProfile
import com.eter.undiamas.core.domain.repository.Alert
import com.eter.undiamas.core.domain.repository.TrafficLightEntry
import com.eter.undiamas.features.diario.domain.DiaryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Instant

private const val CLAVE_DUENIO = "usuario_duenio"

/**
 * Copia local de los datos del servidor.
 *
 * Su razón de ser: que abrir la app sin conexión muestre la racha, el diario y los
 * check-ins de siempre en vez de pantallas vacías. Alguien que abre esto a las tres de la
 * mañana buscando su "por qué" no puede encontrarse un error de red.
 *
 * Los flujos se recargan enteros tras cada escritura. Es la opción simple y aquí basta: son
 * cientos de filas, no millones, y a cambio no hace falta ningún mecanismo de invalidación
 * parcial que se pueda desincronizar sin que nadie lo note.
 */
class LocalStore(
    private val db: UndiamasDatabase,
    /**
     * Si lo guardado queda marcado como "todavía no está en el servidor".
     *
     * En la beta local es false: no hay servidor al que subir nada, así que marcarlo todo
     * como pendiente dejaría a la app avisando para siempre de una espera que no existe.
     */
    private val marcarPendientes: Boolean = true,
) {

    /** 1 solo si el dato está esperando a salir de verdad hacia algún sitio. */
    private fun marca(pendiente: Boolean): Long = if (pendiente && marcarPendientes) 1L else 0L

    private val _checkIns = MutableStateFlow<List<CheckInEntry>>(emptyList())
    val checkIns: StateFlow<List<CheckInEntry>> = _checkIns.asStateFlow()

    private val _diario = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val diario: StateFlow<List<DiaryEntry>> = _diario.asStateFlow()

    private val _animos = MutableStateFlow<List<MoodEntry>>(emptyList())
    val animos: StateFlow<List<MoodEntry>> = _animos.asStateFlow()

    private val _recaidas = MutableStateFlow<List<RelapseEvent>>(emptyList())
    val recaidas: StateFlow<List<RelapseEvent>> = _recaidas.asStateFlow()

    private val _alertas = MutableStateFlow<List<Alert>>(emptyList())
    val alertas: StateFlow<List<Alert>> = _alertas.asStateFlow()

    private val _semaforo = MutableStateFlow<List<TrafficLightEntry>>(emptyList())
    val semaforo: StateFlow<List<TrafficLightEntry>> = _semaforo.asStateFlow()

    private val _perfil = MutableStateFlow<UserProfile?>(null)
    val perfil: StateFlow<UserProfile?> = _perfil.asStateFlow()

    private val _rachaSegundos = MutableStateFlow(0L)
    val rachaSegundos: StateFlow<Long> = _rachaSegundos.asStateFlow()

    private val _ahorro = MutableStateFlow(0.0)
    val ahorro: StateFlow<Double> = _ahorro.asStateFlow()

    /** Ids de las filas que todavía no llegaron al servidor, para poder marcarlas. */
    private val _sinEnviar = MutableStateFlow<Set<String>>(emptySet())
    val sinEnviar: StateFlow<Set<String>> = _sinEnviar.asStateFlow()

    private var userId: String = ""

    // ---- Dueño de la caché -------------------------------------------------------

    /**
     * Prepara la caché para una sesión.
     *
     * Si el teléfono venía con datos de otra cuenta, se borran. Es la protección más
     * importante de esta clase: dos personas compartiendo un teléfono no pueden verse el
     * historial de recaídas la una a la otra por una caché que sobrevivió al cambio.
     */
    suspend fun abrirSesion(nuevoUserId: String): Boolean {
        userId = nuevoUserId
        val anterior = db.leer { conn ->
            conn.consultarUno(
                "SELECT valor FROM meta WHERE clave = ?",
                bind = { it.bindText(1, CLAVE_DUENIO) },
            ) { it.getText(0) }
        }
        val cambioDeCuenta = anterior != null && anterior != nuevoUserId
        if (cambioDeCuenta) borrarTodo()
        db.transaccion { conn ->
            conn.ejecutar("INSERT OR REPLACE INTO meta (clave, valor) VALUES (?, ?)") { stmt ->
                stmt.bindText(1, CLAVE_DUENIO)
                stmt.bindText(2, nuevoUserId)
            }
        }
        recargarTodo()
        return cambioDeCuenta
    }

    suspend fun borrarTodo() {
        db.transaccion { conn ->
            listOf("check_ins", "diario", "animos", "recaidas", "alertas", "semaforo", "perfil", "outbox", "meta")
                .forEach { tabla -> conn.execSQL("DELETE FROM $tabla") }
        }
        recargarTodo()
    }

    suspend fun recargarTodo() {
        recargarCheckIns()
        recargarDiario()
        recargarAnimos()
        recargarRecaidas()
        recargarAlertas()
        recargarSemaforo()
        recargarPerfil()
        recargarSinEnviar()
    }

    private suspend fun recargarSinEnviar() {
        _sinEnviar.value = db.leer { conn ->
            buildSet {
                listOf("check_ins", "diario", "animos", "recaidas", "semaforo").forEach { tabla ->
                    addAll(conn.consultar("SELECT id FROM $tabla WHERE pendiente = 1") { it.getText(0) })
                }
            }
        }
    }

    // ---- Check-ins ---------------------------------------------------------------

    suspend fun guardarCheckIn(entrada: CheckInEntry, animo: Mood, pendiente: Boolean) {
        db.transaccion { conn ->
            conn.ejecutar(
                """
                INSERT OR REPLACE INTO check_ins
                (id, nivel_riesgo, intensidad, animo, detonantes, nota, respuestas, creado_en, pendiente)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            ) { stmt ->
                stmt.bindText(1, entrada.id)
                stmt.bindText(2, entrada.riskLevel.name)
                stmt.bindLong(3, entrada.urgeIntensity.toLong())
                stmt.bindText(4, animo.name)
                stmt.bindText(5, entrada.triggers.joinToString(",") { it.name })
                stmt.bindText(6, entrada.note)
                stmt.bindText(7, apiJson.encodeToString(entrada.answers))
                stmt.bindLong(8, entrada.answeredAt.toEpochMilliseconds())
                stmt.bindLong(9, marca(pendiente))
            }
        }
        recargarCheckIns()
        recargarSinEnviar()
    }

    suspend fun reemplazarCheckIns(entradas: List<Pair<CheckInEntry, Mood>>) {
        db.transaccion { conn ->
            // Solo se barre lo confirmado: lo pendiente todavía no está en el servidor y
            // borrarlo aquí lo haría desaparecer de la pantalla de quien lo escribió.
            conn.execSQL("DELETE FROM check_ins WHERE pendiente = 0")
            entradas.forEach { (entrada, animo) ->
                conn.ejecutar(
                    """
                    INSERT OR REPLACE INTO check_ins
                    (id, nivel_riesgo, intensidad, animo, detonantes, nota, respuestas, creado_en, pendiente)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
                    """,
                ) { stmt ->
                    stmt.bindText(1, entrada.id)
                    stmt.bindText(2, entrada.riskLevel.name)
                    stmt.bindLong(3, entrada.urgeIntensity.toLong())
                    stmt.bindText(4, animo.name)
                    stmt.bindText(5, entrada.triggers.joinToString(",") { it.name })
                    stmt.bindText(6, entrada.note)
                    stmt.bindText(7, apiJson.encodeToString(entrada.answers))
                    stmt.bindLong(8, entrada.answeredAt.toEpochMilliseconds())
                }
            }
        }
        recargarCheckIns()
        recargarSinEnviar()
    }

    /** Al confirmar el servidor, la fila local cambia su id provisional por el definitivo. */
    suspend fun confirmarCheckIn(idLocal: String, idServidor: String) {
        cambiarId("check_ins", idLocal, idServidor)
        recargarCheckIns()
        recargarSinEnviar()
    }

    private suspend fun recargarCheckIns() {
        _checkIns.value = db.leer { conn ->
            conn.consultar(
                """
                SELECT id, nivel_riesgo, intensidad, detonantes, nota, respuestas, creado_en
                FROM check_ins ORDER BY creado_en DESC
                """,
            ) { stmt ->
                CheckInEntry(
                    id = stmt.getText(0),
                    userId = userId,
                    answeredAt = Instant.fromEpochMilliseconds(stmt.getLong(6)),
                    answers = runCatching {
                        apiJson.decodeFromString<Map<String, String>>(stmt.getText(5))
                    }.getOrDefault(emptyMap()),
                    riskLevel = aRiesgo(stmt.getText(1)),
                    triggers = aDetonantes(stmt.getText(3)),
                    urgeIntensity = stmt.getLong(2).toInt(),
                    note = stmt.getText(4),
                )
            }
        }
    }

    // ---- Diario ------------------------------------------------------------------

    suspend fun guardarEntradaDiario(entrada: DiaryEntry, pendiente: Boolean) {
        db.transaccion { conn ->
            conn.ejecutar(
                "INSERT OR REPLACE INTO diario (id, texto, creado_en, pendiente) VALUES (?, ?, ?, ?)",
            ) { stmt ->
                stmt.bindText(1, entrada.id)
                stmt.bindText(2, entrada.text)
                stmt.bindLong(3, entrada.createdAt.toEpochMilliseconds())
                stmt.bindLong(4, marca(pendiente))
            }
        }
        recargarDiario()
        recargarSinEnviar()
    }

    suspend fun reemplazarDiario(entradas: List<DiaryEntry>) {
        db.transaccion { conn ->
            conn.execSQL("DELETE FROM diario WHERE pendiente = 0")
            entradas.forEach { entrada ->
                conn.ejecutar(
                    "INSERT OR REPLACE INTO diario (id, texto, creado_en, pendiente) VALUES (?, ?, ?, 0)",
                ) { stmt ->
                    stmt.bindText(1, entrada.id)
                    stmt.bindText(2, entrada.text)
                    stmt.bindLong(3, entrada.createdAt.toEpochMilliseconds())
                }
            }
        }
        recargarDiario()
        recargarSinEnviar()
    }

    suspend fun borrarEntradaDiario(id: String) {
        db.transaccion { conn ->
            conn.ejecutar("DELETE FROM diario WHERE id = ?") { it.bindText(1, id) }
        }
        recargarDiario()
        recargarSinEnviar()
    }

    suspend fun confirmarEntradaDiario(idLocal: String, idServidor: String) {
        cambiarId("diario", idLocal, idServidor)
        recargarDiario()
        recargarSinEnviar()
    }

    private suspend fun recargarDiario() {
        _diario.value = db.leer { conn ->
            conn.consultar("SELECT id, texto, creado_en FROM diario ORDER BY creado_en DESC") { stmt ->
                DiaryEntry(
                    id = stmt.getText(0),
                    userId = userId,
                    createdAt = Instant.fromEpochMilliseconds(stmt.getLong(2)),
                    text = stmt.getText(1),
                )
            }
        }
    }

    // ---- Ánimo -------------------------------------------------------------------

    suspend fun guardarAnimo(entrada: MoodEntry, pendiente: Boolean) {
        db.transaccion { conn ->
            conn.ejecutar(
                "INSERT OR REPLACE INTO animos (id, animo, creado_en, pendiente) VALUES (?, ?, ?, ?)",
            ) { stmt ->
                stmt.bindText(1, entrada.id)
                stmt.bindText(2, entrada.mood.name)
                stmt.bindLong(3, entrada.registeredAt.toEpochMilliseconds())
                stmt.bindLong(4, marca(pendiente))
            }
        }
        recargarAnimos()
        recargarSinEnviar()
    }

    suspend fun reemplazarAnimos(entradas: List<MoodEntry>) {
        db.transaccion { conn ->
            conn.execSQL("DELETE FROM animos WHERE pendiente = 0")
            entradas.forEach { entrada ->
                conn.ejecutar(
                    "INSERT OR REPLACE INTO animos (id, animo, creado_en, pendiente) VALUES (?, ?, ?, 0)",
                ) { stmt ->
                    stmt.bindText(1, entrada.id)
                    stmt.bindText(2, entrada.mood.name)
                    stmt.bindLong(3, entrada.registeredAt.toEpochMilliseconds())
                }
            }
        }
        recargarAnimos()
        recargarSinEnviar()
    }

    suspend fun confirmarAnimo(idLocal: String, idServidor: String) {
        cambiarId("animos", idLocal, idServidor)
        recargarAnimos()
        recargarSinEnviar()
    }

    private suspend fun recargarAnimos() {
        _animos.value = db.leer { conn ->
            conn.consultar("SELECT id, animo, creado_en FROM animos ORDER BY creado_en DESC") { stmt ->
                MoodEntry(
                    id = stmt.getText(0),
                    userId = userId,
                    mood = Mood.entries.firstOrNull { it.name == stmt.getText(1) } ?: Mood.NEUTRAL,
                    registeredAt = Instant.fromEpochMilliseconds(stmt.getLong(2)),
                )
            }
        }
    }

    // ---- Recaídas ----------------------------------------------------------------

    suspend fun guardarRecaida(evento: RelapseEvent, detonantes: List<String>, pendiente: Boolean) {
        db.transaccion { conn ->
            conn.ejecutar(
                """
                INSERT OR REPLACE INTO recaidas (id, nota, detonantes, racha_previa, creado_en, pendiente)
                VALUES (?, ?, ?, 0, ?, ?)
                """,
            ) { stmt ->
                stmt.bindText(1, evento.id)
                stmt.bindText(2, evento.notes.orEmpty())
                stmt.bindText(3, detonantes.joinToString(","))
                stmt.bindLong(4, evento.occurredAt.toEpochMilliseconds())
                stmt.bindLong(5, marca(pendiente))
            }
        }
        recargarRecaidas()
        recargarSinEnviar()
    }

    suspend fun reemplazarRecaidas(eventos: List<RelapseEvent>) {
        db.transaccion { conn ->
            conn.execSQL("DELETE FROM recaidas WHERE pendiente = 0")
            eventos.forEach { evento ->
                conn.ejecutar(
                    """
                    INSERT OR REPLACE INTO recaidas (id, nota, detonantes, racha_previa, creado_en, pendiente)
                    VALUES (?, ?, '', 0, ?, 0)
                    """,
                ) { stmt ->
                    stmt.bindText(1, evento.id)
                    stmt.bindText(2, evento.notes.orEmpty())
                    stmt.bindLong(3, evento.occurredAt.toEpochMilliseconds())
                }
            }
        }
        recargarRecaidas()
        recargarSinEnviar()
    }

    suspend fun confirmarRecaida(idLocal: String, idServidor: String) {
        cambiarId("recaidas", idLocal, idServidor)
        recargarRecaidas()
        recargarSinEnviar()
    }

    private suspend fun recargarRecaidas() {
        _recaidas.value = db.leer { conn ->
            conn.consultar("SELECT id, nota, creado_en FROM recaidas ORDER BY creado_en DESC") { stmt ->
                RelapseEvent(
                    id = stmt.getText(0),
                    userId = userId,
                    occurredAt = Instant.fromEpochMilliseconds(stmt.getLong(2)),
                    notes = stmt.getText(1).takeIf { it.isNotBlank() },
                )
            }
        }
    }

    // ---- Alertas y semáforo ------------------------------------------------------

    suspend fun guardarAlerta(alerta: Alert, creadoEn: Long) {
        db.transaccion { conn ->
            conn.ejecutar(
                """
                INSERT OR REPLACE INTO alertas (id, nivel_riesgo, mensaje, atendida, creado_en)
                VALUES (?, ?, ?, ?, ?)
                """,
            ) { stmt ->
                stmt.bindText(1, alerta.id)
                stmt.bindText(2, alerta.riskLevel.name)
                stmt.bindText(3, alerta.message)
                stmt.bindLong(4, if (alerta.handled) 1 else 0)
                stmt.bindLong(5, creadoEn)
            }
        }
        recargarAlertas()
    }

    suspend fun reemplazarAlertas(alertas: List<Pair<Alert, Long>>) {
        db.transaccion { conn ->
            conn.execSQL("DELETE FROM alertas")
            alertas.forEach { (alerta, creadoEn) ->
                conn.ejecutar(
                    """
                    INSERT OR REPLACE INTO alertas (id, nivel_riesgo, mensaje, atendida, creado_en)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                ) { stmt ->
                    stmt.bindText(1, alerta.id)
                    stmt.bindText(2, alerta.riskLevel.name)
                    stmt.bindText(3, alerta.message)
                    stmt.bindLong(4, if (alerta.handled) 1 else 0)
                    stmt.bindLong(5, creadoEn)
                }
            }
        }
        recargarAlertas()
    }

    suspend fun marcarAlertaAtendida(id: String) {
        db.transaccion { conn ->
            conn.ejecutar("UPDATE alertas SET atendida = 1 WHERE id = ?") { it.bindText(1, id) }
        }
        recargarAlertas()
    }

    private suspend fun recargarAlertas() {
        _alertas.value = db.leer { conn ->
            conn.consultar(
                "SELECT id, nivel_riesgo, mensaje, atendida FROM alertas ORDER BY creado_en DESC",
            ) { stmt ->
                Alert(
                    id = stmt.getText(0),
                    riskLevel = aRiesgo(stmt.getText(1)),
                    message = stmt.getText(2),
                    handled = stmt.getLong(3) == 1L,
                )
            }
        }
    }

    suspend fun guardarSemaforo(entrada: TrafficLightEntry, creadoEn: Long, pendiente: Boolean) {
        db.transaccion { conn ->
            conn.ejecutar(
                """
                INSERT OR REPLACE INTO semaforo (id, estado, motivo, nivel, acciones, creado_en, pendiente)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
            ) { stmt ->
                stmt.bindText(1, entrada.id)
                stmt.bindText(2, entrada.status.name)
                stmt.bindText(3, entrada.reason)
                stmt.bindLong(4, entrada.triggerLevel.toLong())
                stmt.bindText(5, entrada.suggestedActions.joinToString("|"))
                stmt.bindLong(6, creadoEn)
                stmt.bindLong(7, marca(pendiente))
            }
        }
        recargarSemaforo()
        recargarSinEnviar()
    }

    suspend fun reemplazarSemaforo(entradas: List<Pair<TrafficLightEntry, Long>>) {
        db.transaccion { conn ->
            conn.execSQL("DELETE FROM semaforo WHERE pendiente = 0")
            entradas.forEach { (entrada, creadoEn) ->
                conn.ejecutar(
                    """
                    INSERT OR REPLACE INTO semaforo (id, estado, motivo, nivel, acciones, creado_en, pendiente)
                    VALUES (?, ?, ?, ?, ?, ?, 0)
                    """,
                ) { stmt ->
                    stmt.bindText(1, entrada.id)
                    stmt.bindText(2, entrada.status.name)
                    stmt.bindText(3, entrada.reason)
                    stmt.bindLong(4, entrada.triggerLevel.toLong())
                    stmt.bindText(5, entrada.suggestedActions.joinToString("|"))
                    stmt.bindLong(6, creadoEn)
                }
            }
        }
        recargarSemaforo()
        recargarSinEnviar()
    }

    suspend fun confirmarSemaforo(idLocal: String, idServidor: String) {
        cambiarId("semaforo", idLocal, idServidor)
        recargarSemaforo()
        recargarSinEnviar()
    }

    private suspend fun recargarSemaforo() {
        _semaforo.value = db.leer { conn ->
            conn.consultar(
                "SELECT id, estado, motivo, nivel, acciones FROM semaforo ORDER BY creado_en DESC",
            ) { stmt ->
                TrafficLightEntry(
                    id = stmt.getText(0),
                    status = aRiesgo(stmt.getText(1)),
                    reason = stmt.getText(2),
                    triggerLevel = stmt.getLong(3).toInt(),
                    suggestedActions = stmt.getText(4).split("|").filter { it.isNotBlank() },
                )
            }
        }
    }

    // ---- Perfil ------------------------------------------------------------------

    suspend fun guardarPerfil(perfil: UserProfile, rachaSegundos: Long, ahorro: Double) {
        db.transaccion { conn ->
            conn.ejecutar(
                "INSERT OR REPLACE INTO perfil (id, json, racha_segundos, ahorro) VALUES (1, ?, ?, ?)",
            ) { stmt ->
                stmt.bindText(1, apiJson.encodeToString(UserProfile.serializer(), perfil))
                stmt.bindLong(2, rachaSegundos)
                stmt.bindDouble(3, ahorro)
            }
        }
        recargarPerfil()
    }

    private suspend fun recargarPerfil() {
        val fila = db.leer { conn ->
            conn.consultarUno("SELECT json, racha_segundos, ahorro FROM perfil WHERE id = 1") { stmt ->
                Triple(stmt.getText(0), stmt.getLong(1), stmt.getDouble(2))
            }
        }
        if (fila == null) return
        _perfil.value = runCatching {
            apiJson.decodeFromString(UserProfile.serializer(), fila.first)
        }.getOrNull()
        _rachaSegundos.value = fila.second
        _ahorro.value = fila.third
    }

    // ---- Utilidades --------------------------------------------------------------

    private suspend fun cambiarId(tabla: String, idLocal: String, idServidor: String) {
        if (idLocal == idServidor) {
            db.transaccion { conn ->
                conn.ejecutar("UPDATE $tabla SET pendiente = 0 WHERE id = ?") { it.bindText(1, idLocal) }
            }
            return
        }
        db.transaccion { conn ->
            // El id definitivo puede existir ya si el refresco del servidor llegó primero;
            // en ese caso la fila local sobra.
            conn.ejecutar("DELETE FROM $tabla WHERE id = ?") { it.bindText(1, idServidor) }
            conn.ejecutar("UPDATE $tabla SET id = ?, pendiente = 0 WHERE id = ?") { stmt ->
                stmt.bindText(1, idServidor)
                stmt.bindText(2, idLocal)
            }
        }
    }

    private fun aRiesgo(valor: String): RiskLevel =
        RiskLevel.entries.firstOrNull { it.name == valor } ?: RiskLevel.VERDE

    private fun aDetonantes(valor: String): Set<Trigger> =
        valor.split(",")
            .mapNotNull { nombre -> Trigger.entries.firstOrNull { it.name == nombre } }
            .toSet()
}
