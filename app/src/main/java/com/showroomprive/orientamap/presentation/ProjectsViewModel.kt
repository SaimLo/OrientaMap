package com.showroomprive.orientamap.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.showroomprive.orientamap.domain.model.GeoBounds
import com.showroomprive.orientamap.domain.model.MapProject
import com.showroomprive.orientamap.domain.repository.MapProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ProjectsViewModel(
    private val repository: MapProjectRepository
) : ViewModel() {
    val projects: StateFlow<List<MapProject>> = repository
        .observeProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun createSampleProject() {
        viewModelScope.launch {
            repository.createProject(
                name = "Nouveau projet",
                description = "Projet cartographique à compléter"
            )
        }
    }

    fun renameProject(project: MapProject, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.renameProject(project.id, trimmed)
        }
    }

    fun defineProjectArea(projectId: UUID, bounds: GeoBounds) {
        viewModelScope.launch {
            repository.defineProjectArea(projectId, bounds)
        }
    }

    fun deleteProject(project: MapProject) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }
}

class ProjectsViewModelFactory(
    private val repository: MapProjectRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProjectsViewModel::class.java))
        return ProjectsViewModel(repository) as T
    }
}
