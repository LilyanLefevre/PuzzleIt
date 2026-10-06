package com.lilyan_lefevre.puzzleit.feature.puzzle.capture

import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.RoundIconButton
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleColors
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleText
import kotlin.math.cos
import kotlin.math.min

/**
 * The camera: live preview, the viewfinder that says where to put the piece, close and light buttons, the shutter.
 *
 * The preview itself is CameraX's [PreviewView] (a View, wrapped with [AndroidView]); the route gets it through
 * [onPreviewReady] and binds the camera to it. [flash] is the white veil that blinks when the photo is taken.
 */
@Composable
fun PieceCaptureScreen(
    onPreviewReady: (PreviewView) -> Unit,
    torchOn: Boolean,
    shutterEnabled: Boolean,
    flash: Float,
    onClose: () -> Unit,
    onTorch: () -> Unit,
    onShutter: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx -> PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER; onPreviewReady(this) } },
            modifier = Modifier.fillMaxSize().testTag("previewView"),
        )
        ScanFrame(Modifier.fillMaxSize())

        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(R.drawable.ic_close, stringResource(R.string.cancel), onClose, Modifier.testTag("buttonClose"), iconSize = 24.dp)
            Box(Modifier.weight(1f))
            FilledTonalButton(
                onTorch, Modifier.alpha(if (torchOn) 1f else 0.6f).testTag("buttonTorch"),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = PuzzleColors.scrim, contentColor = MaterialTheme.colorScheme.onSurface),
            ) {
                Icon(painterResource(R.drawable.ic_bolt), contentDescription = null, Modifier.size(20.dp))
                Text(stringResource(R.string.light), Modifier.padding(start = 8.dp))
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.scan_hint_title), style = PuzzleText.display(22), color = MaterialTheme.colorScheme.onSurface)
            Text(
                stringResource(R.string.scan_hint_body), Modifier.padding(horizontal = 32.dp, vertical = 4.dp),
                fontSize = 14.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Shutter(shutterEnabled, onShutter, Modifier.padding(top = 24.dp))
        }

        // White veil: the flash when the picture is taken.
        Box(Modifier.fillMaxSize().alpha(flash).background(Color.White))
    }
}

/** 84 dp ring with a citron disc inside; it shrinks a little while pressed. */
@Composable
private fun Shutter(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, tween(if (pressed) 90 else 160), label = "shutter")
    val label = stringResource(R.string.scan_piece)
    Box(
        modifier.size(84.dp).scale(scale).border(4.dp, MaterialTheme.colorScheme.primary, CircleShape).padding(8.dp)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }.testTag("shutter"),
    ) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary, CircleShape))
    }
}

/** Dimmed viewfinder with a breathing square cut-out and corner brackets: where to put the piece. */
@Composable
private fun ScanFrame(modifier: Modifier = Modifier) {
    val breathe by rememberInfiniteTransition(label = "frame").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val b = 0.5f - 0.5f * cos(breathe * 2 * Math.PI).toFloat()
        val side = min(size.width, size.height) * (0.62f + 0.02f * b)
        val centre = Offset(size.width / 2f, size.height * 0.44f)
        val r = Rect(centre.x - side / 2, centre.y - side / 2, centre.x + side / 2, centre.y + side / 2)
        val rad = 28.dp.toPx()

        // The dim veil is the whole screen minus the rounded square (even-odd fill leaves the hole).
        val veil = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addRoundRect(RoundRect(r, CornerRadius(rad)))
        }
        drawPath(veil, Color(0xCC0B0E13))

        val len = 36.dp.toPx()
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            val p = Path().apply {
                moveTo(x, y + dy * len); lineTo(x, y + dy * rad * 0.2f)
                quadraticTo(x, y, x + dx * rad * 0.2f, y); lineTo(x + dx * len, y)
            }
            drawPath(p, accent.copy(alpha = (200 + 55 * b) / 255f), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
        }
        corner(r.left, r.top, 1f, 1f); corner(r.right, r.top, -1f, 1f)
        corner(r.left, r.bottom, 1f, -1f); corner(r.right, r.bottom, -1f, -1f)
    }
}
