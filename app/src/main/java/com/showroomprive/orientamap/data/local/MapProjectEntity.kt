package com.showroomprive.orientamap.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.showroomprive.orientamap.domain.model.MapProject
import java.util.UUID

@Entity(tableName = "map_projects")
data class MapProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
) {
    fun toDomain(): MapProject = MapProject(
        id = UUID.fromString(id),
        name = name,
        description = description,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}
