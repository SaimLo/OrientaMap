package com.showroomprive.orientamap.domain.model

/**
 * Geographic bounding box in WGS84 degrees.
 */
data class GeoBounds(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double
) {
    init {
        require(north in -90.0..90.0) { "north must be between -90 and 90" }
        require(south in -90.0..90.0) { "south must be between -90 and 90" }
        require(east in -180.0..180.0) { "east must be between -180 and 180" }
        require(west in -180.0..180.0) { "west must be between -180 and 180" }
        require(north >= south) { "north must be greater than or equal to south" }
    }
}
