package com.showroomprive.orientamap

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.showroomprive.orientamap.data.print.PrintPdfExporter
import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.IsomContour
import com.showroomprive.orientamap.domain.model.Lambert93Projection
import com.showroomprive.orientamap.domain.model.Wgs84Coordinate
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrintPdfExporterTest {

    @Test
    fun exportsA3PdfWithReferenceBackground() {
        val bitmap = Bitmap.createBitmap(320, 220, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(230, 240, 230))
        val output = ByteArrayOutputStream()

        PrintPdfExporter().exportA3(
            output = output,
            title = "Test OrientaMap",
            background = bitmap
        )

        val pdf = output.toByteArray()
        assertTrue(pdf.size > 1_000)
        assertTrue(pdf.copyOfRange(0, 5).contentEquals("%PDF-".toByteArray()))
        bitmap.recycle()
    }

    @Test
    fun exportsA3PdfWithContourLayer() {
        val bitmap = Bitmap.createBitmap(320, 220, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(230, 240, 230))
        val output = ByteArrayOutputStream()
        val bounds = GeoBounds(48.90, 48.70, 2.50, 2.10)
        val contour = IsomContour(
            elevationMeters = 125.0,
            coordinates = listOf(
                Lambert93Projection.project(Wgs84Coordinate(2.20, 48.75)),
                Lambert93Projection.project(Wgs84Coordinate(2.40, 48.85))
            ),
            isIndexContour = true
        )

        PrintPdfExporter().exportA3(
            output = output,
            title = "Test OrientaMap contours",
            background = bitmap,
            geographicBounds = bounds,
            contours = listOf(contour)
        )

        val pdf = output.toByteArray()
        assertTrue(pdf.size > 1_000)
        assertTrue(pdf.copyOfRange(0, 5).contentEquals("%PDF-".toByteArray()))
        bitmap.recycle()
    }
}
