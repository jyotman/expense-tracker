@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.expensetracker.ServiceLocator
import app.expensetracker.data.CategoryItem
import app.expensetracker.data.RecurringRuleItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RecurringRow(val rule: RecurringRuleItem, val category: CategoryItem?)

class RecurringViewModel : ViewModel() {
    private val recurring = ServiceLocator.recurringRepository
    private val categories = ServiceLocator.categoryRepository

    val rows: StateFlow<List<RecurringRow>> = combine(
        recurring.observeAll(),
        categories.observeAll(),
    ) { rules, cats ->
        val byId = cats.associateBy { it.id }
        rules.map { RecurringRow(it, it.categoryId?.let(byId::get)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleActive(id: Long, active: Boolean) {
        launchIo {
            recurring.setActive(id, active)
            if (active) recurring.materializeDue()
        }
    }

    fun delete(id: Long) { launchIo { recurring.delete(id) } }
}
