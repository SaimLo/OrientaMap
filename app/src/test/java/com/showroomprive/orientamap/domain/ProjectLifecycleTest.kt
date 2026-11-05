package com.showroomprive.orientamap.domain

import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.MapProject
import com.showroomprive.orientamap.domain.model.ProjectStatus
import com.showroomprive.orientamap.domain.model.archive
import com.showroomprive.orientamap.domain.model.defineArea
import com.showroomprive.orientamap.domain.model.displayLabel
import com.showroomprive.orientamap.domain.model.startEditing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.UUID

class ProjectLifecycleTest {

    private fun draftProject() = MapProject(
        id = UUID.randomUUID(),
        name = "Projet test",
        description = null,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L
    )

    private val bounds = GeoBounds(
        north = 48.9,
        south = 48.7,
        east = 2.5,
        west = 2.1
    )

    @Test
    fun defineAreaMovesDraftToDefinedAndLocksBounds() {
        val defined = draftProject().defineArea(bounds, updatedAtEpochMillis = 2L)

        assertEquals(ProjectStatus.ZONE_DEFINIE, defined.status)
        assertEquals(bounds, defined.geoBounds)
        assertEquals(2L, defined.updatedAtEpochMillis)
    }

    @Test
    fun definingAreaTwiceIsRejected() {
        val defined = draftProject().defineArea(bounds)

        assertThrows(IllegalStateException::class.java) {
            defined.defineArea(bounds)
        }
    }

    @Test
    fun definedProjectCanEnterEditing() {
        val editing = draftProject()
            .defineArea(bounds)
            .startEditing(updatedAtEpochMillis = 3L)

        assertEquals(ProjectStatus.EDITION_EN_COURS, editing.status)
        assertEquals(3L, editing.updatedAtEpochMillis)
    }

    @Test
    fun projectCanBeArchived() {
        val archived = draftProject().archive(updatedAtEpochMillis = 4L)

        assertEquals(ProjectStatus.ARCHIVE, archived.status)
        assertEquals("Archivé", archived.status.displayLabel())
    }
}
