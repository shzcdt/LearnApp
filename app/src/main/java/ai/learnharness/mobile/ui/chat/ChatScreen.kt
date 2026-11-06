package ai.learnharness.mobile.ui.chat

import ai.learnharness.mobile.data.db.MessageEntity
import ai.learnharness.mobile.data.sse.LiveRow
import ai.learnharness.mobile.ui.common.BrailleSpinner
import ai.learnharness.mobile.ui.common.MarkdownText
import ai.learnharness.mobile.ui.common.sessionMeta
import ai.learnharness.mobile.ui.theme.LhColors
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.lifecycle.viewmodel.compose.viewModel

/** The web chat's look: serif title + kind chip in the header, flat prose
 *  transcript with mono micro-labels, live streaming turn, composer dock. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
	chatId: String,
	onBack: () -> Unit,
	vm: ChatViewModel = viewModel(),
) {
	val session by vm.session.collectAsState()
	val messages by vm.messages.collectAsState()
	val loading by vm.loading.collectAsState()
	val error by vm.error.collectAsState()
	val missingSlots by vm.missingSlots.collectAsState()
	val busy by vm.busy.collectAsState()
	val live by vm.live.collectAsState()
	val lastSteer by vm.lastSteer.collectAsState()

	var composer by rememberSaveable { mutableStateOf("") }
	var abortDialog by remember { mutableStateOf(false) }

	val listState = rememberLazyListState()
	val tailCount = messages.size + (if (live != null || busy) 1 else 0)
	val atBottom by remember { derivedStateOf {
		val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()
		last == null || last.index >= listState.layoutInfo.totalItemsCount - 2
	} }
	LaunchedEffect(tailCount, live?.rows?.size) {
		if (atBottom) listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
	}

	Scaffold(
		containerColor = MaterialTheme.colorScheme.background,
		bottomBar = {
			Column(
				Modifier
					.background(MaterialTheme.colorScheme.background)
					.navigationBarsPadding()
					.imePadding(),
			) {
				HorizontalDivider(color = LhColors.Border)
				// queued steer: a quiet right-aligned line above the composer
				if (busy && lastSteer != null) {
					Text(
						"steer: $lastSteer",
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 16.dp, vertical = 4.dp),
						textAlign = androidx.compose.ui.text.style.TextAlign.End,
						maxLines = 1,
					)
				}
				Row(
					Modifier
						.fillMaxWidth()
						.padding(horizontal = 8.dp, vertical = 6.dp),
					verticalAlignment = Alignment.Bottom,
				) {
					OutlinedTextField(
						value = composer,
						onValueChange = { composer = it },
						modifier = Modifier.weight(1f),
						placeholder = {
							Text(
								if (busy) "Steer the lesson…" else "Ask, answer, or steer the lesson…",
								style = MaterialTheme.typography.bodySmall,
							)
						},
						textStyle = MaterialTheme.typography.bodyMedium,
						maxLines = 5,
						shape = RoundedCornerShape(12.dp),
					)
					if (busy && composer.isBlank()) {
						// Stop (abort) while empty-handed and busy
						IconButton(onClick = { abortDialog = true }) {
							Text("■", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
						}
					} else {
						IconButton(
							onClick = {
								vm.sendOrSteer(composer)
								composer = ""
							},
							enabled = composer.isNotBlank(),
						) {
							Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
						}
					}
				}
			}
		},
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
								if (busy) BrailleSpinner()
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
					error ?: "",
					color = MaterialTheme.colorScheme.error,
					modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
				)
				if (missingSlots.isNotEmpty()) {
					Text(
						"Не настроены слоты моделей: ${missingSlots.joinToString(", ")}. Настрой их на сервере (Settings → Agent → Models) или через веб-версию.",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.padding(horizontal = 16.dp),
					)
				}
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
				// the live turn: rows replace every frame
				if (live != null) {
					items(live!!.rows) { row ->
						LiveRowView(row)
					}
					if (live?.toolName != null) {
						item {
							Text(
								"⚙ ${live?.toolName}",
								style = MaterialTheme.typography.labelSmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
							)
						}
					}
				} else if (busy) {
					item {
						Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
							BrailleSpinner()
							Text("THINKING…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
						}
					}
				}
				if (messages.isEmpty() && !loading && error == null && live == null && !busy) {
					item { Text("Пусто", style = MaterialTheme.typography.bodySmall) }
				}
			}
		}
	}

	if (abortDialog) {
		AlertDialog(
			onDismissRequest = { abortDialog = false },
			title = { Text("Прервать ход?") },
			text = { Text("Частичный ответ сохранится в транскрипте.") },
			confirmButton = {
				Button(onClick = {
					vm.abort()
					abortDialog = false
				}) { Text("Прервать") }
			},
			dismissButton = {
				TextButton(onClick = { abortDialog = false }) { Text("Отмена") }
			},
		)
	}
}

@Composable
private fun LiveRowView(row: LiveRow) {
	when {
		row.isThinking -> Text(
			row.text,
			style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)

		row.role == "user" -> Column {
			Text("YOU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
			Text(row.text, style = MaterialTheme.typography.bodyLarge)
		}

		row.role == "tool" -> Text(
			"⚙ ${row.text}",
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)

		else -> MarkdownText(row.text, modifier = Modifier.fillMaxWidth())
	}
}

@Composable
private fun MessageRow(m: MessageEntity) {
	when (m.role) {
		"user" -> Column {
			Text("YOU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
			Text(m.text, style = MaterialTheme.typography.bodyLarge)
		}

		"tool" -> Text(
			"⚙ ${m.text}",
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)

		"thinking" -> ThinkingRow(m.text)

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

/** Collapsible "AGENT'S THINKING" block, closed by default (web parity). */
@Composable
private fun ThinkingRow(text: String) {
	var open by remember { mutableStateOf(false) }
	Column {
		Text(
			if (open) "▾ AGENT'S THINKING" else "▸ AGENT'S THINKING",
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier
				.clickable { open = !open }
				.padding(vertical = 2.dp),
		)
		if (open) {
			Column(
				Modifier
					.fillMaxWidth()
					.background(MaterialTheme.colorScheme.surface)
					.padding(10.dp),
			) {
				Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
		}
	}
}
