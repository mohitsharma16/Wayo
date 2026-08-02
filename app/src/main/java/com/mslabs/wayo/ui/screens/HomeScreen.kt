package com.mslabs.wayo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mslabs.wayo.ui.MainViewModel
import com.mslabs.wayo.ui.NavigationState
import com.mslabs.wayo.ui.components.CompassDial
import com.mslabs.wayo.ui.components.PhotoThumbnail
import com.mslabs.wayo.util.PhotoUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenHistory: () -> Unit
) {
    val context = LocalContext.current
    val activeSpot by viewModel.activeSpot.collectAsStateWithLifecycle()
    val navState by viewModel.navigationState.collectAsStateWithLifecycle()

    // --- Location permission state ---
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    // --- Camera permission state ---
    // This was the missing piece that caused the crash: CAMERA is a dangerous
    // permission and must be requested at runtime, same as location. Declaring
    // it in the manifest alone is not enough on API 23+.
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var launchCameraAfterPermission by remember { mutableStateOf(false) }

    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedPhotoPath = pendingPhotoFile?.absolutePath
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) launchCameraAfterPermission = true
    }

    fun launchCamera() {
        val file = PhotoUtils.createImageFile(context)
        pendingPhotoFile = file
        cameraLauncher.launch(PhotoUtils.uriForFile(context, file))
    }

    fun requestPhoto() {
        if (hasCameraPermission) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(launchCameraAfterPermission) {
        if (launchCameraAfterPermission) {
            launchCamera()
            launchCameraAfterPermission = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Wayo", style = MaterialTheme.typography.titleLarge)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = when {
                    !hasLocationPermission -> HomeState.PERMISSION
                    activeSpot == null -> HomeState.CAPTURE
                    else -> HomeState.NAVIGATE
                },
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                label = "homeState"
            ) { state ->
                when (state) {
                    HomeState.PERMISSION -> PermissionRationale {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    HomeState.CAPTURE -> CaptureContent(
                        capturedPhotoPath = capturedPhotoPath,
                        onTakePhoto = { requestPhoto() },
                        onRemovePhoto = { capturedPhotoPath = null },
                        onParkHere = {
                            viewModel.parkHere(photoPath = capturedPhotoPath, note = null)
                            capturedPhotoPath = null
                        }
                    )
                    HomeState.NAVIGATE -> CompassContent(
                        navState = navState,
                        photoPath = activeSpot?.photoPath,
                        onFoundIt = { viewModel.foundCar() }
                    )
                }
            }
        }
    }
}

private enum class HomeState { PERMISSION, CAPTURE, NAVIGATE }

@Composable
private fun PermissionRationale(onRequestPermission: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Wayo needs your location to remember where you left this.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        FilledTonalButton(onClick = onRequestPermission) {
            Text("Allow location access", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CaptureContent(
    capturedPhotoPath: String?,
    onTakePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onParkHere: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            "Find your way back.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        if (capturedPhotoPath != null) {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(shape = MaterialTheme.shapes.medium) {
                        PhotoThumbnail(capturedPhotoPath, size = 72.dp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Photo attached", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Helps you recognize the spot later",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onRemovePhoto) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove photo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        MarkSpotButton(onClick = onParkHere)

        Spacer(Modifier.height(28.dp))

        TextButton(onClick = onTakePhoto) {
            Icon(Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (capturedPhotoPath == null) "Add a photo (optional)" else "Retake photo",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Solid primary-color button -- deliberately NOT a gradient anymore. The
 * earlier teal-to-coral gradient looked like a one-off decision disconnected
 * from the rest of the app's palette. Every primary action across the app
 * (this button, the permission button, the unlock button, "Found it") now
 * draws from the same MaterialTheme.colorScheme, so the app reads as one
 * consistent product instead of a different color choice per screen.
 */
@Composable
private fun MarkSpotButton(onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "parkPulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(pulseScale)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(168.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = MaterialTheme.colorScheme.primary,
                    spotColor = MaterialTheme.colorScheme.primary
                )
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Mark this spot",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun CompassContent(
    navState: NavigationState,
    photoPath: String?,
    onFoundIt: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val arrowRotation = (navState.bearing - navState.heading + 360) % 360

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .fillMaxWidth()
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // The fix: without fillMaxWidth() here, this Column has
                // nothing to center against inside the full-width Card above
                // it, so it just wraps its content and sits at the left edge
                // -- which is exactly the "card centered, contents left-
                // aligned" bug.
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
            ) {
                CompassDial(
                    rotationDegrees = arrowRotation,
                    modifier = Modifier.size(200.dp)
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    formatDistance(navState.distanceMeters),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "back to your spot",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (navState.isGpsWeak) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "GPS signal is weak -- try moving to a more open area",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                if (navState.compassNeedsCalibration) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Compass needs calibration -- wave your phone in a figure-8 motion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                if (navState.usingGpsHeadingFallback) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This device has no compass sensor -- direction updates as you walk",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        photoPath?.let {
            Spacer(Modifier.height(20.dp))
            Card(shape = MaterialTheme.shapes.large) {
                PhotoThumbnail(it, size = 88.dp)
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onFoundIt()
            }
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Found it", style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun formatDistance(meters: Float): String {
    return if (meters < 1000) "${meters.toInt()}m"
    else String.format("%.1fkm", meters / 1000)
}
