package ai.learnharness.mobile.ui.sessions

import ai.learnharness.mobile.ServiceLocator
import ai.learnharness.mobile.data.db.SessionEntity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionsViewModel : ViewModel() {

	private val repo = ServiceLocator.repository

	val sessions: StateFlow<List<SessionEntity>> =
		repo.sessions().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

	val filter = MutableStateFlow("all") // all | teach | practice

	private val _refreshing = MutableStateFlow(false)
	val refreshing: StateFlow<Boolean> = _refreshing

	private val _error = MutableStateFlow<String?>(null)
	val error: StateFlow<String?> = _error

	init {
		refresh()
	}

	fun setFilter(f: String) {
		filter.value = f
	}

	fun visibleSessions(): List<SessionEntity> {
		val f = filter.value
		return sessions.value.filter { f == "all" || it.kind == f }
	}

	fun refresh() {
		viewModelScope.launch {
			_refreshing.value = true
			_error.value = null
			try {
				repo.refreshSessions()
			} catch (e: Exception) {
				_error.value = e.message ?: e.javaClass.simpleName
			} finally {
				_refreshing.value = false
			}
		}
	}
}
