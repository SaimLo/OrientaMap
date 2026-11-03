package com.showroomprive.orientamap.presentation.map

import android.location.Location

interface LocationProvider {
    fun start(onLocation: (Location) -> Unit)
    fun stop()
}
