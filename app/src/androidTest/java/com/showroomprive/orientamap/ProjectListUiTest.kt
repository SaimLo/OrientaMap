package com.showroomprive.orientamap

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.showroomprive.orientamap.domain.model.MapProject
import com.showroomprive.orientamap.presentation.projects.ProjectListScreen
import org.junit.Rule
import org.junit.Test

class ProjectListUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testEmptyProjectList() {
        composeTestRule.setContent {
            ProjectListScreen(
                projects = emptyList(),
                onCreateProject = {},
                onOpenProject = {},
                onRenameProject = {},
                onDeleteProject = {}
            )
        }

        composeTestRule.onNodeWithTag("create_project_button").assertIsDisplayed()
    }

    @Test
    fun testProjectListWithItems() {
        val project = MapProject(id = 1L, name = "Projet Test", description = "Description test")

        composeTestRule.setContent {
            ProjectListScreen(
                projects = listOf(project),
                onCreateProject = {},
                onOpenProject = {},
                onRenameProject = {},
                onDeleteProject = {}
            )
        }

        composeTestRule.onNodeWithTag("project_list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("project_card_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_project_button_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_project_button_1").assertIsDisplayed()
    }

    @Test
    fun testCreateProjectClick() {
        var clicked = false
        composeTestRule.setContent {
            ProjectListScreen(
                projects = emptyList(),
                onCreateProject = { clicked = true },
                onOpenProject = {},
                onRenameProject = {},
                onDeleteProject = {}
            )
        }

        composeTestRule.onNodeWithTag("create_project_button").performClick()
        assert(clicked)
    }

    @Test
    fun testDeleteProjectClick() {
        val project = MapProject(id = 2L, name = "Projet Suppr", description = null)
        var deletedProject: MapProject? = null

        composeTestRule.setContent {
            ProjectListScreen(
                projects = listOf(project),
                onCreateProject = {},
                onOpenProject = {},
                onRenameProject = {},
                onDeleteProject = { deletedProject = it }
            )
        }

        composeTestRule.onNodeWithTag("delete_project_button_2").performClick()
        assert(deletedProject == project)
    }
}
