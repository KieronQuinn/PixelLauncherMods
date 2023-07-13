package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.hideapps

import android.content.pm.LauncherActivityInfo
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.BuildConfig
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.repositories.AppsRepository
import com.kieronquinn.app.pixellaunchermods.repositories.OverlayRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class HideAppsViewModel: ViewModel() {

    data class HiddenApp(val launcherApp: LauncherActivityInfo, var hidden: Boolean)

    sealed class State {
        object Loading: State()
        object ModuleRequired: State()
        data class Loaded(val apps: List<HiddenApp>): State()
    }

    abstract val state: StateFlow<State>
    abstract val searchShowClear: StateFlow<Boolean>

    abstract fun reload()
    abstract fun getSearchTerm(): String
    abstract fun setSearchTerm(search: String)
    abstract fun onSaveClicked()
    abstract fun onSaveModuleClicked(launcher: ActivityResultLauncher<String>)
    abstract fun saveModule(uri: Uri)

}

class HideAppsViewModelImpl(
    private val overlayRepository: OverlayRepository,
    private val appsRepository: AppsRepository,
    private val navigation: ContainerNavigation
): HideAppsViewModel() {

    private val reloadBus = MutableStateFlow(System.currentTimeMillis())
    private val searchTerm = MutableStateFlow("")

    override val searchShowClear = searchTerm.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val isOverlayInstalled = flow {
        emit(overlayRepository.isOverlayInstalled())
    }

    private val filteredComponents = flow {
        emit(overlayRepository.getFilteredComponents())
    }

    private val allApps = flow {
        val apps = appsRepository.getAllLauncherApps()
            .filterNot { it.applicationInfo.packageName == BuildConfig.APPLICATION_ID }
            .sortedBy { it.label.toString().lowercase() }
        emit(apps)
    }

    private val apps = combine(filteredComponents, allApps) { filtered, all ->
        all.map {
            HiddenApp(it, filtered.contains(it.componentName.flattenToShortString()))
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override val state = reloadBus.flatMapLatest {
        loadState()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    private fun loadState() = combine(
        isOverlayInstalled,
        apps,
        searchTerm
    ) { installed, apps, search ->
        if(apps == null) return@combine State.Loading
        if(!installed) return@combine State.ModuleRequired
        val filteredApps = apps.filter { it.launcherApp.label.contains(search.trim(), true) }
        State.Loaded(filteredApps)
    }

    override fun getSearchTerm(): String {
        return searchTerm.value
    }

    override fun onSaveModuleClicked(launcher: ActivityResultLauncher<String>) {
        launcher.launch(overlayRepository.getModuleFilename())
    }

    override fun saveModule(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            overlayRepository.saveModule(uri)
        }
    }

    override fun setSearchTerm(search: String) {
        viewModelScope.launch {
            searchTerm.emit(search)
        }
    }

    override fun onSaveClicked() {
        viewModelScope.launch {
            val apps = apps.value ?: return@launch
            val components = apps.filter { it.hidden }.map {
                it.launcherApp.componentName.flattenToShortString()
            }.toTypedArray()
            navigation.navigate(HideAppsFragmentDirections.actionHideAppsFragmentToHideAppsApplyFragment(
                components,
                null,
                null,
                null,
                null,
                null
            ))
        }
    }

    override fun reload() {
        viewModelScope.launch {
            reloadBus.emit(System.currentTimeMillis())
        }
    }

}