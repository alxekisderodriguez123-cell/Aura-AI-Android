plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

val auraKeystorePath = System.getenv("AURA_KEYSTORE_PATH")
val auraKeystorePassword = System.getenv("AURA_KEYSTORE_PASSWORD")
val auraKeyAlias = System.getenv("AURA_KEY_ALIAS")
val auraKeyPassword = System.getenv("AURA_KEY_PASSWORD")

val auraSigningValues = listOf(
    auraKeystorePath,
    auraKeystorePassword,
    auraKeyAlias,
    auraKeyPassword
)

val hasAuraSigning = auraSigningValues.all {
    !it.isNullOrBlank()
}

val hasPartialAuraSigning = auraSigningValues.any {
    !it.isNullOrBlank()
}

if (hasPartialAuraSigning && !hasAuraSigning) {
    throw GradleException(
        "Aura signing configuration is incomplete. Check GitHub Actions secrets."
    )
}

android {
    namespace = "com.aura.ai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aura.ai"
        minSdk = 26
        targetSdk = 36

        versionCode = 2
        versionName = "0.3.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasAuraSigning) {
            create("aura") {
                storeFile = file(auraKeystorePath!!)
                storePassword = auraKeystorePassword
                keyAlias = auraKeyAlias
                keyPassword = auraKeyPassword
                storeType = "JKS"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (hasAuraSigning) {
                signingConfig = signingConfigs.getByName("aura")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-ai")

    debugImplementation(
        "com.google.firebase:firebase-appcheck-debug"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2"
    )

    debugImplementation(libs.androidx.compose.ui.tooling)
}
