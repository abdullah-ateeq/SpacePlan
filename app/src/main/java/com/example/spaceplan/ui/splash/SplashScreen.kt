package com.example.spaceplan.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.spaceplan.ui.theme.WordmarkStyle
import kotlinx.coroutines.delay

private const val DURATION_MS = 1300
private const val BLOCKS = 4

/** ~1.4 s animation: space line, blocks appear, gaps distribute, remaining marker, title fades in. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
        delay(120)
        onFinished()
    }
    val scheme = MaterialTheme.colorScheme
    fun phase(start: Float, end: Float) =
        FastOutSlowInEasing.transform(((progress.value - start) / (end - start)).coerceIn(0f, 1f))

    Box(
        Modifier
            .fillMaxSize()
            .semantics { contentDescription = "SpacePlan. Plan repeated objects with confidence" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Canvas(
                Modifier
                    .widthIn(max = 280.dp)
                    .fillMaxWidth(0.72f)
                    .height(72.dp),
            ) {
                val line = phase(0f, 0.25f)
                val appear = phase(0.15f, 0.5f)
                val spread = phase(0.45f, 0.8f)
                val remainder = phase(0.7f, 0.9f)
                val stroke = 1.5.dp.toPx()
                val baseY = size.height - 6.dp.toPx()
                val tick = 6.dp.toPx()

                // Available space line with end ticks.
                drawLine(scheme.onSurfaceVariant, Offset(0f, baseY), Offset(size.width * line, baseY), stroke)
                drawLine(scheme.onSurfaceVariant, Offset(0f, baseY - tick), Offset(0f, baseY + tick), stroke)
                if (line >= 1f) {
                    drawLine(scheme.onSurfaceVariant, Offset(size.width, baseY - tick), Offset(size.width, baseY + tick), stroke)
                }

                // Blocks: start packed at the left, then spread to equal gaps across 82% of the line.
                val usedWidth = size.width * 0.82f
                val block = usedWidth / (BLOCKS + (BLOCKS - 1) * 0.5f)
                val evenGap = (usedWidth - block * BLOCKS) / (BLOCKS - 1)
                val packedGap = 2.dp.toPx()
                val gap = packedGap + (evenGap - packedGap) * spread
                val blockHeight = size.height - 22.dp.toPx()
                for (i in 0 until BLOCKS) {
                    val local = ((appear * BLOCKS) - i).coerceIn(0f, 1f)
                    if (local <= 0f) continue
                    val x = i * (block + gap)
                    val h = blockHeight * local
                    drawRoundRect(
                        color = scheme.primary.copy(alpha = 0.85f * local),
                        topLeft = Offset(x, baseY - 4.dp.toPx() - h),
                        size = Size(block, h),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                }

                // Remaining space indicator.
                if (remainder > 0f) {
                    val start = usedWidth + 4.dp.toPx()
                    val end = start + (size.width - start) * remainder
                    val y = baseY - blockHeight / 2 - 4.dp.toPx()
                    drawLine(scheme.tertiary, Offset(start, y), Offset(end, y), stroke)
                    drawLine(scheme.tertiary, Offset(start, y - tick / 2), Offset(start, y + tick / 2), stroke)
                    drawLine(scheme.tertiary, Offset(end, y - tick / 2), Offset(end, y + tick / 2), stroke)
                }
            }
            Column(
                Modifier.alpha(phase(0.6f, 1f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("SPACEPLAN", style = WordmarkStyle.copy(fontSize = WordmarkStyle.fontSize * 1.3f), color = scheme.onBackground)
                Text(
                    "Plan repeated objects with confidence",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
