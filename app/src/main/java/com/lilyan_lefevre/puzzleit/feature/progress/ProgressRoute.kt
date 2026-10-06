package com.lilyan_lefevre.puzzleit.feature.progress

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lilyan_lefevre.puzzleit.R

/** Photos of the puzzle as it stands, one per date: a way to see it grow, and later to place pieces on what is already done. */
@Composable
fun ProgressRoute(onBack: () -> Unit, viewModel: ProgressViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val photos by viewModel.photos.collectAsStateWithLifecycle()

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { viewModel.photoTaken(it) }
    fun takePhoto() {
        val file = viewModel.newPhotoFile()
        takePicture.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
    }
    // The system camera refuses to open when the app declares the permission without holding it.
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePhoto() else Toast.makeText(context, R.string.camera_permission_denied, Toast.LENGTH_LONG).show()
    }

    ProgressScreen(
        photos = photos,
        onBack = onBack,
        onAddPhoto = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) takePhoto()
            else requestPermission.launch(Manifest.permission.CAMERA)
        },
        onDelete = viewModel::delete,
    )
}
