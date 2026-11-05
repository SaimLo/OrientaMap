package com.showroomprive.orientamap

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.ProjectStatus
import com.showroomprive.orientamap.presentation.map.FakeLocationProvider
import com.showroomprive.orientamap.presentation.map.FakeMapController
import com.showroomprive.orientamap.presentation.map.MapScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflinePackageUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun definedProjectOffersOfflinePackagePreparation() {
        var clicked = false
        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet défini",
                onBack = {},
                locationProvider = FakeLocationProvider(),
                mapController = FakeMapController(),
                projectStatus = ProjectStatus.ZONE_DEFINIE,
                projectBounds = GeoBounds(48.9, 48.7, 2.5, 2.1),
                onStartOfflineDownload = { clicked = true },
                mapContent = { _ ->
                    Box(modifier = Modifier.fillMaxSize())
                }
            )
        }

        composeTestRule.onNodeWithTag("download_zoom_selector")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("download_area_button")
            .assertIsDisplayed()
            .performClick()

        assertTrue(clicked)
    }
}
