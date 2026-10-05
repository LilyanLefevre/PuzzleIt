package com.lilyan_lefevre.puzzleit.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lilyan_lefevre.puzzleit.R

/** The "puzzle night" font: one variable file, three weights (same ones as `font/space_grotesk_family.xml`). */
@OptIn(ExperimentalTextApi::class)
val SpaceGrotesk = FontFamily(
    listOf(400, 500, 700).map { w ->
        Font(R.font.space_grotesk, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

/** Titles and small-caps labels: the two text styles the old `Text.Display` / `Text.Eyebrow` XML styles gave. */
object PuzzleText {
    /** Big bold titles. Pass the size you need (26 for a screen title, 40 for the list). */
    fun display(sizeSp: Int) = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = sizeSp.sp, letterSpacing = (-0.02).em)

    /** Small upper-case label in the accent colour. Callers upper-case the string themselves. */
    val eyebrow = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.14.em)
}

/** Colours the Material scheme has no slot for. */
object PuzzleColors {
    val warning @Composable get() = colorResource(R.color.puzzle_warning)
    val scrim @Composable get() = colorResource(R.color.puzzle_scrim)
    val secondaryText @Composable get() = colorResource(R.color.puzzle_secondary_text)
}

/**
 * Wraps every screen. The colours come from `res/values/colors.xml` (the same file the remaining Views read),
 * so a colour is changed in one place. The app is dark only, there is no light scheme.
 */
@Composable
fun PuzzleTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = colorResource(R.color.puzzle_primary),
        onPrimary = colorResource(R.color.puzzle_on_primary),
        primaryContainer = colorResource(R.color.puzzle_primary_container),
        onPrimaryContainer = colorResource(R.color.puzzle_on_primary_container),
        secondary = colorResource(R.color.puzzle_secondary),
        onSecondary = colorResource(R.color.puzzle_on_secondary),
        secondaryContainer = colorResource(R.color.puzzle_secondary_container),
        onSecondaryContainer = colorResource(R.color.puzzle_on_secondary_container),
        background = colorResource(R.color.puzzle_background),
        onBackground = colorResource(R.color.puzzle_on_surface),
        surface = colorResource(R.color.puzzle_surface),
        onSurface = colorResource(R.color.puzzle_on_surface),
        onSurfaceVariant = colorResource(R.color.puzzle_on_surface_variant),
        surfaceVariant = colorResource(R.color.puzzle_surface_high),
        surfaceContainer = colorResource(R.color.puzzle_surface),
        surfaceContainerHigh = colorResource(R.color.puzzle_surface_high),
        outline = colorResource(R.color.puzzle_outline),
        outlineVariant = colorResource(R.color.puzzle_outline),
        error = colorResource(R.color.puzzle_error),
    )
    // Every Material text style gets the app font; sizes stay Material's.
    val base = Typography()
    val typography = Typography(
        displayLarge = base.displayLarge.copy(fontFamily = SpaceGrotesk), displayMedium = base.displayMedium.copy(fontFamily = SpaceGrotesk),
        displaySmall = base.displaySmall.copy(fontFamily = SpaceGrotesk), headlineLarge = base.headlineLarge.copy(fontFamily = SpaceGrotesk),
        headlineMedium = base.headlineMedium.copy(fontFamily = SpaceGrotesk), headlineSmall = base.headlineSmall.copy(fontFamily = SpaceGrotesk),
        titleLarge = base.titleLarge.copy(fontFamily = SpaceGrotesk), titleMedium = base.titleMedium.copy(fontFamily = SpaceGrotesk),
        titleSmall = base.titleSmall.copy(fontFamily = SpaceGrotesk), bodyLarge = base.bodyLarge.copy(fontFamily = SpaceGrotesk),
        bodyMedium = base.bodyMedium.copy(fontFamily = SpaceGrotesk), bodySmall = base.bodySmall.copy(fontFamily = SpaceGrotesk),
        labelLarge = base.labelLarge.copy(fontFamily = SpaceGrotesk), labelMedium = base.labelMedium.copy(fontFamily = SpaceGrotesk),
        labelSmall = base.labelSmall.copy(fontFamily = SpaceGrotesk),
    )
    // Same corners as Shape.Puzzle.Small / Medium / Large.
    val shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp))
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
