package com.showroomprive.orientamap.domain.model

enum class OfflinePackageStatus {
    CREE,
    EN_ATTENTE,
    TELECHARGEMENT_EN_COURS,
    PAUSE,
    TERMINE,
    ANNULE,
    ECHEC,
    CORROMPU
}

fun OfflinePackageStatus.displayLabel(): String = when (this) {
    OfflinePackageStatus.CREE -> "Créé"
    OfflinePackageStatus.EN_ATTENTE -> "En attente"
    OfflinePackageStatus.TELECHARGEMENT_EN_COURS -> "Téléchargement en cours"
    OfflinePackageStatus.PAUSE -> "En pause"
    OfflinePackageStatus.TERMINE -> "Terminé"
    OfflinePackageStatus.ANNULE -> "Annulé"
    OfflinePackageStatus.ECHEC -> "Échec"
    OfflinePackageStatus.CORROMPU -> "Corrompu"
}

fun String.toOfflinePackageStatus(): OfflinePackageStatus =
    runCatching { OfflinePackageStatus.valueOf(this) }
        .getOrDefault(OfflinePackageStatus.CORROMPU)
