/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.kieronquinn.app.pixellaunchermods.ui.drawables;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;

import androidx.core.content.res.ResourcesCompat;

import com.kieronquinn.app.pixellaunchermods.model.icon.LegacyThemedIcon;
import com.kieronquinn.app.pixellaunchermods.utils.extensions.Extensions_ColorKt;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

/**
 * Wrapper over {@link AdaptiveIconDrawable} to intercept icon flattening logic for dynamic
 * clock icons
 */
public class ClockDrawableWrapper extends AdaptiveIconDrawable {

    private static final String TAG = "ClockDrawableWrapper";

    private static final boolean DISABLE_SECONDS = true;

    // Time after which the clock icon should check for an update. The actual invalidate
    // will only happen in case of any change.
    public static final long TICK_MS = DISABLE_SECONDS ? TimeUnit.MINUTES.toMillis(1) : 200L;

    private static final String LAUNCHER_PACKAGE = "com.android.launcher3";
    private static final String ROUND_ICON_METADATA_KEY = LAUNCHER_PACKAGE
            + ".LEVEL_PER_TICK_ICON_ROUND";
    private static final String HOUR_INDEX_METADATA_KEY = LAUNCHER_PACKAGE + ".HOUR_LAYER_INDEX";
    private static final String MINUTE_INDEX_METADATA_KEY = LAUNCHER_PACKAGE
            + ".MINUTE_LAYER_INDEX";
    private static final String SECOND_INDEX_METADATA_KEY = LAUNCHER_PACKAGE
            + ".SECOND_LAYER_INDEX";
    private static final String DEFAULT_HOUR_METADATA_KEY = LAUNCHER_PACKAGE
            + ".DEFAULT_HOUR";
    private static final String DEFAULT_MINUTE_METADATA_KEY = LAUNCHER_PACKAGE
            + ".DEFAULT_MINUTE";
    private static final String DEFAULT_SECOND_METADATA_KEY = LAUNCHER_PACKAGE
            + ".DEFAULT_SECOND";

    /* Number of levels to jump per second for the second hand */
    private static final int LEVELS_PER_SECOND = 10;

    public static final int INVALID_VALUE = -1;

    private final AnimationInfo mAnimationInfo = new AnimationInfo();

    public ClockDrawableWrapper(AdaptiveIconDrawable base) {
        super(base.getBackground(), base.getForeground());
    }

    /**
     * Loads and returns the wrapper from the provided package, or returns null
     * if it is unable to load.
     */
    public static ClockDrawableWrapper forPackage(Context context, String pkg, int iconDpi) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo =  pm.getApplicationInfo(pkg,
                    PackageManager.MATCH_UNINSTALLED_PACKAGES | PackageManager.GET_META_DATA);
            Resources res = pm.getResourcesForApplication(appInfo);
            return forExtras(appInfo, appInfo.metaData,
                    resId -> res.getDrawableForDensity(resId, iconDpi));
        } catch (Exception e) {
            Log.d(TAG, "Unable to load clock drawable info", e);
        }
        return null;
    }

    public static ClockDrawableWrapper fromThemeData(Context context, LegacyThemedIcon themeData) {
        try {
            TypedArray ta = themeData.getResources().obtainTypedArray(themeData.getResourceId());
            int count = ta.length();
            Bundle extras = new Bundle();
            for (int i = 0; i < count; i += 2) {
                TypedValue v = ta.peekValue(i + 1);
                extras.putInt(ta.getString(i), v.type >= TypedValue.TYPE_FIRST_INT
                        && v.type <= TypedValue.TYPE_LAST_INT
                        ? v.data : v.resourceId);
            }
            ta.recycle();
            ClockDrawableWrapper drawable = ClockDrawableWrapper.forExtras(
                    context.getApplicationInfo(), extras, resId -> {
                        int[] colors = Extensions_ColorKt.getPlateColours(context);
                        Drawable bg = new ColorDrawable(colors[0]);
                        Drawable fg = ResourcesCompat.getDrawable(themeData.getResources(), resId, null).mutate();
                        fg.setTint(colors[1]);
                        return new AdaptiveIconDrawable(bg, fg);
                    });
            if (drawable != null) {
                return drawable;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading themed clock", e);
        }
        return null;
    }

    private static ClockDrawableWrapper forExtras(ApplicationInfo appInfo, Bundle metadata,
            IntFunction<Drawable> drawableProvider) {
        if (metadata == null) {
            return null;
        }
        int drawableId = metadata.getInt(ROUND_ICON_METADATA_KEY, 0);
        if (drawableId == 0) {
            return null;
        }

        Drawable drawable = drawableProvider.apply(drawableId).mutate();
        if (!(drawable instanceof AdaptiveIconDrawable)) {
            return null;
        }

        ClockDrawableWrapper wrapper =
                new ClockDrawableWrapper((AdaptiveIconDrawable) drawable);
        AnimationInfo info = wrapper.mAnimationInfo;

        info.baseDrawableState = drawable.getConstantState();

        info.hourLayerIndex = metadata.getInt(HOUR_INDEX_METADATA_KEY, INVALID_VALUE);
        info.minuteLayerIndex = metadata.getInt(MINUTE_INDEX_METADATA_KEY, INVALID_VALUE);
        info.secondLayerIndex = metadata.getInt(SECOND_INDEX_METADATA_KEY, INVALID_VALUE);

        info.defaultHour = metadata.getInt(DEFAULT_HOUR_METADATA_KEY, 0);
        info.defaultMinute = metadata.getInt(DEFAULT_MINUTE_METADATA_KEY, 0);
        info.defaultSecond = metadata.getInt(DEFAULT_SECOND_METADATA_KEY, 0);

        LayerDrawable foreground = (LayerDrawable) wrapper.getForeground();
        int layerCount = foreground.getNumberOfLayers();
        if (info.hourLayerIndex < 0 || info.hourLayerIndex >= layerCount) {
            info.hourLayerIndex = INVALID_VALUE;
        }
        if (info.minuteLayerIndex < 0 || info.minuteLayerIndex >= layerCount) {
            info.minuteLayerIndex = INVALID_VALUE;
        }
        if (info.secondLayerIndex < 0 || info.secondLayerIndex >= layerCount) {
            info.secondLayerIndex = INVALID_VALUE;
        } else if (DISABLE_SECONDS) {
            foreground.setDrawable(info.secondLayerIndex, null);
            info.secondLayerIndex = INVALID_VALUE;
        }
        info.applyTime(Calendar.getInstance(), foreground);
        return wrapper;
    }

    private static class AnimationInfo {

        public ConstantState baseDrawableState;

        public int hourLayerIndex;
        public int minuteLayerIndex;
        public int secondLayerIndex;
        public int defaultHour;
        public int defaultMinute;
        public int defaultSecond;

        boolean applyTime(Calendar time, LayerDrawable foregroundDrawable) {
            time.setTimeInMillis(System.currentTimeMillis());

            // We need to rotate by the difference from the default time if one is specified.
            int convertedHour = (time.get(Calendar.HOUR) + (12 - defaultHour)) % 12;
            int convertedMinute = (time.get(Calendar.MINUTE) + (60 - defaultMinute)) % 60;
            int convertedSecond = (time.get(Calendar.SECOND) + (60 - defaultSecond)) % 60;

            boolean invalidate = false;
            if (hourLayerIndex != INVALID_VALUE) {
                final Drawable hour = foregroundDrawable.getDrawable(hourLayerIndex);
                if (hour.setLevel(convertedHour * 60 + time.get(Calendar.MINUTE))) {
                    invalidate = true;
                }
            }

            if (minuteLayerIndex != INVALID_VALUE) {
                final Drawable minute = foregroundDrawable.getDrawable(minuteLayerIndex);
                if (minute.setLevel(time.get(Calendar.HOUR) * 60 + convertedMinute)) {
                    invalidate = true;
                }
            }

            if (secondLayerIndex != INVALID_VALUE) {
                final Drawable second = foregroundDrawable.getDrawable(secondLayerIndex);
                if (second.setLevel(convertedSecond * LEVELS_PER_SECOND)) {
                    invalidate = true;
                }
            }

            return invalidate;
        }
    }

}