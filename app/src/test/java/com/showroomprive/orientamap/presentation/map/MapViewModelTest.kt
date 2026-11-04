package com.showroomprive.orientamap.presentation.map

import android.location.Location
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MapViewModelTest {

    @Test
    fun testUpdateLocation() {
        val viewModel = MapViewModel()
        assertNull(viewModel.location)

        val loc = Location("test_provider")
        viewModel.updateLocation(loc)

        assertSame(loc, viewModel.location)
    }

    @Test
    fun testCenteredOnLocationFlag() {
        val viewModel = MapViewModel()
        assertFalse(viewModel.centeredOnLocation)

        viewModel.centeredOnLocation = true
        assertTrue(viewModel.centeredOnLocation)
    }

    @Test
    fun testSelectionModeAndRect() {
        val viewModel = MapViewModel()
        assertFalse(viewModel.isSelectionModeActive)
        assertNull(viewModel.selectionRect)

        viewModel.isSelectionModeActive = true
        assertTrue(viewModel.isSelectionModeActive)

        val rect = Rect(10f, 20f, 100f, 200f)
        viewModel.selectionRect = rect
        assertEquals(rect, viewModel.selectionRect)
    }
}
