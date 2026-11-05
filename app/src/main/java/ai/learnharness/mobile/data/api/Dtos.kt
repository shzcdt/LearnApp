package ai.learnharness.mobile.data.api

import kotlinx.serialization.json.JsonObject

/**
 * Server DTOs are decoded loosely (kotlinx JsonObject + manual mapping):
 * the wire format evolves with the server, and the client only needs a
 * small projection of each payload. Strict data classes live in the app
 * layer (see data.db entities and ui models).
 */

@kotlinx.serialization.Serializable
data class ChatsResponse(
	val chats: List<ChatDto> = emptyList(),
)

@kotlinx.serialization.Serializable
data class ChatDto(
	val id: String,
	val title: String,
	val createdAt: String? = null,
	val updatedAt: String? = null,
	val kind: String? = null, // "teach" | "practice"
	val messageCount: Int? = null,
	val phase: String? = null,
	val pendingQuestion: Boolean? = null,
	val pendingProblem: Boolean? = null,
	val busy: Boolean? = null,
	val compacting: Boolean? = null,
)

/** POST /api/chat input envelope (M2+). */
@kotlinx.serialization.Serializable
data class ChatInputBody(
	val chatId: String,
	val input: JsonObject,
)

@kotlinx.serialization.Serializable
data class OkResponse(
	val ok: Boolean? = null,
	val error: String? = null,
)

typealias RawJson = JsonObject
