plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt) // Usamos el alias del TOML para Hilt
}

android {
    namespace = "com.example.gastocheck"
    compileSdk = 35 // Te recomiendo usar 35 (Android 15), 36 es experimental/beta

    defaultConfig {
        applicationId = "com.example.gastocheck"
        minSdk = 24
        targetSdk = 35 // Te recomiendo usar 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    configurations.all {
        resolutionStrategy {
            // 🛑 OBLIGAMOS A GRADLE A USAR LAS VERSIONES ANTIGUAS (ESTABLES)
            // Esto evita que 'activity-compose' u otras suban la versión a 1.7.0
            force("androidx.compose.foundation:foundation:1.6.8")
            force("androidx.compose.foundation:foundation-layout:1.6.8")
            force("androidx.compose.ui:ui:1.6.8")
            force("androidx.compose.ui:ui-tooling-preview:1.6.8")
            force("androidx.compose.animation:animation:1.6.8")
            force("androidx.compose.animation:animation-core:1.6.8")
            force("androidx.compose.runtime:runtime:1.6.8")
            force("androidx.compose.material3:material3:1.2.1")
        }
    }
}

dependencies {
    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // -------------------------------------------------------------
    // 🛑 SOLUCIÓN DEL CRASH (Versiones de Compose)
    // -------------------------------------------------------------

    // 1. ELIMINAMOS la línea que jalaba la versión nueva:
    // implementation(platform(libs.androidx.compose.bom))  <-- BORRADA

    // 2. FORZAMOS el BOM de Junio 2024 (Equivale a Compose 1.6.8 stable)
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))

    // 3. Importamos las librerías SIN versión (el BOM decide)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // 4. FORZAMOS Foundation a 1.6.8 explícitamente para asegurar compatibilidad
    implementation("androidx.compose.foundation:foundation:1.6.8")

    // 5. LIBRERÍA REORDERABLE (Corregido el nombre, tenías un typo)
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")
    // -------------------------------------------------------------

    // --- ROOM ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.foundation)
    ksp(libs.androidx.room.compiler)

    // --- HILT ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.hilt.common)
    implementation(libs.androidx.hilt.work)

    // --- NAVEGACIÓN Y OTROS ---
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.material.icons.extended)
    implementation(libs.androidx.compose.animation.core) // Opcional, el BOM ya lo trae

    // --- NETWORKING ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")

    // Configuraciones Java 8+
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("org.jetbrains:annotations:23.0.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00")) // BOM para tests también
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}