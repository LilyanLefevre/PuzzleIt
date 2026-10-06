package com.lilyan_lefevre.puzzleit.feature.account

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lilyan_lefevre.puzzleit.R

/** First screen when nobody is signed in: password, or one button per provider the server offers (Google...). The activity leaves it once signed in. */
@Composable
fun LoginRoute(viewModel: AccountViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val openUrl by viewModel.openUrl.collectAsStateWithLifecycle()

    // The provider's page opens in the browser, once.
    LaunchedEffect(openUrl) {
        val url = openUrl ?: return@LaunchedEffect
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
