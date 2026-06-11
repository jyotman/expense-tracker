# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for readable (and Crashlytics-deobfuscatable) stack traces.
-keepattributes SourceFile,LineNumberTable
#-renamesourcefileattribute SourceFile

# --- Kotlinx Serialization -------------------------------------------------
# Powers GenAI JSON parsing (GenAiResponse), backup export/import, and FX models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-dontnote kotlinx.serialization.SerializationKt
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-dontwarn kotlinx.serialization.internal.**

# --- Ktor client (FX rates over HTTPS) -------------------------------------
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn java.lang.management.**
-dontwarn javax.management.**

# --- App code (includes SQLDelight-generated app.expensetracker.db.** + BuildKonfig) ---
-keep class app.expensetracker.** { *; }
-keepclassmembers class app.expensetracker.** { *; }

# --- SQLDelight runtime ----------------------------------------------------
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# --- ML Kit GenAI (Gemini Nano via Prompt API) -----------------------------
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.mlkit.**

# --- WorkManager + Room ----------------------------------------------------
# WorkManager auto-initializes on startup via androidx.startup and builds its Room database
# (WorkDatabase). Room generates *_Impl classes instantiated reflectively by a no-arg
# constructor, which R8 full-mode strips without these rules (crash:
# NoSuchMethodException androidx.work.impl.WorkDatabase_Impl.<init>).
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}

# --- Jetpack Glance widget -------------------------------------------------
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

# --- Vico charts -----------------------------------------------------------
-dontwarn com.patrykandpatrick.vico.**

# --- Compose ---------------------------------------------------------------
# Do NOT blanket-keep androidx.compose.**: it pins the entire material-icons-extended
# library (~10k icon classes / ~17 MB dex) and defeats R8 tree-shaking. Compose ships its
# own consumer R8 rules; only suppress warnings here.
-dontwarn androidx.compose.**

# --- Multiplatform Settings ------------------------------------------------
-keep class com.russhwolf.settings.** { *; }
-keepclassmembers class com.russhwolf.settings.** { *; }

# --- Navigation Compose ----------------------------------------------------
-keep class androidx.navigation.** { *; }
-keepclassmembers class androidx.navigation.** { *; }

# --- Enums -----------------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
