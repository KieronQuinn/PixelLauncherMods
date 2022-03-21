package com.kieronquinn.app.pixellaunchermods.ui.screens.magiskinfo

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.repositories.OverlayRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class MagiskInfoViewModel: ViewModel() {

    abstract fun onSaveModuleClicked(launcher: ActivityResultLauncher<String>)
    abstract fun saveModule(uri: Uri)

}

class MagiskInfoViewModelImpl(
    private val overlayRepository: OverlayRepository
): MagiskInfoViewModel() {

    override fun onSaveModuleClicked(launcher: ActivityResultLauncher<String>) {
        launcher.launch(overlayRepository.getModuleFilename())
    }

    override fun saveModule(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            overlayRepository.saveModule(uri)
        }
    }

}