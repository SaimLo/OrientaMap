package com.showroomprive.orientamap.presentation.map

import android.Manifest
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.showroomprive.orientamap.data.location.LocationTracker
import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.OfflinePackage
import com.showroomprive.orientamap.domain.model.OfflinePackageStatus
import com.showroomprive.orientamap.domain.model.ProjectStatus
import com.showroomprive.orientamap.domain.model.displayLabel
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

class MapLibreMapController(private val map: MapLibreMap) : MapController {
    override fun moveCameraTo(location: Location) {
        map.animateCameraToLocation(location)
    }

    override fun currentZoom(): Double = map.cameraPosition.zoom

    override fun updateUserLocation(location: Location) {
        map.ensureLocationMarker()
        map.updateLocationMarker(location)
    }

    override fun screenCoordinateProjector(): ScreenCoordinateProjector =
        MapLibreScreenCoordinateProjector(map)

    override fun moveCameraToBounds(bounds: GeoBounds) {
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                LatLngBounds.from(
                    bounds.north,
                    bounds.east,
                    bounds.south,
                    bounds.west
                ),
                64
            )
        )
    }

    override fun showSelectedArea(bounds: GeoBounds) {
        map.getStyle { style ->
            val geoJson = bounds.toPolygonGeoJson()
            val source = style.getSourceAs<GeoJsonSource>(SELECTED_AREA_SOURCE_ID)
            if (source == null) {
                style.addSource(GeoJsonSource(SELECTED_AREA_SOURCE_ID, geoJson))
                style.addLayer(
                    LineLayer(SELECTED_AREA_LAYER_ID, SELECTED_AREA_SOURCE_ID).apply {
                        setProperties(
                            PropertyFactory.lineColor(android.graphics.Color.rgb(30, 136, 229)),
                            PropertyFactory.lineWidth(3f),
                            PropertyFactory.lineOpacity(0.9f)
                        )
                    }
                )
            } else {
                source.setGeoJson(geoJson)
            }
        }
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
        MapLibreView(
            modifier = Modifier.fillMaxSize().testTag("map_view"),
            onMapReady = { map ->
                val ctrl = MapLibreMapController(map)
                map.setMaxZoomPreference(16.0)
                map.ensureLocationMarker()
                if (map.cameraPosition.zoom < 2.0) {
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(46.6, 2.4))
                        .zoom(5.2)
                        .build()
                }
                onControllerReady(ctrl)
            },
        )
    },
    viewModel: MapViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onSelectionValidated: (GeoBounds) -> Unit = {},
    projectStatus: ProjectStatus = ProjectStatus.BROUILLON,
    projectBounds: GeoBounds? = null,
    offlinePackages: List<OfflinePackage> = emptyList(),
    onStartOfflineDownload: () -> Unit = {},
    onPauseOfflineDownload: (OfflinePackage) -> Unit = {},
    onResumeOfflineDownload: (OfflinePackage) -> Unit = {},
    onCancelOfflineDownload: (OfflinePackage) -> Unit = {},
    onExportPdf: () -> Unit = {}
) {
    val context = LocalContext.current
    val activeLocationProvider = remember(locationProvider, context) {
        locationProvider ?: LocationTracker(context)
    }
    var internalMapController by remember { mutableStateOf<MapController?>(null) }
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
    val screenCoordinateProjector = activeController?.screenCoordinateProjector()
    var currentZoom by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(activeController, mapController) {
        if (mapController != null) return@LaunchedEffect
        while (isActive) {
            currentZoom = activeController?.currentZoom()
            delay(250)
        }
    }
    val areaLocked = projectStatus != ProjectStatus.BROUILLON || viewModel.selectedGeoBounds != null

    LaunchedEffect(areaLocked) {
        if (areaLocked) {
            viewModel.isSelectionModeActive = false
        }
    }

    LaunchedEffect(viewModel.location, activeController, projectBounds) {
        val currentLocation = viewModel.location
        val ctrl = activeController
        if (currentLocation != null && ctrl != null) {
            ctrl.updateUserLocation(currentLocation)
            if (projectBounds == null && !viewModel.centeredOnLocation) {
                ctrl.moveCameraTo(currentLocation)
                viewModel.centeredOnLocation = true
            }
        }
    }

    LaunchedEffect(activeController, projectBounds) {
        val bounds = projectBounds ?: return@LaunchedEffect
        val ctrl = activeController ?: return@LaunchedEffect
        ctrl.showSelectedArea(bounds)
        if (!viewModel.centeredOnProjectBounds) {
            ctrl.moveCameraToBounds(bounds)
            viewModel.centeredOnProjectBounds = true
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

        // Non-interactive rectangle canvas. It is rendered before the cards,
        // so it never blocks MapLibre gestures.
        if (viewModel.isSelectionModeActive && widthPx > 0f && heightPx > 0f) {
            ZoneSelectionCanvas(
                containerWidth = widthPx,
                containerHeight = heightPx,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
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
                        text = projectStatus.displayLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("project_status")
                    )
                    currentZoom?.let { zoom ->
                        Text(
                            text = "Zoom : ${"%.1f".format(Locale.US, zoom)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("map_zoom_indicator")
                        )
                    }
                    Text(
                        text = if (offlinePackages.any { it.status == OfflinePackageStatus.TERMINE }) {
                            "Disponible hors connexion"
                        } else {
                            "Hors ligne non configuré"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("offline_mode_indicator")
                    )

                    if (
                        projectBounds != null &&
                            projectStatus != ProjectStatus.BROUILLON &&
                            projectStatus != ProjectStatus.ARCHIVE
                    ) {
                        OfflinePackageCard(
                            offlinePackage = offlinePackages.firstOrNull(),
                            onStart = onStartOfflineDownload,
                            onPause = onPauseOfflineDownload,
                            onResume = onResumeOfflineDownload,
                            onCancel = onCancelOfflineDownload
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onExportPdf,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_pdf_button")
                        ) {
                            Text("Exporter en PDF")
                        }
                    }

                    if (!viewModel.isSelectionModeActive && !areaLocked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Blue circular button with dashed white square icon
                        IconButton(
                            onClick = { viewModel.isSelectionModeActive = true },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("define_area_button")
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

        // The compact action bar is interactive only on its own buttons.
        if (viewModel.isSelectionModeActive && widthPx > 0f && heightPx > 0f) {
            SelectionActionBar(
                containerWidth = widthPx,
                containerHeight = heightPx,
                viewModel = viewModel,
                projector = screenCoordinateProjector,
                onSelectionValidated = { bounds ->
                    viewModel.isSelectionModeActive = false
                    activeController?.showSelectedArea(bounds)
                    onSelectionValidated(bounds)
                },
                onCancel = { viewModel.isSelectionModeActive = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            )

            // Only the four handle boxes are interactive. The parent itself has
            // no pointerInput, so map gestures pass through everywhere else.
            SelectionHandleLayer(
                containerWidth = widthPx,
                containerHeight = heightPx,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
            )
        }
    }
}

@Composable
private fun OfflinePackageCard(
    offlinePackage: OfflinePackage?,
    onStart: () -> Unit,
    onPause: (OfflinePackage) -> Unit,
    onResume: (OfflinePackage) -> Unit,
    onCancel: (OfflinePackage) -> Unit
) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Carte hors connexion",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.testTag("download_area_title")
    )

    if (offlinePackage == null) {
        Text(text = "Aucun paquet préparé", style = MaterialTheme.typography.bodySmall)
        Text(
            text = "Niveaux de zoom : 10 à 16",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("download_zoom_selector")
        )
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().testTag("download_area_button")
        ) { Text("Préparer la zone") }
        return
    }

    Text(offlinePackage.name, style = MaterialTheme.typography.bodySmall)
    Text(
        text = offlineStatusLabel(offlinePackage.status),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag("download_progress")
    )
    Text(
        text = "${offlinePackage.totalTiles} tuiles estimées, zooms ${offlinePackage.minZoom}-${offlinePackage.maxZoom}",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag("download_estimate")
    )

    offlinePackage.lastError?.takeIf { it.isNotBlank() }?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("download_error")
        )
    }

    if (offlinePackage.totalTiles > 0L) {
        val progress = (offlinePackage.downloadedTiles.toFloat() /
            offlinePackage.totalTiles.toFloat()).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().testTag("download_progress_bar")
        )
    }

    when (offlinePackage.status) {
        OfflinePackageStatus.CREE,
        OfflinePackageStatus.ECHEC,
        OfflinePackageStatus.ANNULE -> Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().testTag("download_confirm_button")
        ) {
            Text(if (offlinePackage.status == OfflinePackageStatus.ECHEC) "Réessayer" else "Télécharger")
        }

        OfflinePackageStatus.EN_ATTENTE -> Button(
            onClick = { onCancel(offlinePackage) },
            modifier = Modifier.fillMaxWidth().testTag("download_cancel_button")
        ) { Text("Annuler la préparation") }

        OfflinePackageStatus.TELECHARGEMENT_EN_COURS -> {
            Button(
                onClick = { onPause(offlinePackage) },
                modifier = Modifier.fillMaxWidth().testTag("download_pause_button")
            ) { Text("Pause") }
            Button(
                onClick = { onCancel(offlinePackage) },
                modifier = Modifier.fillMaxWidth().testTag("download_cancel_button")
            ) { Text("Annuler") }
        }

        OfflinePackageStatus.PAUSE -> {
            Button(
                onClick = { onResume(offlinePackage) },
                modifier = Modifier.fillMaxWidth().testTag("download_retry_button")
            ) { Text("Reprendre") }
            Button(
                onClick = { onCancel(offlinePackage) },
                modifier = Modifier.fillMaxWidth().testTag("download_cancel_button")
            ) { Text("Annuler") }
        }

        OfflinePackageStatus.TERMINE -> Text(
            text = "Disponible hors connexion",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("offline_package_ready")
        )

        OfflinePackageStatus.CORROMPU -> Text(
            text = "Paquet corrompu, suppression nécessaire",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun offlineStatusLabel(status: OfflinePackageStatus): String = when (status) {
    OfflinePackageStatus.CREE -> "Créé"
    OfflinePackageStatus.EN_ATTENTE -> "En attente"
    OfflinePackageStatus.TELECHARGEMENT_EN_COURS -> "Téléchargement en cours"
    OfflinePackageStatus.PAUSE -> "En pause"
    OfflinePackageStatus.TERMINE -> "Terminé"
    OfflinePackageStatus.ANNULE -> "Annulé"
    OfflinePackageStatus.ECHEC -> "Échec"
    OfflinePackageStatus.CORROMPU -> "Corrompu"
}

@Composable
private fun ZoneSelectionCanvas(
    containerWidth: Float,
    containerHeight: Float,
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(containerWidth, containerHeight) {
        if (containerWidth <= 0f || containerHeight <= 0f) return@LaunchedEffect
        if (viewModel.selectionRect == null) {
            viewModel.selectionRect = defaultSelectionRect(containerWidth, containerHeight)
        }
    }

    val selectionRect = viewModel.selectionRect
        ?: defaultSelectionRect(containerWidth, containerHeight)

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("selection_overlay")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val primaryColor = Color(0xFF1E88E5)
            val fillColor = primaryColor.copy(alpha = 0.20f)
            val strokeWidthPx = 3.dp.toPx()

            drawRect(
                color = fillColor,
                topLeft = Offset(selectionRect.left, selectionRect.top),
                size = Size(selectionRect.width, selectionRect.height)
            )

            drawRect(
                color = primaryColor,
                topLeft = Offset(selectionRect.left, selectionRect.top),
                size = Size(selectionRect.width, selectionRect.height),
                style = Stroke(width = strokeWidthPx)
            )

            fnDrawHandle(Offset(selectionRect.left, selectionRect.top))
            fnDrawHandle(Offset(selectionRect.right, selectionRect.top))
            fnDrawHandle(Offset(selectionRect.left, selectionRect.bottom))
            fnDrawHandle(Offset(selectionRect.right, selectionRect.bottom))
        }
    }
}

private fun defaultSelectionRect(containerWidth: Float, containerHeight: Float): Rect {
    val marginX = containerWidth * 0.15f
    val marginY = containerHeight * 0.25f
    return Rect(
        left = marginX,
        top = marginY,
        right = containerWidth - marginX,
        bottom = containerHeight - marginY
    )
}

@Composable
private fun SelectionActionBar(
    containerWidth: Float,
    containerHeight: Float,
    viewModel: MapViewModel,
    projector: ScreenCoordinateProjector?,
    onSelectionValidated: (GeoBounds) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sélection de zone",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("selection_cancel_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Annuler la sélection",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = {
                    val selectedRect = viewModel.selectionRect
                    if (selectedRect != null && projector != null) {
                        val bounds = selectedRect.toGeoBounds(projector)
                        viewModel.updateSelectedGeoBounds(bounds)
                        onSelectionValidated(bounds)
                    }

                    val marginX = 24f
                    val marginTop = 160f
                    val marginBottom = 190f
                    viewModel.selectionRect = Rect(
                        marginX,
                        marginTop,
                        containerWidth - marginX,
                        containerHeight - marginBottom
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32)
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("selection_validate_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Valider la sélection",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun SelectionHandleLayer(
    containerWidth: Float,
    containerHeight: Float,
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val minSize = with(density) { 80.dp.toPx() }
    val containerSize = Size(containerWidth, containerHeight)
    val selectionRect = viewModel.selectionRect
        ?: defaultSelectionRect(containerWidth, containerHeight)

    Box(modifier = modifier) {
        SelectionHandleZone(
            tag = "selection_handle_top_left",
            position = Offset(selectionRect.left, selectionRect.top),
            containerSize = containerSize,
            minSize = minSize,
            rect = selectionRect,
            onRectChange = { viewModel.selectionRect = it },
            resizeFunction = ::resizeTopLeft
        )

        SelectionHandleZone(
            tag = "selection_handle_top_right",
            position = Offset(selectionRect.right, selectionRect.top),
            containerSize = containerSize,
            minSize = minSize,
            rect = selectionRect,
            onRectChange = { viewModel.selectionRect = it },
            resizeFunction = ::resizeTopRight
        )

        SelectionHandleZone(
            tag = "selection_handle_bottom_left",
            position = Offset(selectionRect.left, selectionRect.bottom),
            containerSize = containerSize,
            minSize = minSize,
            rect = selectionRect,
            onRectChange = { viewModel.selectionRect = it },
            resizeFunction = ::resizeBottomLeft
        )

        SelectionHandleZone(
            tag = "selection_handle_bottom_right",
            position = Offset(selectionRect.right, selectionRect.bottom),
            containerSize = containerSize,
            minSize = minSize,
            rect = selectionRect,
            onRectChange = { viewModel.selectionRect = it },
            resizeFunction = ::resizeBottomRight
        )
    }
}

@Composable
private fun SelectionHandleZone(
    tag: String,
    position: Offset,
    containerSize: Size,
    minSize: Float,
    rect: Rect,
    onRectChange: (Rect) -> Unit,
    resizeFunction: (Rect, Offset, Size, Float) -> Rect
) {
    val currentRect by rememberUpdatedState(rect)
    val currentOnRectChange by rememberUpdatedState(onRectChange)
    val handleSizePx = with(LocalDensity.current) { 48.dp.toPx() }

    Box(
        modifier = Modifier
            .offset {
                androidx.compose.ui.unit.IntOffset(
                    (position.x - handleSizePx / 2f).roundToInt(),
                    (position.y - handleSizePx / 2f).roundToInt()
                )
            }
            .size(48.dp)
            .zIndex(30f)
            .testTag(tag)
            .pointerInput(resizeFunction, containerSize, minSize) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPoint = down.position
                    val startRect = currentRect
                    down.consume()
                    try {
                        drag(down.id) { change ->
                            val delta = change.position - startPoint
                            val updated = resizeFunction(
                                startRect,
                                delta,
                                containerSize,
                                minSize
                            )
                            if (updated != startRect) {
                                change.consume()
                                currentOnRectChange(updated)
                            }
                        }
                    } finally {
                        // Gesture completed or was cancelled.
                    }
                }
            }
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.fnDrawHandle(center: Offset) {
    val handleRadiusPx = 10.dp.toPx()
    drawCircle(color = Color(0xFF1E88E5), radius = handleRadiusPx, center = center)
    drawCircle(color = Color.White, radius = handleRadiusPx * 0.65f, center = center)
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

const val SELECTED_AREA_SOURCE_ID = "selected-area-source"
const val SELECTED_AREA_LAYER_ID = "selected-area-layer"

private fun GeoBounds.toPolygonGeoJson(): String =
    """{"type":"Feature","geometry":{"type":"Polygon","coordinates":[[[$west,$south],[$east,$south],[$east,$north],[$west,$north],[$west,$south]]]}}"""

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