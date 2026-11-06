package com.showroomprive.orientamap.data.repository

import com.showroomprive.orientamap.data.local.MapProjectDao
import com.showroomprive.orientamap.data.local.MapProjectEntity
import com.showroomprive.orientamap.data.local.observeProjects
import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.MapProject
import com.showroomprive.orientamap.domain.model.ProjectStatus
import com.showroomprive.orientamap.domain.model.canDefineArea
import com.showroomprive.orientamap.domain.repository.MapProjectRepository
import com.showroomprive.orientamap.domain.repository.ProjectDataCleanup
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class RoomMapProjectRepository(
    private val dao: MapProjectDao,
    private val projectDataCleanup: ProjectDataCleanup? = null
) : MapProjectRepository {
    override fun observeProjects(): Flow<List<MapProject>> = dao.observeProjects()

    override suspend fun getProject(id: UUID): MapProject? =
        dao.getProjectById(id.toString())?.toDomain()

    override suspend fun createProject(name: String, description: String?): UUID {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID()
        dao.upsert(
            MapProjectEntity(
                id = id.toString(),
                name = name,
                description = description,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now
            )
        )
        return id
    }

    override suspend fun renameProject(id: UUID, newName: String) {
        dao.updateProjectName(
            id = id.toString(),
            newName = newName,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun defineProjectArea(id: UUID, bounds: GeoBounds): Boolean {
        val currentProject = dao.getProjectById(id.toString())?.toDomain() ?: return false
        if (!currentProject.canDefineArea()) return false

        return dao.defineProjectArea(
            id = id.toString(),
            north = bounds.north,
            south = bounds.south,
            east = bounds.east,
            west = bounds.west,
            draftStatus = ProjectStatus.BROUILLON.name,
            definedStatus = ProjectStatus.ZONE_DEFINIE.name,
            updatedAt = System.currentTimeMillis()
        ) == 1
    }

    override suspend fun deleteProject(project: MapProject) {
        dao.delete(
            MapProjectEntity(
                id = project.id.toString(),
                name = project.name,
                description = project.description,
                createdAtEpochMillis = project.createdAtEpochMillis,
                updatedAtEpochMillis = project.updatedAtEpochMillis
            )
        )
        projectDataCleanup?.deleteProjectData(project.id)
    }
}
