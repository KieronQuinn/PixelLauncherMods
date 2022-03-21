package com.kieronquinn.app.pixellaunchermods.repositories

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.Log
import android.util.Xml
import androidx.core.content.res.ResourcesCompat
import com.kieronquinn.app.pixellaunchermods.model.editor.IconPack
import com.kieronquinn.app.pixellaunchermods.model.icon.ApplicationIcon
import com.kieronquinn.app.pixellaunchermods.repositories.IconPackRepository.*
import com.kieronquinn.app.pixellaunchermods.utils.extensions.isAdaptiveIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.FileNotFoundException

interface IconPackRepository {

    suspend fun getAllIconPacks(): List<IconPack>

    suspend fun getIconForComponent(
        iconPackPackageName: String,
        componentName: String
    ): IconPackIcon?

    suspend fun getAllIcons(
        iconPackPackageName: String
    ): List<IconPackDrawableIcon>

    suspend fun getAllComponentIcons(
        iconPackPackageName: String
    ): List<IconPackComponentIcon>

    fun getIcon(icon: IconPackIcon): Drawable?
    fun getIcon(iconPackPackageName: String, iconResource: String): Drawable?

    data class IconPackIconCategory(val name: String)

    data class IconPackComponentIcon(
        override val iconPackPackageName: String,
        override val resources: Resources,
        override val resourceName: String,
        override val isAdaptiveIcon: Boolean,
        val componentName: String
    ): IconPackIcon(iconPackPackageName, resources, resourceName, isAdaptiveIcon)

    data class IconPackDrawableIcon(
        override val iconPackPackageName: String,
        override val resources: Resources,
        override val resourceName: String,
        override val isAdaptiveIcon: Boolean,
        val category: IconPackIconCategory? = null
    ): IconPackIcon(iconPackPackageName, resources, resourceName, isAdaptiveIcon)

    abstract class IconPackIcon(
        open val iconPackPackageName: String,
        open val resources: Resources,
        open val resourceName: String,
        open val isAdaptiveIcon: Boolean
    )

    data class IconPackIconOptions(val iconPackIcon: IconPackIcon, val mono: Boolean)

}

class IconPackRepositoryImpl(
    context: Context
): IconPackRepository {

    companion object {
        private const val APPFILTER = "appfilter"
        private const val DRAWABLE = "drawable"
        private const val APPFILTER_XML = "$APPFILTER.xml"
        private const val DRAWABLE_XML = "$DRAWABLE.xml"
        private val COMPONENT_REGEX = "ComponentInfo\\{(.*)\\}".toRegex()
        private val ICON_PACK_ACTIONS = arrayOf(
            "org.adw.launcher.THEMES",
            "org.adw.launcher.icons.ACTION_PICK_ICON",
            "com.anddoes.launcher.THEME",
            "com.gau.go.launcherex.theme",
            "com.dlto.atom.launcher.THEME",
            "com.phonemetra.turbo.launcher.icons.ACTION_PICK_ICON",
            "com.gridappsinc.launcher.theme.apk_action",
            "ch.deletescape.lawnchair.ICONPACK",
            "com.novalauncher.THEME",
            "home.solo.launcher.free.THEMES",
            "home.solo.launcher.free.ACTION_ICON",
            "com.lge.launcher2.THEME",
            "net.oneplus.launcher.icons.ACTION_PICK_ICON",
            "com.tsf.shell.themes",
            "ginlemon.smartlauncher.THEMES",
            "com.sonymobile.home.ICON_PACK",
            "com.gau.go.launcherex.theme",
            "com.zeroteam.zerolauncher.theme",
            "jp.co.a_tm.android.launcher.icons.ACTION_PICK_ICON",
            "com.vivid.launcher.theme"
        )
    }

    private val packageManager = context.packageManager

    private val createPackageContext = { packageName: String ->
        context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
    }

    private val createPackageResources = { packageName: String ->
        context.packageManager.getResourcesForApplication(packageName)
    }

    override suspend fun getAllIconPacks(): List<IconPack> {
        return withContext(Dispatchers.IO) {
            ICON_PACK_ACTIONS.map {
                packageManager.queryIntentActivities(Intent(it), 0)
            }.flatten().map {

                IconPack(
                    it.activityInfo.packageName,
                    it.loadLabel(packageManager),
                    ApplicationIcon(
                        packageManager.getApplicationInfo(it.activityInfo.packageName, 0),
                        true
                    ),
                    getExternalIntent(it.activityInfo.packageName)
                )
            }.distinctBy {
                it.packageName
            }
        }
    }

    private fun getExternalIntent(packageName: String): Intent? {
        val resolveInfo = packageManager.queryIntentActivities(
            createExternalIntent(packageName), 0
        ).firstOrNull()
        return createExternalIntent(packageName, resolveInfo?.activityInfo?.name ?: return null)
    }

    private fun createExternalIntent(packageName: String, activityName: String? = null): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            `package` = packageName
            type = "image/*"
            if(activityName != null) {
                component = ComponentName(packageName, activityName)
            }
        }
    }

    override suspend fun getIconForComponent(
        iconPackPackageName: String,
        componentName: String
    ): IconPackIcon? {
        return withContext(Dispatchers.IO) {
            getAppFilter(iconPackPackageName){
                it == componentName
            }.firstOrNull()
        }
    }

    override suspend fun getAllIcons(iconPackPackageName: String): List<IconPackDrawableIcon> {
        return withContext(Dispatchers.IO) {
            getDrawableOrAppFilter(iconPackPackageName)
        }
    }

    override suspend fun getAllComponentIcons(iconPackPackageName: String): List<IconPackComponentIcon> {
        return withContext(Dispatchers.IO) {
            getAppFilter(iconPackPackageName)
        }
    }

    override fun getIcon(icon: IconPackIcon): Drawable? {
        return getIcon(icon.resources, icon.resourceName, icon.iconPackPackageName)
    }

    override fun getIcon(iconPackPackageName: String, iconResource: String): Drawable? {
        val resources = createPackageResources(iconPackPackageName)
        return getIcon(resources, iconResource, iconPackPackageName)
    }

    private fun getIcon(resources: Resources, resourceName: String, iconPackPackageName: String): Drawable? {
        val id = resources.getIdentifier(
            resourceName,
            "drawable",
            iconPackPackageName
        )
        if(id == 0) return null
        return try {
            ResourcesCompat.getDrawable(resources, id, null)
        }catch (e: Resources.NotFoundException){
            null
        }
    }

    private fun getAppFilter(
        iconPackPackageName: String,
        componentNameFilter: ((componentName: String) -> Boolean) = { true }
    ): List<IconPackComponentIcon> {
        val packageContext = createPackageContext(iconPackPackageName)
        val packageResources = createPackageResources(iconPackPackageName)
        val appFilterResId = packageResources.getIdentifier(
            APPFILTER,
            "xml",
            iconPackPackageName
        )
        val parser = if(appFilterResId == 0) {
            try {
                packageContext.assets.open(APPFILTER_XML)
            }catch (e: FileNotFoundException){
                return emptyList()
            }.run {
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(this, null)
                parser
            }
        }else{
            packageResources.getXml(appFilterResId)
        }
        return try {
            parser.parseAppFilter(
                iconPackPackageName,
                packageResources,
                componentNameFilter
            )
        }catch (e: XmlPullParserException) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun getDrawableOrAppFilter(
        iconPackPackageName: String
    ): List<IconPackDrawableIcon> {
        //Get the drawable map or fall back to an uncategorised appfilter if required
        return getDrawable(iconPackPackageName) ?: getAppFilter(iconPackPackageName).map {
            IconPackDrawableIcon(it.iconPackPackageName, it.resources, it.resourceName, it.isAdaptiveIcon)
        }.distinctBy { it.resourceName }.sortedBy { it.resourceName.lowercase() }
    }

    private fun getDrawable(
        iconPackPackageName: String
    ): List<IconPackDrawableIcon>? {
        val packageContext = createPackageContext(iconPackPackageName)
        val packageResources = createPackageResources(iconPackPackageName)
        val appFilterResId = packageResources.getIdentifier(
            DRAWABLE,
            "xml",
            iconPackPackageName
        )
        val parser = if(appFilterResId == 0){
            try {
                packageContext.assets.open(DRAWABLE_XML)
            }catch (e: FileNotFoundException){
                return null
            }.run {
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(this, null)
                parser
            }
        }else{
            packageResources.getXml(appFilterResId)
        }
        return try {
            parser.parseDrawable(iconPackPackageName, packageResources)
        }catch (e: XmlPullParserException) {
            null
        }?.distinctBy { (it.category?.name ?: "") + it.resourceName }
    }

    private fun XmlPullParser.parseAppFilter(
        packageName: String,
        resources: Resources,
        componentNameFilter: (componentName: String) -> Boolean
    ): List<IconPackComponentIcon> {
        val icons = ArrayList<IconPackComponentIcon>()
        while(eventType != XmlPullParser.END_DOCUMENT){
            if(eventType == XmlPullParser.START_TAG && name == "item"){
                val component = getAttributeValue(null, "component")
                val parsedComponent = component?.let {
                    COMPONENT_REGEX.find(component)?.groupValues?.get(1)
                }
                if(parsedComponent != null && componentNameFilter(parsedComponent)){
                    val drawable = getAttributeValue(null, "drawable")
                    if(drawable == null) {
                        next()
                        continue
                    }
                    val identifier = resources.getIdentifier(drawable, "drawable", packageName)
                    if(identifier == 0) {
                        next()
                        continue
                    }
                    if(drawable != null){
                        icons.add(
                            IconPackComponentIcon(
                                packageName,
                                resources,
                                drawable,
                                resources.isAdaptiveIcon(identifier),
                                parsedComponent
                            )
                        )
                    }
                }
            }
            next()
        }
        return icons
    }

    private fun XmlPullParser.parseDrawable(
        packageName: String,
        resources: Resources
    ): List<IconPackDrawableIcon> {
        val icons = ArrayList<IconPackDrawableIcon>()
        var category: IconPackIconCategory? = null
        while(eventType != XmlPullParser.END_DOCUMENT){
            if(eventType == XmlPullParser.START_TAG){
                when(name){
                    "category" -> category = IconPackIconCategory(
                        getAttributeValue(null, "title")
                    )
                    "item" -> {
                        val drawable = getAttributeValue(null, "drawable")
                        if(drawable == null) {
                            next()
                            continue
                        }
                        val identifier = resources.getIdentifier(drawable, "drawable", packageName)
                        if(identifier == 0) {
                            next()
                            continue
                        }
                        icons.add(IconPackDrawableIcon(
                            packageName,
                            resources,
                            drawable,
                            resources.isAdaptiveIcon(identifier),
                            category
                        ))
                    }
                }
            }
            next()
        }
        return icons
    }

}