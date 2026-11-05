package com.showroomprive.orientamap.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineTileEstimatorTest {

    private val world = GeoBounds(
        north = 85.05112878,
        south = -85.05112878,
        east = 180.0,
        west = -180.0
    )

    @Test
    fun estimatesAllWorldTilesForZoomRange() {
        val estimate = OfflineTileEstimator.estimate(
            bounds = world,
            minZoom = 0,
            maxZoom = 2
        )

        assertEquals(mapOf(0 to 1L, 1 to 4L, 2 to 16L), estimate.tilesByZoom)
        assertEquals(21L, estimate.totalTiles)
        assertTrue(estimate.estimatedSizeBytes == null)
    }

    @Test
    fun estimatesSizeFromAverageTileSize() {
        val estimate = OfflineTileEstimator.estimate(
            bounds = world,
            minZoom = 0,
            maxZoom = 1,
            averageTileBytes = 10L
        )

        assertEquals(5L, estimate.totalTiles)
        assertEquals(50L, estimate.estimatedSizeBytes)
    }

    @Test
    fun estimatesARealisticSmallArea() {
        val estimate = OfflineTileEstimator.estimate(
            bounds = GeoBounds(
                north = 48.9,
                south = 48.7,
                east = 2.5,
                west = 2.1
            ),
            minZoom = 10,
            maxZoom = 12
        )

        assertTrue(estimate.totalTiles > 0L)
        assertEquals(3, estimate.tilesByZoom.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvertedZoomRange() {
        OfflineTileEstimator.estimate(world, minZoom = 4, maxZoom = 3)
    }
}
