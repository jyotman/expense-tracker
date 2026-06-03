package app.expensetracker.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Where the app should jump when launched from outside (widget quick-add, a capture push). */
sealed interface DeepLink {
    /** Open a blank expense form (widget "+" button). */
    data object AddExpense : DeepLink

    /** Open the form for a captured notification: prefilled if new, or its existing expense. */
    data class OpenCaptured(val capturedId: Long) : DeepLink

    /** Open the in-app inbox of captured notifications. */
    data object OpenInbox : DeepLink
}

/**
 * A tiny event bus so platform entry points (Android Activity handling an Intent) can ask
 * the Compose app to navigate. The app collects [events] once it is composed.
 */
object DeepLinks {
    private val _events = MutableSharedFlow<DeepLink>(replay = 1, extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    fun emit(link: DeepLink) {
        _events.tryEmit(link)
    }

    fun clear() {
        _events.resetReplayCache()
    }
}
