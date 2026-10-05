package com.lilyan_lefevre.puzzleit.feature.puzzle

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.ConfirmDialog
import com.lilyan_lefevre.puzzleit.core.ui.PrimaryButton
import com.lilyan_lefevre.puzzleit.core.ui.PuzzleLoader
import com.lilyan_lefevre.puzzleit.core.ui.RoundIconButton
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleColors
import com.lilyan_lefevre.puzzleit.feature.project.data.Project
import com.lilyan_lefevre.puzzleit.feature.recognition.Grid

/** What the buttons of the table do. Grouped so the screen has one parameter for them instead of ten. */
class TableActions(
    val onBack: () -> Unit,
    val onEdit: () -> Unit,
    val onDelete: () -> Unit,
    val onHistory: () -> Unit,
    val onProgress: () -> Unit,
    val onScan: () -> Unit,
    val onSelectLead: (Int) -> Unit,
    val onEvaluate: (Boolean) -> Unit,
    val onDismiss: () -> Unit,
)

/**
 * The puzzle table: the box image fills the screen, a bar floats on top of it and a sheet slides up from the bottom.
 *
 * The sheet shows [scan]: the scan button (idle), a loader (analyzing), an error, or the result. Only the result sheet
 * moves; it rests at one of four levels (see [SheetLevel]) and the map keeps its picture in the part of the screen
 * the sheet leaves free.
 */
@Composable
fun PuzzleWorkingScreen(
    project: Project?,
    reference: Pair<Bitmap, Grid>?,
    loading: Boolean,
    scan: ScanState,
    actions: TableActions,
) {
    val sheet = rememberLevelSheetState()
    var overlayOn by remember { mutableStateOf(true) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Measured heights, in pixels: the floating bar, the fixed sheet, and the two spots of the result content.
    var topBarHeight by remember { mutableIntStateOf(0) }
    var fixedSheetHeight by remember { mutableIntStateOf(0) }
    var peekContent by remember { mutableFloatStateOf(0f) }
    var halfContent by remember { mutableFloatStateOf(0f) }
    var mapInsetBottom by remember { mutableIntStateOf(0) }

    val result = scan as? ScanState.Result
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenHeight = with(density) { maxHeight.toPx() }
        val gap = with(density) { 72.dp.toPx() } // kept free above the sheet when it is fully up: the floating bar
        val grabber = with(density) { 33.dp.toPx() } // the grabber's own height: 12 above, 5, 16 below

        // Where each level puts the top edge of the sheet: peek shows the chips, half shows up to the buttons.
        val peekVisible = grabber + peekContent + with(density) { 14.dp.toPx() }
        val halfVisible = (grabber + halfContent + with(density) { 24.dp.toPx() }).coerceIn(screenHeight * 0.25f, screenHeight * 0.9f)
        LaunchedEffect(screenHeight, peekVisible, halfVisible) {
            sheet.updateAnchors(
                mapOf(SheetLevel.Hidden to screenHeight, SheetLevel.Peek to screenHeight - peekVisible, SheetLevel.Half to screenHeight - halfVisible, SheetLevel.Full to gap),
            )
        }
        // A new result opens at the half level; leaving the result hides the sheet.
        LaunchedEffect(result?.match) { if (result != null) sheet.animateTo(SheetLevel.Half) else sheet.reset() }

        // The map stays clear of the sheet whatever its level; the full level covers it on purpose, so the inset stays as it was.
        LaunchedEffect(sheet.level, sheet.anchors, result != null, fixedSheetHeight) {
            if (result == null) mapInsetBottom = fixedSheetHeight
            else if (sheet.level != SheetLevel.Full) mapInsetBottom = (screenHeight - (sheet.anchors[sheet.level] ?: screenHeight)).toInt()
        }

        PuzzleMap(reference, scan, overlayOn, topBarHeight, mapInsetBottom, onLeadTapped = actions.onSelectLead, modifier = Modifier.fillMaxSize())

        TopBar(project, actions, confirmDelete = { confirmDelete = true }, Modifier.onSizeChanged { topBarHeight = it.height })

        if (loading) PuzzleLoader(Modifier.align(Alignment.Center), size = 72.dp)

        if (result != null && reference != null) {
            LevelSheet(sheet, height = with(density) { (screenHeight - gap).toDp() }) {
                // The scrolling part: its grabber stays, the content moves.
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    ResultContent(
                        result, reference.first, reference.second,
                        onSelect = actions.onSelectLead, onEvaluate = actions.onEvaluate, onNewPiece = actions.onScan, onDismiss = actions.onDismiss,
                        onShowOnMap = { actions.onSelectLead(it); sheet.animateTo(SheetLevel.Peek) },
                        onPeekBottom = { peekContent = it }, onHalfBottom = { halfContent = it },
                    )
                }
            }
            // Back to the leads after the sheet was swiped away to move around the puzzle.
            if (sheet.level == SheetLevel.Hidden) {
                PrimaryButton(
                    stringResource(R.string.show_leads), { sheet.animateTo(SheetLevel.Half) },
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp).testTag("pillLeads"), icon = R.drawable.ic_bolt, height = 52.dp,
                )
            }
            // Shows or hides the piece over the suggested place on the map.
            if (sheet.level == SheetLevel.Peek || sheet.level == SheetLevel.Hidden) {
                RoundIconButton(
                    R.drawable.ic_layers, stringResource(R.string.overlay_toggle), { overlayOn = !overlayOn },
                    Modifier.align(Alignment.TopEnd).padding(top = 76.dp, end = 12.dp).alpha(if (overlayOn) 1f else 0.5f).testTag("buttonOverlay"),
                )
            }
        } else {
            FixedSheet(scan, actions, Modifier.align(Alignment.BottomCenter).onSizeChanged { fixedSheetHeight = it.height })
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            stringResource(R.string.delete_project_title), stringResource(R.string.delete_project_message, project?.name.orEmpty()), stringResource(R.string.delete),
            onConfirm = actions.onDelete, onDismiss = { confirmDelete = false },
        )
    }
}

/** The floating bar: back, name and piece count, edit, delete and the "more" menu (history, progress photos). */
@Composable
private fun TopBar(project: Project?, actions: TableActions, confirmDelete: () -> Unit, modifier: Modifier = Modifier) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundIconButton(R.drawable.ic_arrow_back, stringResource(R.string.back), actions.onBack, Modifier.testTag("buttonBack"))
        Column(
            Modifier.weight(1f).padding(start = 12.dp).clip(RoundedCornerShape(18.dp)).background(PuzzleColors.scrim).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Name and piece count only: the rows x columns are not shown, nobody knows them before the border is complete.
            Text(
                project?.name.orEmpty(), Modifier.testTag("textViewProjectName"), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                project?.let { stringResource(R.string.pieces_name, it.puzzleSize) }.orEmpty(), Modifier.testTag("textViewProjectInfo"),
                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RoundIconButton(R.drawable.ic_edit, stringResource(R.string.edit_button), actions.onEdit, Modifier.padding(start = 8.dp).testTag("buttonEdit"), iconSize = 20.dp)
        RoundIconButton(R.drawable.ic_delete, stringResource(R.string.delete_button), confirmDelete, Modifier.padding(start = 8.dp).testTag("buttonDelete"), iconSize = 20.dp)
        Box(Modifier.padding(start = 8.dp)) {
            RoundIconButton(R.drawable.ic_more, stringResource(R.string.more_button), { menuOpen = true }, Modifier.testTag("buttonMore"))
            DropdownMenu(menuOpen, { menuOpen = false }) {
                DropdownMenuItem({ Text(stringResource(R.string.menu_history)) }, { menuOpen = false; actions.onHistory() })
                DropdownMenuItem({ Text(stringResource(R.string.menu_progress)) }, { menuOpen = false; actions.onProgress() })
            }
        }
    }
}

/** The sheet of the states that do not move (scan button, loader, error): as tall as its content, glued to the bottom. */
@Composable
private fun FixedSheet(scan: ScanState, actions: TableActions, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, shape)
            // The sheet covers the map: touches on it must not reach the map below.
            .pointerInput(Unit) {},
    ) {
        Grabber()
                Crossfade(scan, animationSpec = tween(180), label = "sheet") { state ->
            when (state) {
                ScanState.Idle -> IdleContent(actions.onScan)
                ScanState.Analyzing -> AnalyzingContent()
                ScanState.Blurry -> ErrorContent(R.string.blurry_title, R.string.blurry_body, actions.onScan, actions.onDismiss)
                ScanState.NoPiece -> ErrorContent(R.string.no_piece_title, R.string.no_piece_body, actions.onScan, actions.onDismiss)
                is ScanState.Result -> Unit // handled by the moving sheet
            }
        }
    }
}
