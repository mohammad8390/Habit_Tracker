package com.example.habittracker.ui.components

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

@Composable
fun CelebrationOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        try {
            playProfessionalChime()
        } catch (e: Exception) {
            // Fallback
        }

        launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
            )
        }

        delay(1200)
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        val animVal = progress.value

        // Glowing Expanding Ring & Star Particle Canvas
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer Ripple
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = (1f - animVal) * 0.35f),
                radius = animVal * 240.dp.toPx(),
                center = center
            )

            // Particle Starburst
            val particleCount = 14
            for (i in 0 until particleCount) {
                val angle = (i.toFloat() / particleCount) * 2f * Math.PI.toFloat()
                val dist = animVal * 160.dp.toPx()
                val particleX = center.x + cos(angle) * dist
                val particleY = center.y + sin(angle) * dist
                val particleRadius = (1f - animVal) * 6.dp.toPx()

                drawCircle(
                    color = if (i % 2 == 0) Color(0xFF10B981) else Color(0xFFF59E0B),
                    radius = particleRadius.coerceAtLeast(1f),
                    center = Offset(particleX, particleY)
                )

                if (i % 3 == 0) {
                    drawStar(
                        center = Offset(particleX, particleY),
                        size = (1f - animVal) * 12.dp.toPx(),
                        color = Color(0xFFF59E0B)
                    )
                }
            }
        }

        // Center Floating Badge
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF10B981),
            contentColor = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Habit Completed! ✨",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Keep up the momentum! 🔥",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun DrawScope.drawStar(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        val points = 5
        val outerRadius = size / 2f
        val innerRadius = outerRadius * 0.4f
        var angle = -Math.PI / 2.0

        val step = Math.PI / points
        moveTo(
            (center.x + outerRadius * cos(angle)).toFloat(),
            (center.y + outerRadius * sin(angle)).toFloat()
        )

        for (i in 1 until points * 2) {
            angle += step
            val r = if (i % 2 == 1) innerRadius else outerRadius
            lineTo(
                (center.x + r * cos(angle)).toFloat(),
                (center.y + r * sin(angle)).toFloat()
            )
        }
        close()
    }
    drawPath(path = path, color = color)
}

private fun playProfessionalChime() {
    try {
        val sampleRate = 44100
        val durationMs = 280
        val numSamples = sampleRate * durationMs / 1000
        val buffer = ByteArray(2 * numSamples)

        // Studio Dual-Note Rising Harmony (E5 659.25Hz -> B5 987.77Hz)
        for (i in 0 until numSamples) {
            val timeSec = i.toDouble() / sampleRate
            val freq = if (i < numSamples * 0.35) 659.25 else 987.77
            val decayEnvelope = exp(-timeSec * 7.5)
            val sampleVal = sin(2.0 * Math.PI * freq * timeSec) * decayEnvelope

            val pcmVal = (sampleVal * 32767 * 0.35).toInt().coerceIn(-32768, 32767)
            buffer[2 * i] = (pcmVal and 0x00ff).toByte()
            buffer[2 * i + 1] = (pcmVal shr 8 and 0x00ff).toByte()
        }

        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            buffer.size,
            AudioTrack.MODE_STATIC
        )
        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()

        // Release AudioTrack hardware resource after playback
        CoroutineScope(Dispatchers.IO).launch {
            delay(350L)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // Ignore release error
            }
        }
    } catch (e: Exception) {
        // Fallback tone if AudioTrack is busy
    }
}
