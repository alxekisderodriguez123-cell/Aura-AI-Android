
package com.aura.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.ai.chat.ChatMessage

@Composable
fun AuraChatScreen(
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    var input by remember {
        mutableStateOf("")
    }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(
                messages.lastIndex
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF080B19)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF131C38),
                            Color(0xFF0B1022),
                            Color(0xFF070A16)
                        )
                    )
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 18.dp
                )
        ) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "A U R A",
                        color = Color(0xFFEAF5FF),
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 6.sp
                    )

                    Text(
                        text = "CONVERSACIONES",
                        color = Color(0xFF8296BC),
                        fontSize = 9.sp,
                        letterSpacing = 2.sp
                    )
                }

                TextButton(
                    onClick = onBack
                ) {
                    Text(
                        text = "← VOLVER",
                        color = Color(0xFF9DBBFF),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Conversation
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Hablemos.",
                            color = Color(0xFFEAF5FF),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Tu espacio de conversación con Aura.",
                            color = Color(0xFF899AB9),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 9.dp)
                        )

                        Text(
                            text = "Escribe tu primer mensaje.",
                            color = Color(0xFF687B9F),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 22.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->

                        ChatBubble(message = message)
                    }
                }
            }

            // Composer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Escribe a Aura...",
                            color = Color(0xFF8491AE),
                            fontSize = 13.sp
                        )
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp)
                )

                Button(
                    onClick = {
                        val message = input.trim()

                        if (message.isNotEmpty()) {
                            onSendMessage(message)
                            input = ""
                        }
                    },
                    enabled = input.isNotBlank(),
                    modifier = Modifier
                        .widthIn(min = 48.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7189FF),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "↑",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "LOS MENSAJES SE GUARDAN EN ESTE DISPOSITIVO",
                color = Color(0xFF586887),
                fontSize = 8.sp,
                letterSpacing = 1.1.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 5.dp)
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Column(
            modifier = Modifier.widthIn(max = 310.dp),
            horizontalAlignment = if (message.isUser) {
                Alignment.End
            } else {
                Alignment.Start
            }
        ) {
            Text(
                text = if (message.isUser) "TÚ" else "AURA",
                color = if (message.isUser) {
                    Color(0xFF9CB5FF)
                } else {
                    Color(0xFF80E8F3)
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.8.sp,
                modifier = Modifier.padding(
                    horizontal = 9.dp,
                    vertical = 5.dp
                )
            )

            Surface(
                color = if (message.isUser) {
                    Color(0xFF536FE0).copy(alpha = 0.28f)
                } else {
                    Color(0xFF202943).copy(alpha = 0.92f)
                },
                shape = RoundedCornerShape(
                    topStart = 21.dp,
                    topEnd = 21.dp,
                    bottomStart = if (message.isUser) 21.dp else 6.dp,
                    bottomEnd = if (message.isUser) 6.dp else 21.dp
                )
            ) {
                Text(
                    text = message.text,
                    color = Color(0xFFEAF2FF),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 13.dp
                    )
                )
            }
        }
    }
}
