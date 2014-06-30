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

import com.ceco.kitkat.gravitybox.GravityBoxSettings;
import com.ceco.kitkat.gravitybox.ModExpandedDesktop;
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

public class ExpandedDesktopTile extends BasicTile {
    private static final String TAG = "GB:ExpandedDesktopTile";

    private int mMode;
    private boolean mExpanded;
    private Handler mHandler;
    private SettingsObserver mSettingsObserver;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public ExpandedDesktopTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMode != GravityBoxSettings.ED_DISABLED) {
                    collapsePanels();
                    // give panels chance to collapse before changing expanded desktop state
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Settings.Global.putInt(mContext.getContentResolver(),
                                    ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE,
                                    (mExpanded ? 0 : 1));
                        }
                    }, 300);
                }
            }
        };
    }

    @Override
    protected int onGetLayoutId() {
        return R.layout.quick_settings_tile_expanded_desktop;
    }

    class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(Settings.Global.getUriFor(
                   ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE), false, this);
        }

        @Override 
        public void onChange(boolean selfChange) {
            updateResources();
        }
    }

    @Override
    protected void onTilePostCreate() {
        mHandler = new Handler();
        mSettingsObserver = new SettingsObserver(mHandler);
        mSettingsObserver.observe();

        super.onTilePostCreate();
    }

    @Override
    protected synchronized void updateTile() {
        mExpanded = (Settings.Global.getInt(mContext.getContentResolver(),
                ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE, 0) == 1)
                && (mMode > 0);

        if (mExpanded) {
            mLabel = mGbContext.getString(R.string.quick_settings_expanded_desktop_expanded);
            mDrawableId = R.drawable.ic_qs_expanded_desktop_on;
        } else {
            mLabel = (mMode == GravityBoxSettings.ED_DISABLED) ? 
                    mGbContext.getString(R.string.quick_settings_expanded_desktop_disabled) :
                        mGbContext.getString(R.string.quick_settings_expanded_desktop_normal);
            mDrawableId = R.drawable.ic_qs_expanded_desktop_off;
        }

        super.updateTile();
    }

    @Override
    public void onPreferenceInitialize(XSharedPreferences prefs) {
        mMode = GravityBoxSettings.ED_DISABLED;
        try {
            mMode = Integer.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_EXPANDED_DESKTOP, "0"));
        } catch (NumberFormatException nfe) {
            log("Invalid value for PREF_KEY_EXPANDED_DESKTOP preference");
        }
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        super.onBroadcastReceived(context, intent);

        if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_EXPANDED_DESKTOP_MODE_CHANGED) &&
                intent.hasExtra(GravityBoxSettings.EXTRA_ED_MODE)) {
            mMode = intent.getIntExtra(GravityBoxSettings.EXTRA_ED_MODE, GravityBoxSettings.ED_DISABLED);
            updateResources();
        }
    }
}
