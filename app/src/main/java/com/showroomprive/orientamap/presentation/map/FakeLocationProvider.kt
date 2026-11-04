package com.showroomprive.orientamap.presentation.map

import android.location.Location

class FakeLocationProvider(
    private val initialLocation: Location? = null
) : LocationProvider {
    private var callback: ((Location) -> Unit)? = null
    var isListening: Boolean = false
        private set

    override fun start(onLocation: (Location) -> Unit) {
        callback = onLocation
        isListening = true
        initialLocation?.let { onLocation(it) }
    }

    override fun stop() {
        callback = null
        isListening = false
    }

    fun emitLocation(location: Location) {
        callback?.invoke(location)
    }
}
