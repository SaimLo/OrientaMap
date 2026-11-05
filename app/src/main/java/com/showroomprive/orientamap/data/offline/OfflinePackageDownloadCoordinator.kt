package com.showroomprive.orientamap.data.offline

import com.showroomprive.orientamap.domain.model.OfflinePackage
import com.showroomprive.orientamap.domain.model.OfflinePackageStatus
import com.showroomprive.orientamap.domain.repository.OfflineDownloadObserver
import com.showroomprive.orientamap.domain.repository.OfflineDownloadProgress
import com.showroomprive.orientamap.domain.repository.OfflineDownloader
import com.showroomprive.orientamap.domain.repository.OfflinePackageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

class OfflinePackageDownloadCoordinator(
    private val downloader: OfflineDownloader,
    private val repository: OfflinePackageRepository,
    private val scope: CoroutineScope,
    private val nowEpochMillis: () -> Long = { System.currentTimeMillis() }
) {
    fun start(packageId: UUID) {
        scope.launch {
            val offlinePackage = repository.get(packageId) ?: return@launch
            repository.update(
                offlinePackage.copy(
                    status = OfflinePackageStatus.EN_ATTENTE,
                    lastError = null
                )
            )
            downloader.start(offlinePackage, observerFor(packageId))
        }
    }

    fun observe(packageId: UUID, regionId: Long) {
        downloader.observe(regionId, observerFor(packageId))
    }

    fun pause(packageId: UUID) {
        withRegion(packageId) { _, regionId ->
            downloader.pause(regionId) { error ->
                updateAfterCommand(
                    packageId = packageId,
                    error = error,
                    successStatus = OfflinePackageStatus.PAUSE
                )
            }
        }
    }

    fun resume(packageId: UUID) {
        withRegion(packageId) { _, regionId ->
            downloader.resume(regionId) { error ->
                updateAfterCommand(
                    packageId = packageId,
                    error = error,
                    successStatus = OfflinePackageStatus.TELECHARGEMENT_EN_COURS
                )
            }
        }
    }

    fun cancel(packageId: UUID) {
        scope.launch {
            val offlinePackage = repository.get(packageId) ?: return@launch
            val regionId = offlinePackage.mapLibreRegionId
            if (regionId == null) {
                repository.update(
                    offlinePackage.copy(
                        status = OfflinePackageStatus.ANNULE,
                        lastError = null
                    )
                )
                return@launch
            }

            downloader.delete(regionId) { error ->
                scope.launch {
                    val currentPackage = repository.get(packageId) ?: return@launch
                    repository.update(
                        if (error == null) {
                            currentPackage.copy(
                                mapLibreRegionId = null,
                                status = OfflinePackageStatus.ANNULE,
                                lastError = null
                            )
                        } else {
                            currentPackage.copy(
                                status = OfflinePackageStatus.ECHEC,
                                lastError = error
                            )
                        }
                    )
                }
            }
        }
    }

    fun delete(packageId: UUID) {
        scope.launch {
            val offlinePackage = repository.get(packageId) ?: return@launch
            val regionId = offlinePackage.mapLibreRegionId
            if (regionId == null) {
                repository.delete(offlinePackage)
                return@launch
            }
            downloader.delete(regionId) { error ->
                if (error == null) {
                    scope.launch { repository.delete(offlinePackage) }
                } else {
                    scope.launch {
                        repository.update(
                            offlinePackage.copy(
                                status = OfflinePackageStatus.ECHEC,
                                lastError = error
                            )
                        )
                    }
                }
            }
        }
    }

    private fun observerFor(packageId: UUID) = object : OfflineDownloadObserver {
        override fun onRegionCreated(regionId: Long) {
            updatePackage(packageId) { offlinePackage ->
                offlinePackage.copy(
                    mapLibreRegionId = regionId,
                    status = OfflinePackageStatus.TELECHARGEMENT_EN_COURS,
                    lastError = null
                )
            }
        }

        override fun onProgress(progress: OfflineDownloadProgress) {
            updatePackage(packageId) { offlinePackage ->
                val complete = progress.isComplete
                offlinePackage.copy(
                    status = when {
                        complete -> OfflinePackageStatus.TERMINE
                        !progress.isActive -> OfflinePackageStatus.PAUSE
                        else -> OfflinePackageStatus.TELECHARGEMENT_EN_COURS
                    },
                    downloadedTiles = progress.completedTileCount,
                    downloadedBytes = progress.completedTileSizeBytes,
                    lastError = null,
                    completedAtEpochMillis = if (complete) nowEpochMillis() else null
                )
            }
        }

        override fun onError(message: String, recoverable: Boolean) {
            updatePackage(packageId) { offlinePackage ->
                offlinePackage.copy(
                    status = if (recoverable) {
                        OfflinePackageStatus.TELECHARGEMENT_EN_COURS
                    } else {
                        OfflinePackageStatus.ECHEC
                    },
                    lastError = message
                )
            }
        }

        override fun onTileCountLimitExceeded(limit: Long) {
            updatePackage(packageId) { offlinePackage ->
                offlinePackage.copy(
                    status = OfflinePackageStatus.ECHEC,
                    lastError = "La limite de tuiles MapLibre a été atteinte : $limit"
                )
            }
        }
    }

    private fun withRegion(
        packageId: UUID,
        action: (OfflinePackage, Long) -> Unit
    ) {
        scope.launch {
            val offlinePackage = repository.get(packageId) ?: return@launch
            val regionId = offlinePackage.mapLibreRegionId ?: return@launch
            action(offlinePackage, regionId)
        }
    }

    private fun updateAfterCommand(
        packageId: UUID,
        error: String?,
        successStatus: OfflinePackageStatus
    ) {
        scope.launch {
            val offlinePackage = repository.get(packageId) ?: return@launch
            repository.update(
                offlinePackage.copy(
                    status = if (error == null) successStatus else OfflinePackageStatus.ECHEC,
                    lastError = error
                )
            )
        }
    }

    private fun updatePackage(
        packageId: UUID,
        transform: (OfflinePackage) -> OfflinePackage
    ) {
        scope.launch {
            val offlinePackage = repository.get(packageId) ?: return@launch
            repository.update(transform(offlinePackage))
        }
    }
}
