package com.showroomprive.orientamap.presentation.map

import android.location.Location

class FakeMapController : MapController {
    val movedCameraLocations = mutableListOf<Location>()
    val updatedUserLocations = mutableListOf<Location>()

    override fun moveCameraTo(location: Location) {
        movedCameraLocations.add(location)
    }

    override fun updateUserLocation(location: Location) {
        updatedUserLocations.add(location)
    }

    fun clear() {
        movedCameraLocations.clear()
        updatedUserLocations.clear()
    }
}
