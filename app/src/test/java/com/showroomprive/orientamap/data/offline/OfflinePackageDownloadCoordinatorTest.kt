package com.showroomprive.orientamap.data.offline

import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.OfflinePackage
import com.showroomprive.orientamap.domain.model.OfflinePackageStatus
import com.showroomprive.orientamap.domain.repository.OfflineDownloadObserver
import com.showroomprive.orientamap.domain.repository.OfflineDownloadProgress
import com.showroomprive.orientamap.domain.repository.OfflineDownloader
import com.showroomprive.orientamap.domain.repository.OfflinePackageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class OfflinePackageDownloadCoordinatorTest {

    @Test
    fun startAndProgressUpdatePackageMetadata() {
        val packageId = UUID.randomUUID()
        val repository = FakeOfflinePackageRepository(packageFixture(packageId))
        val downloader = FakeOfflineDownloader()
        val coordinator = OfflinePackageDownloadCoordinator(
            downloader = downloader,
            repository = repository,
            scope = CoroutineScope(Dispatchers.Unconfined),
            nowEpochMillis = { 99L }
        )

        coordinator.start(packageId)
        assertEquals(OfflinePackageStatus.EN_ATTENTE, repository.value.status)

        downloader.observer!!.onRegionCreated(42L)
        assertEquals(42L, repository.value.mapLibreRegionId)
        assertEquals(
            OfflinePackageStatus.TELECHARGEMENT_EN_COURS,
            repository.value.status
        )

        downloader.observer!!.onProgress(
            OfflineDownloadProgress(
                regionId = 42L,
                isActive = false,
                completedTileCount = 12L,
                requiredResourceCount = 14L,
                completedTileSizeBytes = 1_200L,
                completedResourceSizeBytes = 1_500L,
                isComplete = true,
                requiredResourceCountIsPrecise = true
            )
        )

        assertEquals(OfflinePackageStatus.TERMINE, repository.value.status)
        assertEquals(12L, repository.value.downloadedTiles)
        assertEquals(1_500L, repository.value.downloadedBytes)
        assertEquals(99L, repository.value.completedAtEpochMillis)
    }

    @Test
    fun cancelBeforeRegionCreationMarksPackageCancelled() {
        val packageId = UUID.randomUUID()
        val repository = FakeOfflinePackageRepository(packageFixture(packageId))
        val coordinator = OfflinePackageDownloadCoordinator(
            downloader = FakeOfflineDownloader(),
            repository = repository,
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        coordinator.cancel(packageId)

        assertEquals(OfflinePackageStatus.ANNULE, repository.value.status)
    }

    @Test
    fun pauseAndResumeUpdatePackageStatus() {
        val packageId = UUID.randomUUID()
        val repository = FakeOfflinePackageRepository(
            packageFixture(packageId).copy(
                mapLibreRegionId = 7L,
                status = OfflinePackageStatus.TELECHARGEMENT_EN_COURS
            )
        )
        val downloader = FakeOfflineDownloader()
        val coordinator = OfflinePackageDownloadCoordinator(
            downloader = downloader,
            repository = repository,
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        coordinator.pause(packageId)
        assertEquals(OfflinePackageStatus.PAUSE, repository.value.status)

        coordinator.resume(packageId)
        assertEquals(
            OfflinePackageStatus.TELECHARGEMENT_EN_COURS,
            repository.value.status
        )
    }

    private fun packageFixture(id: UUID) = OfflinePackage(
        id = id,
        projectId = UUID.randomUUID(),
        mapLibreRegionId = null,
        name = "Plan IGN",
        bounds = GeoBounds(48.9, 48.7, 2.5, 2.1),
        minZoom = 10,
        maxZoom = 12,
        sourceId = "GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2",
        attribution = "© IGN",
        status = OfflinePackageStatus.CREE,
        downloadedTiles = 0L,
        totalTiles = 100L,
        downloadedBytes = 0L,
        estimatedSizeBytes = 5_000L,
        localPath = null,
        lastError = null,
        createdAtEpochMillis = 1L,
        completedAtEpochMillis = null
    )

    private class FakeOfflineDownloader : OfflineDownloader {
        var observer: OfflineDownloadObserver? = null

        override fun start(
            offlinePackage: OfflinePackage,
            observer: OfflineDownloadObserver
        ) {
            this.observer = observer
        }

        override fun observe(regionId: Long, observer: OfflineDownloadObserver) {
            this.observer = observer
        }

        override fun pause(regionId: Long, onResult: (String?) -> Unit) {
            onResult(null)
        }

        override fun resume(regionId: Long, onResult: (String?) -> Unit) {
            onResult(null)
        }

        override fun delete(regionId: Long, onResult: (String?) -> Unit) {
            onResult(null)
        }
    }

    private class FakeOfflinePackageRepository(
        initial: OfflinePackage
    ) : OfflinePackageRepository {
        var value: OfflinePackage = initial

        override fun observeForProject(projectId: UUID): Flow<List<OfflinePackage>> =
            flowOf(listOf(value))

        override suspend fun get(id: UUID): OfflinePackage? =
            value.takeIf { it.id == id }

        override suspend fun createForProject(
            projectId: UUID,
            name: String,
            minZoom: Int,
            maxZoom: Int,
            averageTileBytes: Long?
        ): UUID? = value.id

        override suspend fun update(offlinePackage: OfflinePackage) {
            value = offlinePackage
        }

        override suspend fun delete(offlinePackage: OfflinePackage) {
            value = offlinePackage
        }
    }
}
