package com.kieronquinn.app.pixellaunchermods.ui.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.repositories.AppStateRepository
import com.kieronquinn.app.pixellaunchermods.service.PixelLauncherModsForegroundService
import com.kieronquinn.app.pixellaunchermods.ui.activities.MainActivityViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.delayPreDrawUntilFlow
import com.kieronquinn.app.pixellaunchermods.widget.BlankWidget
import com.kieronquinn.app.pixellaunchermods.widget.ProxyWidget
import com.kieronquinn.app.pixellaunchermods.work.UpdateCheckWorker
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val appStateRepository by inject<AppStateRepository>()
    private val viewModel by viewModel<MainActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        DynamicColors.applyToActivityIfAvailable(this)
        UpdateCheckWorker.queueCheckWorker(this)
        setContentView(R.layout.activity_main)
        findViewById<View>(android.R.id.content).delayPreDrawUntilFlow(
            viewModel.isAppReady.map { it !is State.Loading },
            lifecycle
        )
        setupAppState()
    }

    private fun setupAppState() {
        viewModel.isAppReady.value.let {
            handleAppState(it)
        }
        lifecycleScope.launchWhenResumed {
            viewModel.isAppReady.filterNotNull().collect {
                handleAppState(it)
            }
        }
    }

    private fun handleAppState(state: State) {
        when(state) {
            is State.Loaded -> {
                ProxyWidget.sendUpdate(this)
                BlankWidget.sendUpdate(this)
                PixelLauncherModsForegroundService.start(this)
            }
            is State.NoRoot -> {
                showNoRootDialog()
            }
            is State.NoPixelLauncher -> {
                showNoPixelLauncherDialog()
            }
            is State.Loading -> {} //no-op
        }
    }

    private fun showNoRootDialog() {
        MaterialAlertDialogBuilder(this).apply {
            setTitle(R.string.no_root_dialog_title)
            setMessage(R.string.no_root_dialog_content)
            setPositiveButton(R.string.no_root_dialog_close) { _, _ ->
                finish()
            }
            setCancelable(false)
        }.show()
    }

    private fun showNoPixelLauncherDialog() {
        MaterialAlertDialogBuilder(this).apply {
            setTitle(R.string.no_pixel_launcher_dialog_title)
            setMessage(R.string.no_pixel_launcher_dialog_content)
            setPositiveButton(R.string.no_pixel_launcher_dialog_close) { _, _ ->
                finish()
            }
            setCancelable(false)
        }.show()
    }

    override fun onResume() {
        super.onResume()
        appStateRepository.onResume()
    }

    override fun onPause() {
        super.onPause()
        appStateRepository.onPause()
    }

}