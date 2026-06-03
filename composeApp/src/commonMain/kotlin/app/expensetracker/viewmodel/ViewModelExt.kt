package app.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Run a database / IO mutation off the main thread, scoped to the ViewModel's lifecycle. Repository
 * writes are synchronous SQLDelight calls; UI callbacks invoke them on the main thread, so this keeps
 * a large write (e.g. mark-all-read, cascading delete) from janking or ANR-ing the UI.
 */
fun ViewModel.launchIo(block: suspend CoroutineScope.() -> Unit): Job =
    viewModelScope.launch(Dispatchers.Default, block = block)
