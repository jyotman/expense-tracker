package app.expensetracker.platform

import android.app.Activity
import android.content.Intent
import app.expensetracker.CurrentActivity
import kotlinx.coroutines.CompletableDeferred

/** Android backup via the Storage Access Framework — the user picks the destination/source file. */
actual object Backup {
    actual val supported: Boolean = true

    actual suspend fun export(payload: String, suggestedName: String): Result<String> = runCatching {
        val activity = CurrentActivity.get() ?: error("App not in foreground")
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/json")
            .putExtra(Intent.EXTRA_TITLE, suggestedName)
        val data = SafRelay.launch(activity, intent, SafRelay.REQ_EXPORT) ?: error("Cancelled")
        val uri = data.data ?: error("No file chosen")
        activity.contentResolver.openOutputStream(uri)?.use { it.write(payload.encodeToByteArray()) }
            ?: error("Couldn't open file for writing")
        suggestedName
    }

    actual suspend fun import(): Result<String> = runCatching {
        val activity = CurrentActivity.get() ?: error("App not in foreground")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/json")
        val data = SafRelay.launch(activity, intent, SafRelay.REQ_IMPORT) ?: error("Cancelled")
        val uri = data.data ?: error("No file chosen")
        activity.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Couldn't read file")
    }
}

/** Bridges the SAF picker's Activity result back to the suspending export/import calls. */
object SafRelay {
    const val REQ_EXPORT = 4712
    const val REQ_IMPORT = 4713

    @Volatile
    private var pending: CompletableDeferred<Intent?>? = null

    suspend fun launch(activity: Activity, intent: Intent, requestCode: Int): Intent? {
        val deferred = CompletableDeferred<Intent?>()
        pending = deferred
        activity.startActivityForResult(intent, requestCode)
        return deferred.await()
    }

    fun complete(data: Intent?) {
        pending?.complete(data)
        pending = null
    }
}
