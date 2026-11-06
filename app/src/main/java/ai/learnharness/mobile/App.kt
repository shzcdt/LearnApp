package ai.learnharness.mobile

import ai.learnharness.mobile.data.SettingsStore
import ai.learnharness.mobile.data.api.LhClient
import ai.learnharness.mobile.data.db.LhDatabase
import ai.learnharness.mobile.data.repo.LhRepository
import ai.learnharness.mobile.data.sse.EventsChannel
import ai.learnharness.mobile.data.sse.TurnStream
import android.app.Application

/** Manual DI — the app is small; no framework needed. */
object ServiceLocator {
	lateinit var settings: SettingsStore
	lateinit var repository: LhRepository
		private set
	lateinit var events: EventsChannel
	lateinit var turnStream: TurnStream
		private set

	fun init(app: Application) {
		settings = SettingsStore(app)
		repository = LhRepository(settings, LhDatabase.get(app).dao())
		events = EventsChannel(LhClient::sseClient, LhClient::baseUrl)
		turnStream = TurnStream(LhClient::sseClient, LhClient::baseUrl)
	}
}

class App : Application() {
	override fun onCreate() {
		super.onCreate()
		ServiceLocator.init(this)
	}
}
