import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val releaseSigningPropertiesFile = rootProject.file("release-signing/keystore.properties")
val releaseSigningEnvironment = mapOf(
    "storeFile" to "SLEEPDOWN_KEYSTORE_FILE",
    "storePassword" to "SLEEPDOWN_KEYSTORE_PASSWORD",
    "keyAlias" to "SLEEPDOWN_KEY_ALIAS",
    "keyPassword" to "SLEEPDOWN_KEY_PASSWORD",
).mapValues { (_, variable) -> providers.environmentVariable(variable).orNull }
val useEnvironmentSigning = releaseSigningEnvironment.values.any { it != null }
val releaseSigningProperties = Properties().apply {
    if (useEnvironmentSigning) {
        require(releaseSigningEnvironment.values.all { !it.isNullOrBlank() }) {
            "Release signing requires all four SLEEPDOWN_KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD environment variables"
        }
        releaseSigningEnvironment.forEach { (key, value) -> setProperty(key, requireNotNull(value)) }
    } else if (releaseSigningPropertiesFile.isFile) {
        releaseSigningPropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = useEnvironmentSigning || releaseSigningPropertiesFile.isFile

android {
    namespace = "com.letr.sleepdown"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.letr.sleepdown"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.1.0-beta.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseSigningProperties.getProperty("storeFile"))
                storePassword = releaseSigningProperties.getProperty("storePassword")
                keyAlias = releaseSigningProperties.getProperty("keyAlias")
                keyPassword = releaseSigningProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(project(":shared"))
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
