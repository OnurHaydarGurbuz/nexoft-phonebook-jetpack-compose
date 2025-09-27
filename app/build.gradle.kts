import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// --- local.properties / env'den API key yükle ---
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val apiKeyFromLocal = localProps.getProperty("NEXOFT_API_KEY")
val apiKeyFromEnv = System.getenv("NEXOFT_API_KEY")
val resolvedApiKey = apiKeyFromLocal ?: apiKeyFromEnv ?: ""

android {
    namespace = "com.example.nexoft"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.nexoft"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig'lere güvenli yaz
        buildConfigField("String", "API_KEY", "\"$resolvedApiKey\"")
        buildConfigField("String", "BASE_URL", "\"http://146.59.52.68:11235/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // istersen debug'ta ekstra şeyler koyabilirsin
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform(libs.androidx.compose.bom.v20250901))
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.ui)
    implementation(libs.material3)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)

    implementation(libs.androidx.activity.compose.v1110)
    implementation(libs.androidx.navigation.compose)

    // Coil
    implementation(libs.coil.compose)

    // Lottie
    implementation(libs.lottie.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.logging.interceptor)
    implementation(libs.moshi.kotlin) // <-- moshi-kotlin adaptörü
}
