package com.showroomprive.orientamap.data.importer

import com.showroomprive.orientamap.domain.model.IsomContour
import com.showroomprive.orientamap.domain.model.IsomFeatureSource
import com.showroomprive.orientamap.domain.model.Lambert93Projection
import com.showroomprive.orientamap.domain.model.ProjectedCoordinate
import com.showroomprive.orientamap.domain.model.Wgs84Coordinate

data class RgeAltiContourRecord(
    val elevationMeters: Double,
    val coordinates: List<Wgs84Coordinate>
)

class RgeAltiContourImporter(
    private val indexIntervalMeters: Double = 25.0
) {

    init {
        require(indexIntervalMeters > 0.0) {
            "L'intervalle des courbes maîtresses doit être positif"
        }
    }

    fun import(records: List<RgeAltiContourRecord>): List<IsomContour> =
        records.map { record ->
            require(record.coordinates.size >= 2) {
                "Une courbe RGE ALTI doit contenir au moins deux coordonnées"
            }
            val projectedCoordinates = record.coordinates.map(Lambert93Projection::project)
            IsomContour(
                elevationMeters = record.elevationMeters,
                coordinates = projectedCoordinates,
                isIndexContour = isIndexContour(record.elevationMeters),
                source = IsomFeatureSource.IGN_IMPORT
            )
        }

    private fun isIndexContour(elevationMeters: Double): Boolean {
        val remainder = elevationMeters % indexIntervalMeters
        return remainder == 0.0 ||
            kotlin.math.abs(remainder - indexIntervalMeters) < 0.000001
    }
}
