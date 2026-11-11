package com.aura.ai.ui

import android.graphics.BitmapFactory
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.ImageDecoder
import com.aura.ai.R
import com.aura.ai.voice.AuraVoiceState

@Composable
fun AuraOrb(
    state: AuraVoiceState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isSpeaking = state == AuraVoiceState.SPEAKING

    val scale by animateFloatAsState(
        targetValue = if (isSpeaking) 1.055f else 1f,
        animationSpec = if (isSpeaking) {
            infiniteRepeatable(
                animation = tween(durationMillis = 650),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            spring(
                dampingRatio = 0.65f,
                stiffness = 220f
            )
        },
        label = "aura_speaking_pulse"
    )

    val orbDrawable = remember(context) {
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeDrawable(
                    ImageDecoder.createSource(
                        context.resources,
                        R.raw.aura_orb
                    )
                )
            } else {
                BitmapFactory.decodeResource(
                    context.resources,
                    R.raw.aura_orb
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(orbDrawable) {
        if (Build.VERSION.SDK_INT >= 28 &&
            orbDrawable is AnimatedImageDrawable
        ) {
            orbDrawable.repeatCount =
                AnimatedImageDrawable.REPEAT_INFINITE
            orbDrawable.start()
        }

        onDispose {
            if (Build.VERSION.SDK_INT >= 28 &&
                orbDrawable is AnimatedImageDrawable
            ) {
                orbDrawable.stop()
            }
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                ImageView(viewContext).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                    setBackgroundColor(
                        android.graphics.Color.TRANSPARENT
                    )
                    setImageDrawable(orbDrawable)
                }
            },
            update = { imageView ->
                if (imageView.drawable !== orbDrawable) {
                    imageView.setImageDrawable(orbDrawable)
                }
            }
        )
    }
}
