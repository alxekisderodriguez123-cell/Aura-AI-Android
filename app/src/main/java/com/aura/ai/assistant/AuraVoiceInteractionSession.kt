package com.aura.ai.assistant

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.aura.ai.voice.AuraVoiceState

class AuraVoiceInteractionSession(
    context: Context
) : VoiceInteractionSession(context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

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

        mainHandler.removeCallbacks(restartWakeWordRunnable)
        AuraWakeWordController.stop(appContext)

        overlay?.visibility = View.VISIBLE
        overlay?.startOrbAnimation()
        overlay?.animateEntrance()

        if (runtime == null) {
            runtime = AuraVoiceRuntime(
                context = appContext,

                onStateChanged = { state ->
                    overlay?.setSpeaking(
                        state == AuraVoiceState.SPEAKING
                    )
                },

                onTranscriptChanged = {
                    // La esfera no muestra transcripciones.
                },

                onMessageChanged = {
                    // Aura responde mediante voz.
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

        if (AuraWakeWordPreferences.isEnabled(appContext)) {
            mainHandler.removeCallbacks(restartWakeWordRunnable)

            mainHandler.postDelayed(
                restartWakeWordRunnable,
                700L
            )
        }
    }

    private val restartWakeWordRunnable = Runnable {
        if (
            AuraWakeWordPreferences.isEnabled(appContext)
        ) {
            AuraWakeWordController.start(appContext)
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(restartWakeWordRunnable)

        runtime?.release()
        runtime = null

        overlay?.stopOrbAnimation()
        overlay = null

        super.onDestroy()
    }

    private fun dp(value: Int): Int {
        return (
            value *
                appContext.resources.displayMetrics.density
            ).toInt()
    }
}
