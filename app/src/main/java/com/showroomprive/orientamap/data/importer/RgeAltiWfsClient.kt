package com.showroomprive.orientamap.data.importer

import com.showroomprive.orientamap.domain.model.GeoBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class RgeAltiWfsClient(
    private val parser: RgeAltiGeoJsonParser = RgeAltiGeoJsonParser(),
    private val endpoint: String = DEFAULT_ENDPOINT
) {

    suspend fun fetchContours(bounds: GeoBounds): List<RgeAltiContourRecord> =
        parser.parse(fetchGeoJson(bounds))

    suspend fun fetchGeoJson(bounds: GeoBounds): String =
        withContext(Dispatchers.IO) {
            val connection = URL(buildRequestUrl(bounds)).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15_000
                connection.readTimeout = 60_000
                connection.setRequestProperty("Accept", "application/geo+json, application/json")

                val statusCode = connection.responseCode
                if (statusCode !in 200..299) {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    error("IGN WFS a répondu HTTP $statusCode${error?.let { ": $it" } ?: ""}")
                }

                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }

    fun buildRequestUrl(bounds: GeoBounds): String {
        val query = listOf(
            "SERVICE" to "WFS",
            "VERSION" to "2.0.0",
            "REQUEST" to "GetFeature",
            "TYPENAMES" to FEATURE_TYPE,
            "OUTPUTFORMAT" to "application/json",
            "SRSNAME" to "EPSG:4326",
            "BBOX" to "${bounds.west},${bounds.south},${bounds.east},${bounds.north},EPSG:4326"
        ).joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return "$endpoint?$query"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    internal companion object {
        const val SOURCE_KEY = "ign-wfs-elevation-contour-line-v1"
        const val DEFAULT_ENDPOINT = "https://data.geopf.fr/wfs/ows"
        const val FEATURE_TYPE = "ELEVATION.CONTOUR.LINE:courbe"
    }
}
