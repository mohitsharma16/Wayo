package com.mslabs.wayo.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 routes. Each must implement NavKey and be @Serializable
 * so rememberNavBackStack can persist the back stack across process death.
 */
@Serializable
data object Home : NavKey

@Serializable
data object History : NavKey
