package com.lilyan_lefevre.puzzleit.feature.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.ScreenHeader
import com.lilyan_lefevre.puzzleit.core.ui.TonalButton
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleTheme

/** Who is signed in, and the sign-out button. */
@Composable
fun AccountScreen(email: String, onBack: () -> Unit, onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.account_title), onBack)
        Text(
            stringResource(R.string.account_intro), Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp),
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.padding(24.dp)) {
            Text(stringResource(R.string.signed_in_as, email), Modifier.testTag("textAccount"), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            TonalButton(stringResource(R.string.sign_out), onSignOut, Modifier.fillMaxWidth().padding(top = 20.dp).testTag("buttonSignOut"))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun AccountPreview() = PuzzleTheme { AccountScreen("a@test.dev", onBack = {}, onSignOut = {}) }
