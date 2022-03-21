/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.app;

import android.app.IProcessObserver;
import android.app.IApplicationThread;
import android.app.RunningAppProcessInfo;
import android.app.ProfilerInfo;

interface IActivityManager {

    void registerProcessObserver(in IProcessObserver observer);
    void unregisterProcessObserver(in IProcessObserver observer);
    List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses();

    int startActivityWithFeature(
                IApplicationThread caller,
                String callingPackage,
                String callingFeatureId,
                in Intent intent,
                String resolvedType,
                IBinder resultTo,
                String resultWho,
                int requestCode,
                int flags,
                in ProfilerInfo profilerInfo,
                in Bundle options);

}