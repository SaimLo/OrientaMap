package com.showroomprive.orientamap.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class IsomFeatureTest {

    @Test
    fun acceptsProjectedLineFeature() {
        val feature = IsomFeature(
            type = IsomFeatureType.PATH,
            symbolCode = "505",
            geometry = IsomGeometry.LineString(
                listOf(
                    ProjectedCoordinate(650_000.0, 6_860_000.0),
                    ProjectedCoordinate(650_010.0, 6_860_020.0)
                )
            ),
            source = IsomFeatureSource.IGN_IMPORT,
            confidence = 0.8
        )

        assertEquals(IsomFeatureType.PATH, feature.type)
        assertEquals("505", feature.symbolCode)
        assertEquals("EPSG:2154", IsomMapLayer(listOf(feature)).coordinateReferenceSystem)
    }

    @Test
    fun acceptsPolygonWithAnOuterRing() {
        val polygon = IsomGeometry.Polygon(
            listOf(
                listOf(
                    ProjectedCoordinate(650_000.0, 6_860_000.0),
                    ProjectedCoordinate(650_100.0, 6_860_000.0),
                    ProjectedCoordinate(650_100.0, 6_860_100.0),
                    ProjectedCoordinate(650_000.0, 6_860_000.0)
                )
            )
        )

        assertEquals(1, polygon.rings.size)
    }

    @Test
    fun rejectsInvalidConfidence() {
        assertThrows(IllegalArgumentException::class.java) {
            IsomFeature(
                type = IsomFeatureType.WATER,
                symbolCode = "301",
                geometry = IsomGeometry.Point(ProjectedCoordinate(1.0, 1.0)),
                source = IsomFeatureSource.MANUAL_EDIT,
                confidence = 1.5
            )
        }
    }

    @Test
    fun rejectsTooShortLine() {
        assertThrows(IllegalArgumentException::class.java) {
            IsomGeometry.LineString(listOf(ProjectedCoordinate(1.0, 1.0)))
        }
    }
}
