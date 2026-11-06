package com.showroomprive.orientamap.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class Lambert93ProjectionTest {

    @Test
    fun projectsLambert93Origin() {
        val result = Lambert93Projection.project(
            Wgs84Coordinate(longitudeDegrees = 3.0, latitudeDegrees = 46.5)
        )

        assertEquals(700_000.0, result.eastingMeters, 0.001)
        assertEquals(6_600_000.0, result.northingMeters, 0.001)
    }

    @Test
    fun projectsParisWithinExpectedLambert93Range() {
        val result = Lambert93Projection.project(
            Wgs84Coordinate(longitudeDegrees = 2.3522, latitudeDegrees = 48.8566)
        )

        assertTrue(result.eastingMeters in 652_000.0..653_000.0)
        assertTrue(result.northingMeters in 6_861_000.0..6_863_000.0)
    }

    @Test
    fun rejectsInvalidLatitude() {
        assertThrows(IllegalArgumentException::class.java) {
            Lambert93Projection.project(Wgs84Coordinate(3.0, 91.0))
        }
    }
}
