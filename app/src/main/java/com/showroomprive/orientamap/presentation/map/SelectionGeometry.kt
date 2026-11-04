package com.showroomprive.orientamap.presentation.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

internal enum class DragTarget {
    NONE,
    BODY,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
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
    return when (target) {
        DragTarget.BODY -> {
            val newLeft = (startRect.left + delta.x)
                .coerceIn(0f, (containerWidth - startRect.width).coerceAtLeast(0f))
            val newTop = (startRect.top + delta.y)
                .coerceIn(0f, (containerHeight - startRect.height).coerceAtLeast(0f))
            Rect(newLeft, newTop, newLeft + startRect.width, newTop + startRect.height)
        }
        DragTarget.TOP_LEFT -> {
            val newLeft = (startRect.left + delta.x)
                .coerceIn(0f, startRect.right - minSize)
            val newTop = (startRect.top + delta.y)
                .coerceIn(0f, startRect.bottom - minSize)
            Rect(newLeft, newTop, startRect.right, startRect.bottom)
        }
        DragTarget.TOP_RIGHT -> {
            val newRight = (startRect.right + delta.x)
                .coerceIn(startRect.left + minSize, containerWidth)
            val newTop = (startRect.top + delta.y)
                .coerceIn(0f, startRect.bottom - minSize)
            Rect(startRect.left, newTop, newRight, startRect.bottom)
        }
        DragTarget.BOTTOM_LEFT -> {
            val newLeft = (startRect.left + delta.x)
                .coerceIn(0f, startRect.right - minSize)
            val newBottom = (startRect.bottom + delta.y)
                .coerceIn(startRect.top + minSize, containerHeight)
            Rect(newLeft, startRect.top, startRect.right, newBottom)
        }
        DragTarget.BOTTOM_RIGHT -> {
            val newRight = (startRect.right + delta.x)
                .coerceIn(startRect.left + minSize, containerWidth)
            val newBottom = (startRect.bottom + delta.y)
                .coerceIn(startRect.top + minSize, containerHeight)
            Rect(startRect.left, startRect.top, newRight, newBottom)
        }
        DragTarget.NONE -> startRect
    }
}
