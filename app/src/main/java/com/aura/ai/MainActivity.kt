package com.aura.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                AuraScreen()
            }
        }
    }
}

@Composable
private fun AuraScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF080A16)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF10152B),
                            Color(0xFF080A16),
                            Color(0xFF090B18)
                        )
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "A U R A",
                color = Color(0xFFE9F8FF),
                fontSize = 27.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 9.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "YOUR ANDROID ASSISTANT",
                color = Color(0xFF8998B8),
                fontSize = 10.sp,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(75.dp))

            AuraOrb()

            Spacer(modifier = Modifier.height(65.dp))

            Text(
                text = "Aura está preparada",
                color = Color(0xFFDDEBFF),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = "Tu asistente, integrado en tu mundo.",
                color = Color(0xFF8792AD),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AuraOrb() {
    Canvas(
        modifier = Modifier.size(230.dp)
    ) {
        val center = Offset(
            x = size.width / 2f,
            y = size.height / 2f
        )

        val radius = size.minDimension * 0.43f

        drawCircle(
            color = Color(0xFF637CFF).copy(alpha = 0.12f),
            radius = radius * 1.35f,
            center = center
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE7FFFF),
                    Color(0xFF8DEBFF),
                    Color(0xFF687BFF),
                    Color(0xFF443D91),
                    Color(0xFF151C3D)
                ),
                center = Offset(
                    x = center.x - radius * 0.25f,
                    y = center.y - radius * 0.35f
                ),
                radius = radius * 1.7f
            ),
            radius = radius,
            center = center
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.75f),
                    Color.White.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = Offset(
                    x = center.x - radius * 0.38f,
                    y = center.y - radius * 0.45f
                ),
                radius = radius * 0.8f
            ),
            radius = radius,
            center = center
        )
    }
}
