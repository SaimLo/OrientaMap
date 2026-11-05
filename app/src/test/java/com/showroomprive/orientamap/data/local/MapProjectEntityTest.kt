package com.showroomprive.orientamap.data.local

import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.ProjectStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class MapProjectEntityTest {

    @Test
    fun toDomainMapsCompleteGeoBounds() {
        val project = MapProjectEntity(
            id = UUID.randomUUID().toString(),
            name = "Projet test",
            description = null,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
            status = ProjectStatus.ZONE_DEFINIE.name,
            boundsNorth = 48.9,
            boundsSouth = 48.7,
            boundsEast = 2.5,
            boundsWest = 2.1
        ).toDomain()

        assertEquals(
            GeoBounds(north = 48.9, south = 48.7, east = 2.5, west = 2.1),
            project.geoBounds
        )
        assertEquals(ProjectStatus.ZONE_DEFINIE, project.status)
    }

    @Test
    fun toDomainLeavesBoundsNullWhenStoredBoundsAreIncomplete() {
        val project = MapProjectEntity(
            id = UUID.randomUUID().toString(),
            name = "Projet test",
            description = null,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
            boundsNorth = 48.9
        ).toDomain()

        assertNull(project.geoBounds)
    }
}
