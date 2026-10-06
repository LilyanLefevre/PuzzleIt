package com.lilyan_lefevre.puzzleit.feature.project.create

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.ScreenHeader

/** Lets the user adjust the 4 corners of the box on the photo; [onConfirmed] hands back the corners and the rectified photo. */
@Composable
fun PuzzleBoundsRoute(
    imagePath: String,
    onBack: () -> Unit,
    onConfirmed: (quadJson: String, rectifiedPath: String) -> Unit,
    viewModel: PuzzleBoundsViewModel = hiltViewModel(),
) {
    val detected by viewModel.detected.collectAsStateWithLifecycle()
    val confirmed by viewModel.confirmed.collectAsStateWithLifecycle()

    LaunchedEffect(imagePath) { viewModel.detect(imagePath) }
    LaunchedEffect(confirmed) { confirmed?.let { onConfirmed(it.quadJson, it.rectifiedPath) } }

    // No toolbar any more: this screen draws its own header, like the others.
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(stringResource(R.string.puzzle_bounds_title), onBack)
        Box(Modifier.weight(1f)) {
            PuzzleBoundsScreen(
                imagePath = imagePath,
                sourceSize = remember(imagePath) { viewModel.sourceSize(imagePath) },
                detected = detected,
                onCancel = onBack,
                onConfirm = { viewModel.confirm(imagePath, it) },
            )
        }
    }
}
