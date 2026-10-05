package com.lilyan_lefevre.puzzleit.feature.puzzle

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.feature.recognition.Grid

/**
 * The box image you can pan and zoom, with the leads drawn on it.
 *
 * [PuzzleMapView] is a hand-drawn View (pinch, pan, double tap and the camera moves are all in its `onDraw` and its
 * gesture detectors), so it stays a View and [AndroidView] puts it into the Compose layout. This function is the only
 * place that talks to it: it turns the screen's state into calls on the view, each one only when its input changes.
 *
 * [insetTop] / [insetBottom] (pixels) are the strips covered by the top bar and by the sheet: the view keeps the picture between them.
 */
@Composable
fun PuzzleMap(
    reference: Pair<Bitmap, Grid>?,
    scan: ScanState,
    overlayOn: Boolean,
    insetTop: Int,
    insetBottom: Int,
    onLeadTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var view by remember { mutableStateOf<PuzzleMapView?>(null) }
    val tapped by rememberUpdatedState(onLeadTapped)
    val result = scan as? ScanState.Result

    // Order matters: the picture first, then what is drawn on it.
    LaunchedEffect(view, reference) { reference?.let { (bitmap, grid) -> view?.setImage(bitmap, grid) } }
    LaunchedEffect(view, insetTop, insetBottom) { view?.setViewportInsets(insetTop, insetBottom) }
    LaunchedEffect(view, scan is ScanState.Analyzing) { view?.setScanning(scan is ScanState.Analyzing) }
    LaunchedEffect(view, result?.cutout) { view?.setPiece(result?.cutout) }
    LaunchedEffect(view, overlayOn) { view?.overlayOn = overlayOn }
    LaunchedEffect(view, reference, result?.match, result?.selected, scan::class) {
        val v = view ?: return@LaunchedEffect
        when {
            result != null -> v.showLeads(listOf(result.match.best) + result.match.alternatives, result.selected)
            scan is ScanState.Analyzing -> Unit // the previous leads stay under the scan animation
            else -> v.clearMatch()
        }
    }

    val description = stringResource(R.string.puzzle_reference_image)
    AndroidView(
        factory = { ctx ->
            PuzzleMapView(ctx).apply {
                contentDescription = description
                this.onLeadTapped = { tapped(it) }
            }
        },
        modifier = modifier,
        update = { view = it },
    )
}
