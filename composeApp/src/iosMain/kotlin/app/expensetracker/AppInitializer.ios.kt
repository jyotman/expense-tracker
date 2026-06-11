package app.expensetracker

import app.expensetracker.db.Database
import app.expensetracker.db.iosSqlDriver
import app.expensetracker.storage.SettingsStorage
import com.russhwolf.settings.NSUserDefaultsSettings
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import platform.Foundation.NSUserDefaults

object AppInitializer {
    private var initialized = false

    @OptIn(ExperimentalNativeApi::class)
    fun init() {
        if (initialized) return
        initialized = true
        configureLogging(isDebugBuild = Platform.isDebugBinary)
        SettingsStorage.init { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
        Database.init { iosSqlDriver() }
    }
}
