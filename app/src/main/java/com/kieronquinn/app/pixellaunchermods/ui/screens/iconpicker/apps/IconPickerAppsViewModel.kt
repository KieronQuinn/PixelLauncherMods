package com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.apps

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.model.icon.ApplicationIcon
import com.kieronquinn.app.pixellaunchermods.model.icon.IconPickerResult
import com.kieronquinn.app.pixellaunchermods.repositories.AppsRepository
import com.kieronquinn.app.pixellaunchermods.repositories.IconLoaderRepository
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerViewModel
import com.kieronquinn.app.pixellaunchermods.utils.extensions.TAP_DEBOUNCE
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class IconPickerAppsViewModel(
    iconLoaderRepository: IconLoaderRepository,
    navigation: ContainerNavigation
): BasePickerViewModel(iconLoaderRepository, navigation) {

    abstract val state: StateFlow<State>
    abstract val searchShowClear: StateFlow<Boolean>

    abstract fun getSearchTerm(): String
    abstract fun setSearchTerm(search: String)
    abstract fun onAppClicked(icon: ApplicationIcon)
    abstract fun setupWithConfig(mono: Boolean)

    abstract fun onShrinkIconsChanged(enabled: Boolean)

    sealed class State {
        object Loading: State()
        data class Loaded(val items: List<Item>, val mono: Boolean): State()
    }

    sealed class Item(val type: Type) {
        data class Header(val shrinkNonAdaptiveIcons: Boolean): Item(Type.HEADER)
        data class App(val app: AppsRepository.App, val shrinkNonAdaptiveIcons: Boolean): Item(Type.APP)

        enum class Type {
            HEADER, APP
        }
    }

}

class IconPickerAppsViewModelImpl(
    private val appsRepository: AppsRepository,
    iconLoaderRepository: IconLoaderRepository,
    navigation: ContainerNavigation
): IconPickerAppsViewModel(iconLoaderRepository, navigation) {

    private val allApps = flow {
        emit(appsRepository.getAllApps())
    }

    private val searchTerm = MutableStateFlow("")
    private val appClickBus = MutableSharedFlow<ApplicationIcon>()
    private val shrinkNonAdaptiveIconsChangedBus = MutableSharedFlow<Boolean>()
    private val shrinkNonAdaptiveIcons = MutableStateFlow(true)

    override val searchShowClear = searchTerm.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override val state = combine(allApps, searchTerm, shrinkNonAdaptiveIcons, monoConfig.filterNotNull()){ apps, search, shrink, mono ->
        val filteredApps = apps.filter { it.label.contains(search, true) }
            .map { Item.App(it, shrink) }
        State.Loaded(listOfNotNull(
            if(!mono) Item.Header(shrink) else null,
            *filteredApps.toTypedArray()
        ), mono)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun getSearchTerm(): String {
        return searchTerm.value
    }

    override fun setSearchTerm(search: String) {
        viewModelScope.launch {
            searchTerm.emit(search)
        }
    }

    override fun onAppClicked(icon: ApplicationIcon) {
        viewModelScope.launch {
            appClickBus.emit(icon)
        }
    }

    override fun onShrinkIconsChanged(enabled: Boolean) {
        viewModelScope.launch {
            shrinkNonAdaptiveIconsChangedBus.emit(enabled)
        }
    }

    override fun setupWithConfig(mono: Boolean) {
        viewModelScope.launch {
            monoConfig.emit(mono)
        }
    }

    private fun setupAppClickListener() = viewModelScope.launch {
        appClickBus.debounce(TAP_DEBOUNCE).collect { applicationIcon ->
            onIconSelected(IconPickerResult.PackageIcon(applicationIcon))
        }
    }

    private fun setupShrinkIconsListener() = viewModelScope.launch {
        shrinkNonAdaptiveIconsChangedBus.debounce(TAP_DEBOUNCE).collect {
            shrinkNonAdaptiveIcons.emit(it)
        }
    }

    init {
        setupAppClickListener()
        setupShrinkIconsListener()
    }

}