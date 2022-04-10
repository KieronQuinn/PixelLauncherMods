package com.kieronquinn.app.pixellaunchermods.repositories

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface HideClockRepository {

    suspend fun setClockVisible(visible: Boolean)

}

class HideClockRepositoryImpl(
    context: Context,
    private val rootServiceRepository: RootServiceRepository
): HideClockRepository {

    companion object {
        const val SETTINGS_KEY_ICON_BLACKLIST = "icon_blacklist"
        private const val ICON_DENYLIST_CLOCK = "clock"
    }

    private val contentResolver = context.contentResolver

    override suspend fun setClockVisible(visible: Boolean) = withContext(Dispatchers.IO) {
        val iconList = getIconDenylist().toMutableList().apply {
            if(!visible) add(ICON_DENYLIST_CLOCK)
        }.joinToString(",")
        rootServiceRepository.runWithRootService {
            it.setStatusBarIconDenylist(iconList)
        }
        Unit
    }

    /**
     *  Gets the icon denylist for the statusbar from Settings.Secure. If the clock is already
     *  denied, it will be ignored so it can be toggled.
     */
    private fun getIconDenylist(): List<String> {
        val blacklist = Settings.Secure.getString(contentResolver,
            SETTINGS_KEY_ICON_BLACKLIST
        ) ?: ""
        val items = if(blacklist.contains(",")){
            blacklist.split(",")
        }else listOf(blacklist)
        return items.filterNot { it == ICON_DENYLIST_CLOCK }
    }

}