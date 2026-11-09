
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
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.geometry.Offset
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
        label = "aura_siri_orb"
    )

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AuraVoiceState.IDLE -> 11000
                    AuraVoiceState.LISTENING -> 4200
                    AuraVoiceState.PROCESSING -> 2800
                    AuraVoiceState.SPEAKING -> 3400
                }
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquid_phase"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.975f,
        targetValue = when (state) {
            AuraVoiceState.IDLE -> 1.025f
            AuraVoiceState.LISTENING -> 1.065f
            AuraVoiceState.PROCESSING -> 1.045f
            AuraVoiceState.SPEAKING -> 1.075f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AuraVoiceState.IDLE -> 3200
                    AuraVoiceState.LISTENING -> 950
                    AuraVoiceState.PROCESSING -> 1300
                    AuraVoiceState.SPEAKING -> 750
                }
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_breathing"
    )

    val movement by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AuraVoiceState.IDLE -> 5200
                    AuraVoiceState.LISTENING -> 1800
                    AuraVoiceState.PROCESSING -> 1400
                    AuraVoiceState.SPEAKING -> 1100
                }
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_fluid_movement"
    )

    Canvas(
        modifier = modifier
            .size(330.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
    ) {
        val center = Offset(
            size.width / 2f,
            size.height / 2f
        )

        val radius = size.minDimension * 0.335f

        val cyan = Color(0xFF00E8FF)
        val blue = Color(0xFF268DFF)
        val violet = Color(0xFFAC52FF)
        val magenta = Color(0xFFFF318D)
        val pink = Color(0xFFFF5CBA)
        val teal = Color(0xFF00E8B7)

        val phaseRadians = Math.toRadians(
            phase.toDouble()
        )

        val movementRadians = Math.toRadians(
            movement.toDouble() * 22.0
        )

        val listeningIntensity = when (state) {
            AuraVoiceState.IDLE -> 0.70f
            AuraVoiceState.LISTENING -> 1.0f
            AuraVoiceState.PROCESSING -> 0.90f
            AuraVoiceState.SPEAKING -> 1.0f
        }

        // -------------------------------------------------
        // 1. EXTERIOR ATMOSPHERIC GLOW
        // -------------------------------------------------

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    violet.copy(alpha = 0.17f),
                    magenta.copy(alpha = 0.09f),
                    cyan.copy(alpha = 0.07f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 2.05f
            ),
            radius = radius * 2.05f,
            center = center
        )

        // -------------------------------------------------
        // 2. THREE GLASS RINGS
        // -------------------------------------------------

        val ringRadius = radius * 1.29f

        drawCircle(
            color = violet.copy(alpha = 0.045f),
            radius = ringRadius,
            center = center,
            style = Stroke(width = 10.dp.toPx())
        )

        drawCircle(
            color = cyan.copy(alpha = 0.12f),
            radius = ringRadius,
            center = center,
            style = Stroke(width = 3.5.dp.toPx())
        )

        drawCircle(
            color = violet.copy(alpha = 0.13f),
            radius = radius * 1.18f,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )

        drawCircle(
            color = magenta.copy(alpha = 0.16f),
            radius = radius * 1.08f,
            center = center,
            style = Stroke(width = 2.5.dp.toPx())
        )

        // Moving colored light around the exterior.
        val ringLightAngle = phaseRadians

        val ringLight = Offset(
            center.x + cos(ringLightAngle).toFloat() * ringRadius,
            center.y + sin(ringLightAngle).toFloat() * ringRadius
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f),
                    cyan.copy(alpha = 0.55f),
                    Color.Transparent
                ),
                center = ringLight,
                radius = radius * 0.30f
            ),
            radius = radius * 0.30f,
            center = ringLight
        )

        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    cyan.copy(alpha = 0.05f),
                    cyan.copy(alpha = 0.95f),
                    violet.copy(alpha = 0.75f),
                    magenta.copy(alpha = 0.85f),
                    Color(0xFFFFD68A).copy(alpha = 0.8f),
                    cyan.copy(alpha = 0.05f)
                ),
                center = center
            ),
            startAngle = phase,
            sweepAngle = 125f,
            useCenter = false,
            topLeft = Offset(
                center.x - ringRadius,
                center.y - ringRadius
            ),
            size = Size(
                ringRadius * 2f,
                ringRadius * 2f
            ),
            style = Stroke(
                width = 2.8.dp.toPx()
            )
        )

        drawArc(
            color = violet.copy(alpha = 0.55f),
            startAngle = -phase * 0.72f + 160f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(
                center.x - radius * 1.18f,
                center.y - radius * 1.18f
            ),
            size = Size(
                radius * 2.36f,
                radius * 2.36f
            ),
            style = Stroke(
                width = 2.dp.toPx()
            )
        )

        // -------------------------------------------------
        // 3. GLASS SPHERE
        // -------------------------------------------------

        val spherePath = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
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
                    Color(0xFF392F71),
                    Color(0xFF201B43),
                    Color(0xFF100F29),
                    Color(0xFF080D23)
                ),
                center = center,
                radius = radius * 1.15f
            ),
            radius = radius,
            center = center
        )

        clipPath(spherePath) {

            // Inner atmospheric color.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        violet.copy(alpha = 0.40f),
                        magenta.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(
                        center.x,
                        center.y - radius * 0.15f
                    ),
                    radius = radius * 1.3f
                ),
                radius = radius,
                center = center
            )

            // -------------------------------------------------
            // 4. ORGANIC MAGENTA LIQUID FORM
            // -------------------------------------------------

            val magentaPath = Path().apply {
                moveTo(
                    center.x - radius * 0.48f,
                    center.y - radius * 0.10f
                )

                cubicTo(
                    center.x - radius * 0.62f,
                    center.y - radius * 0.83f,
                    center.x + radius * 0.08f,
                    center.y - radius * 0.95f,
                    center.x + radius * 0.50f,
                    center.y - radius * 0.48f
                )

                cubicTo(
                    center.x + radius * 0.92f,
                    center.y - radius * 0.08f,
                    center.x + radius * 0.10f,
                    center.y + radius * 0.12f,
                    center.x - radius * 0.48f,
                    center.y - radius * 0.10f
                )

                close()
            }

            drawPath(
                path = magentaPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF236E).copy(alpha = 0.88f),
                        magenta.copy(alpha = 0.72f),
                        violet.copy(alpha = 0.48f)
                    ),
                    start = Offset(
                        center.x,
                        center.y - radius
                    ),
                    end = Offset(
                        center.x + radius,
                        center.y + radius * 0.15f
                    )
                )
            )

            // -------------------------------------------------
            // 5. CYAN LIQUID FORM
            // -------------------------------------------------

            val cyanPath = Path().apply {
                moveTo(
                    center.x - radius * 0.12f,
                    center.y - radius * 0.32f
                )

                cubicTo(
                    center.x - radius * 0.95f,
                    center.y - radius * 0.70f,
                    center.x - radius * 0.93f,
                    center.y + radius * 0.15f,
                    center.x - radius * 0.40f,
                    center.y + radius * 0.72f
                )

                cubicTo(
                    center.x + radius * 0.02f,
                    center.y + radius * 1.05f,
                    center.x + radius * 0.42f,
                    center.y + radius * 0.70f,
                    center.x + radius * 0.25f,
                    center.y + radius * 0.18f
                )

                cubicTo(
                    center.x + radius * 0.15f,
                    center.y - radius * 0.12f,
                    center.x - radius * 0.12f,
                    center.y - radius * 0.32f,
                    center.x - radius * 0.12f,
                    center.y - radius * 0.32f
                )

                close()
            }

            drawPath(
                path = cyanPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        cyan.copy(alpha = 0.78f),
                        blue.copy(alpha = 0.84f),
                        violet.copy(alpha = 0.52f)
                    ),
                    start = Offset(
                        center.x - radius,
                        center.y - radius * 0.4f
                    ),
                    end = Offset(
                        center.x + radius * 0.3f,
                        center.y + radius
                    )
                )
            )

            // -------------------------------------------------
            // 6. TURQUOISE LIQUID FORM
            // -------------------------------------------------

            val tealPath = Path().apply {
                moveTo(
                    center.x - radius * 0.12f,
                    center.y - radius * 0.12f
                )

                cubicTo(
                    center.x + radius * 0.40f,
                    center.y - radius * 0.62f,
                    center.x + radius * 0.88f,
                    center.y - radius * 0.12f,
                    center.x + radius * 0.92f,
                    center.y + radius * 0.12f
                )

                cubicTo(
                    center.x + radius * 0.62f,
                    center.y + radius * 0.42f,
                    center.x + radius * 0.26f,
                    center.y + radius * 0.24f,
                    center.x - radius * 0.12f,
                    center.y - radius * 0.12f
                )

                close()
            }

            drawPath(
                path = tealPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4CFFF0).copy(alpha = 0.78f),
                        teal.copy(alpha = 0.74f),
                        cyan.copy(alpha = 0.32f)
                    ),
                    start = Offset(
                        center.x,
                        center.y - radius
                    ),
                    end = Offset(
                        center.x + radius,
                        center.y + radius * 0.5f
                    )
                )
            )

            // -------------------------------------------------
            // 7. VIOLET GLASS FORM
            // -------------------------------------------------

            val violetPath = Path().apply {
                moveTo(
                    center.x - radius * 0.34f,
                    center.y - radius * 0.10f
                )

                cubicTo(
                    center.x - radius * 0.12f,
                    center.y - radius * 0.68f,
                    center.x + radius * 0.58f,
                    center.y - radius * 0.15f,
                    center.x + radius * 0.40f,
                    center.y + radius * 0.65f
                )

                cubicTo(
                    center.x + radius * 0.15f,
                    center.y + radius * 0.95f,
                    center.x - radius * 0.50f,
                    center.y + radius * 0.65f,
                    center.x - radius * 0.34f,
                    center.y - radius * 0.10f
                )

                close()
            }

            drawPath(
                path = violetPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFED8AFF).copy(alpha = 0.48f),
                        violet.copy(alpha = 0.64f),
                        blue.copy(alpha = 0.48f)
                    ),
                    start = Offset(
                        center.x - radius * 0.5f,
                        center.y - radius
                    ),
                    end = Offset(
                        center.x + radius * 0.4f,
                        center.y + radius
                    )
                )
            )

            // -------------------------------------------------
            // 8. LIQUID MOVEMENT HIGHLIGHTS
            // -------------------------------------------------

            val movingLight = Offset(
                center.x + cos(
                    phaseRadians + movementRadians
                ).toFloat() * radius * 0.28f,
                center.y + sin(
                    phaseRadians + movementRadians
                ).toFloat() * radius * 0.24f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.98f),
                        Color(0xFFBDFEFF).copy(alpha = 0.72f),
                        cyan.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = movingLight,
                    radius = radius * 0.70f
                ),
                radius = radius * 0.70f,
                center = movingLight
            )

            // Central white light.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 1f),
                        Color(0xFFF5FFFF).copy(alpha = 0.90f),
                        cyan.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.47f
                ),
                radius = radius * 0.47f,
                center = center
            )

            // -------------------------------------------------
            // 9. GLASS REFLECTIONS
            // -------------------------------------------------

            val upperReflection = Path().apply {
                moveTo(
                    center.x - radius * 0.70f,
                    center.y - radius * 0.44f
                )

                cubicTo(
                    center.x - radius * 0.35f,
                    center.y - radius * 0.94f,
                    center.x + radius * 0.30f,
                    center.y - radius * 0.87f,
                    center.x + radius * 0.65f,
                    center.y - radius * 0.48f
                )
            }

            drawPath(
                path = upperReflection,
                color = Color.White.copy(alpha = 0.24f),
                style = Stroke(
                    width = 2.dp.toPx()
                )
            )

            // Soft glass highlight.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.19f),
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(
                        center.x - radius * 0.45f,
                        center.y - radius * 0.55f
                    ),
                    radius = radius * 0.78f
                ),
                radius = radius,
                center = center
            )
        }

        // -------------------------------------------------
        // 10. OUTER GLASS CONTOUR
        // -------------------------------------------------

        drawCircle(
            color = Color.White.copy(alpha = 0.10f),
            radius = radius,
            center = center,
            style = Stroke(
                width = 1.1.dp.toPx()
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryGlow(state).copy(
                        alpha = 0.12f * listeningIntensity
                    ),
                    
