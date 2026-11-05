package com.showroomprive.orientamap.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.OfflinePackage
import com.showroomprive.orientamap.domain.model.toOfflinePackageStatus
import java.util.UUID

@Entity(
    tableName = "offline_packages",
    foreignKeys = [
        ForeignKey(
            entity = MapProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class OfflinePackageEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val mapLibreRegionId: Long? = null,
    val name: String,
    val boundsNorth: Double,
    val boundsSouth: Double,
    val boundsEast: Double,
    val boundsWest: Double,
    val minZoom: Int,
    val maxZoom: Int,
    val sourceId: String,
    val attribution: String,
    val status: String,
    val downloadedTiles: Long,
    val totalTiles: Long,
    val downloadedBytes: Long = 0L,
    val estimatedSizeBytes: Long?,
    val localPath: String?,
    val lastError: String? = null,
    val createdAtEpochMillis: Long,
    val completedAtEpochMillis: Long?
) {
    fun toDomain(): OfflinePackage = OfflinePackage(
        id = UUID.fromString(id),
        projectId = UUID.fromString(projectId),
        mapLibreRegionId = mapLibreRegionId,
        name = name,
        bounds = GeoBounds(
            north = boundsNorth,
            south = boundsSouth,
            east = boundsEast,
            west = boundsWest
        ),
        minZoom = minZoom,
        maxZoom = maxZoom,
        sourceId = sourceId,
        attribution = attribution,
        status = status.toOfflinePackageStatus(),
        downloadedTiles = downloadedTiles,
        totalTiles = totalTiles,
        downloadedBytes = downloadedBytes,
        estimatedSizeBytes = estimatedSizeBytes,
        localPath = localPath,
        lastError = lastError,
        createdAtEpochMillis = createdAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis
    )
}

fun OfflinePackage.toEntity(): OfflinePackageEntity = OfflinePackageEntity(
    id = id.toString(),
    projectId = projectId.toString(),
    mapLibreRegionId = mapLibreRegionId,
    name = name,
    boundsNorth = bounds.north,
    boundsSouth = bounds.south,
    boundsEast = bounds.east,
    boundsWest = bounds.west,
    minZoom = minZoom,
    maxZoom = maxZoom,
    sourceId = sourceId,
    attribution = attribution,
    status = status.name,
    downloadedTiles = downloadedTiles,
    totalTiles = totalTiles,
    downloadedBytes = downloadedBytes,
    estimatedSizeBytes = estimatedSizeBytes,
    localPath = localPath,
    lastError = lastError,
    createdAtEpochMillis = createdAtEpochMillis,
    completedAtEpochMillis = completedAtEpochMillis
)
