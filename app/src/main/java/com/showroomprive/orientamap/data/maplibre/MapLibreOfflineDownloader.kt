package com.showroomprive.orientamap.data.maplibre

import android.content.Context
import com.showroomprive.orientamap.domain.model.OfflinePackage
import com.showroomprive.orientamap.domain.repository.OfflineDownloadObserver
import com.showroomprive.orientamap.domain.repository.OfflineDownloadProgress
import com.showroomprive.orientamap.domain.repository.OfflineDownloader
import org.json.JSONObject
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition

class MapLibreOfflineDownloader(
    context: Context,
    private val styleUri: String = "asset://styles/ign_plan.json",
    private val pixelRatio: Float = 1f
) : OfflineDownloader {
    private val offlineManager = OfflineManager.getInstance(context.applicationContext)

    override fun start(
        offlinePackage: OfflinePackage,
        observer: OfflineDownloadObserver
    ) {
        val bounds = LatLngBounds.from(
            offlinePackage.bounds.north,
            offlinePackage.bounds.east,
            offlinePackage.bounds.south,
            offlinePackage.bounds.west
        )
        val definition = OfflineTilePyramidRegionDefinition(
            styleUri,
            bounds,
            offlinePackage.minZoom.toDouble(),
            offlinePackage.maxZoom.toDouble(),
            pixelRatio
        )
        val metadata = JSONObject()
            .put("offlinePackageId", offlinePackage.id.toString())
            .put("projectId", offlinePackage.projectId.toString())
            .put("name", offlinePackage.name)
            .toString()
            .toByteArray(Charsets.UTF_8)

        offlineManager.createOfflineRegion(
            definition,
            metadata,
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(region: OfflineRegion) {
                    observer.onRegionCreated(region.id)
                    region.setObserver(createObserver(region.id, observer))
                    region.setDownloadState(OfflineRegion.STATE_ACTIVE)
                }

                override fun onError(error: String) {
                    observer.onError(error, recoverable = false)
                }
            }
        )
    }

    override fun observe(regionId: Long, observer: OfflineDownloadObserver) {
        findRegion(regionId) { region, error ->
            if (region == null) {
                observer.onError(error ?: "Région MapLibre introuvable", recoverable = false)
                return@findRegion
            }
            region.setObserver(createObserver(region.id, observer))
            region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                override fun onStatus(status: OfflineRegionStatus?) {
                    if (status != null) {
                        observer.onProgress(status.toProgress(region.id))
                    }
                }

                override fun onError(error: String?) {
                    observer.onError(
                        error ?: "Impossible de lire le statut de la région",
                        recoverable = false
                    )
                }
            })
        }
    }

    override fun pause(regionId: Long, onResult: (String?) -> Unit) {
        setDownloadState(regionId, OfflineRegion.STATE_INACTIVE, onResult)
    }

    override fun resume(regionId: Long, onResult: (String?) -> Unit) {
        setDownloadState(regionId, OfflineRegion.STATE_ACTIVE, onResult)
    }

    override fun delete(regionId: Long, onResult: (String?) -> Unit) {
        findRegion(regionId) { region, error ->
            if (region == null) {
                onResult(error ?: "Région MapLibre introuvable")
                return@findRegion
            }
            region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                override fun onDelete() {
                    onResult(null)
                }

                override fun onError(error: String) {
                    onResult(error)
                }
            })
        }
    }

    private fun setDownloadState(
        regionId: Long,
        state: Int,
        onResult: (String?) -> Unit
    ) {
        findRegion(regionId) { region, error ->
            if (region == null) {
                onResult(error ?: "Région MapLibre introuvable")
                return@findRegion
            }
            region.setDownloadState(state)
            onResult(null)
        }
    }

    private fun findRegion(
        regionId: Long,
        callback: (OfflineRegion?, String?) -> Unit
    ) {
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                callback(offlineRegions?.firstOrNull { it.id == regionId }, null)
            }

            override fun onError(error: String) {
                callback(null, error)
            }
        })
    }

    private fun createObserver(
        regionId: Long,
        observer: OfflineDownloadObserver
    ): OfflineRegion.OfflineRegionObserver =
        object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                observer.onProgress(status.toProgress(regionId))
                if (status.isComplete) {
                    status.downloadState.takeIf { it == OfflineRegion.STATE_ACTIVE }?.let {
                        setDownloadState(regionId, OfflineRegion.STATE_INACTIVE) { error ->
                            if (error != null) observer.onError(error, recoverable = false)
                        }
                    }
                }
            }

            override fun onError(error: OfflineRegionError) {
                observer.onError(error.message, recoverable = true)
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                observer.onTileCountLimitExceeded(limit)
            }
        }

    private fun OfflineRegionStatus.toProgress(regionId: Long) = OfflineDownloadProgress(
        regionId = regionId,
        downloadState = downloadState,
        completedTileCount = completedTileCount,
        requiredResourceCount = requiredResourceCount,
        completedTileSizeBytes = completedTileSize,
        completedResourceSizeBytes = completedResourceSize,
        isComplete = isComplete,
        requiredResourceCountIsPrecise = isRequiredResourceCountPrecise
    )
}
