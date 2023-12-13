package com.kieronquinn.app.pixellaunchermods.ui.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.PIXEL_LAUNCHER_PACKAGE_NAME
import com.kieronquinn.app.pixellaunchermods.repositories.AppsRepository
import com.kieronquinn.app.pixellaunchermods.repositories.RootServiceRepository
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

abstract class MainActivityViewModel: ViewModel() {

    sealed class State {
        object Loading: State()
        object Loaded: State()
        object NoRoot: State()
        object NoPixelLauncher: State()
    }

    abstract val isAppReady: StateFlow<State>

}

class MainActivityViewModelImpl(
    rootServiceRepository: RootServiceRepository,
    appsRepository: AppsRepository,
    settingsRepository: SettingsRepository
): MainActivityViewModel() {

    private val isRooted = flow {
        emit(rootServiceRepository.isRooted())
    }.flowOn(Dispatchers.IO)

    private val isPixelLauncherInstalled = flow {
        emit(appsRepository.getApplicationInfoForPackage(PIXEL_LAUNCHER_PACKAGE_NAME) != null)
    }.flowOn(Dispatchers.IO)

    private val splashTimeout = flow {
        delay(1500L)
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    override val isAppReady = combine(isRooted, isPixelLauncherInstalled, splashTimeout) { rooted, pl, _ ->
        if(!rooted){
            settingsRepository.shouldLaunchService.set(false)
            return@combine State.NoRoot
        }
        if(!pl) {
            settingsRepository.shouldLaunchService.set(false)
            return@combine State.NoPixelLauncher
        }
        settingsRepository.shouldLaunchService.set(true)
        State.Loaded
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

}