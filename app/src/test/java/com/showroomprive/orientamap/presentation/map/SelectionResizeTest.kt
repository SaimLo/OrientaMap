package com.showroomprive.orientamap.presentation.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionResizeTest {
    private val initialRect = Rect(200f, 200f, 600f, 500f)
    private val containerSize = Size(1000f, 1000f)
    private val minSize = 80f

    @Test
    fun testResizeTopLeft() {
        val delta = Offset(50f, 50f)
        val result = resizeTopLeft(initialRect, delta, containerSize, minSize)
        assertEquals(250f, result.left)
        assertEquals(250f, result.top)
        assertEquals(initialRect.right, result.right)
        assertEquals(initialRect.bottom, result.bottom)
    }

    @Test
    fun testResizeTopRight() {
        val delta = Offset(50f, 50f)
        val result = resizeTopRight(initialRect, delta, containerSize, minSize)
        assertEquals(initialRect.left, result.left)
        assertEquals(250f, result.top)
        assertEquals(650f, result.right)
        assertEquals(initialRect.bottom, result.bottom)
    }

    @Test
    fun testResizeBottomLeft() {
        val delta = Offset(50f, 50f)
        val result = resizeBottomLeft(initialRect, delta, containerSize, minSize)
        assertEquals(250f, result.left)
        assertEquals(initialRect.top, result.top)
        assertEquals(initialRect.right, result.right)
        assertEquals(550f, result.bottom)
    }

    @Test
    fun testResizeBottomRight() {
        val delta = Offset(50f, 50f)
        val result = resizeBottomRight(initialRect, delta, containerSize, minSize)
        assertEquals(initialRect.left, result.left)
        assertEquals(initialRect.top, result.top)
        assertEquals(650f, result.right)
        assertEquals(550f, result.bottom)
    }

    @Test
    fun testConservationOfOppositeCorner() {
        val resTL = resizeTopLeft(initialRect, Offset(30f, 30f), containerSize, minSize)
        assertEquals(initialRect.right, resTL.right)
        assertEquals(initialRect.bottom, resTL.bottom)

        val resTR = resizeTopRight(initialRect, Offset(30f, 30f), containerSize, minSize)
        assertEquals(initialRect.left, resTR.left)
        assertEquals(initialRect.bottom, resTR.bottom)

        val resBL = resizeBottomLeft(initialRect, Offset(30f, 30f), containerSize, minSize)
        assertEquals(initialRect.right, resBL.right)
        assertEquals(initialRect.top, resBL.top)

        val resBR = resizeBottomRight(initialRect, Offset(30f, 30f), containerSize, minSize)
        assertEquals(initialRect.left, resBR.left)
        assertEquals(initialRect.top, resBR.top)
    }

    @Test
    fun testRespectMinimumWidth() {
        val delta = Offset(450f, 0f)
        val result = resizeTopLeft(initialRect, delta, containerSize, minSize)
        assertEquals(initialRect.right - minSize, result.left)
        assertEquals(minSize, result.width)
    }

    @Test
    fun testRespectMinimumHeight() {
        val delta = Offset(0f, -400f)
        val result = resizeBottomRight(initialRect, delta, containerSize, minSize)
        assertEquals(initialRect.top + minSize, result.bottom)
        assertEquals(minSize, result.height)
    }

    @Test
    fun testRespectScreenLimits() {
        val delta = Offset(-300f, -300f)
        val result = resizeTopLeft(initialRect, delta, containerSize, minSize)
        assertEquals(0f, result.left)
        assertEquals(0f, result.top)

        val delta2 = Offset(500f, 600f)
        val result2 = resizeBottomRight(initialRect, delta2, containerSize, minSize)
        assertEquals(containerSize.width, result2.right)
        assertEquals(containerSize.height, result2.bottom)
    }

    @Test
    fun testPreventInversion() {
        val delta = Offset(800f, 800f)
        val result = resizeTopLeft(initialRect, delta, containerSize, minSize)
        assertEquals(initialRect.right - minSize, result.left)
        assertEquals(initialRect.bottom - minSize, result.top)
        assertTrue(result.width >= minSize)
        assertTrue(result.height >= minSize)
    }

    @Test
    fun testZeroDelta() {
        val resultTL = resizeTopLeft(initialRect, Offset.Zero, containerSize, minSize)
        assertEquals(initialRect, resultTL)

        val resultTR = resizeTopRight(initialRect, Offset.Zero, containerSize, minSize)
        assertEquals(initialRect, resultTR)

        val resultBL = resizeBottomLeft(initialRect, Offset.Zero, containerSize, minSize)
        assertEquals(initialRect, resultBL)

        val resultBR = resizeBottomRight(initialRect, Offset.Zero, containerSize, minSize)
        assertEquals(initialRect, resultBR)
    }

    @Test
    fun testMovementTowardsEachScreenEdge() {
        val resLeft = resizeTopLeft(initialRect, Offset(-200f, 0f), containerSize, minSize)
        assertEquals(0f, resLeft.left)

        val resTop = resizeTopLeft(initialRect, Offset(0f, -200f), containerSize, minSize)
        assertEquals(0f, resTop.top)

        val resRight = resizeBottomRight(initialRect, Offset(500f, 0f), containerSize, minSize)
        assertEquals(containerSize.width, resRight.right)

        val resBottom = resizeBottomRight(initialRect, Offset(0f, 600f), containerSize, minSize)
        assertEquals(containerSize.height, resBottom.bottom)
    }
}
