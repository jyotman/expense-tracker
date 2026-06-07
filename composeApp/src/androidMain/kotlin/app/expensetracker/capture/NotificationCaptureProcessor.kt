@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.expensetracker.capture

import app.expensetracker.ServiceLocator
import co.touchlab.kermit.Logger
import kotlin.time.Instant

/**
 * Background handling of a finance-app notification. Regex only — it never creates an expense and
 * never guesses a category. It records an inbox item and posts a per-transaction push; the user
 * reviews a prefilled form (where AI may assist) and saves. On-device AI is *not* used here:
 * AICore blocks GenAI inference outside the foreground.
 */
object NotificationCaptureProcessor {
    private val log = Logger.withTag("Capture")

    suspend fun process(
        notifKey: String?,
        packageName: String,
        appLabel: String,
        title: String,
        text: String,
        postedAtMillis: Long,
    ) {
        val settings = ServiceLocator.settings
        if (!settings.notificationCaptureEnabled) return

        val allow = if (!settings.capturePackagesConfigured) CaptureRules.defaultPackages
                    else settings.capturePackages
        if (packageName !in allow) return

        val combined = listOf(title, text).filter { it.isNotBlank() }.joinToString(" — ")
        if (!TransactionDetector.isLikelySpend(combined)) return
        val amount = TransactionDetector.extractAmountMinor(combined) ?: return
        val merchant = TransactionDetector.guessMerchant(combined)
        val currencyToken = TransactionDetector.extractCurrencyToken(combined)

        val id = ServiceLocator.capturedNotificationRepository.recordIfNew(
            notifKey = notifKey,
            packageName = packageName,
            appLabel = appLabel,
            title = title,
            text = text,
            amountMinor = amount,
            merchant = merchant,
            postedAt = Instant.fromEpochMilliseconds(postedAtMillis),
            detectedCurrencyToken = currencyToken,
        ) ?: return // duplicate of an already-recorded notification

        log.i { "Detected expense candidate #$id from $packageName" }
        CaptureNotifications.showDetected(id, amount, currencyToken, merchant)
    }
}
