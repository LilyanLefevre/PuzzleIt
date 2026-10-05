package com.lilyan_lefevre.puzzleit.feature.project.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.screen
import dagger.hilt.android.AndroidEntryPoint

/** Fragment displaying the list of all puzzle projects */
@AndroidEntryPoint
class ProjectListFragment : Fragment() {

    private val viewModel: ProjectListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = screen {
        val projects by viewModel.projects.collectAsStateWithLifecycle()
        val syncing by viewModel.syncing.collectAsStateWithLifecycle()
        ProjectListScreen(
            projects = projects,
            syncing = syncing,
            onOpen = { findNavController().navigate(R.id.action_projectListFragment_to_puzzleWorkingFragment, bundleOf("projectId" to it.id)) },
            onAccount = { findNavController().navigate(R.id.action_projectListFragment_to_accountFragment) },
            onNew = { findNavController().navigate(R.id.action_projectListFragment_to_projectCreationFragment) },
        )
    }

    override fun onStart() {
        super.onStart()
        viewModel.refresh()
    }
}
