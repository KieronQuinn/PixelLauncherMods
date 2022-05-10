package com.kieronquinn.app.pixellaunchermods.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Xml
import androidx.core.os.BuildCompat
import com.android.apksigner.ApkSignerTool
import com.kieronquinn.app.pixellaunchermods.PIXEL_LAUNCHER_PACKAGE_NAME
import com.kieronquinn.app.pixellaunchermods.model.tweaks.WidgetReplacement
import com.kieronquinn.app.pixellaunchermods.repositories.OverlayRepository.OverlayConfig
import com.kieronquinn.app.pixellaunchermods.repositories.OverlayRepository.OverlayProgress
import com.kieronquinn.app.pixellaunchermods.utils.extensions.*
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

interface OverlayRepository {

    sealed class OverlayProgress {
        data class Line(val line: String): OverlayProgress()
        data class Finished(val success: Boolean): OverlayProgress()
    }

    data class OverlayConfig(
        val components: List<String>,
        val widgetReplacement: WidgetReplacement,
        val saveComponents: Boolean,
        val saveWidgetReplacement: Boolean
    )


    /**
     *  Checks if the overlay is installed, via the root service. Will still return true if it
     *  is not enabled.
     */
    suspend fun isOverlayInstalled(): Boolean

    /**
     *  Checks if the overlay is enabled, via the root service.
     */
    suspend fun isOverlayEnabled(): Boolean

    /**
     *  Enables the overlay if needed
     */
    suspend fun enableOverlay()

    /**
     *  Creates and installs an overlay for the given config
     */
    fun createAndInstallOverlay(config: OverlayConfig): Flow<OverlayProgress>

    /**
     *  Gets the current list of filtered components from the Pixel Launcher, enabling the overlay
     *  if needed
     */
    suspend fun getFilteredComponents(): Array<String>

    suspend fun getWidgetReplacement(): WidgetReplacement

    suspend fun saveModule(toUri: Uri)

    fun getModuleFilename(): String

}

class OverlayRepositoryImpl(
    private val applicationContext: Context,
    private val rootServiceRepository: RootServiceRepository,
    private val settingsRepository: SettingsRepository
): OverlayRepository {

    companion object {
        private const val MODULE_FILENAME = "pixel_launcher_mods_overlay_module_%s.zip"

        private const val OVERLAY_RESOURCE_NAME_HIDDEN_COMPONENTS = "filtered_components"
        private const val OVERLAY_RESOURCE_NAME_QSB_SMARTSPACE = "search_container_workspace"
        private const val OVERLAY_RESOURCE_NAME_QSB_SEARCH = "search_container_hotseat"
        private const val OVERLAY_RESOURCE_REPLACEMENT_SEARCH_TERM = "QsbContainerView"
    }

    private val cacheDir = applicationContext.cacheDir
    private val overlayCacheDir = File(cacheDir, "overlay")
    private val assetManager = applicationContext.assets
    private val contentResolver = applicationContext.contentResolver
    private val buildDir = File(overlayCacheDir, "build")
    private val inputDir = File(overlayCacheDir, "input")
    private val outputDir = File(overlayCacheDir, "output")
    private val outputApk = File(outputDir, "overlay-unaligned.apk")
    private val outputAlignedApk = File(outputDir, "overlay.apk")
    private val resDir = File(inputDir, "res")
    private val intermediatesDir = File(overlayCacheDir, "intermediates")
    private val keystore = File(buildDir, "plm.keystore.bk1.keystore")

    //Native libraries are actually binaries
    private val aapt =
        File(applicationContext.applicationInfo.nativeLibraryDir, "libaapt2.so")
    private val zipalign =
        File(applicationContext.applicationInfo.nativeLibraryDir, "libzipalign.so")

    override fun createAndInstallOverlay(config: OverlayConfig) = flow {
        val buildResult = buildOverlay(this, config)
        val installResult = if(buildResult){
            installOverlay(this)
        }else false
        if(installResult) {
            enableOverlay()
        }
        clean()
        val result = buildResult && installResult
        if(result){
            println("Done!")
        }else{
            println("Failed to create overlay. Build successful: $buildResult, install successful: $installResult.")
        }
        rootServiceRepository.runWithRootService {
            it.restartLauncherImmediately()
        }
        val wasSuccessful = buildResult && installResult
        //Save the new settings now the config has applied, if it was successful
        if(wasSuccessful){
            updateSettings(config)
        }
        emit(OverlayProgress.Finished(wasSuccessful))
    }.flowOn(Dispatchers.IO)

    private suspend fun buildOverlay(emitter: FlowCollector<OverlayProgress.Line>, config: OverlayConfig): Boolean {
        overlayCacheDir.mkdirs()
        extractBuildTools()
        emitter.println("Building XML…")
        writeValues(config.components, config.widgetReplacement)
        writeSearchContainer(config.widgetReplacement)
        emitter.println("Compiling resources…")
        if(!emitter.runAndEmit(::compileResources)) return false
        emitter.println("Building overlay APK…")
        if(!emitter.runAndEmit { linkApk(config) }) return false
        emitter.println("Aligning overlay APK…")
        if(!emitter.runAndEmit(::alignApk)) return false
        emitter.println("Signing overlay APK…")
        signApk()
        return true
    }

    private suspend fun installOverlay(emitter: FlowCollector<OverlayProgress.Line>): Boolean {
        emitter.println("Installing overlay APK…")
        return emitter.runAndEmit(::installApk)
    }

    private suspend fun FlowCollector<OverlayProgress.Line>.runAndEmit(
        result: suspend () -> Pair<Boolean, List<String>>
    ): Boolean {
        result().let {
            it.second.forEach { line -> println(line) }
            return it.first
        }
    }

    private suspend fun FlowCollector<OverlayProgress.Line>.println(line: String) {
        emit(OverlayProgress.Line(line))
    }

    private fun extractBuildTools() {
        assetManager.copyRecursively("overlay/build", buildDir)
    }

    private fun writeValues(hiddenComponents: List<String>, widgetReplacement: WidgetReplacement) {
        //Not disabled when empty as we need to apply *something*
        val valuesDir = File(resDir, "values")
        valuesDir.mkdirs()
        val xml = Xml.newSerializer()
        val values = xml.document {
            element("resources") {
                element("string-array") {
                    attribute("name", OVERLAY_RESOURCE_NAME_HIDDEN_COMPONENTS)
                    hiddenComponents.forEach {
                        element("item", it)
                    }
                }
                if(widgetReplacement == WidgetReplacement.TOP) {
                    //Hide existing smartspace
                    element("dimen", "0dp") {
                        attribute("name", "enhanced_smartspace_height")
                    }
                }
            }
        }
        val valuesXml = File(valuesDir, "values.xml")
        valuesXml.writeText(values)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun writeSearchContainer(widgetReplacement: WidgetReplacement) {
        if(widgetReplacement == WidgetReplacement.NONE) {
            //This option is disabled
            return
        }
        val layoutDir = File(resDir, "layout")
        layoutDir.mkdirs()
        val xml = Xml.newSerializer()
        val layout = xml.document {
            //If we're replacing the top we have to keep Smartspace's container, otherwise can get away with a regular FrameLayout
            val rootTag = if(widgetReplacement == WidgetReplacement.TOP){
                "com.google.android.apps.nexuslauncher.qsb.SmartspaceViewContainer"
            }else{
                "FrameLayout"
            }
            element(rootTag) {
                attribute("xmlns:android", "http://schemas.android.com/apk/res/android")
                attribute("android:orientation", "vertical")
                attribute("android:layout_width", "match_parent")
                attribute("android:layout_height", "0dp")
                attribute("android:padding", "0dp")

                element("fragment") {
                    attribute("android:name", "com.android.launcher3.qsb.QsbContainerView\$QsbFragment")
                    attribute("android:layout_width", "match_parent")
                    attribute("android:tag", "qsb_view")
                    attribute("android:layout_height", "match_parent")
                }
            }
        }
        val resourceName = widgetReplacement.toResourceName() ?: return
        val layoutXml = File(layoutDir, "$resourceName.xml")
        layoutXml.writeText(layout)
    }

    private fun compileResources(): Pair<Boolean, List<String>> {
        intermediatesDir.mkdirs()
        val linkOutput = ArrayList<String>()
        val result = runCommands(
            linkOutput,
            "${aapt.absolutePath} compile --dir ${resDir.absolutePath} -o ${intermediatesDir.absolutePath}/",
        )
        return Pair(result, linkOutput)
    }

    private fun linkApk(config: OverlayConfig): Pair<Boolean, List<String>> {
        outputDir.mkdirs()
        val linkOutput = ArrayList<String>()
        val widgetReplacement = config.widgetReplacement
        val command = StringBuffer().apply {
            //Add always-used header
            append("${aapt.absolutePath} link -o ${outputDir.name}/${outputApk.name}")
            append(" -I ${buildDir.name}/android-manifest.jar")
            if(config.components.isNotEmpty()){
                //Add values for component override
                append(" ${intermediatesDir.name}/values_values.arsc.flat")
            }
            if(widgetReplacement != WidgetReplacement.NONE) {
                //Add widget layout overlay
                val resourceName = widgetReplacement.toResourceName() ?: return Pair(
                    false, listOf("Invalid widget replacement ${widgetReplacement.name}")
                )
                append(" ${intermediatesDir.name}/layout_$resourceName.xml.flat")
            }
            //Add manifest
            append(" --manifest ${buildDir.name}/AndroidManifest.xml")
        }
        val result = runCommands(
            linkOutput,
            command.toString()
        )
        return Pair(result, linkOutput)
    }

    private fun alignApk(): Pair<Boolean, List<String>> {
        val alignOutput = ArrayList<String>()
        val result = runCommands(
            alignOutput,
            "${zipalign.absolutePath} -p -f -v 4 ${outputDir.name}/${outputApk.name} ${outputDir.name}/${outputAlignedApk.name}"
        )
        return Pair(result, alignOutput)
    }

    private fun signApk() {
        val args = arrayOf(
            "sign",
            "--ks",
            keystore.absolutePath,
            "--ks-key-alias",
            "plm",
            "--ks-pass",
            "pass:android",
            "--key-pass",
            "pass:android",
            outputAlignedApk.absolutePath
        )
        ApkSignerTool.main(args)
    }

    private fun runCommands(output: ArrayList<String>, vararg commands: String): Boolean {
        val shell = Shell.Builder.create().apply {
            setFlags(Shell.FLAG_NON_ROOT_SHELL or Shell.FLAG_REDIRECT_STDERR)
        }.build()
        val result = shell.newJob().add(
            "sh",
            "cd ${overlayCacheDir.absolutePath}",
            *commands
        ).to(output).exec()
        return result.isSuccess
    }

    private fun installApk(): Pair<Boolean, List<String>> {
        val output = ArrayList<String>()
        val shell = Shell.Builder.create().apply {
            setFlags(Shell.FLAG_REDIRECT_STDERR)
        }.build()
        val result = shell.newJob().add(
            "pm install -r ${outputAlignedApk.absolutePath}"
        ).to(output).exec()
        return Pair(result.isSuccess, output)
    }

    private fun clean() {
        overlayCacheDir.deleteRecursively()
        //Delete leftover apksigner files
        cacheDir.listFiles()?.filter { it.name.startsWith("apksigner") }?.forEach {
            it.delete()
        }
    }

    private suspend fun updateSettings(config: OverlayConfig) {
        if(config.saveComponents){
            settingsRepository.hiddenComponents.set(config.components)
        }
        if(config.saveWidgetReplacement){
            settingsRepository.widgetReplacement.set(config.widgetReplacement)
            //Enable the remote setting too
            rootServiceRepository.runWithRootService {
                it.setSearchWidgetPackageEnabled(config.widgetReplacement != WidgetReplacement.NONE)
            }
        }
    }

    override suspend fun getFilteredComponents(): Array<String> = withContext(Dispatchers.IO) {
        val resources = applicationContext.createPixelLauncherResources()
        val isEnabled = rootServiceRepository.runWithRootService {
            it.isOverlayEnabled
        }
        if(isEnabled == false){
            enableOverlay()
        }
        val filteredComponentsIdentifier = resources.getIdentifier(
            OVERLAY_RESOURCE_NAME_HIDDEN_COMPONENTS, "array", PIXEL_LAUNCHER_PACKAGE_NAME
        )
        val remoteFiltered = resources.getStringArray(filteredComponentsIdentifier)
        val localFiltered = settingsRepository.hiddenComponents.get()
        (remoteFiltered + localFiltered).toSet().toTypedArray()
    }

    override suspend fun enableOverlay() {
        withContext(Dispatchers.IO) {
            rootServiceRepository.runWithRootService {
                it.enableOverlay()
                it.restartLauncherImmediately()
            }
        }
    }

    override suspend fun isOverlayEnabled(): Boolean = withContext(Dispatchers.IO) {
        rootServiceRepository.runWithRootService { it.isOverlayEnabled } ?: false
    }

    override suspend fun isOverlayInstalled(): Boolean = withContext(Dispatchers.IO) {
        rootServiceRepository.runWithRootService { it.isOverlayInstalled } ?: false
    }

    override suspend fun getWidgetReplacement(): WidgetReplacement = withContext(Dispatchers.IO) {
        val resources = applicationContext.createPixelLauncherResources()
        val hotseatResourceIdentifier = resources.getIdentifier(
            OVERLAY_RESOURCE_NAME_QSB_SEARCH, "layout", PIXEL_LAUNCHER_PACKAGE_NAME
        )
        if(resources.rawResourceContains(hotseatResourceIdentifier, OVERLAY_RESOURCE_REPLACEMENT_SEARCH_TERM)){
            return@withContext WidgetReplacement.BOTTOM
        }
        val smartspaceResourceIdentifier = resources.getIdentifier(
            OVERLAY_RESOURCE_NAME_QSB_SMARTSPACE, "layout", PIXEL_LAUNCHER_PACKAGE_NAME
        )
        if(resources.rawResourceContains(smartspaceResourceIdentifier, OVERLAY_RESOURCE_REPLACEMENT_SEARCH_TERM)){
            return@withContext WidgetReplacement.TOP
        }
        WidgetReplacement.NONE
    }

    override suspend fun saveModule(toUri: Uri) = withContext(Dispatchers.IO) {
        val module = assetManager.open("overlay/module.zip")
        contentResolver.openFileDescriptor(toUri, "w")?.use { fileDescriptor ->
            FileOutputStream(fileDescriptor.fileDescriptor).use {
                module.copyTo(it)
                it.flush()
            }
        }
        Unit
    }

    override fun getModuleFilename(): String {
        val timestampFormat = DateTimeFormatter.ofPattern("ddMMyy_HHmmss")
        val timestamp = timestampFormat.format(LocalDateTime.now())
        return String.format(MODULE_FILENAME, timestamp)
    }

    private fun WidgetReplacement.toResourceName(): String? {
        return when(this){
            WidgetReplacement.TOP -> OVERLAY_RESOURCE_NAME_QSB_SMARTSPACE
            WidgetReplacement.BOTTOM -> OVERLAY_RESOURCE_NAME_QSB_SEARCH
            else -> null
        }
    }

}