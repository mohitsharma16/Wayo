package com.mslabs.wayo.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * A hand-drawn compass dial: tick marks every 30 degrees (cardinal points
 * emphasized), a glowing gradient needle, and a soft center hub. Replaces a
 * plain rotated stock icon with something that actually reads as an
 * instrument, since this screen carries the whole product.
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

    val ringColor = MaterialTheme.colorScheme.outlineVariant
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = ringColor,
            radius = radius * 0.95f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        for (angle in 0 until 360 step 30) {
            val rad = Math.toRadians(angle.toDouble())
            val isCardinal = angle % 90 == 0
            val tickLength = if (isCardinal) radius * 0.14f else radius * 0.07f

            val outer = radius * 0.82f
            val start = Offset(
                center.x + outer * cos(rad).toFloat(),
                center.y + outer * sin(rad).toFloat()
            )
            val end = Offset(
                center.x + (outer - tickLength) * cos(rad).toFloat(),
                center.y + (outer - tickLength) * sin(rad).toFloat()
            )

            drawLine(
                color = if (isCardinal) onSurface else tickColor,
                start = start,
                end = end,
                strokeWidth = if (isCardinal) 3.dp.toPx() else 1.5.dp.toPx()
            )
        }

        rotate(degrees = animatedRotation, pivot = center) {
            val tip = Offset(center.x, center.y - radius * 0.72f)

            // Soft glow behind the needle tip
            listOf(0.35f to 14.dp, 0.2f to 10.dp, 0.1f to 6.dp).forEach { (alpha, r) ->
                drawCircle(
                    color = accentColor.copy(alpha = alpha),
                    radius = r.toPx(),
                    center = tip
                )
            }

            val needlePath = Path().apply {
                moveTo(center.x, center.y - radius * 0.72f)
                lineTo(center.x - radius * 0.055f, center.y + radius * 0.08f)
                lineTo(center.x, center.y)
                lineTo(center.x + radius * 0.055f, center.y + radius * 0.08f)
                close()
            }
            drawPath(path = needlePath, color = accentColor)
        }

        drawCircle(
            color = onSurface.copy(alpha = 0.08f),
            radius = radius * 0.15f,
            center = center
        )
        drawCircle(
            color = onSurface.copy(alpha = 0.2f),
            radius = radius * 0.15f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
