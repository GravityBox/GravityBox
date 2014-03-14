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

package com.ceco.gm2.gravitybox.quicksettings;

import com.ceco.gm2.gravitybox.GravityBoxSettings;
import com.ceco.gm2.gravitybox.R;
import com.ceco.gm2.gravitybox.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.View;

public class RingerModeTile extends BasicTile {
    private static final String TAG = "GB:RingerModeTile";
    private static final boolean DEBUG = false;

    public static final String SETTING_VIBRATE_WHEN_RINGING = "vibrate_when_ringing";
    public static final String SETTING_MODE_RINGER = "mode_ringer";

    // Define the available ringer modes
    private static final Ringer[] RINGERS = new Ringer[] {
        new Ringer(AudioManager.RINGER_MODE_SILENT, false, R.drawable.ic_qs_ring_off),
        new Ringer(AudioManager.RINGER_MODE_VIBRATE, true, R.drawable.ic_qs_vibrate_on),
        new Ringer(AudioManager.RINGER_MODE_NORMAL, false, R.drawable.ic_qs_ring_on),
        new Ringer(AudioManager.RINGER_MODE_NORMAL, true, R.drawable.ic_qs_ring_vibrate_on)
    };

    private int mRingerIndex;
    private AudioManager mAudioManager;
    private boolean mHasVibrator;
    private Vibrator mVibrator;
    private SettingsObserver mSettingsObserver;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public RingerModeTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mHasVibrator = Utils.hasVibrator(mContext);
        if (mHasVibrator) {
            mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        }

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleState();
            }
        };

        mOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startActivity(android.provider.Settings.ACTION_SOUND_SETTINGS);
                return true;
            }
        };
    }

    @Override
    protected int onGetLayoutId() {
        return R.layout.quick_settings_tile_ringer_mode;
    }

    @Override
    protected void onTilePostCreate() {
        if (mHasVibrator) {
            mSettingsObserver = new SettingsObserver(new Handler());
            mSettingsObserver.observe();
        }

        super.onTilePostCreate();
    }

    @Override
    protected synchronized void updateTile() {
        // The title does not change
        mLabel = mGbContext.getString(R.string.qs_tile_ringer_mode);

        // The icon will change depending on index
        findCurrentState();
        mDrawableId = RINGERS[mRingerIndex].mDrawable;

        mTileColor = (mDrawableId == R.drawable.ic_qs_ring_off ? 
                KK_COLOR_OFF : KK_COLOR_ON);

        super.updateTile();
    }

    @Override
    public void onPreferenceInitialize(XSharedPreferences prefs) {
        Set<String> smodes = prefs.getStringSet(
                GravityBoxSettings.PREF_KEY_RINGER_MODE_TILE_MODE,
                new HashSet<String>(Arrays.asList(new String[] { "0", "1", "2", "3" })));
        List<String> lmodes = new ArrayList<String>(smodes);
        Collections.sort(lmodes);
        int modes[] = new int[lmodes.size()];
        for (int i=0; i<lmodes.size(); i++) {
            modes[i] = Integer.valueOf(lmodes.get(i));
        }
        if (DEBUG) log("onPreferenceInitialize: modes=" + modes);
        updateSettings(modes);

        super.onPreferenceInitialize(prefs);
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        if (DEBUG) log("Received broadcast: " + intent.toString());

        if (intent.getAction().equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
            updateResources();
        } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_QUICKSETTINGS_CHANGED)
                && intent.hasExtra(GravityBoxSettings.EXTRA_RMT_MODE)) {
            int[] modes = intent.getIntArrayExtra(GravityBoxSettings.EXTRA_RMT_MODE);
            if (DEBUG) log("onBroadcastReceived: modes=" + modes);
            updateSettings(modes);
        }

        super.onBroadcastReceived(context, intent);
    }

    private void updateSettings(int[] modes) {
        // disable all first
        for (Ringer r : RINGERS) {
            r.mEnabled = false;
        }

        // enable only those present in the list taking into account if device has vibrator
        if (modes != null && modes.length > 0) {
            for (int i=0; i<modes.length; i++) {
                int index = modes[i];
                Ringer r = index < RINGERS.length ? RINGERS[index] : null;
                if (r != null && (mHasVibrator || !r.mVibrateWhenRinging)) {
                    r.mEnabled = true;
                }
            }
        }
    }

    private void toggleState() {
        // search for next suitable mode
        // loop will break as soon as new suitable index is found
        // or when we end up with the same index we started from (which means, no suitable index was found)
        final int startIndex = mRingerIndex;
        do {
            if (++mRingerIndex >= RINGERS.length) {
                mRingerIndex = 0;
            }
        } while(!RINGERS[mRingerIndex].mEnabled &&
                    mRingerIndex != startIndex);

        // toggle only if new ringer index found
        if (mRingerIndex != startIndex) {
            if (DEBUG) log("Switching to ringerIndex: " + mRingerIndex);
            Ringer r = RINGERS[mRingerIndex];
    
            // If we are setting a vibrating state, vibrate to indicate it
            if (r.mVibrateWhenRinging && mVibrator != null) {
                mVibrator.vibrate(150);
            }
    
            // Set the desired state
            if (r.mRingerMode != AudioManager.RINGER_MODE_SILENT) {
                ContentResolver resolver = mContext.getContentResolver();
                Settings.System.putInt(resolver, SETTING_VIBRATE_WHEN_RINGING,
                        r.mVibrateWhenRinging ? 1 : 0);
            }
            mAudioManager.setRingerMode(r.mRingerMode);
        } else if (DEBUG) {
            log("No suitable ringer mode for toggling found");
        }
    }

    private void findCurrentState() {
        boolean vibrateWhenRinging = Settings.System.getInt(mContext.getContentResolver(),
                SETTING_VIBRATE_WHEN_RINGING, 0) == 1;
        int ringerMode = mAudioManager.getRingerMode();

        mRingerIndex = 0;

        for (int i = 0; i < RINGERS.length; i++) {
            Ringer r = RINGERS[i];
            if (ringerMode == r.mRingerMode && 
                ((ringerMode == AudioManager.RINGER_MODE_SILENT || 
                        ringerMode == AudioManager.RINGER_MODE_VIBRATE) ||
                        (r.mVibrateWhenRinging == vibrateWhenRinging))) {
                mRingerIndex = i;
                if (DEBUG) log("Current ringerIndex=" + mRingerIndex);
                break;
            }
        }
    }

    private class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                        SETTING_VIBRATE_WHEN_RINGING), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (DEBUG) log("SettingsObserver onChange()");
            updateResources();
        }
    }

    private static class Ringer {
        final boolean mVibrateWhenRinging;
        final int mRingerMode;
        final int mDrawable;
        boolean mEnabled;

        Ringer(int ringerMode, boolean vibrateWhenRinging, int drawable) {
            mVibrateWhenRinging = vibrateWhenRinging;
            mRingerMode = ringerMode;
            mDrawable = drawable;
            mEnabled = false;
        }
    }
}
