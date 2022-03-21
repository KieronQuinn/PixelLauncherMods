package com.kieronquinn.app.pixellaunchermods.repositories

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import com.kieronquinn.app.pixellaunchermods.repositories.AppsRepository.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AppsRepository {

    data class App(val applicationInfo: ApplicationInfo, val label: CharSequence)

    suspend fun getAllApps(): List<App>
    fun getApplicationInfoForPackage(packageName: String): ApplicationInfo?
    fun loadApplicationLabel(applicationInfo: ApplicationInfo): CharSequence

    suspend fun getAllLauncherApps(): List<LauncherActivityInfo>

}

class AppsRepositoryImpl(context: Context): AppsRepository {

    private val packageManager = context.packageManager
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    override suspend fun getAllApps(): List<App> = withContext(Dispatchers.IO) {
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        packageManager.queryIntentActivities(launchIntent, 0).map {
            val applicationInfo = packageManager.getApplicationInfo(it.activityInfo.packageName, 0)
            val label = it.activityInfo.loadLabel(packageManager)
            App(applicationInfo, label)
        }.distinctBy { it.applicationInfo.packageName }.sortedBy { it.label.toString().lowercase() }
    }

    override fun getApplicationInfoForPackage(packageName: String): ApplicationInfo? {
        return try {
            packageManager.getApplicationInfo(packageName, 0)
        }catch (e: PackageManager.NameNotFoundException){
            null
        }
    }

    override fun loadApplicationLabel(applicationInfo: ApplicationInfo): CharSequence {
        return applicationInfo.loadLabel(packageManager)
    }

    override suspend fun getAllLauncherApps(): List<LauncherActivityInfo> = withContext(Dispatchers.IO) {
        launcherApps.getActivityList(null, Process.myUserHandle())
    }

}