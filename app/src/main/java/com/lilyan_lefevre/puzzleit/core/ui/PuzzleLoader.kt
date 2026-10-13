package com.lilyan_lefevre.puzzleit.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** Four tiles that take turns lifting up: the app's "thinking" indicator. */
@Composable
fun PuzzleLoader(modifier: Modifier = Modifier, size: Dp = 64.dp) {
    // One clock going 0 -> 1 every 1.6 s, forever; each tile reads it with its own delay.
    val t by rememberInfiniteTransition(label = "loader").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    val colors = MaterialTheme.colorScheme.let { listOf(it.primary, it.secondary, it.secondary, it.primary) }
    Canvas(modifier.size(size)) {
        val side = this.size.minDimension / 2f
        val gap = side * 0.08f
        for (i in 0..3) {
            val col = i % 2
            val row = i / 2
            val order = intArrayOf(0, 1, 3, 2)[i] // clockwise turn order
            val p = ((t - order * 0.25f) % 1f + 1f) % 1f
            val lift = if (p < 0.25f) sin(p / 0.25f * PI).toFloat() else 0f
            val half = (side - gap) / 2 * (1f + 0.12f * lift)
            val cx = col * side + side / 2
            val cy = row * side + side / 2 - lift * side * 0.12f
            drawRoundRect(
                colors[i], topLeft = Offset(cx - half, cy - half), size = Size(half * 2, half * 2),
                cornerRadius = CornerRadius(half * 0.38f), alpha = (150 + 105 * lift) / 255f,
            )
        }
    }
}
