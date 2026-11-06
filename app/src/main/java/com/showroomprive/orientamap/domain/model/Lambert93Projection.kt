package com.showroomprive.orientamap.domain.model

import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class Wgs84Coordinate(
    val longitudeDegrees: Double,
    val latitudeDegrees: Double
)

object Lambert93Projection {

    private const val SEMI_MAJOR_AXIS = 6_378_137.0
    private const val ECCENTRICITY = 0.0818191910428158
    private const val FALSE_EASTING = 700_000.0
    private const val FALSE_NORTHING = 6_600_000.0

    private val centralMeridian = Math.toRadians(3.0)
    private val latitudeOfOrigin = Math.toRadians(46.5)
    private val firstStandardParallel = Math.toRadians(44.0)
    private val secondStandardParallel = Math.toRadians(49.0)

    private val projectionN: Double = run {
        val m1 = m(firstStandardParallel)
        val m2 = m(secondStandardParallel)
        val t1 = t(firstStandardParallel)
        val t2 = t(secondStandardParallel)
        ln(m1 / m2) / ln(t1 / t2)
    }

    private val projectionF: Double =
        m(firstStandardParallel) / (projectionN * t(firstStandardParallel).pow(projectionN))

    private val originRadius: Double =
        SEMI_MAJOR_AXIS * projectionF * t(latitudeOfOrigin).pow(projectionN)

    fun project(coordinate: Wgs84Coordinate): ProjectedCoordinate {
        require(coordinate.longitudeDegrees in -180.0..180.0) {
            "La longitude doit être comprise entre -180 et 180 degrés"
        }
        require(coordinate.latitudeDegrees in -90.0..90.0) {
            "La latitude doit être comprise entre -90 et 90 degrés"
        }

        val longitude = Math.toRadians(coordinate.longitudeDegrees)
        val latitude = Math.toRadians(coordinate.latitudeDegrees)
        val radius = SEMI_MAJOR_AXIS * projectionF * t(latitude).pow(projectionN)
        val angle = projectionN * (longitude - centralMeridian)

        return ProjectedCoordinate(
            eastingMeters = FALSE_EASTING + radius * sin(angle),
            northingMeters = FALSE_NORTHING + originRadius - radius * cos(angle)
        )
    }

    private fun m(latitude: Double): Double =
        cos(latitude) / sqrt(1.0 - ECCENTRICITY.pow(2) * sin(latitude).pow(2))

    private fun t(latitude: Double): Double =
        tan(Math.PI / 4.0 - latitude / 2.0) /
            ((1.0 - ECCENTRICITY * sin(latitude)) /
                (1.0 + ECCENTRICITY * sin(latitude))).pow(ECCENTRICITY / 2.0)
}

