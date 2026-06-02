package app.spent.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Holds the single SQLDelight database instance. Platform code supplies a driver factory
 * during app startup (Android: AndroidSqliteDriver, iOS: NativeSqliteDriver).
 */
object Database {
    private var driverFactory: (() -> SqlDriver)? = null

    fun init(factory: () -> SqlDriver) {
        driverFactory = factory
    }

    val instance: SpentDatabase by lazy {
        val factory = driverFactory ?: error("Database not initialized. Call Database.init { ... } at startup.")
        SpentDatabase(factory())
    }
}
