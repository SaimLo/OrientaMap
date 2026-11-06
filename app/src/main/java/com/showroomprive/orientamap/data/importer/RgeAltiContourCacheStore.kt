package com.showroomprive.orientamap.data.importer

import android.content.Context
import com.showroomprive.orientamap.domain.model.GeoBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/**
 * Local cache of the raw IGN contour GeoJSON response.
 *
 * The cache is deliberately file-based. Contour geometry is variable-sized
 * source data, so it does not need a Room schema or migration. The project
 * id, geographic bounds and cache version are part of the deterministic key.
 */
class RgeAltiContourCacheStore(
    private val rootDirectory: File
) {

    constructor(context: Context) : this(
        File(context.applicationContext.filesDir, CACHE_DIRECTORY_NAME)
    )

    suspend fun getOrFetch(
        projectId: UUID?,
        bounds: GeoBounds,
        fetcher: suspend () -> String
    ): String {
        val file = cacheFile(projectId, bounds)
        val cached = withContext(Dispatchers.IO) {
            if (file.isFile && file.length() > 0L) {
                file.readText(Charsets.UTF_8)
            } else {
                null
            }
        }
        if (cached != null) return cached

        val fresh = fetcher()
        withContext(Dispatchers.IO) {
            rootDirectory.mkdirs()
            val temporary = File(rootDirectory, ".${file.name}.tmp")
            temporary.writeText(fresh, Charsets.UTF_8)
            if (file.exists() && !file.delete()) {
                error("Impossible de remplacer le cache IGN ${file.name}")
            }
            check(temporary.renameTo(file)) {
                "Impossible d'enregistrer le cache IGN ${file.name}"
            }
        }
        return fresh
    }

    fun cacheFile(projectId: UUID?, bounds: GeoBounds): File =
        File(rootDirectory, "${cacheKey(projectId, bounds)}.geojson")

    fun cacheKey(projectId: UUID?, bounds: GeoBounds): String {
        val canonical = listOf(
            CACHE_FORMAT_VERSION,
            projectId?.toString() ?: "no-project",
            bounds.north,
            bounds.south,
            bounds.east,
            bounds.west,
            RgeAltiWfsClient.SOURCE_KEY
        ).joinToString("|")
        return sha256(canonical)
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val CACHE_DIRECTORY_NAME = "contour-cache"
        const val CACHE_FORMAT_VERSION = "rge-contours-v1"
    }
}
