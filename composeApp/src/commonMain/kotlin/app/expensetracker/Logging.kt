package app.expensetracker

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity

/**
 * Single place that decides log verbosity, shared by both platform app initializers.
 *
 * In a debuggable/development build we keep everything (Verbose). In a release build we raise the
 * floor to Info so app internals — including any captured-notification context — never reach the
 * device log in production.
 *
 * Each platform passes its own debug signal (Android: the APK's `FLAG_DEBUGGABLE`; iOS:
 * `Platform.isDebugBinary`), so the gate tracks the actual build type. The policy itself stays
 * here so Android and iOS never drift.
 */
fun configureLogging(isDebugBuild: Boolean) {
    Logger.setMinSeverity(if (isDebugBuild) Severity.Verbose else Severity.Info)
}
