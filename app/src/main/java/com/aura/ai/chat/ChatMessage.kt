
package com.aura.ai.chat

data class ChatMessage(
    val id: Long = 0L,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
