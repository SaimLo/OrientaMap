package com.showroomprive.orientamap.domain.model

enum class ProjectStatus {
    BROUILLON,
    ZONE_DEFINIE,
    EDITION_EN_COURS,
    ARCHIVE
}

fun ProjectStatus.displayLabel(): String = when (this) {
    ProjectStatus.BROUILLON -> "Zone à définir"
    ProjectStatus.ZONE_DEFINIE -> "Zone définie"
    ProjectStatus.EDITION_EN_COURS -> "Édition en cours"
    ProjectStatus.ARCHIVE -> "Archivé"
}

fun String.toProjectStatus(): ProjectStatus =
    runCatching { ProjectStatus.valueOf(this) }
        .getOrDefault(ProjectStatus.BROUILLON)
