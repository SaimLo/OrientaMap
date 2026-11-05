package com.showroomprive.orientamap

import android.location.Location
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.ProjectStatus
import com.showroomprive.orientamap.presentation.map.FakeLocationProvider
import com.showroomprive.orientamap.presentation.map.MapController
import com.showroomprive.orientamap.presentation.map.MapScreen
import com.showroomprive.orientamap.presentation.map.ScreenCoordinateProjector
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectBoundsUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun definedProjectRestoresSavedBoundsAndCamera() {
        val controller = RecordingMapController()
        val bounds = GeoBounds(
            north = 48.9,
            south = 48.7,
            east = 2.5,
            west = 2.1
        )

        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet défini",
                onBack = {},
                locationProvider = FakeLocationProvider(),
                mapController = controller,
                projectStatus = ProjectStatus.ZONE_DEFINIE,
                projectBounds = bounds,
                mapContent = { _ ->
                    Box(modifier = Modifier.fillMaxSize())
                }
            )
        }

        composeTestRule.waitForIdle()

        assertEquals(bounds, controller.shownBounds)
        assertEquals(bounds, controller.cameraBounds)
    }

    private class RecordingMapController : MapController {
        var shownBounds: GeoBounds? = null
        var cameraBounds: GeoBounds? = null

        override fun moveCameraTo(location: Location) = Unit

        override fun updateUserLocation(location: Location) = Unit

        override fun screenCoordinateProjector(): ScreenCoordinateProjector? = null

        override fun moveCameraToBounds(bounds: GeoBounds) {
            cameraBounds = bounds
        }

        override fun showSelectedArea(bounds: GeoBounds) {
            shownBounds = bounds
        }
    }
}
