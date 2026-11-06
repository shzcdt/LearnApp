package ai.learnharness.mobile.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Port of the server UI's timeAgo (components/sidebar.tsx): "just now",
 * "5m ago", "3h ago", "2d ago", then ISO date. Pure.
 */
fun timeAgo(iso: String?, now: Long = System.currentTimeMillis()): String {
	if (iso.isNullOrBlank()) return ""
	val ts = parseIso(iso) ?: return ""
	val ms = now - ts
	val min = ms / 60_000
	val h = ms / 3_600_000
	val d = ms / 86_400_000
	return when {
		ms < 60_000 -> "just now"
		min < 60 -> "${min}m ago"
		h < 24 -> "${h}h ago"
		d < 7 -> "${d}d ago"
		else -> SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(ts))
	}
}

private fun parseIso(iso: String): Long? = runCatching {
	// ISO-8601 with Z or offset; SimpleDateFormat handles the common shape
	javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(iso).toGregorianCalendar().timeInMillis
}.getOrNull()

/** Web's session meta: state first, then the age. Pure. */
fun sessionMeta(
	busy: Boolean,
	compacting: Boolean,
	pendingQuestion: Boolean,
	pendingProblem: Boolean,
	kind: String,
	updatedAt: String?,
): String = when {
	compacting -> "Compacting context · ${timeAgo(updatedAt)}"
	pendingQuestion -> "Waiting for input · ${timeAgo(updatedAt)}"
	kind == "practice" && pendingProblem -> "Problem waiting · ${timeAgo(updatedAt)}"
	busy -> "Working…"
	else -> timeAgo(updatedAt)
}
