package com.aura.ai.assistant

import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.aura.ai.voice.AuraVoiceState

class AuraVoiceInteractionSession(
    context: Context
) : VoiceInteractionSession(context) {

    private val appContext = context.applicationContext

    private var overlay: AuraVoiceOverlayView? = null
    private var runtime: AuraVoiceRuntime? = null

    override fun onCreateContentView(): View {
        val view = AuraVoiceOverlayView(appContext) {
            runtime?.stopConversation()
            hide()
        }

        view.visibility = View.INVISIBLE
        overlay = view

        window?.window?.apply {
            setGravity(Gravity.BOTTOM)

            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            addFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
            )

            setDimAmount(0.12f)
        }

        return view
    }

    override fun onShow(
        args: Bundle?,
        showFlags: Int
    ) {
        super.onShow(args, showFlags)

        overlay?.visibility = View.VISIBLE

        overlay?.setStatus("Te estoy escuchando")
        overlay?.setTranscript("Dime, ¿en qué te ayudo?")

        if (runtime == null) {
            runtime = AuraVoiceRuntime(
                context = appContext,

                onStateChanged = { state ->
                    overlay?.setSpeaking(
                        state == AuraVoiceState.SPEAKING
                    )
                },

                onTranscriptChanged = { text ->
                    if (text.isNotBlank()) {
                        overlay?.setTranscript(text)
                    }
                },

                onMessageChanged = { message ->
                    overlay?.setStatus(message)
                }
            )
        }

        runtime?.startConversation()
    }

    override fun onHide() {
        runtime?.stopConversation()

        overlay?.visibility = View.INVISIBLE
        overlay?.setSpeaking(false)

        super.onHide()
    }

    override fun onDestroy() {
        runtime?.release()
        runtime = null

        overlay?.stopOrbAnimation()
        overlay = null

        super.onDestroy()
    }
}
