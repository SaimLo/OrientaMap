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
import com.showroomprive.orientamap.domain.model.OfflinePackage
import com.showroomprive.orientamap.domain.model.OfflinePackageStatus
import com.showroomprive.orientamap.domain.model.ProjectStatus
import com.showroomprive.orientamap.presentation.map.FakeLocationProvider
import com.showroomprive.orientamap.presentation.map.FakeMapController
import com.showroomprive.orientamap.presentation.map.MapScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

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
    @Test
    fun definedProjectOffersPdfExport() {
        var clicked = false
        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet imprimable",
                onBack = {},
                locationProvider = FakeLocationProvider(),
                mapController = FakeMapController(),
                projectStatus = ProjectStatus.ZONE_DEFINIE,
                projectBounds = GeoBounds(48.9, 48.7, 2.5, 2.1),
                onExportPdf = { clicked = true },
                mapContent = { _ ->
                    Box(modifier = Modifier.fillMaxSize())
                }
            )
        }

        composeTestRule.onNodeWithTag("export_pdf_button")
            .assertIsDisplayed()
            .performClick()

        assertTrue(clicked)
    }
    @Test
    fun activePackageOffersPauseAndCancel() {
        var paused = false
        var cancelled = false
        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet offline",
                onBack = {},
                locationProvider = FakeLocationProvider(),
                mapController = FakeMapController(),
                projectStatus = ProjectStatus.ZONE_DEFINIE,
                projectBounds = GeoBounds(48.9, 48.7, 2.5, 2.1),
                offlinePackages = listOf(packageFixture(OfflinePackageStatus.TELECHARGEMENT_EN_COURS)),
                onPauseOfflineDownload = { paused = true },
                onCancelOfflineDownload = { cancelled = true },
                mapContent = { _ -> Box(modifier = Modifier.fillMaxSize()) }
            )
        }

        composeTestRule.onNodeWithTag("download_pause_button")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithTag("download_cancel_button")
            .assertIsDisplayed()
            .performClick()

        assertTrue(paused)
        assertTrue(cancelled)
    }

    @Test
    fun pausedPackageOffersResumeAndCancel() {
        var resumed = false
        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet offline",
                onBack = {},
                locationProvider = FakeLocationProvider(),
                mapController = FakeMapController(),
                projectStatus = ProjectStatus.ZONE_DEFINIE,
                projectBounds = GeoBounds(48.9, 48.7, 2.5, 2.1),
                offlinePackages = listOf(packageFixture(OfflinePackageStatus.PAUSE)),
                onResumeOfflineDownload = { resumed = true },
                mapContent = { _ -> Box(modifier = Modifier.fillMaxSize()) }
            )
        }

        composeTestRule.onNodeWithTag("download_retry_button")
            .assertIsDisplayed()
            .performClick()

        assertTrue(resumed)
    }

    @Test
    fun completedPackageShowsOfflineReadyState() {
        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Projet offline",
                onBack = {},
                locationProvider = FakeLocationProvider(),
                mapController = FakeMapController(),
                projectStatus = ProjectStatus.ZONE_DEFINIE,
                projectBounds = GeoBounds(48.9, 48.7, 2.5, 2.1),
                offlinePackages = listOf(packageFixture(OfflinePackageStatus.TERMINE)),
                mapContent = { _ -> Box(modifier = Modifier.fillMaxSize()) }
            )
        }

        composeTestRule.onNodeWithTag("offline_package_ready")
            .assertIsDisplayed()
    }

    private fun packageFixture(status: OfflinePackageStatus) = OfflinePackage(
        id = UUID.randomUUID(),
        projectId = UUID.randomUUID(),
        name = "Plan IGN",
        bounds = GeoBounds(48.9, 48.7, 2.5, 2.1),
        minZoom = 10,
        maxZoom = 16,
        sourceId = "GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2",
        attribution = "© IGN",
        status = status,
        downloadedTiles = if (status == OfflinePackageStatus.TERMINE) 10 else 4,
        totalTiles = 10,
        estimatedSizeBytes = 5_000L,
        localPath = null,
        createdAtEpochMillis = 1L,
        completedAtEpochMillis = null
    )
}
