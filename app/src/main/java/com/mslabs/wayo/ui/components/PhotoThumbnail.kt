package com.mslabs.wayo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mslabs.wayo.util.PhotoUtils

/** Square thumbnail. Pass [onClick] to make it open the full-size viewer (or anything else). */
@Composable
fun PhotoThumbnail(path: String, size: Dp = 120.dp, onClick: (() -> Unit)? = null) {
    PhotoImage(
        path = path,
        modifier = Modifier.size(size),
        iconSize = size / 3,
        onClick = onClick
    )
}

/** Cropped photo at whatever size [modifier] gives it -- used for the larger preview on detail screens. */
@Composable
fun PhotoPreview(path: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    PhotoImage(path = path, modifier = modifier, iconSize = 40.dp, onClick = onClick)
}

@Composable
private fun PhotoImage(path: String, modifier: Modifier, iconSize: Dp, onClick: (() -> Unit)?) {
    val bitmap = remember(path) { PhotoUtils.decodeRotatedBitmap(path)?.asImageBitmap() }
    val clickable = if (onClick != null && bitmap != null) Modifier.clickable(onClick = onClick) else Modifier
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Parking spot photo",
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .then(clickable),
            contentScale = ContentScale.Crop
        )
    } else {
        // Missing/corrupt file on disk (e.g. cleared by the OS, or storage
        // issues) -- without this, the spot showed a blank gap where the
        // photo card should be, with nothing telling the user why.
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.BrokenImage,
                contentDescription = "Photo unavailable",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/** Full-screen, uncropped photo with pinch-to-zoom and pan. Dismiss with the X or the back gesture. */
@Composable
fun PhotoViewerDialog(path: String, onDismiss: () -> Unit) {
    val bitmap = remember(path) { PhotoUtils.decodeRotatedBitmap(path)?.asImageBitmap() }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Parking spot photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset = if (scale == 1f) Offset.Zero else offset + pan
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            } else {
                Icon(
                    Icons.Default.BrokenImage,
                    contentDescription = "Photo unavailable",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(64.dp)
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close photo", tint = Color.White)
            }
        }
    }
}
