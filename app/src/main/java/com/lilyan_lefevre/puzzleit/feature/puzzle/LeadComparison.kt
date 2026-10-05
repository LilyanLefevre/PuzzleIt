package com.lilyan_lefevre.puzzleit.feature.puzzle

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.feature.recognition.Candidate
import com.lilyan_lefevre.puzzleit.feature.recognition.Grid
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * The box around a lead with the piece blinking over it, at the box's scale and turned the way the lead says:
 * if the lead is right, the picture continues across the piece. A tap freezes or resumes the blinking.
 *
 * [piece] is the cut-out piece (transparent outside) as photographed; the lead says how far to turn it.
 */
@Composable
fun LeadComparison(box: Bitmap, grid: Grid, lead: Candidate, piece: Bitmap, modifier: Modifier = Modifier) {
    val boxImage = remember(box) { box.asImageBitmap() }
    val sprite = remember(piece) { PieceSprite(piece) }
    val pieceImage = remember(piece) { piece.asImageBitmap() }
    var frozen by remember { mutableStateOf(false) }
    // One clock 0 -> 1 every 2.6 s; the piece's opacity follows a cosine of it: invisible, visible, invisible...
    val phase by rememberInfiniteTransition(label = "blink").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "phase",
    )
    val background = MaterialTheme.colorScheme.background
    val accent = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.compare_description)

    Canvas(
        modifier.clip(RoundedCornerShape(12.dp)).clickable { frozen = !frozen }.semantics { contentDescription = description },
    ) {
        val cw = box.width / grid.cols.toFloat()
        val ch = box.height / grid.rows.toFloat()
        val leadX = lead.col * cw
        val leadY = lead.row * ch
        drawRect(background)
        // A window 2.4 cells tall, centred on the lead, scaled to the view.
        val k = size.height / (ch * 2.4f)
        withTransform({
            translate(size.width / 2f, size.height / 2f)
            scale(k, k, pivot = Offset.Zero)
            translate(-leadX, -leadY)
        }) {
            drawImage(boxImage)
            drawRect(accent, Offset(leadX - cw / 2, leadY - ch / 2), Size(cw, ch), style = Stroke(2.dp.toPx() / k))
            val alpha = if (frozen) 0.6f else 0.5f - 0.5f * cos(2 * PI.toFloat() * phase)
            val s = sqrt(cw * ch) / sprite.side
            withTransform({
                translate(leadX, leadY)
                rotate(lead.rotationDeg.toFloat(), pivot = Offset.Zero)
                scale(s, s, pivot = Offset.Zero)
                translate(-sprite.cx, -sprite.cy)
            }) { drawImage(pieceImage, alpha = alpha * 240 / 255f) }
        }
    }
}
