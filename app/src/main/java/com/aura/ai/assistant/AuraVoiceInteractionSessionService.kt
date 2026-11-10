package com.aura.ai.assistant

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class AuraVoiceInteractionSessionService :
    VoiceInteractionSessionService() {

    override fun onNewSession(
        args: Bundle?
    ): VoiceInteractionSession {
        return AuraVoiceInteractionSession(this)
    }
}
