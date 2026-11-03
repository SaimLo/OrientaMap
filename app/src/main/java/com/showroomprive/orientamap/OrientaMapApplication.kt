package com.showroomprive.orientamap

import android.app.Application
import com.showroomprive.orientamap.data.AppContainer
import org.maplibre.android.MapLibre

class OrientaMapApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        container = AppContainer(this)
    }
}
