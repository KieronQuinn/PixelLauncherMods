package com.kieronquinn.app.pixellaunchermods.ui.screens.autoiconpack.apply

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import com.kieronquinn.app.pixellaunchermods.utils.extensions.collectUntilNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class AutoIconPackApplyViewModel: ViewModel() {

    sealed class State {
        object Idle: State()
        data class Applying(val progress: Float): State()
        object Finished: State()
    }

    abstract val state: StateFlow<State>
    abstract fun setupWithIconPackPackages(packages: List<String>)

}

class AutoIconPackApplyViewModelImpl(
    private val remoteAppsRepository: RemoteAppsRepository,
    private val settingsRepository: SettingsRepository,
    private val navigation: ContainerNavigation
): AutoIconPackApplyViewModel() {

    private val packages = MutableStateFlow<List<String>?>(null)

    override val state = packages.filterNotNull().flatMapLatest {
        remoteAppsRepository.autoApplyIconPacks(it, true)
    }.map {
        if(it != null){
            State.Applying(it)
        }else{
            State.Finished
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Idle)

    private fun setupFinishedListener() = viewModelScope.launch(Dispatchers.IO) {
        state.collect {
            if(it is State.Finished){
                settingsRepository.autoIconPackOrder.set(packages.value!!)
                navigateUpToApps()
            }
        }
    }

    override fun setupWithIconPackPackages(packages: List<String>) {
        if(this.packages.value != null) return
        viewModelScope.launch {
            this@AutoIconPackApplyViewModelImpl.packages.emit(packages)
        }
    }

    private fun navigateUpToApps() {
        viewModelScope.launch {
            navigation.navigateUpTo(R.id.appsFragment)
        }
    }

    init {
        setupFinishedListener()
    }

}