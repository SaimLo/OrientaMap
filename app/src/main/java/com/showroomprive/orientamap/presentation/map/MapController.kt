package com.showroomprive.orientamap.presentation.map

import android.location.Location

interface MapController {
    fun moveCameraTo(location: Location)
    fun updateUserLocation(location: Location)
}
