package com.lilyan_lefevre.puzzleit.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.PrimaryButton
import com.lilyan_lefevre.puzzleit.core.ui.TonalButton
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleText
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleTheme
import com.lilyan_lefevre.puzzleit.feature.account.data.OAuthProvider

/**
 * Email + password, and one "Continue with ..." button per provider the server offers.
 * Stateless apart from what is being typed: the fragment gives it the data and the actions.
 */
@Composable
fun LoginScreen(
    providers: List<OAuthProvider>,
    busy: Boolean,
    /** The failure to show in red, or null. */
    error: String?,
    onSignIn: (email: String, password: String, create: Boolean) -> Unit,
    onProvider: (OAuthProvider) -> Unit,
) {
    // rememberSaveable keeps what was typed through a rotation.
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.login_title), style = PuzzleText.display(30), color = MaterialTheme.colorScheme.onSurface)
        Text(
            stringResource(R.string.login_subtitle), Modifier.padding(top = 8.dp),
            fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            email, { email = it }, Modifier.fillMaxWidth().padding(top = 32.dp).testTag("editEmail"),
            label = { Text(stringResource(R.string.email_hint)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        OutlinedTextField(
            password, { password = it }, Modifier.fillMaxWidth().padding(top = 12.dp).testTag("editPassword"),
            label = { Text(stringResource(R.string.password_hint)) }, singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton({ showPassword = !showPassword }) { Text(stringResource(if (showPassword) R.string.hide_password else R.string.show_password)) }
            },
        )

        PrimaryButton(
            stringResource(R.string.sign_in), { onSignIn(email, password, false) },
            Modifier.fillMaxWidth().padding(top = 20.dp).testTag("buttonSignIn"),
        )
        TonalButton(
            stringResource(R.string.create_account), { onSignIn(email, password, true) },
            Modifier.fillMaxWidth().padding(top = 8.dp).testTag("buttonCreate"),
        )

        if (providers.isNotEmpty()) {
            Text(
                stringResource(R.string.login_or), Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                providers.forEach { provider ->
                    OutlinedButton({ onProvider(provider) }, Modifier.fillMaxWidth().height(56.dp)) {
                        Text(stringResource(R.string.continue_with, provider.displayName))
                    }
                }
            }
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
    LoginScreen(listOf(OAuthProvider("google", "Google", "", "")), busy = false, error = "Failed to authenticate.", onSignIn = { _, _, _ -> }, onProvider = {})
}
