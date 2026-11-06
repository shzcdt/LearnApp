package ai.learnharness.mobile.data.repo

import ai.learnharness.mobile.data.api.ChatDto
import ai.learnharness.mobile.data.db.MessageEntity
import ai.learnharness.mobile.data.db.SessionEntity
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray

/**
 * Maps loose server JSON onto app entities. The wire format is pi-native:
 * a message has a role and a content list of blocks (text / thinking /
 * toolCall); markers (role "custom") carry rendered UI blocks (plan/quiz
 * shells) in their display field.
 */
object MessageParser {

	fun parseSessions(chats: List<ChatDto>): List<SessionEntity> =
		chats.map { c ->
			SessionEntity(
				id = c.id,
				title = c.title,
				kind = c.kind ?: "teach",
				updatedAt = c.updatedAt,
				messageCount = c.messageCount ?: 0,
				pendingQuestion = c.pendingQuestion ?: false,
				pendingProblem = c.pendingProblem ?: false,
				busy = c.busy ?: false,
			)
		}

	/** Transcript projection: text-bearing lines only, tool activity as chips. */
	fun parseMessages(chatId: String, messages: JsonArray?): List<MessageEntity> {
		if (messages == null) return emptyList()
		val out = mutableListOf<MessageEntity>()
		var idx = 0
		for (el in messages) {
			val m = el as? JsonObject ?: continue
			val role = m.str("role") ?: continue
			when (role) {
				"user" -> {
					val text = concatText(m)
					if (text.isNotBlank()) out += line(chatId, idx++, "user", text, null)
				}

				"assistant" -> {
					// tool activity first (chips), then the prose
					for (block in m.arr("content")) {
						val b = block as? JsonObject ?: continue
						if (b.str("type") == "toolCall") {
							val name = b.str("toolName") ?: b.str("tool") ?: "tool"
							out += line(chatId, idx++, "tool", name, "toolCall")
						}
					}
					// thinking blocks render as collapsible rows, in order
					for (block in m.arr("content")) {
						val b = block as? JsonObject ?: continue
						if (b.str("type") == "thinking") {
							val t = (b.str("thinking") ?: "").trim()
							if (t.isNotBlank()) out += line(chatId, idx++, "thinking", t, "AGENT'S THINKING")
						}
					}
					val text = convertMathDelims(concatText(m))
					if (text.isNotBlank()) out += line(chatId, idx++, "assistant", text, null)
				}

				"custom" -> {
					val display = m.str("display")
					val text = display ?: concatText(m)
					if (text.isNotBlank()) {
						out += line(chatId, idx++, "custom", convertMathDelims(text), m.str("customType") ?: "block")
					}
				}

				// toolResult rows are noise for a reader; skipped
			}
		}
		return out
	}

	private fun line(chatId: String, idx: Int, role: String, text: String, meta: String?) =
		MessageEntity(chatId, idx, role, text, meta)

	private fun concatText(message: JsonObject): String =
		message.arr("content")
			.mapNotNull { b -> b as? JsonObject }
			.filter { it.str("type") == "text" }
			.joinToString("") { it.str("text") ?: "" }
			.trim()

	/**
	 * The web client renders legacy LaTeX delimiters via transformMathDelimiters
	 * (src/core/math-delims.ts): \(x\) → $x$, \[x\] → $$x$$. Same conversion here,
	 * before the text reaches Markwon.
	 */
	private fun convertMathDelims(text: String): String =
		text
			.replace(Regex("\\\\\\((.+?)\\\\\\)")) { match -> "\$${match.groupValues[1]}\$" }
			.replace(Regex("\\\\\\[(.+?)\\\\]")) { match -> "\$\$${match.groupValues[1]}\$\$" }

	private fun concatBlockField(message: JsonObject, field: String): String =
		message.arr("content")
			.mapNotNull { b -> b as? JsonObject }
			.filter { it.str("type") == "thinking" }
			.joinToString("\n") { it.str(field) ?: "" }
			.trim()
}

private fun JsonObject.str(key: String): String? =
	(this[key] as? JsonElement)?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }

private fun JsonObject.arr(key: String): JsonArray =
	(this[key] as? JsonArray) ?: JsonArray(emptyList())
