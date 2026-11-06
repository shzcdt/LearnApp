package ai.learnharness.mobile.data.repo

import ai.learnharness.mobile.data.SettingsStore
import ai.learnharness.mobile.data.api.LhClient
import ai.learnharness.mobile.data.db.LhDao
import ai.learnharness.mobile.data.db.MessageEntity
import ai.learnharness.mobile.data.db.SessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.jsonArray

/**
 * Single orchestrator: REST fetches land in Room, UI observes Room flows.
 * The server stays the source of truth; Room is an instant-start cache.
 */
class LhRepository(
	private val settings: SettingsStore,
	private val dao: LhDao,
) {

	suspend fun ensureConfigured() {
		if (!LhClient.isConfigured()) {
			val c = settings.current()
			LhClient.configure(c.serverUrl, c.desktopSecret)
		}
	}

	suspend fun saveConnection(serverUrl: String, desktopSecret: String) {
		settings.save(serverUrl, desktopSecret)
		LhClient.configure(serverUrl, desktopSecret)
	}

	suspend fun connection() = settings.current()

	fun sessions(): Flow<List<SessionEntity>> = dao.sessions()

	fun session(id: String): Flow<SessionEntity?> = dao.sessionById(id)

	/** Notify→fetch: the UI calls this on start, on manual refresh, and later on SSE events. */
	suspend fun refreshSessions() {
		ensureConfigured()
		val chats = LhClient.api.chats().chats
		// clear+put drops chats deleted on the server; tiny table, no need to diff
		dao.clearSessions()
		dao.putSessions(MessageParser.parseSessions(chats))
	}

	fun messages(chatId: String): Flow<List<MessageEntity>> = dao.messages(chatId)

	suspend fun openChat(chatId: String) {
		ensureConfigured()
		val record = LhClient.api.chatRecord(chatId)
		val messages = record["messages"]?.jsonArray
		dao.clearMessages(chatId)
		dao.putMessages(MessageParser.parseMessages(chatId, messages))
	}
}
