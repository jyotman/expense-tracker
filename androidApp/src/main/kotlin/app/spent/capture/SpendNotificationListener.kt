package app.spent.capture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.spent.AppInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reads notifications from other apps once the user grants "Notification access". Each posted
 * notification from a finance app is handed to [NotificationCaptureProcessor]. Everything else is
 * ignored. All parsing happens on-device.
 *
 * Lives in the app module (not the shared library) so it is always packaged — manifest-declared
 * components in the KMP library can be tree-shaken away if nothing references them in code.
 */
class SpendNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val sbnSafe = sbn ?: return
        if (sbnSafe.packageName == applicationContext.packageName) return // ignore our own

        val extras = sbnSafe.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            )?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        val appLabel = runCatching {
            val pm = applicationContext.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(sbnSafe.packageName, 0)).toString()
        }.getOrDefault(sbnSafe.packageName)

        val notifKey = sbnSafe.key
        val postedAt = sbnSafe.postTime
        scope.launch {
            AppInitializer.init(applicationContext)
            runCatching {
                NotificationCaptureProcessor.process(notifKey, sbnSafe.packageName, appLabel, title, text, postedAt)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) { /* not needed */ }
}
