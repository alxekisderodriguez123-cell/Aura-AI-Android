
package com.aura.ai.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class AuraVoiceSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                AuraVoiceSettingsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
private fun AuraVoiceSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var modelInstalled by remember {
        mutableStateOf(
            AuraWakeWordModelInstaller.isModelInstalled(context)
        )
    }

    var listeningEnabled by remember {
        mutableStateOf(
            AuraWakeWordPreferences.isEnabled(context)
        )
    }

    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var progressMessage by remember {
        mutableStateOf("Modelo español de Vosk")
    }

    var statusMessage by remember {
        mutableStateOf("Configura la escucha de Aura.")
    }

    var microphoneGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun refreshStatus() {
        modelInstalled =
            AuraWakeWordModelInstaller.isModelInstalled(context)

        microphoneGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        notificationsGranted =
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

        listeningEnabled =
            AuraWakeWordPreferences.isEnabled(context)
    }

    fun startIfReady() {
        refreshStatus()

        if (!modelInstalled) {
            statusMessage = "Primero debes instalar el modelo español."
            return
        }

        if (!microphoneGranted) {
            statusMessage = "Falta el permiso del micrófono."
            return
        }

        if (!notificationsGranted) {
            statusMessage =
                "Concede permiso de notificaciones para ver el estado de escucha."
            return
        }

        AuraWakeWordPreferences.setEnabled(context, true)
        listeningEnabled = true
        AuraWakeWordController.start(context)

        statusMessage = "Solicitud de escucha enviada a Aura."
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            notificationsGranted = granted

            if (granted) {
                startIfReady()
            } else {
                statusMessage =
                    "Sin notificaciones, Android puede ocultar el aviso de escucha."
            }
        }

    val microphonePermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            microphoneGranted = granted

            if (granted) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    !notificationsGranted
                ) {
                    notificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } else {
                    startIfReady()
                }
            } else {
                statusMessage =
                    "Aura necesita permiso de micrófono para detectar la palabra de activación."
            }
        }

    LaunchedEffect(Unit) {
        refreshStatus()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF101329)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101329))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Text(
                text = "AURA",
                color = Color(0xFFF2F3FF),
                fontSize = 25.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 6.sp
            )

            Text(
                text = "ESCUCHA Y RECONOCIMIENTO DE VOZ",
                color = Color(0xFF39DDF5),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            VoiceSettingsCard {
                Text(
                    "Reconocimiento sin conexión",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (modelInstalled) {
                        "Modelo español instalado y verificado."
                    } else {
                        "El modelo español todavía no está instalado."
                    },
                    color = Color(0xFFB7BBD5),
                    fontSize = 13.sp
                )

                if (downloading) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF39DDF5),
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )

                    Text(
                        "$progress% · $progressMessage",
                        color = Color(0xFFB7BBD5),
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = {
                        if (downloading) return@Button

                        downloading = true
                        progress = 0
                        progressMessage = "Preparando descarga…"
                        statusMessage = "Descargando el modelo español…"

                        AuraWakeWordModelInstaller.installModel(
                            context = context,
                            onProgress = { value, message ->
                                progress = value
                                progressMessage = message
                            },
                            onComplete = { directory, error ->
                                downloading = false
                                modelInstalled = directory != null

                                if (directory != null) {
                                    progress = 100
                                    statusMessage =
                                        "Modelo instalado correctamente."

                                    refreshStatus()

                                    if (listeningEnabled) {
                                        if (microphoneGranted &&
                                            notificationsGranted
                                        ) {
                                            startIfReady()
                                        } else {
                                            statusMessage =
                                                "Modelo instalado. Concede los permisos para activar la escucha."
                                        }
                                    }
                                } else {
                                    statusMessage =
                                        error ?: "Falló la instalación del modelo."
                                }
                            }
                        )
                    },
                    enabled = !downloading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7146D9)
                    )
                ) {
                    Text(
                        if (modelInstalled) {
                            "Comprobar / reinstalar modelo"
                        } else {
                            "Descargar modelo español"
                        }
                    )
                }
            }

            VoiceSettingsCard {
                Text(
                    "Escucha continua",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    "Aura detectará «Hey Aura», «Ey Aura», «Oye Aura» y «Hola Aura» mientras el servicio esté activo.",
                    color = Color(0xFFB7BBD5),
                    fontSize = 13.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (listeningEnabled) "Activada" else "Pausada",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )

                    Switch(
                        checked = listeningEnabled,
                        onCheckedChange = { enabled ->
                            listeningEnabled = enabled
                            AuraWakeWordPreferences.setEnabled(
                                context,
                                enabled
                            )

                            if (enabled) {
                                refreshStatus()

                                when {
                                    !modelInstalled ->
                                        statusMessage =
                                            "Descarga primero el modelo español."

                                    !microphoneGranted ->
                                        microphonePermissionLauncher.launch(
                                            Manifest.permission.RECORD_AUDIO
                                        )

                                    Build.VERSION.SDK_INT >= 33 &&
                                        !notificationsGranted ->
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )

                                    else -> startIfReady()
                                }
                            } else {
                                AuraWakeWordController.stop(context)
                                statusMessage = "Escucha continua pausada."
                            }
                        }
                    )
                }

                Button(
                    onClick = {
                        when {
                            !microphoneGranted ->
                                microphonePermissionLauncher.launch(
                                    Manifest.permission.RECORD_AUDIO
                                )

                            Build.VERSION.SDK_INT >= 33 &&
                                !notificationsGranted ->
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )

                            else -> startIfReady()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF246E8A)
                    )
                ) {
                    Text("Conceder permisos e iniciar")
                }

                Button(
                    onClick = {
                        refreshStatus()

                        if (modelInstalled &&
                            microphoneGranted &&
                            notificationsGranted
                        ) {
                            startIfReady()
                        } else {
                            statusMessage =
                                "Revisa el modelo y los permisos antes de iniciar."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF30234F)
                    )
                ) {
                    Text("Reintentar escucha")
                }
            }

            VoiceSettingsCard {
                Text(
                    "Estado del dispositivo",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                StatusLine("Modelo español", modelInstalled)
                StatusLine("Micrófono", microphoneGranted)
                StatusLine("Notificaciones", notificationsGranted)
            }

            Text(
                text = statusMessage,
                color = Color(0xFF39DDF5),
                fontSize = 12.sp
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF30234F)
                )
            ) {
                Text("Volver a Aura")
            }

            Text(
                text = "La escucha depende de los permisos y de las políticas de ejecución de Android/XOS.",
                color = Color(0xFF858BAA),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun VoiceSettingsCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color = Color(0xFF211D3D),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFF9D55FF).copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    ready: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Color(0xFFB7BBD5),
            fontSize = 13.sp
        )

        Text(
            if (ready) "Concedido" else "Pendiente",
            color = if (ready) Color(0xFF39DDF5) else Color(0xFFF04FAD),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
