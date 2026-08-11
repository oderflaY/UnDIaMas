package com.eter.undiamas.core.data.local

import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

/** Qué operación quedó pendiente de enviar. */
enum class TipoPendiente {
    CHECK_IN,
    DIARIO_CREAR,
    DIARIO_BORRAR,
    ANIMO,
    RECAIDA,
    SEMAFORO,
    PERFIL_IDENTIDAD,
    PERFIL_CONTACTOS,
    PERFIL_TRACKER,
    RECORDATORIO,
    ALERTA_ATENDIDA,
}

/**
 * Una operación esperando a que haya conexión.
 *
 * [carga] es el JSON del cuerpo que hay que mandar. [idLocal] es el id provisional que la
 * app le dio a la fila mientras no había servidor; al sincronizar se sustituye por el real.
 */
data class Pendiente(
    val seq: Long,
    val tipo: TipoPendiente,
    val carga: String,
    val idLocal: String?,
    val creadoEn: Long,
    val intentos: Int,
    val ultimoError: String?,
)

/**
 * Cola de cambios que todavía no llegaron al servidor.
 *
 * Es lo más delicado de la app sin conexión: aquí está lo que la persona ya considera
 * guardado. Tres reglas que la gobiernan:
 *
 * 1. **Orden estricto.** Se envía por `seq`, que es el orden en que pasaron las cosas. Una
 *    recaída y un check-in en desorden le dan al servidor una historia que no ocurrió.
 * 2. **Se borra solo cuando el servidor confirma.** Si algo falla, la fila se queda y se
 *    reintenta; perder un check-in es peor que mandarlo dos veces.
 * 3. **Nada caduca solo.** Un mensaje de hace tres días sigue siendo el registro de un día
 *    de esa persona.
 */
class Outbox(private val db: UndiamasDatabase) {

    private val _pendientes = MutableStateFlow(0)

    /** Cuántos cambios esperan. La pantalla lo muestra para que nadie dude de si se guardó. */
    val pendientes: StateFlow<Int> = _pendientes.asStateFlow()

    suspend fun refrescarConteo() {
        _pendientes.value = db.leer { conn ->
            conn.consultarUno("SELECT COUNT(*) FROM outbox") { it.getLong(0).toInt() } ?: 0
        }
    }

    /** Encola una operación y devuelve su número de orden. */
    suspend fun encolar(tipo: TipoPendiente, carga: String, idLocal: String? = null): Long {
        val seq = db.transaccion { conn ->
            conn.ejecutar(
                "INSERT INTO outbox (tipo, carga, id_local, creado_en) VALUES (?, ?, ?, ?)",
            ) { stmt ->
                stmt.bindText(1, tipo.name)
                stmt.bindText(2, carga)
                if (idLocal == null) stmt.bindNull(3) else stmt.bindText(3, idLocal)
                stmt.bindLong(4, Clock.System.now().toEpochMilliseconds())
            }
            conn.consultarUno("SELECT last_insert_rowid()") { it.getLong(0) } ?: 0L
        }
        refrescarConteo()
        return seq
    }

    /** Las siguientes operaciones a enviar, en el orden en que ocurrieron. */
    suspend fun siguientes(limite: Int = 50): List<Pendiente> = db.leer { conn ->
        conn.consultar(
            """
            SELECT seq, tipo, carga, id_local, creado_en, intentos, ultimo_error
            FROM outbox ORDER BY seq ASC LIMIT ?
            """,
            bind = { it.bindLong(1, limite.toLong()) },
        ) { stmt ->
            Pendiente(
                seq = stmt.getLong(0),
                tipo = TipoPendiente.valueOf(stmt.getText(1)),
                carga = stmt.getText(2),
                idLocal = if (stmt.isNull(3)) null else stmt.getText(3),
                creadoEn = stmt.getLong(4),
                intentos = stmt.getLong(5).toInt(),
                ultimoError = if (stmt.isNull(6)) null else stmt.getText(6),
            )
        }
    }

    /** El servidor confirmó: la operación deja de estar pendiente. */
    suspend fun completar(seq: Long) {
        db.transaccion { conn ->
            conn.ejecutar("DELETE FROM outbox WHERE seq = ?") { it.bindLong(1, seq) }
        }
        refrescarConteo()
    }

    /** Falló el envío: se anota para poder explicarlo, y se reintentará. */
    suspend fun anotarFallo(seq: Long, error: String) {
        db.transaccion { conn ->
            conn.ejecutar(
                "UPDATE outbox SET intentos = intentos + 1, ultimo_error = ? WHERE seq = ?",
            ) { stmt ->
                stmt.bindText(1, error.take(500))
                stmt.bindLong(2, seq)
            }
        }
    }

    /**
     * Descarta una operación que el servidor rechazó de forma definitiva.
     *
     * Solo para errores que no se arreglan reintentando (un 400 por datos inválidos, un 404
     * de algo que ya no existe). Reintentar eso para siempre bloquearía la cola entera y
     * ningún cambio posterior llegaría nunca.
     */
    suspend fun descartar(seq: Long) = completar(seq)

    /**
     * Quita las operaciones que afectan a una fila local.
     *
     * Sirve para cuando alguien crea una entrada de diario sin conexión y la borra antes de
     * que se envíe: mandar las dos cosas al servidor sería trabajo y datos para nada.
     */
    suspend fun descartarPorIdLocal(idLocal: String): Boolean {
        val borradas = db.transaccion { conn ->
            val cuantas = conn.consultarUno(
                "SELECT COUNT(*) FROM outbox WHERE id_local = ?",
                bind = { it.bindText(1, idLocal) },
            ) { it.getLong(0) } ?: 0L
            conn.ejecutar("DELETE FROM outbox WHERE id_local = ?") { it.bindText(1, idLocal) }
            cuantas
        }
        refrescarConteo()
        return borradas > 0
    }

    /** Cambio de cuenta: la cola del usuario anterior no se manda con el token del nuevo. */
    suspend fun vaciar() {
        db.transaccion { conn -> conn.execSQL("DELETE FROM outbox") }
        refrescarConteo()
    }
}
