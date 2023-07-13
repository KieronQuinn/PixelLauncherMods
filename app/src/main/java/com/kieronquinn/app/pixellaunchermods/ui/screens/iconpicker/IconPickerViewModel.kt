package com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker

import android.content.pm.ApplicationInfo
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.BuildConfig
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.model.editor.IconPack
import com.kieronquinn.app.pixellaunchermods.model.icon.ApplicationIcon
import com.kieronquinn.app.pixellaunchermods.model.icon.IconPickerResult
import com.kieronquinn.app.pixellaunchermods.model.icon.IconPickerResult.PackageIcon
import com.kieronquinn.app.pixellaunchermods.model.icon.LegacyThemedIcon
import com.kieronquinn.app.pixellaunchermods.repositories.AppsRepository
import com.kieronquinn.app.pixellaunchermods.repositories.IconLoaderRepository
import com.kieronquinn.app.pixellaunchermods.repositories.IconPackRepository
import com.kieronquinn.app.pixellaunchermods.repositories.IconPackRepository.IconPackIconOptions
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.IconPickerViewModel.Source.IconPackSource
import com.kieronquinn.app.pixellaunchermods.utils.extensions.TAP_DEBOUNCE
import com.kieronquinn.app.pixellaunchermods.utils.extensions.parseToComponentName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class IconPickerViewModel(
    iconLoaderRepository: IconLoaderRepository,
    navigation: ContainerNavigation
): BasePickerViewModel(iconLoaderRepository, navigation) {

    sealed class Source(val type: Type) {
        object Header: Source(Type.HEADER)
        data class IconPackSource(val iconPack: IconPack, val icon: IconPackIconOptions?, val iconPackIcon: ApplicationIcon): Source(Type.SOURCE)
        data class Apps(val packageIcon: PackageIcon): Source(Type.SOURCE)
        data class LegacyThemedIcons(val legacyThemedIcon: LegacyThemedIcon?, val applicationIcon: ApplicationIcon): Source(Type.SOURCE)
        data class Lawnicons(val legacyThemedIcon: LegacyThemedIcon?, val applicationIcon: ApplicationIcon): Source(Type.SOURCE)
        object File: Source(Type.SOURCE)

        enum class Type {
            HEADER, SOURCE
        }
    }

    sealed class State {
        object Loading: State()
        data class Loaded(val iconPacks: List<Source>, val mono: Boolean): State()
    }

    abstract val state: StateFlow<State>
    abstract val importImageBus: Flow<Unit>
    abstract fun setConfig(componentName: String, mono: Boolean)
    abstract fun onClearClicked()
    abstract fun onSourceClicked(source: Source)
    abstract fun onIconClicked(source: Source)
    abstract fun onImageUriReturned(uri: Uri)

}

class IconPickerViewModelImpl(
    private val iconPackRepository: IconPackRepository,
    private val iconLoaderRepository: IconLoaderRepository,
    private val appsRepository: AppsRepository,
    private val remoteAppsRepository: RemoteAppsRepository,
    private val navigation: ContainerNavigation
): IconPickerViewModel(iconLoaderRepository, navigation) {

    private val component = MutableStateFlow<String?>(null)
    private val sourceClickBus = MutableSharedFlow<Source>()
    private val iconClickBus = MutableSharedFlow<Source>()

    override val importImageBus = MutableSharedFlow<Unit>()

    override val state = combine(
        component.filterNotNull(),
        monoConfig.filterNotNull(),
        remoteAppsRepository.areNativeThemedIconsSupported.filterNotNull()
    ) { component, mono, nativeMono ->
        loadState(component, mono, nativeMono) ?: State.Loading
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    private suspend fun loadState(component: String, mono: Boolean, nativeMono: Boolean): State? {
        val packageName = component.parseToComponentName()?.packageName
        val applicationInfo = appsRepository.getApplicationInfoForPackage(
            packageName ?: BuildConfig.APPLICATION_ID
        ) ?: return null
        val iconPacks = loadIconPacks(component, mono, nativeMono)
        val apps = loadApps(applicationInfo, mono, nativeMono)
        val legacyThemedIcons = loadLegacyThemedIcons(packageName ?: "")
        val lawnicons = loadLawnicons(mono, nativeMono, packageName ?: "")
        val header = if(nativeMono && mono) Source.Header else null
        val options = listOfNotNull(
            header,
            apps,
            if(legacyThemedIcons.legacyThemedIcon != null) legacyThemedIcons else null,
            if(lawnicons?.legacyThemedIcon != null) lawnicons else null,
            *iconPacks.toTypedArray(),
            if(legacyThemedIcons.legacyThemedIcon == null) legacyThemedIcons else null,
            if(lawnicons != null && lawnicons.legacyThemedIcon == null) lawnicons else null,
            if(nativeMono || !mono) Source.File else null
        )
        return State.Loaded(options, mono)
    }

    private suspend fun loadIconPacks(componentName: String, mono: Boolean, nativeMono: Boolean): List<IconPackSource> {
        if(mono && !nativeMono) return emptyList() //Unsupported on non-native mono
        return iconPackRepository.getAllIconPacks().map {
            val icon = iconPackRepository.getIconForComponent(it.packageName, componentName)?.run {
                IconPackIconOptions(this, mono)
            }
            IconPackSource(it, icon, it.iconPackIcon)
        }.sortedWith(compareBy<IconPackSource>{ it.icon == null }
            .thenBy { it.iconPack.label.toString().lowercase() })
    }

    private fun loadApps(applicationInfo: ApplicationInfo, mono: Boolean, nativeMono: Boolean): Source.Apps? {
        if(mono && !nativeMono) return null //Unsupported on non-native mono
        //Shrinking is always enabled at this stage
        val packageIcon = PackageIcon(ApplicationIcon(applicationInfo, true, mono))
        return Source.Apps(packageIcon)
    }

    private fun loadLegacyThemedIcons(packageName: String): Source.LegacyThemedIcons {
        val legacyThemedIcon = iconLoaderRepository.getLegacyThemedIconForPackage(packageName)
        val pixelLauncherIcon = iconLoaderRepository.getPixelLauncherApplicationIcon()
        return Source.LegacyThemedIcons(legacyThemedIcon, pixelLauncherIcon)
    }

    private fun loadLawnicons(mono: Boolean, nativeMono: Boolean, packageName: String): Source.Lawnicons? {
        if(mono && !nativeMono) return null //Unsupported on non-native mono
        val lawnicons = iconLoaderRepository.getAllLawnicons()
        if(lawnicons.isEmpty()) return null
        val legacyThemedIcon = iconLoaderRepository.getLawniconForPackage(packageName)
        val pixelLauncherIcon = iconLoaderRepository.getLawniconsApplicationIcon()
        return Source.Lawnicons(legacyThemedIcon, pixelLauncherIcon)
    }

    override fun setConfig(componentName: String, mono: Boolean) {
        viewModelScope.launch {
            component.emit(componentName)
            monoConfig.emit(mono)
        }
    }

    override fun onSourceClicked(source: Source) {
        viewModelScope.launch {
            sourceClickBus.emit(source)
        }
    }

    override fun onIconClicked(source: Source) {
        viewModelScope.launch {
            iconClickBus.emit(source)
        }
    }

    override fun onImageUriReturned(uri: Uri) {
        viewModelScope.launch {
            onIconSelected(IconPickerResult.UriIcon(uri))
        }
    }

    override fun onClearClicked() {
        viewModelScope.launch {
            onIconSelected(null)
        }
    }

    private fun setupSourceClickListener() = viewModelScope.launch {
        sourceClickBus.debounce(TAP_DEBOUNCE).collect {
            when(it){
                is IconPackSource -> {
                    navigation.navigate(IconPickerFragmentDirections.actionIconPickerFragmentToIconPickerPackFragment(
                        monoConfig.value ?: return@collect, it.iconPack
                    ))
                }
                is Source.File -> {
                    importImageBus.emit(Unit)
                }
                is Source.Apps -> {
                    navigation.navigate(IconPickerFragmentDirections.actionIconPickerFragmentToIconPickerAppsFragment(
                        monoConfig.value ?: return@collect
                    ))
                }
                is Source.LegacyThemedIcons -> {
                    navigation.navigate(IconPickerFragmentDirections.actionIconPickerFragmentToLegacyThemedIconPickerFragment(
                        monoConfig.value ?: return@collect, false
                    ))
                }
                is Source.Lawnicons -> {
                    navigation.navigate(IconPickerFragmentDirections.actionIconPickerFragmentToLegacyThemedIconPickerFragment(
                        monoConfig.value ?: return@collect, true
                    ))
                }
                else -> {
                    //No-op
                }
            }
        }
    }

    private fun setupIconClickListener() = viewModelScope.launch {
        iconClickBus.debounce(TAP_DEBOUNCE).collect {
            when(it) {
                is IconPackSource -> {
                    val iconPackIcon = it.icon?.iconPackIcon ?: return@collect
                    onIconSelected(
                        IconPickerResult.IconPackIcon(
                        iconPackIcon.iconPackPackageName,
                        iconPackIcon.resourceName,
                        iconPackIcon.isAdaptiveIcon
                    ))
                }
                is Source.Apps -> {
                    onIconSelected(PackageIcon(it.packageIcon.applicationIcon))
                }
                is Source.LegacyThemedIcons -> {
                    val resourceId = it.legacyThemedIcon?.resourceId ?: return@collect
                    val resourceName = iconLoaderRepository.getLegacyThemedIconName(it.legacyThemedIcon)
                    onIconSelected(IconPickerResult.LegacyThemedIcon(resourceId, resourceName))
                }
                is Source.Lawnicons -> {
                    val resourceId = it.legacyThemedIcon?.resourceId ?: return@collect
                    val resourceName = iconLoaderRepository.getLawniconName(it.legacyThemedIcon)
                    onIconSelected(IconPickerResult.Lawnicon(resourceId, resourceName))
                }
                else -> {
                    //No-op
                }
            }
        }
    }

    init {
        setupSourceClickListener()
        setupIconClickListener()
    }

}