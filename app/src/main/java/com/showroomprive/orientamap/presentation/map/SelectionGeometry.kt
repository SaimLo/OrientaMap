package com.showroomprive.orientamap.presentation.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

enum class SelectionHandle {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

internal enum class DragTarget {
    NONE,
    BODY,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

fun resizeTopLeft(rect: Rect, delta: Offset, containerSize: Size, minSize: Float): Rect {
    val newLeft = (rect.left + delta.x).coerceIn(0f, rect.right - minSize)
    val newTop = (rect.top + delta.y).coerceIn(0f, rect.bottom - minSize)
    return Rect(newLeft, newTop, rect.right, rect.bottom)
}

fun resizeTopRight(rect: Rect, delta: Offset, containerSize: Size, minSize: Float): Rect {
    val newRight = (rect.right + delta.x).coerceIn(rect.left + minSize, containerSize.width)
    val newTop = (rect.top + delta.y).coerceIn(0f, rect.bottom - minSize)
    return Rect(rect.left, newTop, newRight, rect.bottom)
}

fun resizeBottomLeft(rect: Rect, delta: Offset, containerSize: Size, minSize: Float): Rect {
    val newLeft = (rect.left + delta.x).coerceIn(0f, rect.right - minSize)
    val newBottom = (rect.bottom + delta.y).coerceIn(rect.top + minSize, containerSize.height)
    return Rect(newLeft, rect.top, rect.right, newBottom)
}

fun resizeBottomRight(rect: Rect, delta: Offset, containerSize: Size, minSize: Float): Rect {
    val newRight = (rect.right + delta.x).coerceIn(rect.left + minSize, containerSize.width)
    val newBottom = (rect.bottom + delta.y).coerceIn(rect.top + minSize, containerSize.height)
    return Rect(rect.left, rect.top, newRight, newBottom)
}

internal fun detectDragTarget(
    point: Offset,
    rect: Rect,
    handleTouchRadius: Float
): DragTarget {
    val topLeftDistance = (point - Offset(rect.left, rect.top)).getDistance()
    val topRightDistance = (point - Offset(rect.right, rect.top)).getDistance()
    val bottomLeftDistance = (point - Offset(rect.left, rect.bottom)).getDistance()
    val bottomRightDistance = (point - Offset(rect.right, rect.bottom)).getDistance()

    return when {
        topLeftDistance <= handleTouchRadius -> DragTarget.TOP_LEFT
        topRightDistance <= handleTouchRadius -> DragTarget.TOP_RIGHT
        bottomLeftDistance <= handleTouchRadius -> DragTarget.BOTTOM_LEFT
        bottomRightDistance <= handleTouchRadius -> DragTarget.BOTTOM_RIGHT
        rect.contains(point) -> DragTarget.BODY
        else -> DragTarget.NONE
    }
}

internal fun updateSelectionRect(
    target: DragTarget,
    startRect: Rect,
    delta: Offset,
    containerWidth: Float,
    containerHeight: Float,
    minSize: Float
): Rect {
    val containerSize = Size(containerWidth, containerHeight)
    return when (target) {
        DragTarget.BODY -> startRect
        DragTarget.TOP_LEFT -> resizeTopLeft(startRect, delta, containerSize, minSize)
        DragTarget.TOP_RIGHT -> resizeTopRight(startRect, delta, containerSize, minSize)
        DragTarget.BOTTOM_LEFT -> resizeBottomLeft(startRect, delta, containerSize, minSize)
        DragTarget.BOTTOM_RIGHT -> resizeBottomRight(startRect, delta, containerSize, minSize)
        DragTarget.NONE -> startRect
    }
}
