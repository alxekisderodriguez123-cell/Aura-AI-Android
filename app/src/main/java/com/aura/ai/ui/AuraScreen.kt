package com.aura.ai.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.ai.chat.ChatMessage
import com.aura.ai.media.AuraPlaybackController
import com.aura.ai.voice.AuraVoiceState
import kotlinx.coroutines.delay

private val AuraBackground = Color(0xFF101329)
private val AuraPanel = Color(0xFF211D3D)
private val AuraPanelLight = Color(0xFF30234F)
private val AuraPurple = Color(0xFF9D55FF)
private val AuraPink = Color(0xFFF04FAD)
private val AuraCyan = Color(0xFF39DDF5)
private val AuraBlue = Color(0xFF438AFF)
private val AuraWhite = Color(0xFFF2F3FF)
private val AuraMuted = Color(0xFFB7BBD5)

@Composable
fun AuraScreen(
    voiceState: AuraVoiceState,
    transcript: String,
    message: String,
    chatMessages: List<ChatMessage>,
    assistantRoleHeld: Boolean,
    onVoiceButtonClick: () -> Unit,
    onChatVoiceButtonClick: () -> Unit,
    onSendChatMessage: (String) -> Unit,
    onRequestAssistantRole: () -> Unit
) {
    var showingChat by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val playback = remember {
        AuraPlaybackController(context)
    }

    DisposableEffect(playback) {
        onDispose {
            playback.release()
        }
    }

    LaunchedEffect(playback) {
        while (true) {
            playback.refresh()
            delay(500)
        }
    }

    if (showingChat) {
        AuraChatScreen(
            messages = chatMessages,
            onSendMessage = onSendChatMessage,
            onVoiceButtonClick = onChatVoiceButtonClick,
            onBack = { showingChat = false }
        )
    } else {
        AuraVoiceScreen(
            voiceState = voiceState,
            message = message,
            playback = playback,
            assistantRoleHeld = assistantRoleHeld,
            onVoiceButtonClick = onVoiceButtonClick,
            onChatClick = { showingChat = true },
            onRequestAssistantRole = onRequestAssistantRole
        )
    }
}

@Composable
private fun AuraVoiceScreen(
    voiceState: AuraVoiceState,
    message: String,
    playback: AuraPlaybackController,
    assistantRoleHeld: Boolean,
    onVoiceButtonClick: () -> Unit,
    onChatClick: () -> Unit,
    onRequestAssistantRole: () -> Unit
) {
    val accent = when (voiceState) {
        AuraVoiceState.IDLE -> AuraPurple
        AuraVoiceState.LISTENING -> AuraCyan
        AuraVoiceState.PROCESSING -> AuraPink
        AuraVoiceState.SPEAKING -> AuraBlue
    }

    val status = when (voiceState) {
        AuraVoiceState.IDLE -> "LISTA PARA AYUDARTE"
        AuraVoiceState.LISTENING -> "TE ESTOY ESCUCHANDO"
        AuraVoiceState.PROCESSING -> "ESTOY PENSANDO"
        AuraVoiceState.SPEAKING -> "ESTOY HABLANDO"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AuraBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF191735),
                            Color(0xFF111329),
                            Color(0xFF0B0D1B)
                        )
                    )
                )
                .padding(horizontal = 19.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Encabezado
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "A U R A",
                            color = AuraWhite,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 7.sp
                        )

                        Text(
                            text = "PERSONAL AI ASSISTANT",
                            color = AuraMuted,
                            fontSize = 9.sp,
                            letterSpacing = 2.sp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.07f),
                                CircleShape
                            )
                            .clickable(onClick = onChatClick)
                            .padding(horizontal = 13.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuraIcon(
                            AuraIconType.CHAT,
                            tint = AuraCyan,
                            size = 19.dp
                        )
                        Text(
                            "CHAT",
                            color = AuraWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Esfera y estado
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(310.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            accent.copy(alpha = 0.16f),
                                            AuraPurple.copy(alpha = 0.06f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )

                        AuraOrb(
                            state = voiceState,
                            modifier = Modifier.size(270.dp)
                        )
                    }

                    AnimatedContent(
                        targetState = status,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "aura_home_status"
                    ) { currentStatus ->
                        Text(
                            text = currentStatus,
                            color = AuraWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.1.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = message,
                        color = AuraMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(
                            top = 6.dp,
                            bottom = 9.dp
                        )
                    )
                }

                // Reproductor compacto
                AuraPlayerPanel(playback)

                Spacer(Modifier.height(9.dp))

                // Estado del asistente del sistema
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRequestAssistantRole),
                    color = AuraPanel.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(17.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        AuraPurple.copy(alpha = 0.30f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 13.dp,
                            vertical = 10.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        AuraIcon(
                            AuraIconType.SETTINGS,
                            tint = AuraCyan,
                            size = 21.dp
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "ASISTENTE DE ANDROID",
                                color = AuraWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp
                            )

                            Text(
                                text = if (assistantRoleHeld) {
                                    "Aura está seleccionada como asistente"
                                } else {
                                    "Configura Aura como asistente del sistema"
                                },
                                color = AuraMuted,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }

                        Text(
                            text = if (assistantRoleHeld) "ACTIVA" else "CONFIGURAR",
                            color = if (assistantRoleHeld) AuraCyan else AuraPink,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(9.dp))

                // Controles principales
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeAction(
                        icon = AuraIconType.CHAT,
                        label = "Chat",
                        onClick = onChatClick
                    )

                    Surface(
                        modifier = Modifier
                            .size(65.dp)
                            .clickable(onClick = onVoiceButtonClick),
                        shape = CircleShape,
                        color = AuraPurple,
                        shadowElevation = 9.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            AuraIcon(
                                AuraIconType.MIC,
                                tint = Color.White,
                                size = 29.dp
                            )
                        }
                    }

                    HomeAction(
                        icon = AuraIconType.SETTINGS,
                        label = "Asistente",
                        onClick = onRequestAssistantRole
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeAction(
    icon: AuraIconType,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        AuraIcon(
            icon,
            tint = AuraCyan,
            size = 23.dp
        )

        Text(
            text = label,
            color = AuraMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
