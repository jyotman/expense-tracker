@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.spent.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.spent.data.ExpenseItem
import app.spent.data.ExpenseSource
import app.spent.db.Database
import app.spent.db.SpentDatabase
import app.spent.db.Expense as DbExpense
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/** A category id with its aggregated spend over a period. */
data class CategoryTotal(val categoryId: Long?, val totalMinor: Long, val count: Long)

class ExpenseRepository(
    private val db: SpentDatabase = Database.instance,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val queries get() = db.expenseQueries

    fun observeInRange(start: Instant, end: Instant): Flow<List<ExpenseItem>> =
        queries.selectInRange(start.toEpochMilliseconds(), end.toEpochMilliseconds())
            .asFlow().mapToList(dispatcher).map { rows -> rows.map { it.toItem() } }

    fun observeTotalInRange(start: Instant, end: Instant): Flow<Long> =
        queries.totalInRange(start.toEpochMilliseconds(), end.toEpochMilliseconds())
            .asFlow().mapToOne(dispatcher)

    fun observeCategoryTotals(start: Instant, end: Instant): Flow<List<CategoryTotal>> =
        queries.categoryTotalsInRange(start.toEpochMilliseconds(), end.toEpochMilliseconds())
            .asFlow().mapToList(dispatcher)
            .map { rows -> rows.map { CategoryTotal(it.categoryId, it.total, it.cnt) } }

    fun totalInRange(start: Instant, end: Instant): Long =
        queries.totalInRange(start.toEpochMilliseconds(), end.toEpochMilliseconds())
            .executeAsOne()

    fun getById(id: Long): ExpenseItem? = queries.selectById(id).executeAsOneOrNull()?.toItem()

    fun add(
        amountMinor: Long,
        categoryId: Long?,
        note: String?,
        merchant: String?,
        occurredAt: Instant,
        createdAt: Instant,
        source: ExpenseSource,
        recurringRuleId: Long? = null,
    ): Long {
        // insert + last_insert_rowid must be atomic on one connection, else a concurrent writer
        // (e.g. the notification listener) can make lastInsertedId return the wrong row.
        return db.transactionWithResult {
            queries.insert(
                amountMinor,
                categoryId,
                note,
                merchant,
                occurredAt.toEpochMilliseconds(),
                createdAt.toEpochMilliseconds(),
                source.name,
                recurringRuleId,
            )
            queries.lastInsertedId().executeAsOne()
        }
    }

    fun update(
        id: Long,
        amountMinor: Long,
        categoryId: Long?,
        note: String?,
        merchant: String?,
        occurredAt: Instant,
        source: ExpenseSource,
    ) {
        queries.update(amountMinor, categoryId, note, merchant, occurredAt.toEpochMilliseconds(), source.name, id)
    }

    fun setCategory(id: Long, categoryId: Long?) = queries.setCategory(categoryId, id)

    fun delete(id: Long) = queries.deleteById(id)

    fun allForExport(): List<ExpenseItem> = queries.selectAllForExport().executeAsList().map { it.toItem() }

    fun deleteAll() = queries.deleteAll()
}

private fun DbExpense.toItem() = ExpenseItem(
    id = id,
    amountMinor = amountMinor,
    categoryId = categoryId,
    note = note,
    merchant = merchant,
    occurredAt = Instant.fromEpochMilliseconds(occurredAt),
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    source = ExpenseSource.fromDb(source),
    recurringRuleId = recurringRuleId,
)
