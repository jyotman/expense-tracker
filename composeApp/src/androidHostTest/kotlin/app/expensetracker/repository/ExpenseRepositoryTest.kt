@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.expensetracker.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import app.expensetracker.data.ExpenseSource
import app.expensetracker.db.ExpenseTrackerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ExpenseRepositoryTest {

    private fun createRepo(): ExpenseRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ExpenseTrackerDatabase.Schema.create(driver)
        return ExpenseRepository(ExpenseTrackerDatabase(driver), Dispatchers.Unconfined)
    }

    private fun ExpenseRepository.addOne(amount: Long = 1000L): Long = add(
        amountMinor = amount,
        categoryId = null,
        note = null,
        merchant = null,
        occurredAt = Instant.fromEpochMilliseconds(0L),
        createdAt = Instant.fromEpochMilliseconds(0L),
        source = ExpenseSource.MANUAL,
    )

    @Test
    fun count_returns_zero_for_empty_db() {
        val repo = createRepo()
        assertEquals(0L, repo.count())
    }

    @Test
    fun count_increments_on_insert_and_decrements_on_delete() {
        val repo = createRepo()
        assertEquals(0L, repo.count())
        val id = repo.addOne()
        assertEquals(1L, repo.count())
        repo.delete(id)
        assertEquals(0L, repo.count())
    }

    @Test
    fun observe_count_emits_live_updates() = runTest {
        val repo = createRepo()
        repo.observeCount().test {
            assertEquals(0L, awaitItem())           // initial emission

            val id = repo.addOne()
            assertEquals(1L, awaitItem())           // after insert

            repo.delete(id)
            assertEquals(0L, awaitItem())           // after delete — previously stale if loaded once

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observe_count_reflects_multiple_expenses() = runTest {
        val repo = createRepo()
        repo.observeCount().test {
            assertEquals(0L, awaitItem())

            repo.addOne(500L)
            assertEquals(1L, awaitItem())

            repo.addOne(750L)
            assertEquals(2L, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
