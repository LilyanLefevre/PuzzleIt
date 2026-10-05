package com.lilyan_lefevre.puzzleit.feature.project.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.FileImage
import com.lilyan_lefevre.puzzleit.core.ui.PuzzleCard
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleTheme
import com.lilyan_lefevre.puzzleit.feature.project.data.Project

/**
 * Create or edit a puzzle: the box photo on top (tap to take it, retake / frame it once taken) and the form below.
 *
 * What the person typed lives here, in the screen; the ViewModel only hears about it when they press the save button.
 * [existing] is the puzzle being edited (null when creating): its values fill the form once.
 */
@Composable
fun ProjectCreationScreen(
    /** The box photo to show (rectified if framed, else as taken), or null before the first photo. */
    preview: String?,
    existing: Project?,
    processing: Boolean,
    /** Rows x columns that fit the box photo for this number of pieces, or null when there is none to suggest. */
    suggestGrid: (pieces: Int) -> GridProcessor.GridConfig?,
    onTakePhoto: () -> Unit,
    onFramePhoto: () -> Unit,
    onCancel: () -> Unit,
    onSave: (name: String, pieces: Int?, rows: Int?, cols: Int?) -> Unit,
) {
    // rememberSaveable: what is typed survives a rotation.
    var name by rememberSaveable { mutableStateOf("") }
    var pieces by rememberSaveable { mutableStateOf("") }
    var rows by rememberSaveable { mutableStateOf("") }
    var cols by rememberSaveable { mutableStateOf("") }

    fun suggest() {
        val grid = pieces.toIntOrNull()?.let(suggestGrid) ?: return
        rows = grid.rows.toString()
        cols = grid.cols.toString()
    }

    // Editing: the saved values fill the form, once (not again if the person already typed something).
    LaunchedEffect(existing?.id) {
        if (existing != null && name.isEmpty()) {
            name = existing.name
            pieces = existing.puzzleSize.toString()
            rows = existing.gridRows.toString()
            cols = existing.gridCols.toString()
        }
    }
    // A new framing or a new photo changes the box's proportions: the grid is suggested again (not for the photo first shown).
    var shownPreview by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(preview) {
        if (shownPreview != null && preview != shownPreview) suggest()
        shownPreview = preview
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                PhotoSection(preview, onTakePhoto, onFramePhoto)
                PuzzleCard(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ClearableField(name, { name = it }, stringResource(R.string.puzzle_name), "editTextName")
                        ClearableField(pieces, { pieces = it; suggest() }, stringResource(R.string.number_of_pieces), "editTextPieces", KeyboardType.Number)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                rows, { rows = it }, Modifier.weight(1f).testTag("editTextRows"), label = { Text(stringResource(R.string.rows_hint)) },
                                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = FieldShape,
                            )
                            OutlinedTextField(
                                cols, { cols = it }, Modifier.weight(1f).testTag("editTextCols"), label = { Text(stringResource(R.string.cols_hint)) },
                                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = FieldShape,
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surface)
            Row(Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onCancel, Modifier.weight(1f).testTag("btnCancel")) { Text(stringResource(R.string.cancel)) }
                Button(
                    { onSave(name, pieces.toIntOrNull(), rows.toIntOrNull(), cols.toIntOrNull()) }, Modifier.weight(1f).testTag("btnCreate"),
                ) { Text(stringResource(if (existing != null) R.string.save_changes else R.string.create_project)) }
            }
        }
        if (processing) CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}

private val FieldShape = RoundedCornerShape(14.dp)

/** The 320 dp photo area: an invitation to take the photo, or the photo with its two round buttons. */
@Composable
private fun PhotoSection(preview: String?, onTakePhoto: () -> Unit, onFramePhoto: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(320.dp).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
        if (preview.isNullOrBlank()) {
            PuzzleCard(Modifier.fillMaxSize().testTag("photoPlaceholder"), onClick = onTakePhoto) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(painterResource(android.R.drawable.ic_menu_camera), stringResource(R.string.photo_placeholder), Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.tap_to_take_photo), Modifier.padding(top = 16.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        stringResource(R.string.take_a_photo_of_your_puzzle_box), Modifier.padding(top = 6.dp),
                        fontSize = 14.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        } else {
            PuzzleCard(Modifier.fillMaxSize()) {
                FileImage(preview, stringResource(R.string.puzzle_reference_image), Modifier.fillMaxSize(), contentScale = ContentScale.Fit, maxSizePx = 1600)
            }
            FloatingActionButton(
                onTakePhoto, Modifier.align(Alignment.BottomStart).padding(16.dp).testTag("retakePhotoButton"),
                containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface,
            ) { Icon(painterResource(android.R.drawable.ic_menu_camera), stringResource(R.string.retake)) }
            FloatingActionButton(
                onFramePhoto, Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag("editPhotoButton"),
                containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Icon(painterResource(android.R.drawable.ic_menu_edit), stringResource(R.string.edit_photo)) }
        }
    }
}

/** A text field with a "×" to empty it. */
@Composable
private fun ClearableField(value: String, onChange: (String) -> Unit, label: String, tag: String, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth().testTag(tag), label = { Text(label) }, singleLine = true, shape = FieldShape,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        trailingIcon = {
            if (value.isNotEmpty()) IconButton({ onChange("") }) { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.clear_field), Modifier.size(18.dp)) }
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun CreationPreview() = PuzzleTheme {
    ProjectCreationScreen(preview = null, existing = null, processing = false, suggestGrid = { null }, onTakePhoto = {}, onFramePhoto = {}, onCancel = {}, onSave = { _, _, _, _ -> })
}
