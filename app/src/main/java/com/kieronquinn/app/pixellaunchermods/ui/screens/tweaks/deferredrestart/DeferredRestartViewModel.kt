package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.deferredrestart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class DeferredRestartViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract fun onOptionClicked(option: SettingsRepository.DeferredRestartMode)

    sealed class State {
        object Loading: State()
        data class Loaded(val items: List<Item>): State()
    }

    sealed class Item(val type: Type) {
        object Header: Item(Type.HEADER)

        data class Option(
            val mode: SettingsRepository.DeferredRestartMode,
            val isSelected: Boolean
        ): Item(Type.OPTION)

        enum class Type {
            HEADER, OPTION
        }
    }

}

class DeferredRestartViewModelImpl(
    settingsRepository: SettingsRepository
): DeferredRestartViewModel() {

    private val restartMode = settingsRepository.deferredRestartMode

    override val state = restartMode.asFlow().mapLatest { selected ->
        val items = listOf(Item.Header) + SettingsRepository.DeferredRestartMode.values().reversed().map {
            Item.Option(it, it == selected)
        }
        State.Loaded(items)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onOptionClicked(option: SettingsRepository.DeferredRestartMode) {
        viewModelScope.launch {
            restartMode.set(option)
        }
    }

}