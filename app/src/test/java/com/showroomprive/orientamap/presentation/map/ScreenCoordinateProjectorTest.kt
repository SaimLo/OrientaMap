package com.showroomprive.orientamap.presentation.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ScreenCoordinateProjectorTest {
    private val projector = ScreenCoordinateProjector { point ->
        GeoCoordinate(
            latitude = 50.0 - point.y / 100.0,
            longitude = 2.0 + point.x / 100.0
        )
    }

    @Test
    fun convertsScreenRectToGeoBounds() {
        val bounds = Rect(100f, 200f, 500f, 600f).toGeoBounds(projector)

        assertEquals(48.0, bounds.north, 0.000001)
        assertEquals(44.0, bounds.south, 0.000001)
        assertEquals(7.0, bounds.east, 0.000001)
        assertEquals(3.0, bounds.west, 0.000001)
    }

    @Test
    fun normalizesCoordinatesWhenProjectorReturnsReversedLatitude() {
        val reversedProjector = ScreenCoordinateProjector { point ->
            GeoCoordinate(
                latitude = point.y / 100.0,
                longitude = point.x / 100.0
            )
        }

        val bounds = Rect(0f, 0f, 100f, 100f).toGeoBounds(reversedProjector)

        assertEquals(1.0, bounds.north, 0.000001)
        assertEquals(0.0, bounds.south, 0.000001)
        assertEquals(1.0, bounds.east, 0.000001)
        assertEquals(0.0, bounds.west, 0.000001)
    }

    @Test
    fun rejectsEmptyRectangles() {
        assertThrows(IllegalArgumentException::class.java) {
            Rect(10f, 10f, 10f, 100f).toGeoBounds(projector)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Rect(10f, 10f, 100f, 10f).toGeoBounds(projector)
        }
    }
}
