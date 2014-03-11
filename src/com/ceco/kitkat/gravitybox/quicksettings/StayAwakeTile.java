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

package com.ceco.kitkat.gravitybox.quicksettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.ceco.kitkat.gravitybox.GravityBoxSettings;
import com.ceco.kitkat.gravitybox.R;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;

public class StayAwakeTile extends BasicTile {
    private static final String TAG = "GB:StayAwakeTile";
    private static final boolean DEBUG = false;

    private static final int NEVER_SLEEP = Integer.MAX_VALUE;
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static ScreenTimeout[] SCREEN_TIMEOUT = new ScreenTimeout[] {
        new ScreenTimeout(15000, R.string.stay_awake_15s),
        new ScreenTimeout(30000, R.string.stay_awake_30s),
        new ScreenTimeout(60000, R.string.stay_awake_1m),
        new ScreenTimeout(120000, R.string.stay_awake_2m),
        new ScreenTimeout(300000, R.string.stay_awake_5m),
        new ScreenTimeout(600000, R.string.stay_awake_10m),
        new ScreenTimeout(1800000, R.string.stay_awake_30m),
        new ScreenTimeout(NEVER_SLEEP, R.string.quick_settings_stay_awake_on),
    };

    private SettingsObserver mSettingsObserver;
    private int mCurrentTimeoutIndex;
    private int mPreviousTimeoutIndex;
    private int mLongestTimeoutIndex;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static class ScreenTimeout {
        final int mMillis;
        final int mLabelResId;
        boolean mEnabled;

        public ScreenTimeout(int millis, int labelResId) {
            mMillis = millis;
            mLabelResId = labelResId;
            mEnabled = false;
        }
    }

    public StayAwakeTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mDrawableId = R.drawable.ic_qs_stayawake_on;

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleStayAwake();
            }
        };

        mOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mLongestTimeoutIndex < 0)
                    return false;
                if (Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT,
                            FALLBACK_SCREEN_TIMEOUT_VALUE) == SCREEN_TIMEOUT[mLongestTimeoutIndex].mMillis) {
                    toggleStayAwake(mPreviousTimeoutIndex);
                } else {
                    mPreviousTimeoutIndex = mCurrentTimeoutIndex == -1 ?
                            getIndexFromValue(FALLBACK_SCREEN_TIMEOUT_VALUE) : mCurrentTimeoutIndex;
                    toggleStayAwake(mLongestTimeoutIndex);
                }
                return true;
            }
        };

        mCurrentTimeoutIndex = getIndexFromValue(Settings.System.getInt(mContext.getContentResolver(), 
                Settings.System.SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE));

        mPreviousTimeoutIndex = mCurrentTimeoutIndex == -1 ||
                SCREEN_TIMEOUT[mCurrentTimeoutIndex].mMillis == SCREEN_TIMEOUT[mLongestTimeoutIndex].mMillis ?
                        getIndexFromValue(FALLBACK_SCREEN_TIMEOUT_VALUE) : mCurrentTimeoutIndex;

        if (DEBUG) log("mCurrentTimeoutIndex = " + mCurrentTimeoutIndex +
                "; mPreviousTimeoutIndex = " + mPreviousTimeoutIndex);
    }

    @Override
    protected int onGetLayoutId() {
        return R.layout.quick_settings_tile_stay_awake;
    }

    @Override
    protected void onTilePostCreate() {
        mSettingsObserver = new SettingsObserver(new Handler());
        mSettingsObserver.observe();

        super.onTilePostCreate();
    }

    @Override
    protected synchronized void updateTile() {
        if (mCurrentTimeoutIndex == -1) {
            mLabel = String.format("%ds", TimeUnit.MILLISECONDS.toSeconds(
                    Settings.System.getInt(mContext.getContentResolver(), 
                            Settings.System.SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE)));;
        } else {
            mLabel = mGbContext.getString(SCREEN_TIMEOUT[mCurrentTimeoutIndex].mLabelResId);
        }

        super.updateTile();
    }

    @Override
    public void onPreferenceInitialize(XSharedPreferences prefs) {
        Set<String> smodes = prefs.getStringSet(
                GravityBoxSettings.PREF_STAY_AWAKE_TILE_MODE,
                new HashSet<String>(Arrays.asList(new String[] { "0", "1", "2", "3", "4", "5", "6", "7" })));
        List<String> lmodes = new ArrayList<String>(smodes);
        Collections.sort(lmodes);
        int modes[] = new int[lmodes.size()];
        for (int i = 0; i < lmodes.size(); i++) {
            modes[i] = Integer.valueOf(lmodes.get(i));
        }
        if (DEBUG) log("onPreferenceInitialize: modes=" + modes);
        updateSettings(modes);

        super.onPreferenceInitialize(prefs);
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        if (DEBUG) log("Received broadcast: " + intent.toString());

        if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_QUICKSETTINGS_CHANGED)
                && intent.hasExtra(GravityBoxSettings.EXTRA_SA_MODE)) {
            int[] modes = intent.getIntArrayExtra(GravityBoxSettings.EXTRA_SA_MODE);
            if (DEBUG) log("onBroadcastReceived: modes=" + modes);
            updateSettings(modes);
        }

        super.onBroadcastReceived(context, intent);
    }

    private void updateSettings(int[] modes) {
        for (ScreenTimeout s : SCREEN_TIMEOUT) {
            s.mEnabled = false;
        }
        if (modes != null && modes.length > 0) {
            for (int i=0; i<modes.length; i++) {
                int index = modes[i];
                ScreenTimeout s = index < SCREEN_TIMEOUT.length ? SCREEN_TIMEOUT[index] : null;
                if (s != null) {
                    s.mEnabled = true;
                    mLongestTimeoutIndex = index;
                }
            }
        } else {
            mCurrentTimeoutIndex = getIndexFromValue(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE));
            mLongestTimeoutIndex = -1;
        }
    }

    private void toggleStayAwake() {
        toggleStayAwake(-1);
    }

    private void toggleStayAwake(int index) {
        if (mLongestTimeoutIndex < 0)
            return;
        final int startIndex = mCurrentTimeoutIndex;
        int i = 0;
        do {
            if (index != -1) {
                if (index == mLongestTimeoutIndex) {
                    mCurrentTimeoutIndex = mLongestTimeoutIndex;
                } else if (SCREEN_TIMEOUT[index].mEnabled) {
                    mCurrentTimeoutIndex = index;
                } else {
                    mCurrentTimeoutIndex = i++;
                    index = i;
                }
            } else {
                if (++mCurrentTimeoutIndex >= SCREEN_TIMEOUT.length) {
                    mCurrentTimeoutIndex = 0;
                }
            }
        } while(!SCREEN_TIMEOUT[mCurrentTimeoutIndex].mEnabled &&
                    startIndex != mCurrentTimeoutIndex);
        if (DEBUG) log("mCurrentTimeoutIndex = " + mCurrentTimeoutIndex);

        if (startIndex != mCurrentTimeoutIndex) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT,
                        SCREEN_TIMEOUT[mCurrentTimeoutIndex].mMillis);
        } else {
            mCurrentTimeoutIndex = getIndexFromValue(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE));
        }
    }

    private int getIndexFromValue(int value) {
        for (int i = 0; i < SCREEN_TIMEOUT.length; i++) {
            if (SCREEN_TIMEOUT[i].mMillis == value) {
                return i;
            }
        }
        return -1;
    }

    class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SCREEN_OFF_TIMEOUT), 
                    false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            mCurrentTimeoutIndex = getIndexFromValue(
                    Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE));
            if (DEBUG) log("SettingsObserver onChange; mCurrentTimeoutIndex = " + mCurrentTimeoutIndex);
            updateResources();
        }
    }
}
