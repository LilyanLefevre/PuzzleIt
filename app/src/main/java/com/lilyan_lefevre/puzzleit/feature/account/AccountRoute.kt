package com.lilyan_lefevre.puzzleit.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Who is signed in, and sign out (the activity then brings the login screen back). The sync runs by itself. */
@Composable
fun AccountRoute(onBack: () -> Unit, viewModel: AccountViewModel = hiltViewModel()) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    AccountScreen(
        email = account?.email.orEmpty(),
        onBack = onBack,
        onSignOut = viewModel::signOut,
    )
}
