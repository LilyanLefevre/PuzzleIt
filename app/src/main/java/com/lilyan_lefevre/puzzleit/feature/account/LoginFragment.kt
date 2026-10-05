package com.lilyan_lefevre.puzzleit.feature.account

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.screen
import dagger.hilt.android.AndroidEntryPoint

/** First screen when nobody is signed in: password, or one button per provider the server offers (Google...). The activity leaves it once signed in. */
@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val viewModel: AccountViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = screen {
        val providers by viewModel.providers.collectAsStateWithLifecycle()
        val busy by viewModel.busy.collectAsStateWithLifecycle()
        val notice by viewModel.notice.collectAsStateWithLifecycle()
        val openUrl by viewModel.openUrl.collectAsStateWithLifecycle()

        // The provider's page opens in the browser, once.
        LaunchedEffect(openUrl) {
            val url = openUrl ?: return@LaunchedEffect
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: ActivityNotFoundException) {
                // No browser: the sign-in times out and says so.
            }
            viewModel.urlOpened()
        }

        val failed = notice as? Notice.Failed
        LoginScreen(
            providers = providers,
            busy = busy,
            error = failed?.let { f -> f.reason?.let { stringResource(it) } ?: stringResource(R.string.sync_failed, f.detail) },
            onSignIn = viewModel::signIn,
            onProvider = viewModel::signInWith,
        )
    }
}
