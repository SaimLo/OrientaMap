package com.showroomprive.orientamap.presentation.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.showroomprive.orientamap.domain.model.GeoBounds

data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double
)

/** Converts screen pixels into geographic WGS84 coordinates. */
fun interface ScreenCoordinateProjector {
    fun project(point: Offset): GeoCoordinate
}

/**
 * Converts a screen rectangle into a geographic bounding box.
 * All four corners are projected because the map projection can vary across
 * the rectangle, especially when the map is rotated or tilted.
 */
fun Rect.toGeoBounds(projector: ScreenCoordinateProjector): GeoBounds {
    require(width > 0f) { "The screen rectangle must have a positive width" }
    require(height > 0f) { "The screen rectangle must have a positive height" }

    val corners = listOf(
        Offset(left, top),
        Offset(right, top),
        Offset(left, bottom),
        Offset(right, bottom)
    ).map(projector::project)

    return GeoBounds(
        north = corners.maxOf { it.latitude },
        south = corners.minOf { it.latitude },
        east = corners.maxOf { it.longitude },
        west = corners.minOf { it.longitude }
    )
}
