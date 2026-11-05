package ai.learnharness.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// LearnHarness is a dark-first terminal-ish UI (#141414 canvas, warm paper
// prose). The phone app mirrors that: dark scheme regardless of system theme.
private val LhDark = darkColorScheme(
	primary = androidx.compose.ui.graphics.Color(0xFFD8CFB4),
	onPrimary = androidx.compose.ui.graphics.Color(0xFF141414),
	secondary = androidx.compose.ui.graphics.Color(0xFF9A948A),
	background = androidx.compose.ui.graphics.Color(0xFF141414),
	onBackground = androidx.compose.ui.graphics.Color(0xFFE8E4D8),
	surface = androidx.compose.ui.graphics.Color(0xFF1C1C1C),
	onSurface = androidx.compose.ui.graphics.Color(0xFFE8E4D8),
	surfaceVariant = androidx.compose.ui.graphics.Color(0xFF242424),
	onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB5B0A4),
	error = androidx.compose.ui.graphics.Color(0xFFE57373),
	outline = androidx.compose.ui.graphics.Color(0xFF3A3A3A),
)

@Composable
fun LearnHarnessTheme(content: @Composable () -> Unit) {
	// dark-only: the server UI is dark-first; light variant later if wanted
	isSystemInDarkTheme()
	MaterialTheme(colorScheme = LhDark, content = content)
}
