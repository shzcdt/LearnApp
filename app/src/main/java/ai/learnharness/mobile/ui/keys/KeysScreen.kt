package ai.learnharness.mobile.ui.keys

import ai.learnharness.mobile.ui.theme.LhColors
import ai.learnharness.mobile.ui.theme.lhLabel
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/** BYOK keys: statuses (masked) + add/save/delete. The server never
 *  returns plaintext — only last-4 masks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeysScreen(
	onBack: () -> Unit,
	vm: KeysViewModel = viewModel(),
) {
	val rows by vm.rows.collectAsState()
	val providers by vm.providers.collectAsState()
	val busy by vm.busy.collectAsState()
	val status by vm.status.collectAsState()

	// dialog target: existing key row or a provider to add
	var editTarget by remember { mutableStateOf<String?>(null) }
	var editLabel by remember { mutableStateOf("") }
	var hasKey by remember { mutableStateOf(false) }

	Scaffold(
		containerColor = MaterialTheme.colorScheme.background,
		topBar = {
			TopAppBar(
				title = { Text("Ключи API", style = MaterialTheme.typography.titleLarge) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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

			if (busy) {
				Text(
					"…",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(16.dp),
				)
			}
			if (status != null) {
				Text(
					status ?: "",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
				)
			}

			Text(
				"CONFIGURED",
				style = lhLabel(),
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
			)
			LazyColumn(Modifier.fillMaxSize()) {
				val present = rows.filter { it.present }
				if (present.isEmpty()) {
					item {
						Text(
							"Ключей ещё нет — добавь провайдера ниже",
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
							modifier = Modifier.padding(horizontal = 16.dp),
						)
					}
				}
				items(present, key = { it.id }) { row ->
					Column(
						Modifier
							.fillMaxWidth()
							.clickable {
								editTarget = row.id
								editLabel = row.label
								hasKey = true
							}
							.padding(horizontal = 16.dp, vertical = 10.dp),
					) {
						Row(verticalAlignment = Alignment.CenterVertically) {
							Text(row.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
							Text(
								"✓ ${row.masked ?: "set"}",
								style = MaterialTheme.typography.labelSmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
							)
						}
						Text(
							buildString {
								append(row.id)
								if (row.shadowedByEnv) append(" · shadowed by env")
							},
							style = MaterialTheme.typography.labelSmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
				}

				item {
					Text(
						"ADD A PROVIDER",
						style = lhLabel(),
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
					)
				}
				val addable = providers.filter { p -> rows.none { it.id == p.envVar && it.present } }
				items(addable, key = { it.envVar }) { p ->
					Text(
						p.label,
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.primary,
						modifier = Modifier
							.fillMaxWidth()
							.clickable {
								editTarget = p.envVar
								editLabel = p.label
								hasKey = false
							}
							.padding(horizontal = 16.dp, vertical = 10.dp),
					)
				}
			}
		}
	}

	if (editTarget != null) {
		var value by remember(editTarget) { mutableStateOf("") }
		AlertDialog(
			onDismissRequest = { editTarget = null },
			title = { Text(editLabel) },
			text = {
				Column {
					Text(
						editTarget ?: "",
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
					OutlinedTextField(
						value = value,
						onValueChange = { value = it },
						label = { Text(if (hasKey) "Новый ключ (заменит текущий)" else "Вставь ключ") },
						visualTransformation = PasswordVisualTransformation(),
						singleLine = true,
						modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
					)
				}
			},
			confirmButton = {
				Button(
					enabled = value.isNotBlank() && !busy,
					onClick = {
						val target = editTarget
						vm.save(target ?: "", value) { editTarget = null }
					},
				) { Text("Сохранить") }
			},
			dismissButton = {
				Column(horizontalAlignment = Alignment.End) {
					if (hasKey) {
						OutlinedButton(
							onClick = {
								vm.delete(editTarget ?: "")
								editTarget = null
							},
							colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
								contentColor = MaterialTheme.colorScheme.error,
							),
							shape = RoundedCornerShape(8.dp),
							modifier = Modifier.background(androidx.compose.ui.graphics.Color.Transparent),
						) { Text("Удалить") }
					}
					TextButton(onClick = { editTarget = null }) { Text("Отмена") }
				}
			},
		)
	}
}
