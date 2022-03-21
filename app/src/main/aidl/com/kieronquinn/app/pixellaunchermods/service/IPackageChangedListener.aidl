package com.kieronquinn.app.pixellaunchermods.service;

interface IPackageChangedListener {

    void onPackageChanged(String packageName, boolean isShortcut);

}