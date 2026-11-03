package com.showroomprive.orientamap.presentation.map

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class RectResizeTest {

    @Test
    fun testRectBodyDrag() {
        val initial = Rect(100f, 100f, 300f, 300f)
        val dx = 50f
        val dy = -20f
        val moved = Rect(initial.left + dx, initial.top + dy, initial.right + dx, initial.bottom + dy)
        assertEquals(150f, moved.left)
        assertEquals(80f, moved.top)
        assertEquals(350f, moved.right)
        assertEquals(280f, moved.bottom)
    }

    @Test
    fun testRectTopLeftResize() {
        val initial = Rect(100f, 100f, 300f, 300f)
        val minSize = 80f
        val newLeft = (initial.left + 20f).coerceIn(0f, initial.right - minSize)
        val newTop = (initial.top + 30f).coerceIn(0f, initial.bottom - minSize)
        val resized = Rect(newLeft, newTop, initial.right, initial.bottom)
        assertEquals(120f, resized.left)
        assertEquals(130f, resized.top)
    }
}
