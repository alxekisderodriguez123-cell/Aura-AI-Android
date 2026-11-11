
package com.aura.ai.ui

import android.graphics.BitmapFactory
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.ImageDecoder
import android.os.Build
import android.widget.ImageView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aura.ai.R
import com.aura.ai.voice.AuraVoiceState
import kotlin.math.sin

@Composable
fun AuraOrb(
    state: AuraVoiceState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSpeaking = state == AuraVoiceState.SPEAKING

    val transition = rememberInfiniteTransition(
        label = "aura_speech_pulse"
    )

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1050,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_phase"
    )

    val pulse = if (isSpeaking) {
        1f + sin(phase) * 0.045f
    } else {
        1f
    }

    AndroidView(
        modifier = modifier
            .size(270.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            },
        factory = { viewContext ->
            ImageView(viewContext).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                if (Build.VERSION.SDK_INT >= 28) {
                    try {
                        val source = ImageDecoder.createSource(
                            resources,
                            R.raw.aura_orb
                        )

                        val drawable = ImageDecoder.decodeDrawable(source)

                        setImageDrawable(drawable)

                        (drawable as? AnimatedImageDrawable)?.start()
                    } catch (_: Exception) {
                        setImageResource(R.drawable.ic_launcher_foreground)
                    }
                } else {
                    val bitmap = BitmapFactory.decodeResource(
                        resources,
                        R.raw.aura_orb
                    )
                    setImageBitmap(bitmap)
                }
            }
        },
        update = { imageView ->
            val drawable = imageView.drawable
            if (isSpeaking) {
                (drawable as? AnimatedImageDrawable)?.start()
            }
        }
    )
}
