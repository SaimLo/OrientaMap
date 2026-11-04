package com.showroomprive.orientamap

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.showroomprive.orientamap.presentation.map.FakeLocationProvider
import com.showroomprive.orientamap.presentation.map.FakeMapController
import com.showroomprive.orientamap.presentation.map.MapScreen
import com.showroomprive.orientamap.presentation.map.MapViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectionGestureUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tapOutsideDoesNotMoveSelection() {
        val viewModel = MapViewModel().apply { isSelectionModeActive = true }
        setContent(viewModel)
        composeTestRule.waitForIdle()

        val initial = viewModel.selectionRect
        composeTestRule.onNodeWithTag("selection_overlay").performTouchInput {
            val outside = Offset(50f, 50f)
            down(outside)
            moveBy(Offset(-40f, 20f))
            up()
        }

        composeTestRule.runOnIdle {
            assertEquals(initial, viewModel.selectionRect)
        }
    }

    @Test
    fun draggingBodyDoesNotMoveSelection() {
        val viewModel = MapViewModel().apply { isSelectionModeActive = true }
        setContent(viewModel)
        composeTestRule.waitForIdle()

        val initial = requireNotNull(viewModel.selectionRect)
        val centerX = (initial.left + initial.right) / 2f
        val centerY = (initial.top + initial.bottom) / 2f

        composeTestRule.onNodeWithTag("selection_overlay").performTouchInput {
            down(Offset(centerX, centerY))
            moveBy(Offset(20f, 10f))
            up()
        }

        composeTestRule.runOnIdle {
            val updated = requireNotNull(viewModel.selectionRect)
            assertEquals(initial, updated)
        }
    }

    private fun setContent(viewModel: MapViewModel) {
        composeTestRule.setContent {
            MapScreen(
                projectTitle = "Selection test",
                onBack = {},
                locationProvider = FakeLocationProvider(),
                mapController = FakeMapController(),
                mapContent = { _ ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("map_view")
                    )
                },
                viewModel = viewModel
            )
        }
    }
}
