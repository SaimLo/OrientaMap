package com.showroomprive.orientamap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.showroomprive.orientamap.presentation.OfflinePackagesViewModel
import com.showroomprive.orientamap.presentation.OfflinePackagesViewModelFactory
import com.showroomprive.orientamap.presentation.ProjectsViewModel
import com.showroomprive.orientamap.presentation.ProjectsViewModelFactory
import com.showroomprive.orientamap.presentation.OrientaMapApp
import com.showroomprive.orientamap.ui.theme.OrientaMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as OrientaMapApplication).container
        setContent {
            OrientaMapTheme {
                val viewModel: ProjectsViewModel = viewModel(
                    factory = ProjectsViewModelFactory(container.mapProjectRepository)
                )
                val offlinePackagesViewModel: OfflinePackagesViewModel = viewModel(
                    factory = OfflinePackagesViewModelFactory(
                        container.offlinePackageRepository,
                        container.offlineDownloadCoordinator
                    )
                )
                OrientaMapApp(viewModel, offlinePackagesViewModel)
            }
        }
    }
}
