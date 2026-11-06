package com.showroomprive.orientamap.data.print

import android.content.Context
import com.showroomprive.orientamap.data.importer.RgeAltiContourCacheStore
import com.showroomprive.orientamap.data.importer.RgeAltiContourImporter
import com.showroomprive.orientamap.data.importer.RgeAltiGeoJsonParser
import com.showroomprive.orientamap.data.importer.RgeAltiWfsClient
import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.PrintMapSpec
import java.io.OutputStream
import java.util.UUID

/**
 * Coordinates the MapLibre snapshot and the printable PDF generation.
 */
class PrintMapExportService(
    context: Context,
    private val snapshotter: MapLibrePrintSnapshotter = MapLibrePrintSnapshotter(context),
    private val exporter: PrintPdfExporter = PrintPdfExporter(),
    private val contourClient: RgeAltiWfsClient = RgeAltiWfsClient(),
    private val contourParser: RgeAltiGeoJsonParser = RgeAltiGeoJsonParser(),
    private val contourImporter: RgeAltiContourImporter = RgeAltiContourImporter(),
    private val contourCache: RgeAltiContourCacheStore = RgeAltiContourCacheStore(context)
) {

    suspend fun exportSelectedArea(
        output: OutputStream,
        title: String,
        bounds: GeoBounds,
        spec: PrintMapSpec = PrintMapSpec(),
        projectId: UUID? = null
    ) {
        val geoJson = contourCache.getOrFetch(projectId, bounds) {
            contourClient.fetchGeoJson(bounds)
        }
        val contours = contourImporter.import(contourParser.parse(geoJson))
        val bitmap = snapshotter.capture(
            bounds = bounds,
            widthPx = 2_400,
            heightPx = 1_662
        )
        try {
            exporter.exportA3(
                output = output,
                spec = spec,
                title = title,
                background = bitmap,
                geographicBounds = bounds,
                contours = contours
            )
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}
