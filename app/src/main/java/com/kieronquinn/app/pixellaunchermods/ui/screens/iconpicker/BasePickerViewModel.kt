package com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.model.icon.IconPickerResult
import com.kieronquinn.app.pixellaunchermods.repositories.IconLoaderRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class BasePickerViewModel(
    private val iconLoaderRepository: IconLoaderRepository,
    private val navigation: ContainerNavigation
): ViewModel() {

    data class IconPickerResultOptions(val result: IconPickerResult, val mono: Boolean, val loadFullRes: Boolean)

    val iconPickerResultBus = MutableSharedFlow<IconPickerResult?>()
    val monoConfig = MutableStateFlow<Boolean?>(null)

    fun isMono(): Boolean {
        return monoConfig.value ?: false
    }

    protected fun onIconSelected(result: IconPickerResult?) {
        viewModelScope.launch {
            iconPickerResultBus.emit(result)
        }
    }

    fun navigateUpToAppEditor() {
        viewModelScope.launch {
            navigation.navigateUpTo(R.id.nav_graph_icon_picker, true)
        }
    }

}