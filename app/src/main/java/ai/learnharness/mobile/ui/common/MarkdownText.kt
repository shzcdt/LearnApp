package ai.learnharness.mobile.ui.common

import ai.learnharness.mobile.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import io.noties.markwon.Markwon

/**
 * Renders markdown into a TextView (Markwon spans — code, lists, tables,
 * headers) inside Compose. Prose look matches the server UI: Source Sans 3,
 * 16sp, #e6e6e6 on the dark canvas, leading 1.5.
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
				textSize = 16f
				setTextColor(0xFFE6E6E6.toInt())
				typeface = ResourcesCompat.getFont(ctx, R.font.font_sourcesans3_regular)
				setLineSpacing(0f, 1.4f)
				movementMethod = android.text.method.LinkMovementMethod.getInstance()
			}
		},
		update = { tv -> markwon.setMarkdown(tv, markdown) },
	)
}
