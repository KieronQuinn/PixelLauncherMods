package com.kieronquinn.app.pixellaunchermods.repositories

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.kieronquinn.app.pixellaunchermods.BuildConfig
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.model.tweaks.WidgetReplacement
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository.DeferredRestartMode
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository.DeferredRestartMode.INSTANT
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository.PixelLauncherModsSetting
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepositoryImpl.SettingsConverters.SHARED_BOOLEAN
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepositoryImpl.SettingsConverters.SHARED_FLOAT
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepositoryImpl.SettingsConverters.SHARED_INT
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepositoryImpl.SettingsConverters.SHARED_STRING
import com.kieronquinn.app.pixellaunchermods.utils.extensions.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface SettingsRepository {

    //Used internally only to know whether to launch the service on boot - set to true on successful start
    val shouldLaunchService: PixelLauncherModsSetting<Boolean>

    //The current local icon size
    val iconSize: PixelLauncherModsSetting<Int>

    //The mode of when to restart the launcher
    val deferredRestartMode: PixelLauncherModsSetting<DeferredRestartMode>

    //The order of icon packs to use when automatically applying icon packs
    val autoIconPackOrder: PixelLauncherModsSetting<List<String>>

    //Whether to hide the clock on the homescreen
    val hideClock: PixelLauncherModsSetting<Boolean>

    //Transparency of the recents view background colour
    val recentsBackgroundTransparency: PixelLauncherModsSetting<Float>

    //Whether to disable the wallpaper scrim on the home screen
    val disableWallpaperScrim: PixelLauncherModsSetting<Boolean>

    //Whether to disable widget region-based colour picking
    val disableWallpaperRegionColours: PixelLauncherModsSetting<Boolean>

    //Whether to hide Smartspace
    val disableSmartspace: PixelLauncherModsSetting<Boolean>

    //Whether to suppress the shortcut change listener (which can be quite noisy)
    val suppressShortcutChangeListener: PixelLauncherModsSetting<Boolean>

    //Hidden app component names - local storage. Must be in an overlay to change the launcher.
    val hiddenComponents: PixelLauncherModsSetting<List<String>>

    //The position (top or bottom) for the widget replacement to use, or none if it is not enabled.
    val widgetReplacement: PixelLauncherModsSetting<WidgetReplacement>

    //The provider of the proxy widget replacement to use
    val qsbWidgetProvider: PixelLauncherModsSetting<String>

    //The app widget ID of the proxy widget replacement to use
    val qsbWidgetId: PixelLauncherModsSetting<Int>

    //Widget IDs for the preview of the proxy widget in the settings
    val proxyWidgetPreviewIdTop: PixelLauncherModsSetting<Int>
    val proxyWidgetPreviewIdBottom: PixelLauncherModsSetting<Int>

    //Debug: Show toast message with (possible) restart reason from service
    val debugRestartReasonToast: PixelLauncherModsSetting<Boolean>

    suspend fun resetIcons()

    abstract class PixelLauncherModsSetting<T> {
        abstract suspend fun exists(): Boolean
        abstract fun existsSync(): Boolean
        abstract suspend fun set(value: T)
        abstract suspend fun get(): T
        abstract suspend fun getOrNull(): T?
        abstract suspend fun clear()
        abstract fun getSync(): T
        abstract fun getSyncOrNull(): T?
        abstract fun asFlow(): Flow<T>
        abstract fun asFlowNullable(): Flow<T?>
    }

    /**
    *  Helper implementation of [PixelLauncherModsSetting] that takes a regular StateFlow and calls a method
    *  ([onSet]) when [set] is called, allowing for external data to be handled by regular switch
    *  items. [clear] is not implemented, [exists] and [existsSync] will always return true.
    */
    class FakePixelLauncherModsSetting<T>(private val flow: StateFlow<T>, private val onSet: (value: T) -> Unit): PixelLauncherModsSetting<T>() {

        override fun getSync(): T {
            return flow.value
        }

        override fun asFlow(): Flow<T> {
            return flow
        }

        override fun asFlowNullable(): Flow<T?> {
            throw RuntimeException("Not implemented!")
        }

        override suspend fun set(value: T) {
            onSet(value)
        }

        override suspend fun get(): T {
            return flow.value
        }

        override suspend fun getOrNull(): T? {
            return if(exists()){
                get()
            }else{
                null
            }
        }

        override fun getSyncOrNull(): T? {
            return if(existsSync()){
                getSync()
            }else{
                null
            }
        }

        override suspend fun exists(): Boolean {
            return true
        }

        override fun existsSync(): Boolean {
            return true
        }

        override suspend fun clear() {
            throw RuntimeException("Not implemented!")
        }

    }

    /**
     *  The mode for when to restart the Pixel Launcher after changes. While the app is open,
     *  the mode is always [INSTANT] so changes are applied immediately.
     */
    enum class DeferredRestartMode(val titleRes: Int, val contentRes: Int) {
        /**
         *  Disables all restarts, user must restart manually
         */
        DISABLED(R.string.deferred_restart_disabled, R.string.deferred_restart_disabled_content),

        /**
         *  Restart the launcher immediately, even if it is in the foreground
         */
        INSTANT(R.string.deferred_restart_instant_title, R.string.deferred_restart_instant_content),

        /**
         *  Restart the launcher once the Pixel Launcher is background
         */
        BACKGROUND(R.string.deferred_restart_background_title, R.string.deferred_restart_background_content),

        /**
         *  Restart the launcher only when the screen is off
         */
        SCREEN_OFF(R.string.deferred_restart_screen_off_title, R.string.deferred_restart_screen_off_content)
    }

}

class SettingsRepositoryImpl(context: Context): SettingsRepository {

    companion object {
        private const val KEY_SHOULD_LAUNCH_SERVICE = "should_launch_service"
        private const val DEFUALT_SHOULD_LAUNCH_SERVICE = false
        private const val KEY_ICON_SIZE = "icon_size"
        private const val DEFAULT_ICON_SIZE = -1
        private const val KEY_DEFERRED_RESTART_MODE = "restart_mode"
        private val DEFAULT_DEFERRED_RESTART_MODE = DeferredRestartMode.BACKGROUND
        private const val KEY_AUTO_ICON_PACK_ORDER = "auto_icon_pack_order"
        private val DEFAULT_AUTO_ICON_PACK_ORDER = emptyList<String>()
        private const val KEY_HIDE_CLOCK = "hide_clock"
        private const val DEFAULT_HIDE_CLOCK = false
        private const val KEY_RECENTS_BACKGOUND_TRANSPARENCY = "recents_background_transparency"
        private const val DEFAULT_RECENTS_BACKGROUND_TRANSPARENCY = 0f
        private const val KEY_TWEAKS_DISABLE_WALLPAPER_SCRIM = "tweaks_disable_wallpaper_scrim"
        private const val DEFAULT_TWEAKS_DISABLE_WALLPAPER_SCRIM = false
        private const val KEY_TWEAKS_DISABLE_WALLPAPER_REGION_COLOURS = "tweaks_disable_wallpaper_region_colours"
        private const val DEFAULT_TWEAKS_DISABLE_WALLPAPER_REGION_COLOURS = false
        private const val KEY_TWEAKS_DISABLE_SMARTSPACE = "tweaks_disable_smartspace"
        private const val DEFAULT_TWEAKS_DISABLE_SMARTSPACE = false
        private const val KEY_SUPPRESS_SHORTCUT_LISTENER = "suppress_shortcut_listener"
        private const val DEFAULT_SUPPRESS_SHORTCUT_LISTENER = false
        private const val KEY_HIDDEN_COMPONENTS = "hidden_components"
        //Default hidden components from the Pixel Launcher, subject to change in the future
        private val DEFAULT_HIDDEN_COMPONENTS = listOf(
            "com.google.android.googlequicksearchbox/.VoiceSearchActivity",
            "com.google.android.apps.wallpaper/.picker.CategoryPickerActivity",
            "com.google.android.as/com.google.android.apps.miphone.aiai.allapps.main.MainDummyActivity"
        )
        private const val KEY_WIDGET_REPLACEMENT = "widget_replacement"
        private val DEFAULT_WIDGET_REPLACEMENT = WidgetReplacement.NONE
        private const val KEY_QSB_WIDGET_PROVIDER = "qsb_widget_provider"
        private const val KEY_QSB_WIDGET_ID = "qsb_widget_id"
        private const val DEFAULT_QSB_WIDGET_ID = -1
        private const val KEY_PROXY_WIDGET_PREVIEW_ID_TOP = "proxy_widget_preview_id_top"
        private const val DEFAULT_PROXY_WIDGET_PREVIEW_ID_TOP = -1
        private const val KEY_PROXY_WIDGET_PREVIEW_ID_BOTTOM = "proxy_widget_preview_id_bottom"
        private const val DEFAULT_PROXY_WIDGET_PREVIEW_ID_BOTTOM = -1
        private const val KEY_DEBUG_RESTART_REASON_TOAST = "debug_restart_reason_toast"
        private const val DEFAULT_DEBUG_RESTART_REASON_TOAST = false
    }

    override val shouldLaunchService: PixelLauncherModsSetting<Boolean> = PixelLauncherModsSettingImpl(
        KEY_SHOULD_LAUNCH_SERVICE,
        DEFUALT_SHOULD_LAUNCH_SERVICE,
        SHARED_BOOLEAN
    )

    override val iconSize: PixelLauncherModsSetting<Int> = PixelLauncherModsSettingImpl(
        KEY_ICON_SIZE,
        DEFAULT_ICON_SIZE,
        SHARED_INT
    )

    override val deferredRestartMode: PixelLauncherModsSetting<DeferredRestartMode> = PixelLauncherModsSettingImpl(
        KEY_DEFERRED_RESTART_MODE,
        DEFAULT_DEFERRED_RESTART_MODE
    ) { _, key, default -> sharedEnum(key, default) }

    override val autoIconPackOrder: PixelLauncherModsSetting<List<String>> = PixelLauncherModsSettingImpl(
        KEY_AUTO_ICON_PACK_ORDER,
        DEFAULT_AUTO_ICON_PACK_ORDER
    ) { _, key, default -> sharedList(key, default, ::stringListTypeConverter, ::stringListTypeReverseConverter) }

    override val hideClock: PixelLauncherModsSetting<Boolean> = PixelLauncherModsSettingImpl(
        KEY_HIDE_CLOCK,
        DEFAULT_HIDE_CLOCK,
        SHARED_BOOLEAN
    )

    override val recentsBackgroundTransparency: PixelLauncherModsSetting<Float> = PixelLauncherModsSettingImpl(
        KEY_RECENTS_BACKGOUND_TRANSPARENCY,
        DEFAULT_RECENTS_BACKGROUND_TRANSPARENCY,
        SHARED_FLOAT
    )

    override val disableWallpaperScrim: PixelLauncherModsSetting<Boolean> = PixelLauncherModsSettingImpl(
        KEY_TWEAKS_DISABLE_WALLPAPER_SCRIM,
        DEFAULT_TWEAKS_DISABLE_WALLPAPER_SCRIM,
        SHARED_BOOLEAN
    )

    override val disableWallpaperRegionColours: PixelLauncherModsSetting<Boolean> = PixelLauncherModsSettingImpl(
        KEY_TWEAKS_DISABLE_WALLPAPER_REGION_COLOURS,
        DEFAULT_TWEAKS_DISABLE_WALLPAPER_REGION_COLOURS,
        SHARED_BOOLEAN
    )

    override val disableSmartspace: PixelLauncherModsSetting<Boolean> = PixelLauncherModsSettingImpl(
        KEY_TWEAKS_DISABLE_SMARTSPACE,
        DEFAULT_TWEAKS_DISABLE_SMARTSPACE,
        SHARED_BOOLEAN
    )

    override val suppressShortcutChangeListener: PixelLauncherModsSetting<Boolean> = PixelLauncherModsSettingImpl(
        KEY_SUPPRESS_SHORTCUT_LISTENER,
        DEFAULT_SUPPRESS_SHORTCUT_LISTENER,
        SHARED_BOOLEAN
    )

    override val hiddenComponents: PixelLauncherModsSetting<List<String>> = PixelLauncherModsSettingImpl(
        KEY_HIDDEN_COMPONENTS,
        DEFAULT_HIDDEN_COMPONENTS
    ) { _, key, default -> sharedList(key, default, ::stringListTypeConverter, ::stringListTypeReverseConverter) }

    override val widgetReplacement: PixelLauncherModsSetting<WidgetReplacement> = PixelLauncherModsSettingImpl(
        KEY_WIDGET_REPLACEMENT,
        DEFAULT_WIDGET_REPLACEMENT
    ) { _, key, default -> sharedEnum(key, default) }

    override val qsbWidgetProvider: PixelLauncherModsSetting<String> = PixelLauncherModsSettingImpl(
        KEY_QSB_WIDGET_PROVIDER,
        "",
        SHARED_STRING
    )

    override val qsbWidgetId: PixelLauncherModsSetting<Int> = PixelLauncherModsSettingImpl(
        KEY_QSB_WIDGET_ID,
        DEFAULT_QSB_WIDGET_ID,
        SHARED_INT
    )

    override val proxyWidgetPreviewIdTop: PixelLauncherModsSettingImpl<Int> = PixelLauncherModsSettingImpl(
        KEY_PROXY_WIDGET_PREVIEW_ID_TOP,
        DEFAULT_PROXY_WIDGET_PREVIEW_ID_TOP,
        SHARED_INT
    )

    override val proxyWidgetPreviewIdBottom: PixelLauncherModsSettingImpl<Int> = PixelLauncherModsSettingImpl(
        KEY_PROXY_WIDGET_PREVIEW_ID_BOTTOM,
        DEFAULT_PROXY_WIDGET_PREVIEW_ID_BOTTOM,
        SHARED_INT
    )

    override val debugRestartReasonToast: PixelLauncherModsSetting<Boolean> = PixelLauncherModsSettingImpl(
        KEY_DEBUG_RESTART_REASON_TOAST,
        DEFAULT_DEBUG_RESTART_REASON_TOAST,
        SHARED_BOOLEAN
    )

    override suspend fun resetIcons() {
        //Clear settings that will re-produce icons on changes
        autoIconPackOrder.set(emptyList())
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("${BuildConfig.APPLICATION_ID}_prefs", Context.MODE_PRIVATE)
    }

    private fun shared(key: String, default: Boolean) = ReadWriteProperty({
        sharedPreferences.getBoolean(key, default)
    }, {
        sharedPreferences.edit().putBoolean(key, it).commit()
    })

    private fun shared(key: String, default: String) = ReadWriteProperty({
        sharedPreferences.getString(key, default) ?: default
    }, {
        sharedPreferences.edit().putString(key, it).commit()
    })

    private fun shared(key: String, default: Int) = ReadWriteProperty({
        sharedPreferences.getInt(key, default)
    }, {
        sharedPreferences.edit().putInt(key, it).commit()
    })

    private fun shared(key: String, default: Float) = ReadWriteProperty({
        sharedPreferences.getFloat(key, default)
    }, {
        sharedPreferences.edit().putFloat(key, it).commit()
    })

    private fun sharedColor(key: String, unusedDefault: Int) = ReadWriteProperty({
        val rawColor = sharedPreferences.getString(key, "") ?: ""
        if(rawColor.isEmpty()) Integer.MAX_VALUE
        else Color.parseColor(rawColor)
    }, {
        sharedPreferences.edit().putString(key, it.toHexString()).commit()
    })

    object SettingsConverters {
        internal val SHARED_INT: (SettingsRepositoryImpl, String, Int) -> ReadWriteProperty<Any?, Int> =
            SettingsRepositoryImpl::shared
        internal val SHARED_STRING: (SettingsRepositoryImpl, String, String) -> ReadWriteProperty<Any?, String> =
            SettingsRepositoryImpl::shared
        internal val SHARED_BOOLEAN: (SettingsRepositoryImpl, String, Boolean) -> ReadWriteProperty<Any?, Boolean> =
            SettingsRepositoryImpl::shared
        internal val SHARED_FLOAT: (SettingsRepositoryImpl, String, Float) -> ReadWriteProperty<Any?, Float> =
            SettingsRepositoryImpl::shared
        internal val SHARED_COLOR: (SettingsRepositoryImpl, String, Int) -> ReadWriteProperty<Any?, Int> =
            SettingsRepositoryImpl::sharedColor
    }

    inner class PixelLauncherModsSettingImpl<T>(
        private val key: String,
        private val default: T,
        shared: (SettingsRepositoryImpl, String, T) -> ReadWriteProperty<Any?, T>
    ) : PixelLauncherModsSetting<T>() {

        private var rawSetting by shared(this@SettingsRepositoryImpl, key, default)

        override suspend fun exists(): Boolean {
            return withContext(Dispatchers.IO) {
                sharedPreferences.contains(key)
            }
        }

        /**
         *  Should only be used where there is no alternative
         */
        override fun existsSync(): Boolean {
            return runBlocking {
                exists()
            }
        }

        override suspend fun set(value: T) {
            withContext(Dispatchers.IO) {
                rawSetting = value
            }
        }

        override suspend fun get(): T {
            return withContext(Dispatchers.IO) {
                rawSetting ?: default
            }
        }

        override suspend fun getOrNull(): T? {
            return if(exists()){
                get()
            }else null
        }

        override fun getSyncOrNull(): T? {
            return if(existsSync()){
                getSync()
            }else null
        }

        /**
         *  Should only be used where there is no alternative
         */
        override fun getSync(): T {
            return runBlocking {
                get()
            }
        }

        override suspend fun clear() {
            withContext(Dispatchers.IO) {
                sharedPreferences.edit().remove(key).commit()
            }
        }

        override fun asFlow() = callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                trySend(rawSetting ?: default)
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            trySend(rawSetting ?: default)
            awaitClose {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.flowOn(Dispatchers.IO)

        override fun asFlowNullable() = callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                trySend(rawSetting)
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            if(existsSync()) trySend(rawSetting)
            awaitClose {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.flowOn(Dispatchers.IO)

    }

    private inline fun <reified T : Enum<T>> sharedEnum(
        key: String,
        default: Enum<T>
    ): ReadWriteProperty<Any?, T> {
        return object : ReadWriteProperty<Any?, T> {

            override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return java.lang.Enum.valueOf(
                    T::class.java,
                    sharedPreferences.getString(key, default.name)
                )
            }

            override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                sharedPreferences.edit().putString(key, value.name).commit()
            }

        }
    }

    private inline fun <reified T> sharedList(
        key: String,
        default: List<T>,
        crossinline transform: (List<T>) -> String,
        crossinline reverseTransform: (String) -> List<T>
    ) = ReadWriteProperty({
        reverseTransform(sharedPreferences.getString(key, null) ?: transform(default))
    }, {
        sharedPreferences.edit().putString(key, transform(it)).commit()
    })

    private fun stringListTypeConverter(list: List<String>): String {
        if (list.isEmpty()) return ""
        return list.joinToString(",")
    }

    private fun stringListTypeReverseConverter(pref: String): List<String> {
        if (pref.isEmpty()) return emptyList()
        if (!pref.contains(",")) return listOf(pref.trim())
        return pref.split(",")
    }

    private inline fun <T> ReadWriteProperty(
        crossinline getValue: () -> T,
        crossinline setValue: (T) -> Unit
    ): ReadWriteProperty<Any?, T> {
        return object : ReadWriteProperty<Any?, T> {

            override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return getValue.invoke()
            }

            override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                setValue.invoke(value)
            }

        }
    }

}

suspend fun PixelLauncherModsSetting<Boolean>.invert() {
    set(!get())
}