package com.lilyan_lefevre.puzzleit.feature.puzzle

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lilyan_lefevre.puzzleit.R

/**
 * The puzzle table. This route loads the project, shows the one-shot messages and receives the camera's photo;
 * everything on screen is [PuzzleWorkingScreen], the navigation is in the lambdas.
 */
@Composable
fun PuzzleWorkingRoute(
    projectId: String,
    /** Path of the piece photo the camera screen just handed back, null otherwise. */
    newPhoto: String?,
    onPhotoConsumed: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onHistory: () -> Unit,
    onProgress: () -> Unit,
    onScan: () -> Unit,
    viewModel: PuzzleWorkingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val project by viewModel.project.collectAsStateWithLifecycle()
    val reference by viewModel.reference.collectAsStateWithLifecycle()
    val loading by viewModel.isLoading.collectAsStateWithLifecycle()
    val scan by viewModel.scan.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()

    LaunchedEffect(projectId) { viewModel.loadProject(projectId) }

    // The photo comes back through the previous screen's saved state; it is taken once, then cleared so it is not analysed again.
    LaunchedEffect(newPhoto) {
        newPhoto ?: return@LaunchedEffect
        viewModel.analyze(newPhoto)
        onPhotoConsumed()
    }

    // One-shot events: a message then leave the screen (project missing, or just deleted).
    LaunchedEffect(error) {
        val message = error ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        viewModel.errorShown()
        onBack()
    }
    LaunchedEffect(deleted) {
        if (!deleted) return@LaunchedEffect
        Toast.makeText(context, context.getString(R.string.project_deleted_successfully, viewModel.project.value?.name.orEmpty()), Toast.LENGTH_SHORT).show()
        onBack()
    }

    PuzzleWorkingScreen(
        project, reference, loading, scan,
        TableActions(
            onBack = onBack,
            onEdit = onEdit,
            onDelete = viewModel::delete,
            onHistory = onHistory,
            onProgress = onProgress,
            onScan = onScan,
            onSelectLead = viewModel::select,
            onEvaluate = viewModel::evaluate,
            onDismiss = viewModel::dismiss,
        ),
    )
}
