package app.spent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.spent.ServiceLocator
import app.spent.data.CategoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CategoriesViewModel : ViewModel() {
    private val repo = ServiceLocator.categoryRepository

    val categories: StateFlow<List<CategoryItem>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getById(id: Long): CategoryItem? = repo.getById(id)

    fun add(name: String, iconKey: String, colorHex: String) {
        if (name.isBlank()) return
        repo.add(name.trim(), iconKey, colorHex)
    }

    fun update(item: CategoryItem) {
        if (item.name.isBlank()) return
        repo.update(item.copy(name = item.name.trim()))
    }

    fun setArchived(id: Long, archived: Boolean) = repo.setArchived(id, archived)

    fun delete(id: Long) = repo.delete(id)
}
