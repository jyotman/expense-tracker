@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.expensetracker.data

import kotlin.time.Instant

/** How an expense was created. */
enum class ExpenseSource {
    MANUAL,
    AUTO, // created from a parsed notification
    WIDGET;

    companion object {
        fun fromDb(value: String): ExpenseSource =
            entries.firstOrNull { it.name == value } ?: MANUAL
    }
}

/** Recurrence cadence for a recurring expense. */
enum class IntervalUnit {
    DAY,
    WEEK,
    MONTH;

    companion object {
        fun fromDb(value: String): IntervalUnit =
            entries.firstOrNull { it.name == value } ?: MONTH
    }
}

data class CategoryItem(
    val id: Long,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val sortOrder: Long = 0,
    val isArchived: Boolean = false,
)

data class ExpenseItem(
    val id: Long,
    val amountMinor: Long,
    val categoryId: Long?,
    val note: String?,
    val merchant: String?,
    val occurredAt: Instant,
    val createdAt: Instant,
    val source: ExpenseSource,
    val recurringRuleId: Long?,
    /** Original notification text (title + body) for AUTO expenses; null for manual/widget. */
    val sourceNotificationText: String? = null,
)

data class RecurringRuleItem(
    val id: Long,
    val amountMinor: Long,
    val categoryId: Long?,
    val note: String?,
    val merchant: String?,
    val intervalUnit: IntervalUnit,
    val intervalCount: Long,
    val startAt: Instant,
    val endAt: Instant?,
    val lastGeneratedAt: Instant?,
    val isActive: Boolean,
)

/** A category with its aggregated spend for a period (used by Summary / Reports). */
data class CategorySpend(
    val category: CategoryItem?, // null = uncategorized
    val totalMinor: Long,
    val count: Long,
)

/**
 * A payment notification detected in the background (the in-app "inbox"). We never auto-create an
 * expense from it; the user reviews a prefilled form and saves. [expenseId] links the resulting
 * expense so re-opening the item edits it instead of duplicating.
 */
data class CapturedNotificationItem(
    val id: Long,
    val notifKey: String?,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val amountMinor: Long?,
    val merchant: String?,
    val postedAt: Instant,
    val isRead: Boolean,
    val expenseId: Long?,
)
