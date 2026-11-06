package com.showroomprive.orientamap

import com.showroomprive.orientamap.data.importer.RgeAltiGeoJsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RgeAltiGeoJsonParserTest {

    @Test
    fun parsesLineStringAndMultiLineStringFeatures() {
        val json = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {"elevation": 100},
                  "geometry": {
                    "type": "LineString",
                    "coordinates": [[3.0, 46.5], [3.001, 46.501]]
                  }
                },
                {
                  "type": "Feature",
                  "properties": {"ALTITUDE": 125},
                  "geometry": {
                    "type": "MultiLineString",
                    "coordinates": [
                      [[3.0, 46.5], [3.001, 46.501]],
                      [[3.002, 46.502], [3.003, 46.503]]
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val records = RgeAltiGeoJsonParser().parse(json)

        assertEquals(3, records.size)
        assertEquals(100.0, records[0].elevationMeters, 0.001)
        assertEquals(125.0, records[1].elevationMeters, 0.001)
        assertEquals(3.0, records[0].coordinates[0].longitudeDegrees, 0.001)
    }

    @Test
    fun rejectsGeoJsonWithoutElevation() {
        val json = """
            {"type":"FeatureCollection","features":[{
              "type":"Feature",
              "properties":{},
              "geometry":{"type":"LineString","coordinates":[[3.0,46.5],[3.1,46.6]]}
            }]}
        """.trimIndent()

        assertThrows(IllegalStateException::class.java) {
            RgeAltiGeoJsonParser().parse(json)
        }
    }
}
