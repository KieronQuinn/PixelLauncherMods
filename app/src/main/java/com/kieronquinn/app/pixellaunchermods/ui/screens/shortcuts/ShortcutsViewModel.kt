package com.kieronquinn.app.pixellaunchermods.ui.screens.shortcuts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteApp
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteFavourite
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository.Shortcut
import com.kieronquinn.app.pixellaunchermods.utils.extensions.TAP_DEBOUNCE
import com.kieronquinn.app.pixellaunchermods.utils.extensions.instantCombine
import com.kieronquinn.app.pixellaunchermods.utils.glide.GlideApp
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class ShortcutsViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val searchShowClear: StateFlow<Boolean>

    abstract fun getSearchTerm(): String
    abstract fun setSearchTerm(search: String)
    abstract fun onItemClicked(item: Shortcut)
    abstract fun updateThemedIconsState(isDarkMode: Boolean)
    abstract fun onResume()

    sealed class State {
        object Loading: State()
        object Error: State()
        data class Loaded(val shortcuts: List<Shortcut>, val themedIconsEnabled: Boolean): State()
    }

}

class ShortcutsViewModelImpl(
    context: Context,
    private val remoteAppsRepository: RemoteAppsRepository,
    private val navigation: ContainerNavigation
): ShortcutsViewModel() {

    private val searchTerm = MutableStateFlow("")
    private val loadIcons = MutableSharedFlow<Unit>()

    override val searchShowClear = searchTerm.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val remoteShortcuts = instantCombine(loadIcons, remoteAppsRepository.onRemoteDatabaseChanged).mapLatest {
        remoteAppsRepository.getRemoteShortcuts().sortedBy { it.label?.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val themedIconsEnabled = remoteAppsRepository.areThemedIconsEnabled
    private var glideDarkMode: Boolean? = null
    private val appShortcutClickBus = MutableSharedFlow<RemoteApp>()
    private val shortcutClickBus = MutableSharedFlow<RemoteFavourite>()

    private val glide by lazy {
        GlideApp.get(context)
    }

    override val state = combine(remoteShortcuts, themedIconsEnabled, searchTerm) { shortcuts, themed, search ->
        when {
            shortcuts?.isEmpty() == true -> {
                State.Error
            }
            shortcuts != null && themed != null -> {
                State.Loaded(shortcuts.filter {
                      it.label?.contains(search, true) == true
                }, themed)
            }
            else -> {
                State.Loading
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

    override fun onItemClicked(item: Shortcut) {
        when (item) {
            is Shortcut.AppShortcut -> {
                onAppShortcutClicked(item.shortcut)
            }
            is Shortcut.LegacyShortcut -> {
                onShortcutClicked(item.shortcut)
            }
        }
    }

    override fun onResume() {
        viewModelScope.launch {
            loadIcons.emit(Unit)
        }
    }

    private fun onAppShortcutClicked(app: RemoteApp) {
        viewModelScope.launch {
            appShortcutClickBus.emit(app)
        }
    }

    private fun onShortcutClicked(favourite: RemoteFavourite) {
        viewModelScope.launch {
            shortcutClickBus.emit(favourite)
        }
    }

    private fun setupAppShortcutClickListener() = viewModelScope.launch {
        appShortcutClickBus.debounce(TAP_DEBOUNCE).collect {
            navigation.navigate(ShortcutsFragmentDirections.actionShortcutsFragmentToNavGraphAppEditor(it))
        }
    }

    private fun setupShortcutClickListener() = viewModelScope.launch {
        shortcutClickBus.debounce(TAP_DEBOUNCE).collect {
            navigation.navigate(ShortcutsFragmentDirections.actionShortcutsFragmentToShortcutEditorFragment(it))
        }
    }

    init {
        setupAppShortcutClickListener()
        setupShortcutClickListener()
    }

}