package app.expensetracker.storage

import app.expensetracker.data.CurrencyMeta
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

/**
 * User preferences (default currency, feature toggles, backup bookkeeping).
 * Transaction data lives in SQLDelight; this is only lightweight key-value state.
 */
class SettingsStorage(private val settings: Settings) {

    /**
     * The user's home currency as an ISO 4217 code (e.g. "SGD"), or null until they explicitly pick
     * one. All amounts are stored in this currency; it's a display/labelling choice, not a per-expense
     * fact. Changing it does NOT re-convert existing expenses.
     */
    var defaultCurrencyCode: String?
        get() = settings.getStringOrNull(KEY_CURRENCY_CODE)
        set(value) {
            if (value == null) settings.remove(KEY_CURRENCY_CODE) else settings[KEY_CURRENCY_CODE] = value
        }

    /**
     * Display symbol for amounts — derived from [defaultCurrencyCode] once chosen, otherwise the
     * legacy stored symbol (for installs predating the code-based setting). The currency picker sets
     * [defaultCurrencyCode]; the setter here only writes the legacy symbol fallback.
     */
    var currencySymbol: String
        get() = defaultCurrencyCode?.let { CurrencyMeta.symbolFor(it) } ?: settings.getString(KEY_CURRENCY, "$")
        set(value) { settings[KEY_CURRENCY] = value }

    /** Whether the user opted into reading notifications for auto-capture (Android). */
    var notificationCaptureEnabled: Boolean
        get() = settings.getBoolean(KEY_CAPTURE_ENABLED, false)
        set(value) { settings[KEY_CAPTURE_ENABLED] = value }

    /** Whether on-device AI parsing is enabled (vs. rules-only). */
    var aiEnabled: Boolean
        get() = settings.getBoolean(KEY_AI_ENABLED, true)
        set(value) { settings[KEY_AI_ENABLED] = value }

    var lastBackupAtMillis: Long
        get() = settings.getLong(KEY_LAST_BACKUP, 0L)
        set(value) { settings[KEY_LAST_BACKUP] = value }

    var driveConnectedEmail: String?
        get() = settings.getStringOrNull(KEY_DRIVE_EMAIL)
        set(value) {
            if (value == null) settings.remove(KEY_DRIVE_EMAIL) else settings[KEY_DRIVE_EMAIL] = value
        }

    /** App packages whose notifications should be parsed as transactions (comma-separated). */
    var capturePackages: Set<String>
        get() = settings.getStringOrNull(KEY_CAPTURE_PACKAGES)
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
            ?: emptySet()
        set(value) { settings[KEY_CAPTURE_PACKAGES] = value.joinToString(",") }

    /**
     * True once the user has explicitly configured their capture app list (via the picker or
     * seeding on first open). Distinguishes "deliberately empty" from "never configured", so
     * the processor can fall back to built-in defaults only when this is false.
     */
    var capturePackagesConfigured: Boolean
        get() = settings.getBoolean(KEY_CAPTURE_CONFIGURED, false)
        set(value) { settings[KEY_CAPTURE_CONFIGURED] = value }

    companion object {
        private const val KEY_CURRENCY = "currency_symbol"
        private const val KEY_CURRENCY_CODE = "default_currency_code"
        private const val KEY_CAPTURE_ENABLED = "capture_enabled"
        private const val KEY_AI_ENABLED = "ai_enabled"
        private const val KEY_LAST_BACKUP = "last_backup_at"
        private const val KEY_DRIVE_EMAIL = "drive_email"
        private const val KEY_CAPTURE_PACKAGES = "capture_packages"
        private const val KEY_CAPTURE_CONFIGURED = "capture_configured"

        private lateinit var settingsFactory: () -> Settings

        fun init(factory: () -> Settings) {
            settingsFactory = factory
        }

        val instance: SettingsStorage by lazy { SettingsStorage(settingsFactory()) }

        /** A fresh Settings handle over the same underlying store, for components that own their own keys. */
        fun createSettings(): Settings = settingsFactory()
    }
}
