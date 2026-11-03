package com.showroomprive.orientamap

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.showroomprive.orientamap.presentation.map.MapScreen
import org.junit.Rule
import org.junit.Test

class MapScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMapScreenComponents() {
        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet Carte Test",
                onBack = {}
            )
        }

        composeTestRule.onNodeWithTag("map_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gps_status").assertIsDisplayed()
        composeTestRule.onNodeWithTag("offline_mode_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("download_area_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("download_progress").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cancel_download_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("retry_download_button").assertIsDisplayed()
    }
}
