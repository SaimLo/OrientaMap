package com.showroomprive.orientamap.presentation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.showroomprive.orientamap.data.print.PrintMapExportService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val exportScope = rememberCoroutineScope()
    val exportService = remember(context) { PrintMapExportService(context) }
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
                val exportLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/pdf")
                ) { uri ->
                    val bounds = project?.geoBounds
                    if (uri != null && bounds != null) {
                        val title = project?.name ?: "OrientaMap"
                        exportScope.launch {
                            runCatching {
                                val output = context.contentResolver.openOutputStream(uri)
                                    ?: error("Impossible d'ouvrir le fichier PDF")
                                output.use {
                                    exportService.exportSelectedArea(
                                        output = it,
                                        title = title,
                                        bounds = bounds
                                    )
                                }
                            }.onSuccess {
                                Toast.makeText(
                                    context,
                                    "PDF exporté",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.onFailure { error ->
                                Toast.makeText(
                                    context,
                                    "Échec de l'export PDF : ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
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
                    onExportPdf = {
                        val fileName = "${project?.name ?: "OrientaMap"}.pdf"
                        exportLauncher.launch(fileName)
                    },
                    onSelectionValidated = { bounds ->
                        project?.id?.let { viewModel.defineProjectArea(it, bounds) }
                    }
                )
            }
        }
    }
}
