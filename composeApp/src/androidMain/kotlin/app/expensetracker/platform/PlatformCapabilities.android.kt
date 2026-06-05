package app.expensetracker.platform

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import app.expensetracker.AppContext
import co.touchlab.kermit.Logger
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual object PlatformCapabilities {
    private val log = Logger.withTag("OnDeviceAi")

    actual val notificationCaptureSupported: Boolean = true

    actual fun isNotificationAccessGranted(): Boolean {
        val context = AppContext.context
        val listener = ComponentName(context, "app.expensetracker.capture.SpendNotificationListener")
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(":").any { entry ->
            ComponentName.unflattenFromString(entry)?.let {
                it.packageName == context.packageName &&
                    (it == listener || it.className == listener.className)
            } == true
        }
    }

    actual fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        AppContext.context.startActivity(intent)
    }

    /** ML Kit GenAI (Gemini Nano) requires API 26+. The app's minSdk (29) already guarantees this;
     *  whether a model actually exists is decided by [probeOnDeviceAi]. */
    actual val onDeviceAiPossible: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    /**
     * Ask ML Kit for the on-device feature status. Maps the GenAI [FeatureStatus] directly onto our
     * [AiAvailability]; no inference and no download happen here.
     */
    actual suspend fun probeOnDeviceAi(): AiAvailability {
        if (!onDeviceAiPossible) return AiAvailability.UNAVAILABLE
        val model = runCatching { Generation.getClient() }.getOrElse {
            log.w(it) { "Could not create GenAI client" }
            return AiAvailability.UNAVAILABLE
        }
        return try {
            when (model.checkStatus()) {
                FeatureStatus.AVAILABLE -> AiAvailability.AVAILABLE
                FeatureStatus.DOWNLOADABLE -> AiAvailability.DOWNLOADABLE
                FeatureStatus.DOWNLOADING -> AiAvailability.DOWNLOADING
                else -> AiAvailability.UNAVAILABLE
            }
        } catch (e: Throwable) {
            log.w(e) { "AI status check failed" }
            AiAvailability.UNAVAILABLE
        } finally {
            runCatching { model.close() }
        }
    }

    /**
     * Trigger the model download and translate ML Kit's [DownloadStatus] stream into [AiModelDownload].
     * Progress is reported as a fraction once the total size is known.
     */
    actual fun getInstalledApps(): List<InstalledApp> {
        val pm = AppContext.context.packageManager
        return pm.getInstalledApplications(0)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { InstalledApp(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }
    }

    actual fun downloadOnDeviceAiModel(): Flow<AiModelDownload> = flow {
        val model = Generation.getClient()
        try {
            var totalBytes = 0L
            model.download().collect { status ->
                when (status) {
                    is DownloadStatus.DownloadStarted -> {
                        totalBytes = status.bytesToDownload
                        emit(AiModelDownload.Progress(if (totalBytes > 0) 0f else null))
                    }
                    is DownloadStatus.DownloadProgress -> {
                        val fraction = if (totalBytes > 0) {
                            (status.totalBytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                        } else null
                        emit(AiModelDownload.Progress(fraction))
                    }
                    is DownloadStatus.DownloadCompleted -> emit(AiModelDownload.Done)
                    is DownloadStatus.DownloadFailed -> {
                        log.w(status.e) { "Model download failed" }
                        emit(AiModelDownload.Failed(status.e.message))
                    }
                }
            }
        } finally {
            runCatching { model.close() }
        }
    }
}
