package com.lilyan_lefevre.puzzleit.feature.account

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lilyan_lefevre.puzzleit.BuildConfig
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.feature.account.data.Account
import com.lilyan_lefevre.puzzleit.feature.account.data.AccountStore
import com.lilyan_lefevre.puzzleit.feature.account.data.BackendException
import com.lilyan_lefevre.puzzleit.feature.account.data.OAuthProvider
import com.lilyan_lefevre.puzzleit.feature.account.data.PocketBaseClient
import com.lilyan_lefevre.puzzleit.feature.account.data.Session
import com.lilyan_lefevre.puzzleit.feature.account.data.isAcceptableServer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the login screen tells the person after an action. */
sealed interface Notice {
    /** [detail] is the server's own message or the network error, [reason] a string resource when the cause is on our side. */
    data class Failed(@StringRes val reason: Int?, val detail: String = "") : Notice
}

/** Signing in to the PuzzleIt server built into the app (through a provider such as Google) and syncing the puzzles with it. */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val client: PocketBaseClient,
    private val store: AccountStore,
) : ViewModel() {

    private val server = BuildConfig.PUZZLEIT_SERVER

    val account: StateFlow<Account?> = store.account

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _notice = MutableStateFlow<Notice?>(null)
    val notice: StateFlow<Notice?> = _notice.asStateFlow()

    /** The "continue with ..." buttons: whatever the server has switched on, null until it answered (also when it is unreachable: see [providersFailed]). */
    private val _providers = MutableStateFlow<List<OAuthProvider>?>(null)
    val providers: StateFlow<List<OAuthProvider>?> = _providers.asStateFlow()

    /** A provider's page for the route to open in the browser, cleared once it did. */
    private val _openUrl = MutableStateFlow<String?>(null)
    val openUrl: StateFlow<String?> = _openUrl.asStateFlow()

    /** True while the providers are being read (first time or on retry). */
    private val _providersLoading = MutableStateFlow(false)
    val providersLoading: StateFlow<Boolean> = _providersLoading.asStateFlow()

    /** True when the last read of the providers failed: the screen offers a retry. */
    private val _providersFailed = MutableStateFlow(false)
    val providersFailed: StateFlow<Boolean> = _providersFailed.asStateFlow()

    init { loadProviders() }

    fun loadProviders() {
        if (_providersLoading.value) return
        _providersLoading.value = true
        _providersFailed.value = false
        viewModelScope.launch {
            try {
                _providers.value = client.providers(server)
            } catch (e: BackendException) {
                _providersFailed.value = true
            } finally {
                _providersLoading.value = false
            }
        }
    }

    fun signInWith(provider: OAuthProvider) = authenticate { client.signInWithProvider(server, provider) { _openUrl.value = it } }

    fun urlOpened() { _openUrl.value = null }

    fun signOut() = store.signOut()

    /** No sync here: signing in closes this screen, and the puzzle list syncs by itself when it shows. */
    private fun authenticate(getSession: suspend () -> Session) {
        if (!isAcceptableServer(server)) { _notice.value = Notice.Failed(R.string.server_invalid); return }
        run { store.signIn(server, getSession()); null }
    }

    private fun run(block: suspend () -> Notice?) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            _notice.value = try {
                block()
            } catch (e: BackendException) {
                Notice.Failed(null, e.message.orEmpty())
            } finally {
                _busy.value = false
            }
        }
    }
}
