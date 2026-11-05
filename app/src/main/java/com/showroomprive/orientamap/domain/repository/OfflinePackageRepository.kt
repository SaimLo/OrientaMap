package com.showroomprive.orientamap.domain.repository

import com.showroomprive.orientamap.domain.model.OfflinePackage
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface OfflinePackageRepository {
    fun observeForProject(projectId: UUID): Flow<List<OfflinePackage>>
    suspend fun get(id: UUID): OfflinePackage?
    suspend fun createForProject(
        projectId: UUID,
        name: String,
        minZoom: Int,
        maxZoom: Int,
        averageTileBytes: Long? = null
    ): UUID?
    suspend fun update(offlinePackage: OfflinePackage)
    suspend fun delete(offlinePackage: OfflinePackage)
}
