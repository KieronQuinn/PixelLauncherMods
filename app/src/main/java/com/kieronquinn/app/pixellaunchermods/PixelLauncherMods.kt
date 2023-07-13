package com.kieronquinn.app.pixellaunchermods

import android.app.Application
import androidx.core.content.res.ResourcesCompat
import com.google.gson.Gson
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigationImpl
import com.kieronquinn.app.pixellaunchermods.components.navigation.RootNavigation
import com.kieronquinn.app.pixellaunchermods.components.navigation.RootNavigationImpl
import com.kieronquinn.app.pixellaunchermods.model.room.IconMetadata
import com.kieronquinn.app.pixellaunchermods.model.room.getRoomDatabase
import com.kieronquinn.app.pixellaunchermods.repositories.AppStateRepository
import com.kieronquinn.app.pixellaunchermods.repositories.AppStateRepositoryImpl
import com.kieronquinn.app.pixellaunchermods.repositories.AppWidgetRepository
import com.kieronquinn.app.pixellaunchermods.repositories.AppWidgetRepositoryImpl
import com.kieronquinn.app.pixellaunchermods.repositories.AppsRepository
import com.kieronquinn.app.pixellaunchermods.repositories.AppsRepositoryImpl
import com.kieronquinn.app.pixellaunchermods.repositories.BackupRestoreRepository
import com.kieronquinn.app.pixellaunchermods.repositories.BackupRestoreRepositoryImpl
import com.kieronquinn.app.pixellaunchermods.repositories.DatabaseRepository
import com.kieronquinn.app.pixellaunchermods.repositories.DatabaseRepositoryImpl
import com.kieronquinn.app.pixellaunchermods.repositories.HideClockRepository
import com.kieronquinn.app.pixellaunchermods.repositories.HideClockRepositoryImpl
import com.kieronquinn.app.pixellaunchermods.repositories.IconLoaderRepository
import com.kieronquinn.app.pixellaunchermods.repositories.IconLoaderRepositoryImpl
import com.kieronquinn.app.pixellaunchermods.repositories.IconPackRepository
import com.kieronquinn.app.pixellaunchermods.repositories.IconPackRepositoryImpl
import com.kieronquinn.app.pixellaunchermods.repositories.OverlayRepository
import com.kieronquinn.app.pixellaunchermods.repositories.OverlayRepositoryImpl
import com.kieronquinn.app.pixellaunchermods.repositories.ProxyAppWidgetRepository
import com.kieronquinn.app.pixellaunchermods.repositories.ProxyAppWidgetRepositoryImpl
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepositoryImpl
import com.kieronquinn.app.pixellaunchermods.repositories.RootServiceRepository
import com.kieronquinn.app.pixellaunchermods.repositories.RootServiceRepositoryImpl
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepositoryImpl
import com.kieronquinn.app.pixellaunchermods.repositories.UpdateRepository
import com.kieronquinn.app.pixellaunchermods.repositories.UpdateRepositoryImpl
import com.kieronquinn.app.pixellaunchermods.ui.activities.MainActivityViewModel
import com.kieronquinn.app.pixellaunchermods.ui.activities.MainActivityViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.apps.AppsViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.apps.AppsViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.apps.editor.AppEditorViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.apps.editor.AppEditorViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.autoiconpack.AutoIconPackViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.autoiconpack.AutoIconPackViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.autoiconpack.apply.AutoIconPackApplyViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.autoiconpack.apply.AutoIconPackApplyViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore.BackupRestoreViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore.BackupRestoreViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore.backup.BackupRestoreBackupViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore.backup.BackupRestoreBackupViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore.restore.BackupRestoreRestoreViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore.restore.BackupRestoreRestoreViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.container.ContainerViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.container.ContainerViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.IconPickerViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.IconPickerViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.apps.IconPickerAppsViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.apps.IconPickerAppsViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.legacythemed.LegacyThemedIconPickerViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.legacythemed.LegacyThemedIconPickerViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.pack.IconPickerPackViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.pack.IconPickerPackViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.magiskinfo.MagiskInfoViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.magiskinfo.MagiskInfoViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.options.OptionsViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.options.OptionsViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.options.advanced.OptionsAdvancedViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.options.advanced.OptionsAdvancedViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.options.contributors.ContributorsViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.options.contributors.ContributorsViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.options.reapply.OptionsReapplyViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.options.reapply.OptionsReapplyViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.reset.ResetInfoViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.reset.ResetInfoViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.reset.apply.ResetApplyViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.reset.apply.ResetApplyViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.shortcuts.ShortcutsViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.shortcuts.ShortcutsViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.shortcuts.editor.ShortcutEditorViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.shortcuts.editor.ShortcutEditorViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.TweaksViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.TweaksViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.deferredrestart.DeferredRestartViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.deferredrestart.DeferredRestartViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.hideapps.HideAppsViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.hideapps.HideAppsViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlay.OverlayTweaksViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlay.OverlayTweaksViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlayapply.OverlayApplyViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlayapply.OverlayApplyViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement.WidgetReplacementViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement.WidgetReplacementViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement.widgetpicker.WidgetReplacementPickerViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement.widgetpicker.WidgetReplacementPickerViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetresize.WidgetResizeViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetresize.WidgetResizeViewModelImpl
import com.kieronquinn.app.pixellaunchermods.ui.screens.update.SettingsUpdateViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.update.SettingsUpdateViewModelImpl
import com.topjohnwu.superuser.Shell
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.lsposed.hiddenapibypass.HiddenApiBypass

class PixelLauncherMods: Application() {

    private val repositories = module {
        single<AppStateRepository> { AppStateRepositoryImpl() }
        single<RootServiceRepository> { RootServiceRepositoryImpl(get()) }
        single<RemoteAppsRepository>(createdAtStart = true) { RemoteAppsRepositoryImpl(get(), get(), get(), get(), get(), get(), get()) }
        single<IconPackRepository> { IconPackRepositoryImpl(get()) }
        single<IconLoaderRepository> { IconLoaderRepositoryImpl(get(), get()) }
        single<AppsRepository> { AppsRepositoryImpl(get()) }
        single<DatabaseRepository> { DatabaseRepositoryImpl(get()) }
        single<SettingsRepository> { SettingsRepositoryImpl(get()) }
        single<AppWidgetRepository> { AppWidgetRepositoryImpl(get(), get()) }
        single<OverlayRepository> { OverlayRepositoryImpl(get(), get(), get()) }
        single<ProxyAppWidgetRepository> { ProxyAppWidgetRepositoryImpl(get(), get()) }
        single<BackupRestoreRepository> { BackupRestoreRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
        single<UpdateRepository> { UpdateRepositoryImpl() }
        single<HideClockRepository> { HideClockRepositoryImpl(get(), get()) }
    }

    private val singles = module {
        single { getRoomDatabase(this@PixelLauncherMods) }
        single { createGson() }
        single<RootNavigation> { RootNavigationImpl() }
        single { createMarkwon() }
        single<ContainerNavigation> { ContainerNavigationImpl() }
    }

    private val viewModels = module {
        viewModel<MainActivityViewModel> { MainActivityViewModelImpl(get(), get(), get()) }
        viewModel<ContainerViewModel> { ContainerViewModelImpl(get(), get(), get()) }
        viewModel<AppsViewModel> { AppsViewModelImpl(get(), get(), get()) }
        viewModel<AppEditorViewModel> { AppEditorViewModelImpl(get(), get(), get(), get()) }
        viewModel<IconPickerViewModel> { IconPickerViewModelImpl(get(), get(), get(), get(), get()) }
        viewModel<IconPickerPackViewModel> { IconPickerPackViewModelImpl(get(), get(), get()) }
        viewModel<IconPickerAppsViewModel> { IconPickerAppsViewModelImpl(get(), get(), get()) }
        viewModel<LegacyThemedIconPickerViewModel> { LegacyThemedIconPickerViewModelImpl(get(), get(), get()) }
        viewModel<ShortcutsViewModel> { ShortcutsViewModelImpl(get(), get(), get()) }
        viewModel<ShortcutEditorViewModel> { ShortcutEditorViewModelImpl(get(), get(), get(), get()) }
        viewModel<AutoIconPackViewModel> { AutoIconPackViewModelImpl(get(), get(), get()) }
        viewModel<AutoIconPackApplyViewModel> { AutoIconPackApplyViewModelImpl(get(), get(), get()) }
        viewModel<TweaksViewModel> { TweaksViewModelImpl(get(), get(), get(), get()) }
        viewModel<WidgetResizeViewModel> { WidgetResizeViewModelImpl(get(), get(), get()) }
        viewModel<HideAppsViewModel>{ HideAppsViewModelImpl(get(), get(), get()) }
        viewModel<OverlayApplyViewModel> { OverlayApplyViewModelImpl(get(), get(), get(), get()) }
        viewModel<WidgetReplacementViewModel> { WidgetReplacementViewModelImpl(get(), get(), get(), get()) }
        viewModel<WidgetReplacementPickerViewModel> { WidgetReplacementPickerViewModelImpl(get(), get(), get(), get(), get()) }
        viewModel<OverlayTweaksViewModel> { OverlayTweaksViewModelImpl(get(), get(), get()) }
        viewModel<DeferredRestartViewModel> { DeferredRestartViewModelImpl(get()) }
        viewModel<BackupRestoreBackupViewModel> { BackupRestoreBackupViewModelImpl(get(), get()) }
        viewModel<OptionsViewModel> { OptionsViewModelImpl(get()) }
        viewModel<BackupRestoreViewModel> { BackupRestoreViewModelImpl(get(), get()) }
        viewModel<BackupRestoreRestoreViewModel> { BackupRestoreRestoreViewModelImpl(get(), get(), get()) }
        viewModel<MagiskInfoViewModel> { MagiskInfoViewModelImpl(get()) }
        viewModel<OptionsReapplyViewModel> { OptionsReapplyViewModelImpl(get(), get()) }
        viewModel<OptionsAdvancedViewModel> { OptionsAdvancedViewModelImpl(get(), get(), get()) }
        viewModel<ResetInfoViewModel> { ResetInfoViewModelImpl(get(), get()) }
        viewModel<ResetApplyViewModel> { ResetApplyViewModelImpl(get(), get()) }
        viewModel<ContributorsViewModel> { ContributorsViewModelImpl(get()) }
        viewModel<SettingsUpdateViewModel> { SettingsUpdateViewModelImpl(get(), get()) }
    }

    private fun createMarkwon(): Markwon {
        val typeface = ResourcesCompat.getFont(this, R.font.google_sans_text_medium)
        return Markwon.builder(this).usePlugin(object: AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                typeface?.let {
                    builder.headingTypeface(it)
                    builder.headingBreakHeight(0)
                }
            }
        }).build()
    }

    override fun onCreate() {
        super.onCreate()
        Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER))
        HiddenApiBypass.addHiddenApiExemptions("")
        startKoin {
            androidContext(this@PixelLauncherMods)
            modules(singles, repositories, viewModels)
        }
    }

    private fun createGson(): Gson {
        return Gson().newBuilder()
            .registerTypeAdapterFactory(IconMetadata.getAdapterFactory())
            .create()
    }

}

const val PIXEL_LAUNCHER_PACKAGE_NAME = "com.google.android.apps.nexuslauncher"
const val OVERLAY_PACKAGE_NAME = "com.google.android.apps.nexuslauncher.plmoverlay"
const val LAWNICONS_PACKAGE_NAME = "app.lawnchair.lawnicons"