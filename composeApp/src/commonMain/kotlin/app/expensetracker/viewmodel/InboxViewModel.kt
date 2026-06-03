package app.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.expensetracker.ServiceLocator
import app.expensetracker.data.CapturedNotificationItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class InboxViewModel : ViewModel() {
    private val repo = ServiceLocator.capturedNotificationRepository

    val items: StateFlow<List<CapturedNotificationItem>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Long> =
        repo.observeUnreadCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun markAllRead() { launchIo { repo.markAllRead() } }
}
