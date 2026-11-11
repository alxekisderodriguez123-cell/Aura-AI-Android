package com.aura.ai.assistant

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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

        val view = AuraVoiceOverlayView(
            context = appContext,

            onTapWhileSpeaking = {
                runtime?.interruptSpeechAndListen()
            },

            onClose = {
                runtime?.stopConversation()
                hide()
            }
        )

        overlay = view

        window?.window?.apply {

            setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            clearFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
            )

            addFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            )

            setGravity(
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            )

            setLayout(
                dp(148),
                dp(148)
            )

        }

        return view
    }

    override fun onShow(
        args: Bundle?,
        showFlags: Int
    ) {
        super.onShow(args, showFlags)

        overlay?.visibility = View.VISIBLE
        overlay?.startOrbAnimation()

        if (runtime == null) {

            runtime = AuraVoiceRuntime(
                context = appContext,

                onStateChanged = { state ->
                    overlay?.setSpeaking(
                        state == AuraVoiceState.SPEAKING
                    )
                },

                onTranscriptChanged = {
                    // No se muestra texto en la esfera flotante.
                },

                onMessageChanged = {
                    // Las respuestas se reproducen por voz.
                }
            )
        }

        runtime?.startConversation()
    }

    override fun onHide() {

        runtime?.stopConversation()

        overlay?.setSpeaking(false)
        overlay?.visibility = View.INVISIBLE
        overlay?.stopOrbAnimation()

        super.onHide()
    }

    override fun onDestroy() {

        runtime?.release()
        runtime = null

        overlay?.stopOrbAnimation()
        overlay = null

        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * appContext.resources.displayMetrics.density).toInt()
}
