package com.showroomprive.orientamap.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.showroomprive.orientamap.data.offline.OfflinePackageDownloadCoordinator
import com.showroomprive.orientamap.domain.model.OfflinePackage
import com.showroomprive.orientamap.domain.model.OfflinePackageStatus
import com.showroomprive.orientamap.domain.repository.OfflinePackageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class OfflinePackagesViewModel(
    private val repository: OfflinePackageRepository,
    private val coordinator: OfflinePackageDownloadCoordinator
) : ViewModel() {
    private val selectedProjectId = MutableStateFlow<UUID?>(null)
    private var observationJob: Job? = null

    val packages: StateFlow<List<OfflinePackage>> = selectedProjectId
        .flatMapLatest { projectId ->
            projectId?.let(repository::observeForProject) ?: emptyFlow()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun selectProject(projectId: UUID?) {
        selectedProjectId.value = projectId
        observationJob?.cancel()
        if (projectId == null) return

        observationJob = viewModelScope.launch {
            repository.observeForProject(projectId)
                .first()
                .filter { it.mapLibreRegionId != null }
                .filter {
                    it.status == OfflinePackageStatus.EN_ATTENTE ||
                        it.status == OfflinePackageStatus.TELECHARGEMENT_EN_COURS ||
                        it.status == OfflinePackageStatus.PAUSE
                }
                .forEach { offlinePackage ->
                    coordinator.observe(
                        packageId = offlinePackage.id,
                        regionId = offlinePackage.mapLibreRegionId!!
                    )
                }
        }
    }

    fun startDefaultDownload(projectId: UUID) {
        viewModelScope.launch {
            val existingPackage = repository.observeForProject(projectId)
                .first()
                .firstOrNull { packageIsActive(it) }
            if (existingPackage != null) return@launch

            val packageId = repository.createForProject(
                projectId = projectId,
                name = "Plan IGN",
                minZoom = 10,
                maxZoom = 14,
                averageTileBytes = 50_000L
            ) ?: return@launch
            coordinator.start(packageId)
        }
    }

    fun pause(offlinePackage: OfflinePackage) = coordinator.pause(offlinePackage.id)
    fun resume(offlinePackage: OfflinePackage) = coordinator.resume(offlinePackage.id)
    fun cancel(offlinePackage: OfflinePackage) = coordinator.cancel(offlinePackage.id)
    fun delete(offlinePackage: OfflinePackage) = coordinator.delete(offlinePackage.id)

    private fun packageIsActive(offlinePackage: OfflinePackage): Boolean =
        offlinePackage.status == OfflinePackageStatus.CREE ||
            offlinePackage.status == OfflinePackageStatus.EN_ATTENTE ||
            offlinePackage.status == OfflinePackageStatus.TELECHARGEMENT_EN_COURS ||
            offlinePackage.status == OfflinePackageStatus.PAUSE
}

class OfflinePackagesViewModelFactory(
    private val repository: OfflinePackageRepository,
    private val coordinator: OfflinePackageDownloadCoordinator
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(OfflinePackagesViewModel::class.java))
        return OfflinePackagesViewModel(repository, coordinator) as T
    }
}
