
package com.aura.ai.assistant

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.aura.ai.MainActivity

class AuraAssistActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launchIntent = Intent(
            this,
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

        startActivity(launchIntent)
        finish()
    }
}
