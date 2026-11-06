package ai.learnharness.mobile.ui.sessions

import ai.learnharness.mobile.data.db.SessionEntity
import ai.learnharness.mobile.ui.common.BrailleSpinner
import ai.learnharness.mobile.ui.common.sessionMeta
import ai.learnharness.mobile.ui.theme.LhColors
import ai.learnharness.mobile.ui.theme.lhLabel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/** The web sidebar's look: mono uppercase section labels, filter row, and
 *  session rows with the state meta line. */
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

	val visible = sessions.filter { filter == "all" || it.kind == filter }

	Scaffold(
		containerColor = MaterialTheme.colorScheme.background,
		topBar = {
			TopAppBar(
				title = { Text("LearnHarness", style = MaterialTheme.typography.titleLarge) },
				actions = {
					IconButton(onClick = onOpenSettings) {
						Icon(Icons.Default.Settings, contentDescription = "Настройки")
					}
				},
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = MaterialTheme.colorScheme.background,
					titleContentColor = MaterialTheme.colorScheme.onBackground,
				),
			)
		},
	) { padding ->
		Column(Modifier.padding(padding).fillMaxSize()) {
			HorizontalDivider(color = LhColors.Border)

			Row(
				Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				listOf("all" to "ALL", "teach" to "TEACHING", "practice" to "PRACTICE").forEach { (key, label) ->
					Surface(
						shape = RoundedCornerShape(4.dp),
						color = if (filter == key) LhColors.Accent else MaterialTheme.colorScheme.background,
						modifier = Modifier.clickable { vm.setFilter(key) },
					) {
						Text(
							label,
							style = MaterialTheme.typography.labelSmall,
							color = if (filter == key) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
							modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
						)
					}
				}
				if (refreshing) BrailleSpinner(Modifier.padding(start = 4.dp))
			}

			HorizontalDivider(color = LhColors.Border)

			Text(
				"SESSIONS",
				style = lhLabel(),
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
			)

			if (error != null) {
				Text(
					"Сервер недоступен: $error",
					color = MaterialTheme.colorScheme.error,
					modifier = Modifier.padding(horizontal = 16.dp),
				)
			}

			LazyColumn(Modifier.fillMaxSize()) {
				items(visible, key = { it.id }) { s ->
					SessionRow(s) { onOpenChat(s.id) }
				}
				if (visible.isEmpty() && !refreshing && error == null) {
					item {
						Text(
							"Пока нет сессий",
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
							modifier = Modifier.padding(16.dp),
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
			.padding(horizontal = 16.dp, vertical = 10.dp),
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Text(s.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
			if (s.busy) BrailleSpinner(Modifier.padding(start = 8.dp))
		}
		Text(
			"${if (s.kind == "practice") "Practice" else "Teaching"} · ${sessionMeta(s.busy, false, s.pendingQuestion, s.pendingProblem, s.kind, s.updatedAt)}",
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
	}
}
