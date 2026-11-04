package com.showroomprive.orientamap

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.showroomprive.orientamap.domain.model.MapProject
import com.showroomprive.orientamap.presentation.projects.ProjectListScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProjectListUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun testEmptyProjectList() {
        composeTestRule.setContent {
            ProjectListScreen(
                projects = emptyList(),
                onCreateProject = {},
                onOpenProject = {},
                onRenameProject = { _, _ -> },
                onDeleteProject = {}
            )
        }

        composeTestRule.onNodeWithTag("create_project_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aucun projet. Crée ton premier projet pour commencer.").assertIsDisplayed()
    }

    @Test
    fun testDisplaySingleProject() {
        val projectId = UUID.randomUUID()
        val project = MapProject(
            id = projectId,
            name = "Projet Test",
            description = "Description test",
            createdAtEpochMillis = 0L,
            updatedAtEpochMillis = 0L
        )

        composeTestRule.setContent {
            ProjectListScreen(
                projects = listOf(project),
                onCreateProject = {},
                onOpenProject = {},
                onRenameProject = { _, _ -> },
                onDeleteProject = {}
            )
        }

        composeTestRule.onNodeWithTag("project_card_${project.id}").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_project_button_${project.id}").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_project_button_${project.id}").assertIsDisplayed()
        composeTestRule.onNodeWithText("Projet Test").assertIsDisplayed()
        composeTestRule.onNodeWithText("Description test").assertIsDisplayed()
    }

    @Test
    fun testCreateProject() {
        var createCalled = false

        composeTestRule.setContent {
            ProjectListScreen(
                projects = emptyList(),
                onCreateProject = { createCalled = true },
                onOpenProject = {},
                onRenameProject = { _, _ -> },
                onDeleteProject = {}
            )
        }

        composeTestRule.onNodeWithTag("create_project_button").performClick()
        composeTestRule.runOnIdle {
            assertTrue(createCalled)
        }
    }

    @Test
    fun testOpenProject() {
        val projectId = UUID.randomUUID()
        val project = MapProject(
            id = projectId,
            name = "Projet Test",
            description = "Description test",
            createdAtEpochMillis = 0L,
            updatedAtEpochMillis = 0L
        )
        var openedProject: MapProject? = null

        composeTestRule.setContent {
            ProjectListScreen(
                projects = listOf(project),
                onCreateProject = {},
                onOpenProject = { openedProject = it },
                onRenameProject = { _, _ -> },
                onDeleteProject = {}
            )
        }

        composeTestRule.onNodeWithTag("open_project_button_${project.id}").performClick()
        composeTestRule.runOnIdle {
            assertEquals(project, openedProject)
        }
    }

    @Test
    fun testDeleteProject() {
        val projectId = UUID.randomUUID()
        val project = MapProject(
            id = projectId,
            name = "Projet A Supprimer",
            description = null,
            createdAtEpochMillis = 0L,
            updatedAtEpochMillis = 0L
        )
        var deletedProject: MapProject? = null

        composeTestRule.setContent {
            ProjectListScreen(
                projects = listOf(project),
                onCreateProject = {},
                onOpenProject = {},
                onRenameProject = { _, _ -> },
                onDeleteProject = { deletedProject = it }
            )
        }

        composeTestRule.onNodeWithTag("delete_project_button_${project.id}").performClick()
        composeTestRule.runOnIdle {
            assertEquals(project, deletedProject)
        }
    }

    @Test
    fun testRenameProject() {
        val projectId = UUID.randomUUID()
        val project = MapProject(
            id = projectId,
            name = "Ancien Nom",
            description = null,
            createdAtEpochMillis = 0L,
            updatedAtEpochMillis = 0L
        )
        var renamedProject: MapProject? = null
        var updatedName: String? = null

        composeTestRule.setContent {
            ProjectListScreen(
                projects = listOf(project),
                onCreateProject = {},
                onOpenProject = {},
                onRenameProject = { p, newName ->
                    renamedProject = p
                    updatedName = newName
                },
                onDeleteProject = {}
            )
        }

        composeTestRule.onNodeWithTag("rename_project_button_${project.id}").performClick()
        composeTestRule.onNodeWithTag("rename_input").performTextClearance()
        composeTestRule.onNodeWithTag("rename_input").performTextInput("Nouveau Nom")
        composeTestRule.onNodeWithTag("rename_confirm_button").performClick()

        composeTestRule.runOnIdle {
            assertEquals(project, renamedProject)
            assertEquals("Nouveau Nom", updatedName)
        }
    }

    @Test
    fun testMultipleProjects() {
        val project1 = MapProject(
            id = UUID.randomUUID(),
            name = "Projet 1",
            description = "Desc 1",
            createdAtEpochMillis = 0L,
            updatedAtEpochMillis = 0L
        )
        val project2 = MapProject(
            id = UUID.randomUUID(),
            name = "Projet 2",
            description = "Desc 2",
            createdAtEpochMillis = 0L,
            updatedAtEpochMillis = 0L
        )

        composeTestRule.setContent {
            ProjectListScreen(
                projects = listOf(project1, project2),
                onCreateProject = {},
                onOpenProject = {},
                onRenameProject = { _, _ -> },
                onDeleteProject = {}
            )
        }

        composeTestRule.onNodeWithTag("project_card_${project1.id}").assertIsDisplayed()
        composeTestRule.onNodeWithTag("project_card_${project2.id}").assertIsDisplayed()
        composeTestRule.onNodeWithText("Projet 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Projet 2").assertIsDisplayed()
    }

    @Test
    fun testMultipleProjectsNoTagAmbiguity() {
        val project1 = MapProject(
            id = UUID.randomUUID(),
            name = "Projet 1",
            description = "Desc 1",
            createdAtEpochMillis = 0L,
            updatedAtEpochMillis = 0L
        )
        val project2 = MapProject(
            id = UUID.randomUUID(),
            name = "Projet 2",
            description = "Desc 2",
            createdAtEpochMillis = 0L,
            updatedAtEpochMillis = 0L
        )
        var openedProject: MapProject? = null

        composeTestRule.setContent {
            ProjectListScreen(
                projects = listOf(project1, project2),
                onCreateProject = {},
                onOpenProject = { openedProject = it },
                onRenameProject = { _, _ -> },
                onDeleteProject = {}
            )
        }

        composeTestRule.onNodeWithTag("open_project_button_${project2.id}").performClick()
        composeTestRule.runOnIdle {
            assertEquals(project2, openedProject)
        }
    }
}
