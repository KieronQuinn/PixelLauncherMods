package com.kieronquinn.app.pixellaunchermods.ui.screens.apps

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteApp
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository
import com.kieronquinn.app.pixellaunchermods.utils.extensions.TAP_DEBOUNCE
import com.kieronquinn.app.pixellaunchermods.utils.extensions.instantCombine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class AppsViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val searchShowClear: StateFlow<Boolean>

    abstract fun getSearchTerm(): String
    abstract fun setSearchTerm(search: String)
    abstract fun onItemClicked(item: Item)
    abstract fun updateThemedIconsState(isDarkMode: Boolean)
    abstract fun onResume()

    sealed class State {
        object Loading: State()
        object Error: State()
        data class Loaded(val apps: List<Item>): State()
    }

    sealed class Item(val type: Type) {
        object Header: Item(Type.HEADER)
        data class App(val app: RemoteApp): Item(Type.APP)

        enum class Type {
            HEADER, APP
        }
    }

}

class AppsViewModelImpl(
    private val remoteAppsRepository: RemoteAppsRepository,
    context: Context,
    private val navigation: ContainerNavigation
): AppsViewModel() {

    private val searchTerm = MutableStateFlow("")
    private val loadIcons = MutableSharedFlow<Unit>()

    override val searchShowClear = searchTerm.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val remoteApps = instantCombine(loadIcons, remoteAppsRepository.onRemoteDatabaseChanged).mapLatest {
        remoteAppsRepository.getRemoteApps(false)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var glideDarkMode: Boolean? = null
    private val appClickBus = MutableSharedFlow<RemoteApp>()
    private val applyIconPackClickBus = MutableSharedFlow<Unit>()

    private val glide by lazy {
        Glide.get(context)
    }

    override val state = combine(searchTerm, remoteApps.filterNotNull()) { search, apps ->
        when {
            apps.isEmpty() -> {
                State.Error
            }
            else -> {
                val items = listOf(Item.Header) + apps
                    .filter { it.label.contains(search, true) }
                    .map { Item.App(it) }
                State.Loaded(items)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun getSearchTerm(): String {
        return searchTerm.value
    }

    override fun setSearchTerm(search: String) {
        viewModelScope.launch {
            searchTerm.emit(search)
        }
    }

    override fun updateThemedIconsState(isDarkMode: Boolean) {
        viewModelScope.launch {
            if(glideDarkMode != null && glideDarkMode != isDarkMode) {
                glide.clearMemory()
            }
            glideDarkMode = isDarkMode
            remoteAppsRepository.updateThemedIconsState()
        }
    }

    override fun onItemClicked(item: Item) {
        when (item) {
            is Item.App -> {
                onAppClicked(item.app)
            }
            is Item.Header -> {
                onApplyIconPackClicked()
            }
        }
    }

    override fun onResume() {
        viewModelScope.launch {
            loadIcons.emit(Unit)
        }
    }

    private fun onAppClicked(app: RemoteApp) {
        viewModelScope.launch {
            appClickBus.emit(app)
        }
    }

    private fun onApplyIconPackClicked() {
        viewModelScope.launch {
            applyIconPackClickBus.emit(Unit)
        }
    }

    private fun setupAppClickListener() = viewModelScope.launch {
        appClickBus.debounce(TAP_DEBOUNCE).collect {
            navigation.navigate(AppsFragmentDirections.actionAppsFragmentToNavGraphAppEditor(it))
        }
    }

    private fun setupApplyIconPackListener() = viewModelScope.launch {
        applyIconPackClickBus.debounce(TAP_DEBOUNCE).collect {
            navigation.navigate(AppsFragmentDirections.actionAppsFragmentToAutoIconPackFragment())
        }
    }

    init {
        setupAppClickListener()
        setupApplyIconPackListener()
    }

}