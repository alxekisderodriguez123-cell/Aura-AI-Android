package com.aura.ai.assistant

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import com.aura.ai.MainActivity

class AuraVoiceInteractionSession(
    context: android.content.Context
) : VoiceInteractionSession(context) {

    override fun onShow(
        args: Bundle?,
        showFlags: Int
    ) {
        super.onShow(args, showFlags)

        val intent = Intent(
            context,
            MainActivity::class.java
        ).apply {
            action = MainActivity.ACTION_ASSISTANT_INVOCATION

            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )

            putExtra(
                MainActivity.EXTRA_START_LISTENING,
                true
            )
        }

        startAssistantActivity(intent)
    }
}
