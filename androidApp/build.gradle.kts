import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// Crashlytics is staged but stays inert until a Firebase config is added. The google-services
// plugin fails the build if google-services.json is missing, so we only apply the Firebase
// plugins once that file is present (see RELEASE_TODO.md). Until then the app builds and runs
// normally with crash reporting simply disabled.
val hasGoogleServices = file("google-services.json").exists()
if (hasGoogleServices) {
    apply(plugin = libs.plugins.googleServices.get().pluginId)
    apply(plugin = libs.plugins.firebaseCrashlytics.get().pluginId)
}

android {
    namespace = "app.expensetracker"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.expensetracker"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Debug-key placeholder so `assembleRelease` produces an installable APK for local
            // R8 smoke-testing. The Play upload AAB is signed via the IntelliJ "Generate Signed
            // App Bundle" wizard with the upload keystore — see README "Release build & signing".
            signingConfig = signingConfigs.getByName("debug")

            if (hasGoogleServices) {
                // Upload the R8 mapping so Crashlytics can deobfuscate release stack traces.
                configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                    mappingFileUploadEnabled = true
                }
            }
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            isDebuggable = true
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "false"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        // MainActivity is a ComponentActivity and uses the ActivityResult API directly (no
        // Fragments), so this Fragment-version check is a false positive. It only surfaces in
        // lintVitalRelease, where it would otherwise fail the release build.
        disable += "InvalidFragmentVersionForActivityResult"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.glance.appwidget)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
