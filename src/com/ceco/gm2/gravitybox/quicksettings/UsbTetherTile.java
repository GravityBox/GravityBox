/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.ceco.gm2.gravitybox.quicksettings;

import com.ceco.gm2.gravitybox.R;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.view.View;

public class UsbTetherTile extends BasicTile {
    public static final String ACTION_TETHER_STATE_CHANGED = "android.net.conn.TETHER_STATE_CHANGED";
    public static final String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";
    public static final String ACTION_MEDIA_UNSHARED = "android.intent.action.MEDIA_UNSHARED";
    public static final String USB_CONNECTED = "connected";
    public static final int TETHER_ERROR_NO_ERROR = 0;

    private boolean mUsbTethered = false;
    private boolean mUsbConnected = false;
    private boolean mMassStorageActive = false;
    private String[] mUsbRegexs;

    public UsbTetherTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUsbConnected) {
                    setUsbTethering(!mUsbTethered);
                }
            }
        };

        mOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
                startActivity(intent);
                return true;
            }
        };
    }

    @Override
    protected int onGetLayoutId() {
        return R.layout.quick_settings_tile_usb_tether;
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        super.onBroadcastReceived(context, intent);

        if (intent.getAction().equals(ACTION_TETHER_STATE_CHANGED)) {
            updateResources();
        }

        if (intent.getAction().equals(ACTION_USB_STATE)) {
            mUsbConnected = intent.getBooleanExtra(USB_CONNECTED, false);
            mTile.setVisibility(mUsbConnected ? View.VISIBLE : View.GONE);
            updateResources();
        }

        if (intent.getAction().equals(Intent.ACTION_MEDIA_SHARED)) {
            mMassStorageActive = true;
            updateResources();
        }

        if (intent.getAction().equals(ACTION_MEDIA_UNSHARED)) {
            mMassStorageActive = false;
            updateResources();
        }
    }

    @Override
    protected void updateTile() {
        updateState();
        if (mUsbConnected && !mMassStorageActive) {
            if (mUsbTethered) {
                mDrawableId = R.drawable.ic_qs_usb_tether_on;
                mLabel = mGbContext.getString(R.string.quick_settings_usb_tether_on);
                mTileColor = KK_COLOR_ON;
            } else {
                mDrawableId = R.drawable.ic_qs_usb_tether_connected;
                mLabel = mGbContext.getString(R.string.quick_settings_usb_tether_connected);
                mTileColor = KK_COLOR_OFF;
            }
        } else {
            mDrawableId = R.drawable.ic_qs_usb_tether_off;
            mLabel = mGbContext.getString(R.string.quick_settings_usb_tether_off);
            mTileColor = KK_COLOR_OFF;
        }
        mTile.setVisibility(mUsbConnected ? View.VISIBLE : View.GONE);

        super.updateTile();
    }

    private void updateState() {
        try {
            ConnectivityManager cm = 
                    (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            mUsbRegexs = (String[]) XposedHelpers.callMethod(cm, "getTetherableUsbRegexs");
            String[] available = (String[]) XposedHelpers.callMethod(cm, "getTetherableIfaces");
            String[] tethered = (String[]) XposedHelpers.callMethod(cm, "getTetheredIfaces");
            String[] errored = (String[]) XposedHelpers.callMethod(cm, "getTetheringErroredIfaces");
            updateState(available, tethered, errored);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private void updateState(String[] available, String[] tethered,
            String[] errored) {
        updateUsbState(available, tethered, errored);
    }

    private void updateUsbState(String[] available, String[] tethered,
            String[] errored) {

        mUsbTethered = false;
        for (String s : tethered) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) mUsbTethered = true;
            }
        }
    }

    private void setUsbTethering(boolean enabled) {
        try {
            ConnectivityManager cm = 
                    (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            XposedHelpers.callMethod(cm, "setUsbTethering", enabled);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
