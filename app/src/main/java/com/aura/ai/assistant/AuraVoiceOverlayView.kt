package com.aura.ai.assistant

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.aura.ai.R
import kotlin.math.abs

class AuraVoiceOverlayView(
    context: Context,
    private val onTapWhileSpeaking: () -> Unit,
    private val onClose: () -> Unit
) : FrameLayout(context) {

    private val orbView: ImageView
    private val animatedOrb: AnimatedImageDrawable?

    private var isSpeaking = false
    private var downX = 0f
    private var downY = 0f
    private var pulseAnimator: ValueAnimator? = null

    private val swipeThreshold =
        55f * resources.displayMetrics.density

    init {
        setBackgroundColor(Color.TRANSPARENT)
        clipChildren = false
        clipToPadding = false

        orbView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(loadOrbDrawable())
            contentDescription = "Aura"
            isClickable = true
            isFocusable = true
        }

        addView(
            orbView,
            LayoutParams(
                dp(126),
                dp(126),
                Gravity.CENTER
            )
        )

        animatedOrb =
            orbView.drawable as? AnimatedImageDrawable

        orbView.setOnTouchListener { view, event ->
            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val deltaX = event.rawX - downX
                    val deltaY = event.rawY - downY

                    val horizontalSwipe =
                        abs(deltaX) > swipeThreshold

                    val downwardSwipe =
                        deltaY > swipeThreshold

                    if (horizontalSwipe || downwardSwipe) {
                        onClose()
                    } else {
                        view.performClick()
                    }

                    true
                }

                MotionEvent.ACTION_CANCEL -> true

                else -> true
            }
        }

        orbView.setOnClickListener {
            if (isSpeaking) {
                onTapWhileSpeaking()
            }
        }

        val closeButton = TextView(context).apply {
            text = "×"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            contentDescription = "Cerrar Aura"

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(185, 30, 25, 48))
                setStroke(
                    dp(1),
                    Color.argb(150, 150, 115, 220)
                )
            }

            setOnClickListener {
                onClose()
            }
        }

        addView(
            closeButton,
            LayoutParams(
                dp(32),
                dp(32),
                Gravity.TOP or Gravity.END
            ).apply {
                topMargin = dp(5)
                marginEnd = dp(5)
            }
        )

        startOrbAnimation()
    }

    private fun loadOrbDrawable(): Drawable? {
        return try {
            if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeDrawable(
                    ImageDecoder.createSource(
                        resources,
                        R.raw.aura_orb
                    )
                )
            } else {
                val bitmap = BitmapFactory.decodeResource(
                    resources,
                    R.raw.aura_orb
                )
                BitmapDrawable(resources, bitmap)
            }
        } catch (error: Exception) {
            android.util.Log.e(
                "AuraOverlay",
                "No se pudo cargar la esfera",
                error
            )
            null
        }
    }

    fun setSpeaking(speaking: Boolean) {
        if (isSpeaking == speaking) return

        isSpeaking = speaking

        pulseAnimator?.cancel()
        pulseAnimator = null

        if (speaking) {
            pulseAnimator = ValueAnimator.ofFloat(
                1f,
                1.085f
            ).apply {
                duration = 480L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE

                addUpdateListener { animator ->
                    val scale = animator.animatedValue as Float
                    orbView.scaleX = scale
                    orbView.scaleY = scale
                }

                start()
            }
        } else {
            orbView.scaleX = 1f
            orbView.scaleY = 1f
        }
    }

    fun startOrbAnimation() {
        if (
            Build.VERSION.SDK_INT >= 28 &&
            animatedOrb != null &&
            !animatedOrb.isRunning
        ) {
            animatedOrb.repeatCount =
                AnimatedImageDrawable.REPEAT_INFINITE
            animatedOrb.start()
        }
    }

    fun stopOrbAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null

        animatedOrb?.stop()

        orbView.animate().cancel()
        orbView.scaleX = 1f
        orbView.scaleY = 1f
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
