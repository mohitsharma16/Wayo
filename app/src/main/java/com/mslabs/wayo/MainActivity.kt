package com.mslabs.wayo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.mslabs.wayo.ui.MainViewModel
import com.mslabs.wayo.ui.navigation.History
import com.mslabs.wayo.ui.navigation.Home
import com.mslabs.wayo.ui.navigation.SpotDetail
import com.mslabs.wayo.ui.screens.SpotDetailScreen
import com.mslabs.wayo.ui.screens.HistoryScreen
import com.mslabs.wayo.ui.screens.HomeScreen
import com.mslabs.wayo.ui.theme.WayoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() per the SplashScreen API contract.
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // installSplashScreen() dismisses the splash the instant this
        // Activity's first frame is drawn. For a lightweight Compose screen
        // like this one, that first frame arrives almost immediately --
        // fast enough that the splash icon could flash for a single frame
        // and never actually register with the user, which reads as "the
        // splash never shows, just black." Holding it for a short, fixed
        // minimum makes it something people actually perceive.
        var keepSplashScreenOn = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreenOn }
        lifecycleScope.launch {
            delay(500)
            keepSplashScreenOn = false
        }

        // Required for edge-to-edge, which is enforced with no opt-out
        // starting with apps targeting Android 16 (API 36) and higher.
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            WayoTheme(darkTheme = isDarkTheme) {
                // Navigation 3: a plain, saveable back stack you own directly.
                val backStack = rememberNavBackStack(Home)

                // One-time confirmation right when a purchase actually
                // completes, from whichever screen it was started on --
                // otherwise the only signal that payment went through is
                // the UI silently unlocking.
                val haptics = LocalHapticFeedback.current
                LaunchedEffect(Unit) {
                    viewModel.billingManager.purchaseCompleted.collect {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        Toast.makeText(
                            this@MainActivity,
                            "Full access unlocked -- thank you!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

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
                                onBack = { backStack.removeLastOrNull() },
                                onOpenSpot = { backStack.add(SpotDetail(it.id)) }
                            )
                        }
                        entry<SpotDetail> { key ->
                            SpotDetailScreen(
                                viewModel = viewModel,
                                spotId = key.spotId,
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }
                    }
                )
            }
        }
    }
}
