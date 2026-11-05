package com.showroomprive.orientamap.domain.repository

import com.showroomprive.orientamap.domain.model.OfflinePackage

/** Production-facing boundary for offline map downloads. */
interface OfflineDownloadObserver {
    fun onRegionCreated(regionId: Long)
    fun onProgress(progress: OfflineDownloadProgress)
    fun onError(message: String, recoverable: Boolean)
    fun onTileCountLimitExceeded(limit: Long)
}

data class OfflineDownloadProgress(
    val regionId: Long,
    val downloadState: Int,
    val completedTileCount: Long,
    val requiredResourceCount: Long,
    val completedTileSizeBytes: Long,
    val completedResourceSizeBytes: Long,
    val isComplete: Boolean,
    val requiredResourceCountIsPrecise: Boolean
)

interface OfflineDownloader {
    fun start(offlinePackage: OfflinePackage, observer: OfflineDownloadObserver)
    fun observe(regionId: Long, observer: OfflineDownloadObserver)
    fun pause(regionId: Long, onResult: (String?) -> Unit)
    fun resume(regionId: Long, onResult: (String?) -> Unit)
    fun delete(regionId: Long, onResult: (String?) -> Unit)
}
