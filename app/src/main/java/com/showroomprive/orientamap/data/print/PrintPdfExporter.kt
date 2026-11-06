package com.showroomprive.orientamap.data.print

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.IsomContour
import com.showroomprive.orientamap.domain.model.Lambert93Projection
import com.showroomprive.orientamap.domain.model.PrintMapSpec
import com.showroomprive.orientamap.domain.model.Wgs84Coordinate
import java.io.OutputStream
import kotlin.math.roundToInt

/**
 * First printable-map prototype.
 *
 * The bitmap is currently a reference background, for example a MapLibre
 * snapshot. The ISOM vector layers will be added in a later iteration.
 */
class PrintPdfExporter {

    fun exportA3(
        output: OutputStream,
        spec: PrintMapSpec = PrintMapSpec(),
        title: String,
        background: Bitmap? = null,
        geographicBounds: GeoBounds? = null,
        contours: List<IsomContour> = emptyList()
    ) {
        require(spec.pageWidthMm > 0.0 && spec.pageHeightMm > 0.0)
        require(title.isNotBlank()) { "Le titre de la carte ne peut pas être vide" }

        val document = PdfDocument()
        try {
            val pageWidth = mmToPoints(spec.pageWidthMm)
            val pageHeight = mmToPoints(spec.pageHeightMm)
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            drawPage(page.canvas, spec, title, background, geographicBounds, contours)
            document.finishPage(page)
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun drawPage(
        canvas: Canvas,
        spec: PrintMapSpec,
        title: String,
        background: Bitmap?,
        geographicBounds: GeoBounds?,
        contours: List<IsomContour>
    ) {
        canvas.drawColor(Color.WHITE)

        val margin = mmToPoints(spec.marginMm).toFloat()
        val pageWidth = canvas.width.toFloat()
        val pageHeight = canvas.height.toFloat()
        val content = RectF(
            margin,
            margin,
            pageWidth - margin,
            pageHeight - margin
        )
        val headerHeight = mmToPoints(16.0).toFloat()
        val mapRect = RectF(
            content.left,
            content.top + headerHeight,
            content.right,
            content.bottom
        )

        background?.let { drawBitmapCover(canvas, it, mapRect) }
        if (geographicBounds != null && contours.isNotEmpty()) {
            drawContours(canvas, mapRect, spec, geographicBounds, contours)
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = mmToPoints(0.25).toFloat()
        }
        canvas.drawRect(mapRect, linePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.SANS_SERIF,
                android.graphics.Typeface.NORMAL
            )
        }
        textPaint.textSize = mmToPoints(5.0).toFloat()
        canvas.drawText(title, content.left, content.top + mmToPoints(6.0), textPaint)

        textPaint.textSize = mmToPoints(3.0).toFloat()
        canvas.drawText(
            "A3 paysage   Échelle 1:${spec.scaleDenominator}   ${spec.coordinateReferenceSystem}",
            content.left,
            content.top + mmToPoints(11.0),
            textPaint
        )

        drawScaleBar(canvas, mapRect, spec)
        drawNorthArrow(canvas, mapRect)
    }


    private fun drawContours(
        canvas: Canvas,
        mapRect: RectF,
        spec: PrintMapSpec,
        bounds: GeoBounds,
        contours: List<IsomContour>
    ) {
        val projectedCorners = listOf(
            Lambert93Projection.project(Wgs84Coordinate(bounds.west, bounds.south)),
            Lambert93Projection.project(Wgs84Coordinate(bounds.west, bounds.north)),
            Lambert93Projection.project(Wgs84Coordinate(bounds.east, bounds.south)),
            Lambert93Projection.project(Wgs84Coordinate(bounds.east, bounds.north))
        )
        val minEasting = projectedCorners.minOf { it.eastingMeters }
        val maxEasting = projectedCorners.maxOf { it.eastingMeters }
        val minNorthing = projectedCorners.minOf { it.northingMeters }
        val maxNorthing = projectedCorners.maxOf { it.northingMeters }
        val eastingSpan = (maxEasting - minEasting).coerceAtLeast(1.0)
        val northingSpan = (maxNorthing - minNorthing).coerceAtLeast(1.0)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 164, 119, 70)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        canvas.save()
        canvas.clipRect(mapRect)
        contours.forEach { contour ->
            paint.strokeWidth = mmToPoints(
                if (contour.isIndexContour) {
                    0.22 * spec.isomSymbolScaleFactor
                } else {
                    0.12 * spec.isomSymbolScaleFactor
                }
            ).toFloat()
            val path = Path()
            contour.coordinates.forEachIndexed { index, coordinate ->
                val x = mapRect.left +
                    ((coordinate.eastingMeters - minEasting) / eastingSpan * mapRect.width())
                val y = mapRect.bottom -
                    ((coordinate.northingMeters - minNorthing) / northingSpan * mapRect.height())
                if (index == 0) path.moveTo(x.toFloat(), y.toFloat())
                else path.lineTo(x.toFloat(), y.toFloat())
            }
            canvas.drawPath(path, paint)
        }
        canvas.restore()
    }

    private fun drawScaleBar(canvas: Canvas, mapRect: RectF, spec: PrintMapSpec) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        val barWidth = mmToPoints(100.0).toFloat()
        val barHeight = mmToPoints(2.0).toFloat()
        val left = mapRect.left + mmToPoints(8.0)
        val top = mapRect.bottom - mmToPoints(12.0)
        canvas.drawRect(left, top, left + barWidth / 2f, top + barHeight, paint)
        paint.color = Color.WHITE
        canvas.drawRect(left + barWidth / 2f, top, left + barWidth, top + barHeight, paint)
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = mmToPoints(0.25).toFloat()
        canvas.drawRect(left, top, left + barWidth, top + barHeight, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = mmToPoints(3.0).toFloat()
        canvas.drawText("0", left, top + mmToPoints(6.0), paint)
        canvas.drawText("500 m", left + barWidth / 2f - mmToPoints(8.0), top + mmToPoints(6.0), paint)
        canvas.drawText("1 km", left + barWidth - mmToPoints(8.0), top + mmToPoints(6.0), paint)
    }

    private fun drawNorthArrow(canvas: Canvas, mapRect: RectF) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = mmToPoints(0.5).toFloat()
        }
        val centerX = mapRect.right - mmToPoints(15.0)
        val tipY = mapRect.top + mmToPoints(12.0)
        val baseY = tipY + mmToPoints(15.0)
        canvas.drawLine(centerX, baseY, centerX, tipY, paint)
        canvas.drawLine(centerX, tipY, centerX - mmToPoints(3.0), tipY + mmToPoints(6.0), paint)
        canvas.drawLine(centerX, tipY, centerX + mmToPoints(3.0), tipY + mmToPoints(6.0), paint)
        paint.style = Paint.Style.FILL
        paint.textSize = mmToPoints(3.0).toFloat()
        canvas.drawText("N", centerX - mmToPoints(2.0), baseY + mmToPoints(5.0), paint)
    }

    private fun drawBitmapCover(canvas: Canvas, bitmap: Bitmap, destination: RectF) {
        val sourceWidth = bitmap.width.toFloat()
        val sourceHeight = bitmap.height.toFloat()
        val sourceRatio = sourceWidth / sourceHeight
        val destinationRatio = destination.width() / destination.height()
        val source = if (sourceRatio > destinationRatio) {
            val width = sourceHeight * destinationRatio
            Rect(
                ((sourceWidth - width) / 2f).roundToInt(),
                0,
                ((sourceWidth + width) / 2f).roundToInt(),
                bitmap.height
            )
        } else {
            val height = sourceWidth / destinationRatio
            Rect(
                0,
                ((sourceHeight - height) / 2f).roundToInt(),
                bitmap.width,
                ((sourceHeight + height) / 2f).roundToInt()
            )
        }
        canvas.drawBitmap(bitmap, source, destination, Paint(Paint.ANTI_ALIAS_FLAG))
    }

    private fun mmToPoints(millimeters: Double): Int =
        (millimeters / 25.4 * 72.0).roundToInt()
}

