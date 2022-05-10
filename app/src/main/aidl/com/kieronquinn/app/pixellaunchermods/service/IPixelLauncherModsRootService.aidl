package com.kieronquinn.app.pixellaunchermods.service;

import com.kieronquinn.app.pixellaunchermods.model.ipc.ParceledListSlice;
import com.kieronquinn.app.pixellaunchermods.model.ipc.GridSize;
import com.kieronquinn.app.pixellaunchermods.model.ipc.ProxyAppWidgetServiceContainer;
import com.kieronquinn.app.pixellaunchermods.service.IPixelLauncherRestartListener;
import com.kieronquinn.app.pixellaunchermods.service.IDatabaseChangedListener;
import com.kieronquinn.app.pixellaunchermods.service.IPackageChangedListener;
import com.kieronquinn.app.pixellaunchermods.service.IIconSizeChangedListener;
import com.kieronquinn.app.pixellaunchermods.service.IResetCompleteCallback;
import com.kieronquinn.app.pixellaunchermods.service.IPixelLauncherForegroundListener;

interface IPixelLauncherModsRootService {

    //Apps & Shortcuts
    ParceledListSlice<Bundle> loadDatabase(in ParceledListSlice<Bundle> modifiedApps);
    ParceledListSlice<Bundle> loadFavourites(boolean includeFolders);
    void updateModifiedApps(in ParceledListSlice<Bundle> modifiedApps, boolean skipRestart, int iconSize);
    void updateFavourites(in ParceledListSlice<Bundle> favourites);
    void deleteEntryAndRestart(in String componentName);
    void setPixelLauncherRestartListener(in IPixelLauncherRestartListener listener);
    void setPixelLauncherForegroundListener(in IPixelLauncherForegroundListener listener);
    void setPackageChangedListener(in IPackageChangedListener listener);
    void setDatabaseChangedListener(in IDatabaseChangedListener listener);
    void setIconSizeChangedListener(in IIconSizeChangedListener listener);
    void resetAllIcons(in IResetCompleteCallback listener);

    GridSize getGridSize();
    int getRemoteIconSize();
    boolean areThemedIconsEnabled();
    boolean areNativeThemedIconsSupported();

    //Widget Resizing
    ProxyAppWidgetServiceContainer getProxyAppWidgetService();
    void updateWidgets(in ParceledListSlice<Bundle> widgets);

    //Hide Clock
    void setStatusBarIconDenylist(String denylist);

    //Overlay
    void restartLauncherImmediately();
    boolean isOverlayInstalled();
    boolean isOverlayEnabled();
    void enableOverlay();

    //Widget replacement
    void setSearchWidgetPackageEnabled(boolean enabled);

    //Reset
    void uninstallOverlayUpdates();

}