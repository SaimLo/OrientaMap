package com.showroomprive.orientamap.domain.model

enum class PrintOrientation {
    LANDSCAPE,
    PORTRAIT
}

data class PrintMapSpec(
    val paperWidthMm: Double = 420.0,
    val paperHeightMm: Double = 297.0,
    val marginMm: Double = 10.0,
    val scaleDenominator: Int = 10_000,
    val orientation: PrintOrientation = PrintOrientation.LANDSCAPE,
    val coordinateReferenceSystem: String = "EPSG:2154"
) {
    init {
        require(paperWidthMm > 0.0) { "La largeur du papier doit être positive" }
        require(paperHeightMm > 0.0) { "La hauteur du papier doit être positive" }
        require(marginMm >= 0.0) { "La marge ne peut pas être négative" }
        require(marginMm * 2.0 < paperWidthMm) {
            "Les marges dépassent la largeur du papier"
        }
        require(marginMm * 2.0 < paperHeightMm) {
            "Les marges dépassent la hauteur du papier"
        }
        require(scaleDenominator > 0) { "Le dénominateur d'échelle doit être positif" }
        require(coordinateReferenceSystem.isNotBlank()) {
            "Le système de coordonnées ne peut pas être vide"
        }
    }

    val pageWidthMm: Double
        get() = if (orientation == PrintOrientation.LANDSCAPE) {
            maxOf(paperWidthMm, paperHeightMm)
        } else {
            minOf(paperWidthMm, paperHeightMm)
        }

    val pageHeightMm: Double
        get() = if (orientation == PrintOrientation.LANDSCAPE) {
            minOf(paperWidthMm, paperHeightMm)
        } else {
            maxOf(paperWidthMm, paperHeightMm)
        }

    val contentWidthMm: Double
        get() = pageWidthMm - marginMm * 2.0

    val contentHeightMm: Double
        get() = pageHeightMm - marginMm * 2.0

    val groundWidthMeters: Double
        get() = contentWidthMm * scaleDenominator / 1000.0

    val groundHeightMeters: Double
        get() = contentHeightMm * scaleDenominator / 1000.0

    val metersPerMillimeter: Double
        get() = scaleDenominator / 1000.0

    /** ISOM symbol enlargement relative to the 1:15,000 reference scale. */
    val isomSymbolScaleFactor: Double
        get() = 15_000.0 / scaleDenominator
}

