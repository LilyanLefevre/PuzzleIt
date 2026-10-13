package com.lilyan_lefevre.puzzleit.feature.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.ConfirmDialog
import com.lilyan_lefevre.puzzleit.core.ui.EmptyMessage
import com.lilyan_lefevre.puzzleit.core.ui.FileImage
import com.lilyan_lefevre.puzzleit.core.ui.PuzzleCard
import com.lilyan_lefevre.puzzleit.core.ui.RoundIconButton
import com.lilyan_lefevre.puzzleit.core.ui.ScreenHeader
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleTheme
import com.lilyan_lefevre.puzzleit.feature.history.data.ScanRecord
import com.lilyan_lefevre.puzzleit.feature.history.data.Verdict
import com.lilyan_lefevre.puzzleit.feature.puzzle.zoneRes
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

/** The pieces scanned on a puzzle, newest first, with the verdict the person gave on each. */
@Composable
fun HistoryScreen(scans: List<ScanRecord>, onBack: () -> Unit, onDelete: (ScanRecord) -> Unit) {
    // The scan the person is about to delete, while the confirmation dialog is open.
    var toDelete by remember { mutableStateOf<ScanRecord?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            stringResource(R.string.history_title), onBack,
            subtitle = pluralStringResource(R.plurals.history_summary, scans.size, scans.size, scans.count { it.verdict == Verdict.CORRECT }),
        )
        Box(Modifier.fillMaxSize()) {
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(scans, key = { it.id }) { record -> ScanRecordCard(record, onDelete = { toDelete = record }) }
            }
            if (scans.isEmpty()) EmptyMessage(stringResource(R.string.history_empty), Modifier.align(Alignment.Center))
        }
    }

    toDelete?.let { record ->
        ConfirmDialog(
            stringResource(R.string.delete_scan_title), stringResource(R.string.delete_scan_message), stringResource(R.string.delete),
            onConfirm = { onDelete(record) }, onDismiss = { toDelete = null },
        )
    }
}

/** One scan: the piece, where it was sent, when, and whether the person said it was right. */
@Composable
private fun ScanRecordCard(record: ScanRecord, onDelete: () -> Unit) {
    val lead = remember(record) { record.shownLead }
    val (verdictText, verdictColour) = when (record.verdict) {
        Verdict.CORRECT -> R.string.verdict_correct to MaterialTheme.colorScheme.primary
        Verdict.WRONG -> R.string.verdict_wrong to MaterialTheme.colorScheme.error
        else -> R.string.verdict_unknown to MaterialTheme.colorScheme.onSurfaceVariant
    }
    PuzzleCard(Modifier.fillMaxWidth().testTag("scanRecord"), contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FileImage(record.piecePath, stringResource(R.string.scanned_piece), Modifier.size(72.dp), maxSizePx = 256)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                if (lead != null) {
                    Text(stringResource(zoneRes(lead.x, lead.y)), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        stringResource(R.string.spot_percent, (100 * lead.x).roundToInt().coerceIn(0, 100), (100 * lead.y).roundToInt().coerceIn(0, 100)) +
                            " · " + stringResource(R.string.confidence_percent, lead.confidence),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.createdAt)),
                    Modifier.padding(top = 2.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(verdictText), Modifier.padding(top = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = verdictColour)
            }
            RoundIconButton(R.drawable.ic_delete, stringResource(R.string.delete_button), onDelete, size = 40.dp, iconSize = 18.dp)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun HistoryPreview() = PuzzleTheme {
    val scan = ScanRecord(id = 1, projectId = "p", createdAt = 1_760_000_000_000, piecePath = "", leads = "0.48,0.43,90,82", verdict = Verdict.CORRECT)
    HistoryScreen(listOf(scan, scan.copy(id = 2, verdict = Verdict.WRONG)), onBack = {}, onDelete = {})
}
