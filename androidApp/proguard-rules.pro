# Reglas de R8 para la build de release.
#
# Todo lo de aquí protege código al que NADIE llama por su nombre desde Kotlin: se resuelve
# por reflexión o desde código nativo, así que R8 no ve la referencia y lo borra. El
# resultado típico es una app que compila, instala, arranca y revienta en la primera
# petición al servidor — justo lo que no se puede descubrir en la consola de Play.

# ---- Trazas legibles ----------------------------------------------------------------
# Sin esto, un fallo en producción llega como una pila de nombres de una letra. El mapping
# queda en build/outputs/mapping/release/ y hay que subirlo a Play para desofuscar.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Anotaciones y firmas genéricas: kotlinx.serialization y Ktor las leen en tiempo de
# ejecución para saber a qué tipo deserializar una respuesta.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod

# ---- kotlinx.serialization ----------------------------------------------------------
# El compilador genera un `$serializer` por cada @Serializable. No lo referencia nadie por
# nombre, así que sin estas reglas R8 lo borra y toda respuesta del backend falla al
# deserializarse.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    *** Companion;
}
-keepclasseswithmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}
-keep,includedescriptorclasses class kotlinx.serialization.** { *; }
-dontnote kotlinx.serialization.**
-dontwarn kotlinx.serialization.**

# Los DTOs y modelos de dominio de la app, que son los que viajan como JSON.
-keep @kotlinx.serialization.Serializable class com.eter.undiamas.** { *; }
-keepclassmembers class com.eter.undiamas.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Los enums viajan por su nombre (VERDE, ROJO, MUY_MAL...). Si R8 los renombra, el semáforo
# y los detonantes dejan de coincidir con lo que entiende el servidor.
-keepclassmembers enum com.eter.undiamas.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# ---- Ktor y OkHttp ------------------------------------------------------------------
# El motor HTTP se descubre por ServiceLoader, no por una llamada directa.
-keep class io.ktor.** { *; }
-keep class io.ktor.client.engine.okhttp.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn io.ktor.**

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---- Corrutinas ---------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }
-dontwarn kotlinx.coroutines.**

# ---- SQLite ------------------------------------------------------------------------
# La librería nativa llama a estas clases desde C con JNI: si R8 las renombra, la base de
# datos local no abre y se pierde todo lo que la app guarda sin conexión.
-keep class androidx.sqlite.** { *; }
-keep class androidx.sqlite.driver.bundled.** { *; }
-dontwarn androidx.sqlite.**

# ---- DataStore ---------------------------------------------------------------------
# Ahí viven los tokens de sesión; se serializan con protobuf por reflexión.
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ---- Health Connect ----------------------------------------------------------------
-keep class androidx.health.connect.** { *; }
-dontwarn androidx.health.connect.**

# ---- Compose -----------------------------------------------------------------------
# El runtime ya trae sus reglas, pero los @Composable con parámetros por defecto se
# resuelven con métodos sintéticos que conviene no tocar.
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ---- Kotlin ------------------------------------------------------------------------
-keepclassmembers class **$WhenMappings { <fields>; }
-dontwarn kotlin.**
-dontwarn org.slf4j.**
-dontwarn java.lang.invoke.**
