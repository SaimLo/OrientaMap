package com.showroomprive.orientamap.data.importer

import com.showroomprive.orientamap.domain.model.GeoBounds
import java.nio.file.Files
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RgeAltiContourCacheStoreTest {

    @Test
    fun fetchesOnceThenReadsTheSameProjectAndBoundsFromDisk() = runBlocking {
        val directory = Files.createTempDirectory("orientamap-contours").toFile()
        try {
            val store = RgeAltiContourCacheStore(directory)
            val projectId = UUID.randomUUID()
            val bounds = GeoBounds(48.90, 48.80, 2.40, 2.30)
            var fetchCount = 0

            val first = store.getOrFetch(projectId, bounds) {
                fetchCount += 1
                "{\"type\":\"FeatureCollection\",\"features\":[]}"
            }
            val second = store.getOrFetch(projectId, bounds) {
                fetchCount += 1
                error("Le réseau ne devrait plus être appelé")
            }

            assertEquals(first, second)
            assertEquals(1, fetchCount)
            assertTrue(store.cacheFile(projectId, bounds).isFile)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun changesTheCacheKeyWhenTheProjectOrBoundsChange() {
        val directory = Files.createTempDirectory("orientamap-contours").toFile()
        try {
            val store = RgeAltiContourCacheStore(directory)
            val bounds = GeoBounds(48.90, 48.80, 2.40, 2.30)
            val otherBounds = bounds.copy(east = 2.41)
            val firstProject = UUID.randomUUID()
            val secondProject = UUID.randomUUID()

            assertFalse(store.cacheKey(firstProject, bounds) == store.cacheKey(secondProject, bounds))
            assertFalse(store.cacheKey(firstProject, bounds) == store.cacheKey(firstProject, otherBounds))
        } finally {
            directory.deleteRecursively()
        }
    }
}
