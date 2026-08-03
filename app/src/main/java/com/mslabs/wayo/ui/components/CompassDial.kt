package com.mslabs.wayo.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * A soft, minimal "radar ping + arrow" visual -- deliberately closer to the
 * clean, uncluttered feel of AirTag's Precision Finding UI than the earlier
 * tick-marked instrument-panel look. Two continuously pulsing rings suggest
 * an active search without needing literal cardinal markings, and the arrow
 * itself uses spring physics for a slightly organic, non-mechanical motion.
 *
 * Note: this is a visual style choice, not a precision upgrade -- the
 * underlying accuracy is still whatever GPS (and the compass sensor, where
 * available) can provide. See MainViewModel's NavigationState.isArrived for
 * how the "you've arrived" moment is actually decided.
 */
@Composable
fun CompassDial(
    rotationDegrees: Float,
    modifier: Modifier = Modifier
) {
    val animatedRotation by animateFloatAsState(
        targetValue = rotationDegrees,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "needleRotation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "radarPing")
    val pingProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pingProgress"
    )

    val accentColor = MaterialTheme.colorScheme.primary
    val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Soft static backdrop circle
        drawCircle(color = ringColor, radius = radius * 0.9f, center = center)

        // A single expanding-and-fading ring, looping continuously --
        // reads as "actively searching" without needing tick marks or
        // labels, similar in spirit to AirTag's radar animation.
        drawCircle(
            color = accentColor.copy(alpha = (1f - pingProgress) * 0.35f),
            radius = radius * (0.35f + 0.55f * pingProgress),
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )

        rotate(degrees = animatedRotation, pivot = center) {
            val tip = Offset(center.x, center.y - radius * 0.62f)

            // Soft glow behind the arrow tip
            listOf(0.3f to 16.dp, 0.18f to 11.dp, 0.09f to 6.dp).forEach { (alpha, r) ->
                drawCircle(
                    color = accentColor.copy(alpha = alpha),
                    radius = r.toPx(),
                    center = tip
                )
            }

            val arrowPath = Path().apply {
                moveTo(center.x, center.y - radius * 0.62f)
                lineTo(center.x - radius * 0.24f, center.y + radius * 0.1f)
                lineTo(center.x, center.y - radius * 0.02f)
                lineTo(center.x + radius * 0.24f, center.y + radius * 0.1f)
                close()
            }
            drawPath(path = arrowPath, color = accentColor)
        }

        // Center hub
        drawCircle(
            color = surfaceColor,
            radius = radius * 0.1f,
            center = center
        )
        drawCircle(
            color = accentColor,
            radius = radius * 0.1f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}
