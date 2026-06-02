package app.spent.platform

// iOS export/import via UIDocumentPicker is wired in the iOS app phase; iCloud auto-backup
// covers the automatic case in the meantime.
actual object Backup {
    actual val supported: Boolean = false
    actual suspend fun export(payload: String, suggestedName: String): Result<String> =
        Result.failure(NotImplementedError("Not available on iOS yet"))
    actual suspend fun import(): Result<String> =
        Result.failure(NotImplementedError("Not available on iOS yet"))
}
