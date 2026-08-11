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

    // Lectura de la pulsera vía Health Connect (Android-only: por eso vive aquí y no en :shared).
    implementation(libs.androidx.healthConnect)

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
            // El backend de desarrollo habla http:// en la red local, que Android bloquea
            // por defecto. Esta excepción existe SOLO en debug.
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            buildConfigField("boolean", "PERMITE_CAMBIAR_SERVIDOR", "true")
            buildConfigField("String", "API_BASE_URL", "\"${servidorDeDesarrollo()}\"")
        }
        release {
            // Sin esto la app publicada aceptaría tráfico sin cifrar, y el historial de
            // recaídas de alguien viajaría en claro por cualquier wifi pública.
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            buildConfigField("boolean", "PERMITE_CAMBIAR_SERVIDOR", "false")
            buildConfigField("String", "API_BASE_URL", "\"${servidorDeProduccion()}\"")

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
        buildConfig = true
    }
}

/**
 * Dirección del backend en desarrollo.
 *
 * Se puede cambiar sin tocar el repositorio poniendo `undiamas.apiUrl=http://...` en
 * `local.properties`, que es donde va la configuración de cada máquina.
 */
fun servidorDeDesarrollo(): String =
    (project.findProperty("undiamas.apiUrl") as String?) ?: "http://192.168.1.145:8080"

/**
 * Dirección del backend en producción.
 *
 * Tiene que ser https: la app publicada no acepta tráfico sin cifrar. Se define con
 * `undiamas.apiUrlProd=https://...` en `local.properties` o como propiedad de Gradle en el
 * servidor de compilación.
 */
fun servidorDeProduccion(): String =
    (project.findProperty("undiamas.apiUrlProd") as String?) ?: "https://api.undiamas.mx"

/**
 * Avisa antes de publicar algo que no funcionaría.
 *
 * Un release sin firmar no se puede subir, y uno apuntando a la dirección de ejemplo se
 * instalaría bien y luego no conectaría con nada — que es peor, porque el fallo aparece en
 * el teléfono de alguien y no en la consola de quien compila.
 */
// Los avisos se calculan al configurar, no al ejecutar: leer `project` dentro de un
// `doFirst` rompe la caché de configuración de Gradle.
val avisosDeRelease: List<String> = buildList {
    if (!hayFirma) {
        add("no hay keystore.properties: el artefacto saldrá SIN FIRMAR y Play lo rechazará")
    }
    if (project.findProperty("undiamas.apiUrlProd") == null) {
        add("undiamas.apiUrlProd no está definida: se usará ${servidorDeProduccion()}")
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
