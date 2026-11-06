package ai.learnharness.mobile.ui.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

private val BRAILLE = charArrayOf('⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏')

/**
 * The web UI's BrailleSpinner (components/sidebar.tsx) — monochrome motion,
 * exists exactly as long as the work does.
 */
@Composable
fun BrailleSpinner(modifier: Modifier = Modifier) {
	var frame by remember { mutableIntStateOf(0) }
	LaunchedEffect(Unit) {
		while (true) {
			delay(80)
			frame++
		}
	}
	Text("${BRAILLE[frame % BRAILLE.size]}", modifier = modifier)
}
