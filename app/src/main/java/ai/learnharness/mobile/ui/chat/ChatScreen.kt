package ai.learnharness.mobile.ui.chat

import ai.learnharness.mobile.data.db.MessageEntity
import ai.learnharness.mobile.ui.common.MarkdownText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
	chatId: String,
	onBack: () -> Unit,
	vm: ChatViewModel = viewModel(),
) {
	val messages by vm.messages.collectAsState()
	val loading by vm.loading.collectAsState()
	val error by vm.error.collectAsState()

	val listState = rememberLazyListState()
	// Stick to the bottom while the tail changes (streaming later, opening now)
	val atBottom by remember {
		derivedStateOf {
			val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()
			last == null || last.index >= listState.layoutInfo.totalItemsCount - 2
		}
	}
	LaunchedEffect(messages.size) {
		if (atBottom && messages.isNotEmpty()) {
			listState.animateScrollToItem(messages.size - 1)
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Сессия", maxLines = 1) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
					}
				},
				actions = {
					IconButton(onClick = { vm.open() }) {
						Text("↻", style = MaterialTheme.typography.titleLarge)
					}
				},
			)
		},
	) { padding ->
		Column(Modifier.padding(padding).fillMaxSize()) {
			if (loading) {
				Row(
					Modifier.fillMaxWidth().padding(12.dp),
					horizontalArrangement = Arrangement.Center,
				) { CircularProgressIndicator() }
			}
			if (error != null) {
				Text(
					"Не удалось загрузить сессию: $error",
					color = MaterialTheme.colorScheme.error,
					modifier = Modifier.padding(12.dp),
				)
			}
			LazyColumn(
				state = listState,
				modifier = Modifier.fillMaxSize(),
				contentPadding = androidx.compose.foundation.layout.PaddingValues(
					horizontal = 12.dp,
					vertical = 8.dp,
				),
				verticalArrangement = Arrangement.spacedBy(10.dp),
			) {
				items(messages) { m ->
					MessageRow(m)
				}
				if (messages.isEmpty() && !loading && error == null) {
					item { Text("Пусто", style = MaterialTheme.typography.bodySmall) }
				}
			}
		}
	}
}

@Composable
private fun MessageRow(m: MessageEntity) {
	when (m.role) {
		"user" -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
			Column(
				Modifier
					.clip(RoundedCornerShape(12.dp))
					.background(MaterialTheme.colorScheme.surfaceVariant)
					.padding(10.dp),
			) {
				Text(m.text, style = MaterialTheme.typography.bodyMedium)
			}
		}

		"tool" -> Row(verticalAlignment = Alignment.CenterVertically) {
			Text(
				"⚙ ${m.text}",
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}

		"custom" -> Column(
			Modifier
				.clip(RoundedCornerShape(10.dp))
				.background(MaterialTheme.colorScheme.surface)
				.padding(10.dp),
		) {
			Text(
				m.meta ?: "блок",
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			MarkdownText(m.text, modifier = Modifier.fillMaxWidth())
		}

		else -> MarkdownText(m.text, modifier = Modifier.fillMaxWidth())
	}
}
