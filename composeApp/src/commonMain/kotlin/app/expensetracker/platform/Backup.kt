package app.expensetracker.platform

/**
 * Manual backup transport: hands a serialized snapshot to the OS file picker so the user can
 * save it anywhere (their Drive, Dropbox, local storage) and read one back to restore — no
 * server, no cloud credentials. Pairs with the OS's automatic app-data backup as a safety net.
 */
expect object Backup {
    val supported: Boolean

    /** Human-readable name of the OS automatic backup service ("Google One", "iCloud", …). */
    val autoBackupProviderName: String

    /** Whether the platform can deep-link directly to the OS backup settings screen. */
    val canOpenAutoBackupSettings: Boolean

    /** Let the user choose where to save [payload]. Returns the chosen file's display name. */
    suspend fun export(payload: String, suggestedName: String): Result<String>

    /** Let the user pick a previously exported file and return its contents. */
    suspend fun import(): Result<String>

    /** Open the OS backup settings screen. No-op if [canOpenAutoBackupSettings] is false. */
    fun openAutoBackupSettings()
}
