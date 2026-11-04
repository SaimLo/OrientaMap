package com.showroomprive.orientamap.presentation.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

@Composable
fun MapLibreView(
    modifier: Modifier = Modifier,
    onMapReady: (MapLibreMap) -> Unit,
    onCameraMove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(context).also { it.onCreate(null) }
    }
    val currentOnMapReady = rememberUpdatedState(onMapReady)
    val currentOnCameraMove = rememberUpdatedState(onCameraMove)

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = mapView.onStart()
            override fun onResume(owner: LifecycleOwner) = mapView.onResume()
            override fun onPause(owner: LifecycleOwner) = mapView.onPause()
            override fun onStop(owner: LifecycleOwner) = mapView.onStop()
            override fun onDestroy(owner: LifecycleOwner) = mapView.onDestroy()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(mapView) {
        mapView.getMapAsync { map ->
            map.addOnCameraMoveListener {
                currentOnCameraMove.value?.invoke()
            }
            map.setStyle(Style.Builder().fromUri("asset://styles/ign_plan.json")) {
                currentOnMapReady.value(map)
            }
        }
        onDispose { }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { }
    )
}
