package ai.learnharness.mobile

import ai.learnharness.mobile.ui.chat.ChatScreen
import ai.learnharness.mobile.ui.sessions.SessionsScreen
import ai.learnharness.mobile.ui.settings.SettingsScreen
import ai.learnharness.mobile.ui.theme.LearnHarnessTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			LearnHarnessTheme {
				// configure from DataStore once, then keep the notification bus alive
				androidx.compose.runtime.LaunchedEffect(Unit) {
					runCatching { ServiceLocator.repository.ensureConfigured() }
					ServiceLocator.events.start()
				}
				val nav = rememberNavController()
				NavHost(navController = nav, startDestination = "sessions") {
					composable("sessions") {
						SessionsScreen(
							onOpenChat = { id -> nav.navigate("chat/$id") },
							onOpenSettings = { nav.navigate("settings") },
						)
					}
					composable(
						route = "chat/{chatId}",
						arguments = listOf(navArgument("chatId") { }),
					) { entry ->
						val chatId = entry.arguments?.getString("chatId") ?: return@composable
						ChatScreen(
							chatId = chatId,
							onBack = { nav.popBackStack() },
						)
					}
					composable("settings") {
						SettingsScreen(
							onBack = { nav.popBackStack() },
							onOpenKeys = { nav.navigate("keys") },
						)
					}
					composable("keys") {
						ai.learnharness.mobile.ui.keys.KeysScreen(onBack = { nav.popBackStack() })
					}
				}
			}
		}
	}
}
