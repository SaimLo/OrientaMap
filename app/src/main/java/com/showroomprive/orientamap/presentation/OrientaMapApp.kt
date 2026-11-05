package com.showroomprive.orientamap.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.showroomprive.orientamap.domain.model.OfflinePackage
import com.showroomprive.orientamap.domain.model.ProjectStatus
import com.showroomprive.orientamap.presentation.map.MapScreen
import com.showroomprive.orientamap.presentation.projects.ProjectListScreen

@Composable
fun OrientaMapApp(
    viewModel: ProjectsViewModel,
    offlinePackagesViewModel: OfflinePackagesViewModel? = null
) {
    val navController = rememberNavController()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val emptyPackages = remember { MutableStateFlow<List<OfflinePackage>>(emptyList()) }
    val packageFlow = offlinePackagesViewModel?.packages ?: emptyPackages
    val offlinePackages by packageFlow.collectAsStateWithLifecycle()

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
                LaunchedEffect(project?.id) {
                    offlinePackagesViewModel?.selectProject(project?.id)
                }
                MapScreen(
                    projectTitle = project?.name ?: "Projet ${projectId ?: "inconnu"}",
                    onBack = { navController.popBackStack() },
                    projectStatus = project?.status ?: ProjectStatus.BROUILLON,
                    projectBounds = project?.geoBounds,
                    offlinePackages = offlinePackages,
                    onStartOfflineDownload = {
                        project?.id?.let { offlinePackagesViewModel?.startDefaultDownload(it) }
                    },
                    onPauseOfflineDownload = { offlinePackagesViewModel?.pause(it) },
                    onResumeOfflineDownload = { offlinePackagesViewModel?.resume(it) },
                    onCancelOfflineDownload = { offlinePackagesViewModel?.cancel(it) },
                    onSelectionValidated = { bounds ->
                        project?.id?.let { viewModel.defineProjectArea(it, bounds) }
                    }
                )
            }
        }
    }
}
