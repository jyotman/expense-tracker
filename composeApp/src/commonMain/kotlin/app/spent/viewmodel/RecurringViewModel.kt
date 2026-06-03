@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.spent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.spent.ServiceLocator
import app.spent.data.CategoryItem
import app.spent.data.RecurringRuleItem
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
