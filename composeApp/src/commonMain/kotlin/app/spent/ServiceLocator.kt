package app.spent

import app.spent.repository.CapturedNotificationRepository
import app.spent.repository.CategoryRepository
import app.spent.repository.ExpenseRepository
import app.spent.repository.RecurringRepository
import app.spent.storage.SettingsStorage

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
    val settings: SettingsStorage get() = SettingsStorage.instance

    /** One-time-per-launch work: seed categories, roll forward recurring expenses. */
    fun onStart() {
        categoryRepository.ensureSeeded()
        recurringRepository.materializeDue()
    }
}
