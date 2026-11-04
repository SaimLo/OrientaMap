package com.showroomprive.orientamap.presentation.map

import android.Manifest
import android.graphics.PointF
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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

class MapLibreMapController(val map: MapLibreMap) : MapController {
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("map_screen")
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // Map component
        mapContent { ctrl ->
            internalMapController = ctrl
        }

        val activeMapInstance = (activeController as? MapLibreMapController)?.map

        // Isolated zone selection section
        if (viewModel.isSelectionModeActive && widthPx > 0f && heightPx > 0f) {
            ZoneSelectionSection(
                containerWidth = widthPx,
                containerHeight = heightPx,
                mapInstance = activeMapInstance,
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
        if (containerWidth <= 0f || containerHeight <= 0f) return@LaunchedEffect
        val rect = viewModel.selectionRect
        if (rect == null) {
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

internal enum class DragTarget {
    NONE,
    BODY,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

internal fun detectDragTarget(
    point: Offset,
    rect: Rect,
    handleRadius: Float
): DragTarget {
    val distTL = (point - Offset(rect.left, rect.top)).getDistance()
    if (distTL <= handleRadius) return DragTarget.TOP_LEFT

    val distTR = (point - Offset(rect.right, rect.top)).getDistance()
    if (distTR <= handleRadius) return DragTarget.TOP_RIGHT

    val distBL = (point - Offset(rect.left, rect.bottom)).getDistance()
    if (distBL <= handleRadius) return DragTarget.BOTTOM_LEFT

    val distBR = (point - Offset(rect.right, rect.bottom)).getDistance()
    if (distBR <= handleRadius) return DragTarget.BOTTOM_RIGHT

    if (rect.contains(point)) return DragTarget.BODY

    return DragTarget.NONE
}

internal fun applyDragDelta(
    target: DragTarget,
    startRect: Rect,
    delta: Offset,
    containerWidth: Float,
    containerHeight: Float,
    minSize: Float = 80f
): Rect {
    val r = startRect
    return when (target) {
        DragTarget.BODY -> {
            val w = r.width
            val h = r.height
            val maxLeft = (containerWidth - w).coerceAtLeast(0f)
            val maxTop = (containerHeight - h).coerceAtLeast(0f)
            val newLeft = (r.left + delta.x).coerceIn(0f, maxLeft)
            val newTop = (r.top + delta.y).coerceIn(0f, maxTop)
            Rect(newLeft, newTop, newLeft + w, newTop + h)
        }
        DragTarget.TOP_LEFT -> {
            val newLeft = (r.left + delta.x).coerceIn(0f, r.right - minSize)
            val newTop = (r.top + delta.y).coerceIn(0f, r.bottom - minSize)
            Rect(newLeft, newTop, r.right, r.bottom)
        }
        DragTarget.TOP_RIGHT -> {
            val newRight = (r.right + delta.x).coerceIn(r.left + minSize, containerWidth)
            val newTop = (r.top + delta.y).coerceIn(0f, r.bottom - minSize)
            Rect(r.left, newTop, newRight, r.bottom)
        }
        DragTarget.BOTTOM_LEFT -> {
            val newLeft = (r.left + delta.x).coerceIn(0f, r.right - minSize)
            val newBottom = (r.bottom + delta.y).coerceIn(r.top + minSize, containerHeight)
            Rect(newLeft, r.top, r.right, newBottom)
        }
        DragTarget.BOTTOM_RIGHT -> {
            val newRight = (r.right + delta.x).coerceIn(r.left + minSize, containerWidth)
            val newBottom = (r.bottom + delta.y).coerceIn(r.top + minSize, containerHeight)
            Rect(r.left, r.top, newRight, newBottom)
        }
        DragTarget.NONE -> r
    }
}

@Composable
private fun ZoneSelectionOverlay(
    rect: Rect,
    containerWidth: Float,
    containerHeight: Float,
    onRectChange: (Rect) -> Unit,
    onDragStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val handleTouchRadius = remember(density) { with(density) { 48.dp.toPx() } }
    val minSize = remember(density) { with(density) { 80.dp.toPx() } }

    val currentRect = rememberUpdatedState(rect)
    val currentOnRectChange = rememberUpdatedState(onRectChange)
    val currentOnDragStateChanged = rememberUpdatedState(onDragStateChanged)

    Box(
        modifier = modifier
            .testTag("zone_selection_overlay")
            .fillMaxSize()
            .pointerInput(containerWidth, containerHeight) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPoint = down.position
                    val startRect = currentRect.value
                    val target = detectDragTarget(startPoint, startRect, handleTouchRadius)

                    if (target != DragTarget.NONE) {
                        down.consume()
                        currentOnDragStateChanged.value(true)
                        var currentPos = startPoint
                        try {
                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                val newPos = change.position
                                if (newPos != currentPos) {
                                    change.consume()
                                    currentPos = newPos
                                    val delta = newPos - startPoint
                                    val updated = applyDragDelta(
                                        target = target,
                                        startRect = startRect,
                                        delta = delta,
                                        containerWidth = containerWidth,
                                        containerHeight = containerHeight,
                                        minSize = minSize
                                    )
                                    currentOnRectChange.value(updated)
                                }
                                if (!change.pressed) break
                            }
                        } finally {
                            currentOnDragStateChanged.value(false)
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
