package com.showroomprive.orientamap

import android.location.Location
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.showroomprive.orientamap.presentation.map.FakeLocationProvider
import com.showroomprive.orientamap.presentation.map.FakeMapController
import com.showroomprive.orientamap.presentation.map.MapScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapScreenUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun testMapScreenInitialDisplayAndTags() {
        val fakeLocationProvider = FakeLocationProvider()
        val fakeMapController = FakeMapController()

        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet Test",
                onBack = {},
                locationProvider = fakeLocationProvider,
                mapController = fakeMapController,
                mapContent = { _ ->
                    Box(modifier = Modifier.fillMaxSize().testTag("map_view"))
                }
            )
        }

        composeTestRule.onNodeWithTag("map_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("map_view").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gps_status").assertIsDisplayed().assertTextEquals("GPS actif")
        composeTestRule.onNodeWithText("Projet Test").assertIsDisplayed()

        // Placeholder UI components for offline downloading are displayed
        composeTestRule.onNodeWithTag("offline_mode_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("download_area_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cancel_download_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("retry_download_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("download_progress").assertIsDisplayed()
    }

    @Test
    fun testGpsLocationUpdateAndMapController() {
        val fakeLocationProvider = FakeLocationProvider()
        val fakeMapController = FakeMapController()

        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet Test",
                onBack = {},
                locationProvider = fakeLocationProvider,
                mapController = fakeMapController,
                mapContent = { _ ->
                    Box(modifier = Modifier.fillMaxSize().testTag("map_view"))
                }
            )
        }

        val location1 = Location("gps").apply {
            latitude = 48.8566
            longitude = 2.3522
            accuracy = 5.0f
        }

        fakeLocationProvider.emitLocation(location1)

        composeTestRule.runOnIdle {
            assertEquals(1, fakeMapController.updatedUserLocations.size)
            assertEquals(48.8566, fakeMapController.updatedUserLocations[0].latitude, 0.0001)
            assertEquals(2.3522, fakeMapController.updatedUserLocations[0].longitude, 0.0001)

            assertEquals(1, fakeMapController.movedCameraLocations.size)
            assertEquals(48.8566, fakeMapController.movedCameraLocations[0].latitude, 0.0001)
            assertEquals(2.3522, fakeMapController.movedCameraLocations[0].longitude, 0.0001)
        }

        composeTestRule.onNodeWithTag("location_coordinates").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_accuracy").assertIsDisplayed()
        composeTestRule.onNodeWithText("48.85660, 2.35220").assertIsDisplayed()
        composeTestRule.onNodeWithText("Précision : 5.0 m").assertIsDisplayed()
    }

    @Test
    fun testInitialCameraCenteringOnlyOnce() {
        val fakeLocationProvider = FakeLocationProvider()
        val fakeMapController = FakeMapController()

        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet Test",
                onBack = {},
                locationProvider = fakeLocationProvider,
                mapController = fakeMapController,
                mapContent = { _ ->
                    Box(modifier = Modifier.fillMaxSize().testTag("map_view"))
                }
            )
        }

        val location1 = Location("gps").apply {
            latitude = 48.8566
            longitude = 2.3522
            accuracy = 5.0f
        }
        val location2 = Location("gps").apply {
            latitude = 48.8570
            longitude = 2.3530
            accuracy = 3.0f
        }

        fakeLocationProvider.emitLocation(location1)
        composeTestRule.runOnIdle {
            assertEquals(1, fakeMapController.updatedUserLocations.size)
            assertEquals(1, fakeMapController.movedCameraLocations.size)
        }

        fakeLocationProvider.emitLocation(location2)
        composeTestRule.runOnIdle {
            assertEquals(2, fakeMapController.updatedUserLocations.size)
            // Camera move must still be 1 (initial centering only once)
            assertEquals(1, fakeMapController.movedCameraLocations.size)
        }
    }

    @Test
    fun testPermissionDeniedStatusDisplay() {
        val fakeLocationProvider = FakeLocationProvider()
        val fakeMapController = FakeMapController()

        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet Perm Denied",
                onBack = {},
                locationProvider = fakeLocationProvider,
                mapController = fakeMapController,
                hasLocationPermission = false,
                mapContent = { _ ->
                    Box(modifier = Modifier.fillMaxSize().testTag("map_view"))
                }
            )
        }

        composeTestRule.onNodeWithTag("gps_status").assertIsDisplayed().assertTextEquals("Permission GPS requise")
    }

    @Test
    fun testOnBackCallback() {
        var backClicked = false
        val fakeLocationProvider = FakeLocationProvider()
        val fakeMapController = FakeMapController()

        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet Test",
                onBack = { backClicked = true },
                locationProvider = fakeLocationProvider,
                mapController = fakeMapController,
                mapContent = { _ ->
                    Box(modifier = Modifier.fillMaxSize().testTag("map_view"))
                }
            )
        }

        composeTestRule.onNodeWithContentDescription("Fermer").performClick()
        composeTestRule.runOnIdle {
            assertTrue(backClicked)
        }
    }
}
