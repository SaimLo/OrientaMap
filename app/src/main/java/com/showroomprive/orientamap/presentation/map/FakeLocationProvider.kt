package com.showroomprive.orientamap.presentation.map

import android.location.Location

class FakeLocationProvider(
    private val initialLocation: Location? = null
) : LocationProvider {
    private var callback: ((Location) -> Unit)? = null

    override fun start(onLocation: (Location) -> Unit) {
        callback = onLocation
        initialLocation?.let { onLocation(it) }
    }

    override fun stop() {
        callback = null
    }

    fun emitLocation(location: Location) {
        callback?.invoke(location)
    }
}
