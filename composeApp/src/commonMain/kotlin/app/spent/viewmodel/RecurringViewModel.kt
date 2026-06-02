@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.spent.viewmodel

import androidx.lifecycle.ViewModel
import app.spent.ServiceLocator
import app.spent.data.CategoryItem
import app.spent.data.RecurringRuleItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RecurringRow(val rule: RecurringRuleItem, val category: CategoryItem?)

class RecurringViewModel : ViewModel() {
    private val recurring = ServiceLocator.recurringRepository
    private val categories = ServiceLocator.categoryRepository

    private val _rows = MutableStateFlow(load())
    val rows: StateFlow<List<RecurringRow>> = _rows.asStateFlow()

    private fun load(): List<RecurringRow> {
        val byId = categories.listActive().associateBy { it.id }
        return recurring.listAll().map { RecurringRow(it, it.categoryId?.let(byId::get)) }
    }

    fun refresh() { _rows.value = load() }

    fun toggleActive(id: Long, active: Boolean) {
        recurring.setActive(id, active)
        if (active) recurring.materializeDue()
        refresh()
    }

    fun delete(id: Long) {
        recurring.delete(id)
        refresh()
    }
}
