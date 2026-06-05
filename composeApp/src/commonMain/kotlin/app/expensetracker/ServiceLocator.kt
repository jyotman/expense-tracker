@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.expensetracker

import app.expensetracker.fx.FxRateService
import app.expensetracker.repository.CapturedNotificationRepository
import app.expensetracker.repository.CategoryRepository
import app.expensetracker.repository.ExpenseRepository
import app.expensetracker.repository.RecurringRepository
import app.expensetracker.storage.SettingsStorage
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Minimal manual DI. Repositories are stateless wrappers over the shared SQLDelight
 * database, so a lazy singleton each is sufficient. Platform startup must call
 * Database.init { } and SettingsStorage.init { } before any of these are touched.
 */
object ServiceLocator {
    val categoryRepository: CategoryRepository by lazy { CategoryRepository() }
    val expenseRepository: ExpenseRepository by lazy { ExpenseRepository() }
    val recurringRepository: RecurringRepository by lazy { RecurringRepository() }
    val capturedNotificationRepository: CapturedNotificationRepository by lazy { CapturedNotificationRepository() }
    val fxRateService: FxRateService by lazy { FxRateService() }
    val settings: SettingsStorage get() = SettingsStorage.instance

    /** One-time-per-launch work: seed categories, roll forward recurring expenses, prune old inbox. */
    fun onStart() {
        categoryRepository.ensureSeeded()
        recurringRepository.materializeDue()
        capturedNotificationRepository.pruneReadBefore(Clock.System.now().minus(90.days))
    }
}
