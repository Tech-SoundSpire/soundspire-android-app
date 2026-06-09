package com.example.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CardBackground
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SoundSpireNeonTeal
import com.example.ui.theme.SoundSpireVibrantBlue
import kotlin.random.Random

@Composable
fun VideoPlayer(
    videoUrl: String,
    title: String,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }
    var rawVideoView: VideoView? by remember { mutableStateOf(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    // Use Multiple animations to make visualizer frequencies look extremely dynamic & custom
    val waveOffset1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "w1"
    )
    val waveOffset2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "w2"
    )
    val waveOffset3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "w3"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightBlack)
    ) {
        // Native android VideoView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                VideoView(context).apply {
                    val mediaController = MediaController(context)
                    mediaController.setAnchorView(this)
                    setMediaController(null) // Disable stock float window overlay for premium aesthetics
                    setVideoURI(Uri.parse(videoUrl))
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        rawVideoView = this
                        if (isPlaying) {
                            start()
                        }
                    }
                }
            },
            update = { view ->
                if (isPlaying) {
                    view.start()
                } else {
                    view.pause()
                }
            }
        )

        // Custom Overlay Gradient for premium glassmorphism HUD look
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // Title Info
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Playing Indicator",
                tint = SoundSpireNeonTeal,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        // Animated Studio Frequency Equalizer Overlay (Bottom-right)
        if (isPlaying) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .size(width = 64.dp, height = 36.dp)
            ) {
                val padding = 4.dp.toPx()
                val barCount = 6
                val barWidth = (size.width - (padding * (barCount - 1))) / barCount

                // Draw 6 bouncing EQ columns
                for (i in 0 until barCount) {
                    val multiplier = when (i % 3) {
                        0 -> waveOffset1
                        1 -> waveOffset2
                        else -> waveOffset3
                    }
                    val currentBarHeight = size.height * (0.15f + 0.85f * multiplier * (1f - (i * 0.05f)))
                    val x = i * (barWidth + padding)
                    val y = size.height - currentBarHeight

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(SoundSpireNeonTeal, SoundSpireVibrantBlue)
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, currentBarHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }
        }

        // Studio HUD overlay showing controls (Bottom-left)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    isPlaying = !isPlaying
                    rawVideoView?.let {
                        if (isPlaying) it.start() else it.pause()
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50.dp))
                    .testTag("video_play_toggle")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = SoundSpireNeonTeal,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = if (isPlaying) "Streaming Live" else "Stream Paused",
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        }
    }

    DisposableEffect(videoUrl) {
        onDispose {
            rawVideoView?.stopPlayback()
        }
    }
}
