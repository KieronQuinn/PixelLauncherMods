package com.kieronquinn.app.pixellaunchermods.model.backup

import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteApp
import com.kieronquinn.app.pixellaunchermods.model.room.IconMetadata
import com.kieronquinn.app.pixellaunchermods.model.tweaks.WidgetReplacement
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository

class Backup {

    companion object {
        private const val BACKUP_VERSION = 1
    }

    @SerializedName("version")
    val version = BACKUP_VERSION

    @SerializedName("apps")
    var apps: List<App> = emptyList()

    @SerializedName("shortcuts")
    var shortcuts: List<Shortcut> = emptyList()

    @SerializedName("settings")
    var settings: Settings = Settings()

    class App {
        @SerializedName("component")
        var componentName: String? = null

        @SerializedName("label")
        var label: String? = null

        @SerializedName("icon_color")
        var iconColor: Int? = null

        @SerializedName("icon_type")
        var iconType: RemoteApp.Type? = null

        @SerializedName("icon_metadata")
        var iconMetadata: IconMetadata? = null

        @SerializedName("mono_icon_metadata")
        var monoIconMetadata: IconMetadata? = null
    }

    class Shortcut {
        @SerializedName("intent")
        var intent: String? = null

        @SerializedName("title")
        var title: String? = null

        @SerializedName("icon_metadata")
        var iconMetadata: IconMetadata? = null

        @SerializedName("original_title")
        var originalTitle: String? = null
    }

    class Settings {
        @SerializedName("deferred_restart_mode")
        var deferredRestartMode: SettingsRepository.DeferredRestartMode? = null

        @SerializedName("auto_icon_pack_order")
        var autoIconPackOrder: List<String>? = null

        @SerializedName("hide_clock")
        var hideClock: Boolean? = null

        @SerializedName("suppress_shortcut_listener")
        var suppressShortcutChangeListener: Boolean? = null

        @SerializedName("hidden_components")
        var hiddenComponents: List<String>? = null

        @SerializedName("widget_replacement")
        var widgetReplacement: WidgetReplacement? = null

        @SerializedName("recents_transparency")
        var recentsTransparency: Float? = null
    }

}