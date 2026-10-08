package de.yummify.app.ui.components

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import kotlin.math.roundToInt

/**
 * A bar that follows the finger while the user scrolls: it slides away when the content moves up and
 * comes back as soon as the user scrolls back (Material "enter always" behavior), and settles on
 * "shown" or "hidden" when the finger lifts. State is read in layout and draw only, so scrolling does
 * not recompose anything.
 */
@Stable
class HideOnScrollState(initialHeightPx: Float = 0f) {
    var heightPx by mutableFloatStateOf(initialHeightPx)
        private set
    /** 0 = fully visible, [heightPx] = fully hidden. */
    var offsetPx by mutableFloatStateOf(0f)
        private set

    val hiddenFraction: Float get() = if (heightPx <= 0f) 0f else (offsetPx / heightPx).coerceIn(0f, 1f)
    /** How much of the bar is still on screen. Content that sits above the bar uses this as bottom padding. */
    val visiblePx: Float get() = (heightPx - offsetPx).coerceAtLeast(0f)

    fun updateHeight(px: Float) {
        heightPx = px.coerceAtLeast(0f)
        offsetPx = offsetPx.coerceIn(0f, heightPx)
    }

    /** [scrollDeltaY] is the nested scroll delta: negative while the content moves up (finger moves up). */
    fun dragBy(scrollDeltaY: Float) {
        offsetPx = (offsetPx - scrollDeltaY).coerceIn(0f, heightPx)
    }

    /** Where the bar rests after the finger lifts: the nearer end. */
    fun settleTarget(): Float = if (offsetPx > heightPx / 2f) heightPx else 0f

    suspend fun settle() = animateTo(settleTarget())
    suspend fun show() = animateTo(0f)

    fun snapShow() { offsetPx = 0f }

    private suspend fun animateTo(target: Float) {
        if (offsetPx == target) return
        animate(offsetPx, target, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { value, _ ->
            offsetPx = value.coerceIn(0f, heightPx)
        }
    }
}

/** Feeds scroll movement into a [HideOnScrollState] without consuming any of it. */
class HideOnScrollConnection(private val state: HideOnScrollState, private val enabled: () -> Boolean = { true }) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (enabled()) state.dragBy(available.y)
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (enabled()) state.settle()
        return Velocity.Zero
    }
}

/** The bottom navigation bar of the app; screens with a scrolling list hide it through [hideBottomBarOnScroll]. */
val LocalBottomBarScroll = staticCompositionLocalOf<HideOnScrollState?> { null }

/** True while a screen reader explores by touch; bars then stay put because hidden controls cannot be reached. */
@Composable
fun rememberTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager }
    var enabled by remember { mutableStateOf(manager?.isTouchExplorationEnabled == true) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled = it }
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    return enabled
}

/**
 * Hides the bottom bar while this list scrolls down and shows it again on the way back up.
 * A list that fits on the screen, or a running screen reader, keeps the bar visible.
 */
@Composable
fun Modifier.hideBottomBarOnScroll(scrollable: ScrollableState): Modifier {
    val bar = LocalBottomBarScroll.current ?: return this
    val touchExploration = rememberTouchExplorationEnabled()
    val canScroll = { !touchExploration && (scrollable.canScrollForward || scrollable.canScrollBackward) }
    LaunchedEffect(bar, scrollable, touchExploration) {
        snapshotFlow(canScroll).collect { if (!it) bar.show() }
    }
    return nestedScroll(remember(bar, scrollable, touchExploration) { HideOnScrollConnection(bar, canScroll) })
}

/** Replaces `padding(bottom = inset())`, but reads the value while laying out, so animating it does not recompose. */
fun Modifier.bottomInset(inset: () -> Dp): Modifier = layout { measurable, constraints ->
    val px = inset().roundToPx().coerceAtLeast(0)
    val placeable = measurable.measure(constraints.offset(vertical = -px))
    layout(placeable.width, (placeable.height + px).coerceIn(constraints.minHeight, constraints.maxHeight.coerceAtLeast(constraints.minHeight))) {
        placeable.place(0, 0)
    }
}

/**
 * A header that moves with the finger and shrinks the space it takes, so the list below grows with it.
 * Use it with `Modifier.nestedScroll(behavior.nestedScrollConnection)` on a parent of the list and a
 * behavior from `TopAppBarDefaults.enterAlwaysScrollBehavior()`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingHeader(scrollBehavior: TopAppBarScrollBehavior, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = scrollBehavior.state
    Layout(
        content = {
            Box(Modifier.onSizeChanged { size ->
                val limit = -size.height.toFloat()
                if (state.heightOffsetLimit != limit) state.heightOffsetLimit = limit
            }) { content() }
        },
        modifier = modifier.clipToBounds()
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val offset = state.heightOffset.roundToInt().coerceIn(-placeable.height, 0)
        layout(placeable.width, placeable.height + offset) { placeable.place(0, offset) }
    }
}

/** Brings a collapsed header back when the list below no longer fills the screen, so nothing stays out of reach. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeepHeaderReachable(scrollBehavior: TopAppBarScrollBehavior, scrollable: ScrollableState) {
    LaunchedEffect(scrollBehavior, scrollable) {
        snapshotFlow { scrollable.canScrollForward || scrollable.canScrollBackward }.collect { canScroll ->
            if (!canScroll) {
                val state = scrollBehavior.state
                animate(state.heightOffset, 0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { value, _ -> state.heightOffset = value }
            }
        }
    }
}

/** Convenience for screens: the height of the bar in the unit the layout uses. */
val NoBottomInset: () -> Dp = { 0.dp }
