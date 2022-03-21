package com.kieronquinn.app.pixellaunchermods.ui.screens.autoiconpack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.model.editor.IconPack
import com.kieronquinn.app.pixellaunchermods.repositories.IconPackRepository
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class AutoIconPackViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val toastBus: Flow<Int>
    abstract fun onApplyClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(val items: ArrayList<Item>): State()
        object Empty: State()
    }

    sealed class Item(val type: ItemType) {
        object Header: Item(ItemType.HEADER)
        object Line: Item(ItemType.LINE)
        data class Pack(val iconPack: IconPack): Item(ItemType.PACK)

        enum class ItemType {
            HEADER, LINE, PACK
        }
    }

}

class AutoIconPackViewModelImpl(
    iconPackRepository: IconPackRepository,
    settingsRepository: SettingsRepository,
    private val navigation: ContainerNavigation
): AutoIconPackViewModel() {

    private val allIconPacks = flow {
        emit(iconPackRepository.getAllIconPacks().sortedBy { it.label.toString().lowercase() })
    }

    private val savedIconPacks = settingsRepository.autoIconPackOrder.asFlow()

    override val toastBus = MutableSharedFlow<Int>()

    override val state = combine(allIconPacks, savedIconPacks) { all, saved ->
        val items = ArrayList<Item>()
        val allPacks = all.toMutableList()
        if(allPacks.isEmpty()){
            return@combine State.Empty
        }
        items.add(Item.Header)
        if(saved.isEmpty()){
            //Default to showing all the packs above the line
            items.addAll(all.map { Item.Pack(it) })
            items.add(Item.Line)
            return@combine State.Loaded(items)
        }
        //Add the saved packs first, if they exist
        saved.forEach { packageName ->
            val foundPack = allPacks.firstOrNull { it.packageName == packageName } ?: return@forEach
            items.add(Item.Pack(foundPack))
            allPacks.remove(foundPack)
        }
        //Add the line
        items.add(Item.Line)
        //Add the remaining found packs
        items.addAll(allPacks.map { Item.Pack(it) })
        State.Loaded(items)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onApplyClicked() {
        viewModelScope.launch {
            val selectedPacks = getSelectedPacks()
            if(selectedPacks == null || selectedPacks.isEmpty()){
                toastBus.emit(R.string.auto_icon_pack_snackbar)
                return@launch
            }
            navigation.navigate(AutoIconPackFragmentDirections.actionAutoIconPackFragmentToAutoIconPackApplyFragment(selectedPacks.toTypedArray()))
        }
    }

    private fun getSelectedPacks(): ArrayList<String>? {
        val items = (state.value as? State.Loaded)?.items ?: return null
        //Iterate through items, skipping header, until the line
        val packages = ArrayList<String>()
        items.forEach {
            when (it) {
                is Item.Header -> return@forEach //Skip header
                is Item.Line -> return packages //Reached end
                is Item.Pack -> packages.add(it.iconPack.packageName)
            }
        }
        return packages
    }

}