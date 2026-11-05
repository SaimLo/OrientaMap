package com.showroomprive.orientamap.presentation.map

import android.location.Location
import com.showroomprive.orientamap.domain.model.GeoBounds

interface MapController {
    fun moveCameraTo(location: Location)
    fun updateUserLocation(location: Location)

    /**
     * Provides screen-to-geographic projection when the backing map supports it.
     * Test doubles may keep the default null implementation.
     */
    fun screenCoordinateProjector(): ScreenCoordinateProjector? = null
    fun moveCameraToBounds(bounds: GeoBounds) = Unit
    fun showSelectedArea(bounds: GeoBounds) = Unit
}
