package ai.learnharness.mobile.ui.theme

import ai.learnharness.mobile.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Design tokens lifted 1:1 from the server UI (learnharness app/globals.css
 * :root): dark-only, JetBrains Mono chrome, Source Sans 3 prose, CMU Serif
 * bold titles, prose size 16px / leading 1.5.
 */
object LhColors {
	val Background = Color(0xFF141414)
	val Foreground = Color(0xFFE6E6E6)
	val Card = Color(0xFF1C1C1C)
	val MutedForeground = Color(0xFF8D8D8D)
	val Accent = Color(0xFF2A2A2A)
	val Border = Color(0xFF434343)
	val Destructive = Color(0xFFE5484D) // oklch(0.704 0.191 22.216) approximated
}

private val JbMono = FontFamily(
	Font(R.font.font_jbmono_regular, FontWeight.Normal),
	Font(R.font.font_jbmono_medium, FontWeight.Medium),
)

private val SourceSans = FontFamily(
	Font(R.font.font_sourcesans3_regular, FontWeight.Normal),
	Font(R.font.font_sourcesans3_semibold, FontWeight.SemiBold),
)

private val CmuSerif = FontFamily(Font(R.font.font_cmu_bold, FontWeight.Bold))

/** Uppercase mono micro-label — the web's "text-xs uppercase tracking-wider". */
fun lhLabel(): TextStyle = TextStyle(
	fontFamily = JbMono,
	fontWeight = FontWeight.Normal,
	fontSize = 11.sp,
	letterSpacing = 0.8.sp,
)

private val LhTypography = Typography(
	titleLarge = TextStyle(fontFamily = CmuSerif, fontWeight = FontWeight.Bold, fontSize = 21.sp),
	titleMedium = TextStyle(fontFamily = SourceSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
	bodyLarge = TextStyle(fontFamily = SourceSans, fontSize = 16.sp, lineHeight = 24.sp),
	bodyMedium = TextStyle(fontFamily = SourceSans, fontSize = 14.sp, lineHeight = 20.sp),
	bodySmall = TextStyle(fontFamily = SourceSans, fontSize = 12.sp, lineHeight = 16.sp),
	labelSmall = lhLabel(),
	labelMedium = TextStyle(fontFamily = JbMono, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

private val LhDark = darkColorScheme(
	primary = LhColors.Foreground,
	onPrimary = LhColors.Background,
	secondary = LhColors.Card,
	onSecondary = LhColors.Foreground,
	background = LhColors.Background,
	onBackground = LhColors.Foreground,
	surface = LhColors.Card,
	onSurface = LhColors.Foreground,
	surfaceVariant = LhColors.Accent,
	onSurfaceVariant = LhColors.MutedForeground,
	error = LhColors.Destructive,
	outline = LhColors.Border,
)

@Composable
fun LearnHarnessTheme(content: @Composable () -> Unit) {
	MaterialTheme(colorScheme = LhDark, typography = LhTypography, content = content)
}
