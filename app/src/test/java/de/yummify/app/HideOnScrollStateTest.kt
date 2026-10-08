package de.yummify.app

import de.yummify.app.ui.components.HideOnScrollState
import org.junit.Assert.assertEquals
import org.junit.Test

class HideOnScrollStateTest {
    @Test fun followsFingerAndClamps() {
        val bar = HideOnScrollState(100f)
        bar.dragBy(-30f)                       // finger moves up, content scrolls down
        assertEquals(30f, bar.offsetPx, 0.001f)
        bar.dragBy(-500f)
        assertEquals(100f, bar.offsetPx, 0.001f)
        bar.dragBy(40f)                        // scroll back
        assertEquals(60f, bar.offsetPx, 0.001f)
        bar.dragBy(500f)
        assertEquals(0f, bar.offsetPx, 0.001f)
    }

    @Test fun settlesOnNearerEnd() {
        val bar = HideOnScrollState(100f)
        bar.dragBy(-49f); assertEquals(0f, bar.settleTarget(), 0.001f)
        bar.dragBy(-2f); assertEquals(100f, bar.settleTarget(), 0.001f)
    }

    @Test fun visibleSpaceShrinksWithTheBar() {
        val bar = HideOnScrollState(80f)
        bar.dragBy(-20f)
        assertEquals(60f, bar.visiblePx, 0.001f)
        assertEquals(0.25f, bar.hiddenFraction, 0.001f)
    }

    @Test fun heightChangeKeepsOffsetInRange() {
        val bar = HideOnScrollState(100f)
        bar.dragBy(-100f)
        bar.updateHeight(60f)
        assertEquals(60f, bar.offsetPx, 0.001f)
        bar.snapShow()
        assertEquals(0f, bar.offsetPx, 0.001f)
    }
}
