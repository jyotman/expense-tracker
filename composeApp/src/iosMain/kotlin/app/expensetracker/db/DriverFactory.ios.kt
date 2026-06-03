package app.expensetracker.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

fun iosSqlDriver(): SqlDriver =
    NativeSqliteDriver(
        schema = ExpenseTrackerDatabase.Schema,
        name = "expensetracker.db",
    )
