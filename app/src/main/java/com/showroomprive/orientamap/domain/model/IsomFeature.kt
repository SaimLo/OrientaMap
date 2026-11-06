package com.showroomprive.orientamap.domain.model

import java.util.UUID

data class ProjectedCoordinate(
    val eastingMeters: Double,
    val northingMeters: Double
)

sealed interface IsomGeometry {
    data class Point(
        val coordinate: ProjectedCoordinate
    ) : IsomGeometry

    data class LineString(
        val coordinates: List<ProjectedCoordinate>
    ) : IsomGeometry {
        init {
            require(coordinates.size >= 2) {
                "Une ligne ISOM doit contenir au moins deux coordonnées"
            }
        }
    }

    data class Polygon(
        val rings: List<List<ProjectedCoordinate>>
    ) : IsomGeometry {
        init {
            require(rings.isNotEmpty()) { "Un polygone ISOM doit contenir un anneau" }
            require(rings.all { it.size >= 3 }) {
                "Chaque anneau ISOM doit contenir au moins trois coordonnées"
            }
        }
    }
}

enum class IsomFeatureType {
    CONTOUR,
    INDEX_CONTOUR,
    WATER,
    WATERCOURSE,
    PATH,
    ROAD,
    BUILDING,
    OPEN_LAND,
    VEGETATION,
    ROCK,
    POINT_FEATURE
}

enum class IsomFeatureSource {
    IGN_IMPORT,
    FIELD_SURVEY,
    MANUAL_EDIT
}

data class IsomFeature(
    val id: UUID = UUID.randomUUID(),
    val type: IsomFeatureType,
    val symbolCode: String,
    val geometry: IsomGeometry,
    val source: IsomFeatureSource,
    val confidence: Double = 1.0,
    val isFieldVerified: Boolean = false
) {
    init {
        require(symbolCode.isNotBlank()) { "Le code symbole ISOM est obligatoire" }
        require(confidence in 0.0..1.0) {
            "La confiance doit être comprise entre 0 et 1"
        }
    }
}

data class IsomMapLayer(
    val features: List<IsomFeature>,
    val coordinateReferenceSystem: String = "EPSG:2154"
)
