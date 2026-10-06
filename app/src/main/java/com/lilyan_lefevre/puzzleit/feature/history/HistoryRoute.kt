package com.lilyan_lefevre.puzzleit.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** The pieces scanned on a puzzle, with the verdict the person gave on each. */
@Composable
fun HistoryRoute(onBack: () -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val scans by viewModel.scans.collectAsStateWithLifecycle()
    HistoryScreen(scans, onBack = onBack, onDelete = viewModel::delete)
}
