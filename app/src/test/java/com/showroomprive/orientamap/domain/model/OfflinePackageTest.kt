package com.showroomprive.orientamap.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.UUID

class OfflinePackageTest {

    private val bounds = GeoBounds(
        north = 48.9,
        south = 48.7,
        east = 2.5,
        west = 2.1
    )

    private val estimate = OfflineTileEstimator.estimate(
        bounds = bounds,
        minZoom = 10,
        maxZoom = 12
    )

    @Test
    fun definedProjectCreatesPackageInCreatedState() {
        val project = MapProject(
            id = UUID.randomUUID(),
            name = "Projet test",
            description = null,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
            geoBounds = bounds,
            status = ProjectStatus.ZONE_DEFINIE
        )

        val offlinePackage = project.createOfflinePackage(
            name = "Plan IGN",
            minZoom = 10,
            maxZoom = 12,
            estimate = estimate,
            nowEpochMillis = 2L
        )

        assertEquals(OfflinePackageStatus.CREE, offlinePackage.status)
        assertEquals(estimate.totalTiles, offlinePackage.totalTiles)
        assertEquals(2L, offlinePackage.createdAtEpochMillis)
        assertEquals(project.id, offlinePackage.projectId)
    }

    @Test
    fun draftProjectCannotCreatePackage() {
        val project = MapProject(
            id = UUID.randomUUID(),
            name = "Projet brouillon",
            description = null,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L
        )

        assertThrows(IllegalStateException::class.java) {
            project.createOfflinePackage(
                name = "Plan IGN",
                minZoom = 10,
                maxZoom = 12,
                estimate = estimate
            )
        }
    }
}
