package com.showroomprive.orientamap.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PrintMapSpecTest {

    @Test
    fun defaultSpecUsesA3LandscapeAtTenThousandthScale() {
        val spec = PrintMapSpec()

        assertEquals(420.0, spec.pageWidthMm, 0.001)
        assertEquals(297.0, spec.pageHeightMm, 0.001)
        assertEquals(400.0, spec.contentWidthMm, 0.001)
        assertEquals(277.0, spec.contentHeightMm, 0.001)
        assertEquals(4_000.0, spec.groundWidthMeters, 0.001)
        assertEquals(2_770.0, spec.groundHeightMeters, 0.001)
        assertEquals(10.0, spec.metersPerMillimeter, 0.001)
        assertEquals(1.5, spec.isomSymbolScaleFactor, 0.001)
    }

    @Test
    fun portraitOrientationSwapsPageDimensions() {
        val spec = PrintMapSpec(orientation = PrintOrientation.PORTRAIT)

        assertEquals(297.0, spec.pageWidthMm, 0.001)
        assertEquals(420.0, spec.pageHeightMm, 0.001)
    }

    @Test
    fun rejectsMarginsThatDoNotFitThePage() {
        assertThrows(IllegalArgumentException::class.java) {
            PrintMapSpec(marginMm = 210.0)
        }
    }

    @Test
    fun rejectsNonPositiveScale() {
        assertThrows(IllegalArgumentException::class.java) {
            PrintMapSpec(scaleDenominator = 0)
        }
    }
}
