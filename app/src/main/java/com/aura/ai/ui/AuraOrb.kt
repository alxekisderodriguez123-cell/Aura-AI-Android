package com.aura.ai.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.aura.ai.voice.AuraVoiceState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AuraOrb(
    state: AuraVoiceState,
    modifier: Modifier = Modifier,
    audioLevel: Float = 0f
) {
    val transition = rememberInfiniteTransition(label = "aura_liquid")

    val duration = when (state) {
        AuraVoiceState.IDLE -> 9000
        AuraVoiceState.LISTENING -> 6200
        AuraVoiceState.PROCESSING -> 3300
        AuraVoiceState.SPEAKING -> 1900
    }

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquid_phase"
    )

    val breathing by transition.animateFloat(
        initialValue = 0.975f,
        targetValue = when (state) {
            AuraVoiceState.IDLE -> 1.025f
            AuraVoiceState.LISTENING -> 1.035f
            AuraVoiceState.PROCESSING -> 1.055f
            AuraVoiceState.SPEAKING -> 1.065f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AuraVoiceState.IDLE -> 4200
                    AuraVoiceState.LISTENING -> 3600
                    AuraVoiceState.PROCESSING -> 1700
                    AuraVoiceState.SPEAKING -> 950
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_breath"
    )

    val pulse = audioLevel.coerceIn(0f, 1f) *
        if (state == AuraVoiceState.SPEAKING) 0.085f else 0.025f

    Canvas(
        modifier = modifier
            .size(330.dp)
            .scale(breathing + pulse)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.335f

        val cyan = Color(0xFF62F4FF)
        val blue = Color(0xFF397BFF)
        val indigo = Color(0xFF5140D9)
        val violet = Color(0xFF9854FF)
        val magenta = Color(0xFFFF4FA8)
        val coral = Color(0xFFFF806F)

        val saturation = when (state) {
            AuraVoiceState.IDLE -> 0.78f
            AuraVoiceState.LISTENING -> 0.68f
            AuraVoiceState.PROCESSING -> 0.92f
            AuraVoiceState.SPEAKING -> 1f
        }

        val energy = when (state) {
            AuraVoiceState.IDLE -> 0.48f
            AuraVoiceState.LISTENING -> 0.38f
            AuraVoiceState.PROCESSING -> 0.76f
            AuraVoiceState.SPEAKING -> 0.88f
        } + pulse

        fun organicPath(
            baseRadius: Float,
            offset: Float,
            amount: Float,
            frequency: Float
        ): Path {
            val count = 72
            val points = ArrayList<Offset>(count)

            for (i in 0 until count) {
                val angle = i.toFloat() / count * 2f * PI.toFloat()

                val waveA = sin(
                    angle * 3f + phase * frequency + offset
                )
                val waveB = cos(
                    angle * 2f - phase * 0.73f + offset
                )
                val waveC = sin(
                    angle * 5f + phase * 0.42f + offset * 1.7f
                )

                val deformation = 1f +
                    waveA * amount * 0.46f +
                    waveB * amount * 0.34f +
                    waveC * amount * 0.20f

                val r = baseRadius * deformation

                points.add(
                    Offset(
                        center.x + cos(angle).toFloat() * r,
                        center.y + sin(angle).toFloat() * r
                    )
                )
            }

            val path = Path()
            val first = points.first()
            val last = points.last()

            path.moveTo(
                (first.x + last.x) / 2f,
                (first.y + last.y) / 2f
            )

            for (i in points.indices) {
                val current = points[i]
                val next = points[(i + 1) % points.size]

                val midpoint = Offset(
                    (current.x + next.x) / 2f,
                    (current.y + next.y) / 2f
                )

                path.quadraticTo(
                    current.x,
                    current.y,
                    midpoint.x,
                    midpoint.y
                )
            }

            path.close()
            return path
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    violet.copy(alpha = 0.25f * energy),
                    magenta.copy(alpha = 0.12f * energy),
                    cyan.copy(alpha = 0.055f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 2.05f
            ),
            radius = radius * 2.05f,
            center = center
        )

        val silhouette = organicPath(
            radius,
            0f,
            if (state == AuraVoiceState.SPEAKING) 0.105f else 0.075f,
            1f
        )

        drawPath(
            path = silhouette,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF342C72),
                    Color(0xFF171A42),
                    Color(0xFF080D25)
                ),
                center = Offset(
                    center.x - radius * 0.22f,
                    center.y - radius * 0.30f
                ),
                radius = radius * 1.55f
            )
        )

        clipPath(silhouette) {
            val pinkLayer = organicPath(
                radius * 0.76f,
                1.4f,
                0.30f + pulse,
                0.82f
            )

            drawPath(
                path = pinkLayer,
                brush = Brush.linearGradient(
                    colors = listOf(
                        coral.copy(alpha = 0.88f * saturation),
                        magenta.copy(alpha = 0.92f * saturation),
                        violet.copy(alpha = 0.72f)
                    ),
                    start = Offset(
                        center.x - radius * 0.8f,
                        center.y - radius * 0.75f
                    ),
                    end = Offset(
                        center.x + radius,
                        center.y + radius * 0.8f
                    )
                )
            )

            val blueLayer = organicPath(
                radius * 0.83f,
                3.1f,
                0.27f + pulse * 0.7f,
                -0.67f
            )

            drawPath(
                path = blueLayer,
                brush = Brush.linearGradient(
                    colors = listOf(
                        cyan.copy(alpha = 0.96f * saturation),
                        blue.copy(alpha = 0.92f),
                        indigo.copy(alpha = 0.78f),
                        violet.copy(alpha = 0.54f)
                    ),
                    start = Offset(
                        center.x - radius,
                        center.y - radius * 0.55f
                    ),
                    end = Offset(
                        center.x + radius,
                        center.y + radius
                    )
                )
            )

            val innerWave = organicPath(
                radius * 0.56f,
                5.2f,
                0.25f + pulse,
                1.22f
            )

            drawPath(
                path = innerWave,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.70f * energy),
                        cyan.copy(alpha = 0.70f * saturation),
                        violet.copy(alpha = 0.34f),
                        Color.Transparent
                    ),
                    center = Offset(
                        center.x - radius * 0.08f,
                        center.y - radius * 0.12f
                    ),
                    radius = radius * 0.95f
                )
            )

            val warmCurrent = organicPath(
                radius * 0.43f,
                2.4f,
                0.34f,
                -0.91f
            )

            drawPath(
                path = warmCurrent,
                brush = Brush.linearGradient(
                    colors = listOf(
                        coral.copy(alpha = 0.56f * saturation),
                        magenta.copy(alpha = 0.42f),
                        violet.copy(alpha = 0.12f)
                    ),
                    start = Offset(
                        center.x - radius * 0.4f,
                        center.y - radius * 0.5f
                    ),
                    end = Offset(
                        center.x + radius * 0.7f,
                        center.y + radius * 0.6f
                    )
                )
            )

            val highlightCenter = Offset(
                center.x - radius * 0.18f,
                center.y - radius * 0.23f
            )
            val highlightRadius = radius * (0.68f + pulse)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.88f * energy),
                        Color(0xFFD9FCFF).copy(alpha = 0.62f * energy),
                        cyan.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = highlightCenter,
                    radius = highlightRadius
                ),
                radius = highlightRadius,
                center = highlightCenter
            )

            val reflection = Path().apply {
                moveTo(
                    center.x - radius * 0.62f,
                    center.y - radius * 0.42f
                )
                cubicTo(
                    center.x - radius * 0.32f,
                    center.y - radius * 0.91f,
                    center.x + radius * 0.30f,
                    center.y - radius * 0.82f,
                    center.x + radius * 0.61f,
                    center.y - radius * 0.47f
                )
            }

            drawPath(
                path = reflection,
                color = Color.White.copy(alpha = 0.28f),
                style = Stroke(width = 2.1.dp.toPx())
            )
        }

        drawPath(
            path = silhouette,
            color = Color.White.copy(alpha = 0.16f),
            style = Stroke(width = 1.15.dp.toPx())
        )
    }
}
