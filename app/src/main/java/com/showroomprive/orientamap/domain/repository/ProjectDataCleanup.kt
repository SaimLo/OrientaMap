package com.showroomprive.orientamap.domain.repository

import java.util.UUID

fun interface ProjectDataCleanup {
    suspend fun deleteProjectData(projectId: UUID)
}
