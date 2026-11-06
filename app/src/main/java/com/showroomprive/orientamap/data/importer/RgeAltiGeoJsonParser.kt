package com.showroomprive.orientamap.data.importer

import com.showroomprive.orientamap.domain.model.Wgs84Coordinate
import org.json.JSONArray
import org.json.JSONObject

class RgeAltiGeoJsonParser {

    fun parse(json: String): List<RgeAltiContourRecord> {
        val root = JSONObject(json)
        require(root.optString("type") == "FeatureCollection") {
            "Le GeoJSON RGE ALTI doit être une FeatureCollection"
        }

        val features = root.optJSONArray("features") ?: JSONArray()
        return buildList {
            for (index in 0 until features.length()) {
                val feature = features.getJSONObject(index)
                val properties = feature.optJSONObject("properties") ?: JSONObject()
                val elevation = readElevation(properties)
                val geometry = feature.optJSONObject("geometry")
                    ?: error("La feature RGE ALTI $index n'a pas de géométrie")
                val geometryType = geometry.optString("type")
                val coordinates = geometry.optJSONArray("coordinates")
                    ?: error("La feature RGE ALTI $index n'a pas de coordonnées")

                when (geometryType) {
                    "LineString" -> add(parseLine(coordinates, elevation))
                    "MultiLineString" -> {
                        for (lineIndex in 0 until coordinates.length()) {
                            add(parseLine(coordinates.getJSONArray(lineIndex), elevation))
                        }
                    }
                    else -> error("Géométrie RGE ALTI non supportée : $geometryType")
                }
            }
        }
    }

    private fun parseLine(coordinates: JSONArray, elevation: Double): RgeAltiContourRecord {
        val points = buildList {
            for (index in 0 until coordinates.length()) {
                val coordinate = coordinates.getJSONArray(index)
                require(coordinate.length() >= 2) {
                    "Une coordonnée GeoJSON doit contenir longitude et latitude"
                }
                add(
                    Wgs84Coordinate(
                        longitudeDegrees = coordinate.getDouble(0),
                        latitudeDegrees = coordinate.getDouble(1)
                    )
                )
            }
        }
        require(points.size >= 2) {
            "Une courbe GeoJSON doit contenir au moins deux points"
        }
        return RgeAltiContourRecord(elevation, points)
    }

    private fun readElevation(properties: JSONObject): Double {
        val keys = listOf("elevation", "altitude", "ALTITUDE", "z")
        val key = keys.firstOrNull { properties.has(it) }
            ?: error("La feature RGE ALTI ne contient pas d'altitude")
        return properties.getDouble(key)
    }
}
