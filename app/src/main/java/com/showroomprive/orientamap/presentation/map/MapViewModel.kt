package com.showroomprive.orientamap.presentation.map

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import com.showroomprive.orientamap.domain.model.GeoBounds
import androidx.lifecycle.ViewModel

class MapViewModel : ViewModel() {
    var location by mutableStateOf<Location?>(null)
        private set
    var isSelectionModeActive by mutableStateOf(false)
    var selectionRect by mutableStateOf<Rect?>(null)
    var selectedGeoBounds by mutableStateOf<GeoBounds?>(null)
        private set
    var centeredOnLocation by mutableStateOf(false)
    var centeredOnProjectBounds by mutableStateOf(false)

    fun updateLocation(newLocation: Location?) {
        location = newLocation
    }

    fun updateSelectedGeoBounds(bounds: GeoBounds?) {
        selectedGeoBounds = bounds
    }
}
