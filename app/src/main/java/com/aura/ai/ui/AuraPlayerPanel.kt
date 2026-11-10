package com.aura.ai.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.ai.media.AuraPlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AuraPlayerPanel(
    playback: AuraPlaybackController
) {
    var showQueue by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.065f),
        shape = RoundedCornerShape(27.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.13f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AURA MUSIC",
                    color = Color(0xFF9BBEFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = if (playback.isPlaying) {
                        "REPRODUCIENDO"
                    } else {
                        "REPRODUCTOR"
                    },
                    color = Color(0xFF9BAAC7),
                    fontSize = 8.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(15.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AuraAlbumArtwork(
                    uri = playback.artworkUri,
                    modifier = Modifier.size(84.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = playback.title,
                        color = Color(0xFFF2F6FF),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = playback.artist,
                        color = Color(0xFFB0BED8),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (playback.album.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))

                        Text(
                            text = playback.album,
                            color = Color(0xFF7889AB),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Slider(
                value = if (playback.duration > 0L) {
                    playback.position
                        .toFloat()
                        .div(playback.duration.toFloat())
                        .coerceIn(0f, 1f)
                } else {
                    0f
                },
                onValueChange = { fraction ->
                    if (playback.duration > 0L) {
                        playback.seekTo(
                            (fraction * playback.duration).toLong()
                        )
                    }
                },
                enabled = playback.duration > 0L,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFB8C7FF),
                    activeTrackColor = Color(0xFF8B9FFF),
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(playback.position),
                    color = Color(0xFF91A1C0),
                    fontSize = 10.sp
                )

                Text(
                    text = formatTime(playback.duration),
                    color = Color(0xFF91A1C0),
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerControl(
                    symbol = "PREV",
                    onClick = playback::previous
                )

                Spacer(Modifier.size(24.dp))

                Surface(
                    modifier = Modifier
                        .size(55.dp)
                        .clickable(
                            enabled = playback.queue.isNotEmpty(),
                            onClick = playback::togglePlayback
                        ),
                    shape = CircleShape,
                    color = Color(0xFFB8C7FF)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (playback.isPlaying) "Ⅱ" else "▶",
                            color = Color(0xFF10172D),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.size(24.dp))

                PlayerControl(
                    symbol = "NEXT",
                    onClick = playback::next
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showQueue = !showQueue
                    }
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COLA DE REPRODUCCIÓN",
                    color = Color(0xFFB5C8F5),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = "${playback.queue.size} pistas  ${if (showQueue) "−" else "+"}",
                    color = Color(0xFF91A3C6),
                    fontSize = 10.sp
                )
            }

            if (showQueue && playback.queue.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    playback.queue.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playback.playQueueItem(index)
                                }
                                .background(
                                    if (index == playback.currentIndex) {
                                        Color(0xFF788DFF).copy(alpha = 0.17f)
                                    } else {
                                        Color.Transparent
                                    },
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = 9.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (index == playback.currentIndex) {
                                    "▶"
                                } else {
                                    "${index + 1}"
                                },
                                color = Color(0xFF9DB5FF),
                                fontSize = 10.sp
                            )

                            Spacer(Modifier.size(10.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = item.title,
                                    color = Color(0xFFEAF0FF),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = item.artist,
                                    color = Color(0xFF91A0BD),
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerControl(
    symbol: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.07f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                color = Color(0xFFD9E5FF),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AuraAlbumArtwork(
    uri: Uri?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = uri
    ) {
        value = withContext(Dispatchers.IO) {
            try {
                if (uri == null) {
                    null
                } else {
                    context.contentResolver
                        .openInputStream(uri)
                        ?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        color = Color(0xFF202D52)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Carátula del álbum",
                modifier = Modifier.size(84.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF586BCA),
                                Color(0xFF9B55B9),
                                Color(0xFFEC7198)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♫",
                    color = Color.White,
                    fontSize = 36.sp
                )
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L

    return "%d:%02d".format(minutes, seconds)
}
