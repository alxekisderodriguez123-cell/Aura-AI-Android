package com.aura.ai.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.ai.voice.AuraVoiceState

@Composable
fun AuraScreen(
    voiceState: AuraVoiceState,
    transcript: String,
    message: String,
    onVoiceButtonClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF080A16)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF111831),
                            Color(0xFF080A16),
                            Color(0xFF080A16)
                        )
                    )
                )
                .padding(horizontal = 26.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(modifier = Modifier.weight(0.5f))

            Text(
                text = "A U R A",
                color = Color(0xFFE9F8FF),
                fontSize = 29.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 10.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "YOUR ANDROID ASSISTANT",
                color = Color(0xFF8998B8),
                fontSize = 10.sp,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(62.dp))

            AuraOrb(
                state = voiceState
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = when (voiceState) {
                    AuraVoiceState.IDLE -> "Aura está preparada"
                    AuraVoiceState.LISTENING -> "Te estoy escuchando"
                    AuraVoiceState.PROCESSING -> "Estoy procesando"
                    AuraVoiceState.SPEAKING -> "Aura está hablando"
                },
                color = Color(0xFFE4EDFF),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = message,
                color = Color(0xFF8998B8),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            if (transcript.isNotBlank()) {
                Spacer(modifier = Modifier.height(25.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF171D35).copy(alpha = 0.85f),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "LO QUE HE ESCUCHADO",
                            color = Color(0xFF8EA5D7),
                            fontSize = 9.sp,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(9.dp))

                        Text(
                            text = transcript,
                            color = Color(0xFFE9F8FF),
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = onVoiceButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF657DFF),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (
                        voiceState == AuraVoiceState.LISTENING
                    ) {
                        "DETENER ESCUCHA"
                    } else {
                        "HABLAR CON AURA"
                    },
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            Text(
                text = "VOICE FOUNDATION · EXPERIMENTAL",
                color = Color(0xFF525E7B),
                fontSize = 9.sp,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
private fun AuraOrb(
    state: AuraVoiceState
) {
    val transition = rememberInfiniteTransition(
        label = "aura_orb_animation"
    )

    val breathing by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = if (
            state == AuraVoiceState.LISTENING
        ) 1.07f else 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (
                    state == AuraVoiceState.LISTENING
                ) 900 else 2200
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_breathing"
    )

    val glowColor = when (state) {
        AuraVoiceState.IDLE -> Color(0xFF687CFF)
        AuraVoiceState.LISTENING -> Color(0xFF45E7FF)
        AuraVoiceState.PROCESSING -> Color(0xFFAD82FF)
        AuraVoiceState.SPEAKING -> Color(0xFF72CFFF)
    }

    Box(
        modifier = Modifier
            .size(270.dp)
            .scale(breathing),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(
                size.width / 2f,
                size.height / 2f
            )

            val radius = size.minDimension * 0.43f

            drawCircle(
                color = glowColor.copy(alpha = 0.09f),
                radius = radius * 1.48f,
                center = center
            )

            drawCircle(
                color = glowColor.copy(alpha = 0.12f),
                radius = radius * 1.28f,
                center = center
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE9FFFF),
                        Color(0xFF8DEBFF),
                        glowColor,
                        Color(0xFF443D91),
                        Color(0xFF151C3D)
                    ),
                    center = Offset(
                        center.x - radius * 0.25f,
                        center.y - radius * 0.35f
                    ),
                    radius = radius * 1.7f
                ),
                radius = radius,
                center = center
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.72f),
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(
                        center.x - radius * 0.38f,
                        center.y - radius * 0.45f
                    ),
                    radius = radius * 0.8f
                ),
                radius = radius,
                center = center
            )
        }
    }
}
