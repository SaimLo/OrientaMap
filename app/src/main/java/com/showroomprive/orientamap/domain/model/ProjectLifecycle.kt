package com.showroomprive.orientamap.domain.model

fun MapProject.canDefineArea(): Boolean =
    status == ProjectStatus.BROUILLON && geoBounds == null

fun MapProject.defineArea(
    bounds: GeoBounds,
    updatedAtEpochMillis: Long = System.currentTimeMillis()
): MapProject {
    check(canDefineArea()) { "La zone de ce projet est déjà définie" }
    return copy(
        geoBounds = bounds,
        status = ProjectStatus.ZONE_DEFINIE,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}

fun MapProject.startEditing(
    updatedAtEpochMillis: Long = System.currentTimeMillis()
): MapProject {
    check(status == ProjectStatus.ZONE_DEFINIE) {
        "L'édition ne peut commencer qu'après la définition de la zone"
    }
    return copy(
        status = ProjectStatus.EDITION_EN_COURS,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}

fun MapProject.archive(
    updatedAtEpochMillis: Long = System.currentTimeMillis()
): MapProject {
    check(status != ProjectStatus.ARCHIVE) { "Le projet est déjà archivé" }
    return copy(
        status = ProjectStatus.ARCHIVE,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}
