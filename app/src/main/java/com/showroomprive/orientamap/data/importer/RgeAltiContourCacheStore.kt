package com.showroomprive.orientamap.data.importer

import android.content.Context
import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.repository.ProjectDataCleanup
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
) : ProjectDataCleanup {

    constructor(context: Context) : this(
        File(context.applicationContext.filesDir, CACHE_DIRECTORY_NAME)
    )

    suspend fun getOrFetch(
        projectId: UUID?,
        bounds: GeoBounds,
        validate: (String) -> Unit = {},
        fetcher: suspend () -> String
    ): String {
        val file = cacheFile(projectId, bounds)
        val cached = withContext(Dispatchers.IO) {
            if (!file.isFile || file.length() == 0L) {
                null
            } else {
                runCatching {
                    file.readText(Charsets.UTF_8).also(validate)
                }.getOrNull()
            }
        }
        if (cached != null) return cached

        val fresh = fetcher()
        validate(fresh)
        withContext(Dispatchers.IO) {
            atomicWrite(file, fresh)
        }
        return fresh
    }

    override suspend fun deleteProjectData(projectId: UUID) {
        deleteProject(projectId)
    }

    suspend fun deleteProject(projectId: UUID) = withContext(Dispatchers.IO) {
        val directory = cacheDirectory(projectId)
        if (directory.exists() && !directory.deleteRecursively()) {
            error("Impossible de supprimer le cache IGN du projet $projectId")
        }
    }

    fun cacheDirectory(projectId: UUID?): File =
        File(rootDirectory, projectId?.toString() ?: NO_PROJECT_DIRECTORY)

    fun cacheFile(projectId: UUID?, bounds: GeoBounds): File =
        File(cacheDirectory(projectId), "${cacheKey(projectId, bounds)}.geojson")

    fun cacheKey(projectId: UUID?, bounds: GeoBounds): String {
        val canonical = listOf(
            CACHE_FORMAT_VERSION,
            projectId?.toString() ?: NO_PROJECT_DIRECTORY,
            bounds.north,
            bounds.south,
            bounds.east,
            bounds.west,
            RgeAltiWfsClient.SOURCE_KEY
        ).joinToString("|")
        return sha256(canonical)
    }

    private fun atomicWrite(file: File, content: String) {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, ".${file.name}.tmp")
        temporary.writeText(content, Charsets.UTF_8)
        if (file.exists() && !file.delete()) {
            error("Impossible de remplacer le cache IGN ${file.name}")
        }
        check(temporary.renameTo(file)) {
            "Impossible d'enregistrer le cache IGN ${file.name}"
        }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val CACHE_DIRECTORY_NAME = "contour-cache"
        const val CACHE_FORMAT_VERSION = "rge-contours-v2"
        const val NO_PROJECT_DIRECTORY = "no-project"
    }
}
