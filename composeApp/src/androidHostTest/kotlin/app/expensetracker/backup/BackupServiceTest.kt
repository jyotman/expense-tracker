package app.expensetracker.backup

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.expensetracker.db.ExpenseTrackerDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class BackupServiceTest {

    private fun createDb(): ExpenseTrackerDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ExpenseTrackerDatabase.Schema.create(driver)
        return ExpenseTrackerDatabase(driver)
    }

    @Test
    fun round_trip_preserves_expenses_categories_and_recurring_rules() {
        val db = createDb()
        db.categoryQueries.insertWithId(1L, "Food", "restaurant", "#FF5722", 0L, 0L)
        db.expenseQueries.insertWithId(1L, 1500L, 1L, "lunch", "Cafe", 1000L, 1000L, "MANUAL", null, null, null)
        db.recurringRuleQueries.insertWithId(1L, 2000L, 1L, "rent", null, "MONTH", 1L, 0L, null, null, 1L)

        val json = BackupService.export(nowMillis = 0L, db = db)

        val restored = createDb()
        BackupService.restore(json, restored)

        val categories = restored.categoryQueries.selectAll().executeAsList()
        assertEquals(1, categories.size)
        assertEquals("Food", categories[0].name)

        val expenses = restored.expenseQueries.selectAllForExport().executeAsList()
        assertEquals(1, expenses.size)
        assertEquals(1500L, expenses[0].amountMinor)
        assertEquals("lunch", expenses[0].note)

        val rules = restored.recurringRuleQueries.selectAll().executeAsList()
        assertEquals(1, rules.size)
        assertEquals(2000L, rules[0].amountMinor)
        assertEquals("MONTH", rules[0].intervalUnit)
    }

    @Test
    fun restore_wipes_all_pre_existing_data() {
        val db = createDb()
        db.categoryQueries.insertWithId(1L, "Old", "tag", "#000000", 0L, 0L)
        db.expenseQueries.insertWithId(1L, 500L, 1L, null, null, 0L, 0L, "MANUAL", null, null, null)

        val sourceDb = createDb()
        sourceDb.categoryQueries.insertWithId(2L, "New", "star", "#FFFFFF", 0L, 0L)
        val json = BackupService.export(nowMillis = 0L, db = sourceDb)

        BackupService.restore(json, db)

        val categories = db.categoryQueries.selectAll().executeAsList()
        assertEquals(1, categories.size)
        assertEquals("New", categories[0].name)
        assertEquals(0, db.expenseQueries.selectAllForExport().executeAsList().size)
    }

    @Test
    fun restore_with_invalid_json_throws() {
        assertFails { BackupService.restore("not valid json", createDb()) }
    }

    @Test
    fun export_empty_database_produces_empty_lists() {
        val json = BackupService.export(nowMillis = 0L, db = createDb())
        assertTrue(json.contains("\"expenses\":[]"))
        assertTrue(json.contains("\"categories\":[]"))
        assertTrue(json.contains("\"recurring\":[]"))
    }
}
