
package com.aura.ai.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
        label = "aura_orb"
    )

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AuraVoiceState.IDLE -> 10000
                    AuraVoiceState.LISTENING -> 4000
                    AuraVoiceState.PROCESSING -> 2500
                    AuraVoiceState.SPEAKING -> 3000
                }
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = when (state) {
            AuraVoiceState.IDLE -> 1.025f
            AuraVoiceState.LISTENING -> 1.07f
            AuraVoiceState.PROCESSING -> 1.045f
            AuraVoiceState.SPEAKING -> 1.075f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AuraVoiceState.IDLE -> 3000
                    AuraVoiceState.LISTENING -> 850
                    AuraVoiceState.PROCESSING -> 1200
                    AuraVoiceState.SPEAKING -> 700
                }
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val fluid by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AuraVoiceState.IDLE -> 5000
                    AuraVoiceState.LISTENING -> 1800
                    AuraVoiceState.PROCESSING -> 1300
                    AuraVoiceState.SPEAKING -> 1000
                }
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fluid"
    )

    Canvas(
        modifier = modifier
            .size(330.dp)
            .scale(pulse)
    ) {
        val center = Offset(
            size.width / 2f,
            size.height / 2f
        )

        val radius = size.minDimension * 0.335f

        val cyan = Color(0xFF39EFFF)
        val blue = Color(0xFF318BFF)
        val violet = Color(0xFFAD55FF)
        val magenta = Color(0xFFFF398F)
        val teal = Color(0xFF18E8C0)

        val angle = Math.toRadians(phase.toDouble())
        val fluidOffset = fluid * radius * 0.10f

        val intensity = when (state) {
            AuraVoiceState.IDLE -> 0.65f
            AuraVoiceState.LISTENING -> 1f
            AuraVoiceState.PROCESSING -> 0.85f
            AuraVoiceState.SPEAKING -> 1f
        }

        // 1. Ambient light
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    violet.copy(alpha = 0.20f),
                    magenta.copy(alpha = 0.10f),
                    cyan.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 2f
            ),
            radius = radius * 2f,
            center = center
        )

        // 2. Exterior glass rings
        val outerRadius = radius * 1.30f

        drawCircle(
            color = violet.copy(alpha = 0.08f),
            radius = outerRadius,
            center = center,
            style = Stroke(width = 9.dp.toPx())
        )

        drawCircle(
            color = cyan.copy(alpha = 0.22f),
            radius = outerRadius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        drawCircle(
            color = violet.copy(alpha = 0.22f),
            radius = radius * 1.17f,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )

        drawCircle(
            color = magenta.copy(alpha = 0.19f),
            radius = radius * 1.08f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // 3. Moving light around the outer ring
        val orbitPoint = Offset(
            center.x + cos(angle).toFloat() * outerRadius,
            center.y + sin(angle).toFloat() * outerRadius
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    cyan.copy(alpha = 0.65f),
                    Color.Transparent
                ),
                center = orbitPoint,
                radius = radius * 0.32f
            ),
            radius = radius * 0.32f,
            center = orbitPoint
        )

        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    cyan.copy(alpha = 0.05f),
                    cyan,
                    violet,
                    magenta,
                    Color(0xFFFFD68A),
                    cyan.copy(alpha = 0.05f)
                ),
                center = center
            ),
            startAngle = phase,
            sweepAngle = 115f,
            useCenter = false,
            topLeft = Offset(
                center.x - outerRadius,
                center.y - outerRadius
            ),
            size = Size(
                outerRadius * 2f,
                outerRadius * 2f
            ),
            style = Stroke(width = 2.5.dp.toPx())
        )

        drawArc(
            color = violet.copy(alpha = 0.65f),
            startAngle = -phase * 0.75f + 180f,
            sweepAngle = 85f,
            useCenter = false,
            topLeft = Offset(
                center.x - radius * 1.17f,
                center.y - radius * 1.17f
            ),
            size = Size(
                radius * 2.34f,
                radius * 2.34f
            ),
            style = Stroke(width = 2.dp.toPx())
        )

        // 4. Main glass sphere
        val spherePath = Path().apply {
            addOval(
                Rect(
                    center.x - radius,
                    center.y - radius,
                    center.x + radius,
                    center.y + radius
                )
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF46366F),
                    Color(0xFF242044),
                    Color(0xFF100F28),
                    Color(0xFF080D23)
                ),
                center = Offset(
                    center.x - radius * 0.2f,
                    center.y - radius * 0.3f
                ),
                radius = radius * 1.7f
            ),
            radius = radius,
            center = center
        )

        clipPath(spherePath) {

            // Inner atmosphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        violet.copy(alpha = 0.48f),
                        magenta.copy(alpha = 0.23f),
                        Color.Transparent
                    ),
                    center = Offset(
                        center.x,
                        center.y - radius * 0.2f
                    ),
                    radius = radius * 1.35f
                ),
                radius = radius,
                center = center
            )

            // 5. Magenta liquid shape
            val pinkPath = Path().apply {
                moveTo(
                    center.x - radius * 0.48f + fluidOffset,
                    center.y - radius * 0.12f
                )

                cubicTo(
                    center.x - radius * 0.65f,
                    center.y - radius * 0.85f,
                    center.x + radius * 0.10f,
                    center.y - radius * 0.95f + fluidOffset,
                    center.x + radius * 0.52f,
                    center.y - radius * 0.45f
                )

                cubicTo(
                    center.x + radius * 0.90f,
                    center.y - radius * 0.05f,
                    center.x + radius * 0.08f,
                    center.y + radius * 0.12f,
                    center.x - radius * 0.48f + fluidOffset,
                    center.y - radius * 0.12f
                )

                close()
            }

            drawPath(
                path = pinkPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF287C).copy(alpha = 0.95f),
                        magenta.copy(alpha = 0.75f),
                        violet.copy(alpha = 0.55f)
                    ),
                    start = Offset(
                        center.x,
                        center.y - radius
                    ),
                    end = Offset(
                        center.x + radius,
                        center.y + radius
                    )
                )
            )

            // 6. Cyan liquid shape
            val cyanPath = Path().apply {
                moveTo(
                    center.x - radius * 0.12f,
                    center.y - radius * 0.30f
                )

                cubicTo(
                    center.x - radius * 0.95f,
                    center.y - radius * 0.72f + fluidOffset,
                    center.x - radius * 0.95f,
                    center.y + radius * 0.20f,
                    center.x - radius * 0.40f,
                    center.y + radius * 0.72f
                )

                cubicTo(
                    center.x + radius * 0.05f,
                    center.y + radius * 1.02f,
                    center.x + radius * 0.45f,
                    center.y + radius * 0.65f,
                    center.x + radius * 0.25f,
                    center.y + radius * 0.15f
                )

                cubicTo(
                    center.x + radius * 0.12f,
                    center.y - radius * 0.12f,
                    center.x - radius * 0.12f,
                    center.y - radius * 0.30f,
                    center.x - radius * 0.12f,
                    center.y - radius * 0.30f
                )

                close()
            }

            drawPath(
                path = cyanPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        cyan.copy(alpha = 0.90f),
                        blue.copy(alpha = 0.83f),
                        violet.copy(alpha = 0.48f)
                    ),
                    start = Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    end = Offset(
                        center.x + radius,
                        center.y + radius
                    )
                )
            )

            // 7. Turquoise liquid shape
            val tealPath = Path().apply {
                moveTo(
                    center.x - radius * 0.15f,
                    center.y - radius * 0.10f
                )

                cubicTo(
                    center.x + radius * 0.38f,
                    center.y - radius * 0.65f,
                    center.x + radius * 0.92f,
                    center.y - radius * 0.10f,
                    center.x + radius * 0.90f,
                    center.y + radius * 0.15f
                )

                cubicTo(
                    center.x + radius * 0.65f,
                    center.y + radius * 0.45f,
                    center.x + radius * 0.25f,
                    center.y + radius * 0.25f,
                    center.x - radius * 0.15f,
                    center.y - radius * 0.10f
                )

                close()
            }

            drawPath(
                path = tealPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF8DFFF1).copy(alpha = 0.85f),
                        teal.copy(alpha = 0.80f),
                        cyan.copy(alpha = 0.38f)
                    ),
                    start = Offset(
                        center.x,
                        center.y - radius
                    ),
                    end = Offset(
                        center.x + radius,
                        center.y + radius
                    )
                )
            )

            // 8. Violet translucent shape
            val violetPath = Path().apply {
                moveTo(
                    center.x - radius * 0.30f,
                    center.y - radius * 0.15f
                )

                cubicTo(
                    center.x - radius * 0.10f,
                    center.y - radius * 0.72f,
                    center.x + radius * 0.62f,
                    center.y - radius * 0.15f,
                    center.x + radius * 0.40f,
                    center.y + radius * 0.65f
                )

                cubicTo(
                    center.x + radius * 0.10f,
                    center.y + radius * 0.95f,
                    center.x - radius * 0.50f,
                    center.y + radius * 0.65f,
                    center.x - radius * 0.30f,
                    center.y - radius * 0.15f
                )

                close()
            }

            drawPath(
                path = violetPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFB4FF).copy(alpha = 0.52f),
                        violet.copy(alpha = 0.68f),
                        blue.copy(alpha = 0.48f)
                    ),
                    start = Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    end = Offset(
                        center.x + radius,
                        center.y + radius
                    )
                )
            )

            // 9. Moving inner light
            val lightPoint = Offset(
                center.x + cos(angle).toFloat() * radius * 0.25f,
                center.y + sin(angle).toFloat() * radius * 0.22f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.98f),
                        Color(0xFFBDFBFF).copy(alpha = 0.75f),
                        cyan.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = lightPoint,
                    radius = radius * 0.72f
                ),
                radius = radius * 0.72f,
                center = lightPoint
            )

            // Central white glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFF2FFFF).copy(alpha = 0.90f),
                        cyan.copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.42f
                ),
                radius = radius * 0.42f,
                center = center
            )

            // 10. Glass reflection
            val reflection = Path().apply {
                moveTo(
                    center.x - radius * 0.70f,
                    center.y - radius * 0.45f
                )

                cubicTo(
                    center.x - radius * 0.35f,
                    center.y - radius * 0.95f,
                    center.x + radius * 0.30f,
                    center.y - radius * 0.88f,
                    center.x + radius * 0.65f,
                    center.y - radius * 0.48f
                )
            }

            drawPath(
                path = reflection,
                color = Color.White.copy(alpha = 0.25f),
                style = Stroke(width = 2.dp.toPx())
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(
                        center.x - radius * 0.42f,
                        center.y - radius * 0.55f
                    ),
                    radius = radius * 0.80f
                ),
                radius = radius,
                center = center
            )
        }

        // 11. Outer glass contour
        drawCircle(
            color = Color.White.copy(alpha = 0.13f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.2.dp.toPx())
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryGlow(state).copy(
                        alpha = 0.12f * intensity
                    ),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 1.25f
            ),
            radius = radius * 1.25f,
            center = center
        )
    }
}

private fun primaryGlow(state: AuraVoiceState): Color {
    return when (state) {
        AuraVoiceState.IDLE -> Color(0xFFB36BFF)
        AuraVoiceState.LISTENING -> Color(0xFF36EFFF)
        AuraVoiceState.PROCESSING -> Color(0xFFB77AFF)
        AuraVoiceState.SPEAKING -> Color(0xFFFF70B8)
    }
}
