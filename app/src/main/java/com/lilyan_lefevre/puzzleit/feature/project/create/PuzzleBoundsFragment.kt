package com.lilyan_lefevre.puzzleit.feature.project.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.image.ImageUtils
import com.lilyan_lefevre.puzzleit.core.ui.screen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Lets the user adjust the 4 corners of the box on the photo. */
@AndroidEntryPoint
class PuzzleBoundsFragment : Fragment() {

    private val args: PuzzleBoundsFragmentArgs by navArgs()
    private val viewModel: PuzzleBoundsViewModel by viewModels()

    @Inject lateinit var imageUtils: ImageUtils

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = screen {
        val detected by viewModel.detected.collectAsStateWithLifecycle()
        PuzzleBoundsScreen(
            imagePath = args.imagePath,
            sourceSize = remember { imageUtils.readOrientedDimensions(args.imagePath) },
            detected = detected,
            onCancel = { findNavController().navigateUp() },
            onConfirm = { viewModel.confirm(args.imagePath, it) },
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.confirmed.collect { b ->
                    b ?: return@collect
                    setFragmentResult(RESULT_KEY, bundleOf(QUAD_JSON to b.quadJson, RECTIFIED_PATH to b.rectifiedPath))
                    findNavController().popBackStack(R.id.projectCreationFragment, false)
                }
            }
        }
        viewModel.detect(args.imagePath)
    }

    companion object {
        const val RESULT_KEY = "puzzleBoundsResult"
        const val QUAD_JSON = "quadJson"
        const val RECTIFIED_PATH = "croppedImagePath"
    }
}
