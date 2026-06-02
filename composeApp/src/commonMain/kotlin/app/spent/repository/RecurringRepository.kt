@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.spent.repository

import app.spent.data.ExpenseSource
import app.spent.data.IntervalUnit
import app.spent.data.RecurringRuleItem
import app.spent.db.Database
import app.spent.db.SpentDatabase
import app.spent.db.Recurring_rule as DbRule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Clock
import kotlin.time.Instant

class RecurringRepository(
    private val db: SpentDatabase = Database.instance,
    private val expenses: ExpenseRepository = ExpenseRepository(db),
    private val tz: TimeZone = TimeZone.currentSystemDefault(),
) {
    private val queries get() = db.recurringRuleQueries

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
        queries.insert(
            amountMinor, categoryId, note, merchant, unit.name, count,
            startAt.toEpochMilliseconds(), endAt?.toEpochMilliseconds(), null,
        )
        return queries.lastInsertedId().executeAsOne()
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
        var first = advance(anchor, unit, count)
        while (first <= now) first = advance(first, unit, count)
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
     * Materialize any occurrences that have come due since the rule was last generated,
     * up to [now]. Idempotent: tracks progress via lastGeneratedAt. Call on app start
     * and after creating/editing a rule.
     */
    fun materializeDue(now: Instant = Clock.System.now()) {
        listActive().forEach { rule ->
            var occurrence = nextOccurrenceToGenerate(rule)
            while (occurrence <= now && (rule.endAt == null || occurrence <= rule.endAt)) {
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
                occurrence = advance(occurrence, rule.intervalUnit, rule.intervalCount)
            }
        }
    }

    private fun nextOccurrenceToGenerate(rule: RecurringRuleItem): Instant {
        val last = rule.lastGeneratedAt ?: return rule.startAt
        return advance(last, rule.intervalUnit, rule.intervalCount)
    }

    private fun advance(from: Instant, unit: IntervalUnit, count: Long): Instant {
        val date = from.toLocalDateTime(tz).date
        val advanced = when (unit) {
            IntervalUnit.DAY -> date.plus(count.toInt(), DateTimeUnit.DAY)
            IntervalUnit.WEEK -> date.plus(count.toInt(), DateTimeUnit.WEEK)
            IntervalUnit.MONTH -> date.plus(count.toInt(), DateTimeUnit.MONTH)
        }
        return advanced.atStartOfDayIn(tz)
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
