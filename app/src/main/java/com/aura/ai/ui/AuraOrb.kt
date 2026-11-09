
package com.aura.ai.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.aura.ai.voice.AuraVoiceState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AuraOrb(
    state: AuraVoiceState,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(
        label = "aura_liquid_motion"
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AuraVoiceState.LISTENING -> 4200
                    AuraVoiceState.PROCESSING -> 2600
                    AuraVoiceState.SPEAKING -> 3200
                    AuraVoiceState.IDLE -> 11000
                }
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = when (state) {
            AuraVoiceState.IDLE -> 1.025f
            AuraVoiceState.LISTENING -> 1.075f
            AuraVoiceState.PROCESSING -> 1.045f
            AuraVoiceState.SPEAKING -> 1.065f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AuraVoiceState.IDLE -> 2800
                    AuraVoiceState.LISTENING -> 850
                    AuraVoiceState.PROCESSING -> 1100
                    AuraVoiceState.SPEAKING -> 650
                }
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    val drift by transition.animateFloat(
        initialValue = -0.035f,
        targetValue = 0.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3600
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_drift"
    )

    val cyan = Color(0xFF72E9FF)
    val blue = Color(0xFF6389FF)
    val violet = Color(0xFF9C72FF)

    val primaryColor = when (state) {
        AuraVoiceState.IDLE -> blue
        AuraVoiceState.LISTENING -> cyan
        AuraVoiceState.PROCESSING -> violet
        AuraVoiceState.SPEAKING -> Color(0xFF71CFFF)
    }

    Box(
        modifier = modifier.size(310.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(
                size.width / 2f,
                size.height / 2f
            )

            val radius = size.minDimension * 0.355f

            // Ambient light surrounding the orb.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.22f),
                        primaryColor.copy(alpha = 0.09f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 2.05f
                ),
                radius = radius * 2.05f,
                center = center
            )

            // Outer translucent halo.
            drawCircle(
                color = primaryColor.copy(alpha = 0.045f),
                radius = radius * 1.65f,
                center = center
            )

            drawCircle(
                color = primaryColor.copy(alpha = 0.075f),
                radius = radius * 1.43f,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Moving orbital arcs.
            drawArc(
                color = cyan.copy(alpha = 0.30f),
                startAngle = rotation,
                sweepAngle = 105f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius * 1.49f,
                    center.y - radius * 1.49f
                ),
                size = Size(
                    radius * 2.98f,
                    radius * 2.98f
                ),
                style = Stroke(width = 1.7.dp.toPx())
            )

            drawArc(
                color = violet.copy(alpha = 0.24f),
                startAngle = -rotation * 0.72f + 180f,
                sweepAngle = 72f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius * 1.57f,
                    center.y - radius * 1.57f
                ),
                size = Size(
                    radius * 3.14f,
                    radius * 3.14f
                ),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Main glass sphere.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF2FFFF),
                        Color(0xFFB7F5FF),
                        Color(0xFF72DFFF),
                        primaryColor,
                        Color(0xFF5453B7),
                        Color(0xFF24254F)
                    ),
                    center = Offset(
                        center.x - radius * 0.33f + drift * size.width,
                        center.y - radius * 0.43f
                    ),
                    radius = radius * 1.72f
                ),
                radius = radius,
                center = center
            )

            // Secondary moving light inside the glass.
            val lightAngle = Math.toRadians(
                rotation.toDouble()
            )

            val lightCenter = Offset(
                center.x + cos(lightAngle).toFloat() * radius * 0.48f,
                center.y + sin(lightAngle).toFloat() * radius * 0.38f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        cyan.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = lightCenter,
                    radius = radius * 1.15f
                ),
                radius = radius,
                center = center
            )

            // Glass reflection.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.78f),
                        Color.White.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(
                        center.x - radius * 0.38f,
                        center.y - radius * 0.48f
                    ),
                    radius = radius * 0.83f
                ),
                radius = radius,
                center = center
            )

            // Curved translucent reflections.
            val reflection = Path().apply {
                moveTo(
                    center.x - radius * 0.72f,
                    center.y - radius * 0.43f
                )
                cubicTo(
                    center.x - radius * 0.43f,
                    center.y - radius * 0.94f,
                    center.x + radius * 0.25f,
                    center.y - radius * 0.92f,
                    center.x + radius * 0.63f,
                    center.y - radius * 0.58f
                )
            }

            drawPath(
                path = reflection,
                color = Color.White.copy(alpha = 0.19f),
                style = Stroke(
                    width = 2.4.dp.toPx()
                )
            )

            // Subtle lower glass contour.
            val lowerReflection = Path().apply {
                moveTo(
                    center.x - radius * 0.72f,
                    center.y + radius * 0.49f
                )
                cubicTo(
                    center.x - radius * 0.24f,
                    center.y + radius * 0.99f,
                    center.x + radius * 0.42f,
                    center.y + radius * 0.83f,
                    center.x + radius * 0.76f,
                    center.y + radius * 0.35f
                )
            }

            drawPath(
                path = lowerReflection,
                color = Color.White.copy(alpha = 0.10f),
                style = Stroke(
                    width = 1.5.dp.toPx()
                )
            )
        }

        // Organic scale animation.
        Box(
            modifier = Modifier
                .size(310.dp)
                .scale(pulse)
        )
    }
}
