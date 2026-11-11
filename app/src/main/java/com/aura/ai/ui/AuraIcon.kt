
package com.aura.ai.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AuraIconType {
    CHAT,
    MIC,
    BACK,
    SEND,
    MUSIC,
    PLAY,
    PAUSE,
    PREVIOUS,
    NEXT,
    SETTINGS,
    SPARKLES
}

@Composable
fun AuraIcon(
    type: AuraIconType,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = this.size.minDimension
        val stroke = s * 0.085f

        fun line(
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float
        ) {
            drawLine(
                color = tint,
                start = Offset(x1 * w, y1 * h),
                end = Offset(x2 * w, y2 * h),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }

        fun circle(
            cx: Float,
            cy: Float,
            radius: Float,
            filled: Boolean = false
        ) {
            drawCircle(
                color = tint,
                radius = radius * s,
                center = Offset(cx * w, cy * h),
                style = if (filled) {
                    androidx.compose.ui.graphics.drawscope.Fill
                } else {
                    Stroke(width = stroke)
                }
            )
        }

        fun path(
            points: List<Pair<Float, Float>>,
            close: Boolean = false,
            filled: Boolean = false
        ) {
            val p = Path()
            points.forEachIndexed { index, point ->
                val x = point.first * w
                val y = point.second * h

                if (index == 0) p.moveTo(x, y)
                else p.lineTo(x, y)
            }

            if (close) p.close()

            drawPath(
                path = p,
                color = tint,
                style = if (filled) {
                    androidx.compose.ui.graphics.drawscope.Fill
                } else {
                    Stroke(
                        width = stroke,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                }
            )
        }

        when (type) {
            AuraIconType.CHAT -> {
                val p = Path().apply {
                    moveTo(w * 0.18f, h * 0.20f)
                    quadraticTo(w * 0.18f, h * 0.12f, w * 0.28f, h * 0.12f)
                    lineTo(w * 0.72f, h * 0.12f)
                    quadraticTo(w * 0.82f, h * 0.12f, w * 0.82f, h * 0.22f)
                    lineTo(w * 0.82f, h * 0.66f)
                    quadraticTo(w * 0.82f, h * 0.76f, w * 0.72f, h * 0.76f)
                    lineTo(w * 0.43f, h * 0.76f)
                    lineTo(w * 0.25f, h * 0.90f)
                    lineTo(w * 0.28f, h * 0.76f)
                    quadraticTo(w * 0.18f, h * 0.76f, w * 0.18f, h * 0.66f)
                    close()
                }

                drawPath(
                    p,
                    tint,
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                line(0.34f, 0.39f, 0.66f, 0.39f)
                line(0.34f, 0.54f, 0.58f, 0.54f)
            }

            AuraIconType.MIC -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.36f, h * 0.12f),
                    size = Size(w * 0.28f, h * 0.52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.2f),
                    style = Stroke(width = stroke)
                )

                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.22f, h * 0.34f),
                    size = Size(w * 0.56f, h * 0.48f),
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round
                    )
                )

                line(0.50f, 0.82f, 0.50f, 0.93f)
                line(0.37f, 0.93f, 0.63f, 0.93f)
            }

            AuraIconType.BACK -> {
                line(0.78f, 0.50f, 0.22f, 0.50f)
                line(0.22f, 0.50f, 0.47f, 0.25f)
                line(0.22f, 0.50f, 0.47f, 0.75f)
            }

            AuraIconType.SEND -> {
                path(
                    listOf(
                        0.12f to 0.47f,
                        0.88f to 0.12f,
                        0.64f to 0.88f,
                        0.47f to 0.56f,
                        0.12f to 0.47f
                    ),
                    close = true
                )
                line(0.47f, 0.56f, 0.88f, 0.12f)
            }

            AuraIconType.MUSIC -> {
                line(0.65f, 0.16f, 0.65f, 0.72f)
                line(0.65f, 0.16f, 0.86f, 0.22f)
                line(0.65f, 0.30f, 0.86f, 0.36f)
                circle(0.43f, 0.77f, 0.12f, true)
                circle(0.76f, 0.68f, 0.12f, true)
            }

            AuraIconType.PLAY -> {
                path(
                    listOf(
                        0.30f to 0.16f,
                        0.78f to 0.50f,
                        0.30f to 0.84f
                    ),
                    close = true,
                    filled = true
                )
            }

            AuraIconType.PAUSE -> {
                drawRoundRect(
                    tint,
                    Offset(w * 0.27f, h * 0.19f),
                    Size(w * 0.17f, h * 0.62f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.05f)
                )
                drawRoundRect(
                    tint,
                    Offset(w * 0.57f, h * 0.19f),
                    Size(w * 0.17f, h * 0.62f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.05f)
                )
            }

            AuraIconType.PREVIOUS -> {
                line(0.38f, 0.20f, 0.38f, 0.80f)
                path(
                    listOf(
                        0.76f to 0.22f,
                        0.43f to 0.50f,
                        0.76f to 0.78f
                    ),
                    close = true,
                    filled = true
                )
            }

            AuraIconType.NEXT -> {
                line(0.62f, 0.20f, 0.62f, 0.80f)
                path(
                    listOf(
                        0.24f to 0.22f,
                        0.57f to 0.50f,
                        0.24f to 0.78f
                    ),
                    close = true,
                    filled = true
                )
            }

            AuraIconType.SETTINGS -> {
                circle(0.50f, 0.50f, 0.22f)
                circle(0.50f, 0.50f, 0.07f)

                for (i in 0 until 8) {
                    val angle = i * (Math.PI / 4.0)
                    val x1 = 0.50f + kotlin.math.cos(angle).toFloat() * 0.29f
                    val y1 = 0.50f + kotlin.math.sin(angle).toFloat() * 0.29f
                    val x2 = 0.50f + kotlin.math.cos(angle).toFloat() * 0.39f
                    val y2 = 0.50f + kotlin.math.sin(angle).toFloat() * 0.39f
                    line(x1, y1, x2, y2)
                }
            }

            AuraIconType.SPARKLES -> {
                path(
                    listOf(
                        0.50f to 0.08f,
                        0.59f to 0.41f,
                        0.92f to 0.50f,
                        0.59f to 0.59f,
                        0.50f to 0.92f,
                        0.41f to 0.59f,
                        0.08f to 0.50f,
                        0.41f to 0.41f
                    ),
                    close = true
                )
                line(0.80f, 0.10f, 0.80f, 0.27f)
                line(0.72f, 0.18f, 0.88f, 0.18f)
            }
        }
    }
}
