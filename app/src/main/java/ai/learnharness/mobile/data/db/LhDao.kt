package ai.learnharness.mobile.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Cached session list entry (server is the source of truth). */
@Entity(tableName = "sessions")
data class SessionEntity(
	@PrimaryKey val id: String,
	val title: String,
	val kind: String, // "teach" | "practice"
	val updatedAt: String?,
	val messageCount: Int,
	val pendingQuestion: Boolean,
	val pendingProblem: Boolean,
	val busy: Boolean,
)

/** Cached transcript line for a chat, ordered by idx. */
@Entity(tableName = "messages", primaryKeys = ["chatId", "idx"])
data class MessageEntity(
	val chatId: String,
	val idx: Int,
	val role: String, // "user" | "assistant" | "tool" | "custom"
	val text: String,
	val meta: String?, // e.g. tool name, question kind — free-form annotation
)

@Dao
interface LhDao {

	@Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
	fun sessions(): Flow<List<SessionEntity>>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun putSessions(sessions: List<SessionEntity>)

	@Query("DELETE FROM sessions")
	suspend fun clearSessions()

	@Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY idx ASC")
	fun messages(chatId: String): Flow<List<MessageEntity>>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun putMessages(messages: List<MessageEntity>)

	@Query("DELETE FROM messages WHERE chatId = :chatId")
	suspend fun clearMessages(chatId: String)
}
