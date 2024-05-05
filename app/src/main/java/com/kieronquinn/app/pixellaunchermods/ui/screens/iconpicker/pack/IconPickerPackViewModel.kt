package com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.pack


import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.model.icon.IconPickerResult
import com.kieronquinn.app.pixellaunchermods.repositories.IconLoaderRepository
import com.kieronquinn.app.pixellaunchermods.repositories.IconPackRepository
import com.kieronquinn.app.pixellaunchermods.repositories.IconPackRepository.IconPackIconCategory
import com.kieronquinn.app.pixellaunchermods.repositories.IconPackRepository.IconPackIconOptions
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerViewModel
import com.kieronquinn.app.pixellaunchermods.utils.extensions.TAP_DEBOUNCE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class IconPickerPackViewModel(
    iconLoaderRepository: IconLoaderRepository,
    navigation: ContainerNavigation
): BasePickerViewModel(iconLoaderRepository, navigation) {

    sealed class Item(val itemType: ItemType, val section: String) {
        data class Icon(val iconPackIcon: IconPackIconOptions, val category: IconPackIconCategory? = null) :
            Item(ItemType.ICON, category?.name ?: "")

        data class Category(val iconPackIconCategory: IconPackIconCategory) :
            Item(ItemType.CATEGORY, iconPackIconCategory.name)

        enum class ItemType {
            ICON, CATEGORY
        }
    }

    sealed class State {
        object Loading : State()
        data class Loaded(val items: List<Item>) : State()
    }

    abstract val state: StateFlow<State>
    abstract val showClose: StateFlow<Boolean>

    abstract fun setupWithConfig(packageName: String, mono: Boolean)
    abstract fun setSearchTerm(search: String)
    abstract fun getSearchTerm(): String
    abstract fun onLaunchExternalClicked(
        externalPicker: ActivityResultLauncher<Intent>,
        intent: Intent?
    )
    abstract fun onExternalResult(uri: Uri)
    abstract fun onIconClicked(icon: Item.Icon)

}

class IconPickerPackViewModelImpl(
    iconPackRepository: IconPackRepository,
    iconLoaderRepository: IconLoaderRepository,
    navigation: ContainerNavigation
): IconPickerPackViewModel(iconLoaderRepository, navigation) {

    private val iconPackPackage = MutableStateFlow<String?>(null)
    private val search = MutableStateFlow("")
    private val iconClickBus = MutableSharedFlow<Item.Icon>()

    private val icons = iconPackPackage.filterNotNull().map { packageName ->
        iconPackRepository.getAllIcons(packageName)
    }

    override val state = combine(icons, search, monoConfig.filterNotNull()) { i, s, mono ->
        val items = ArrayList<Item>()
        withContext(Dispatchers.IO) {
            var category: IconPackIconCategory? = null
            i.forEach {
                if(it.resourceName.contains(s, true) || it.category?.name?.contains(s, true) == true){
                    if (it.category != null && it.category.name.isNotBlank() && it.category != category) {
                        items.add(Item.Category(it.category))
                        category = it.category
                    }
                    items.add(Item.Icon(IconPackIconOptions(it, mono), category))
                }
            }
        }
        State.Loaded(items)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val showClose = search.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override fun setupWithConfig(packageName: String, mono: Boolean) {
        viewModelScope.launch {
            iconPackPackage.emit(packageName)
            monoConfig.emit(mono)
        }
    }

    override fun setSearchTerm(search: String) {
        viewModelScope.launch {
            this@IconPickerPackViewModelImpl.search.emit(search.trim())
        }
    }

    override fun onLaunchExternalClicked(
        externalPicker: ActivityResultLauncher<Intent>,
        intent: Intent?
    ) {
        viewModelScope.launch {
            externalPicker.launch(intent ?: return@launch)
        }
    }

    override fun getSearchTerm(): String {
        return search.value
    }

    override fun onExternalResult(uri: Uri) {
        viewModelScope.launch {
            onIconSelected(IconPickerResult.UriIcon(uri))
        }
    }

    override fun onIconClicked(icon: Item.Icon) {
        viewModelScope.launch {
            iconClickBus.emit(icon)
        }
    }

    private fun setupIconClickListener() = viewModelScope.launch {
        iconClickBus.debounce(TAP_DEBOUNCE).collect {
            val icon = it.iconPackIcon.iconPackIcon
            onIconSelected(
                IconPickerResult.IconPackIcon(
                icon.iconPackPackageName,
                icon.resourceName,
                icon.isAdaptiveIcon
            ))
        }
    }

    init {
        setupIconClickListener()
    }

}