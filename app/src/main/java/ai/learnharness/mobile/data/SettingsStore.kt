package ai.learnharness.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** App-local settings: which server to talk to (and optional desktop secret). */
class SettingsStore(private val context: Context) {

	companion object {
		val KEY_SERVER_URL = stringPreferencesKey("server_url")
		val KEY_DESKTOP_SECRET = stringPreferencesKey("desktop_secret")
		const val DEFAULT_URL = "http://100.76.226.86:3000"
	}

	data class Connection(val serverUrl: String, val desktopSecret: String)

	val connection: Flow<Connection> = context.dataStore.data.map { prefs ->
		Connection(
			serverUrl = prefs[KEY_SERVER_URL] ?: DEFAULT_URL,
			desktopSecret = prefs[KEY_DESKTOP_SECRET] ?: "",
		)
	}

	suspend fun current(): Connection = connection.first()

	suspend fun save(serverUrl: String, desktopSecret: String) {
		context.dataStore.edit { prefs ->
			prefs[KEY_SERVER_URL] = serverUrl.trim()
			prefs[KEY_DESKTOP_SECRET] = desktopSecret.trim()
		}
	}
}
