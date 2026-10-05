package com.lilyan_lefevre.puzzleit.feature.puzzle

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Far below any screen: where the sheet waits before its first layout. */
private const val OUT_OF_SIGHT = 1_000_000f

/** The four resting places of the result sheet. */
enum class SheetLevel {
    /** Swiped away: the map is free (a "Leads" pill brings the sheet back). */
    Hidden,

    /** Only the lead chips show. */
    Peek,

    /** The piece next to the box. */
    Half,

    /** Almost the whole screen: why these leads. */
    Full,
}

/**
 * Where the result sheet is, and how it moves between its [SheetLevel]s.
 *
 * Everything is in pixels from the top of the screen: [offset] is the sheet's top edge, [anchors] says which offset
 * each level sits at (small number = high on the screen). The screen fills [anchors] once it knows the screen
 * and the content heights. A finger moves [offset] directly ([dragBy]); letting go glides to the nearest level
 * ([settle]); [animateTo] goes to a given level.
 */
@Stable
class LevelSheetState(private val scope: CoroutineScope) {

    /** The level the sheet is at, or heading to. Starts at [SheetLevel.Half], where a new result opens. */
    var level by mutableStateOf(SheetLevel.Half)
        private set

    /** Top edge of the sheet in pixels from the top of its container. Big until the first layout: the sheet starts out of sight. */
    var offset by mutableFloatStateOf(OUT_OF_SIGHT)
        private set

    var anchors by mutableStateOf<Map<SheetLevel, Float>>(emptyMap())
        private set

    private var animation: Job? = null

    fun updateAnchors(newAnchors: Map<SheetLevel, Float>) {
        val moved = newAnchors[level] != anchors[level]
        anchors = newAnchors
        if (offset > newAnchors.getValue(SheetLevel.Hidden)) offset = newAnchors.getValue(SheetLevel.Hidden) // first layout
        if (moved) animateTo(level) // the content got taller or shorter: follow
    }

    fun animateTo(target: SheetLevel, velocity: Float = 0f) {
        level = target
        val to = anchors[target] ?: return
        animation?.cancel()
        animation = scope.launch {
            animate(offset, to, velocity, spring(stiffness = Spring.StiffnessMediumLow)) { value, _ -> offset = value }
        }
    }

    /** Back to the start: out of sight, ready to slide in at the half level for the next result. */
    fun reset() {
        animation?.cancel()
        level = SheetLevel.Half
        offset = anchors[SheetLevel.Hidden] ?: OUT_OF_SIGHT
    }

    /** A finger moved the sheet by [dy] pixels. Returns how much of it the sheet really moved (it stops at the highest and lowest level). */
    fun dragBy(dy: Float): Float {
        if (anchors.isEmpty()) return 0f
        animation?.cancel()
        val next = (offset + dy).coerceIn(anchors.values.min(), anchors.values.max())
        return (next - offset).also { offset = next }
    }

    /** The finger left the sheet at [velocity] px/s (positive = downwards): glide to the level it would reach in a quarter of a second. */
    fun settle(velocity: Float) {
        val projected = offset + velocity * 0.25f
        val target = anchors.minByOrNull { abs(it.value - projected) }?.key ?: return
        animateTo(target, velocity)
    }

    /** True while the sheet is anywhere but at its highest level. */
    val canGrow: Boolean get() = anchors[SheetLevel.Full]?.let { offset > it + 0.5f } ?: false
}

@Composable
fun rememberLevelSheetState(): LevelSheetState {
    val scope = rememberCoroutineScope()
    return remember { LevelSheetState(scope) }
}

/**
 * A sheet that slides between the levels of [state]. [content] is the scrolling part (give it a `verticalScroll`);
 * the sheet and its content share the finger: dragging up first lifts the sheet to the top, then scrolls the content,
 * and scrolling back down to the start of the content lowers the sheet again.
 *
 * [height] is the sheet's own height (the screen minus the gap kept at the top when it is fully up).
 */
@Composable
fun LevelSheet(state: LevelSheetState, height: Dp, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val connection = remember(state) {
        object : NestedScrollConnection {
            // Finger going up while the sheet is not at the top: the sheet takes the movement before the content.
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                if (available.y < 0 && state.canGrow) Offset(0f, state.dragBy(available.y)) else Offset.Zero

            // Finger going down and the content is already at its start: the leftover lowers the sheet.
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                if (available.y > 0) Offset(0f, state.dragBy(available.y)) else Offset.Zero

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!state.canGrow) return Velocity.Zero
                state.settle(available.y)
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                state.settle(available.y)
                return available
            }
        }
    }
    Column(
        modifier
            .offset { IntOffset(0, state.offset.roundToInt()) }
            .testTag("sheet") // after offset: the node's bounds must follow the sheet, the tests aim their swipes with them
            .fillMaxWidth().height(height)
            .clip(shape).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .nestedScroll(connection)
            .draggable(
                rememberDraggableState { state.dragBy(it) }, Orientation.Vertical,
                onDragStopped = { state.settle(it) },
            ),
    ) {
        Grabber()
        content()
    }
}

/** The little bar on top of a sheet that says "drag me". */
@Composable
fun Grabber(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 40.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.outline))
    }
}
