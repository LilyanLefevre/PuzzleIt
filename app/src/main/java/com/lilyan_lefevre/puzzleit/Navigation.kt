package com.lilyan_lefevre.puzzleit

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.lilyan_lefevre.puzzleit.feature.account.AccountRoute
import com.lilyan_lefevre.puzzleit.feature.account.LoginRoute
import com.lilyan_lefevre.puzzleit.feature.account.data.AccountStore
import com.lilyan_lefevre.puzzleit.feature.history.HistoryRoute
import com.lilyan_lefevre.puzzleit.feature.progress.ProgressRoute
import com.lilyan_lefevre.puzzleit.feature.project.create.Framing
import com.lilyan_lefevre.puzzleit.feature.project.create.ProjectCreationRoute
import com.lilyan_lefevre.puzzleit.feature.project.create.PuzzleBoundsRoute
import com.lilyan_lefevre.puzzleit.feature.project.list.ProjectListRoute
import com.lilyan_lefevre.puzzleit.feature.puzzle.PuzzleWorkingRoute
import com.lilyan_lefevre.puzzleit.feature.puzzle.capture.PieceCaptureRoute
import kotlinx.serialization.Serializable

/** Every screen of the app and the arguments it needs: the routes of the NavHost (typed, no string to get wrong). */
@Serializable
sealed interface Dest {
    /** Shown instead of the list while nobody is signed in. */
    @Serializable data object Login : Dest
    @Serializable data object ProjectList : Dest
    @Serializable data object Account : Dest
    /** [projectId] is null when creating, the puzzle's id when editing. */
    @Serializable data class ProjectCreation(val projectId: String? = null) : Dest
    @Serializable data class PuzzleBounds(val imagePath: String) : Dest
    @Serializable data class PuzzleWorking(val projectId: String) : Dest
    @Serializable data class History(val projectId: String) : Dest
    @Serializable data class Progress(val projectId: String) : Dest
    @Serializable data object PieceCapture : Dest
}

/**
 * Results that a screen hands to the one below it, written in the saved state of the entry underneath:
 * the piece photo (camera -> table) and the box framing (corners + rectified photo, framing -> creation).
 */
const val PIECE_PHOTO = "piecePhoto"
private const val BOUNDS_QUAD = "boundsQuad"
private const val BOUNDS_PATH = "boundsPath"

private const val SLIDE_MS = 300
private const val FADE_MS = 90

/** The one NavHost of the app. Screens know nothing about it: they get lambdas, this is where they are wired. */
@Composable
fun PuzzleNavHost(navController: NavHostController, accountStore: AccountStore) {
    // Nobody signed in: the app opens on the login screen, never on the list.
    val start: Dest = if (accountStore.account.value != null) Dest.ProjectList else Dest.Login

    // Signing in leaves the login screen; signing out (or the server refusing the token) brings it back, whatever screen is open.
    val account by accountStore.account.collectAsStateWithLifecycle()
    LaunchedEffect(account == null) {
        val current = navController.currentDestination ?: return@LaunchedEffect
        val onLogin = current.hasRoute<Dest.Login>()
        if (account == null && !onLogin) {
            navController.navigate(Dest.Login) { popUpTo(navController.graph.id) { inclusive = true } }
        } else if (account != null && onLogin) {
            navController.navigate(Dest.ProjectList) { popUpTo<Dest.Login> { inclusive = true } }
        }
    }

    NavHost(
        navController, startDestination = start,
        enterTransition = { enter(forward = true) },
        exitTransition = { exit(forward = true) },
        popEnterTransition = { enter(forward = false) },
        popExitTransition = { exit(forward = false) },
    ) {
        composable<Dest.Login> { LoginRoute() }

        composable<Dest.ProjectList> {
            ProjectListRoute(
                onOpen = { navController.navigate(Dest.PuzzleWorking(it)) },
                onAccount = { navController.navigate(Dest.Account) },
                onNew = { navController.navigate(Dest.ProjectCreation()) },
            )
        }

        composable<Dest.Account> { AccountRoute(onBack = navController::navigateUp) }

        composable<Dest.ProjectCreation> { entry ->
            val saved = entry.savedStateHandle
            val quad by saved.getStateFlow<String?>(BOUNDS_QUAD, null).collectAsStateWithLifecycle()
            val path by saved.getStateFlow<String?>(BOUNDS_PATH, null).collectAsStateWithLifecycle()
            val (q, p) = quad to path
            ProjectCreationRoute(
                projectId = entry.toRoute<Dest.ProjectCreation>().projectId,
                framing = if (q != null && p != null) Framing(q, p) else null,
                onFramingConsumed = { saved.remove<String>(BOUNDS_QUAD); saved.remove<String>(BOUNDS_PATH) },
                onBack = navController::navigateUp,
                onOpenBounds = { navController.navigate(Dest.PuzzleBounds(it)) },
            )
        }

        composable<Dest.PuzzleBounds> { entry ->
            PuzzleBoundsRoute(
                imagePath = entry.toRoute<Dest.PuzzleBounds>().imagePath,
                onBack = navController::navigateUp,
                onConfirmed = { quadJson, rectifiedPath ->
                    navController.previousBackStackEntry?.savedStateHandle?.let {
                        it[BOUNDS_QUAD] = quadJson
                        it[BOUNDS_PATH] = rectifiedPath
                    }
                    navController.popBackStack()
                },
            )
        }

        composable<Dest.PuzzleWorking> { entry ->
            val projectId = entry.toRoute<Dest.PuzzleWorking>().projectId
            val photo by entry.savedStateHandle.getStateFlow<String?>(PIECE_PHOTO, null).collectAsStateWithLifecycle()
            PuzzleWorkingRoute(
                projectId = projectId,
                newPhoto = photo,
                onPhotoConsumed = { entry.savedStateHandle[PIECE_PHOTO] = null },
                onBack = navController::navigateUp,
                onEdit = { navController.navigate(Dest.ProjectCreation(projectId)) },
                onHistory = { navController.navigate(Dest.History(projectId)) },
                onProgress = { navController.navigate(Dest.Progress(projectId)) },
                onScan = { navController.navigate(Dest.PieceCapture) },
            )
        }

        composable<Dest.History> { HistoryRoute(onBack = navController::navigateUp) }

        composable<Dest.Progress> { ProgressRoute(onBack = navController::navigateUp) }

        composable<Dest.PieceCapture> {
            PieceCaptureRoute(
                onPhoto = { path ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(PIECE_PHOTO, path)
                    navController.navigateUp()
                },
                onClose = navController::navigateUp,
            )
        }
    }
}

// Transitions (they replace the Fragments' MaterialSharedAxis): screens slide a little along X and cross-fade, the camera comes from below (Y), the login just fades.
private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(forward: Boolean): EnterTransition = when {
    targetState.destination.hasRoute<Dest.Login>() || initialState.destination.hasRoute<Dest.Login>() -> fadeIn(tween(SLIDE_MS))
    targetState.destination.hasRoute<Dest.PieceCapture>() -> fadeIn(tween(SLIDE_MS)) + slideInVertically(tween(SLIDE_MS)) { it / 12 }
    initialState.destination.hasRoute<Dest.PieceCapture>() -> EnterTransition.None
    else -> fadeIn(tween(SLIDE_MS, delayMillis = FADE_MS)) + slideInHorizontally(tween(SLIDE_MS)) { if (forward) it / 12 else -it / 12 }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(forward: Boolean): ExitTransition = when {
    targetState.destination.hasRoute<Dest.Login>() || initialState.destination.hasRoute<Dest.Login>() -> fadeOut(tween(SLIDE_MS))
    initialState.destination.hasRoute<Dest.PieceCapture>() -> fadeOut(tween(SLIDE_MS)) + slideOutVertically(tween(SLIDE_MS)) { it / 12 }
    targetState.destination.hasRoute<Dest.PieceCapture>() -> ExitTransition.None
    else -> fadeOut(tween(FADE_MS)) + slideOutHorizontally(tween(SLIDE_MS)) { if (forward) -it / 12 else it / 12 }
}
