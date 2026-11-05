package com.showroomprive.orientamap.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflinePackageDao {
    @Query("SELECT * FROM offline_packages WHERE projectId = :projectId ORDER BY createdAtEpochMillis DESC")
    fun observeForProject(projectId: String): Flow<List<OfflinePackageEntity>>

    @Query("SELECT * FROM offline_packages WHERE id = :id")
    suspend fun getById(id: String): OfflinePackageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OfflinePackageEntity)

    @Update
    suspend fun update(entity: OfflinePackageEntity)

    @Delete
    suspend fun delete(entity: OfflinePackageEntity)
}
