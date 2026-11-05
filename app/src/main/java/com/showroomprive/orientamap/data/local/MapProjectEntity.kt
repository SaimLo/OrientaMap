package com.showroomprive.orientamap.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.MapProject
import java.util.UUID

@Entity(tableName = "map_projects")
data class MapProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val boundsNorth: Double? = null,
    val boundsSouth: Double? = null,
    val boundsEast: Double? = null,
    val boundsWest: Double? = null
) {
    fun toDomain(): MapProject = MapProject(
        id = UUID.fromString(id),
        name = name,
        description = description,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        geoBounds = toGeoBoundsOrNull()
    )

    private fun toGeoBoundsOrNull(): GeoBounds? {
        val north = boundsNorth ?: return null
        val south = boundsSouth ?: return null
        val east = boundsEast ?: return null
        val west = boundsWest ?: return null
        return GeoBounds(north = north, south = south, east = east, west = west)
    }
}
