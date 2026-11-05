package com.showroomprive.orientamap.data.local

import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.OfflinePackageStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class OfflinePackageEntityTest {

    @Test
    fun entityRoundTripsThroughDomain() {
        val projectId = UUID.randomUUID()
        val packageId = UUID.randomUUID()
        val entity = OfflinePackageEntity(
            id = packageId.toString(),
            projectId = projectId.toString(),
            name = "Plan IGN",
            boundsNorth = 48.9,
            boundsSouth = 48.7,
            boundsEast = 2.5,
            boundsWest = 2.1,
            minZoom = 10,
            maxZoom = 12,
            sourceId = "GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2",
            attribution = "© IGN",
            status = OfflinePackageStatus.TERMINE.name,
            downloadedTiles = 100L,
            totalTiles = 100L,
            estimatedSizeBytes = 5_000_000L,
            localPath = "offline/$packageId",
            createdAtEpochMillis = 1L,
            completedAtEpochMillis = 2L
        )

        val domain = entity.toDomain()

        assertEquals(packageId, domain.id)
        assertEquals(projectId, domain.projectId)
        assertEquals("Plan IGN", domain.name)
        assertEquals(GeoBounds(48.9, 48.7, 2.5, 2.1), domain.bounds)
        assertEquals(OfflinePackageStatus.TERMINE, domain.status)
        assertEquals(100L, domain.downloadedTiles)
        assertEquals(2L, domain.completedAtEpochMillis)
    }
}
