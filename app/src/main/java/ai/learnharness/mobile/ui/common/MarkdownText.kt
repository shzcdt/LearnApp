package ai.learnharness.mobile.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

/**
 * Renders markdown into a TextView (Markwon spans — code, lists, tables,
 * headers) inside Compose.
 */
@Composable
fun MarkdownText(
	markdown: String,
	modifier: Modifier = Modifier,
) {
	val context = LocalContext.current
	val markwon = remember(context) { Markwon.create(context) }
	AndroidView(
		modifier = modifier,
		factory = { ctx ->
			android.widget.TextView(ctx).apply {
				textSize = 15f
				setTextColor(android.graphics.Color.parseColor("#E8E4D8"))
				movementMethod = android.text.method.LinkMovementMethod.getInstance()
			}
		},
		update = { tv -> markwon.setMarkdown(tv, markdown) },
	)
}
