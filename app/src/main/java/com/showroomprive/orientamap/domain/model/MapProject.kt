package com.showroomprive.orientamap.domain.model

import java.util.UUID

data class MapProject(
    val id: UUID,
    val name: String,
    val description: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
