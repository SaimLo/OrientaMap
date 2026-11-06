package com.showroomprive.orientamap

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.showroomprive.orientamap.data.importer.RgeAltiContourCacheStore
import com.showroomprive.orientamap.domain.model.GeoBounds
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RgeAltiContourCacheInstrumentedTest {

    @Test
    fun cachedContoursAreReadableFromAndroidAppStorageWithoutNetwork() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val directory = File(context.cacheDir, "contour-cache-test-${UUID.randomUUID()}")
        try {
            val store = RgeAltiContourCacheStore(directory)
            val projectId = UUID.randomUUID()
            val bounds = GeoBounds(48.90, 48.70, 2.50, 2.10)
            val expected = "{\"type\":\"FeatureCollection\",\"features\":[]}"

            val first = store.getOrFetch(projectId, bounds) { expected }
            val second = store.getOrFetch(projectId, bounds) {
                error("Le réseau ne doit pas être appelé pour une zone déjà mise en cache")
            }

            assertEquals(expected, first)
            assertEquals(expected, second)
            assertTrue(store.cacheFile(projectId, bounds).isFile)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun deletingProjectRemovesItsAndroidCacheDirectory() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val directory = File(context.cacheDir, "contour-cache-test-${UUID.randomUUID()}")
        try {
            val store = RgeAltiContourCacheStore(directory)
            val projectId = UUID.randomUUID()
            val bounds = GeoBounds(48.90, 48.70, 2.50, 2.10)
            store.getOrFetch(projectId, bounds) {
                "{\"type\":\"FeatureCollection\",\"features\":[]}"
            }

            store.deleteProject(projectId)

            assertFalse(store.cacheDirectory(projectId).exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}
