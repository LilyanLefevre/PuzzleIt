package com.lilyan_lefevre.puzzleit.feature.project.create

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.image.PhotoHelper
import com.lilyan_lefevre.puzzleit.core.ui.screen
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlinx.coroutines.launch

/** Create or edit a project: box photo (system camera), framing, name, piece count and grid. */
@AndroidEntryPoint
class ProjectCreationFragment : Fragment() {

    private val viewModel: ProjectCreationViewModel by viewModels()
    private val args: ProjectCreationFragmentArgs by navArgs()

    /** File the system camera writes into; only meaningful while the camera app is open. */
    private var pendingPhoto: File? = null

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePhoto() else Toast.makeText(requireContext(), R.string.camera_permission_denied, Toast.LENGTH_SHORT).show()
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val file = pendingPhoto ?: return@registerForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onPhotoTaken(file.absolutePath)
            openBounds(file.absolutePath)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = screen {
        val draft by viewModel.draft.collectAsStateWithLifecycle()
        val existing by viewModel.existing.collectAsStateWithLifecycle()
        val processing by viewModel.isProcessing.collectAsStateWithLifecycle()

        // The toolbar of the activity shows the title: "New puzzle" or "Edit puzzle".
        LaunchedEffect(existing) { (activity as? AppCompatActivity)?.supportActionBar?.setTitle(viewModel.titleRes()) }

        ProjectCreationScreen(
            preview = draft.preview,
            existing = existing,
            processing = processing,
            suggestGrid = viewModel::suggestGrid,
            onTakePhoto = ::checkPermissionAndTakePhoto,
            onFramePhoto = { draft.originalPath?.let(::openBounds) },
            onCancel = { viewModel.discardDraft(); findNavController().navigateUp() },
            onSave = viewModel::save,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        args.projectId?.let(viewModel::loadForEdit)

        setFragmentResultListener(PuzzleBoundsFragment.RESULT_KEY) { _, b ->
            val quad = b.getString(PuzzleBoundsFragment.QUAD_JSON) ?: return@setFragmentResultListener
            val path = b.getString(PuzzleBoundsFragment.RECTIFIED_PATH) ?: return@setFragmentResultListener
            viewModel.onBoundsConfirmed(quad, path)
        }
        observeEvents()
    }

    /** One-shot events: an error to tell, or the save that ends this screen. Everything else is drawn by the screen. */
    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.error.collect { e ->
                        e ?: return@collect
                        when (e) {
                            is Int -> Toast.makeText(requireContext(), e, Toast.LENGTH_SHORT).show()
                            else -> MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.error).setMessage(e.toString())
                                .setPositiveButton(R.string.ok, null).show()
                        }
                        viewModel.errorShown()
                    }
                }
                launch {
                    viewModel.saved.collect { s ->
                        s ?: return@collect
                        val msg = when (s) {
                            is ProjectCreationViewModel.Saved.Created -> getString(R.string.project_created_successfully, s.project.name)
                            ProjectCreationViewModel.Saved.Updated -> getString(R.string.project_updated)
                        }
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        viewModel.savedHandled()
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    private fun checkPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) takePhoto()
        else requestPermission.launch(Manifest.permission.CAMERA)
    }

    private fun takePhoto() {
        try {
            val file = PhotoHelper.createImageFile(requireContext()).also { pendingPhoto = it }
            PhotoHelper.createCameraIntent(requireContext(), file)?.let(takePicture::launch)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera", e)
            Toast.makeText(requireContext(), R.string.camera_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openBounds(imagePath: String) {
        findNavController().navigate(ProjectCreationFragmentDirections.actionProjectCreationFragmentToPuzzleBoundsFragment(imagePath))
    }

    private companion object { const val TAG = "ProjectCreation" }
}
