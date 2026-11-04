package com.showroomprive.orientamap.presentation.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionGeometryTest {
    private val rect = Rect(100f, 100f, 500f, 400f)
    private val radius = 48f

    @Test
    fun detectsBodyAndOutside() {
        assertEquals(DragTarget.BODY, detectDragTarget(Offset(300f, 250f), rect, radius))
        assertEquals(DragTarget.NONE, detectDragTarget(Offset(700f, 250f), rect, radius))
    }

    @Test
    fun detectsAllHandles() {
        assertEquals(DragTarget.TOP_LEFT, detectDragTarget(Offset(100f, 100f), rect, radius))
        assertEquals(DragTarget.TOP_RIGHT, detectDragTarget(Offset(500f, 100f), rect, radius))
        assertEquals(DragTarget.BOTTOM_LEFT, detectDragTarget(Offset(100f, 400f), rect, radius))
        assertEquals(DragTarget.BOTTOM_RIGHT, detectDragTarget(Offset(500f, 400f), rect, radius))
    }

    @Test
    fun outsideDoesNotChangeRect() {
        val result = updateSelectionRect(
            target = DragTarget.NONE,
            startRect = rect,
            delta = Offset(100f, 100f),
            containerWidth = 800f,
            containerHeight = 600f,
            minSize = 80f
        )
        assertEquals(rect, result)
    }

    @Test
    fun bodyMoveStaysInsideContainer() {
        val result = updateSelectionRect(
            target = DragTarget.BODY,
            startRect = rect,
            delta = Offset(500f, 400f),
            containerWidth = 800f,
            containerHeight = 600f,
            minSize = 80f
        )
        assertEquals(400f, result.left, 0.001f)
        assertEquals(300f, result.top, 0.001f)
        assertTrue(result.right <= 800f)
        assertTrue(result.bottom <= 600f)
    }

    @Test
    fun topLeftResizePreservesOppositeCornerAndMinimumSize() {
        val result = updateSelectionRect(
            target = DragTarget.TOP_LEFT,
            startRect = rect,
            delta = Offset(450f, 350f),
            containerWidth = 800f,
            containerHeight = 600f,
            minSize = 80f
        )
        assertEquals(rect.right, result.right, 0.001f)
        assertEquals(rect.bottom, result.bottom, 0.001f)
        assertTrue(result.width >= 80f)
        assertTrue(result.height >= 80f)
    }
}
