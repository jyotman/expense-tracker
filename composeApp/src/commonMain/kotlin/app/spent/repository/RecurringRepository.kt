@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.spent.repository

import app.spent.data.ExpenseSource
import app.spent.data.IntervalUnit
import app.spent.data.RecurringRuleItem
import app.spent.db.Database
import app.spent.db.SpentDatabase
import app.spent.db.Recurring_rule as DbRule
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

class RecurringRepository(
    private val db: SpentDatabase = Database.instance,
    private val expenses: ExpenseRepository = ExpenseRepository(db),
    private val tz: TimeZone = TimeZone.currentSystemDefault(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val queries get() = db.recurringRuleQueries

    fun observeAll(): Flow<List<RecurringRuleItem>> =
        queries.selectAll().asFlow().mapToList(dispatcher).map { rows -> rows.map { it.toItem() } }

    fun listActive(): List<RecurringRuleItem> = queries.selectActive().executeAsList().map { it.toItem() }

    fun listAll(): List<RecurringRuleItem> = queries.selectAll().executeAsList().map { it.toItem() }

    fun getById(id: Long): RecurringRuleItem? = queries.selectById(id).executeAsOneOrNull()?.toItem()

    fun add(
        amountMinor: Long,
        categoryId: Long?,
        note: String?,
        merchant: String?,
        unit: IntervalUnit,
        count: Long,
        startAt: Instant,
        endAt: Instant?,
    ): Long {
        return db.transactionWithResult {
            queries.insert(
                amountMinor, categoryId, note, merchant, unit.name, count,
                startAt.toEpochMilliseconds(), endAt?.toEpochMilliseconds(), null,
            )
            queries.lastInsertedId().executeAsOne()
        }
    }

    /**
     * Create a rule that continues an existing expense's cadence but only generates **future**
     * occurrences — the first occurrence is the next cadence date strictly after [now], anchored on
     * [anchor] (the edited expense's date). Used when turning a one-off expense into a repeating one:
     * the existing expense stays put, no past occurrences are back-filled, and no duplicate is created.
     */
    fun addGoingForward(
        amountMinor: Long,
        categoryId: Long?,
        note: String?,
        merchant: String?,
        unit: IntervalUnit,
        count: Long,
        anchor: Instant,
        now: Instant = Clock.System.now(),
    ): Long {
        // First occurrence strictly after [now], anchored on [anchor] so cadence + time-of-day hold.
        var index = 1
        var first = occurrenceAt(anchor, unit, count, index)
        while (first <= now) first = occurrenceAt(anchor, unit, count, ++index)
        return add(amountMinor, categoryId, note, merchant, unit, count, startAt = first, endAt = null)
    }

    fun update(item: RecurringRuleItem) {
        queries.update(
            item.amountMinor, item.categoryId, item.note, item.merchant,
            item.intervalUnit.name, item.intervalCount,
            item.startAt.toEpochMilliseconds(), item.endAt?.toEpochMilliseconds(), item.id,
        )
    }

    fun setActive(id: Long, active: Boolean) = queries.setActive(if (active) 1L else 0L, id)

    fun delete(id: Long) = queries.deleteById(id)

    /**
     * Materialize any occurrences that have come due since the rule was last generated, up to [now].
     * Idempotent: tracks progress via lastGeneratedAt. Occurrences are anchored on the rule's startAt
     * (not the previously-generated value), so a monthly rule on the 31st keeps hitting month-ends
     * instead of drifting to the 28th, and each occurrence keeps startAt's time-of-day. Caps the
     * number generated per call as a runaway guard (e.g. a rule restored with a startAt years back),
     * resuming on the next call. Call on app start and after creating/editing a rule.
     */
    fun materializeDue(now: Instant = Clock.System.now()) {
        listActive().forEach { rule ->
            db.transaction {
                val last = rule.lastGeneratedAt
                var index = 0
                var occurrence = rule.startAt
                var generated = 0
                while (occurrence <= now && (rule.endAt == null || occurrence <= rule.endAt)) {
                    if (last == null || occurrence > last) {
                        expenses.add(
                            amountMinor = rule.amountMinor,
                            categoryId = rule.categoryId,
                            note = rule.note,
                            merchant = rule.merchant,
                            occurredAt = occurrence,
                            createdAt = now,
                            source = ExpenseSource.MANUAL,
                            recurringRuleId = rule.id,
                        )
                        queries.setLastGenerated(occurrence.toEpochMilliseconds(), rule.id)
                        if (++generated >= MAX_OCCURRENCES_PER_CALL) {
                            Logger.withTag("Recurring").w {
                                "Rule ${rule.id} hit the $MAX_OCCURRENCES_PER_CALL/call cap; resuming next launch"
                            }
                            break
                        }
                    }
                    occurrence = occurrenceAt(rule.startAt, rule.intervalUnit, rule.intervalCount, ++index)
                }
            }
        }
    }

    /** The [index]-th occurrence after [start], advancing whole date units while preserving time-of-day. */
    private fun occurrenceAt(start: Instant, unit: IntervalUnit, count: Long, index: Int): Instant {
        if (index <= 0) return start
        val ldt = start.toLocalDateTime(tz)
        val steps = (count * index).toInt()
        val advancedDate = when (unit) {
            IntervalUnit.DAY -> ldt.date.plus(steps, DateTimeUnit.DAY)
            IntervalUnit.WEEK -> ldt.date.plus(steps, DateTimeUnit.WEEK)
            IntervalUnit.MONTH -> ldt.date.plus(steps, DateTimeUnit.MONTH)
        }
        return LocalDateTime(advancedDate, ldt.time).toInstant(tz)
    }

    private companion object {
        const val MAX_OCCURRENCES_PER_CALL = 500
    }
}

private fun DbRule.toItem() = RecurringRuleItem(
    id = id,
    amountMinor = amountMinor,
    categoryId = categoryId,
    note = note,
    merchant = merchant,
    intervalUnit = IntervalUnit.fromDb(intervalUnit),
    intervalCount = intervalCount,
    startAt = Instant.fromEpochMilliseconds(startAt),
    endAt = endAt?.let { Instant.fromEpochMilliseconds(it) },
    lastGeneratedAt = lastGeneratedAt?.let { Instant.fromEpochMilliseconds(it) },
    isActive = isActive == 1L,
)
