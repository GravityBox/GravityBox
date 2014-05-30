/*
 * Copyright (C) 2014 Peter Gregus for GravityBox Project (C3C076@xda)
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

package com.ceco.kitkat.gravitybox.ledcontrol;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

public class LedSettings {

    public static final String PREF_KEY_LOCKED = "uncLocked";
    public static final String PREF_KEY_ACTIVE_SCREEN_ENABLED = "activeScreenEnabled";

    public static final String ACTION_UNC_SETTINGS_CHANGED = "gravitybox.intent.action.UNC_SETTINGS_CHANGED";
    public static final String EXTRA_UNC_AS_ENABLED = "uncActiveScreenEnabled";

    public enum LedMode { ORIGINAL, OVERRIDE, OFF };

    private Context mContext;
    private String mPackageName;
    private boolean mEnabled;
    private boolean mOngoing;
    private int mLedOnMs;
    private int mLedOffMs;
    private int mColor;
    private boolean mSoundOverride;
    private Uri mSoundUri;
    private boolean mSoundOnlyOnce;
    private long mSoundOnlyOnceTimeout;
    private boolean mInsistent;
    private boolean mVibrateOverride;
    private String mVibratePatternStr;
    private long[] mVibratePattern;
    private boolean mActiveScreenEnabled;
    private boolean mActiveScreenExpanded;
    private LedMode mLedMode;
    private boolean mQhIgnore;
    private String mQhIgnoreList;

    protected static LedSettings deserialize(Context context, String packageName) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                    "ledcontrol", Context.MODE_WORLD_READABLE);
            Set<String> dataSet = prefs.getStringSet(packageName, null);
            if (dataSet == null) {
                if (packageName.equals("default")) {
                    return new LedSettings(context, packageName);
                } else {
                    LedSettings defLs = LedSettings.getDefault(context);
                    defLs.mPackageName = packageName;
                    defLs.mEnabled = false;
                    return defLs;
                }
            }
            return deserialize(context, packageName, dataSet);
        } catch (Throwable t) {
            t.printStackTrace();
            return new LedSettings(context, packageName);
        }
    }

    public static LedSettings deserialize(Set<String> dataSet) {
        return deserialize(null, null, dataSet);
    }

    private static LedSettings deserialize(Context context, String packageName, Set<String> dataSet) {
        LedSettings ls = new LedSettings(context, packageName);
        if (dataSet == null) {
            return ls;
        }
        for (String val : dataSet) {
            String[] data = val.split(":", 2);
            if (data[0].equals("enabled")) {
                ls.setEnabled(Boolean.valueOf(data[1]));
            } else if (data[0].equals("ongoing")) {
                ls.setOngoing(Boolean.valueOf(data[1]));
            } else if (data[0].equals("ledOnMs")) {
                ls.setLedOnMs(Integer.valueOf(data[1]));
            } else if (data[0].equals("ledOffMs")) {
                ls.setLedOffMs(Integer.valueOf(data[1]));
            } else if (data[0].equals("color")) {
                ls.setColor(Integer.valueOf(data[1]));
            } else if (data[0].equals("soundOverride")) {
                ls.setSoundOverride(Boolean.valueOf(data[1]));
            } else if (data[0].equals("sound")) {
                ls.setSoundUri(Uri.parse(data[1]));
            } else if (data[0].equals("soundOnlyOnce")) {
                ls.setSoundOnlyOnce(Boolean.valueOf(data[1]));
            } else if (data[0].equals("soundOnlyOnceTimeout")) {
                ls.setSoundOnlyOnceTimeout(Long.valueOf(data[1]));
            } else if (data[0].equals("insistent")) {
                ls.setInsistent(Boolean.valueOf(data[1]));
            } else if (data[0].equals("vibrateOverride")) {
                ls.setVibrateOverride(Boolean.valueOf(data[1]));
            } else if (data[0].equals("vibratePattern")) {
                ls.setVibratePatternFromString(data[1]);
            } else if (data[0].equals("activeScreenEnabled")) {
                ls.setActiveScreenEnabled(Boolean.valueOf(data[1]));
            } else if (data[0].equals("activeScreenExpanded")) {
                ls.setActiveScreenExpanded(Boolean.valueOf(data[1]));
            } else if (data[0].equals("ledMode")) {
                ls.setLedMode(LedMode.valueOf(data[1]));
            } else if (data[0].equals("qhIgnore")) {
                ls.setQhIgnore(Boolean.valueOf(data[1]));
            } else if (data[0].equals("qhIgnoreList")) {
                ls.setQhIgnoreList(data[1]);
            }
        }
        return ls;
    }

    private LedSettings(Context context, String packageName) {
        mContext = context;
        mPackageName = packageName;
        mEnabled = false;
        mOngoing = false;
        mLedOnMs = 1000;
        mLedOffMs = 5000;
        mColor = 0xffffffff;
        mSoundOverride = false;
        mSoundUri = null;
        mSoundOnlyOnce = false;
        mSoundOnlyOnceTimeout = 0;
        mInsistent = false;
        mVibrateOverride = false;
        mVibratePatternStr = null;
        mVibratePattern = null;
        mActiveScreenEnabled = false;
        mActiveScreenExpanded = false;
        mLedMode = LedMode.OVERRIDE;
        mQhIgnore = false;
        mQhIgnoreList = null;
    }

    protected static LedSettings getDefault(Context context) {
        return deserialize(context, "default");
    }

    protected static boolean isActiveScreenMasterEnabled(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                    "ledcontrol", Context.MODE_WORLD_READABLE);
            return prefs.getBoolean(PREF_KEY_ACTIVE_SCREEN_ENABLED, false);
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    protected static boolean isQuietHoursEnabled(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                    "ledcontrol", Context.MODE_WORLD_READABLE);
            return prefs.getBoolean(QuietHoursActivity.PREF_KEY_QH_ENABLED, false);
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static boolean isUncLocked(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                    "ledcontrol", Context.MODE_WORLD_READABLE);
            return prefs.getBoolean(PREF_KEY_LOCKED, false);
        } catch (Throwable t) {
            t.printStackTrace();
            return true;
        }
    }

    public static void lockUnc(Context context, boolean lock) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                    "ledcontrol", Context.MODE_WORLD_READABLE);
            prefs.edit().putBoolean(PREF_KEY_LOCKED, lock).commit();
            Intent intent = new Intent(ACTION_UNC_SETTINGS_CHANGED);
            context.sendBroadcast(intent);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected void setPackageName(String pkgName) {
        mPackageName = pkgName;
    }

    protected void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    protected void setOngoing(boolean ongoing) {
        mOngoing = ongoing;
    }

    protected void setLedOnMs(int ms) {
        mLedOnMs = ms;
    }

    protected void setLedOffMs(int ms) {
        mLedOffMs = ms;
    }

    protected void setColor(int color) {
        mColor = color;
    }

    protected void setSoundOverride(boolean override) {
        mSoundOverride = override;
    }

    protected void setSoundUri(Uri soundUri) {
        mSoundUri = soundUri;
    }

    protected void setSoundOnlyOnce(boolean onlyOnce) {
        mSoundOnlyOnce = onlyOnce;
    }

    protected void setSoundOnlyOnceTimeout(long timeout) {
        mSoundOnlyOnceTimeout = timeout;
    }

    protected void setInsistent(boolean insistent) {
        mInsistent = insistent;
    }

    protected void setVibrateOverride(boolean override) {
        mVibrateOverride = override;
    }

    protected static long[] parseVibratePatternString(String patternStr) throws Exception {
        String[] vals = patternStr.split(",");
        long[] pattern = new long[vals.length];
        for (int i=0; i<pattern.length; i++) {
            pattern[i] = Long.valueOf(vals[i]);
        }
        return pattern;
    }

    protected void setVibratePatternFromString(String pattern) {
        mVibratePatternStr = pattern == null || pattern.isEmpty() ?
                null : pattern;
        mVibratePattern = null;
        if (mVibratePatternStr != null) {
            try {
                mVibratePattern = parseVibratePatternString(mVibratePatternStr);
            } catch (Exception e) {
                mVibratePatternStr = null;
            }
        }
    }

    protected void setActiveScreenEnabled(boolean enabled) {
        mActiveScreenEnabled = enabled;
    }

    protected void setActiveScreenExpanded(boolean expanded) {
        mActiveScreenExpanded = expanded;
    }

    protected void setLedMode(LedMode ledMode) {
        mLedMode = ledMode;
    }

    protected void setQhIgnore(boolean ignore) {
        mQhIgnore = ignore;
    }

    protected void setQhIgnoreList(String ignoreList) {
        mQhIgnoreList = ignoreList;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public boolean getEnabled() {
        return mEnabled;
    }

    public boolean getOngoing() {
        return mOngoing;
    }

    public int getLedOnMs() {
        return mLedOnMs;
    }

    public int getLedOffMs() {
        return mLedOffMs;
    }

    public int getColor() {
        return mColor;
    }

    public boolean getSoundOverride() {
        return mSoundOverride;
    }

    public Uri getSoundUri() {
        return mSoundUri;
    }

    public boolean getSoundOnlyOnce() {
        return mSoundOnlyOnce;
    }

    public long getSoundOnlyOnceTimeout() {
        return mSoundOnlyOnceTimeout;
    }

    public boolean getInsistent() {
        return mInsistent;
    }

    public boolean getVibrateOverride() {
        return mVibrateOverride;
    }

    public String getVibratePatternAsString() {
        return mVibratePatternStr;
    }

    public long[] getVibratePattern() {
        return mVibratePattern;
    }

    public boolean getActiveScreenEnabled() {
        return mActiveScreenEnabled;
    }

    public boolean getActiveScreenExpanded() {
        return mActiveScreenExpanded;
    }

    public LedMode getLedMode() {
        return mLedMode;
    }

    public boolean getQhIgnore() {
        return mQhIgnore;
    }

    public String getQhIgnoreList() {
        return mQhIgnoreList;
    }

    protected void serialize() {
        try {
            Set<String> dataSet = new HashSet<String>();
            dataSet.add("enabled:" + mEnabled);
            dataSet.add("ongoing:" + mOngoing);
            dataSet.add("ledOnMs:" + mLedOnMs);
            dataSet.add("ledOffMs:" + mLedOffMs);
            dataSet.add("color:" + mColor);
            dataSet.add("soundOverride:" + mSoundOverride);
            if (mSoundUri != null) {
                dataSet.add("sound:" + mSoundUri.toString());
            }
            dataSet.add("soundOnlyOnce:" + mSoundOnlyOnce);
            dataSet.add("soundOnlyOnceTimeout:" + mSoundOnlyOnceTimeout);
            dataSet.add("insistent:" + mInsistent);
            dataSet.add("vibrateOverride:" + mVibrateOverride);
            if (mVibratePatternStr != null) {
                dataSet.add("vibratePattern:" + mVibratePatternStr);
            }
            dataSet.add("activeScreenEnabled:" + mActiveScreenEnabled);
            dataSet.add("activeScreenExpanded:" + mActiveScreenExpanded);
            dataSet.add("ledMode:" + mLedMode);
            dataSet.add("qhIgnore:" + mQhIgnore);
            if (mQhIgnoreList != null) {
                dataSet.add("qhIgnoreList:" + mQhIgnoreList);
            }
            SharedPreferences prefs = mContext.getSharedPreferences(
                    "ledcontrol", Context.MODE_WORLD_READABLE);
            prefs.edit().putStringSet(mPackageName, dataSet).commit();
            Intent intent = new Intent(ACTION_UNC_SETTINGS_CHANGED);
            mContext.sendBroadcast(intent);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public String toString() {
        String buf = "[" + mPackageName + "," + mEnabled + "," + mColor + "," + mLedOnMs + 
                "," + mLedOffMs + "," + mOngoing + ";" + mSoundOverride + ";" + 
                mSoundUri + ";" + mSoundOnlyOnce + ";" + mInsistent + "]";
        return buf;
    }
}
