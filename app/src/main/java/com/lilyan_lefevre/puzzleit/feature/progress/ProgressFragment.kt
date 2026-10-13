package com.lilyan_lefevre.puzzleit.feature.progress

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.screen
import dagger.hilt.android.AndroidEntryPoint

/** Photos of the puzzle as it stands, one per date: a way to see it grow, and later to place pieces on what is already done. */
@AndroidEntryPoint
class ProgressFragment : Fragment() {

    private val viewModel: ProgressViewModel by viewModels()

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { viewModel.photoTaken(it) }

    // The system camera refuses to open when the app declares the permission without holding it.
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePhoto() else Toast.makeText(requireContext(), R.string.camera_permission_denied, Toast.LENGTH_LONG).show()
    }

    private fun takePhoto() {
        val file = viewModel.newPhotoFile()
        takePicture.launch(FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = screen {
        val photos by viewModel.photos.collectAsStateWithLifecycle()
        ProgressScreen(
            photos = photos,
            onBack = { findNavController().navigateUp() },
            onAddPhoto = {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) takePhoto()
                else requestPermission.launch(Manifest.permission.CAMERA)
            },
            onDelete = viewModel::delete,
        )
    }
}
