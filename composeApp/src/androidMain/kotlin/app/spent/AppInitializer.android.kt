package app.spent

import android.content.Context
import app.spent.db.Database
import app.spent.db.androidSqlDriver
import app.spent.storage.SettingsStorage
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
            AppContext.init(context)
            val prefs = context.applicationContext.getSharedPreferences("spent_prefs", Context.MODE_PRIVATE)
            SettingsStorage.init { SharedPreferencesSettings(prefs) }
            Database.init { androidSqlDriver(context) }
            initialized = true
        }
    }
}
