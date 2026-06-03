@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.expensetracker.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.expensetracker.data.CapturedNotificationItem
import app.expensetracker.db.Captured_notification as DbCaptured
import app.expensetracker.db.Database
import app.expensetracker.db.ExpenseTrackerDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

class CapturedNotificationRepository(
    private val db: ExpenseTrackerDatabase = Database.instance,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val queries get() = db.capturedNotificationQueries

    fun observeAll(): Flow<List<CapturedNotificationItem>> =
        queries.selectAll().asFlow().mapToList(dispatcher).map { rows -> rows.map { it.toItem() } }

    fun observeUnreadCount(): Flow<Long> =
        queries.unreadCount().asFlow().mapToOne(dispatcher)

    fun getById(id: Long): CapturedNotificationItem? = queries.selectById(id).executeAsOneOrNull()?.toItem()

    /** Record a detected notification unless an identical one (same slot + amount) already exists.
     *  Returns the row id to notify on, or null if it was a duplicate. */
    fun recordIfNew(
        notifKey: String?,
        packageName: String,
        appLabel: String,
        title: String,
        text: String,
        amountMinor: Long?,
        merchant: String?,
        postedAt: Instant,
    ): Long? {
        return db.transactionWithResult {
            if (notifKey != null && queries.findDuplicate(notifKey, amountMinor).executeAsOneOrNull() != null) {
                null
            } else {
                queries.insert(notifKey, packageName, appLabel, title, text, amountMinor, merchant, postedAt.toEpochMilliseconds())
                queries.lastInsertedId().executeAsOne()
            }
        }
    }

    fun markRead(id: Long) = queries.markRead(id)
    fun markAllRead() = queries.markAllRead()
    fun linkExpense(id: Long, expenseId: Long) = queries.setExpense(expenseId, id)

    /** Detach any captured notification pointing at [expenseId] (e.g. when that expense is deleted). */
    fun clearExpenseLink(expenseId: Long) = queries.clearExpenseLink(expenseId)

    /** Drop already-read notifications posted before [cutoff] so the table can't grow unbounded. */
    fun pruneReadBefore(cutoff: Instant) = queries.deleteReadBefore(cutoff.toEpochMilliseconds())
}

private fun DbCaptured.toItem() = CapturedNotificationItem(
    id = id,
    notifKey = notifKey,
    packageName = packageName,
    appLabel = appLabel,
    title = title,
    text = text,
    amountMinor = amountMinor,
    merchant = merchant,
    postedAt = Instant.fromEpochMilliseconds(postedAt),
    isRead = isRead == 1L,
    expenseId = expenseId,
)
