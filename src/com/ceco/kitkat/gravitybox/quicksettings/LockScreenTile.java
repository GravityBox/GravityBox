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

package com.ceco.kitkat.gravitybox.quicksettings;

import com.ceco.kitkat.gravitybox.R;

import de.robv.android.xposed.XposedBridge;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.provider.Settings;
import android.view.View;

@SuppressWarnings("deprecation")
public class LockScreenTile extends BasicTile {
    private static final String TAG = "GB:LockScreenTile";

    private boolean mLockScreenEnabled;
    private KeyguardLock mKeyguardLock;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public LockScreenTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLockscreenState();
            }
        };

        mOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startActivity(Settings.ACTION_SECURITY_SETTINGS);
                return true;
            }
        };

        mLockScreenEnabled = true;
    }

    @Override
    protected int onGetLayoutId() {
        return R.layout.quick_settings_tile_lock_screen;
    }

    @Override
    protected synchronized void updateTile() {
        if (mLockScreenEnabled) {
            mLabel = mGbContext.getString(R.string.quick_settings_lock_screen_on);
            mDrawableId = R.drawable.ic_qs_lock_screen_on;
        } else {
            mLabel = mGbContext.getString(R.string.quick_settings_lock_screen_off);
            mDrawableId = R.drawable.ic_qs_lock_screen_off;
        }

        super.updateTile();
    }

    private void toggleLockscreenState() {
        try {
            if (mKeyguardLock == null) {
                final KeyguardManager kgManager = 
                        (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
                mKeyguardLock = kgManager.newKeyguardLock(TAG);
            }
            if (mKeyguardLock != null) {
                mLockScreenEnabled = !mLockScreenEnabled;
                if (mLockScreenEnabled) {
                    mKeyguardLock.reenableKeyguard();
                } else {
                    mKeyguardLock.disableKeyguard();
                }
                updateResources();
            }
        } catch (Throwable t) {
            log("Error toggling lock screen state: " + t.getMessage());
        }
    }
}
