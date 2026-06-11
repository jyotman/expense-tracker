@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.expensetracker.ServiceLocator
import app.expensetracker.backup.BackupService
import app.expensetracker.platform.Backup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

data class BackupUiState(
    val supported: Boolean = false,
    val autoBackupProviderName: String = Backup.autoBackupProviderName,
    val lastBackupMillis: Long = 0,
    val busy: Boolean = false,
    val message: String? = null,
    /** Whether [message] reports a failure (drives its colour in the UI). */
    val messageIsError: Boolean = false,
)

class BackupViewModel : ViewModel() {
    private val settings = ServiceLocator.settings

    private val _state = MutableStateFlow(
        BackupUiState(supported = Backup.supported, lastBackupMillis = settings.lastBackupAtMillis)
    )
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun exportNow() {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, message = null)
        viewModelScope.launch {
            val now = Clock.System.now()
            val payload = BackupService.export(now.toEpochMilliseconds())
            Backup.export(payload, suggestedName(now))
                .onSuccess {
                    settings.lastBackupAtMillis = now.toEpochMilliseconds()
                    _state.value = _state.value.copy(busy = false, lastBackupMillis = now.toEpochMilliseconds(), message = "Saved “$it”", messageIsError = false)
                }
                .onFailure { fail(it, "Export failed") }
        }
    }

    fun importNow() {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, message = null)
        viewModelScope.launch {
            Backup.import()
                .onSuccess { payload ->
                    runCatching { BackupService.restore(payload) }
                        .onSuccess { _state.value = _state.value.copy(busy = false, message = "Restored — reopen the tabs to see your data", messageIsError = false) }
                        .onFailure { _state.value = _state.value.copy(busy = false, message = "That file wasn't a valid Expense Tracker backup", messageIsError = true) }
                }
                .onFailure { fail(it, "Import failed") }
        }
    }

    /** Backing out of the system file picker isn't an error — clear busy and stay quiet. */
    private fun fail(e: Throwable, fallback: String) {
        val cancelled = e.message == "Cancelled"
        _state.value = _state.value.copy(
            busy = false,
            message = if (cancelled) null else (e.message ?: fallback),
            messageIsError = !cancelled,
        )
    }

    private fun suggestedName(now: Instant): String {
        val d = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val m = d.month.number.toString().padStart(2, '0')
        val day = d.day.toString().padStart(2, '0')
        return "expense-tracker-backup-${d.year}-$m-$day.json"
    }
}
