package com.showroomprive.orientamap.presentation.map

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import org.maplibre.android.maps.MapLibreMap

/** MapLibre adapter kept outside the pure geometry code. */
class MapLibreScreenCoordinateProjector(
    private val map: MapLibreMap
) : ScreenCoordinateProjector {
    override fun project(point: Offset): GeoCoordinate {
        val latLng = map.projection.fromScreenLocation(PointF(point.x, point.y))
        return GeoCoordinate(
            latitude = latLng.latitude,
            longitude = latLng.longitude
        )
    }
}
