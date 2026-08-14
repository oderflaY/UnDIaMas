import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

/**
 * Credenciales de firma.
 *
 * Viven en `keystore.properties`, que NO se sube al repositorio: una clave de firma filtrada
 * permite publicar actualizaciones falsas de la app en nombre de quien la publicó. El
 * archivo de ejemplo es `keystore.properties.example`.
 */
val keystoreProperties = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val hayFirma = keystoreProperties.getProperty("storeFile") != null

android {
    namespace = "com.eter.undiamas"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.eter.undiamas"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (hayFirma) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "MODO_LOCAL", "false")
        }

        /**
         * Beta para repartir: la app entera sin backend, guardando en el teléfono.
         *
         * Se firma con la clave de depuración a propósito, para que salga un APK
         * instalable sin montar un keystore. El sufijo del id la deja convivir con la app
         * de verdad en el mismo teléfono, que es justo lo que hace falta para compararlas.
         */
        create("beta") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta-local"
            buildConfigField("boolean", "MODO_LOCAL", "true")

            // No depurable aunque venga de debug. Aquí dentro hay diario e historial de
            // recaídas: con `debuggable` cualquiera con el teléfono en la mano puede
            // sacarlos por adb sin desbloquear nada.
            isDebuggable = false

            // Se minifica como la de verdad. Es lo que baja el APK de 78 MB a algo que se
            // puede mandar por mensajería, y de paso hace que la beta pruebe las mismas
            // reglas de R8 que acabarán en la tienda.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            // x86 solo lo usan los emuladores: en un APK para repartir son 2 MB de
            // librería de SQLite que ningún teléfono va a abrir.
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
        }

        release {
            buildConfigField("boolean", "MODO_LOCAL", "false")

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hayFirma) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        // Solo para MODO_LOCAL: es lo que separa la beta local de la app de verdad.
        buildConfig = true
    }
}

/**
 * Avisa antes de publicar algo que no funcionaría.
 *
 * Un release sin firmar no se puede subir a Play, y el fallo no aparece hasta el final de
 * una compilación larga si nadie lo dice antes.
 */
// Los avisos se calculan al configurar, no al ejecutar: leer `project` dentro de un
// `doFirst` rompe la caché de configuración de Gradle.
val avisosDeRelease: List<String> = buildList {
    if (!hayFirma) {
        add("no hay keystore.properties: el artefacto saldrá SIN FIRMAR y Play lo rechazará")
    }
}

val mensajeDeRelease: String? = avisosDeRelease
    .takeIf { it.isNotEmpty() }
    ?.let { "\n⚠ Release incompleta:\n" + it.joinToString("\n") { aviso -> "  · $aviso" } + "\n" }

tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
    // El mensaje se copia a una variable local para que la acción capture solo un String:
    // capturar la propiedad del script arrastraría el objeto del build, que la caché de
    // configuración no sabe serializar.
    val aviso = mensajeDeRelease
    doFirst { if (aviso != null) logger.warn(aviso) }
}
