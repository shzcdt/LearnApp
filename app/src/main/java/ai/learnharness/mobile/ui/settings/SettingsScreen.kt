package ai.learnharness.mobile.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	onBack: () -> Unit,
	vm: SettingsViewModel = viewModel(),
) {
	val serverUrl by vm.serverUrl.collectAsState()
	val secret by vm.desktopSecret.collectAsState()
	val status by vm.status.collectAsState()
	val saved by vm.saved.collectAsState()
	val checking by vm.checking.collectAsState()

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Соединение") },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
					}
				},
			)
		},
	) { padding ->
		Column(
			Modifier
				.padding(padding)
				.fillMaxSize()
				.padding(16.dp),
		) {
			OutlinedTextField(
				value = serverUrl,
				onValueChange = { vm.serverUrl.value = it },
				label = { Text("Адрес сервера") },
				placeholder = { Text("http://100.76.226.86:3000") },
				singleLine = true,
				modifier = Modifier.fillMaxWidth(),
			)
			OutlinedTextField(
				value = secret,
				onValueChange = { vm.desktopSecret.value = it },
				label = { Text("Desktop secret (необязательно)") },
				singleLine = true,
				modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
			)
			Text(
				"Сервер доступен только через Tailscale. Секрет нужен, только если сервер запущен в desktop-режиме.",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(top = 8.dp),
			)
			Row(Modifier.padding(top = 16.dp)) {
				Button(onClick = { vm.save() }) { Text("Сохранить") }
				OutlinedButton(
					onClick = { vm.check() },
					enabled = !checking,
					modifier = Modifier.padding(start = 12.dp),
				) { Text(if (checking) "Проверяю…" else "Проверить") }
			}
			if (status != null) {
				Text(
					status ?: "",
					color = if (saved == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
					modifier = Modifier.padding(top = 16.dp),
				)
			}
		}
	}
}
