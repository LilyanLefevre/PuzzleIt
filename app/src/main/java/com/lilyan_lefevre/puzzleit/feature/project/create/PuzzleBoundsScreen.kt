package com.lilyan_lefevre.puzzleit.feature.project.create

import android.graphics.PointF
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.FileImage
import com.lilyan_lefevre.puzzleit.core.ui.QuadSelectionView

/**
 * The photo with the four draggable corners of the box on top, and Cancel / OK below.
 *
 * The corner handles are still the hand-drawn [QuadSelectionView] (touch handling for four points and a body drag):
 * [AndroidView] puts it into the Compose layout, exactly over the photo so both use the same fit-center maths.
 */
@Composable
fun PuzzleBoundsScreen(
    imagePath: String,
    /** Photo size after EXIF rotation, needed by the view to place the corners. */
    sourceSize: Pair<Int, Int>,
    /** Corners suggested by the detection, null until it is done. */
    detected: List<PointF>?,
    onCancel: () -> Unit,
    onConfirm: (corners: List<PointF>) -> Unit,
) {
    var quadView by remember { mutableStateOf<QuadSelectionView?>(null) }
    // Put the detected corners on the view once, when they arrive (not on every redraw: that would undo the person's dragging).
    LaunchedEffect(quadView, detected) { detected?.let { quadView?.setNormalizedQuad(it) } }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.weight(1f)) {
            FileImage(imagePath, stringResource(R.string.puzzle_box_image), Modifier.fillMaxSize(), contentScale = ContentScale.Fit, maxSizePx = 2048)
            AndroidView(
                factory = { ctx -> QuadSelectionView(ctx).also { it.setSourceSize(sourceSize.first, sourceSize.second); quadView = it } },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onCancel, Modifier.weight(1f).testTag("btnCancel")) { Text(stringResource(R.string.cancel)) }
            Button({ quadView?.let { onConfirm(it.getNormalizedQuad()) } }, Modifier.weight(1f).testTag("btnConfirm")) { Text(stringResource(R.string.ok)) }
        }
    }
}
