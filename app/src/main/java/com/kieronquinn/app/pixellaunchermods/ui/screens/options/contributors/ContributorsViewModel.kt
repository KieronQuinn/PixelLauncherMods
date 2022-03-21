package com.kieronquinn.app.pixellaunchermods.ui.screens.options.contributors

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel
import kotlinx.coroutines.launch

abstract class ContributorsViewModel: BaseSettingsViewModel() {

    abstract fun onLinkClicked(url: String)

}

class ContributorsViewModelImpl(
    private val navigation: ContainerNavigation
): ContributorsViewModel() {

    override fun onLinkClicked(url: String) {
        viewModelScope.launch {
            navigation.navigate(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            })
        }
    }

}