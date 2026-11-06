package com.showroomprive.orientamap

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.showroomprive.orientamap.data.importer.RgeAltiContourCacheStore
import com.showroomprive.orientamap.data.local.OrientaMapDatabase
import com.showroomprive.orientamap.data.repository.RoomMapProjectRepository
import com.showroomprive.orientamap.domain.model.GeoBounds
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectDataCleanupTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var database: OrientaMapDatabase? = null

    @After
    fun closeDatabase() {
        database?.close()
        database = null
    }

    @Test
    fun deletingProjectRemovesItsContourCache() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            context,
            OrientaMapDatabase::class.java
        ).allowMainThreadQueries().build()
        val cacheDirectory = File(context.cacheDir, "project-cleanup-${UUID.randomUUID()}")
        val cacheStore = RgeAltiContourCacheStore(cacheDirectory)
        val repository = RoomMapProjectRepository(
            dao = database!!.mapProjectDao(),
            projectDataCleanup = cacheStore
        )
        val projectId = repository.createProject("Projet à supprimer", null)
        val bounds = GeoBounds(48.90, 48.70, 2.50, 2.10)
        cacheStore.getOrFetch(projectId, bounds) {
            "{\"type\":\"FeatureCollection\",\"features\":[]}"
        }

        assertTrue(cacheStore.cacheDirectory(projectId).exists())
        repository.deleteProject(repository.getProject(projectId)!!)

        assertNull(repository.getProject(projectId))
        assertFalse(cacheStore.cacheDirectory(projectId).exists())
        cacheDirectory.deleteRecursively()
        Unit
    }
}
