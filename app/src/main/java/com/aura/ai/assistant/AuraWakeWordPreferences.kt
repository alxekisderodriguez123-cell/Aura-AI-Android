package com.aura.ai.assistant

import android.content.Context

object AuraWakeWordPreferences {

    private const val FILE_NAME = "aura_wake_word"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(
            FILE_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(KEY_ENABLED, true)
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean
    ) {
        context.getSharedPreferences(
            FILE_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}
