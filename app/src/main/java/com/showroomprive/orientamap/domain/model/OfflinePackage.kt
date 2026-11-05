package com.showroomprive.orientamap.domain.model

import java.util.UUID

data class OfflinePackage(
    val id: UUID,
    val projectId: UUID,
    val name: String,
    val bounds: GeoBounds,
    val minZoom: Int,
    val maxZoom: Int,
    val sourceId: String,
    val attribution: String,
    val status: OfflinePackageStatus,
    val downloadedTiles: Long,
    val totalTiles: Long,
    val estimatedSizeBytes: Long?,
    val localPath: String?,
    val createdAtEpochMillis: Long,
    val completedAtEpochMillis: Long?
)
