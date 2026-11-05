package com.showroomprive.orientamap.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.showroomprive.orientamap.domain.model.MapProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface MapProjectDao {
    @Query("SELECT * FROM map_projects ORDER BY updatedAtEpochMillis DESC")
    fun observeEntities(): Flow<List<MapProjectEntity>>

    @Query("SELECT * FROM map_projects WHERE id = :id")
    suspend fun getProjectById(id: String): MapProjectEntity?

    @Query("UPDATE map_projects SET name = :newName, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateProjectName(id: String, newName: String, updatedAt: Long)

    @Query("""
        UPDATE map_projects
        SET boundsNorth = :north,
            boundsSouth = :south,
            boundsEast = :east,
            boundsWest = :west,
            status = :definedStatus,
            updatedAtEpochMillis = :updatedAt
        WHERE id = :id
          AND status = :draftStatus
          AND boundsNorth IS NULL
          AND boundsSouth IS NULL
          AND boundsEast IS NULL
          AND boundsWest IS NULL
    """)
    suspend fun defineProjectArea(
        id: String,
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        draftStatus: String,
        definedStatus: String,
        updatedAt: Long
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MapProjectEntity)

    @Delete
    suspend fun delete(entity: MapProjectEntity)
}

fun MapProjectDao.observeProjects(): Flow<List<MapProject>> =
    observeEntities().map { entities -> entities.map(MapProjectEntity::toDomain) }
