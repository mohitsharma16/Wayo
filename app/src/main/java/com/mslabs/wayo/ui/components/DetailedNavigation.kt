package com.mslabs.wayo.ui.components

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mslabs.wayo.data.ParkingSpot
import com.mslabs.wayo.ui.MainViewModel
import com.mslabs.wayo.util.MapsLauncher

/** Starts the Play purchase flow, with the same feedback everywhere it's offered. */
@Composable
fun rememberUnlockAction(viewModel: MainViewModel): () -> Unit {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    return {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        val launched = activity?.let { viewModel.billingManager.launchPurchase(it) } ?: false
        if (!launched) {
            Toast.makeText(context, "Store isn't ready yet -- try again in a moment", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Returns the action behind every "Get detailed navigation" button: opens
 * maps with directions to the spot if the user is Pro or hasn't used their
 * one free try yet; otherwise shows the unlock prompt. Also emits that
 * prompt dialog, so callers just invoke the returned lambda.
 */
@Composable
fun rememberDetailedNavigation(viewModel: MainViewModel): (ParkingSpot) -> Unit {
    val context = LocalContext.current
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val unlock = rememberUnlockAction(viewModel)
    var showUpsell by remember { mutableStateOf(false) }

    // Buying from the prompt should dismiss it, not leave it up over a now-unlocked app.
    LaunchedEffect(isPro) { if (isPro) showUpsell = false }

    if (showUpsell) {
        AlertDialog(
            onDismissRequest = { showUpsell = false },
            title = { Text("Detailed navigation is a Pro feature") },
            text = {
                Text(
                    "You've used your free detailed navigation. Unlock full access for unlimited " +
                        "turn-by-turn directions to any spot, plus your full history. " +
                        "One-time purchase, no subscription."
                )
            },
            confirmButton = { TextButton(onClick = unlock) { Text("Unlock full access") } },
            dismissButton = { TextButton(onClick = { showUpsell = false }) { Text("Not now") } }
        )
    }

    return { spot ->
        if (viewModel.canUseDetailedNavigation()) {
            if (MapsLauncher.openWalkingDirections(context, spot.latitude, spot.longitude)) {
                viewModel.recordDetailedNavigationUse()
            } else {
                Toast.makeText(context, "No maps app found on this device", Toast.LENGTH_SHORT).show()
            }
        } else {
            showUpsell = true
        }
    }
}
