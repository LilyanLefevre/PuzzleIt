package com.lilyan_lefevre.puzzleit.feature.project.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.FileImage
import com.lilyan_lefevre.puzzleit.core.ui.PuzzleCard
import com.lilyan_lefevre.puzzleit.core.ui.PuzzleLoader
import com.lilyan_lefevre.puzzleit.core.ui.RoundIconButton
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleText
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleTheme
import com.lilyan_lefevre.puzzleit.feature.project.data.Project
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The home screen: a title, the account button, one card per puzzle and the "new puzzle" button.
 * [projects] is null while the database is still loading.
 */
@Composable
fun ProjectListScreen(
    projects: List<Project>?,
    syncing: Boolean,
    onOpen: (Project) -> Unit,
    onAccount: () -> Unit,
    onNew: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)) {
                Text(stringResource(R.string.app_tagline).uppercase(), style = PuzzleText.eyebrow, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.my_puzzles), Modifier.padding(top = 4.dp), style = PuzzleText.display(40), color = MaterialTheme.colorScheme.onSurface)
            }
            if (projects != null) ProjectCards(projects, onOpen)
        }

        // Thin bar over the very top while the list is synced with the server.
        if (syncing) {
            LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
        RoundIconButton(R.drawable.ic_account, stringResource(R.string.account_button), onAccount, Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 24.dp).testTag("buttonAccount"))

        when {
            projects == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            projects.isEmpty() -> Column(Modifier.align(Alignment.Center).padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PuzzleLoader(size = 120.dp)
                Text(
                    stringResource(R.string.no_puzzles_yet), Modifier.padding(top = 24.dp),
                    fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNew,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).height(64.dp).testTag("fabAddProject"),
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
            text = { Text(stringResource(R.string.new_puzzle_project), fontWeight = FontWeight.Bold) },
        )
    }
}

@Composable
private fun ProjectCards(projects: List<Project>, onOpen: (Project) -> Unit) {
    // Ids already shown once: a card falls into place the first time it appears, not every time it scrolls back.
    val alreadyShown = remember { mutableSetOf<String>() }
    LazyColumn(
        Modifier.fillMaxSize().testTag("projectList"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(projects, key = { it.id }) { project ->
            val firstTime = remember { alreadyShown.add(project.id) }
            ProjectCard(project, onOpen = { onOpen(project) }, fallIn = firstTime)
        }
    }
}

/** One puzzle: thumbnail, name, number of pieces and creation date. */
@Composable
fun ProjectCard(project: Project, onOpen: () -> Unit, modifier: Modifier = Modifier, fallIn: Boolean = false) {
    // Starts 12 % lower and transparent, settles over 420 ms (the old list animation).
    val appear = remember { Animatable(if (fallIn) 0f else 1f) }
    LaunchedEffect(Unit) { if (fallIn) appear.animateTo(1f, tween(420)) }

    PuzzleCard(
        modifier.fillMaxWidth().testTag("project_${project.id}").graphicsLayer { alpha = appear.value; translationY = (1f - appear.value) * 60.dp.toPx() },
        onClick = onOpen, corner = 24.dp, contentPadding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FileImage(
                project.thumbnailPath.ifEmpty { project.imagePath }, stringResource(R.string.project_thumbnail),
                Modifier.size(88.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                maxSizePx = 256,
            )
            Column(Modifier.padding(start = 16.dp, end = 8.dp)) {
                Text(project.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    stringResource(R.string.pieces_name, project.puzzleSize),
                    Modifier.padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 10.dp, vertical = 3.dp),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(project.creationDate)),
                    Modifier.padding(top = 8.dp), fontSize = 13.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun ProjectListPreview() = PuzzleTheme {
    val demo = Project(id = "1", name = "La vague", puzzleSize = 1000, imagePath = "", thumbnailPath = "")
    ProjectListScreen(listOf(demo, demo.copy(id = "2", name = "Famille", puzzleSize = 500)), syncing = true, onOpen = {}, onAccount = {}, onNew = {})
}
