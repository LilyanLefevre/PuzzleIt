package com.lilyan_lefevre.puzzleit.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.ConfirmDialog
import com.lilyan_lefevre.puzzleit.core.ui.EmptyMessage
import com.lilyan_lefevre.puzzleit.core.ui.FileImage
import com.lilyan_lefevre.puzzleit.core.ui.PrimaryButton
import com.lilyan_lefevre.puzzleit.core.ui.PuzzleCard
import com.lilyan_lefevre.puzzleit.core.ui.RoundIconButton
import com.lilyan_lefevre.puzzleit.core.ui.ScreenHeader
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleTheme
import com.lilyan_lefevre.puzzleit.feature.progress.data.ProgressPhoto
import java.text.DateFormat
import java.util.Date

/** Dated photos of the puzzle as it stands, newest first, and the button that takes a new one. */
@Composable
fun ProgressScreen(photos: List<ProgressPhoto>, onBack: () -> Unit, onAddPhoto: () -> Unit, onDelete: (ProgressPhoto) -> Unit) {
    var toDelete by remember { mutableStateOf<ProgressPhoto?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(stringResource(R.string.progress_title), onBack)
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(photos, key = { it.id }) { photo -> PhotoCard(photo, onDelete = { toDelete = photo }) }
            }
        }
        if (photos.isEmpty()) EmptyMessage(stringResource(R.string.progress_empty), Modifier.align(Alignment.Center).testTag("textEmpty"))
        PrimaryButton(
            stringResource(R.string.progress_add), onAddPhoto, Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp).testTag("buttonAddPhoto"),
            icon = R.drawable.ic_scan,
        )
    }

    toDelete?.let { photo ->
        ConfirmDialog(
            stringResource(R.string.delete_photo_title), stringResource(R.string.delete_photo_message), stringResource(R.string.delete),
            onConfirm = { onDelete(photo) }, onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun PhotoCard(photo: ProgressPhoto, onDelete: () -> Unit) {
    PuzzleCard(Modifier.fillMaxWidth()) {
        FileImage(photo.path, stringResource(R.string.progress_photo), Modifier.fillMaxWidth().height(220.dp))
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(photo.createdAt)),
                Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface,
            )
            RoundIconButton(R.drawable.ic_delete, stringResource(R.string.delete_button), onDelete, size = 40.dp, iconSize = 18.dp)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun ProgressPreview() = PuzzleTheme { ProgressScreen(emptyList(), onBack = {}, onAddPhoto = {}, onDelete = {}) }
