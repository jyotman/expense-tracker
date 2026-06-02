package app.spent.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

fun androidSqlDriver(context: Context): SqlDriver =
    AndroidSqliteDriver(
        schema = SpentDatabase.Schema,
        context = context.applicationContext,
        name = "spent.db",
    )
