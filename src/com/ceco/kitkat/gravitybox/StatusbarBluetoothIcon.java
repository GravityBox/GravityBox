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
package com.ceco.kitkat.gravitybox;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class StatusbarBluetoothIcon implements BroadcastSubReceiver {
    private static final String TAG = "GB:StatusbarBluetoothIcon";
    private static final boolean DEBUG = false;

    private static final String CLASS_PHONE_STATUSBAR_POLICY = 
            "com.android.systemui.statusbar.phone.PhoneStatusBarPolicy";

    private enum Mode { DEFAULT, CONNECTED, HIDDEN };
    private Object mSbService;
    private Mode mMode;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public StatusbarBluetoothIcon(ClassLoader classLoader, XSharedPreferences prefs) {
        mMode = Mode.valueOf(prefs.getString(
                GravityBoxSettings.PREF_KEY_STATUSBAR_BT_VISIBILITY, "DEFAULT"));

        createHooks(classLoader);
    }

    private void createHooks(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(CLASS_PHONE_STATUSBAR_POLICY, classLoader, 
                    "updateBluetooth", Intent.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mSbService == null) {
                        mSbService = XposedHelpers.getObjectField(param.thisObject, "mService");
                        if (DEBUG) log ("mSbService set");
                    }
                    if (mMode != Mode.DEFAULT) {
                        updateBtIconVisibility();
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private void updateBtIconVisibility() {
        if (mSbService == null || mMode == null) return;

        try {
            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            if (btAdapter != null) {
                boolean enabled = btAdapter.getState() == BluetoothAdapter.STATE_ON;
                boolean connected = (Integer) XposedHelpers.callMethod(btAdapter, "getConnectionState") ==
                        BluetoothAdapter.STATE_CONNECTED;
                boolean visible;
                switch (mMode) {
                    default:
                    case DEFAULT: visible = enabled; break;
                    case CONNECTED: visible = connected; break;
                    case HIDDEN: visible = false; break;
                }
                if (DEBUG) log("updateBtIconVisibility: enabled=" + enabled + "; connected=" + connected +
                        "; visible=" + visible);
                XposedHelpers.callMethod(mSbService, "setIconVisibility", "bluetooth", visible);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_STATUSBAR_BT_VISIBILITY_CHANGED) &&
                intent.hasExtra(GravityBoxSettings.EXTRA_SB_BT_VISIBILITY)) {
            try {
                mMode = Mode.valueOf(intent.getStringExtra(GravityBoxSettings.EXTRA_SB_BT_VISIBILITY));
            } catch (Throwable t) { 
                log("Invalid Mode value: " + t.getMessage());
            }
            updateBtIconVisibility();
        }
    }
}
