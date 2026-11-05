package ai.learnharness.mobile.ui.chat

import ai.learnharness.mobile.ServiceLocator
import ai.learnharness.mobile.data.db.MessageEntity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(handle: SavedStateHandle) : ViewModel() {

	private val repo = ServiceLocator.repository
	val chatId: String = handle["chatId"] ?: ""

	val messages: StateFlow<List<MessageEntity>> =
		repo.messages(chatId).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

	private val _loading = MutableStateFlow(false)
	val loading: StateFlow<Boolean> = _loading.asStateFlow()

	private val _error = MutableStateFlow<String?>(null)
	val error: StateFlow<String?> = _error.asStateFlow()

	init {
		open()
	}

	fun open() {
		viewModelScope.launch {
			_loading.value = true
			_error.value = null
			try {
				repo.openChat(chatId)
			} catch (e: Exception) {
				_error.value = e.message ?: e.javaClass.simpleName
			} finally {
				_loading.value = false
			}
		}
	}
}
