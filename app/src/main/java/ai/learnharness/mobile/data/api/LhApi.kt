package ai.learnharness.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * LearnHarness server API (web mode, no auth by design — tailnet-only).
 * Only the endpoints the client actually consumes live here.
 */
interface LhApi {

	@GET("api/chats")
	suspend fun chats(): ChatsResponse

	@GET("api/chats/{id}")
	suspend fun chatRecord(@Path("id") id: String): RawJson

	// Response<> wrappers: the client must distinguish 409 (turn running)
	// and 503 (models_not_configured {missing}) from plain failures.
	@POST("api/chat")
	suspend fun sendInput(@Body body: ChatInputBody): Response<OkResponse>

	@POST("api/chats/{id}/steer")
	suspend fun steer(@Path("id") id: String, @Body body: SteerBody): Response<OkResponse>

	@POST("api/activity/abort")
	suspend fun abort(@Body body: AbortBody): Response<OkResponse>

	@GET("api/models")
	suspend fun models(): RawJson

	@POST("api/chats")
	suspend fun createChat(@Body body: CreateChatBody): RawJson
}

@kotlinx.serialization.Serializable
data class SteerBody(val input: SteerInput)

@kotlinx.serialization.Serializable
data class SteerInput(val type: String = "steer", val id: String, val text: String)

@kotlinx.serialization.Serializable
data class AbortBody(val kind: String = "turn", val id: String)

@kotlinx.serialization.Serializable
data class CreateChatBody(
	val title: String,
	val kind: String? = null,
)
