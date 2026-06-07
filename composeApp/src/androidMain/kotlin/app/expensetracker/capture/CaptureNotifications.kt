package app.expensetracker.capture

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.expensetracker.AndroidEntry
import app.expensetracker.AppContext
import app.expensetracker.ServiceLocator

/** Posts one isolated "expense detected — tap to add" notification per captured transaction. */
object CaptureNotifications {
    private const val CHANNEL_ID = "expensetracker_capture"

    private fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Detected expenses", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Prompts to add expenses detected from your bank & wallet notifications"
                }
            )
        }
    }

    /** One notification per detected transaction. Distinct id ⇒ they stack, never group, and
     *  tapping one (auto-cancel) leaves the others. */
    fun showDetected(capturedId: Long, amountMinor: Long, currencyToken: String?, merchant: String?) {
        val context = AppContext.context
        ensureChannel(context)
        val homeSymbol = ServiceLocator.settings.currencySymbol
        val title = "Add expense · ${formatCapturedAmount(amountMinor, currencyToken, homeSymbol)}"
        val body = merchant?.takeIf { it.isNotBlank() }?.let { "at $it — tap to review & save" }
            ?: "Tap to review & save"
        val notifId = notifId(capturedId)

        val pending = PendingIntent.getActivity(
            context,
            notifId,
            AndroidEntry.openCapturedIntent(context, capturedId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_input_add)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(notifId, notification) }
    }

    /** Dismiss a transaction's notification once its expense has been saved. */
    fun cancel(capturedId: Long) {
        runCatching { NotificationManagerCompat.from(AppContext.context).cancel(notifId(capturedId)) }
    }

    // SQLite rowids are positive Longs; mask to 31 bits so the Int notification ID is always positive
    // and never overflows, even after billions of cumulative inserts.
    private fun notifId(capturedId: Long): Int = (capturedId % Int.MAX_VALUE).toInt()
}
