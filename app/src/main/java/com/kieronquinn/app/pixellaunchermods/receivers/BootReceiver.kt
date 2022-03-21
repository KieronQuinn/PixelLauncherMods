package com.kieronquinn.app.pixellaunchermods.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import com.kieronquinn.app.pixellaunchermods.service.PixelLauncherModsForegroundService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver: BroadcastReceiver(), KoinComponent {

    private val settings by inject<SettingsRepository>()

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if(!settings.shouldLaunchService.getSync()) return
        PixelLauncherModsForegroundService.start(context, true)
    }

}