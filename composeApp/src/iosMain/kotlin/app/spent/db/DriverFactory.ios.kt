package app.spent.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

fun iosSqlDriver(): SqlDriver =
    NativeSqliteDriver(
        schema = SpentDatabase.Schema,
        name = "spent.db",
    )
