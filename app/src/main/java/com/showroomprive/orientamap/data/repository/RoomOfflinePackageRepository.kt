package com.showroomprive.orientamap.data.repository

import com.showroomprive.orientamap.data.local.MapProjectDao
import com.showroomprive.orientamap.data.local.OfflinePackageDao
import com.showroomprive.orientamap.data.local.OfflinePackageEntity
import com.showroomprive.orientamap.data.local.toEntity
import com.showroomprive.orientamap.domain.model.OfflinePackage
import com.showroomprive.orientamap.domain.model.OfflineTileEstimator
import com.showroomprive.orientamap.domain.model.ProjectStatus
import com.showroomprive.orientamap.domain.model.createOfflinePackage
import com.showroomprive.orientamap.domain.repository.OfflinePackageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomOfflinePackageRepository(
    private val projectDao: MapProjectDao,
    private val packageDao: OfflinePackageDao
) : OfflinePackageRepository {
    override fun observeForProject(projectId: UUID): Flow<List<OfflinePackage>> =
        packageDao.observeForProject(projectId.toString())
            .map { entities -> entities.map(OfflinePackageEntity::toDomain) }

    override suspend fun get(id: UUID): OfflinePackage? =
        packageDao.getById(id.toString())?.toDomain()

    override suspend fun createForProject(
        projectId: UUID,
        name: String,
        minZoom: Int,
        maxZoom: Int,
        averageTileBytes: Long?
    ): UUID? {
        val project = projectDao.getProjectById(projectId.toString())?.toDomain() ?: return null
        val bounds = project.geoBounds ?: return null
        if (project.status == ProjectStatus.BROUILLON ||
            project.status == ProjectStatus.ARCHIVE
        ) {
            return null
        }

        val estimate = OfflineTileEstimator.estimate(
            bounds = bounds,
            minZoom = minZoom,
            maxZoom = maxZoom,
            averageTileBytes = averageTileBytes
        )
        val offlinePackage = project.createOfflinePackage(
            name = name,
            minZoom = minZoom,
            maxZoom = maxZoom,
            estimate = estimate
        )
        packageDao.insert(offlinePackage.toEntity())
        return offlinePackage.id
    }

    override suspend fun update(offlinePackage: OfflinePackage) {
        packageDao.update(offlinePackage.toEntity())
    }

    override suspend fun delete(offlinePackage: OfflinePackage) {
        packageDao.delete(offlinePackage.toEntity())
    }
}
