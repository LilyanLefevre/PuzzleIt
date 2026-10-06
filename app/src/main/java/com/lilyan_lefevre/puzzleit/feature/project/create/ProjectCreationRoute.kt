package com.lilyan_lefevre.puzzleit.feature.project.create

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.image.PhotoHelper
import com.lilyan_lefevre.puzzleit.core.ui.ScreenHeader

/** What the framing screen hands back: the four corners (JSON) and the rectified photo. */
data class Framing(val quadJson: String, val rectifiedPath: String)

/** Create or edit a project: box photo (system camera), framing, name, piece count and grid. */
@Composable
fun ProjectCreationRoute(
    /** The puzzle to edit, null when creating. */
    projectId: String?,
    /** Result of the framing screen, null until there is one. */
    framing: Framing?,
    onFramingConsumed: () -> Unit,
    onBack: () -> Unit,
    onOpenBounds: (imagePath: String) -> Unit,
    viewModel: ProjectCreationViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val existing by viewModel.existing.collectAsStateWithLifecycle()
    val processing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    // File the system camera writes into; only meaningful while the camera app is open (saved: the activity can be recreated meanwhile).
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val path = pendingPhoto ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onPhotoTaken(path)
            onOpenBounds(path)
        }
    }
    fun takePhoto() {
        try {
            val file = PhotoHelper.createImageFile(context).also { pendingPhoto = it.absolutePath }
            PhotoHelper.createCameraIntent(context, file)?.let(takePicture::launch)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera", e)
            Toast.makeText(context, R.string.camera_error, Toast.LENGTH_SHORT).show()
        }
    }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePhoto() else Toast.makeText(context, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(projectId) { projectId?.let(viewModel::loadForEdit) }

    LaunchedEffect(framing) {
        framing ?: return@LaunchedEffect
        viewModel.onBoundsConfirmed(framing.quadJson, framing.rectifiedPath)
        onFramingConsumed()
    }

    // One-shot events: an error to tell, or the save that ends this screen. Everything else is drawn by the screen.
    LaunchedEffect(error) {
        when (val e = error ?: return@LaunchedEffect) {
            is Int -> Toast.makeText(context, e, Toast.LENGTH_SHORT).show()
            else -> MaterialAlertDialogBuilder(context).setTitle(R.string.error).setMessage(e.toString())
                .setPositiveButton(R.string.ok, null).show()
        }
        viewModel.errorShown()
    }
    LaunchedEffect(saved) {
        val s = saved ?: return@LaunchedEffect
        val msg = when (s) {
            is ProjectCreationViewModel.Saved.Created -> context.getString(R.string.project_created_successfully, s.project.name)
            ProjectCreationViewModel.Saved.Updated -> context.getString(R.string.project_updated)
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        viewModel.savedHandled()
        onBack()
    }

    // No toolbar any more: this screen draws its own header, like the others ("New puzzle" or "Edit puzzle").
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(stringResource(viewModel.titleRes()), onBack)
        Box(Modifier.weight(1f)) {
            ProjectCreationScreen(
                preview = draft.preview,
                existing = existing,
                processing = processing,
                suggestGrid = viewModel::suggestGrid,
                onTakePhoto = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) takePhoto()
                    else requestPermission.launch(Manifest.permission.CAMERA)
                },
                onFramePhoto = { draft.originalPath?.let(onOpenBounds) },
                onCancel = { viewModel.discardDraft(); onBack() },
                onSave = viewModel::save,
            )
        }
    }
}

private const val TAG = "ProjectCreation"
