package com.showroomprive.orientamap.domain.repository

import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.MapProject
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface MapProjectRepository {
    fun observeProjects(): Flow<List<MapProject>>
    suspend fun getProject(id: UUID): MapProject?
    suspend fun createProject(name: String, description: String?): UUID
    suspend fun renameProject(id: UUID, newName: String)
    suspend fun defineProjectArea(id: UUID, bounds: GeoBounds): Boolean
    suspend fun deleteProject(project: MapProject)
}
