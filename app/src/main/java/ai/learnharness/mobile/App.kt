package ai.learnharness.mobile

import ai.learnharness.mobile.data.SettingsStore
import ai.learnharness.mobile.data.db.LhDatabase
import ai.learnharness.mobile.data.repo.LhRepository
import android.app.Application

/** Manual DI — the app is small; no framework needed. */
object ServiceLocator {
	lateinit var settings: SettingsStore
	lateinit var repository: LhRepository
		private set

	fun init(app: Application) {
		settings = SettingsStore(app)
		repository = LhRepository(settings, LhDatabase.get(app).dao())
	}
}

class App : Application() {
	override fun onCreate() {
		super.onCreate()
		ServiceLocator.init(this)
	}
}
