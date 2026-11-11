
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

private val PlayerPanel = Color(0xFF211D3D)
private val PlayerPurple = Color(0xFF9D55FF)
private val PlayerPink = Color(0xFFF04FAD)
private val PlayerCyan = Color(0xFF39DDF5)
private val PlayerText = Color(0xFFF2F3FF)
private val PlayerMuted = Color(0xFFB7BBD5)

@Composable
fun AuraPlayerPanel(
    playback: AuraPlaybackController
) {
    var showQueue by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PlayerPanel.copy(alpha = 0.94f),
        shape = RoundedCornerShape(19.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            PlayerPurple.copy(alpha = 0.32f)
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 9.dp
            ),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AuraAlbumArtwork(
                    uri = playback.artworkUri,
                    modifier = Modifier.size(49.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = playback.title,
                        color = PlayerText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = playback.artist,
                        color = PlayerMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = if (playback.isPlaying) {
                            "REPRODUCIENDO"
                        } else {
                            "AURA MUSIC"
                        },
                        color = PlayerCyan,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                PlayerButton(
                    icon = AuraIconType.PREVIOUS,
                    onClick = playback::previous
                )

                Surface(
                    modifier = Modifier
                        .size(39.dp)
                        .clickable(
                            enabled = playback.queue.isNotEmpty(),
                            onClick = playback::togglePlayback
                        ),
                    color = PlayerPurple,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AuraIcon(
                            if (playback.isPlaying) {
                                AuraIconType.PAUSE
                            } else {
                                AuraIconType.PLAY
                            },
                            tint = Color.White,
                            size = 19.dp
                        )
                    }
                }

                PlayerButton(
                    icon = AuraIconType.NEXT,
                    onClick = playback::next
                )
            }

            Slider(
                value = if (playback.duration > 0L) {
                    (playback.position.toFloat() /
                        playback.duration.toFloat()).coerceIn(0f, 1f)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 1.dp),
                colors = SliderDefaults.colors(
                    thumbColor = PlayerCyan,
                    activeTrackColor = PlayerPurple,
                    inactiveTrackColor = Color.White.copy(alpha = 0.17f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatTime(playback.position),
                    color = PlayerMuted,
                    fontSize = 9.sp
                )

                Spacer(Modifier.weight(1f))

                Box {
                    Row(
                        modifier = Modifier
                            .clickable { showQueue = true }
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        AuraIcon(
                            AuraIconType.MUSIC,
                            tint = PlayerCyan,
                            size = 15.dp
                        )

                        Text(
                            "${playback.queue.size} pistas",
                            color = PlayerText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    DropdownMenu(
                        expanded = showQueue,
                        onDismissRequest = { showQueue = false }
                    ) {
                        if (playback.queue.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("La cola está vacía") },
                                onClick = { showQueue = false }
                            )
                        } else {
                            playback.queue.forEachIndexed { index, item ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                item.title,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                item.artist,
                                                fontSize = 11.sp,
                                                color = PlayerMuted,
                                                maxLines = 1
                                            )
                                        }
                                    },
                                    onClick = {
                                        playback.playQueueItem(index)
                                        showQueue = false
                                    }
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
private fun PlayerButton(
    icon: AuraIconType,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(31.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            AuraIcon(
                icon,
                tint = PlayerText,
                size = 16.dp
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
                        ?.use { BitmapFactory.decodeStream(it) }
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = PlayerPanel
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Carátula del álbum",
                modifier = Modifier.size(49.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.background(
                    Brush.linearGradient(
                        listOf(
                            PlayerPurple,
                            PlayerPink,
                            PlayerCyan
                        )
                    )
                ),
                contentAlignment = Alignment.Center
            ) {
                AuraIcon(
                    AuraIconType.MUSIC,
                    tint = Color.White,
                    size = 25.dp
                )
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}
