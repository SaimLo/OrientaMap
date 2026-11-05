package com.showroomprive.orientamap.data.maplibre

import android.content.Context
import android.util.Log
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
    private val styleUri: String = "https://raw.githubusercontent.com/SaimLo/OrientaMap/bf5bad3afa76ff64373ac4d8eea162341b6232d6/app/src/main/assets/styles/ign_plan.json",
    private val pixelRatio: Float = 1f
) : OfflineDownloader {
    private val offlineManager = OfflineManager.getInstance(context.applicationContext)

    private companion object {
        const val TAG = "OrientaMapOffline"
    }

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

        Log.d(
            TAG,
            "create package=${offlinePackage.id} bounds=${offlinePackage.bounds} " +
                "zoom=${offlinePackage.minZoom}-${offlinePackage.maxZoom} style=$styleUri"
        )
        offlineManager.createOfflineRegion(
            definition,
            metadata,
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(region: OfflineRegion) {
                    Log.d(TAG, "region created id=${region.id}")
                    observer.onRegionCreated(region.id)
                    region.setObserver(createObserver(region.id, observer))
                    region.setDownloadState(OfflineRegion.STATE_ACTIVE)
                    requestCurrentStatus(region, observer)
                }

                override fun onError(error: String) {
                    Log.e(TAG, "create error: $error")
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
            requestCurrentStatus(region, observer)
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
            Log.d(TAG, "set state=$state region=$regionId")
            region.setDownloadState(state)
            region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                override fun onStatus(status: OfflineRegionStatus?) {
                    if (status == null) {
                        onResult("Impossible de confirmer le nouvel état de la région")
                    } else if (status.downloadState == state) {
                        Log.d(TAG, "state confirmed=$state region=$regionId")
                        onResult(null)
                    } else {
                        Log.w(
                            TAG,
                            "state not confirmed expected=$state actual=${status.downloadState} " +
                                "region=$regionId"
                        )
                        onResult("MapLibre n'a pas confirmé le changement d'état")
                    }
                }

                override fun onError(error: String?) {
                    onResult(error ?: "Impossible de confirmer le nouvel état de la région")
                }
            })
        }
    }

    private fun findRegion(
        regionId: Long,
        callback: (OfflineRegion?, String?) -> Unit
    ) {
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                Log.d(TAG, "regions listed count=${offlineRegions?.size ?: 0}, requested=$regionId")
                callback(offlineRegions?.firstOrNull { it.id == regionId }, null)
            }

            override fun onError(error: String) {
                Log.e(TAG, "list regions error: $error")
                callback(null, error)
            }
        })
    }

    private fun requestCurrentStatus(
        region: OfflineRegion,
        observer: OfflineDownloadObserver
    ) {
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

    private fun createObserver(
        regionId: Long,
        observer: OfflineDownloadObserver
    ): OfflineRegion.OfflineRegionObserver =
        object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                Log.d(
                    TAG,
                    "status region=$regionId active=${status.downloadState == OfflineRegion.STATE_ACTIVE} " +
                        "tiles=${status.completedTileCount} resources=${status.completedResourceCount}/" +
                        "${status.requiredResourceCount} complete=${status.isComplete}"
                )
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
                Log.e(TAG, "region=$regionId error=${error.message}")
                observer.onError(error.message, recoverable = true)
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                Log.e(TAG, "region=$regionId tile limit=$limit")
                observer.onTileCountLimitExceeded(limit)
            }
        }

    private fun OfflineRegionStatus.toProgress(regionId: Long) = OfflineDownloadProgress(
        regionId = regionId,
        isActive = downloadState == OfflineRegion.STATE_ACTIVE,
        completedTileCount = completedTileCount,
        completedResourceCount = completedResourceCount,
        requiredResourceCount = requiredResourceCount,
        completedTileSizeBytes = completedTileSize,
        completedResourceSizeBytes = completedResourceSize,
        isComplete = isComplete,
        requiredResourceCountIsPrecise = isRequiredResourceCountPrecise
    )
}
