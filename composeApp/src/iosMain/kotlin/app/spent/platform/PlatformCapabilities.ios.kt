package app.spent.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual object PlatformCapabilities {
    // iOS cannot read other apps' notifications, so auto-capture is unsupported here.
    actual val notificationCaptureSupported: Boolean = false
    actual fun isNotificationAccessGranted(): Boolean = false
    actual fun openNotificationAccessSettings() { /* no-op on iOS */ }
    actual val onDeviceAiPossible: Boolean = false
    actual suspend fun probeOnDeviceAi(): AiAvailability = AiAvailability.UNAVAILABLE
    actual fun downloadOnDeviceAiModel(): Flow<AiModelDownload> = emptyFlow()
}
