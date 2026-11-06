package com.showroomprive.orientamap.domain.model

import java.util.UUID

data class IsomContour(
    val id: UUID = UUID.randomUUID(),
    val elevationMeters: Double,
    val coordinates: List<ProjectedCoordinate>,
    val isIndexContour: Boolean,
    val source: IsomFeatureSource = IsomFeatureSource.IGN_IMPORT,
    val confidence: Double = 1.0,
    val isFieldVerified: Boolean = false
) {
    init {
        require(coordinates.size >= 2) {
            "Une courbe de niveau doit contenir au moins deux coordonnées"
        }
        require(elevationMeters.isFinite()) {
            "L'altitude de la courbe doit être finie"
        }
    }

    fun toIsomFeature(symbolCode: String): IsomFeature = IsomFeature(
        id = id,
        type = if (isIndexContour) {
            IsomFeatureType.INDEX_CONTOUR
        } else {
            IsomFeatureType.CONTOUR
        },
        symbolCode = symbolCode,
        geometry = IsomGeometry.LineString(coordinates),
        source = source,
        confidence = confidence,
        isFieldVerified = isFieldVerified
    )
}
