package app.expensetracker

import app.expensetracker.db.Database
import app.expensetracker.db.iosSqlDriver
import app.expensetracker.storage.SettingsStorage
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
