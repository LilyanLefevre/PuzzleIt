package com.lilyan_lefevre.puzzleit.feature.puzzle

import android.graphics.Bitmap
import android.view.animation.OvershootInterpolator
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.PrimaryButton
import com.lilyan_lefevre.puzzleit.core.ui.PuzzleLoader
import com.lilyan_lefevre.puzzleit.core.ui.TonalButton
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleColors
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleText
import com.lilyan_lefevre.puzzleit.feature.recognition.Candidate
import com.lilyan_lefevre.puzzleit.feature.recognition.Grid
import com.lilyan_lefevre.puzzleit.feature.recognition.PieceKind
import com.lilyan_lefevre.puzzleit.feature.recognition.PieceMatcher
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * What the bottom sheet of the puzzle table shows, one composable per state of the scan (see ScanState):
 * IdleContent -> AnalyzingContent -> ResultContent, or ErrorContent when the photo is unusable.
 * The sheet itself (its frame, its levels, its dragging) is in PuzzleWorkingScreen and LevelSheet.
 */

/** Side margin of everything in the sheet. */
private val SheetPadding = 24.dp

/** Nothing scanned yet: a hint and the scan button. */
@Composable
fun IdleContent(onScan: () -> Unit) {
    Column(Modifier.padding(start = SheetPadding, end = SheetPadding, bottom = 28.dp)) {
        Text(stringResource(R.string.scan_hint_title), style = PuzzleText.display(26), color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.scan_hint_body), Modifier.padding(top = 4.dp), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryButton(stringResource(R.string.scan_piece), onScan, Modifier.fillMaxWidth().padding(top = 20.dp).testTag("buttonCapturePiece"), icon = R.drawable.ic_scan, height = 64.dp)
    }
}

/** The photo is being read. */
@Composable
fun AnalyzingContent() {
    Row(Modifier.padding(start = SheetPadding, end = SheetPadding, top = 8.dp, bottom = 36.dp).testTag("groupAnalyzing"), verticalAlignment = Alignment.CenterVertically) {
        PuzzleLoader(size = 64.dp)
        Column(Modifier.padding(start = 20.dp)) {
            Text(stringResource(R.string.analyzing), style = PuzzleText.display(22), color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.analyzing_sub), Modifier.padding(top = 2.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Blurry photo or no piece found: what went wrong, retake or dismiss. */
@Composable
fun ErrorContent(@StringRes title: Int, @StringRes body: Int, onRetake: () -> Unit, onDismiss: () -> Unit) {
    Column(Modifier.padding(start = SheetPadding, end = SheetPadding, bottom = 28.dp).testTag("groupError")) {
        Text(stringResource(title), Modifier.testTag("textErrorTitle"), style = PuzzleText.display(24), color = PuzzleColors.warning)
        Text(stringResource(body), Modifier.padding(top = 4.dp), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(stringResource(R.string.retake), onRetake, Modifier.weight(1f).testTag("buttonRetake"), icon = R.drawable.ic_scan)
            TonalButton(stringResource(R.string.dismiss), onDismiss, Modifier.testTag("buttonErrorDismiss"))
        }
    }
}

/**
 * The answer: lead chips, where it is in words, the piece next to the box, confidence, the verdict buttons and,
 * lower down (full level), one card per lead explaining why.
 *
 * [onPeekBottom] and [onHalfBottom] tell the sheet where the chips end and where the buttons end (pixels from the top
 * of this content), which are the heights of its "peek" and "half" levels.
 */
@Composable
fun ResultContent(
    result: ScanState.Result,
    box: Bitmap,
    grid: Grid,
    onSelect: (Int) -> Unit,
    onEvaluate: (Boolean) -> Unit,
    onNewPiece: () -> Unit,
    onDismiss: () -> Unit,
    onShowOnMap: (Int) -> Unit,
    onPeekBottom: (Float) -> Unit,
    onHalfBottom: (Float) -> Unit,
) {
    val leads = listOf(result.match.best) + result.match.alternatives
    val lead = result.shown
    Column(Modifier.padding(start = SheetPadding, end = SheetPadding, bottom = 28.dp).testTag("groupResult")) {
        LeadChips(leads, result.selected, onSelect, Modifier.onGloballyPositioned { onPeekBottom(it.positionInParent().y + it.size.height) })
        Spacer(Modifier.height(14.dp))

        val leadName = if (result.selected == 0) stringResource(R.string.best_lead) else stringResource(R.string.alt_lead, result.selected + 1)
        val kind = when (result.match.kind) {
            PieceKind.CORNER -> " · " + stringResource(R.string.piece_corner)
            PieceKind.EDGE -> " · " + stringResource(R.string.piece_edge)
            else -> ""
        }
        Text((leadName + kind).uppercase(), Modifier.testTag("textLead"), style = PuzzleText.eyebrow, color = MaterialTheme.colorScheme.primary)
        Text(
            stringResource(zoneRes(lead.col / grid.cols, lead.row / grid.rows)), Modifier.padding(top = 2.dp).testTag("textSpot"),
            style = PuzzleText.display(26), color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.spot_percent, percent(lead.col / grid.cols), percent(lead.row / grid.rows)), Modifier.padding(top = 2.dp),
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Compare(result.cutout, lead, box, grid, Modifier.padding(top = 14.dp))
        Confidence(lead.confidence, Modifier.padding(top = 20.dp))

        Row(Modifier.fillMaxWidth().padding(top = 16.dp).testTag("rowEvaluate"), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (result.evaluated) R.string.evaluate_thanks else R.string.evaluate_question), Modifier.weight(1f).testTag("textEvaluate"),
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!result.evaluated) {
                TonalButton(stringResource(R.string.evaluate_yes), { onEvaluate(true) }, Modifier.padding(start = 8.dp).testTag("buttonRight"), height = 44.dp, corner = 16.dp)
                TonalButton(stringResource(R.string.evaluate_no), { onEvaluate(false) }, Modifier.padding(start = 8.dp).testTag("buttonWrong"), height = 44.dp, corner = 16.dp)
            }
        }

        Row(
            Modifier.padding(top = 20.dp).onGloballyPositioned { onHalfBottom(it.positionInParent().y + it.size.height) },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton(stringResource(R.string.new_piece), onNewPiece, Modifier.weight(1f).testTag("buttonNewPiece"), icon = R.drawable.ic_scan)
            TonalButton(stringResource(R.string.dismiss), onDismiss, Modifier.testTag("buttonDismiss"))
        }

        Column(Modifier.padding(top = 32.dp).testTag("groupExplain")) {
            Text(stringResource(R.string.explain_title), style = PuzzleText.display(22), color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.explain_intro), Modifier.padding(top = 4.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                leads.forEachIndexed { i, c -> ExplainCard(i, c, result, box, grid, onClick = { onShowOnMap(i) }) }
            }
        }
    }
}

private fun percent(fraction: Float) = (100 * fraction).roundToInt().coerceIn(0, 100)

/** One chip per lead, best first, with its confidence; the selected one is filled with the accent colour. */
@Composable
private fun LeadChips(leads: List<Candidate>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()).testTag("chipLeads"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        leads.forEachIndexed { i, c ->
            FilterChip(
                selected = i == selected,
                onClick = { onSelect(i) },
                label = { Text(stringResource(R.string.alt_lead, i + 1) + " · " + c.confidence + " %", fontWeight = FontWeight.Bold) },
                modifier = Modifier.height(40.dp).testTag("chipLead${i + 1}"),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant, labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(true, i == selected, borderColor = MaterialTheme.colorScheme.outline, selectedBorderColor = MaterialTheme.colorScheme.primary),
            )
        }
    }
}

/** The piece, turned the right way, next to the box at this lead, and how to turn it. */
@Composable
private fun Compare(cutout: Bitmap, lead: Candidate, box: Bitmap, grid: Grid, modifier: Modifier = Modifier) {
    val patch = remember(lead, box, grid) { boxPatch(box, grid, lead) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(104.dp).tile(), contentAlignment = Alignment.Center) { TurningPiece(cutout, lead.rotationDeg) }
            Text(stringResource(R.string.your_piece), Modifier.padding(top = 6.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("≈", Modifier.padding(start = 6.dp, end = 6.dp, bottom = 18.dp), fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                patch.asImageBitmap(), stringResource(R.string.on_the_box),
                Modifier.size(104.dp).clip(RoundedCornerShape(12.dp)).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)).padding(2.dp),
                contentScale = ContentScale.Crop,
            )
            Text(stringResource(R.string.on_the_box), Modifier.padding(top = 6.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f).padding(start = 8.dp, bottom = 18.dp)) {
            Icon(painterResource(R.drawable.ic_rotate_small), contentDescription = null, tint = Color.Unspecified)
            Text(
                rotationText(lead.rotationDeg), Modifier.padding(top = 6.dp).testTag("textRotate"),
                fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = PuzzleColors.secondaryText,
            )
        }
    }
}

/** A dark rounded square with a thin outline, the background of the two pictures being compared. */
@Composable
private fun Modifier.tile(): Modifier = this
    .clip(RoundedCornerShape(12.dp))
    .background(MaterialTheme.colorScheme.surfaceVariant)
    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))

/**
 * The cut-out piece. A new piece pops in (grows and fades in with a little overshoot), then turns to the orientation it has on the box;
 * picking another lead of the same piece only turns it.
 */
@Composable
private fun TurningPiece(cutout: Bitmap, rotationDeg: Int) {
    val overshoot = Easing { OvershootInterpolator(1.4f).getInterpolation(it) }
    val scale = remember(cutout) { Animatable(0.4f) }
    val alpha = remember(cutout) { Animatable(0f) }
    val turn = remember(cutout) { Animatable(0f) }
    LaunchedEffect(cutout) {
        // Pop in; the effect below waits for it on a new piece.
        launch { alpha.animateTo(1f, tween(450)) }
        scale.animateTo(1f, tween(450, easing = overshoot))
    }
    LaunchedEffect(cutout, rotationDeg) {
        if (scale.value < 1f) delay(450) // new piece: let it pop in first
        delay(150)
        turn.animateTo(if (rotationDeg <= 180) rotationDeg.toFloat() else rotationDeg - 360f, tween(750, easing = { OvershootInterpolator(0.8f).getInterpolation(it) }))
    }
    Image(
        cutout.asImageBitmap(), stringResource(R.string.your_piece),
        Modifier.size(84.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value; rotationZ = turn.value },
    )
}

/** "Confidence · strong match", the number counting up, and the bar. */
@Composable
private fun Confidence(value: Int, modifier: Modifier = Modifier) {
    val label = when {
        value >= PieceMatcher.CELL_CONF -> R.string.conf_high
        value >= PieceMatcher.ZONE_CONF -> R.string.conf_medium
        else -> R.string.conf_low
    }
    val shown by animateIntAsState(value, tween(700), label = "confidence")
    val bar by animateFloatAsState(value / 100f, tween(700), label = "bar")
    Column(modifier) {
        Row {
            Text(stringResource(R.string.confidence_label) + " · " + stringResource(label), Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$shown %", Modifier.testTag("textConfValue"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        ConfidenceBar(bar, Modifier.padding(top = 8.dp), MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
private fun ConfidenceBar(progress: Float, modifier: Modifier = Modifier, track: Color) {
    LinearProgressIndicator(
        progress = { progress }, modifier = modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(6.dp)),
        color = MaterialTheme.colorScheme.primary, trackColor = track, gapSize = 0.dp, drawStopIndicator = {},
    )
}

/** Full level: one lead, its confidence, the piece blinking over its place on the box, and the reasons. Tapping the card shows the lead on the map. */
@Composable
private fun ExplainCard(index: Int, lead: Candidate, result: ScanState.Result, box: Bitmap, grid: Grid, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceVariant).border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape)
            .clickable(onClick = onClick).padding(16.dp).testTag("explainCard$index"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.alt_lead, index + 1), Modifier.weight(1f), style = PuzzleText.display(18), color = MaterialTheme.colorScheme.onSurface)
            Text("${lead.confidence} %", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        ConfidenceBar(lead.confidence / 100f, Modifier.padding(top = 8.dp), MaterialTheme.colorScheme.surface)
        LeadComparison(box, grid, lead, result.cutout, Modifier.fillMaxWidth().padding(top = 14.dp).height(200.dp).testTag("leadCompare$index"))
        Text(stringResource(R.string.compare_hint), Modifier.padding(top = 6.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(reasons(result, lead), Modifier.padding(top = 10.dp), fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.see_on_puzzle), Modifier.padding(top = 12.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PuzzleColors.secondaryText)
    }
}

/** Why a lead is a lead: the ranks of its visual and colour matches, the turn, the outline the app read. */
@Composable
private fun reasons(result: ScanState.Result, c: Candidate): String = buildList {
    if (c.pool > 0) {
        add(stringResource(R.string.reason_similarity, c.simRank, c.pool))
        add(stringResource(R.string.reason_colour, c.colourRank))
    }
    add(rotationText(c.rotationDeg))
    when (result.match.kind) {
        PieceKind.CORNER -> R.string.piece_corner
        PieceKind.EDGE -> R.string.piece_edge
        PieceKind.INTERIOR -> R.string.piece_inner
        PieceKind.UNKNOWN -> null
    }?.let { add(stringResource(R.string.reason_shape, stringResource(it))) }
}.joinToString("\n")

@Composable
private fun rotationText(deg: Int): String = when {
    deg == 0 -> stringResource(R.string.rotate_none)
    deg <= 180 -> stringResource(R.string.rotate_hint, deg)
    else -> stringResource(R.string.rotate_hint_ccw, 360 - deg)
}

/** The box at this lead: one cell plus a little margin, in the box's orientation. */
private fun boxPatch(box: Bitmap, g: Grid, c: Candidate): Bitmap {
    val cw = box.width / g.cols.toFloat()
    val ch = box.height / g.rows.toFloat()
    val w = (cw * 1.3f).toInt().coerceIn(1, box.width)
    val h = (ch * 1.3f).toInt().coerceIn(1, box.height)
    val x = (c.col * cw - w / 2f).toInt().coerceIn(0, box.width - w)
    val y = (c.row * ch - h / 2f).toInt().coerceIn(0, box.height - h)
    return Bitmap.createBitmap(box, x, y, w, h)
}
