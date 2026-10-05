package com.lilyan_lefevre.puzzleit.feature.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import com.lilyan_lefevre.puzzleit.core.ui.screen
import dagger.hilt.android.AndroidEntryPoint

/** Who is signed in, and sign out (the activity then brings the login screen back). The sync runs by itself. */
@AndroidEntryPoint
class AccountFragment : Fragment() {

    private val viewModel: AccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = screen {
        val account by viewModel.account.collectAsStateWithLifecycle()
        AccountScreen(
            email = account?.email.orEmpty(),
            onBack = { findNavController().navigateUp() },
            onSignOut = viewModel::signOut,
        )
    }
}
