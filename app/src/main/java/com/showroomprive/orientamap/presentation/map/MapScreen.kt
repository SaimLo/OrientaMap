package com.showroomprive.orientamap.presentation.map

import android.Manifest
import android.graphics.PointF
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.showroomprive.orientamap.data.location.LocationTracker
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import java.util.Locale
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt

class MapLibreMapController(private val map: MapLibreMap) : MapController {
    override fun moveCameraTo(location: Location) {
        map.animateCameraToLocation(location)
    }

    override fun updateUserLocation(location: Location) {
        map.ensureLocationMarker()
        map.updateLocationMarker(location)
    }
}

@Composable
fun MapScreen(
    projectTitle: String,
    onBack: () -> Unit,
    locationProvider: LocationProvider? = null,
    mapController: MapController? = null,
    hasLocationPermission: Boolean = locationProvider != null || hasLocationPermission(LocalContext.current),
    mapContent: @Composable (onControllerReady: (MapController) -> Unit) -> Unit = { onControllerReady ->
        var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
        var cameraTick by remember { mutableIntStateOf(0) }
        MapLibreView(
            modifier = Modifier.fillMaxSize().testTag("map_view"),
            onMapReady = { map ->
                mapInstance = map
                val ctrl = MapLibreMapController(map)
                map.ensureLocationMarker()
                if (map.cameraPosition.zoom < 2.0) {
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(46.6, 2.4))
                        .zoom(5.2)
                        .build()
                }
                onControllerReady(ctrl)
            },
            onCameraMove = {
                cameraTick++
            }
        )
    },
    viewModel: MapViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val activeLocationProvider = remember(locationProvider, context) {
        locationProvider ?: LocationTracker(context)
    }
    var internalMapController by remember { mutableStateOf<MapController?>(null) }
    var cameraTick by remember { mutableIntStateOf(0) }
    var hasPermissionState by remember(hasLocationPermission) { mutableStateOf(hasLocationPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissionState = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(hasPermissionState) {
        if (!hasPermissionState && locationProvider == null) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    DisposableEffect(hasPermissionState, activeLocationProvider) {
        if (hasPermissionState) {
            activeLocationProvider.start { newLocation ->
                viewModel.updateLocation(newLocation)
            }
        }
        onDispose { activeLocationProvider.stop() }
    }

    val activeController = mapController ?: internalMapController

    LaunchedEffect(viewModel.location, activeController) {
        val currentLocation = viewModel.location
        val ctrl = activeController
        if (currentLocation != null && ctrl != null) {
            ctrl.updateUserLocation(currentLocation)
            if (!viewModel.centeredOnLocation) {
                ctrl.moveCameraTo(currentLocation)
                viewModel.centeredOnLocation = true
            }
        }
    }

    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("map_screen")
            .onSizeChanged { size ->
                containerWidthPx = size.width.toFloat()
                containerHeightPx = size.height.toFloat()
            }
    ) {
        val widthPx = containerWidthPx
        val heightPx = containerHeightPx

        // Map component
        mapContent { ctrl ->
            internalMapController = ctrl
        }

        // Isolated zone selection section
        if (viewModel.isSelectionModeActive && widthPx > 0f && heightPx > 0f) {
            ZoneSelectionSection(
                containerWidth = widthPx,
                containerHeight = heightPx,
                mapInstance = null,
                cameraTick = cameraTick,
                viewModel = viewModel,
                onCancel = { viewModel.isSelectionModeActive = false }
            )
        }

        // Top info card (Project description block with close cross at top right and dashed zone selection icon)
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(end = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(projectTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (hasPermissionState) "GPS actif" else "Permission GPS requise",
                        color = if (hasPermissionState) Color(0xFF167A32) else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("gps_status")
                    )
                    viewModel.location?.let { current ->
                        Text(
                            text = "${"%.5f".format(Locale.US, current.latitude)}, ${"%.5f".format(Locale.US, current.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("location_coordinates")
                        )
                        Text(
                            text = "Précision : ${"%.1f".format(Locale.US, current.accuracy)} m",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("location_accuracy")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mode hors ligne : Prêt",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("offline_mode_indicator")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = {},
                            modifier = Modifier.testTag("download_area_button")
                        ) {
                            Text("Télécharger")
                        }
                        Button(
                            onClick = {},
                            modifier = Modifier.testTag("cancel_download_button")
                        ) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {},
                            modifier = Modifier.testTag("retry_download_button")
                        ) {
                            Text("Réessayer")
                        }
                    }
                    Text(
                        text = "Progression : 0%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("download_progress")
                    )

                    if (!viewModel.isSelectionModeActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Blue circular button with dashed white square icon
                        IconButton(
                            onClick = { viewModel.isSelectionModeActive = true },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF1E88E5), CircleShape)
                        ) {
                            Canvas(modifier = Modifier.size(20.dp)) {
                                val strokeWidth = 2.dp.toPx()
                                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(strokeWidth, strokeWidth),
                                    size = Size(size.width - 2 * strokeWidth, size.height - 2 * strokeWidth),
                                    style = Stroke(
                                        width = strokeWidth,
                                        pathEffect = dashEffect
                                    )
                                )
                            }
                        }
                    }
                }

                // Close cross at top right of the project description block
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneSelectionSection(
    containerWidth: Float,
    containerHeight: Float,
    mapInstance: MapLibreMap?,
    cameraTick: Int,
    viewModel: MapViewModel,
    onCancel: () -> Unit
) {
    LaunchedEffect(containerWidth, containerHeight) {
        val rect = viewModel.selectionRect
        if (rect == null || rect.right > containerWidth || rect.bottom > containerHeight) {
            val marginX = containerWidth * 0.15f
            val marginY = containerHeight * 0.25f
            viewModel.selectionRect = Rect(marginX, marginY, containerWidth - marginX, containerHeight - marginY)
        }
    }

    val selectionRect = viewModel.selectionRect ?: Rect(
        containerWidth * 0.15f,
        containerHeight * 0.25f,
        containerWidth * 0.85f,
        containerHeight * 0.75f
    )

    var isDraggingSelection by remember { mutableStateOf(false) }

    val (zoneWidthMeters, zoneHeightMeters, zoneAreaM2) = remember(selectionRect, cameraTick, mapInstance, isDraggingSelection) {
        val map = mapInstance
        val rect = selectionRect
        if (map != null) {
            if (isDraggingSelection) {
                val zoom = map.cameraPosition.zoom
                val lat = map.cameraPosition.target?.latitude ?: 46.6
                val metersPerPixel = 156543.03392 * cos(lat * Math.PI / 180.0) / (2.0.pow(zoom))
                val w = rect.width * metersPerPixel
                val h = rect.height * metersPerPixel
                Triple(w, h, w * h)
            } else {
                val pTL = map.projection.fromScreenLocation(PointF(rect.left, rect.top))
                val pTR = map.projection.fromScreenLocation(PointF(rect.right, rect.top))
                val pBL = map.projection.fromScreenLocation(PointF(rect.left, rect.bottom))
                val pBR = map.projection.fromScreenLocation(PointF(rect.right, rect.bottom))

                val distTop = FloatArray(1)
                Location.distanceBetween(pTL.latitude, pTL.longitude, pTR.latitude, pTR.longitude, distTop)
                val distBottom = FloatArray(1)
                Location.distanceBetween(pBL.latitude, pBL.longitude, pBR.latitude, pBR.longitude, distBottom)
                val w = (distTop[0] + distBottom[0]) / 2.0

                val distLeft = FloatArray(1)
                Location.distanceBetween(pTL.latitude, pTL.longitude, pBL.latitude, pBL.longitude, distLeft)
                val distRight = FloatArray(1)
                Location.distanceBetween(pTR.latitude, pTR.longitude, pBR.latitude, pBR.longitude, distRight)
                val h = (distLeft[0] + distRight[0]) / 2.0

                Triple(w, h, w * h)
            }
        } else {
            Triple(0.0, 0.0, 0.0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ZoneSelectionOverlay(
            rect = selectionRect,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            onRectChange = { viewModel.selectionRect = it },
            onDragStateChanged = { isDraggingSelection = it },
            isDragging = isDraggingSelection,
            modifier = Modifier.fillMaxSize()
        )

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Mode de sélection de zone",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Largeur", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatDistance(zoneWidthMeters), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Hauteur", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatDistance(zoneHeightMeters), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Surface approx.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatArea(zoneAreaM2), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Green validation button with only the icon (no text)
                    Button(
                        onClick = {
                            val marginX = 24f
                            val marginTop = 160f
                            val marginBottom = 190f
                            viewModel.selectionRect = Rect(marginX, marginTop, containerWidth - marginX, containerHeight - marginBottom)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Valider la sélection",
                            tint = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private enum class DragTarget {
    NONE,
    BODY,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

@Composable
private fun ZoneSelectionOverlay(
    rect: Rect,
    containerWidth: Float,
    containerHeight: Float,
    onRectChange: (Rect) -> Unit,
    onDragStateChanged: (Boolean) -> Unit,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    val handleTouchRadius = 56f
    val minSize = 80f

    val currentRect = rememberUpdatedState(rect)
    val currentOnRectChange = rememberUpdatedState(onRectChange)
    val currentOnDragStateChanged = rememberUpdatedState(onDragStateChanged)

    var initialBoundsForDrag by remember { mutableStateOf<Rect?>(null) }

    val activeBounds = if (isDragging && initialBoundsForDrag != null) {
        initialBoundsForDrag!!
    } else {
        val l = (rect.left - handleTouchRadius).coerceAtLeast(0f)
        val t = (rect.top - handleTouchRadius).coerceAtLeast(0f)
        val right = (rect.right + handleTouchRadius).coerceAtMost(containerWidth)
        val b = (rect.bottom + handleTouchRadius).coerceAtMost(containerHeight)
        Rect(l, t, right, b)
    }

    Box(
        modifier = modifier
            .bounds(activeBounds.left, activeBounds.top, activeBounds.right, activeBounds.bottom)
            .pointerInput(containerWidth, containerHeight) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val offset = down.position
                        val globalOffset = Offset(offset.x + activeBounds.left, offset.y + activeBounds.top)
                        val activeRect = currentRect.value

                        val distTL = (globalOffset - Offset(activeRect.left, activeRect.top)).getDistance()
                        val distTR = (globalOffset - Offset(activeRect.right, activeRect.top)).getDistance()
                        val distBL = (globalOffset - Offset(activeRect.left, activeRect.bottom)).getDistance()
                        val distBR = (globalOffset - Offset(activeRect.right, activeRect.bottom)).getDistance()

                        val target = when {
                            distTL <= handleTouchRadius -> DragTarget.TOP_LEFT
                            distTR <= handleTouchRadius -> DragTarget.TOP_RIGHT
                            distBL <= handleTouchRadius -> DragTarget.BOTTOM_LEFT
                            distBR <= handleTouchRadius -> DragTarget.BOTTOM_RIGHT
                            activeRect.contains(globalOffset) -> DragTarget.BODY
                            else -> DragTarget.NONE
                        }

                        if (target != DragTarget.NONE) {
                            down.consume()
                            initialBoundsForDrag = activeBounds
                            currentOnDragStateChanged.value(true)

                            val startRect = activeRect
                            val startGlobalOffset = globalOffset
                            val pointerId = down.id
                            try {
                                drag(pointerId) { change ->
                                    val totalDrag = change.position - startGlobalOffset
                                    if (totalDrag != Offset.Zero) {
                                        change.consume()
                                        val r = startRect
                                        val updatedRect = when (target) {
                                            DragTarget.BODY -> {
                                                val dx = totalDrag.x
                                                val dy = totalDrag.y
                                                val w = r.width
                                                val h = r.height
                                                val newLeft = (r.left + dx).coerceIn(0f, containerWidth - w)
                                                val newTop = (r.top + dy).coerceIn(0f, containerHeight - h)
                                                Rect(newLeft, newTop, newLeft + w, newTop + h)
                                            }
                                            DragTarget.TOP_LEFT -> {
                                                val newLeft = (r.left + totalDrag.x).coerceIn(0f, r.right - minSize)
                                                val newTop = (r.top + totalDrag.y).coerceIn(0f, r.bottom - minSize)
                                                Rect(newLeft, newTop, r.right, r.bottom)
                                            }
                                            DragTarget.TOP_RIGHT -> {
                                                val newRight = (r.right + totalDrag.x).coerceIn(r.left + minSize, containerWidth)
                                                val newTop = (r.top + totalDrag.y).coerceIn(0f, r.bottom - minSize)
                                                Rect(r.left, newTop, newRight, r.bottom)
                                            }
                                            DragTarget.BOTTOM_LEFT -> {
                                                val newLeft = (r.left + totalDrag.x).coerceIn(0f, r.right - minSize)
                                                val newBottom = (r.bottom + totalDrag.y).coerceIn(r.top + minSize, containerHeight)
                                                Rect(newLeft, r.top, r.right, newBottom)
                                            }
                                            DragTarget.BOTTOM_RIGHT -> {
                                                val newRight = (r.right + totalDrag.x).coerceIn(r.left + minSize, containerWidth)
                                                val newBottom = (r.bottom + totalDrag.y).coerceIn(r.top + minSize, containerHeight)
                                                Rect(r.left, r.top, newRight, newBottom)
                                            }
                                            else -> r
                                        }
                                        currentOnRectChange.value(updatedRect)
                                    }
                                }
                            } finally {
                                initialBoundsForDrag = null
                                currentOnDragStateChanged.value(false)
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val primaryColor = Color(0xFF1E88E5)
            val fillColor = primaryColor.copy(alpha = 0.20f)
            val strokeWidthPx = 3.dp.toPx()

            drawRect(
                color = fillColor,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height)
            )

            drawRect(
                color = primaryColor,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                style = Stroke(width = strokeWidthPx)
            )

            fnDrawHandle(Offset(rect.left, rect.top))
            fnDrawHandle(Offset(rect.right, rect.top))
            fnDrawHandle(Offset(rect.left, rect.bottom))
            fnDrawHandle(Offset(rect.right, rect.bottom))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.fnDrawHandle(center: Offset) {
    val handleRadiusPx = 10.dp.toPx()
    drawCircle(color = Color(0xFF1E88E5), radius = handleRadiusPx, center = center)
    drawCircle(color = Color.White, radius = handleRadiusPx * 0.65f, center = center)
}

private fun Modifier.bounds(left: Float, top: Float, right: Float, bottom: Float) = this.layout { measurable, constraints ->
    val width = (right - left).roundToInt().coerceAtLeast(0)
    val height = (bottom - top).roundToInt().coerceAtLeast(0)
    val placeable = measurable.measure(constraints.copy(
        minWidth = width, maxWidth = width,
        minHeight = height, maxHeight = height
    ))
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.placeRelative(left.roundToInt(), top.roundToInt())
    }
}

internal fun formatDistance(meters: Double): String {
    return if (meters < 1000.0) {
        "${meters.roundToInt()} m"
    } else {
        "%.2f km".format(Locale.US, meters / 1000.0)
    }
}

internal fun formatArea(areaM2: Double): String {
    return when {
        areaM2 < 10_000.0 -> {
            "${areaM2.roundToInt()} m²"
        }
        areaM2 < 1_000_000.0 -> {
            val ha = areaM2 / 10_000.0
            "%.2f ha (%d m²)".format(Locale.US, ha, areaM2.roundToInt())
        }
        else -> {
            val km2 = areaM2 / 1_000_000.0
            val ha = areaM2 / 10_000.0
            "%.2f km² (%.1f ha)".format(km2, ha)
        }
    }
}

private fun hasLocationPermission(context: android.content.Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private fun MapLibreMap.animateCameraToLocation(location: Location) {
    animateCamera(
        org.maplibre.android.camera.CameraUpdateFactory.newLatLng(
            LatLng(location.latitude, location.longitude)
        )
    )
}

const val LOCATION_SOURCE_ID = "device-location-source"
const val LOCATION_LAYER_ID = "device-location-layer"

const val EMPTY_LOCATION_GEOJSON =
    "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[0,0]}}"

private fun MapLibreMap.ensureLocationMarker() {
    val currentState = style ?: return
    if (currentState.getSourceAs<GeoJsonSource>(LOCATION_SOURCE_ID) == null) {
        currentState.addSource(GeoJsonSource(LOCATION_SOURCE_ID, EMPTY_LOCATION_GEOJSON))
        currentState.addLayer(
            CircleLayer(LOCATION_LAYER_ID, LOCATION_SOURCE_ID).apply {
                setProperties(
                    PropertyFactory.circleColor(android.graphics.Color.rgb(20, 91, 200)),
                    PropertyFactory.circleRadius(7f),
                    PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
                    PropertyFactory.circleStrokeWidth(2f)
                )
            }
        )
    }
}

private fun MapLibreMap.updateLocationMarker(location: Location) {
    val source = style?.getSourceAs<GeoJsonSource>(LOCATION_SOURCE_ID) ?: return
    source.setGeoJson(
        "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[${location.longitude},${location.latitude}]}}"
    )
}
