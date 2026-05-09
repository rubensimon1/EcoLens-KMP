import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

val generatedSecretsDir = layout.buildDirectory.dir("generated/source/secrets/commonMain/kotlin")

val generateSecrets by tasks.registering {
    val localPropsFile = rootProject.file("local.properties")
    val outputDir = generatedSecretsDir.get().asFile.resolve("com/rubensimon/ecolens")
    val outputFile = outputDir.resolve("EcoLensSecrets.kt")

    outputs.file(outputFile)

    doLast {
        val props = Properties()
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { props.load(it) }
        }

        val supabaseUrl = (props.getProperty("SUPABASE_URL") ?: "").trim()
        val supabaseKey = (props.getProperty("SUPABASE_KEY") ?: "").trim()
        val mlBackendUrl = (props.getProperty("ML_BACKEND_URL") ?: "").trim()

        fun escapeKotlin(value: String): String =
            value.replace("\\", "\\\\").replace("\"", "\\\"")

        outputDir.mkdirs()
        outputFile.writeText(
            """
            package com.rubensimon.ecolens
            
            /**
             * Archivo generado automáticamente desde local.properties.
             * NO versionar este archivo: vive en build/.
             */
            object EcoLensSecrets {
                const val SUPABASE_URL: String = "${escapeKotlin(supabaseUrl)}"
                const val SUPABASE_KEY: String = "${escapeKotlin(supabaseKey)}"
                const val ML_BACKEND_URL: String = "${escapeKotlin(mlBackendUrl)}"
            }
            """.trimIndent()
        )
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            // Enlace explícito de frameworks de sistema
            linkerOpts("-framework", "AVFoundation", "-framework", "AVFAudio", "-framework", "AudioToolbox", "-framework", "UserNotifications", "-framework", "MapKit", "-framework", "PhotosUI")
        }
    }
    
    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(generatedSecretsDir)
        }

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            // Ktor Android engine (Using OkHttp for WebSockets support)
            implementation(libs.ktor.client.okhttp)
            
            // Maps, Location, CameraX & QR Code
            implementation("com.google.android.gms:play-services-maps:18.2.0")
            implementation("com.google.android.gms:play-services-location:21.2.0")
            implementation("com.google.mlkit:image-labeling:17.0.8")
            implementation("com.google.mlkit:barcode-scanning:17.2.0")
            implementation("androidx.camera:camera-core:1.3.0")
            implementation("androidx.camera:camera-camera2:1.3.0")
            implementation("androidx.camera:camera-lifecycle:1.3.0")
            implementation("androidx.camera:camera-view:1.3.0")
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            implementation("com.google.zxing:core:3.5.3")
        }

        commonMain.dependencies {
            // ── Compose Multiplatform ──────────────────────────────────────
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // ── Supabase KMP v3 ───────────────────────────────────────────
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.realtime)

            // ── Ktor core ─────────────────────────────────────────────────
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.websockets)

            // ── Kotlinx ───────────────────────────────────────────────────
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)

            // ── Multiplatform Settings (reemplaza SharedPreferences) ───────
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.noarg)

            // ── Coil 3 (async image) ───────────────────────────────────────
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }

        iosMain.dependencies {
            // Ktor iOS engine
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.configureEach {
    if (name.startsWith("compile") && name.contains("Kotlin")) {
        dependsOn(generateSecrets)
    }
}

android {
    namespace = "com.rubensimon.ecolens"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.rubensimon.ecolens"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { localProps.load(it) }
        }
        manifestPlaceholders["MAPS_API_KEY"] = localProps.getProperty("MAPS_API_KEY") ?: ""
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}
