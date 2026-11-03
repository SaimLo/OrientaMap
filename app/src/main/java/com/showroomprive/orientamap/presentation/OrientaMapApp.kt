package com.showroomprive.orientamap.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.showroomprive.orientamap.presentation.map.MapScreen
import com.showroomprive.orientamap.presentation.projects.ProjectListScreen

@Composable
fun OrientaMapApp(viewModel: ProjectsViewModel) {
    val navController = rememberNavController()
    val projects by viewModel.projects.collectAsStateWithLifecycle()

    Scaffold { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "projects",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("projects") {
                ProjectListScreen(
                    projects = projects,
                    onCreateProject = viewModel::createSampleProject,
                    onOpenProject = { project ->
                        navController.navigate("map/${project.id}")
                    },
                    onRenameProject = viewModel::renameProject,
                    onDeleteProject = viewModel::deleteProject
                )
            }
            composable("map/{projectId}") { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId")
                val project = projects.find { it.id.toString() == projectId }
                MapScreen(
                    projectTitle = project?.name ?: "Projet ${projectId ?: "inconnu"}",
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
