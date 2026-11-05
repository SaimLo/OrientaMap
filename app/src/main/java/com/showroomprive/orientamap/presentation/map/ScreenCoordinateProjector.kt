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
 * The top-left screen point is north-west and the bottom-right point is south-east.
 */
fun Rect.toGeoBounds(projector: ScreenCoordinateProjector): GeoBounds {
    require(width > 0f) { "The screen rectangle must have a positive width" }
    require(height > 0f) { "The screen rectangle must have a positive height" }

    val northWest = projector.project(Offset(left, top))
    val southEast = projector.project(Offset(right, bottom))

    return GeoBounds(
        north = maxOf(northWest.latitude, southEast.latitude),
        south = minOf(northWest.latitude, southEast.latitude),
        east = maxOf(northWest.longitude, southEast.longitude),
        west = minOf(northWest.longitude, southEast.longitude)
    )
}
