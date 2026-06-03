package app.expensetracker.platform

/**
 * Manual backup transport: hands a serialized snapshot to the OS file picker so the user can
 * save it anywhere (their Drive, Dropbox, local storage) and read one back to restore — no
 * server, no cloud credentials. Pairs with the OS's automatic app-data backup as a safety net.
 */
expect object Backup {
    val supported: Boolean

    /** Let the user choose where to save [payload]. Returns the chosen file's display name. */
    suspend fun export(payload: String, suggestedName: String): Result<String>

    /** Let the user pick a previously exported file and return its contents. */
    suspend fun import(): Result<String>
}
