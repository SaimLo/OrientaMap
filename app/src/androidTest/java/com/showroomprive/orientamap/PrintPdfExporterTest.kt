package com.showroomprive.orientamap

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.showroomprive.orientamap.data.print.PrintPdfExporter
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
}
