package app.expensetracker

import android.content.Context
import android.content.pm.ApplicationInfo
import app.expensetracker.db.Database
import app.expensetracker.db.androidSqlDriver
import app.expensetracker.storage.SettingsStorage
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Wires storage + database for every Android entry point (Activity, notification service,
 * widget). Safe to call multiple times — the underlying holders are idempotent.
 */
object AppInitializer {
    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            configureLogging(isDebugBuild = debuggable)
            AppContext.init(context)
            val prefs = context.applicationContext.getSharedPreferences("expensetracker_prefs", Context.MODE_PRIVATE)
            SettingsStorage.init { SharedPreferencesSettings(prefs) }
            Database.init { androidSqlDriver(context) }
            initialized = true
        }
    }
}
