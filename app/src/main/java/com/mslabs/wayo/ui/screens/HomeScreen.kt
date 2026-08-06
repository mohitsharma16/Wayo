package com.mslabs.wayo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mslabs.wayo.ui.MainViewModel
import com.mslabs.wayo.ui.NavigationState
import com.mslabs.wayo.ui.components.CompassDial
import com.mslabs.wayo.ui.components.PhotoThumbnail
import com.mslabs.wayo.ui.components.ProBadge
import com.mslabs.wayo.ui.components.ThemeToggleButton
import com.mslabs.wayo.util.PhotoUtils
import com.mslabs.wayo.util.VibrationUtils
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenHistory: () -> Unit
) {
    val context = LocalContext.current
    val activeSpot by viewModel.activeSpot.collectAsStateWithLifecycle()
    val navState by viewModel.navigationState.collectAsStateWithLifecycle()
    val isMarkingSpot by viewModel.isMarkingSpot.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    val activity = LocalActivity.current

    // --- Combined permission state ---
    // Both permissions are now explained and requested together, upfront,
    // instead of location-at-start / camera-only-when-tapped. Location is
    // required to use the app at all; camera stays optional (denying it
    // just means no photos, not a blocked app).
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission state above is only ever updated inside the permission-
    // launcher callbacks, so it goes stale the moment permission changes
    // outside of them -- most notably, the app's own "Open Settings"
    // recovery path: the user grants location in system Settings, returns
    // here via the back gesture (not a launcher result), and without this
    // the screen would stay stuck showing "Location access was denied"
    // forever. Rechecking on every resume covers that and the mirror case
    // (permission revoked while backgrounded).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasLocationPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Tracks the case Android gives no way to recover from via a normal
    // request: once a permission has been denied and the system will no
    // longer show the dialog at all (shouldShowRequestPermissionRationale
    // returns false AFTER a prior denial), the only path forward is the
    // app's system Settings screen. Without this, tapping "Allow" again
    // silently does nothing, which is exactly the bug being fixed here.
    var locationPermanentlyDenied by remember { mutableStateOf(false) }
    var cameraPermanentlyDenied by remember { mutableStateOf(false) }

    fun openAppSettings() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    var launchCameraAfterPermission by remember { mutableStateOf(false) }

    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    var noteText by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedPhotoPath = pendingPhotoFile?.absolutePath
        }
    }

    // Requests BOTH permissions together -- this is the upfront combined ask.
    val combinedPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasLocationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission
        hasCameraPermission = results[Manifest.permission.CAMERA] ?: hasCameraPermission

        if (!hasLocationPermission) {
            locationPermanentlyDenied = activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } ?: false
        }
        if (!hasCameraPermission) {
            cameraPermanentlyDenied = activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false
        }
    }

    // Used when camera is requested later on its own (e.g. user granted
    // location upfront but skipped camera, then taps "Add a photo").
    val cameraOnlyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            launchCameraAfterPermission = true
        } else {
            cameraPermanentlyDenied = activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false
        }
    }

    fun launchCamera() {
        val file = PhotoUtils.createImageFile(context)
        pendingPhotoFile = file
        cameraLauncher.launch(PhotoUtils.uriForFile(context, file))
    }

    fun requestPhoto() {
        when {
            hasCameraPermission -> launchCamera()
            cameraPermanentlyDenied -> openAppSettings()
            else -> cameraOnlyPermissionLauncher.launch(Manifest.permission.CAMERA)
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
                    if (isPro) {
                        ProBadge()
                        Spacer(Modifier.width(4.dp))
                    }
                    ThemeToggleButton(isDarkTheme = isDarkTheme, onToggle = { viewModel.toggleTheme() })
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
                // A very soft radial wash of the brand color behind the
                // content, instead of a single flat background color for
                // the whole screen -- the same trick AirTag/Find My-style
                // finder apps use so the screen reads as "crafted" rather
                // than a plain list-app background.
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                            MaterialTheme.colorScheme.background
                        ),
                        radius = 1200f
                    )
                )
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
                    HomeState.PERMISSION -> PermissionRationale(
                        permanentlyDenied = locationPermanentlyDenied,
                        onRequestPermission = {
                            combinedPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA)
                            )
                        },
                        onOpenSettings = { openAppSettings() }
                    )
                    HomeState.CAPTURE -> CaptureContent(
                        capturedPhotoPath = capturedPhotoPath,
                        noteText = noteText,
                        onNoteChange = { noteText = it },
                        isMarkingSpot = isMarkingSpot,
                        onTakePhoto = { requestPhoto() },
                        onRemovePhoto = { capturedPhotoPath = null },
                        onParkHere = {
                            viewModel.parkHere(
                                photoPath = capturedPhotoPath,
                                note = noteText.trim().ifBlank { null },
                                onSuccess = {
                                    capturedPhotoPath = null
                                    noteText = ""
                                },
                                onFailure = {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Couldn't get your location -- try again",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
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
private fun PermissionRationale(
    permanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
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

        if (!permanentlyDenied) {
            Text(
                "Wayo needs your location to remember where you left this. " +
                        "Camera access is optional, for attaching a photo to help you recognize the spot.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text("Continue", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Text(
                "Location access was denied. Wayo can't work without it -- " +
                        "enable it from Settings to continue.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onOpenSettings) {
                Text("Open Settings", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun CaptureContent(
    capturedPhotoPath: String?,
    noteText: String,
    onNoteChange: (String) -> Unit,
    isMarkingSpot: Boolean,
    onTakePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onParkHere: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        // A small branded hero instead of dropping straight into a form --
        // gives the first screen a focal point and some presence, instead
        // of starting cold with a text field.
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(18.dp))

        Text(
            "Never lose your spot",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Mark where you parked and Wayo will guide you straight back to it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        // Sets the right expectation upfront rather than letting people
        // discover GPS's real-world limits mid-use and read it as the app
        // being wrong -- phone GPS just can't pinpoint an exact spot, only
        // narrow things down to a nearby area.
        Text(
            "GPS gets you close, not exact -- expect the general area, not the precise spot.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add a note (optional)") },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    shape = MaterialTheme.shapes.medium
                )

                if (capturedPhotoPath != null) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            PhotoThumbnail(capturedPhotoPath, size = 60.dp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Photo attached", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Helps you recognize the spot later",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onRemovePhoto) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove photo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add a photo (optional)", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        Spacer(Modifier.height(36.dp))

        MarkSpotButton(isLoading = isMarkingSpot, onClick = onParkHere)

        if (capturedPhotoPath != null) {
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onTakePhoto) {
                Icon(Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Retake photo", style = MaterialTheme.typography.labelLarge)
            }
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
private fun MarkSpotButton(isLoading: Boolean, onClick: () -> Unit) {
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
                    indication = null,
                    enabled = !isLoading
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            } else {
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
}

@Composable
private fun CompassContent(
    navState: NavigationState,
    photoPath: String?,
    onFoundIt: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val arrowRotation = (navState.bearing - navState.heading + 360) % 360

    // Fire once, right when we cross into "arrived" -- not on every
    // recomposition while already arrived. A distinct double-pulse
    // vibration rather than the generic tap-click haptic used elsewhere,
    // since this is the one moment in the app worth calling out specially.
    var hasFiredArrivalHaptic by remember { mutableStateOf(false) }
    LaunchedEffect(navState.isArrived) {
        if (navState.isArrived && !hasFiredArrivalHaptic) {
            VibrationUtils.vibrateArrivalSuccess(context)
            hasFiredArrivalHaptic = true
        } else if (!navState.isArrived) {
            hasFiredArrivalHaptic = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
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
                if (navState.isArrived) {
                    ArrivedContent(arrivalRadiusMeters = navState.arrivalRadiusMeters)
                } else {
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
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "±${navState.gpsAccuracyMeters.toInt()}m GPS accuracy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
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
                if (navState.usingGpsHeadingFallback && !navState.isArrived) {
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

/**
 * Shown once distance settles within the arrival radius -- which factors in
 * BOTH live GPS accuracy and however accurate the fix was when the spot was
 * originally marked (see MainViewModel's arrivalRadius calculation). Showing
 * that actual number here, not just live accuracy, is what makes it legible
 * when this radius is wider than expected instead of it just looking stuck.
 */
@Composable
private fun ArrivedContent(arrivalRadiusMeters: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "arrivedPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrivedPulseScale"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(pulseScale)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(44.dp)
            )
        }
    }

    Spacer(Modifier.height(20.dp))

    Text(
        "You're here",
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Text(
        "Within reach of your marked spot",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "You're within about ${arrivalRadiusMeters.toInt()}m -- look around this area",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
    )
}

private fun formatDistance(meters: Float): String {
    return if (meters < 1000) "${meters.toInt()}m"
    else String.format(Locale.getDefault(), "%.1fkm", meters / 1000)
}