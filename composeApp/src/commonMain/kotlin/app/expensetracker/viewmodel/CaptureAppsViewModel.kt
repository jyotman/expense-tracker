package app.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.expensetracker.ServiceLocator
import app.expensetracker.capture.CaptureRules
import app.expensetracker.platform.InstalledApp
import app.expensetracker.platform.PlatformCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CaptureAppsUiState(
    val loading: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
) {
    val filtered: List<InstalledApp>
        get() = if (searchQuery.isBlank()) apps
                else apps.filter {
                    it.label.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
                }
}

class CaptureAppsViewModel : ViewModel() {
    private val settings = ServiceLocator.settings
    private val _state = MutableStateFlow(CaptureAppsUiState())
    val state: StateFlow<CaptureAppsUiState> = _state.asStateFlow()

    init {
        // PackageManager calls are blocking I/O — use the platform IO dispatcher.
        viewModelScope.launch(PlatformCapabilities.ioDispatcher) {
            val installed = PlatformCapabilities.getInstalledApps()
            val installedPackages = installed.map { it.packageName }.toSet()
            var selected = settings.capturePackages
            if (!settings.capturePackagesConfigured) {
                // First open: pre-select whichever defaults are on this device, then mark
                // configured so the processor stops using the built-in fallback and the
                // seeding coroutine doesn't re-run on every screen visit.
                selected = CaptureRules.defaultPackages.intersect(installedPackages)
                settings.capturePackages = selected
                settings.capturePackagesConfigured = true
            }
            _state.update { it.copy(loading = false, apps = installed, selectedPackages = selected) }
        }
    }

    fun toggle(packageName: String) {
        // Perform the read-modify-write inside update{} so concurrent rapid taps always
        // operate on the latest state rather than a stale snapshot.
        _state.update { current ->
            val s = current.selectedPackages
            val updated = if (packageName in s) s - packageName else s + packageName
            settings.capturePackages = updated
            settings.capturePackagesConfigured = true
            current.copy(selectedPackages = updated)
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }
}
