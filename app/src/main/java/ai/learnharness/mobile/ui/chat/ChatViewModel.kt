package ai.learnharness.mobile.ui.chat

import ai.learnharness.mobile.ServiceLocator
import ai.learnharness.mobile.data.api.AbortBody
import ai.learnharness.mobile.data.api.ChatInputBody
import ai.learnharness.mobile.data.api.LhClient
import ai.learnharness.mobile.data.api.SteerBody
import ai.learnharness.mobile.data.api.SteerInput
import ai.learnharness.mobile.data.db.MessageEntity
import ai.learnharness.mobile.data.db.SessionEntity
import ai.learnharness.mobile.data.sse.LiveTurn
import ai.learnharness.mobile.data.sse.TurnEvent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * One chat screen: cached transcript from Room + the LIVE turn from the
 * wire. Send → POST /api/chat → attach the turn stream; steer when busy;
 * abort with confirm; 409 (turn already running) waits for the bus's
 * turn-settled then retries the same POST (the write-ahead is idempotent).
 */
class ChatViewModel(handle: SavedStateHandle) : ViewModel() {

	private val repo = ServiceLocator.repository
	private val events = ServiceLocator.events
	private val turnStream = ServiceLocator.turnStream
	val chatId: String = handle["chatId"] ?: ""

	val session: StateFlow<SessionEntity?> =
		repo.session(chatId).stateIn(viewModelScope, SharingStarted.Eagerly, null)

	val messages: StateFlow<List<MessageEntity>> =
		repo.messages(chatId).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

	private val _loading = MutableStateFlow(false)
	val loading: StateFlow<Boolean> = _loading.asStateFlow()

	private val _error = MutableStateFlow<String?>(null)
	val error: StateFlow<String?> = _error.asStateFlow()

	/** Missing model slots from 503 models_not_configured. */
	private val _missingSlots = MutableStateFlow<List<String>>(emptyList())
	val missingSlots: StateFlow<List<String>> = _missingSlots.asStateFlow()

	/** True while a turn is in flight (locally started or observed). */
	private val _busy = MutableStateFlow(false)
	val busy: StateFlow<Boolean> = _busy.asStateFlow()

	/** The growing live turn (full-snapshot rows replace each frame). */
	private val _live = MutableStateFlow<LiveTurn?>(null)
	val live: StateFlow<LiveTurn?> = _live.asStateFlow()

	private val _lastSteer = MutableStateFlow<String?>(null)
	val lastSteer: StateFlow<String?> = _lastSteer.asStateFlow()

	private var streamJob: Job? = null

	init {
		open()
		observeBus()
	}

	fun open() {
		viewModelScope.launch {
			_loading.value = true
			_error.value = null
			try {
				repo.openChat(chatId)
				if (session.value?.busy == true) attachTurn()
			} catch (e: Exception) {
				_error.value = e.message ?: e.javaClass.simpleName
			} finally {
				_loading.value = false
			}
		}
	}

	/** notify→fetch: bus facts about THIS chat reload the record. */
	private fun observeBus() {
		viewModelScope.launch {
			events.events.collect { ev ->
				if (ev.chatId != null && ev.chatId != chatId) return@collect
				when (ev.type) {
					"turn-settled" -> {
						_busy.value = false
						_live.value = null
						runCatching { repo.openChat(chatId) }
					}

					"turn-started" -> {
						_busy.value = true
						attachTurn()
					}

					"record-changed", "compaction-changed" -> runCatching { repo.openChat(chatId) }
				}
			}
		}
	}

	/** Enter: send text, or steer when a turn is running. */
	fun sendOrSteer(text: String) {
		val t = text.trim()
		if (t.isEmpty()) return
		if (_busy.value) steer(t) else send(t, attempt = 0)
	}

	private fun send(text: String, attempt: Int) {
		viewModelScope.launch {
			_error.value = null
			_missingSlots.value = emptyList()
			try {
				repo.ensureConfigured()
				val response = LhClient.api.sendInput(
					ChatInputBody(
						chatId = chatId,
						input = buildJsonObject {
							put("type", "text")
							put("text", text)
						},
					),
				)
				when {
					response.isSuccessful -> attachTurn()

					response.code() == 409 && attempt < 1 -> {
						// a turn is already running (maybe from another device):
						// the same POST is idempotent — wait for settle, retry once
						withTimeout(120_000L) {
							events.events.first {
								it.type == "turn-settled" && (it.chatId == null || it.chatId == chatId)
							}
						}
						send(text, attempt = attempt + 1)
					}

					response.code() == 503 -> {
						val info = parseRefusal(response)
						when (info.code) {
							"provider_key_missing" -> {
								_error.value = "Нет ключа провайдера ${info.provider} — добавь его: Настройки → Ключи API"
								_missingSlots.value = emptyList()
							}

							else -> {
								_missingSlots.value = info.missing
								_error.value = "Модели не настроены: ${info.missing.joinToString(", ")}"
							}
						}
					}

					else -> _error.value = "HTTP ${response.code()}: ${response.message()}"
				}
			} catch (e: Exception) {
				_error.value = e.message ?: e.javaClass.simpleName
			}
		}
	}

	/** Steer mid-turn; the server 409s when no turn is live — fall back to send. */
	private fun steer(text: String) {
		viewModelScope.launch {
			_lastSteer.value = text
			try {
				repo.ensureConfigured()
				val response = LhClient.api.steer(
					chatId,
					SteerBody(SteerInput(id = "steer_${System.currentTimeMillis()}", text = text)),
				)
				if (response.code() == 409) send(text, attempt = 1) // no live turn anymore
			} catch (e: Exception) {
				_error.value = e.message ?: e.javaClass.simpleName
			}
		}
	}

	/** Stop: confirm dialog already shown; abort through the activity API.
	 *  The bus delivers turn-settled → the record reloads with the partial. */
	fun abort() {
		viewModelScope.launch {
			runCatching {
				repo.ensureConfigured()
				LhClient.api.abort(AbortBody(kind = "turn", id = chatId))
			}
		}
	}

	/** Attach the SSE turn wire: replay + live frames mutate the live state. */
	private fun attachTurn() {
		if (streamJob?.isActive == true) return
		_busy.value = true
		streamJob = viewModelScope.launch {
			runCatching { repo.ensureConfigured() }
			turnStream.open(
				chatId,
				onEvent = { ev ->
					when (ev) {
						is TurnEvent.Snapshot -> _live.value = ev.turn
						is TurnEvent.Failure -> _error.value = ev.errorText
						TurnEvent.Settled -> viewModelScope.launch {
							_busy.value = false
							_live.value = null
							runCatching { repo.openChat(chatId) }
						}
					}
				},
			)
		}
	}

	/** 503 envelope: {error, code: models_not_configured|provider_key_missing,
	 *  missing?: slots[], provider?} — src/core/settings.ts. */
	private fun parseRefusal(response: retrofit2.Response<*>): Refusal = runCatching {
		val body = response.errorBody()?.string() ?: return Refusal(emptyList(), null, null)
		val obj = LhClient.json().parseToJsonElement(body).jsonObject
		Refusal(
			code = obj["code"]?.jsonPrimitive?.contentOrNull,
			provider = obj["provider"]?.jsonPrimitive?.contentOrNull,
			missing = obj["missing"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
		)
	}.getOrDefault(Refusal(emptyList(), null, null))

	private data class Refusal(
		val missing: List<String>,
		val code: String?,
		val provider: String?,
	)

	override fun onCleared() {
		streamJob?.cancel()
	}
}
