package com.showroomprive.orientamap.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class IsomContourTest {

    @Test
    fun convertsIndexContourToIndexIsomFeature() {
        val contour = IsomContour(
            elevationMeters = 125.0,
            coordinates = listOf(
                ProjectedCoordinate(650_000.0, 6_860_000.0),
                ProjectedCoordinate(650_100.0, 6_860_100.0)
            ),
            isIndexContour = true
        )

        val feature = contour.toIsomFeature("101.1")

        assertEquals(IsomFeatureType.INDEX_CONTOUR, feature.type)
        assertEquals("101.1", feature.symbolCode)
    }

    @Test
    fun preservesElevationAndVerificationMetadata() {
        val contour = IsomContour(
            elevationMeters = 130.0,
            coordinates = listOf(
                ProjectedCoordinate(650_000.0, 6_860_000.0),
                ProjectedCoordinate(650_100.0, 6_860_100.0)
            ),
            isIndexContour = false,
            source = IsomFeatureSource.FIELD_SURVEY,
            confidence = 0.75,
            isFieldVerified = true
        )

        assertEquals(130.0, contour.elevationMeters, 0.001)
        assertEquals(IsomFeatureSource.FIELD_SURVEY, contour.source)
        assertEquals(0.75, contour.confidence, 0.001)
        assertEquals(true, contour.isFieldVerified)
    }

    @Test
    fun rejectsContourWithOnePoint() {
        assertThrows(IllegalArgumentException::class.java) {
            IsomContour(
                elevationMeters = 100.0,
                coordinates = listOf(ProjectedCoordinate(1.0, 1.0)),
                isIndexContour = false
            )
        }
    }
}
