package ai.learnharness.mobile.data.sse

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import kotlin.coroutines.resume

/** One in-flight turn row: the growing transcript of the CURRENT turn. */
data class LiveRow(val role: String, val text: String, val isThinking: Boolean = false)

data class LiveTurn(
	val rows: List<LiveRow>,
	val toolName: String?,
)

sealed class TurnEvent {
	data class Snapshot(val turn: LiveTurn) : TurnEvent()
	data class Failure(val errorText: String) : TurnEvent()
	data object Settled : TurnEvent()
}

/**
 * The turn wire: GET /api/chats/[id]/stream — server replays the compact
 * log, then live frames until `turn-settled` + [DONE]. message frames carry
 * FULL message snapshots (a view REPLACES, never assembles deltas).
 */
class TurnStream(
	private val clientProvider: () -> OkHttpClient,
	private val baseUrlProvider: () -> String?,
) {
	private val json = Json { ignoreUnknownKeys = true }

	fun open(chatId: String, onEvent: (TurnEvent) -> Unit): EventSource {
		val base = baseUrlProvider() ?: throw IllegalStateException("not configured")
		val request = Request.Builder().url("$base/api/chats/$chatId/stream").build()

		// Replay + live both ride through here; rows are keyed by message id
		// (the growing assistant message has none until persist — it always
		// replaces the trailing id-less assistant row).
		val rows = LinkedHashMap<String, LiveRow>()
		var toolName: String? = null

		fun emit() {
			onEvent(TurnEvent.Snapshot(LiveTurn(rows.values.toList(), toolName)))
		}

		return EventSources.createFactory(clientProvider()).newEventSource(
			request,
			object : EventSourceListener() {
				override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
					if (data == "[DONE]") {
						onEvent(TurnEvent.Settled)
						eventSource.cancel()
						return
					}
					val frame: JsonObject = runCatching {
						json.parseToJsonElement(data).jsonObject
					}.getOrNull() ?: return
					when (frame.str("type")) {
						"message_start", "message_update", "message_end" -> {
							val msg = frame["message"] as? JsonObject ?: return
							val role = msg.str("role") ?: return
							val text = msg.blocks("text")
							val thinking = msg.blocks("thinking")
							val key = msg.str("id") ?: "__live_$role"
							// an id-less assistant row is the growing one: replace, never append
							val replaced = rows.containsKey(key)
							rows[key] = LiveRow(
								role = role,
								text = text,
								isThinking = false,
							)
							if (thinking.isNotBlank()) rows["$key#th"] = LiveRow(role, thinking, isThinking = true)
							if (!replaced && role == "assistant") {
								// drop any previous id-less assistant row superseded by this id
								rows.entries.removeAll { it.key.startsWith("__live_assistant") && it.key != key }
							}
							emit()
						}

						"tool_execution_start" -> {
							toolName = frame.str("toolName") ?: frame.str("tool") ?: "tool"
							emit()
						}

						"tool_execution_end" -> {
							toolName = null
							emit()
						}

						"error" -> onEvent(TurnEvent.Failure(frame.str("errorText") ?: "Ошибка хода"))

						"turn-settled" -> onEvent(TurnEvent.Settled)
					}
				}

				override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
					// stream dropped mid-turn: the bus's turn-settled brings the truth
					onEvent(TurnEvent.Settled)
				}
			},
		)
	}

	private fun JsonObject.str(key: String): String? =
		(this[key] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull

	private fun JsonObject.blocks(type: String): String =
		(this["content"] as? kotlinx.serialization.json.JsonArray)
			?.mapNotNull { b -> b as? JsonObject }
			?.filter { it.str("type") == type }
			?.joinToString("") { it.str(type) ?: "" }
			?.trim()
			.orEmpty()
}
