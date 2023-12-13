package com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.legacythemed

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.model.icon.IconPickerResult
import com.kieronquinn.app.pixellaunchermods.model.icon.LegacyThemedIcon
import com.kieronquinn.app.pixellaunchermods.repositories.IconLoaderRepository
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerViewModel
import com.kieronquinn.app.pixellaunchermods.utils.extensions.TAP_DEBOUNCE
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class LegacyThemedIconPickerViewModel(
    iconLoaderRepository: IconLoaderRepository,
    navigation: ContainerNavigation
): BasePickerViewModel(iconLoaderRepository, navigation) {

    sealed class Item(val type: ItemType) {
        data class Icon(val icon: LegacyThemedIcon): Item(ItemType.ICON)
        object Header: Item(ItemType.HEADER)

        enum class ItemType {
            HEADER, ICON
        }
    }

    sealed class State {
        object Loading: State()
        data class Loaded(val icons: List<Item>): State()
    }

    abstract val state: StateFlow<State>
    abstract fun onIconClicked(icon: LegacyThemedIcon)
    abstract fun setupWithConfig(mono: Boolean, lawnicons: Boolean)
    abstract val searchShowClear: StateFlow<Boolean>

    abstract fun getSearchTerm(): String
    abstract fun setSearchTerm(search: String)

}

class LegacyThemedIconPickerViewModelImpl(
    private val iconLoaderRepository: IconLoaderRepository,
    private val remoteAppsRepository: RemoteAppsRepository,
    navigation: ContainerNavigation
): LegacyThemedIconPickerViewModel(iconLoaderRepository, navigation) {

    private val itemClickBus = MutableSharedFlow<LegacyThemedIcon>()
    private val lawnicons = MutableStateFlow<Boolean?>(null)
    private val searchTerm = MutableStateFlow("")
    private val nativeMono = remoteAppsRepository.areNativeThemedIconsSupported.filterNotNull()

    private val icons = combine(lawnicons.filterNotNull(), searchTerm) { l, s ->
        if(l){
            iconLoaderRepository.getAllLawnicons()
        }else{
            iconLoaderRepository.getAllLegacyThemedIcons()
        }.filter {
            it.key.contains(s, true) || it.value.resourceEntryName.contains(s, true)
        }.mapNotNull {
            if(it.value.type != LegacyThemedIcon.Type.DRAWABLE) null
            else Pair(it.key, it.value)
        }.sortedBy { it.second.resourceEntryName.lowercase() }
            .distinctBy { it.second.resourceId }
            .map { Item.Icon(it.second) }
    }

    override val searchShowClear = searchTerm.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override val state = combine(nativeMono, lawnicons.filterNotNull(), icons) {
        nativeMonoSupported, useLawnicons, icons ->
        State.Loaded(listOfNotNull(
            if(!nativeMonoSupported && !useLawnicons) Item.Header else null,
            *icons.toTypedArray()
        ))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onIconClicked(icon: LegacyThemedIcon) {
        viewModelScope.launch {
            itemClickBus.emit(icon)
        }
    }

    private fun String.getLastSegment(): String {
        return if(contains(".")){
            substring(lastIndexOf(".") + 1)
        }else this
    }

    override fun getSearchTerm(): String {
        return searchTerm.value
    }

    override fun setSearchTerm(search: String) {
        viewModelScope.launch {
            searchTerm.emit(search)
        }
    }

    private fun setupClickListener() {
        viewModelScope.launch {
            itemClickBus.debounce(TAP_DEBOUNCE).collect {
                val lawnicons = lawnicons.filterNotNull().first()
                val icon = if(lawnicons){
                    val resourceName = iconLoaderRepository.getLawniconName(it)
                    IconPickerResult.Lawnicon(it.resourceId, resourceName)
                }else{
                    val resourceName = iconLoaderRepository.getLegacyThemedIconName(it)
                    IconPickerResult.LegacyThemedIcon(it.resourceId, resourceName)
                }
                onIconSelected(icon)
            }
        }
    }

    override fun setupWithConfig(mono: Boolean, lawnicons: Boolean) {
        viewModelScope.launch {
            monoConfig.emit(mono)
            this@LegacyThemedIconPickerViewModelImpl.lawnicons.emit(lawnicons)
        }
    }

    init {
        setupClickListener()
    }

}