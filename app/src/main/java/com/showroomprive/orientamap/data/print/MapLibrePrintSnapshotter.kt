package com.showroomprive.orientamap.data.print

import android.content.Context
import android.graphics.Bitmap
import com.showroomprive.orientamap.config.MapStyleConfig
import com.showroomprive.orientamap.domain.model.GeoBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshotter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Captures the selected map area as a bitmap for the first printable PDF
 * prototype. The snapshot is rendered with the same style as the live map.
 */
class MapLibrePrintSnapshotter(
    private val context: Context
) {

    suspend fun capture(
        bounds: GeoBounds,
        widthPx: Int,
        heightPx: Int
    ): Bitmap = withContext(Dispatchers.Main.immediate) {
        require(widthPx > 0) { "La largeur du snapshot doit être positive" }
        require(heightPx > 0) { "La hauteur du snapshot doit être positive" }

        suspendCancellableCoroutine { continuation ->
            val options = MapSnapshotter.Options(widthPx, heightPx)
                .withStyleBuilder(Style.Builder().fromUri(MapStyleConfig.IGN_STYLE_URI))
                .withRegion(
                    LatLngBounds.from(
                        bounds.north,
                        bounds.east,
                        bounds.south,
                        bounds.west
                    )
                )
                .withPixelRatio(1f)
                .withLogo(false)
            val snapshotter = MapSnapshotter(context, options)

            continuation.invokeOnCancellation {
                snapshotter.cancel()
            }

            snapshotter.start(
                { snapshot ->
                    if (continuation.isActive) {
                        continuation.resume(snapshot.bitmap)
                    } else {
                        snapshot.bitmap.recycle()
                    }
                },
                { error ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            IllegalStateException("Échec du snapshot MapLibre : $error")
                        )
                    }
                }
            )
        }
    }
}
