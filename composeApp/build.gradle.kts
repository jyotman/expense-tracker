import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidLibrary {
        namespace = "app.expensetracker.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        withHostTest { }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.work.runtime)
            implementation(libs.glance.appwidget)
            implementation(libs.glance.material3)
            implementation(libs.mlkit.genai.prompt)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kermit)
            implementation(libs.navigation.compose)
            implementation(libs.vico.multiplatform.m3)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.primitive.adapters)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.turbine)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.multiplatform.settings.test)
            implementation(libs.ktor.client.mock)
        }
        val androidHostTest by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}

sqldelight {
    databases {
        create("ExpenseTrackerDatabase") {
            packageName.set("app.expensetracker.db")
            // Where versioned schema snapshots (1.db, 2.db, …) are written/read. Each snapshot is the
            // schema at a given version; migrations are verified by replaying them onto these.
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            // Build fails if the .sq schema and the .sqm migration chain ever disagree, so an update
            // can never ship a missing/incorrect migration. See "Database migrations" in CLAUDE.md.
            verifyMigrations.set(true)
        }
    }
}

// SQLDelight only wires migration verification into `check` by default. Also run it as part of the
// host unit tests (the day-to-day command in CLAUDE.md) so schema drift is caught locally, not just CI.
tasks.matching { it.name == "testAndroidHostTest" }.configureEach {
    dependsOn("verifyCommonMainExpenseTrackerDatabaseMigration")
}

buildkonfig {
    packageName = "app.expensetracker"

    defaultConfigs {
        buildConfigField(BOOLEAN, "IS_RELEASE", "false")
    }

    defaultConfigs("prod") {
        buildConfigField(BOOLEAN, "IS_RELEASE", "true")
    }
}
