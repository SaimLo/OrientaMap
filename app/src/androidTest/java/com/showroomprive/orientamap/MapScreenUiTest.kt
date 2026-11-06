package com.showroomprive.orientamap

import android.location.Location
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.showroomprive.orientamap.presentation.map.FakeLocationProvider
import com.showroomprive.orientamap.presentation.map.FakeMapController
import com.showroomprive.orientamap.presentation.map.MapScreen
import com.showroomprive.orientamap.presentation.map.MapViewModel
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

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("map_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("map_view").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gps_status").assertIsDisplayed().assertTextEquals("GPS actif")
        composeTestRule.onNodeWithText("Projet Test").assertIsDisplayed()
        composeTestRule.onNodeWithTag("offline_mode_indicator").assertIsDisplayed()
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

        composeTestRule.waitForIdle()
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

        composeTestRule.waitForIdle()
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

        composeTestRule.waitForIdle()
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

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Fermer").performClick()
        composeTestRule.runOnIdle {
            assertTrue(backClicked)
        }
    }

    @Test
    fun testMapPanningThroughCenterDoesNotMoveRect() {
        val fakeLocationProvider = FakeLocationProvider()
        val fakeMapController = FakeMapController()
        val viewModel = MapViewModel().apply {
            isSelectionModeActive = true
            selectionRect = Rect(200f, 200f, 600f, 500f)
        }

        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet Test",
                onBack = {},
                locationProvider = fakeLocationProvider,
                mapController = fakeMapController,
                viewModel = viewModel,
                mapContent = { _ ->
                    Box(modifier = Modifier.fillMaxSize().testTag("map_view"))
                }
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("selection_overlay").assertIsDisplayed()

        val initialRect = viewModel.selectionRect!!

        composeTestRule.onNodeWithTag("selection_overlay").performTouchInput {
            down(Offset(400f, 350f))
            moveBy(Offset(100f, 100f))
            up()
        }

        composeTestRule.runOnIdle {
            assertEquals(initialRect, viewModel.selectionRect)
        }
    }

    @Test
    fun testZoneSelectionAllHandlesDragging() {
        val fakeLocationProvider = FakeLocationProvider()
        val fakeMapController = FakeMapController()
        val viewModel = MapViewModel().apply {
            isSelectionModeActive = true
            selectionRect = Rect(200f, 200f, 600f, 500f)
        }

        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet Test",
                onBack = {},
                locationProvider = fakeLocationProvider,
                mapController = fakeMapController,
                viewModel = viewModel,
                mapContent = { _ ->
                    Box(modifier = Modifier.fillMaxSize().testTag("map_view"))
                }
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("selection_overlay").assertIsDisplayed()

        // 1. Top-Left Handle
        val r1 = viewModel.selectionRect!!
        composeTestRule.onNodeWithTag("selection_handle_top_left").performTouchInput {
            down(center)
            moveBy(Offset(30f, 30f))
            up()
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            val updated = viewModel.selectionRect!!
            assertEquals(r1.left + 30f, updated.left, 1f)
            assertEquals(r1.top + 30f, updated.top, 1f)
            assertEquals(r1.right, updated.right, 1f)
            assertEquals(r1.bottom, updated.bottom, 1f)
        }

        // 2. Top-Right Handle
        val r2 = viewModel.selectionRect!!
        composeTestRule.onNodeWithTag("selection_handle_top_right").performTouchInput {
            down(center)
            moveBy(Offset(30f, 30f))
            up()
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            val updated = viewModel.selectionRect!!
            assertEquals(r2.left, updated.left, 1f)
            assertEquals(r2.top + 30f, updated.top, 1f)
            assertEquals(r2.right + 30f, updated.right, 1f)
            assertEquals(r2.bottom, updated.bottom, 1f)
        }

        // 3. Bottom-Left Handle
        val r3 = viewModel.selectionRect!!
        composeTestRule.onNodeWithTag("selection_handle_bottom_left").performTouchInput {
            down(center)
            moveBy(Offset(30f, 30f))
            up()
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            val updated = viewModel.selectionRect!!
            assertEquals(r3.left + 30f, updated.left, 1f)
            assertEquals(r3.top, updated.top, 1f)
            assertEquals(r3.right, updated.right, 1f)
            assertEquals(r3.bottom + 30f, updated.bottom, 1f)
        }

        // 4. Bottom-Right Handle
        val r4 = viewModel.selectionRect!!
        composeTestRule.onNodeWithTag("selection_handle_bottom_right").performTouchInput {
            down(center)
            moveBy(Offset(30f, 30f))
            up()
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            val updated = viewModel.selectionRect!!
            assertEquals(r4.left, updated.left, 1f)
            assertEquals(r4.top, updated.top, 1f)
            assertEquals(r4.right + 30f, updated.right, 1f)
            assertEquals(r4.bottom + 30f, updated.bottom, 1f)
        }
    }

    @Test
    fun testOutsideClicksPassThroughToMap() {
        val fakeLocationProvider = FakeLocationProvider()
        val fakeMapController = FakeMapController()
        val viewModel = MapViewModel().apply {
            isSelectionModeActive = true
            selectionRect = Rect(200f, 200f, 600f, 500f)
        }

        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet Test",
                onBack = {},
                locationProvider = fakeLocationProvider,
                mapController = fakeMapController,
                viewModel = viewModel,
                mapContent = { _ ->
                    Box(modifier = Modifier.fillMaxSize().testTag("map_view"))
                }
            )
        }

        composeTestRule.waitForIdle()
        val initialRect = viewModel.selectionRect!!

        composeTestRule.onNodeWithTag("selection_overlay").performTouchInput {
            down(Offset(50f, 50f))
            moveBy(Offset(50f, 50f))
            up()
        }

        composeTestRule.runOnIdle {
            assertEquals(initialRect, viewModel.selectionRect)
        }
    }

    @Test
    fun testCancelAndValidateButtons() {
        val fakeLocationProvider = FakeLocationProvider()
        val fakeMapController = FakeMapController()
        val viewModel = MapViewModel().apply {
            isSelectionModeActive = true
            selectionRect = Rect(200f, 200f, 600f, 500f)
        }

        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet Test",
                onBack = {},
                locationProvider = fakeLocationProvider,
                mapController = fakeMapController,
                viewModel = viewModel,
                mapContent = { _ ->
                    Box(modifier = Modifier.fillMaxSize().testTag("map_view"))
                }
            )
        }

        composeTestRule.waitForIdle()
        assertTrue(viewModel.isSelectionModeActive)

        composeTestRule.onNodeWithTag("selection_cancel_button").performClick()
        composeTestRule.runOnIdle {
            assertEquals(false, viewModel.isSelectionModeActive)
        }

        viewModel.isSelectionModeActive = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("selection_validate_button").performClick()
        composeTestRule.runOnIdle {
            assertTrue(viewModel.selectionRect != null)
        }
    }

    @Test
    fun testNoWidthHeightAreaInfoDisplayed() {
        val fakeLocationProvider = FakeLocationProvider()
        val fakeMapController = FakeMapController()
        val viewModel = MapViewModel().apply {
            isSelectionModeActive = true
            selectionRect = Rect(200f, 200f, 600f, 500f)
        }

        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet Test",
                onBack = {},
                locationProvider = fakeLocationProvider,
                mapController = fakeMapController,
                viewModel = viewModel,
                mapContent = { _ ->
                    Box(modifier = Modifier.fillMaxSize().testTag("map_view"))
                }
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Largeur", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Hauteur", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Surface", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("m²", substring = true).assertDoesNotExist()
    }
}
