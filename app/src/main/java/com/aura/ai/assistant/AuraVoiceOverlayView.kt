
package com.aura.ai.assistant

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import com.aura.ai.R

class AuraVoiceOverlayView(
    context: Context,
    private val onClose: () -> Unit
) : LinearLayout(context) {

    private val statusView: TextView
    private val transcriptView: TextView
    private val orbView: ImageView
    private val animatedOrb: AnimatedImageDrawable?

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(22), dp(18), dp(22), dp(22))

        background = GradientDrawable().apply {
            colors = intArrayOf(
                Color.rgb(32, 25, 66),
                Color.rgb(15, 15, 35)
            )
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            cornerRadii = floatArrayOf(
                dp(28).toFloat(), dp(28).toFloat(),
                dp(28).toFloat(), dp(28).toFloat(),
                dp(28).toFloat(), dp(28).toFloat(),
                dp(28).toFloat(), dp(28).toFloat()
            )
            setStroke(dp(1), Color.rgb(112, 67, 190))
        }

        val header = TextView(context).apply {
            text = "A U R A"
            textSize = 13f
            letterSpacing = 0.32f
            setTextColor(Color.rgb(95, 224, 255))
            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.BOLD
            )
            gravity = Gravity.CENTER
        }

        addView(
            header,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(28)
            )
        )

        orbView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            val drawable = loadOrbDrawable()
            setImageDrawable(drawable)
        }

        animatedOrb = orbView.drawable as? AnimatedImageDrawable

        addView(
            orbView,
            LayoutParams(dp(154), dp(154)).apply {
                gravity = Gravity.CENTER
                topMargin = dp(8)
                bottomMargin = dp(8)
            }
        )

        statusView = TextView(context).apply {
            text = "Conectando..."
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )
        }

        addView(
            statusView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(32)
            )
        )

        transcriptView = TextView(context).apply {
            text = "Estoy aquí."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(205, 198, 230))
            maxLines = 3
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }

        addView(
            transcriptView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        val closeButton = TextView(context).apply {
            text = "Cerrar"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(225, 215, 255))
            background = GradientDrawable().apply {
                setColor(Color.rgb(54, 43, 91))
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.rgb(112, 67, 190))
            }
            setPadding(dp(28), dp(10), dp(28), dp(10))
            setOnClickListener { onClose() }
        }

        addView(
            closeButton,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                dp(44)
            ).apply {
                gravity = Gravity.CENTER
                topMargin = dp(12)
            }
        )

        animatedOrb?.let {
            it.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            it.start()
        }
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
        } catch (_: Exception) {
            null
        }
    }

    fun setStatus(value: String) {
        statusView.text = value
    }

    fun setTranscript(value: String) {
        transcriptView.text = value
    }

    fun setSpeaking(speaking: Boolean) {
        orbView.animate().cancel()

        val target = if (speaking) 1.07f else 1f

        orbView.animate()
            .scaleX(target)
            .scaleY(target)
            .setDuration(if (speaking) 420L else 260L)
            .start()
    }

    fun stopOrbAnimation() {
        animatedOrb?.stop()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
