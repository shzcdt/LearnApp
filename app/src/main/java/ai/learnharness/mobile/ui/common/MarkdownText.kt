package ai.learnharness.mobile.ui.common

import ai.learnharness.mobile.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableTheme

/**
 * Renders markdown into a TextView inside Compose — the server UI's prose
 * pipeline on Android: Source Sans 3, 16sp, #e6e6e6, leading 1.4, GFM
 * tables, LaTeX ($…$ / $$…$$), strikethrough, links.
 */
@Composable
fun MarkdownText(
	markdown: String,
	modifier: Modifier = Modifier,
) {
	val context = LocalContext.current
	val markwon = remember(context) {
		Markwon.builder(context)
			.usePlugin(CorePlugin.create())
			.usePlugin(TablePlugin.create(TableTheme.buildWithDefaults(context).build()))
			.usePlugin(StrikethroughPlugin.create())
			.usePlugin(JLatexMathPlugin.create(16f))
			.build()
	}
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
