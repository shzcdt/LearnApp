package ai.learnharness.mobile.ui.sessions

import ai.learnharness.mobile.data.db.SessionEntity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
	onOpenChat: (String) -> Unit,
	onOpenSettings: () -> Unit,
	vm: SessionsViewModel = viewModel(),
) {
	val sessions by vm.sessions.collectAsState()
	val filter by vm.filter.collectAsState()
	val refreshing by vm.refreshing.collectAsState()
	val error by vm.error.collectAsState()

	val visible = remember(sessions, filter) {
		sessions.filter { filter == "all" || it.kind == filter }
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("LearnHarness") },
				actions = {
					IconButton(onClick = onOpenSettings) {
						Icon(Icons.Default.Settings, contentDescription = "Настройки")
					}
				},
			)
		},
	) { padding ->
		Column(Modifier.padding(padding).fillMaxSize()) {
			Row(
				Modifier.fillMaxWidth().padding(horizontal = 12.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
			) {
				listOf("all" to "Все", "teach" to "Teaching", "practice" to "Practice").forEach { (key, label) ->
					FilterChip(
						selected = filter == key,
						onClick = { vm.setFilter(key) },
						label = { Text(label) },
					)
				}
			}

			if (error != null) {
				Text(
					"Сервер недоступен: $error",
					color = MaterialTheme.colorScheme.error,
					modifier = Modifier.padding(12.dp),
				)
			}

			if (refreshing) {
				Row(
					Modifier.fillMaxWidth().padding(12.dp),
					horizontalArrangement = Arrangement.Center,
				) { CircularProgressIndicator() }
			}

			LazyColumn(Modifier.fillMaxSize()) {
				items(visible, key = { it.id }) { s ->
					SessionRow(s) { onOpenChat(s.id) }
				}
				if (visible.isEmpty() && !refreshing && error == null) {
					item {
						Text(
							"Пока нет сессий. Потяни список вниз позже — теперь обновление кнопкой ⋮ → «Обновить».",
							modifier = Modifier.padding(16.dp),
							style = MaterialTheme.typography.bodySmall,
						)
					}
				}
			}
		}
	}
}

@Composable
private fun SessionRow(s: SessionEntity, onClick: () -> Unit) {
	Column(
		Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(horizontal = 16.dp, vertical = 12.dp),
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Text(s.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
			if (s.busy) CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp))
		}
		val state = when {
			s.busy -> "Работает…"
			s.pendingQuestion -> "Ждёт ответа"
			s.pendingProblem -> "Проблема ждёт"
			else -> s.updatedAt?.take(10) ?: ""
		}
		Text(
			"${if (s.kind == "practice") "Practice" else "Teaching"} · $state",
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
	}
}
