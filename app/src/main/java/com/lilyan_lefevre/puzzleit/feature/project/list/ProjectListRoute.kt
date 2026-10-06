package com.lilyan_lefevre.puzzleit.feature.project.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** The list of all puzzles; the navigation is in the lambdas, decided by the NavHost. */
@Composable
fun ProjectListRoute(
    onOpen: (projectId: String) -> Unit,
    onAccount: () -> Unit,
    onNew: () -> Unit,
    viewModel: ProjectListViewModel = hiltViewModel(),
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    // Every time the list comes back on screen (what onStart used to do).
    LifecycleStartEffect(Unit) {
        viewModel.refresh()
        onStopOrDispose {}
    }
    ProjectListScreen(
        projects = projects,
        syncing = syncing,
        onOpen = { onOpen(it.id) },
        onAccount = onAccount,
        onNew = onNew,
    )
}
