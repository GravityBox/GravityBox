/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
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

package com.ceco.kitkat.gravitybox;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ceco.kitkat.gravitybox.BatteryInfoManager.BatteryStatusListener;
import com.ceco.kitkat.gravitybox.R;
import de.robv.android.xposed.XposedBridge;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

public class StatusBarIconManager implements BroadcastSubReceiver {
    private static final String TAG = "GB:StatusBarIconManager";
    private static final boolean DEBUG = false;

    public static final int SI_MODE_GB = 0;
    public static final int SI_MODE_STOCK = 1;
    public static final int SI_MODE_DISABLED = 2;

    public static final int JELLYBEAN = 0;
    public static final int KITKAT = 1;

    public static final int FLAG_COLORING_ENABLED_CHANGED = 1 << 0;
    public static final int FLAG_SIGNAL_ICON_MODE_CHANGED = 1 << 1;
    public static final int FLAG_ICON_COLOR_CHANGED = 1 << 2;
    public static final int FLAG_ICON_COLOR_SECONDARY_CHANGED = 1 << 3;
    public static final int FLAG_DATA_ACTIVITY_COLOR_CHANGED = 1 << 4;
    public static final int FLAG_LOW_PROFILE_CHANGED = 1 << 5;
    public static final int FLAG_ICON_STYLE_CHANGED = 1 << 6;
    public static final int FLAG_ICON_ALPHA_CHANGED = 1 << 7;
    private static final int FLAG_ALL = 0xFF;

    private Context mContext;
    private Resources mGbResources;
    private Resources mSystemUiRes;
    private Map<String, Integer> mWifiIconIds;
    private Map<String, Integer> mMobileIconIds;
    private Map<String, Integer[]> mBasicIconIds;
    private Map<String, SoftReference<Drawable>> mIconCache;
    private boolean[] mAllowMobileIconChange;
    private ColorInfo mColorInfo;
    private List<IconManagerListener> mListeners;
    private BatteryInfoManager mBatteryInfo;

    public interface IconManagerListener {
        void onIconManagerStatusChanged(int flags, ColorInfo colorInfo);
    }

    static class ColorInfo {
        boolean coloringEnabled;
        int defaultIconColor;
        int[] iconColor;
        int defaultDataActivityColor;
        int[] dataActivityColor;
        int signalIconMode;
        boolean lowProfile;
        int iconStyle;
        float alphaSignalCluster;
        float alphaTextAndBattery;
    }

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public StatusBarIconManager(Context context, Context gbContext) {
        mContext = context;
        mSystemUiRes = mContext.getResources();
        mGbResources = gbContext.getResources();
        mAllowMobileIconChange = new boolean[] { true, true };

        Map<String, Integer> tmpMap = new HashMap<String, Integer>();
        tmpMap.put("stat_sys_wifi_signal_0", R.drawable.stat_sys_wifi_signal_0);
        tmpMap.put("stat_sys_wifi_signal_1", R.drawable.stat_sys_wifi_signal_1);
        tmpMap.put("stat_sys_wifi_signal_1_fully", R.drawable.stat_sys_wifi_signal_1_fully);
        tmpMap.put("stat_sys_wifi_signal_2", R.drawable.stat_sys_wifi_signal_2);
        tmpMap.put("stat_sys_wifi_signal_2_fully", R.drawable.stat_sys_wifi_signal_2_fully);
        tmpMap.put("stat_sys_wifi_signal_3", R.drawable.stat_sys_wifi_signal_3);
        tmpMap.put("stat_sys_wifi_signal_3_fully", R.drawable.stat_sys_wifi_signal_3_fully);
        tmpMap.put("stat_sys_wifi_signal_4", R.drawable.stat_sys_wifi_signal_4);
        tmpMap.put("stat_sys_wifi_signal_4_fully", R.drawable.stat_sys_wifi_signal_4_fully);
        tmpMap.put("stat_sys_wifi_signal_null", R.drawable.stat_sys_wifi_signal_null);
        mWifiIconIds = Collections.unmodifiableMap(tmpMap);

        if (Utils.isMtkDevice()) {
            tmpMap = new HashMap<String, Integer>();
            tmpMap.put("stat_sys_gemini_signal_1_blue", R.drawable.stat_sys_signal_1_fully);
            tmpMap.put("stat_sys_gemini_signal_2_blue", R.drawable.stat_sys_signal_2_fully);
            tmpMap.put("stat_sys_gemini_signal_3_blue", R.drawable.stat_sys_signal_3_fully);
            tmpMap.put("stat_sys_gemini_signal_4_blue", R.drawable.stat_sys_signal_4_fully);
            tmpMap.put("stat_sys_gemini_signal_1_orange", R.drawable.stat_sys_signal_1_fully);
            tmpMap.put("stat_sys_gemini_signal_2_orange", R.drawable.stat_sys_signal_2_fully);
            tmpMap.put("stat_sys_gemini_signal_3_orange", R.drawable.stat_sys_signal_3_fully);
            tmpMap.put("stat_sys_gemini_signal_4_orange", R.drawable.stat_sys_signal_4_fully);
            mMobileIconIds = Collections.unmodifiableMap(tmpMap);
        } else {
            tmpMap = new HashMap<String, Integer>();
            tmpMap.put("stat_sys_signal_0", R.drawable.stat_sys_signal_0);
            tmpMap.put("stat_sys_signal_0_fully", R.drawable.stat_sys_signal_0_fully);
            tmpMap.put("stat_sys_signal_1", R.drawable.stat_sys_signal_1);
            tmpMap.put("stat_sys_signal_1_fully", R.drawable.stat_sys_signal_1_fully);
            tmpMap.put("stat_sys_signal_2", R.drawable.stat_sys_signal_2);
            tmpMap.put("stat_sys_signal_2_fully", R.drawable.stat_sys_signal_2_fully);
            tmpMap.put("stat_sys_signal_3", R.drawable.stat_sys_signal_3);
            tmpMap.put("stat_sys_signal_3_fully", R.drawable.stat_sys_signal_3_fully);
            tmpMap.put("stat_sys_signal_4", R.drawable.stat_sys_signal_4);
            tmpMap.put("stat_sys_signal_4_fully", R.drawable.stat_sys_signal_4_fully);
            mMobileIconIds = Collections.unmodifiableMap(tmpMap);
        }

        Map<String, Integer[]> basicIconMap = new HashMap<String, Integer[]>();
        basicIconMap.put("stat_sys_data_bluetooth", new Integer[] 
                { R.drawable.stat_sys_data_bluetooth, R.drawable.stat_sys_data_bluetooth });
        basicIconMap.put("stat_sys_data_bluetooth_connected", new Integer[] {
                R.drawable.stat_sys_data_bluetooth_connected, 
                R.drawable.stat_sys_data_bluetooth_connected });
        basicIconMap.put("stat_sys_alarm", new Integer[] {
                R.drawable.stat_sys_alarm_jb, null });
        basicIconMap.put("stat_sys_ringer_vibrate", new Integer[] { 
                R.drawable.stat_sys_ringer_vibrate_jb, null });
        basicIconMap.put("stat_sys_ringer_silent", new Integer[] {
                R.drawable.stat_sys_ringer_silent_jb, null });
        basicIconMap.put("stat_sys_headset_with_mic", new Integer[] {
                R.drawable.stat_sys_headset_with_mic_jb, null });
        basicIconMap.put("stat_sys_headset_without_mic", new Integer[] {
                R.drawable.stat_sys_headset_without_mic_jb, null });
        mBasicIconIds = Collections.unmodifiableMap(basicIconMap);

        mIconCache = new HashMap<String, SoftReference<Drawable>>();

        initColorInfo();
        mBatteryInfo = new BatteryInfoManager(context, gbContext);

        mListeners = new ArrayList<IconManagerListener>();
    }

    private void initColorInfo() {
        mColorInfo = new ColorInfo();
        mColorInfo.coloringEnabled = false;
        mColorInfo.defaultIconColor = getDefaultIconColor();
        mColorInfo.iconColor = new int[2];
        mColorInfo.defaultDataActivityColor = mGbResources.getInteger(
                R.integer.signal_cluster_data_activity_icon_color);
        mColorInfo.dataActivityColor = new int[2];
        mColorInfo.signalIconMode = SI_MODE_STOCK;
        mColorInfo.lowProfile = false;
        mColorInfo.iconStyle = KITKAT;
        mColorInfo.alphaSignalCluster = 1;
        mColorInfo.alphaTextAndBattery = 1;
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_STATUSBAR_COLOR_CHANGED)) {
            if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_ICON_COLOR)) {
                setIconColor(intent.getIntExtra(
                        GravityBoxSettings.EXTRA_SB_ICON_COLOR, getDefaultIconColor()));
            } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_ICON_STYLE)) {
                setIconStyle(intent.getIntExtra(GravityBoxSettings.EXTRA_SB_ICON_STYLE, 0));
            } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_ICON_COLOR_SECONDARY)) {
                setIconColor(1, intent.getIntExtra(
                        GravityBoxSettings.EXTRA_SB_ICON_COLOR_SECONDARY, 
                        getDefaultIconColor()));
            } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_DATA_ACTIVITY_COLOR)) {
                setDataActivityColor(intent.getIntExtra(
                        GravityBoxSettings.EXTRA_SB_DATA_ACTIVITY_COLOR, 
                        mColorInfo.defaultDataActivityColor));
            } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_DATA_ACTIVITY_COLOR_SECONDARY)) {
                setDataActivityColor(1, intent.getIntExtra(
                        GravityBoxSettings.EXTRA_SB_DATA_ACTIVITY_COLOR_SECONDARY, 
                        mColorInfo.defaultDataActivityColor));
            } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_ICON_COLOR_ENABLE)) {
                setColoringEnabled(intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_SB_ICON_COLOR_ENABLE, false));
                if (DEBUG) log("Icon colors master switch set to: " + isColoringEnabled());
            } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_SIGNAL_COLOR_MODE)) {
                setSignalIconMode(intent.getIntExtra(
                        GravityBoxSettings.EXTRA_SB_SIGNAL_COLOR_MODE,
                        StatusBarIconManager.SI_MODE_GB));
            }
        } else if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
            mBatteryInfo.updateBatteryInfo(intent);
        } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_BATTERY_CHARGED_SOUND_CHANGED)) {
            if (intent.hasExtra(GravityBoxSettings.EXTRA_BATTERY_CHARGED_SOUND)) {
                mBatteryInfo.setChargedSoundEnabled(intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_BATTERY_CHARGED_SOUND, false));
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_CHARGER_PLUGGED_SOUND)) {
                mBatteryInfo.setPluggedSoundEnabled(intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_CHARGER_PLUGGED_SOUND, false));
            }
        }
    }

    public BatteryInfoManager getBatteryInfoManager() {
        return mBatteryInfo;
    }

    public void registerListener(IconManagerListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
            listener.onIconManagerStatusChanged(FLAG_ALL, mColorInfo);
        }
        if (listener instanceof BatteryStatusListener) {
            mBatteryInfo.registerListener((BatteryStatusListener) listener);
        }
    }

    public void unregisterListener(IconManagerListener listener) {
        if (mListeners.contains(listener)) {
            mListeners.remove(listener);
        }
    }

    private void notifyListeners(int flags) {
        for (IconManagerListener listener : mListeners) {
            listener.onIconManagerStatusChanged(flags, mColorInfo);
        }
    }

    public void refreshState() {
        notifyListeners(FLAG_ALL);
    }

    public void setColoringEnabled(boolean enabled) {
        if (mColorInfo.coloringEnabled != enabled) {
            mColorInfo.coloringEnabled = enabled;
            clearCache();
            notifyListeners(FLAG_COLORING_ENABLED_CHANGED | FLAG_ICON_COLOR_CHANGED);
        }
    }

    public boolean isColoringEnabled() {
        return mColorInfo.coloringEnabled;
    }

    public void setLowProfile(boolean lowProfile) {
        if (mColorInfo.lowProfile != lowProfile) {
            mColorInfo.lowProfile = lowProfile;
            notifyListeners(FLAG_LOW_PROFILE_CHANGED);
        }
    }

    public int getDefaultIconColor() {
        return Color.WHITE;
    }

    public void setSignalIconMode(int mode) {
        if (mColorInfo.signalIconMode != mode) {
            mColorInfo.signalIconMode = mode;
            clearCache();
            notifyListeners(FLAG_SIGNAL_ICON_MODE_CHANGED);
        }
    }

    public int getSignalIconMode() {
        return mColorInfo.signalIconMode;
    }

    public int getIconColor(int index) {
        return mColorInfo.iconColor[index];
    }

    public int getIconColor() {
        return getIconColor(0);
    }

    public int getDataActivityColor() {
        return getDataActivityColor(0);
    }

    public int getDataActivityColor(int index) {
        return mColorInfo.dataActivityColor[index];
    }

    public void setIconColor(int index, int color) {
        if (mColorInfo.iconColor[index] != color) {
            mColorInfo.iconColor[index] = color;
            clearCache();
            notifyListeners(index == 0 ?
                    FLAG_ICON_COLOR_CHANGED : FLAG_ICON_COLOR_SECONDARY_CHANGED);
        }
    }

    public void setIconColor(int color) {
        setIconColor(0, color);
    }

    public void setDataActivityColor(int index, int color) {
        if (mColorInfo.dataActivityColor[index] != color) {
            mColorInfo.dataActivityColor[index] = color;
            notifyListeners(FLAG_DATA_ACTIVITY_COLOR_CHANGED);
        }
    }

    public void setDataActivityColor(int color) {
        setDataActivityColor(0, color);
    }

    public void setIconStyle(int style) {
        if((style == JELLYBEAN || style == KITKAT) &&
                mColorInfo.iconStyle != style) {
            mColorInfo.iconStyle = style;
            clearCache();
            notifyListeners(FLAG_ICON_STYLE_CHANGED);
        }
    }

    public void setIconAlpha(float alphaSignalCluster, float alphaTextAndBattery) {
        if (mColorInfo.alphaSignalCluster != alphaSignalCluster ||
                mColorInfo.alphaTextAndBattery != alphaTextAndBattery) {
            mColorInfo.alphaSignalCluster = alphaSignalCluster;
            mColorInfo.alphaTextAndBattery = alphaTextAndBattery;
            notifyListeners(FLAG_ICON_ALPHA_CHANGED);
        }
    }

    public Drawable applyColorFilter(int index, Drawable drawable, PorterDuff.Mode mode) {
        if (drawable != null) {
            drawable.setColorFilter(mColorInfo.iconColor[index], mode);
        }
        return drawable;
    }

    public Drawable applyColorFilter(int index, Drawable drawable) {
        return applyColorFilter(index, drawable, PorterDuff.Mode.SRC_IN);
    }

    public Drawable applyColorFilter(Drawable drawable) {
        return applyColorFilter(0, drawable, PorterDuff.Mode.SRC_IN);
    }

    public Drawable applyColorFilter(Drawable drawable, PorterDuff.Mode mode) {
        return applyColorFilter(0, drawable, mode);
    }

    public Drawable applyDataActivityColorFilter(int index, Drawable drawable) {
        drawable.setColorFilter(mColorInfo.dataActivityColor[index], PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    public Drawable applyDataActivityColorFilter(Drawable drawable) {
        return applyDataActivityColorFilter(0, drawable);
    }

    public void clearCache() {
        mIconCache.clear();
        if (DEBUG) log("Cache cleared");
    }

    private Drawable getCachedDrawable(String key) {
        if (mIconCache.containsKey(key)) {
            if (DEBUG) log("getCachedDrawable('" + key + "') - cached drawable found");
            return mIconCache.get(key).get();
        }
        return null;
    }

    private void setCachedDrawable(String key, Drawable d) {
        mIconCache.put(key, new SoftReference<Drawable>(d));
        if (DEBUG) log("setCachedDrawable('" + key + "') - storing to cache");
    }

    public Drawable getWifiIcon(int resId, boolean fullyConnected) {
        Drawable cd;
        String key;

        try {
            key = mSystemUiRes.getResourceEntryName(resId);
            if (!fullyConnected && key.endsWith("_fully")) {
                key = key.substring(0, key.length() - "_fully".length());
            }
        } catch (Resources.NotFoundException nfe) {
            return null;
        }

        switch(mColorInfo.signalIconMode) {
            case SI_MODE_GB:
                cd = getCachedDrawable(key);
                if (cd != null) return cd;
                if (mWifiIconIds.containsKey(key)) {
                    Drawable d = mGbResources.getDrawable(mWifiIconIds.get(key)).mutate();
                    d = applyColorFilter(d);
                    setCachedDrawable(key, d);
                    return d;
                }
                if (DEBUG) log("getWifiIcon: no drawable for key: " + key);
                return null;

            case SI_MODE_STOCK:
                cd = getCachedDrawable(key);
                if (cd != null) return cd;
                Drawable d = mSystemUiRes.getDrawable(resId).mutate();
                d = applyColorFilter(d);
                setCachedDrawable(key, d);
                return d;

            case SI_MODE_DISABLED:
            default:
                return null;
        }
    }

    public Drawable getMobileIcon(int index, int resId, boolean fullyConnected) {
        Drawable cd;
        String key;

        try {
            key = mSystemUiRes.getResourceEntryName(resId);
            if (!fullyConnected && key.endsWith("_fully")) {
                key = key.substring(0, key.length() - "_fully".length());
            }
        } catch (Resources.NotFoundException nfe) {
            return null;
        }

        mAllowMobileIconChange[index] = !Utils.isMtkDevice() ||
                key.contains("blue") || key.contains("orange");
        if (!mAllowMobileIconChange[index]) {
            return null;
        }

        switch(mColorInfo.signalIconMode) {
            case SI_MODE_GB:
                cd = getCachedDrawable(key);
                if (cd != null) return cd;
                if (mMobileIconIds.containsKey(key)) {
                    Drawable d = mGbResources.getDrawable(mMobileIconIds.get(key)).mutate();
                    d = applyColorFilter(index, d);
                    setCachedDrawable(key, d);
                    return d;
                }
                if (DEBUG) log("getMobileIcon: no drawable for key: " + key);
                return null;

            case SI_MODE_STOCK:
                cd = getCachedDrawable(key);
                if (cd != null) return cd;
                Drawable d = mSystemUiRes.getDrawable(resId).mutate();
                d = applyColorFilter(index, d);
                setCachedDrawable(key, d);
                return d;

            case SI_MODE_DISABLED:
            default:
                return null;
        }
    }

    public Drawable getMobileIcon(int resId, boolean fullyConnected) {
        return getMobileIcon(0, resId, fullyConnected);
    }

    public boolean isMobileIconChangeAllowed(int index) {
        return mAllowMobileIconChange[index];
    }

    public boolean isMobileIconChangeAllowed() {
        return isMobileIconChangeAllowed(0);
    }

    public Drawable getBasicIcon(int resId) {
        if (resId == 0) return null;

        try {
            String key = mSystemUiRes.getResourceEntryName(resId);
            if (!mBasicIconIds.containsKey(key)) {
                if (DEBUG) log("getBasicIcon: no record for key: " + key);
                return null;
            }

            if (mColorInfo.coloringEnabled) {
                Drawable d = getCachedDrawable(key);
                if (d != null) return d;
                if (mBasicIconIds.get(key)[mColorInfo.iconStyle] != null) {
                    d = mGbResources.getDrawable(mBasicIconIds.get(key)[mColorInfo.iconStyle]).mutate();
                    d = applyColorFilter(d);
                } else {
                    d = mSystemUiRes.getDrawable(resId).mutate();
                    d = applyColorFilter(d, PorterDuff.Mode.SRC_ATOP);
                }
                setCachedDrawable(key, d);
                if (DEBUG) log("getBasicIcon: returning drawable for key: " + key);
                return d;
            } else {
                return mSystemUiRes.getDrawable(resId);
            }
        } catch (Throwable t) {
            log("getBasicIcon: " + t.getMessage());
            return null;
        }
    }
}
