import java.io.FileInputStream
import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
}

val localSecrets = Properties()
val secretsFile = rootProject.file("secrets.properties")

if (secretsFile.isFile) {
    FileInputStream(secretsFile).use { inputStream ->
        localSecrets.load(inputStream)
    }
}

val localHfToken = (localSecrets["HF_TOKEN"] as? String).orEmpty().trim()
val localHfModelId = (localSecrets["HF_MODEL_ID"] as? String).orEmpty().trim()
val hfModelId = localHfModelId.ifEmpty { "clapAI/modernBERT-base-multilingual-sentiment" }

fun asBuildConfigString(value: String): String =
    "\"" + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "")
        .replace("\n", "") + "\""

android {
    namespace = "com.example.mooddiaryapp"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.mooddiaryapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "HF_TOKEN",
                asBuildConfigString(localHfToken)
            )
            buildConfigField(
                "String",
                "HF_MODEL_ID",
                asBuildConfigString(hfModelId)
            )
        }

        release {
            // Le token local de développement reste absent des builds release.
            buildConfigField("String", "HF_TOKEN", asBuildConfigString(""))
            buildConfigField("String", "HF_MODEL_ID", asBuildConfigString(hfModelId))

            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.google.code.gson:gson:2.15.0")

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}