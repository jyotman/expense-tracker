package app.expensetracker.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

fun androidSqlDriver(context: Context): SqlDriver =
    AndroidSqliteDriver(
        schema = ExpenseTrackerDatabase.Schema,
        context = context.applicationContext,
        name = "expensetracker.db",
    )
