package app.spent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.spent.ServiceLocator
import app.spent.platform.AiAvailability
import app.spent.platform.AiModelDownload
import app.spent.platform.PlatformCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val currencySymbol: String = "$",
    val aiEnabled: Boolean = true,
    val captureEnabled: Boolean = false,
    val captureSupported: Boolean = false,
    val notificationAccessGranted: Boolean = false,
    val lastBackupAtMillis: Long = 0,
    /** Result of the last on-device AI probe; UNAVAILABLE up-front when the device can't run it. */
    val aiAvailability: AiAvailability = AiAvailability.UNKNOWN,
    /** A probe is in flight. */
    val aiChecking: Boolean = false,
    /** Model-download progress (0f..1f), or null when not downloading / size unknown. */
    val aiDownloadFraction: Float? = null,
)

class SettingsViewModel : ViewModel() {
    private val settings = ServiceLocator.settings

    private val _state = MutableStateFlow(read(null))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private fun read(prev: SettingsUiState?) = SettingsUiState(
        currencySymbol = settings.currencySymbol,
        aiEnabled = settings.aiEnabled,
        captureEnabled = settings.notificationCaptureEnabled,
        captureSupported = PlatformCapabilities.notificationCaptureSupported,
        notificationAccessGranted = PlatformCapabilities.isNotificationAccessGranted(),
        lastBackupAtMillis = settings.lastBackupAtMillis,
        // Devices that can't possibly run on-device AI are known-unavailable without a probe.
        aiAvailability = if (!PlatformCapabilities.onDeviceAiPossible) AiAvailability.UNAVAILABLE
            else prev?.aiAvailability ?: AiAvailability.UNKNOWN,
        aiChecking = prev?.aiChecking ?: false,
        aiDownloadFraction = prev?.aiDownloadFraction,
    )

    /** Re-read state (e.g. after returning from the system notification-access screen). */
    fun refresh() { _state.value = read(_state.value) }

    fun setCurrencySymbol(symbol: String) {
        settings.currencySymbol = symbol.ifBlank { "$" }
        refresh()
    }

    fun setAiEnabled(enabled: Boolean) {
        settings.aiEnabled = enabled
        refresh()
        // Turning AI on is an explicit user action — the right (and only) time to run the probe,
        // since it may trigger a model download.
        if (enabled) checkAiAvailability()
    }

    /** Foreground probe for on-device AI. Safe to call repeatedly (no-op while one is running). */
    fun checkAiAvailability() {
        if (_state.value.aiChecking || _state.value.aiDownloadFraction != null) return
        if (!PlatformCapabilities.onDeviceAiPossible) {
            _state.update { it.copy(aiAvailability = AiAvailability.UNAVAILABLE) }
            return
        }
        _state.update { it.copy(aiChecking = true) }
        viewModelScope.launch {
            val result = PlatformCapabilities.probeOnDeviceAi()
            // Don't leave the toggle on for a device that can't run it.
            if (result == AiAvailability.UNAVAILABLE) settings.aiEnabled = false
            _state.update {
                it.copy(aiChecking = false, aiAvailability = result, aiEnabled = settings.aiEnabled)
            }
            // Model present but not downloaded (or already downloading) → fetch it, streaming progress.
            if (result == AiAvailability.DOWNLOADABLE || result == AiAvailability.DOWNLOADING) downloadModel()
        }
    }

    /** Download the on-device model, reflecting progress into the UI. No-op if already downloading. */
    fun downloadModel() {
        if (_state.value.aiDownloadFraction != null) return
        _state.update { it.copy(aiAvailability = AiAvailability.DOWNLOADING, aiDownloadFraction = 0f) }
        viewModelScope.launch {
            PlatformCapabilities.downloadOnDeviceAiModel().collect { progress ->
                when (progress) {
                    is AiModelDownload.Progress ->
                        _state.update { it.copy(aiAvailability = AiAvailability.DOWNLOADING, aiDownloadFraction = progress.fraction ?: 0f) }
                    AiModelDownload.Done ->
                        _state.update { it.copy(aiAvailability = AiAvailability.AVAILABLE, aiDownloadFraction = null) }
                    is AiModelDownload.Failed ->
                        // Leave the toggle on; the form still works via rules and the user can retry.
                        _state.update { it.copy(aiAvailability = AiAvailability.DOWNLOADABLE, aiDownloadFraction = null) }
                }
            }
        }
    }

    fun setCaptureEnabled(enabled: Boolean) {
        settings.notificationCaptureEnabled = enabled
        refresh()
    }

    fun openNotificationAccessSettings() = PlatformCapabilities.openNotificationAccessSettings()
}
