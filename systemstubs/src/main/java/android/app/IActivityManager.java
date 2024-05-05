package android.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import java.util.List;

public interface IActivityManager extends android.os.IInterface {

    abstract class Stub extends android.os.Binder implements android.app.IServiceConnection {
        public static IActivityManager asInterface(android.os.IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }

    void registerProcessObserver(IProcessObserver observer);
    void unregisterProcessObserver(IProcessObserver observer);
    List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses();

    int startActivityWithFeature(
            IApplicationThread caller,
            String callingPackage,
            String callingFeatureId,
            Intent intent,
            String resolvedType,
            IBinder resultTo,
            String resultWho,
            int requestCode,
            int flags,
            ProfilerInfo profilerInfo,
            Bundle options);

}
