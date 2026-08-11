package com.eter.undiamas.core.data.local

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Nombre del archivo de base de datos; la carpeta la resuelve cada plataforma. */
const val DATABASE_FILE = "undiamas.db"

/** Ruta completa del archivo, distinta en Android y en iOS. */
expect fun databaseFilePath(): String

/**
 * Abre la base del teléfono, o una en memoria si no hay ruta.
 *
 * El caso sin ruta son las previews de Compose y los tests, donde nadie inyectó el
 * directorio de la app. Ahí interesa que todo funcione en memoria y no que reviente al
 * construir la pantalla; en el teléfono la ruta siempre está y se usa el archivo real.
 */
fun crearBaseDeDatos(): UndiamasDatabase {
    val ruta = runCatching { databaseFilePath() }.getOrDefault(":memory:")
    return UndiamasDatabase(ruta)
}

/**
 * Base de datos local del teléfono.
 *
 * Guarda dos cosas distintas que conviene no confundir:
 *
 * - **La caché**: copia de lo que hay en el servidor, para que la app abra y funcione sin
 *   conexión en vez de mostrar pantallas vacías.
 * - **La bandeja de salida** (`outbox`): lo que la persona hizo sin red y todavía no llegó
 *   al servidor. Es la parte que no se puede perder — ahí está el check-in que alguien
 *   escribió en mitad de una crisis, en el metro y sin señal.
 *
 * Se usa una sola conexión protegida por un mutex. SQLite admite concurrencia, pero una
 * conexión de androidx.sqlite no es segura entre hilos, y serializar es más simple que
 * gestionar un pool para el volumen de datos que maneja esta app.
 */
class UndiamasDatabase(
    private val path: String = databaseFilePath(),
    private val driver: BundledSQLiteDriver = BundledSQLiteDriver(),
) {
    private val mutex = Mutex()

    /**
     * Se abre en el primer uso, no al construir la clase.
     *
     * Abrir el archivo y crear el esquema es trabajo de disco. Como la app construye su
     * grafo de datos al montar la primera pantalla, hacerlo en el constructor significaba
     * tocar disco en el hilo principal en cada arranque: jank garantizado y ANR si el
     * teléfono va justo. Aquí la primera consulta ya viene de una corrutina de fondo.
     */
    private val connection: SQLiteConnection by lazy {
        driver.open(path).also { conn ->
            conn.execSQL("PRAGMA journal_mode = WAL")
            // Sin esto SQLite ignora las claves foráneas; aquí no hay ninguna todavía, pero
            // activarlo desde el principio evita sorpresas cuando se añada la primera.
            conn.execSQL("PRAGMA foreign_keys = ON")
            ESQUEMA.forEach(conn::execSQL)
        }
    }

    /** Ejecuta algo contra la base, en exclusiva. */
    suspend fun <T> transaccion(bloque: (SQLiteConnection) -> T): T = mutex.withLock {
        val conn = connection
        conn.execSQL("BEGIN IMMEDIATE")
        try {
            val resultado = bloque(conn)
            conn.execSQL("COMMIT")
            resultado
        } catch (error: Throwable) {
            conn.execSQL("ROLLBACK")
            throw error
        }
    }

    /** Lectura sin transacción, para consultas sueltas. */
    suspend fun <T> leer(bloque: (SQLiteConnection) -> T): T = mutex.withLock { bloque(connection) }

    fun cerrar() = connection.close()

    private companion object {
        val ESQUEMA = listOf(
            """
            CREATE TABLE IF NOT EXISTS meta (
                clave TEXT PRIMARY KEY NOT NULL,
                valor TEXT NOT NULL
            )
            """,
            // `pendiente` distingue lo que ya confirmó el servidor de lo que sigue en la
            // bandeja de salida. La UI lo usa para marcar "sin enviar" sin mentir.
            """
            CREATE TABLE IF NOT EXISTS check_ins (
                id TEXT PRIMARY KEY NOT NULL,
                nivel_riesgo TEXT NOT NULL,
                intensidad INTEGER NOT NULL,
                animo TEXT NOT NULL,
                detonantes TEXT NOT NULL,
                nota TEXT NOT NULL,
                respuestas TEXT NOT NULL,
                creado_en INTEGER NOT NULL,
                pendiente INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS diario (
                id TEXT PRIMARY KEY NOT NULL,
                texto TEXT NOT NULL,
                creado_en INTEGER NOT NULL,
                pendiente INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS animos (
                id TEXT PRIMARY KEY NOT NULL,
                animo TEXT NOT NULL,
                creado_en INTEGER NOT NULL,
                pendiente INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS recaidas (
                id TEXT PRIMARY KEY NOT NULL,
                nota TEXT NOT NULL,
                detonantes TEXT NOT NULL,
                racha_previa INTEGER NOT NULL,
                creado_en INTEGER NOT NULL,
                pendiente INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS alertas (
                id TEXT PRIMARY KEY NOT NULL,
                nivel_riesgo TEXT NOT NULL,
                mensaje TEXT NOT NULL,
                atendida INTEGER NOT NULL,
                creado_en INTEGER NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS semaforo (
                id TEXT PRIMARY KEY NOT NULL,
                estado TEXT NOT NULL,
                motivo TEXT NOT NULL,
                nivel INTEGER NOT NULL,
                acciones TEXT NOT NULL,
                creado_en INTEGER NOT NULL,
                pendiente INTEGER NOT NULL DEFAULT 0
            )
            """,
            // El perfil se guarda serializado: es un único registro y su forma la marca el
            // modelo de dominio, no el esquema.
            """
            CREATE TABLE IF NOT EXISTS perfil (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                json TEXT NOT NULL,
                racha_segundos INTEGER NOT NULL DEFAULT 0,
                ahorro REAL NOT NULL DEFAULT 0
            )
            """,
            // El orden de `seq` es el orden en que ocurrieron las cosas, y es el orden en
            // que hay que enviarlas: una recaída registrada antes que un check-in tiene que
            // llegar antes, o el servidor calcula la racha sobre datos que no pasaron así.
            """
            CREATE TABLE IF NOT EXISTS outbox (
                seq INTEGER PRIMARY KEY AUTOINCREMENT,
                tipo TEXT NOT NULL,
                carga TEXT NOT NULL,
                id_local TEXT,
                creado_en INTEGER NOT NULL,
                intentos INTEGER NOT NULL DEFAULT 0,
                ultimo_error TEXT
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_check_ins_fecha ON check_ins (creado_en DESC)",
            "CREATE INDEX IF NOT EXISTS idx_diario_fecha ON diario (creado_en DESC)",
            "CREATE INDEX IF NOT EXISTS idx_animos_fecha ON animos (creado_en DESC)",
            "CREATE INDEX IF NOT EXISTS idx_alertas_fecha ON alertas (creado_en DESC)",
        )
    }
}

// ---- Utilidades de lectura ------------------------------------------------------

/** Recorre todas las filas de una consulta y las convierte. */
inline fun <T> SQLiteConnection.consultar(
    sql: String,
    bind: (SQLiteStatement) -> Unit = {},
    fila: (SQLiteStatement) -> T,
): List<T> {
    val stmt = prepare(sql)
    return try {
        bind(stmt)
        buildList {
            while (stmt.step()) add(fila(stmt))
        }
    } finally {
        stmt.close()
    }
}

/** Primera fila de una consulta, o null si no hay ninguna. */
inline fun <T> SQLiteConnection.consultarUno(
    sql: String,
    bind: (SQLiteStatement) -> Unit = {},
    fila: (SQLiteStatement) -> T,
): T? {
    val stmt = prepare(sql)
    return try {
        bind(stmt)
        if (stmt.step()) fila(stmt) else null
    } finally {
        stmt.close()
    }
}

/** Ejecuta una sentencia con parámetros. */
inline fun SQLiteConnection.ejecutar(sql: String, bind: (SQLiteStatement) -> Unit = {}) {
    val stmt = prepare(sql)
    try {
        bind(stmt)
        stmt.step()
    } finally {
        stmt.close()
    }
}
