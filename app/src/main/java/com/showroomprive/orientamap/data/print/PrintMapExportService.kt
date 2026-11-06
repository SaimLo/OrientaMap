package com.showroomprive.orientamap.data.print

import android.content.Context
import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.PrintMapSpec
import java.io.OutputStream

/**
 * Coordinates the MapLibre snapshot and the printable PDF generation.
 */
class PrintMapExportService(
    context: Context,
    private val snapshotter: MapLibrePrintSnapshotter = MapLibrePrintSnapshotter(context),
    private val exporter: PrintPdfExporter = PrintPdfExporter()
) {

    suspend fun exportSelectedArea(
        output: OutputStream,
        title: String,
        bounds: GeoBounds,
        spec: PrintMapSpec = PrintMapSpec()
    ) {
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
                background = bitmap
            )
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}
