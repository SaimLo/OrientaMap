package com.showroomprive.orientamap.data.importer

import com.showroomprive.orientamap.domain.model.IsomFeatureSource
import com.showroomprive.orientamap.domain.model.Wgs84Coordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RgeAltiContourImporterTest {

    private val recordCoordinates = listOf(
        Wgs84Coordinate(3.0, 46.5),
        Wgs84Coordinate(3.001, 46.501)
    )

    @Test
    fun importsAndProjectsContourCoordinates() {
        val result = RgeAltiContourImporter().import(
            listOf(RgeAltiContourRecord(105.0, recordCoordinates))
        )

        assertEquals(1, result.size)
        assertEquals(IsomFeatureSource.IGN_IMPORT, result.single().source)
        assertEquals(700_000.0, result.single().coordinates.first().eastingMeters, 0.001)
        assertEquals(6_600_000.0, result.single().coordinates.first().northingMeters, 0.001)
    }

    @Test
    fun marksEveryTwentyFiveMetersAsIndexContour() {
        val result = RgeAltiContourImporter().import(
            listOf(
                RgeAltiContourRecord(100.0, recordCoordinates),
                RgeAltiContourRecord(105.0, recordCoordinates),
                RgeAltiContourRecord(125.0, recordCoordinates)
            )
        )

        assertTrue(result[0].isIndexContour)
        assertFalse(result[1].isIndexContour)
        assertTrue(result[2].isIndexContour)
    }
}
