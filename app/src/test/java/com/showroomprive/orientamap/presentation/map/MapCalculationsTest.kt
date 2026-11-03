package com.showroomprive.orientamap.presentation.map

import org.junit.Assert.assertEquals
import org.junit.Test

class MapCalculationsTest {

    @Test
    fun testFormatDistanceMeters() {
        assertEquals("50 m", formatDistance(50.4))
        assertEquals("999 m", formatDistance(999.0))
    }

    @Test
    fun testFormatDistanceKilometers() {
        assertEquals("1.50 km", formatDistance(1500.0))
        assertEquals("10.00 km", formatDistance(10000.0))
    }

    @Test
    fun testFormatAreaSquareMeters() {
        assertEquals("500 m²", formatArea(500.0))
        assertEquals("9999 m²", formatArea(9999.0))
    }

    @Test
    fun testFormatAreaHectares() {
        val result = formatArea(15000.0)
        assert(result.contains("ha"))
    }
}
