package com.showroomprive.orientamap.data.importer

import com.showroomprive.orientamap.domain.model.GeoBounds
import org.junit.Assert.assertTrue
import org.junit.Test

class RgeAltiWfsClientTest {

    @Test
    fun buildsIgnWfsRequestForProjectBounds() {
        val url = RgeAltiWfsClient().buildRequestUrl(
            GeoBounds(
                north = 48.90,
                south = 48.80,
                east = 2.40,
                west = 2.30
            )
        )

        assertTrue(url.startsWith("https://data.geopf.fr/wfs/ows?"))
        assertTrue(url.contains("SERVICE=WFS"))
        assertTrue(url.contains("REQUEST=GetFeature"))
        assertTrue(url.contains("TYPENAMES=ELEVATION.CONTOUR.LINE%3Acourbe"))
        assertTrue(url.contains("OUTPUTFORMAT=application%2Fjson"))
        assertTrue(url.contains("BBOX=2.3%2C48.8%2C2.4%2C48.9%2CEPSG%3A4326"))
    }
}
