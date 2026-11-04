package com.showroomprive.orientamap.presentation.map

import android.location.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeLocationProviderTest {

    @Test
    fun testStartAndEmitLocation() {
        val provider = FakeLocationProvider()
        val received = mutableListOf<Location>()

        assertFalse(provider.isListening)

        provider.start { received.add(it) }
        assertTrue(provider.isListening)

        val loc1 = Location("provider_1")
        provider.emitLocation(loc1)

        assertEquals(1, received.size)
        assertSame(loc1, received[0])
    }

    @Test
    fun testStartWithInitialLocation() {
        val initialLoc = Location("initial")
        val provider = FakeLocationProvider(initialLocation = initialLoc)
        val received = mutableListOf<Location>()

        provider.start { received.add(it) }

        assertEquals(1, received.size)
        assertSame(initialLoc, received[0])
    }

    @Test
    fun testEmitMultipleLocations() {
        val provider = FakeLocationProvider()
        val received = mutableListOf<Location>()

        provider.start { received.add(it) }

        val loc1 = Location("loc1")
        val loc2 = Location("loc2")
        val loc3 = Location("loc3")

        provider.emitLocation(loc1)
        provider.emitLocation(loc2)
        provider.emitLocation(loc3)

        assertEquals(3, received.size)
        assertSame(loc1, received[0])
        assertSame(loc2, received[1])
        assertSame(loc3, received[2])
    }

    @Test
    fun testStopListeningNoFurtherLocationsReceived() {
        val provider = FakeLocationProvider()
        val received = mutableListOf<Location>()

        provider.start { received.add(it) }

        val loc1 = Location("loc1")
        provider.emitLocation(loc1)
        assertEquals(1, received.size)

        provider.stop()
        assertFalse(provider.isListening)

        val loc2 = Location("loc2")
        provider.emitLocation(loc2)
        assertEquals(1, received.size)
    }
}
