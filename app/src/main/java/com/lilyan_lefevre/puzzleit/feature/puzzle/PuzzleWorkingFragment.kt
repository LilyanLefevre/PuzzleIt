package com.lilyan_lefevre.puzzleit.feature.puzzle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.screen
import com.lilyan_lefevre.puzzleit.feature.puzzle.capture.PieceCaptureFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * The puzzle table. This Fragment navigates and shows the one-shot messages; everything on screen is [PuzzleWorkingScreen].
 */
@AndroidEntryPoint
class PuzzleWorkingFragment : Fragment() {

    private val viewModel: PuzzleWorkingViewModel by viewModels()
    private lateinit var projectId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectId = arguments?.getString("projectId") ?: ""
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = screen {
        val project by viewModel.project.collectAsStateWithLifecycle()
        val reference by viewModel.reference.collectAsStateWithLifecycle()
        val loading by viewModel.isLoading.collectAsStateWithLifecycle()
        val scan by viewModel.scan.collectAsStateWithLifecycle()
        PuzzleWorkingScreen(
            project, reference, loading, scan,
            TableActions(
                onBack = { findNavController().navigateUp() },
                onEdit = { findNavController().navigate(R.id.action_puzzleWorkingFragment_to_projectCreationFragment, bundleOf("projectId" to projectId)) },
                onDelete = viewModel::delete,
                onHistory = { findNavController().navigate(R.id.action_puzzleWorkingFragment_to_historyFragment, bundleOf("projectId" to projectId)) },
                onProgress = { findNavController().navigate(R.id.action_puzzleWorkingFragment_to_progressFragment, bundleOf("projectId" to projectId)) },
                onScan = { findNavController().navigate(R.id.action_puzzleWorkingFragment_to_pieceCaptureFragment) },
                onSelectLead = viewModel::select,
                onEvaluate = viewModel::evaluate,
                onDismiss = viewModel::dismiss,
            ),
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentResultListener(PieceCaptureFragment.RESULT_KEY) { _, b ->
            b.getString(PieceCaptureFragment.PHOTO_PATH)?.let(viewModel::analyze)
        }
        observeEvents()
        viewModel.loadProject(projectId)
    }

    /** One-shot events from the ViewModel: a message then leave the screen (project missing, or just deleted). */
    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.error.collect { error ->
                        error ?: return@collect
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                        viewModel.errorShown()
                        findNavController().navigateUp()
                    }
                }
                launch {
                    viewModel.deleted.collect { gone ->
                        if (!gone) return@collect
                        Toast.makeText(requireContext(), getString(R.string.project_deleted_successfully, viewModel.project.value?.name.orEmpty()), Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }
}
