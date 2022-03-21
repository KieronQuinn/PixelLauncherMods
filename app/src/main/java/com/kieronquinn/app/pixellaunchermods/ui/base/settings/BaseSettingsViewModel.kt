package com.kieronquinn.app.pixellaunchermods.ui.base.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository

abstract class BaseSettingsViewModel : ViewModel() {

    sealed class SettingsItem(
        val type: SettingsItemType,
        open val isVisible: () -> Boolean,
        open val isEnabled: () -> Boolean
    ) {

        data class Text(
            @DrawableRes val icon: Int,
            @StringRes val titleRes: Int? = null,
            val title: CharSequence? = null,
            @StringRes val contentRes: Int? = null,
            val content: (() -> CharSequence)? = null,
            val onClick: (() -> Unit)? = null,
            val linkClicked: ((String) -> Unit)? = null,
            override val isVisible: () -> Boolean = { true },
            override val isEnabled: () -> Boolean = { true }
        ) : SettingsItem(SettingsItemType.TEXT, isVisible, isEnabled)

        data class Switch(
            @DrawableRes val icon: Int,
            @StringRes val titleRes: Int? = null,
            val title: CharSequence? = null,
            @StringRes val contentRes: Int? = null,
            val content: (() -> CharSequence)? = null,
            val setting: SettingsRepository.PixelLauncherModsSetting<Boolean>,
            override val isVisible: () -> Boolean = { true },
            override val isEnabled: () -> Boolean = { true }
        ) : SettingsItem(SettingsItemType.SWITCH, isVisible, isEnabled)

        data class About(
            val onContributorsClicked: () -> Unit,
            val onDonateClicked: () -> Unit,
            val onGitHubClicked: () -> Unit,
            val onTwitterClicked: () -> Unit,
            val onXdaClicked: () -> Unit,
            val onLibrariesClicked: () -> Unit
        ): SettingsItem(SettingsItemType.ABOUT, { true }, { true })

        data class Header(
            @StringRes val titleRes: Int
        ): SettingsItem(SettingsItemType.HEADER, { true }, { true })

        enum class SettingsItemType {
            TEXT, SWITCH, ABOUT, HEADER;

            companion object {
                fun fromViewType(type: Int): SettingsItemType {
                    return values()[type]
                }
            }

        }

    }

}