@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.spent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.spent.ServiceLocator
import app.spent.backup.BackupService
import app.spent.platform.Backup
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
    val lastBackupMillis: Long = 0,
    val busy: Boolean = false,
    val message: String? = null,
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
                    _state.value = _state.value.copy(busy = false, lastBackupMillis = now.toEpochMilliseconds(), message = "Saved “$it”")
                }
                .onFailure { _state.value = _state.value.copy(busy = false, message = it.message ?: "Export cancelled") }
        }
    }

    fun importNow() {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, message = null)
        viewModelScope.launch {
            Backup.import()
                .onSuccess { payload ->
                    runCatching { BackupService.restore(payload) }
                        .onSuccess { _state.value = _state.value.copy(busy = false, message = "Restored — reopen the tabs to see your data") }
                        .onFailure { _state.value = _state.value.copy(busy = false, message = "That file wasn't a valid Spent backup") }
                }
                .onFailure { _state.value = _state.value.copy(busy = false, message = it.message ?: "Import cancelled") }
        }
    }

    private fun suggestedName(now: Instant): String {
        val d = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val m = d.month.number.toString().padStart(2, '0')
        val day = d.day.toString().padStart(2, '0')
        return "spent-backup-${d.year}-$m-$day.json"
    }
}
