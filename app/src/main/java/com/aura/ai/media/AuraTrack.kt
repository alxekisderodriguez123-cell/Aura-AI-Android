package com.aura.ai.media

import android.net.Uri

data class AuraTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val uri: Uri,
    val duration: Long
)
