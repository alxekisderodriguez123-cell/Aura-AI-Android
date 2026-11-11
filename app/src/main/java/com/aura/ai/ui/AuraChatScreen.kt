
package com.aura.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.ai.chat.ChatMessage
import androidx.compose.foundation.layout.size

private val ChatBackground = Color(0xFF101329)
private val ChatPanel = Color(0xFF211D3D)
private val ChatPurple = Color(0xFF9D55FF)
private val ChatPink = Color(0xFFF04FAD)
private val ChatCyan = Color(0xFF39DDF5)
private val ChatWhite = Color(0xFFF2F3FF)
private val ChatMuted = Color(0xFFB7BBD5)

@Composable
fun AuraChatScreen(
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onVoiceButtonClick: () -> Unit,
    onBack: () -> Unit
) {
    var input by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    fun send() {
        val value = input.trim()
        if (value.isBlank()) return

        onSendMessage(value)
        input = ""
        focusManager.clearFocus()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ChatBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1A1736),
                            Color(0xFF111329),
                            Color(0xFF0B0D1B)
                        )
                    )
                )
                .padding(horizontal = 17.dp)
        ) {
            // Barra superior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 13.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clickable(onClick = onBack),
                    shape = CircleShape,
                    color = ChatPanel
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AuraIcon(
                            AuraIconType.BACK,
                            tint = ChatWhite,
                            size = 21.dp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "A U R A",
                        color = ChatWhite,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 5.sp
                    )

                    Text(
                        "CONVERSACIÓN",
                        color = ChatMuted,
                        fontSize = 9.sp,
                        letterSpacing = 1.8.sp
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = ChatPurple.copy(alpha = 0.17f)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 7.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(ChatCyan, CircleShape)
                        )

                        Text(
                            "ONLINE",
                            color = ChatWhite,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            // Historial: el desplazamiento se limita al chat.
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(62.dp),
                            shape = CircleShape,
                            color = ChatPurple.copy(alpha = 0.17f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AuraIcon(
                                    AuraIconType.SPARKLES,
                                    tint = ChatCyan,
                                    size = 29.dp
                                )
                            }
                        }

                        Text(
                            "¿En qué puedo ayudarte?",
                            color = ChatWhite,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            "Escribe un mensaje o háblame.",
                            color = ChatMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(21.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        if (message.isUser) {
                            UserMessage(message.text)
                        } else {
                            AuraMessage(message.text)
                        }
                    }
                }
            }

            // Campo de escritura y acciones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = ChatPanel,
                    shape = RoundedCornerShape(25.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        ChatPurple.copy(alpha = 0.45f)
                    )
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Escribe a Aura...",
                                color = ChatMuted,
                                fontSize = 14.sp
                            )
                        },
                        textStyle = TextStyle(
                            color = ChatWhite,
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        ),
                        maxLines = 4,
                        shape = RoundedCornerShape(25.dp),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { send() }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = ChatWhite,
                            unfocusedTextColor = ChatWhite,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = ChatCyan,
                            focusedPlaceholderColor = ChatMuted,
                            unfocusedPlaceholderColor = ChatMuted
                        )
                    )
                }

                Surface(
                    modifier = Modifier.size(48.dp).clickable(
                        onClick = onVoiceButtonClick
                    ),
                    shape = CircleShape,
                    color = ChatPanel
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AuraIcon(
                            AuraIconType.MIC,
                            tint = ChatCyan,
                            size = 22.dp
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(
                            enabled = input.isNotBlank(),
                            onClick = { send() }
                        ),
                    shape = CircleShape,
                    color = if (input.isNotBlank()) {
                        ChatPurple
                    } else {
                        ChatPanel
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AuraIcon(
                            AuraIconType.SEND,
                            tint = if (input.isNotBlank()) {
                                Color.White
                            } else {
                                ChatMuted.copy(alpha = 0.55f)
                            },
                            size = 21.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMessage(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 310.dp),
            color = Color(0xFF49316F),
            shape = RoundedCornerShape(
                topStart = 21.dp,
                topEnd = 21.dp,
                bottomStart = 21.dp,
                bottomEnd = 6.dp
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                ChatPurple.copy(alpha = 0.35f)
            )
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
            )
        }
    }
}

@Composable
private fun AuraMessage(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 3.dp, end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(25.dp),
                shape = CircleShape,
                color = ChatPurple.copy(alpha = 0.25f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AuraIcon(
                        AuraIconType.SPARKLES,
                        tint = ChatCyan,
                        size = 14.dp
                    )
                }
            }

            Text(
                "AURA",
                color = ChatCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp
            )
        }

        Text(
            text = text,
            color = ChatWhite,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            modifier = Modifier.padding(start = 33.dp)
        )
    }
}
