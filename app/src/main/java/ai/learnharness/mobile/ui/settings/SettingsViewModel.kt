package ai.learnharness.mobile.ui.settings

import ai.learnharness.mobile.ServiceLocator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

	private val repo = ServiceLocator.repository

	val serverUrl = MutableStateFlow("")
	val desktopSecret = MutableStateFlow("")
	private val _status = MutableStateFlow<String?>(null)
	val status: StateFlow<String?> = _status.asStateFlow()
	private val _saved = MutableStateFlow(false)
	val saved: StateFlow<Boolean> = _saved.asStateFlow()
	private val _checking = MutableStateFlow(false)
	val checking: StateFlow<Boolean> = _checking.asStateFlow()

	init {
		viewModelScope.launch {
			val c = repo.connection()
			serverUrl.value = c.serverUrl
			desktopSecret.value = c.desktopSecret
		}
	}

	fun save() {
		viewModelScope.launch {
			try {
				repo.saveConnection(serverUrl.value, desktopSecret.value)
				_status.value = null
				_saved.value = true
			} catch (e: Exception) {
				_saved.value = false
				_status.value = "Не сохранено: ${e.message}"
			}
		}
	}

	/** Quick reachability probe: a cheap authenticated GET against the server. */
	fun check() {
		viewModelScope.launch {
			_checking.value = true
			_status.value = null
			try {
				repo.saveConnection(serverUrl.value, desktopSecret.value)
				ServiceLocator.repository.refreshSessions()
				_status.value = "Соединение работает"
				_saved.value = true
			} catch (e: Exception) {
				_status.value = "Не отвечает: ${e.message}"
				_saved.value = false
			} finally {
				_checking.value = false
			}
		}
	}
}
