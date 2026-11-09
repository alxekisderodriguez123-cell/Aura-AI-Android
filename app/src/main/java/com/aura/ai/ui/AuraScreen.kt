
package com.aura.ai.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
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
    val isListening = voiceState == AuraVoiceState.LISTENING

    val accent = when (voiceState) {
        AuraVoiceState.IDLE -> Color(0xFF728BFF)
        AuraVoiceState.LISTENING -> Color(0xFF64E8FF)
        AuraVoiceState.PROCESSING -> Color(0xFFB18BFF)
        AuraVoiceState.SPEAKING -> Color(0xFF83D9FF)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF070A16)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF111B35),
                            Color(0xFF090E20),
                            Color(0xFF070A16)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .size(360.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 25.dp,
                        vertical = 24.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "A U R A",
                            color = Color(0xFFEAF7FF),
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 7.sp
                        )

                        Text(
                            text = "PERSONAL AI ASSISTANT",
                            color = Color(0xFF8293B7),
                            fontSize = 8.sp,
                            letterSpacing = 2.3.sp
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.055f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        accent,
                                        CircleShape
                                    )
                            )

                            Spacer(
                                modifier = Modifier.size(7.dp)
                            )

                            Text(
                                text = when (voiceState) {
                                    AuraVoiceState.IDLE -> "READY"
                                    AuraVoiceState.LISTENING -> "LISTENING"
                                    AuraVoiceState.PROCESSING -> "THINKING"
                                    AuraVoiceState.SPEAKING -> "SPEAKING"
                                },
                                color = Color(0xFFC6D7F4),
                                fontSize = 8.sp,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.65f))

                // Main orb
                AuraOrb(
                    state = voiceState
                )

                Spacer(modifier = Modifier.height(15.dp))

                AnimatedContent(
                    targetState = voiceState,
                    transitionSpec = {
                        fadeIn(tween(350)) togetherWith
                            fadeOut(tween(200))
                    },
                    label = "aura_status_transition"
                ) { state ->
                    Text(
                        text = when (state) {
                            AuraVoiceState.IDLE ->
                                "Estoy aquí"

                            AuraVoiceState.LISTENING ->
                                "Te estoy escuchando"

                            AuraVoiceState.PROCESSING ->
                                "Procesando"

                            AuraVoiceState.SPEAKING ->
                                "Estoy hablando"
                        },
                        color = Color(0xFFF0F6FF),
                        fontSize = 23.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(9.dp))

                Text(
                    text = message,
                    color = Color(0xFF8F9FBD),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Recognized speech
                if (transcript.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.055f),
                        shape = RoundedCornerShape(23.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(17.dp)
                        ) {
                            Text(
                                text = "TE HE ESCUCHADO",
                                color = Color(0xFF91B9E8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 2.sp
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = transcript,
                                color = Color(0xFFEAF7FF),
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(17.dp))
                }

                Spacer(modifier = Modifier.weight(0.25f))

                // Voice action
                Button(
                    onClick = onVoiceButtonClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color(0xFF081020)
                    )
                ) {
                    Text(
                        text = if (isListening) {
                            "DETENER ESCUCHA"
                        } else {
                            "HABLAR CON AURA"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(23.dp))

                Text(
                    text = "AURA · INTELLIGENCE IN MOTION",
                    color = Color(0xFF566482),
                    fontSize = 8.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
