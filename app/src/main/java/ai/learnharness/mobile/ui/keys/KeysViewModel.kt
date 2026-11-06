package ai.learnharness.mobile.ui.keys

import ai.learnharness.mobile.ServiceLocator
import ai.learnharness.mobile.data.api.KeyValueBody
import ai.learnharness.mobile.data.api.LhClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** One key row from GET /api/keys: masked status only — plaintext never
 *  leaves the server (owner decision upstream). */
data class KeyRow(
	val id: String, // env-var name, e.g. DEEPSEEK_API_KEY
	val label: String,
	val present: Boolean,
	val masked: String?,
	val source: String?, // "env" | "file" | "none"
	val shadowedByEnv: Boolean,
)

/** A provider without a key yet: the discovery surface for new keys. */
data class ProviderRow(val id: String, val envVar: String, val label: String)

class KeysViewModel : ViewModel() {

	private val repo = ServiceLocator.repository

	private val _rows = MutableStateFlow<List<KeyRow>>(emptyList())
	val rows: kotlinx.coroutines.flow.StateFlow<List<KeyRow>> = _rows.asStateFlow()

	private val _providers = MutableStateFlow<List<ProviderRow>>(emptyList())
	val providers: kotlinx.coroutines.flow.StateFlow<List<ProviderRow>> = _providers.asStateFlow()

	private val _busy = MutableStateFlow(false)
	val busy: kotlinx.coroutines.flow.StateFlow<Boolean> = _busy.asStateFlow()

	private val _status = MutableStateFlow<String?>(null)
	val status: kotlinx.coroutines.flow.StateFlow<String?> = _status.asStateFlow()

	init {
		refresh()
	}

	fun refresh() {
		viewModelScope.launch {
			_busy.value = true
			try {
				repo.ensureConfigured()
				val raw = LhClient.api.keys()
				val keys = (raw["keys"] as? kotlinx.serialization.json.JsonArray)
					?.mapNotNull { it as? JsonObject }
					?.mapNotNull { o ->
						val id = o.str("id") ?: return@mapNotNull null
						val info = o["info"] as? JsonObject
						KeyRow(
							id = id,
							label = info?.str("label") ?: id,
							present = o.bool("present") ?: false,
							masked = o.str("masked"),
							source = o.str("source"),
							shadowedByEnv = o.bool("shadowedByEnv") ?: false,
						)
					} ?: emptyList()
				val providers = (raw["providers"] as? kotlinx.serialization.json.JsonArray)
					?.mapNotNull { it as? JsonObject }
					?.mapNotNull { o ->
						val envVar = o.str("envVar") ?: return@mapNotNull null
						ProviderRow(
							id = o.str("id") ?: envVar,
							envVar = envVar,
							label = o.str("label") ?: envVar,
						)
					} ?: emptyList()
				_rows.value = keys.sortedWith(compareByDescending<KeyRow> { it.present }.thenBy { it.label })
				_providers.value = providers
				_status.value = null
			} catch (e: Exception) {
				_status.value = "Не удалось загрузить ключи: ${e.message}"
			} finally {
				_busy.value = false
			}
		}
	}

	/** PUT /api/keys/[id] — 400 on a bad paste, ok reloads the list. */
	fun save(envVar: String, value: String, onDone: () -> Unit) {
		viewModelScope.launch {
			_busy.value = true
			try {
				repo.ensureConfigured()
				val response = LhClient.api.putKey(envVar, KeyValueBody(value.trim()))
				if (response.isSuccessful) {
					_status.value = "Ключ $envVar сохранён"
					refresh()
					onDone()
				} else {
					val err = runCatching {
						LhClient.json().parseToJsonElement(response.errorBody()?.string() ?: "")
							.jsonObject["error"]?.jsonPrimitive?.contentOrNull
					}.getOrNull()
					_status.value = err ?: "HTTP ${response.code()}"
				}
			} catch (e: Exception) {
				_status.value = e.message ?: e.javaClass.simpleName
			} finally {
				_busy.value = false
			}
		}
	}

	fun delete(envVar: String) {
		viewModelScope.launch {
			_busy.value = true
			try {
				repo.ensureConfigured()
				LhClient.api.deleteKey(envVar)
				_status.value = "Ключ $envVar удалён"
				refresh()
			} catch (e: Exception) {
				_status.value = e.message ?: e.javaClass.simpleName
			} finally {
				_busy.value = false
			}
		}
	}
}

private fun JsonObject.str(key: String): String? =
	(this[key] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull

private fun JsonObject.bool(key: String): Boolean? =
	(this[key] as? kotlinx.serialization.json.JsonPrimitive)?.booleanOrNull
