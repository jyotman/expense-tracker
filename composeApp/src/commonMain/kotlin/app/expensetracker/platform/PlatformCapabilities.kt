package app.expensetracker.platform

import kotlinx.coroutines.flow.Flow

/** Result of probing whether on-device generative AI (Gemini Nano) can run here. */
enum class AiAvailability {
    /** Not probed yet — status unknown. */
    UNKNOWN,

    /** The model is present and the inference engine is ready. */
    AVAILABLE,

    /** Supported here, but the model still needs to be downloaded (call the download flow). */
    DOWNLOADABLE,

    /** The model is currently downloading — not ready yet. Track progress via the download flow. */
    DOWNLOADING,

    /** This device can't run on-device AI (no AICore / unsupported / needs a system update). */
    UNAVAILABLE,
}

/** Progress of an on-device model download. */
sealed interface AiModelDownload {
    /** Download underway. [fraction] is 0f..1f, or null while the total size is still unknown. */
    data class Progress(val fraction: Float?) : AiModelDownload

    /** Model fully downloaded and ready. */
    data object Done : AiModelDownload

    /** Download failed (often transient, e.g. no network). [reason] is best-effort. */
    data class Failed(val reason: String?) : AiModelDownload
}

/**
 * Device capabilities that differ by platform. On iOS these are currently all "unsupported"
 * stubs — reading other apps' notifications is impossible on iOS, so auto-capture is
 * Android-only (a future iOS path would parse forwarded bank emails instead).
 */
expect object PlatformCapabilities {
    /** Can this platform read other apps' notifications at all? (Android: yes, iOS: no.) */
    val notificationCaptureSupported: Boolean

    /** Has the user granted "Notification access" to this app? */
    fun isNotificationAccessGranted(): Boolean

    /** Open the system screen where the user grants/revokes notification access. */
    fun openNotificationAccessSettings()

    /**
     * Cheap, synchronous check: could this device *possibly* run on-device AI? (Android: API 31+.)
     * `true` does NOT mean the model is downloaded — call [probeOnDeviceAi] for a real answer.
     */
    val onDeviceAiPossible: Boolean

    /**
     * Quick status check for on-device AI: is the model available, downloadable, downloading, or
     * unsupported here? Cheap (no inference, no download), so safe on an explicit user action.
     */
    suspend fun probeOnDeviceAi(): AiAvailability

    /**
     * Kick off the on-device model download and emit progress until it completes or fails. Only call
     * on an explicit user action (e.g. toggling AI on) — the model can be hundreds of MB.
     */
    fun downloadOnDeviceAiModel(): Flow<AiModelDownload>

    /**
     * Return all non-system apps installed on this device, sorted by display label.
     * Used to populate the notification-capture app picker. iOS returns an empty list
     * (no notification reading anyway).
     */
    fun getInstalledApps(): List<InstalledApp>
}
