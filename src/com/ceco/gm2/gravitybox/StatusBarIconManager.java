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

package com.ceco.gm2.gravitybox;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ceco.gm2.gravitybox.BatteryInfoManager.BatteryStatusListener;
import de.robv.android.xposed.XposedBridge;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

public class StatusBarIconManager implements BroadcastSubReceiver {
    private static final String TAG = "GB:StatusBarIconManager";
    private static final boolean DEBUG = false;
    public static final int DEFAULT_DATA_ACTIVITY_COLOR = Color.WHITE;

    public static final int SI_MODE_GB = 0;
    public static final int SI_MODE_STOCK = 1;
    public static final int SI_MODE_DISABLED = 2;

    public static final int JELLYBEAN = 0;
    public static final int KITKAT = 1;

    public static final int FLAG_COLORING_ENABLED_CHANGED = 1 << 0;
    public static final int FLAG_SKIP_BATTERY_ICON_CHANGED = 1 << 1;
    public static final int FLAG_SIGNAL_ICON_MODE_CHANGED = 1 << 2;
    public static final int FLAG_FOLLOW_STOCK_BATTERY_COLOR_CHANGED = 1 << 3;
    public static final int FLAG_ICON_COLOR_CHANGED = 1 << 4;
    public static final int FLAG_ICON_COLOR_SECONDARY_CHANGED = 1 << 5;
    public static final int FLAG_DATA_ACTIVITY_COLOR_CHANGED = 1 << 6;
    public static final int FLAG_LOW_PROFILE_CHANGED = 1 << 7;
    public static final int FLAG_ICON_STYLE_CHANGED = 1 << 8;
    private static final int FLAG_ALL = 0x1FF;

    private Context mContext;
    private Resources mGbResources;
    private Resources mSystemUiRes;
    private Map<String, Integer> mWifiIconIds;
    private Map<String, Integer> mMobileIconIds;
    private Map<String, Integer> mBatteryIconIds;
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
        Integer stockBatteryColor;
        int signalIconMode;
        boolean skipBatteryIcon;
        boolean followStockBatteryColor;
        boolean lowProfile;
        int iconStyle;
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

        tmpMap = new HashMap<String, Integer>();
        tmpMap.put("stat_sys_battery_0", R.drawable.stat_sys_battery_0);
        tmpMap.put("stat_sys_battery_15", R.drawable.stat_sys_battery_15);
        tmpMap.put("stat_sys_battery_35", R.drawable.stat_sys_battery_28);
        tmpMap.put("stat_sys_battery_49", R.drawable.stat_sys_battery_43);
        tmpMap.put("stat_sys_battery_60", R.drawable.stat_sys_battery_57);
        tmpMap.put("stat_sys_battery_75", R.drawable.stat_sys_battery_71);
        tmpMap.put("stat_sys_battery_90", R.drawable.stat_sys_battery_85);
        tmpMap.put("stat_sys_battery_100", R.drawable.stat_sys_battery_100);
        tmpMap.put("stat_sys_battery_charge_anim0", R.drawable.stat_sys_battery_charge_anim0);
        tmpMap.put("stat_sys_battery_charge_anim15", R.drawable.stat_sys_battery_charge_anim15);
        tmpMap.put("stat_sys_battery_charge_anim35", R.drawable.stat_sys_battery_charge_anim28);
        tmpMap.put("stat_sys_battery_charge_anim49", R.drawable.stat_sys_battery_charge_anim43);
        tmpMap.put("stat_sys_battery_charge_anim60", R.drawable.stat_sys_battery_charge_anim57);
        tmpMap.put("stat_sys_battery_charge_anim75", R.drawable.stat_sys_battery_charge_anim71);
        tmpMap.put("stat_sys_battery_charge_anim90", R.drawable.stat_sys_battery_charge_anim85);
        tmpMap.put("stat_sys_battery_charge_anim100", R.drawable.stat_sys_battery_charge_anim100);
        mBatteryIconIds = Collections.unmodifiableMap(tmpMap);

        Map<String, Integer[]> basicIconMap = new HashMap<String, Integer[]>();
        basicIconMap.put("stat_sys_data_bluetooth", new Integer[] 
                { R.drawable.stat_sys_data_bluetooth, R.drawable.stat_sys_data_bluetooth });
        basicIconMap.put("stat_sys_data_bluetooth_connected", new Integer[] {
                R.drawable.stat_sys_data_bluetooth_connected, 
                R.drawable.stat_sys_data_bluetooth_connected });
        basicIconMap.put("stat_sys_alarm", new Integer[] {
                null, R.drawable.stat_sys_alarm_kk });
        basicIconMap.put("stat_sys_ringer_vibrate", new Integer[] { 
                null, R.drawable.stat_sys_ringer_vibrate_kk });
        basicIconMap.put("stat_sys_ringer_silent", new Integer[] {
                R.drawable.stat_sys_ringer_silent_jb, R.drawable.stat_sys_ringer_silent_kk });
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
        mColorInfo.defaultDataActivityColor = DEFAULT_DATA_ACTIVITY_COLOR;
        mColorInfo.dataActivityColor = new int[2];
        mColorInfo.followStockBatteryColor = false;
        mColorInfo.signalIconMode = SI_MODE_GB;
        mColorInfo.lowProfile = false;
        mColorInfo.iconStyle = JELLYBEAN;
        initStockBatteryColor();
    }

    private void initStockBatteryColor() {
        try {
            final int resId = mSystemUiRes.getIdentifier(
                    "stat_sys_battery_100", "drawable", "com.android.systemui");
            if (resId != 0) {
                final Bitmap b = BitmapFactory.decodeResource(mSystemUiRes, resId);
                final int x = b.getWidth() / 2;
                final int y = b.getHeight() / 2;
                mColorInfo.stockBatteryColor = b.getPixel(x, y);
            }
            if (DEBUG) log("mStockBatteryColor = " + 
                    ((mColorInfo.stockBatteryColor != null ) ? 
                            Integer.toHexString(mColorInfo.stockBatteryColor) : "NULL"));
        } catch (Throwable t) {
            log("Error initializing stock battery color: " + t.getMessage());
        }
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
                        StatusBarIconManager.DEFAULT_DATA_ACTIVITY_COLOR));
            } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_DATA_ACTIVITY_COLOR_SECONDARY)) {
                setDataActivityColor(1, intent.getIntExtra(
                        GravityBoxSettings.EXTRA_SB_DATA_ACTIVITY_COLOR_SECONDARY, 
                        StatusBarIconManager.DEFAULT_DATA_ACTIVITY_COLOR));
            } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_ICON_COLOR_ENABLE)) {
                setColoringEnabled(intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_SB_ICON_COLOR_ENABLE, false));
                if (DEBUG) log("Icon colors master switch set to: " + isColoringEnabled());
            } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_COLOR_FOLLOW)) {
                setFollowStockBatteryColor(intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_SB_COLOR_FOLLOW, false));
            } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_COLOR_SKIP_BATTERY)) {
                setSkipBatteryIcon(intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_SB_COLOR_SKIP_BATTERY, false));
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

    public void setSkipBatteryIcon(boolean skip) {
        if (mColorInfo.skipBatteryIcon != skip) {
            mColorInfo.skipBatteryIcon = skip;
            notifyListeners(FLAG_SKIP_BATTERY_ICON_CHANGED);
        }
    }

    public boolean shouldSkipBatteryIcon() {
        return mColorInfo.skipBatteryIcon;
    }

    public int getDefaultIconColor() {
        if (mColorInfo.followStockBatteryColor && mColorInfo.stockBatteryColor != null) {
            return mColorInfo.stockBatteryColor;
        } else {
            return mGbResources.getColor(android.R.color.holo_blue_dark);
        }
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

    public void setFollowStockBatteryColor(boolean follow) {
        if (mColorInfo.followStockBatteryColor != follow) {
            mColorInfo.followStockBatteryColor = follow;
            mColorInfo.defaultIconColor = getDefaultIconColor();
            int flags = FLAG_FOLLOW_STOCK_BATTERY_COLOR_CHANGED;
            if (!mColorInfo.coloringEnabled) {
                flags |= FLAG_ICON_COLOR_CHANGED;
            }
            notifyListeners(flags);
        }
    }

    public int getIconColor(int index) {
        return mColorInfo.iconColor[index];
    }

    public int getIconColor() {
        return getIconColor(0);
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

    public Drawable getWifiIcon(int resId) {
        Drawable cd;
        String key;

        try {
            key = mSystemUiRes.getResourceEntryName(resId);
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

    public Drawable getMobileIcon(int index, int resId) {
        Drawable cd;
        String key;

        try {
            key = mSystemUiRes.getResourceEntryName(resId);
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

    public Drawable getMobileIcon(int resId) {
        return getMobileIcon(0, resId);
    }

    public boolean isMobileIconChangeAllowed(int index) {
        return mAllowMobileIconChange[index];
    }

    public boolean isMobileIconChangeAllowed() {
        return isMobileIconChangeAllowed(0);
    }

    public Drawable getBatteryIcon(int level, boolean plugged) {
        String key = getKeyForBatteryStatus(level, plugged);

        Drawable cd = getCachedDrawable(key);
        if (cd != null) return cd;

        if (mBatteryIconIds.containsKey(key)) {
            Drawable d = mGbResources.getDrawable(mBatteryIconIds.get(key)).mutate();
            d = applyColorFilter(d);
            setCachedDrawable(key, d);
            return d;
        }

        if (DEBUG) log("getBatteryIcon: no drawable for key: " + key);
        return null;
    }

    private String getKeyForBatteryStatus(int level, boolean plugged) {
        String key = plugged ? "stat_sys_battery_charge_anim" : "stat_sys_battery_";

        if (level <= 4) {
            key += "0";
        } else if (level <= 15) {
            key += "15";
        } else if (level <= 35) {
            key += "35";
        } else if (level <= 49) {
            key += "49";
        } else if (level <= 60) {
            key += "60";
        } else if (level <= 75) {
            key += "75";
        } else if (level <= 90) {
            key += "90";
        } else if (level <= 100) {
            key += "100";
        }

        return key;
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
