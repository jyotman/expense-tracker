package app.spent

import app.spent.db.Database
import app.spent.db.iosSqlDriver
import app.spent.storage.SettingsStorage
import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

object AppInitializer {
    private var initialized = false

    fun init() {
        if (initialized) return
        initialized = true
        SettingsStorage.init { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
        Database.init { iosSqlDriver() }
    }
}
