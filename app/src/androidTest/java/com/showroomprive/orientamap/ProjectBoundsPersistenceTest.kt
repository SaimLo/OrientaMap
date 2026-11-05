package com.showroomprive.orientamap

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.showroomprive.orientamap.data.local.OrientaMapDatabase
import com.showroomprive.orientamap.data.repository.RoomMapProjectRepository
import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.ProjectStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProjectBoundsPersistenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var database: OrientaMapDatabase? = null

    @Before
    fun deletePreviousTestDatabase() {
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    @After
    fun closeAndDeleteTestDatabase() {
        database?.close()
        database = null
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    @Test
    fun definedAreaSurvivesDatabaseReopen() = runBlocking {
        database = openDatabase()
        val repository = RoomMapProjectRepository(database!!.mapProjectDao())
        val projectId = repository.createProject(
            name = "Projet persistant",
            description = null
        )
        val bounds = GeoBounds(
            north = 48.9,
            south = 48.7,
            east = 2.5,
            west = 2.1
        )

        assertTrue(repository.defineProjectArea(projectId, bounds))
        database!!.close()
        database = null

        database = openDatabase()
        val reloadedProject = RoomMapProjectRepository(database!!.mapProjectDao())
            .getProject(projectId)

        assertNotNull(reloadedProject)
        assertEquals(bounds, reloadedProject!!.geoBounds)
        assertEquals(ProjectStatus.ZONE_DEFINIE, reloadedProject.status)
    }

    @Test
    fun definingAreaTwiceIsRejectedByTheLifecycleRule() = runBlocking {
        database = openDatabase()
        val repository = RoomMapProjectRepository(database!!.mapProjectDao())
        val projectId = repository.createProject("Projet verrouillé", null)
        val firstBounds = GeoBounds(48.9, 48.7, 2.5, 2.1)
        val secondBounds = GeoBounds(49.0, 48.8, 2.7, 2.3)

        assertTrue(repository.defineProjectArea(projectId, firstBounds))
        assertTrue(!repository.defineProjectArea(projectId, secondBounds))

        val storedProject = repository.getProject(projectId)
        assertEquals(firstBounds, storedProject?.geoBounds)
        assertEquals(ProjectStatus.ZONE_DEFINIE, storedProject?.status)
    }

    private fun openDatabase(): OrientaMapDatabase =
        Room.databaseBuilder(
            context,
            OrientaMapDatabase::class.java,
            TEST_DATABASE_NAME
        ).build()

    private companion object {
        const val TEST_DATABASE_NAME = "project-bounds-persistence-test.db"
    }
}
