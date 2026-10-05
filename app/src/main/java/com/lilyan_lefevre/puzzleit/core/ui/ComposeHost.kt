package com.lilyan_lefevre.puzzleit.core.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.fragment.app.Fragment
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleTheme

/**
 * The bridge between the Fragments (which still do the navigation) and Compose: a Fragment's `onCreateView` returns
 * `screen { MyScreen(...) }`. The screen gets the app theme and the dark background; it is dropped when the Fragment's view is.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Fragment.screen(content: @Composable () -> Unit): ComposeView = ComposeView(requireContext()).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        PuzzleTheme {
            // testTagsAsResourceId: the tests tag nodes with names (like the old view ids), and the screen recorder / UiAutomator read them as ids.
            Surface(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }, color = MaterialTheme.colorScheme.background) { content() }
        }
    }
}
