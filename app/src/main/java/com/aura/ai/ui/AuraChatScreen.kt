
package com.aura.ai.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import com.aura.ai.chat.ChatMessage

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
        if (value.isNotEmpty()) {
            onSendMessage(value)
            input = ""
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
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
                        listOf(
                            Color(0xFF151F3D),
                            Color(0xFF0B1228),
                            Color(0xFF070A16)
                        )
                    )
                )
                .padding(horizontal = 17.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "A U R A",
                        color = Color(0xFFF0F7FF),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 6.sp
                    )
                    Text(
                        text = "CONVERSACIONES",
                        color = Color(0xFF9BAFD2),
                        fontSize = 9.sp,
                        letterSpacing = 2.sp
                    )
                }

                TextButton(onClick = onBack) {
                    Text(
                        text = "←  VOLVER",
                        color = Color(0xFFB8CCFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Hablemos.",
                            color = Color(0xFFF0F6FF),
                            fontSize = 29.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Puedes escribir o hablar conmigo.",
                            color = Color(0xFF9BAECC),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(17.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        ChatBubble(message)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF151D35),
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFF788BFF).copy(alpha = 0.65f)
                    )
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Escribe tu mensaje...",
                                color = Color(0xFF9EACC8),
                                fontSize = 14.sp
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color(0xFFF4F7FF),
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        ),
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { send() }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFF4F7FF),
                            unfocusedTextColor = Color(0xFFF4F7FF),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF9CB5FF),
                            focusedPlaceholderColor = Color(0xFF9EACC8),
                            unfocusedPlaceholderColor = Color(0xFF9EACC8)
                        )
                    )
                }

                Button(
                    onClick = onVoiceButtonClick,
                    modifier = Modifier.widthIn(min = 54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF26365B),
                        contentColor = Color(0xFFBDEEFF)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 11.dp,
                        vertical = 17.dp
                    )
                ) {
                    Text(
                        text = "MIC",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { send() },
                    enabled = input.isNotBlank(),
                    modifier = Modifier.widthIn(min = 62.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7189FF),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF303A59),
                        disabledContentColor = Color(0xFF8490AC)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 17.dp
                    )
                ) {
                    Text(
                        text = "ENVIAR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "AURA · CONVERSACIÓN LOCAL",
                color = Color(0xFF71809F),
                fontSize = 8.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (message.isUser) {
                Alignment.End
            } else {
                Alignment.Start
            }
        ) {
            Text(
                text = if (message.isUser) "TÚ" else "AURA",
                color = if (message.isUser) {
                    Color(0xFFB5C7FF)
                } else {
                    Color(0xFF80E8F3)
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.7.sp,
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 5.dp
                )
            )

            Surface(
                color = if (message.isUser) {
                    Color(0xFF344B91)
                } else {
                    Color(0xFF202D4A)
                },
                shape = RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 22.dp,
                    bottomStart = if (message.isUser) 22.dp else 7.dp,
                    bottomEnd = if (message.isUser) 7.dp else 22.dp
                )
            ) {
                Text(
                    text = message.text,
                    color = Color(0xFFF1F6FF),
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    modifier = Modifier.padding(
                        horizontal = 17.dp,
                        vertical = 14.dp
                    )
                )
            }
        }
    }
}
