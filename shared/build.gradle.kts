import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    android {
       namespace = "com.eter.undiamas.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_17
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.uiBackHandler)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.datastorePreferences)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.logging)
            // SQLite propio en el teléfono: lo que se escribe sin conexión se guarda aquí
            // y se envía al servidor cuando vuelve la red.
            implementation(libs.androidx.sqlite)
            implementation(libs.androidx.sqliteBundled)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            // Motor HTTP de cada plataforma: Ktor solo define la API en commonMain.
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            // Responde peticiones con JSON fijo: permite probar la capa API sin servidor.
            implementation(libs.ktor.client.mock)
        }
        getByName("androidHostTest").dependencies {
            // La prueba de integracion habla con el backend de verdad, no con un mock, asi
            // que necesita un motor HTTP real en la JVM.
            implementation(libs.ktor.client.okhttp)
            // El artefacto de Android trae la libreria nativa de SQLite compilada para el
            // telefono; estos tests corren en la JVM del escritorio y necesitan la suya.
            implementation(libs.androidx.sqliteBundled.jvm)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}