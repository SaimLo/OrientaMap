package com.showroomprive.orientamap.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat

class LocationTracker(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onLocation?.invoke(location)
        }
    }

    private var onLocation: ((Location) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun start(onLocation: (Location) -> Unit) {
        if (!hasAnyLocationPermission()) return
        this.onLocation = onLocation
        if (hasFineLocationPermission()) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2_000L,
                2f,
                listener
            )
        }
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            5_000L,
            5f,
            listener
        )
    }

    fun stop() {
        locationManager.removeUpdates(listener)
        onLocation = null
    }

    private fun hasAnyLocationPermission(): Boolean =
        hasFineLocationPermission() || ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasFineLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}
