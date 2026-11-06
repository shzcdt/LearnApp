package ai.learnharness.mobile.data.sse

import kotlinx.coroutines.CoroutineScope
import androidx.annotation.CheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
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

/** One notification fact from GET /api/events: notify→fetch — the payload
 *  carries only WHAT changed; consumers refetch the truth over REST. */
data class ServerEvent(val type: String, val chatId: String?)

/**
 * Long-lived connection to the server's notification bus. Auto-reconnects
 * with exponential backoff (mobile networks drop idle connections); backoff
 * resets on a successful open.
 */
class EventsChannel(
	private val clientProvider: () -> OkHttpClient,
	private val baseUrlProvider: () -> String?,
) {
	private val json = Json { ignoreUnknownKeys = true }

	private val _events = MutableSharedFlow<ServerEvent>(extraBufferCapacity = 64)
	val events: SharedFlow<ServerEvent> = _events

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	@Volatile
	private var running = false

	@Volatile
	private var source: EventSource? = null

	@Volatile
	private var connectedOnce = false

	fun start() {
		if (running) return
		running = true
		scope.launch { loop() }
	}

	fun stop() {
		running = false
		source?.cancel()
		scope.cancel()
	}

	private suspend fun loop() {
		var backoff = 1000L
		while (running) {
			val base = baseUrlProvider() ?: return
			val request = Request.Builder().url("$base/api/events").build()
			suspendCancellableCoroutine { cont ->
				source = EventSources.createFactory(clientProvider()).newEventSource(
					request,
					object : EventSourceListener() {
						override fun onOpen(eventSource: EventSource, response: Response) {
							backoff = 1000L
							connectedOnce = true
						}

						override fun onEvent(
							eventSource: EventSource,
							id: String?,
							type: String?,
							data: String,
						) {
							val obj: JsonObject = runCatching {
								json.parseToJsonElement(data).jsonObject
							}.getOrNull() ?: return
							val chatId = obj["chatId"]?.jsonPrimitive?.contentOrNull
							_events.tryEmit(ServerEvent(type ?: "unknown", chatId))
						}

						override fun onClosed(eventSource: EventSource) {
							if (cont.isActive) cont.resume(Unit)
						}

						override fun onFailure(
							eventSource: EventSource,
							t: Throwable?,
							response: Response?,
						) {
							if (cont.isActive) cont.resume(Unit)
						}
					},
				)
			}
			if (!running) return
			delay(backoff)
			backoff = (backoff * 2).coerceAtMost(30_000L)
		}
	}
}
