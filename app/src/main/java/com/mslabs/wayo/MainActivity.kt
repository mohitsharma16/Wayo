package com.mslabs.wayo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.mslabs.wayo.ui.MainViewModel
import com.mslabs.wayo.ui.navigation.History
import com.mslabs.wayo.ui.navigation.Home
import com.mslabs.wayo.ui.screens.HistoryScreen
import com.mslabs.wayo.ui.screens.HomeScreen
import com.mslabs.wayo.ui.theme.WayoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Required for edge-to-edge, which is enforced with no opt-out
        // starting with apps targeting Android 16 (API 36) and higher.
        enableEdgeToEdge()

        setContent {
            WayoTheme {
                // Navigation 3: a plain, saveable back stack you own directly.
                val backStack = rememberNavBackStack(Home)

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        entry<Home> {
                            HomeScreen(
                                viewModel = viewModel,
                                onOpenHistory = { backStack.add(History) }
                            )
                        }
                        entry<History> {
                            HistoryScreen(
                                viewModel = viewModel,
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }
                    }
                )
            }
        }
    }
}
