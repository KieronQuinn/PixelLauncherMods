package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlayapply

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.model.tweaks.WidgetReplacement
import com.kieronquinn.app.pixellaunchermods.repositories.OverlayRepository
import com.kieronquinn.app.pixellaunchermods.repositories.OverlayRepository.OverlayConfig
import com.kieronquinn.app.pixellaunchermods.repositories.OverlayRepository.OverlayProgress
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class OverlayApplyViewModel: ViewModel() {

    sealed class State {
        object Applying: State()
        data class Finished(val success: Boolean): State()
    }

    abstract val consoleLines: StateFlow<List<String>>
    abstract val state: StateFlow<State>
    abstract fun setConfig(
        components: Array<String>?,
        widgetReplacement: WidgetReplacement?,
        recentsTransparency: Float?,
        disableWallpaperScrim: Boolean?,
        disableWallpaperRegionColours: Boolean?,
        disableSmartspace: Boolean?
    )
    abstract fun saveLog(outputUri: Uri)
    abstract fun onSaveLogClicked(launcher: ActivityResultLauncher<String>)
    abstract fun onBackPressed()
    abstract fun getSuccessfulState(): Boolean

}

class OverlayApplyViewModelImpl(
    private val overlayRepository: OverlayRepository,
    private val navigation: ContainerNavigation,
    private val settingsRepository: SettingsRepository,
    context: Context
): OverlayApplyViewModel() {

    companion object {
        private const val LOG_FILENAME = "pixel_launcher_mods_overlay_log_%s.txt"
    }
    private val config = MutableStateFlow<OverlayConfig?>(null)
    private val contentResolver = context.contentResolver

    override val consoleLines = MutableStateFlow<List<String>>(emptyList())

    override val state = config.filterNotNull().flatMapLatest {
        overlayRepository.createAndInstallOverlay(it).map { progress ->
            when(progress){
                is OverlayProgress.Line -> {
                    consoleLines.emitLine(progress)
                    State.Applying
                }
                is OverlayProgress.Finished -> {
                    State.Finished(progress.success)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Applying)

    override fun setConfig(
        components: Array<String>?,
        widgetReplacement: WidgetReplacement?,
        recentsTransparency: Float?,
        disableWallpaperScrim: Boolean?,
        disableWallpaperRegionColours: Boolean?,
        disableSmartspace: Boolean?
    ) {
        viewModelScope.launch {
            val overlayComponents = components?.toList() ?: settingsRepository.hiddenComponents.get()
            val overlayWidgetReplacement = widgetReplacement ?: settingsRepository.widgetReplacement.get()
            val transparency = recentsTransparency ?: settingsRepository.recentsBackgroundTransparency.get()
            val disableScrim = disableWallpaperScrim ?: settingsRepository.disableWallpaperScrim.get()
            val disableWallpaperColours = disableWallpaperRegionColours
                ?: settingsRepository.disableWallpaperRegionColours.get()
            val disableSmartspaceBool = disableSmartspace ?: settingsRepository.disableSmartspace.get()
            val config = OverlayConfig(
                overlayComponents,
                overlayWidgetReplacement,
                transparency,
                disableScrim,
                disableWallpaperColours,
                disableSmartspaceBool,
                components != null,
                widgetReplacement != null,
                recentsTransparency != null,
                disableWallpaperScrim != null,
                disableWallpaperRegionColours != null,
                disableSmartspace != null
            )
            this@OverlayApplyViewModelImpl.config.emit(config)
        }
    }

    override fun saveLog(outputUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val lines = consoleLines.value.joinToString("\n")
            contentResolver.openFileDescriptor(outputUri, "w")?.use { fileDescriptor ->
                FileOutputStream(fileDescriptor.fileDescriptor).bufferedWriter().use {
                    it.write(lines)
                    it.flush()
                }
            }
        }
    }

    override fun onSaveLogClicked(launcher: ActivityResultLauncher<String>) {
        launcher.launch(getFilename())
    }

    override fun onBackPressed() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun getSuccessfulState(): Boolean {
        return (state.value as? State.Finished)?.success ?: false
    }

    private suspend fun MutableStateFlow<List<String>>.emitLine(line: OverlayProgress.Line) {
        emit(value + line.line)
    }

    private fun getFilename(): String {
        val timestampFormat = DateTimeFormatter.ofPattern("ddMMyy_HHmmss")
        val timestamp = timestampFormat.format(LocalDateTime.now())
        return String.format(LOG_FILENAME, timestamp)
    }

}