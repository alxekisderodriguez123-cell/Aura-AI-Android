
package com.aura.ai.ui

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
        AuraVoiceState.IDLE -> 11000
        AuraVoiceState.LISTENING -> 7500
        AuraVoiceState.PROCESSING -> 4300
        AuraVoiceState.SPEAKING -> 2800
    }

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = duration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquid_phase"
    )

    val secondaryPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = duration * 2,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "secondary_liquid_phase"
    )

    val pulse = audioLevel.coerceIn(0f, 1f) *
        if (state == AuraVoiceState.SPEAKING) 0.075f else 0.025f

    Canvas(
        modifier = modifier.size(330.dp)
    ) {
        val center = Offset(
            size.width / 2f,
            size.height / 2f
        )

        // El radio y el contorno nunca cambian.
        val radius = size.minDimension * 0.405f

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

        val cyan = Color(0xFF62F4FF)
        val blue = Color(0xFF287BFF)
        val indigo = Color(0xFF2435B8)
        val violet = Color(0xFF9854FF)
        val magenta = Color(0xFFFF45A8)
        val coral = Color(0xFFFF806F)

        val saturation = when (state) {
            AuraVoiceState.IDLE -> 0.78f
            AuraVoiceState.LISTENING -> 0.85f
            AuraVoiceState.PROCESSING -> 0.95f
            AuraVoiceState.SPEAKING -> 1f
        }

        val energy = when (state) {
            AuraVoiceState.IDLE -> 0.58f
            AuraVoiceState.LISTENING -> 0.68f
            AuraVoiceState.PROCESSING -> 0.82f
            AuraVoiceState.SPEAKING -> 1f
        }

        fun organicPath(
            baseRadius: Float,
            offset: Float,
            amount: Float,
            frequency: Float,
            movementPhase: Float = phase
        ): Path {
            val count = 100
            val points = ArrayList<Offset>(count)

            for (i in 0 until count) {
                val angle =
                    i.toFloat() / count * 2f * PI.toFloat()

                val waveA = sin(
                    angle * 2.5f +
                        movementPhase * frequency +
                        offset
                )

                val waveB = cos(
                    angle * 3.5f -
                        movementPhase * 0.65f +
                        offset
                )

                val waveC = sin(
                    angle * 5f +
                        movementPhase * 0.38f +
                        offset * 1.6f
                )

                val deformation = 1f +
                    waveA * amount * 0.48f +
                    waveB * amount * 0.32f +
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

        // Resplandor exterior circular.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    violet.copy(alpha = 0.23f * energy),
                    magenta.copy(alpha = 0.12f * energy),
                    cyan.copy(alpha = 0.07f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 1.55f
            ),
            radius = radius * 1.55f,
            center = center
        )

        // Base de cristal: siempre perfectamente circular.
        drawPath(
            path = spherePath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF263D91),
                    Color(0xFF101C52),
                    Color(0xFF080D25)
                ),
                center = Offset(
                    center.x - radius * 0.30f,
                    center.y - radius * 0.42f
                ),
                radius = radius * 1.75f
            )
        )

        // Todas las formas líquidas quedan confinadas
        // dentro del contorno circular.
        clipPath(spherePath) {

            // Corriente rosa.
            val pinkLayer = organicPath(
                baseRadius = radius * 0.77f,
                offset = 1.4f,
                amount = 0.22f + pulse,
                frequency = 0.78f
            )

            drawPath(
                path = pinkLayer,
                brush = Brush.linearGradient(
                    colors = listOf(
                        coral.copy(alpha = 0.90f * saturation),
                        magenta.copy(alpha = 0.92f * saturation),
                        violet.copy(alpha = 0.76f)
                    ),
                    start = Offset(
                        center.x - radius,
                        center.y - radius * 0.75f
                    ),
                    end = Offset(
                        center.x + radius,
                        center.y + radius
                    )
                )
            )

            // Corriente azul y cian.
            val blueLayer = organicPath(
                baseRadius = radius * 0.82f,
                offset = 3.1f,
                amount = 0.19f + pulse,
                frequency = -0.62f
            )

            drawPath(
                path = blueLayer,
                brush = Brush.linearGradient(
                    colors = listOf(
                        cyan.copy(alpha = 0.96f * saturation),
                        blue.copy(alpha = 0.93f),
                        indigo.copy(alpha = 0.88f),
                        violet.copy(alpha = 0.65f)
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

            // Segunda corriente interior.
            val innerWave = organicPath(
                baseRadius = radius * 0.59f,
                offset = 5.2f,
                amount = 0.28f + pulse,
                frequency = 1.12f,
                movementPhase = secondaryPhase
            )

            drawPath(
                path = innerWave,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.66f * energy),
                        cyan.copy(alpha = 0.75f * saturation),
                        violet.copy(alpha = 0.42f),
                        Color.Transparent
                    ),
                    center = Offset(
                        center.x - radius * 0.10f,
                        center.y - radius * 0.12f
                    ),
                    radius = radius * 0.95f
                )
            )

            // Núcleo cálido que fluye lentamente.
            val warmCurrent = organicPath(
                baseRadius = radius * 0.39f,
                offset = 2.4f,
                amount = 0.30f + pulse,
                frequency = -0.85f,
                movementPhase = secondaryPhase
            )

            drawPath(
                path = warmCurrent,
                brush = Brush.linearGradient(
                    colors = listOf(
                        coral.copy(alpha = 0.62f * saturation),
                        magenta.copy(alpha = 0.50f),
                        violet.copy(alpha = 0.18f)
                    ),
                    start = Offset(
                        center.x - radius * 0.55f,
                        center.y - radius * 0.55f
                    ),
                    end = Offset(
                        center.x + radius * 0.65f,
                        center.y + radius * 0.65f
                    )
                )
            )

            // Luz central difuminada.
            val highlightCenter = Offset(
                center.x - radius * 0.18f,
                center.y - radius * 0.24f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.52f * energy),
                        Color(0xFFD9FCFF).copy(alpha = 0.38f * energy),
                        cyan.copy(alpha = 0.17f),
                        Color.Transparent
                    ),
                    center = highlightCenter,
                    radius = radius * 0.78f
                ),
                radius = radius * 0.78f,
                center = highlightCenter
            )

            // Reflejo superior de cristal.
            val reflection = Path().apply {
                moveTo(
                    center.x - radius * 0.66f,
                    center.y - radius * 0.43f
                )

                cubicTo(
                    center.x - radius * 0.40f,
                    center.y - radius * 0.88f,
                    center.x + radius * 0.28f,
                    center.y - radius * 0.91f,
                    center.x + radius * 0.64f,
                    center.y - radius * 0.48f
                )
            }

            drawPath(
                path = reflection,
                color = Color.White.copy(alpha = 0.34f),
                style = Stroke(width = 2.1.dp.toPx())
            )
        }

        // Borde exterior: círculo fijo, sin deformaciones.
        drawCircle(
            color = Color(0xFFB9DFFF).copy(alpha = 0.20f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.3.dp.toPx())
        )

        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.42f),
                    cyan.copy(alpha = 0.24f),
                    violet.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.12f)
                ),
                start = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                end = Offset(
                    center.x + radius,
                    center.y + radius
                )
            ),
            radius = radius - 1.2.dp.toPx(),
            center = center,
            style = Stroke(width = 1.6.dp.toPx())
        )
    }
}
