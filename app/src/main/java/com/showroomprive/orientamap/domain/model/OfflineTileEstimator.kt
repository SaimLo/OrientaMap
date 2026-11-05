package com.showroomprive.orientamap.domain.model

import java.util.UUID
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/** Estimated tile counts for a Web Mercator XYZ tile source. */
data class TileEstimate(
    val totalTiles: Long,
    val tilesByZoom: Map<Int, Long>,
    val estimatedSizeBytes: Long?
)

object OfflineTileEstimator {
    private const val MAX_ZOOM = 30
    private const val MAX_MERCATOR_LATITUDE = 85.05112878

    fun estimate(
        bounds: GeoBounds,
        minZoom: Int,
        maxZoom: Int,
        averageTileBytes: Long? = null
    ): TileEstimate {
        require(minZoom >= 0) { "minZoom doit être positif ou nul" }
        require(maxZoom in minZoom..MAX_ZOOM) {
            "maxZoom doit être compris entre minZoom et $MAX_ZOOM"
        }
        require(averageTileBytes == null || averageTileBytes >= 0) {
            "averageTileBytes doit être positif ou nul"
        }

        val tilesByZoom = (minZoom..maxZoom).associateWith { zoom ->
            tileCountAtZoom(bounds, zoom)
        }
        val totalTiles = tilesByZoom.values.sumOf { it }
        val estimatedSizeBytes = averageTileBytes?.let { bytesPerTile ->
            if (bytesPerTile == 0L || totalTiles <= Long.MAX_VALUE / bytesPerTile) {
                totalTiles * bytesPerTile
            } else {
                Long.MAX_VALUE
            }
        }

        return TileEstimate(
            totalTiles = totalTiles,
            tilesByZoom = tilesByZoom,
            estimatedSizeBytes = estimatedSizeBytes
        )
    }

    private fun tileCountAtZoom(bounds: GeoBounds, zoom: Int): Long {
        val tileCount = 1L shl zoom
        val westX = longitudeToTileX(bounds.west, tileCount)
        val eastX = longitudeToTileX(bounds.east, tileCount)
        val northY = latitudeToTileY(bounds.north, tileCount)
        val southY = latitudeToTileY(bounds.south, tileCount)

        val xCount = if (bounds.east >= bounds.west) {
            eastX - westX + 1L
        } else {
            tileCount - westX + eastX + 1L
        }
        val yCount = southY - northY + 1L
        return xCount * yCount
    }

    private fun longitudeToTileX(longitude: Double, tileCount: Long): Long =
        floor(((longitude.coerceIn(-180.0, 180.0) + 180.0) / 360.0) * tileCount)
            .toLong()
            .coerceIn(0L, tileCount - 1L)

    private fun latitudeToTileY(latitude: Double, tileCount: Long): Long {
        val clampedLatitude = latitude.coerceIn(-MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE)
        val latitudeRadians = Math.toRadians(clampedLatitude)
        val projected = (1.0 - ln(tan(latitudeRadians) + 1.0 / cos(latitudeRadians)) / Math.PI) / 2.0
        return floor(projected * tileCount)
            .toLong()
            .coerceIn(0L, tileCount - 1L)
    }
}

fun MapProject.createOfflinePackage(
    name: String,
    minZoom: Int,
    maxZoom: Int,
    estimate: TileEstimate,
    sourceId: String = "GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2",
    attribution: String = "© IGN",
    nowEpochMillis: Long = System.currentTimeMillis()
): OfflinePackage {
    val projectBounds = geoBounds ?: error("Le projet doit avoir une zone définie")
    check(status != ProjectStatus.BROUILLON) {
        "Le téléchargement nécessite une zone définie"
    }
    check(status != ProjectStatus.ARCHIVE) {
        "Un projet archivé ne peut pas préparer un téléchargement"
    }
    require(name.isNotBlank()) { "Le nom du paquet ne peut pas être vide" }

    return OfflinePackage(
        id = UUID.randomUUID(),
        projectId = id,
        name = name.trim(),
        bounds = projectBounds,
        minZoom = minZoom,
        maxZoom = maxZoom,
        sourceId = sourceId,
        attribution = attribution,
        status = OfflinePackageStatus.CREE,
        downloadedTiles = 0L,
        totalTiles = estimate.totalTiles,
        estimatedSizeBytes = estimate.estimatedSizeBytes,
        localPath = null,
        createdAtEpochMillis = nowEpochMillis,
        completedAtEpochMillis = null
    )
}
