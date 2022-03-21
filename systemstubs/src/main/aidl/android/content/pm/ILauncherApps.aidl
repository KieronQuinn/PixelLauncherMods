package android.content.pm;

import android.content.pm.IOnAppsChangedListener;

interface ILauncherApps {

    void addOnAppsChangedListener(String callingPackage, in IOnAppsChangedListener listener);
    void removeOnAppsChangedListener(in IOnAppsChangedListener listener);

}