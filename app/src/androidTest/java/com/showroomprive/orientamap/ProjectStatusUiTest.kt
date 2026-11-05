package com.showroomprive.orientamap

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.showroomprive.orientamap.domain.model.ProjectStatus
import com.showroomprive.orientamap.presentation.map.FakeLocationProvider
import com.showroomprive.orientamap.presentation.map.FakeMapController
import com.showroomprive.orientamap.presentation.map.MapScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectStatusUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun definedAreaDisplaysFrenchStatusAndLocksAreaSelection() {
        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet défini",
                onBack = {},
                locationProvider = FakeLocationProvider(),
                mapController = FakeMapController(),
                projectStatus = ProjectStatus.ZONE_DEFINIE,
                mapContent = { _ ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("map_view")
                    )
                }
            )
        }

        composeTestRule.onNodeWithTag("project_status")
            .assertIsDisplayed()
            .assertTextEquals("Zone définie")
        composeTestRule.onNodeWithTag("define_area_button")
            .assertDoesNotExist()
    }
}
