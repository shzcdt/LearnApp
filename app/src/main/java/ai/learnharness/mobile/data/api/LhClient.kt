package ai.learnharness.mobile.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

/**
 * Connection to one LearnHarness server. Rebuilt whenever the user changes
 * the server URL or the optional desktop secret (proxy.ts honours the
 * x-learnharness-desktop header when the server runs in desktop mode).
 */
object LhClient {

	private val json = Json {
		ignoreUnknownKeys = true
		isLenient = true
	}

	@Volatile
	private var secret: String? = null

	@Volatile
	private var apiRef: LhApi? = null

	@Volatile
	private var currentBaseUrl: String? = null

	val api: LhApi
		get() = apiRef ?: throw IllegalStateException("LhClient is not configured with a server URL")

	val baseUrl: String?
		get() = currentBaseUrl

	fun isConfigured(): Boolean = apiRef != null

	/** Accepts bare host[:port], adds http:// if no scheme, trims trailing '/'. */
	fun configure(rawUrl: String, desktopSecret: String? = null) {
		var url = rawUrl.trim()
		if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://$url"
		url = url.trimEnd('/')
		require(url.length > "http://".length) { "Empty server URL" }
		this.secret = desktopSecret?.takeIf { it.isNotBlank() }
		this.currentBaseUrl = url

		val authInterceptor = Interceptor { chain ->
			val req = chain.request().newBuilder().apply {
				secret?.let { header("x-learnharness-desktop", it) }
			}.build()
			chain.proceed(req)
		}

		val http = OkHttpClient.Builder()
			.addInterceptor(authInterceptor)
			.connectTimeout(10, TimeUnit.SECONDS)
			.readTimeout(75, TimeUnit.SECONDS) // SSE frames arrive within seconds; generous for slow relays
			.build()

		apiRef = Retrofit.Builder()
			.baseUrl("$url/")
			.client(http)
			.addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
			.build()
			.create(LhApi::class.java)
	}

	/** Raw OkHttp client for SSE streams — same auth header, longer read timeout. */
	fun sseClient(): OkHttpClient {
		check(apiRef != null) { "LhClient is not configured" }
		return OkHttpClient.Builder()
			.addInterceptor(Interceptor { chain ->
				val req = chain.request().newBuilder().apply {
					secret?.let { header("x-learnharness-desktop", it) }
				}.build()
				chain.proceed(req)
			})
			.connectTimeout(10, TimeUnit.SECONDS)
			.readTimeout(0, TimeUnit.MILLISECONDS) // SSE: server pushes pings every 25s
			.build()
	}

	fun json(): Json = json
}
