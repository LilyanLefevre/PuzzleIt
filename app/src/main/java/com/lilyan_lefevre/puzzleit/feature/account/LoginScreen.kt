package com.lilyan_lefevre.puzzleit.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.TonalButton
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleText
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleTheme
import com.lilyan_lefevre.puzzleit.feature.account.data.OAuthProvider

/**
 * One "Continue with ..." button per provider the server offers (Google...). Accounts are created by the provider only.
 * Stateless: the route gives it the data and the actions.
 */
@Composable
fun LoginScreen(
    /** Null while the server is being asked. */
    providers: List<OAuthProvider>?,
    busy: Boolean,
    /** The server could not be reached to read the providers: say so and offer [onRetry]. */
    serverUnreachable: Boolean,
    /** The failure to show in red, or null. */
    error: String?,
    onProvider: (OAuthProvider) -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.login_title), style = PuzzleText.display(30), color = MaterialTheme.colorScheme.onSurface)
        Text(
            stringResource(R.string.login_subtitle), Modifier.padding(top = 8.dp),
            fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            providers?.forEach { provider ->
                OutlinedButton({ onProvider(provider) }, Modifier.fillMaxWidth().height(56.dp).testTag("button_${provider.name}")) {
                    Text(stringResource(R.string.continue_with, provider.displayName))
                }
            }
        }
        if (providers?.isEmpty() == true) {
            Text(stringResource(R.string.login_no_provider), Modifier.testTag("textNoProvider"), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (serverUnreachable && !busy) {
            Text(
                stringResource(R.string.server_unreachable), Modifier.padding(top = 16.dp).testTag("textUnreachable"),
                color = MaterialTheme.colorScheme.error, fontSize = 14.sp,
            )
            TonalButton(stringResource(R.string.retry), onRetry, Modifier.fillMaxWidth().padding(top = 8.dp).testTag("buttonRetry"))
        }
        if (busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp))
        if (error != null) {
            Text(error, Modifier.padding(top = 12.dp).testTag("textNotice"), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun LoginPreview() = PuzzleTheme {
    LoginScreen(listOf(OAuthProvider("google", "Google", "", "")), busy = false, serverUnreachable = false, error = "Failed to authenticate.", onProvider = {}, onRetry = {})
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun LoginUnreachablePreview() = PuzzleTheme {
    LoginScreen(null, busy = false, serverUnreachable = true, error = null, onProvider = {}, onRetry = {})
}
