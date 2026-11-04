package com.showroomprive.orientamap.presentation.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class ZoneSelectionTest {

    private val rect = Rect(100f, 100f, 300f, 300f)
    private val handleRadius = 48f
    private val containerWidth = 1000f
    private val containerHeight = 1000f

    @Test
    fun testDetectDragTargetTopLeft() {
        val target = detectDragTarget(Offset(105f, 105f), rect, handleRadius)
        assertEquals(DragTarget.TOP_LEFT, target)
    }

    @Test
    fun testDetectDragTargetTopRight() {
        val target = detectDragTarget(Offset(295f, 105f), rect, handleRadius)
        assertEquals(DragTarget.TOP_RIGHT, target)
    }

    @Test
    fun testDetectDragTargetBottomLeft() {
        val target = detectDragTarget(Offset(105f, 295f), rect, handleRadius)
        assertEquals(DragTarget.BOTTOM_LEFT, target)
    }

    @Test
    fun testDetectDragTargetBottomRight() {
        val target = detectDragTarget(Offset(295f, 295f), rect, handleRadius)
        assertEquals(DragTarget.BOTTOM_RIGHT, target)
    }

    @Test
    fun testDetectDragTargetBody() {
        val target = detectDragTarget(Offset(200f, 200f), rect, handleRadius)
        assertEquals(DragTarget.BODY, target)
    }

    @Test
    fun testDetectDragTargetJustOutside() {
        val target = detectDragTarget(Offset(40f, 40f), rect, handleRadius)
        assertEquals(DragTarget.NONE, target)
    }

    @Test
    fun testDetectDragTargetFarAway() {
        val target = detectDragTarget(Offset(800f, 800f), rect, handleRadius)
        assertEquals(DragTarget.NONE, target)
    }

    @Test
    fun testDetectDragTargetNearMapBoundaries() {
        val boundaryRect = Rect(0f, 0f, 100f, 100f)
        val targetTL = detectDragTarget(Offset(0f, 0f), boundaryRect, handleRadius)
        assertEquals(DragTarget.TOP_LEFT, targetTL)

        val targetOutside = detectDragTarget(Offset(-50f, -50f), boundaryRect, handleRadius)
        assertEquals(DragTarget.NONE, targetOutside)
    }

    @Test
    fun testUpdateSelectionRectTopLeftResize() {
        val delta = Offset(20f, 30f)
        val updated = updateSelectionRect(
            target = DragTarget.TOP_LEFT,
            startRect = rect,
            delta = delta,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            minSize = 80f
        )
        assertEquals(120f, updated.left)
        assertEquals(130f, updated.top)
        assertEquals(300f, updated.right)
        assertEquals(300f, updated.bottom)
    }

    @Test
    fun testUpdateSelectionRectTopRightResize() {
        val delta = Offset(20f, -30f)
        val updated = updateSelectionRect(
            target = DragTarget.TOP_RIGHT,
            startRect = rect,
            delta = delta,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            minSize = 80f
        )
        assertEquals(100f, updated.left)
        assertEquals(70f, updated.top)
        assertEquals(320f, updated.right)
        assertEquals(300f, updated.bottom)
    }

    @Test
    fun testUpdateSelectionRectBottomLeftResize() {
        val delta = Offset(-20f, 20f)
        val updated = updateSelectionRect(
            target = DragTarget.BOTTOM_LEFT,
            startRect = rect,
            delta = delta,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            minSize = 80f
        )
        assertEquals(80f, updated.left)
        assertEquals(100f, updated.top)
        assertEquals(300f, updated.right)
        assertEquals(320f, updated.bottom)
    }

    @Test
    fun testUpdateSelectionRectBottomRightResize() {
        val delta = Offset(30f, 40f)
        val updated = updateSelectionRect(
            target = DragTarget.BOTTOM_RIGHT,
            startRect = rect,
            delta = delta,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            minSize = 80f
        )
        assertEquals(100f, updated.left)
        assertEquals(100f, updated.top)
        assertEquals(330f, updated.right)
        assertEquals(340f, updated.bottom)
    }

    @Test
    fun testUpdateSelectionRectBodyMovePreservesSize() {
        val delta = Offset(50f, 50f)
        val updated = updateSelectionRect(
            target = DragTarget.BODY,
            startRect = rect,
            delta = delta,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            minSize = 80f
        )
        assertEquals(150f, updated.left)
        assertEquals(150f, updated.top)
        assertEquals(350f, updated.right)
        assertEquals(350f, updated.bottom)
        assertEquals(rect.width, updated.width)
        assertEquals(rect.height, updated.height)
    }

    @Test
    fun testUpdateSelectionRectEnforcesMinSize() {
        val delta = Offset(300f, 300f) // Try to shrink rect to negative size
        val minSize = 80f
        val updated = updateSelectionRect(
            target = DragTarget.TOP_LEFT,
            startRect = rect,
            delta = delta,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            minSize = minSize
        )
        assertEquals(rect.right - minSize, updated.left)
        assertEquals(rect.bottom - minSize, updated.top)
        assertEquals(minSize, updated.width)
        assertEquals(minSize, updated.height)
    }

    @Test
    fun testUpdateSelectionRectClampsToContainerBounds() {
        val delta = Offset(-200f, -200f) // Try to move outside top-left
        val updated = updateSelectionRect(
            target = DragTarget.BODY,
            startRect = rect,
            delta = delta,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            minSize = 80f
        )
        assertEquals(0f, updated.left)
        assertEquals(0f, updated.top)
        assertEquals(rect.width, updated.width)
        assertEquals(rect.height, updated.height)
    }
}
