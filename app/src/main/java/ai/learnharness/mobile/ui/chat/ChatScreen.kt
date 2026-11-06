package ai.learnharness.mobile.ui.chat

import ai.learnharness.mobile.data.db.MessageEntity
import ai.learnharness.mobile.ui.common.BrailleSpinner
import ai.learnharness.mobile.ui.common.MarkdownText
import ai.learnharness.mobile.ui.common.sessionMeta
import ai.learnharness.mobile.ui.theme.LhColors
import ai.learnharness.mobile.ui.theme.lhLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** The web chat's look: serif title + kind chip in the header, flat prose
 *  transcript with mono micro-labels, no bubbles. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
	chatId: String,
	onBack: () -> Unit,
	vm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
	val session by vm.session.collectAsState()
	val messages by vm.messages.collectAsState()
	val loading by vm.loading.collectAsState()
	val error by vm.error.collectAsState()

	val listState = rememberLazyListState()
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
		containerColor = MaterialTheme.colorScheme.background,
		topBar = {
			Column {
				TopAppBar(
					title = {
						Column {
							Text(
								session?.title ?: "…",
								style = MaterialTheme.typography.titleLarge,
								maxLines = 1,
							)
							Row(
								horizontalArrangement = Arrangement.spacedBy(8.dp),
								verticalAlignment = Alignment.CenterVertically,
							) {
								Text(
									(session?.kind ?: "teach").uppercase(),
									style = MaterialTheme.typography.labelSmall,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
								)
								if (session != null) {
									Text(
										sessionMeta(
											busy = session?.busy ?: false,
											compacting = false,
											pendingQuestion = session?.pendingQuestion ?: false,
											pendingProblem = session?.pendingProblem ?: false,
											kind = session?.kind ?: "teach",
											updatedAt = session?.updatedAt,
										),
										style = MaterialTheme.typography.labelSmall,
										color = MaterialTheme.colorScheme.onSurfaceVariant,
									)
								}
								if (session?.busy == true) BrailleSpinner()
							}
						}
					},
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
					colors = TopAppBarDefaults.topAppBarColors(
						containerColor = MaterialTheme.colorScheme.background,
						titleContentColor = MaterialTheme.colorScheme.onBackground,
					),
				)
				HorizontalDivider(color = LhColors.Border)
			}
		},
	) { padding ->
		Column(Modifier.padding(padding).fillMaxSize()) {
			if (loading) {
				Row(
					Modifier.fillMaxWidth().padding(12.dp),
					horizontalArrangement = Arrangement.Center,
				) { BrailleSpinner() }
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
					horizontal = 16.dp,
					vertical = 12.dp,
				),
				verticalArrangement = Arrangement.spacedBy(12.dp),
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
		"user" -> Column {
			Text(
				"YOU",
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			Text(m.text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Normal)
		}

		"tool" -> Text(
			"⚙ ${m.text}",
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)

		"custom" -> Column(
			Modifier
				.fillMaxWidth()
				.background(MaterialTheme.colorScheme.surface)
				.border(1.dp, LhColors.Border, RoundedCornerShape(8.dp))
				.padding(10.dp),
		) {
			Text(
				(m.meta ?: "block").uppercase(),
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			MarkdownText(m.text, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
		}

		else -> MarkdownText(m.text, modifier = Modifier.fillMaxWidth())
	}
}
