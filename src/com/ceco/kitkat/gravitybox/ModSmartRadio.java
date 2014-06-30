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

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModSmartRadio {
    private static final String TAG = "GB:SmartRadio";
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final boolean DEBUG = false;

    public static final String SETTING_SMART_RADIO_ENABLED = "gb_smart_radio_enabled";
    public static final String SETTING_SMART_RADIO_STATE = "gb_smart_radio_state";
    public static final String ACTION_TOGGLE_SMART_RADIO = "gravitybox.intent.action.TOGGLE_SMART_RADIO";

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static enum State { UNKNOWN, NORMAL, POWER_SAVING };

    private static Context mContext;
    private static int mNormalMode;
    private static int mPowerSavingMode;
    private static ConnectivityManager mConnManager;
    private static State mCurrentState = State.UNKNOWN;
    private static boolean mIsScreenOff;
    private static boolean mPowerSaveWhenScreenOff;
    private static boolean mIgnoreWhileLocked;
    private static NetworkModeChanger mNetworkModeChanger;
    private static int mModeChangeDelay;
    private static KeyguardManager mKeyguardManager;
    private static int mScreenOffDelay;
    private static boolean mSmartRadioEnabled;
    private static boolean mIgnoreMobileDataAvailability;

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_SMART_RADIO_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_SR_NORMAL_MODE)) {
                    setNewModeValue(State.NORMAL, 
                            intent.getIntExtra(GravityBoxSettings.EXTRA_SR_NORMAL_MODE, -1));
                    if (DEBUG) log("mNormalMode = " + mNormalMode);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_SR_POWER_SAVING_MODE)) {
                    setNewModeValue(State.POWER_SAVING,
                            intent.getIntExtra(GravityBoxSettings.EXTRA_SR_POWER_SAVING_MODE, -1));
                    if (DEBUG) log("mPowerSavingMode = " + mPowerSavingMode);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_SR_SCREEN_OFF)) {
                    mPowerSaveWhenScreenOff = intent.getBooleanExtra(GravityBoxSettings.EXTRA_SR_SCREEN_OFF, false);
                    if (DEBUG) log("mPowerSaveWhenScreenOff = " + mPowerSaveWhenScreenOff);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_SR_IGNORE_LOCKED)) {
                    mIgnoreWhileLocked = intent.getBooleanExtra(GravityBoxSettings.EXTRA_SR_IGNORE_LOCKED, true);
                    if (DEBUG) log("mIgnoreWhileLocked = " + mIgnoreWhileLocked);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_SR_MODE_CHANGE_DELAY)) {
                    mModeChangeDelay = intent.getIntExtra(GravityBoxSettings.EXTRA_SR_MODE_CHANGE_DELAY, 5);
                    if (DEBUG) log("mModeChangeDelay = " + mModeChangeDelay);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_SR_SCREEN_OFF_DELAY)) {
                    mScreenOffDelay = intent.getIntExtra(GravityBoxSettings.EXTRA_SR_SCREEN_OFF_DELAY, 0);
                    if (DEBUG) log("mScreenOffDelay = " + mScreenOffDelay);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_SR_MDA_IGNORE)) {
                    mIgnoreMobileDataAvailability = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_SR_MDA_IGNORE, false);
                }
            } else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                int nwType = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, -1);
                if (nwType == -1) return;
                NetworkInfo nwInfo = mConnManager.getNetworkInfo(nwType);
                if (nwType == ConnectivityManager.TYPE_WIFI ||
                        nwType == ConnectivityManager.TYPE_MOBILE) {
                    if (DEBUG) log("Network type: " + nwType + "; connected: " + nwInfo.isConnected());
                    if (shouldSwitchToNormalState()) {
                        switchToState(State.NORMAL);
                    } else {
                        switchToState(State.POWER_SAVING);
                    }
                }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                if (DEBUG) log("Screen turning off");
                mIsScreenOff = true;
                if (mPowerSaveWhenScreenOff && !isTetheringViaMobileNetwork()) {
                    switchToState(State.POWER_SAVING, true);
                }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                if (DEBUG) log("Screen turning on");
                mIsScreenOff = false;
                if (shouldSwitchToNormalState()) {
                    switchToState(State.NORMAL);
                }
            } else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                if (DEBUG) log("Keyguard unlocked");
                if (shouldSwitchToNormalState()) {
                    switchToState(State.NORMAL);
                }
            } else if (intent.getAction().equals(ACTION_TOGGLE_SMART_RADIO)) {
                toggleSmartRadio();
            }

            if (mNetworkModeChanger != null) {
                mNetworkModeChanger.onBroadcastReceived(context, intent);
            }
        }
    };

    private static boolean isMobileDataEnabled() {
        try {
            return (Boolean) XposedHelpers.callMethod(mConnManager, "getMobileDataEnabled");
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isMobileNetworkAvailable() {
        if (mIgnoreMobileDataAvailability) {
            return true;
        }
        try {
            return mConnManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isWifiConnected() {
        try {
            return mConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isTetheringViaMobileNetwork() {
        try {
            String[] wifiRegexs = (String[]) XposedHelpers.callMethod(mConnManager, "getTetherableWifiRegexs");
            String[] usbRegexs = (String[]) XposedHelpers.callMethod(mConnManager, "getTetherableUsbRegexs");
            String[] btRegexes = (String[]) XposedHelpers.callMethod(mConnManager, "getTetherableBluetoothRegexs");
            String[] tetheredIfaces = (String[]) XposedHelpers.callMethod(mConnManager, "getTetheredIfaces");

            for (String tiface : tetheredIfaces) {
                // if wifi tethering active it's obvious it goes via mobile network
                for (String regex : wifiRegexs) {
                    if (tiface.matches(regex)) {
                        if (DEBUG) log("isTetheringViaMobileNetwork: WiFi tethering enabled");
                        return true;
                    }
                }

                // if not WiFi connected check for USB and BT tethering
                if (!isWifiConnected()) {
                    for (String regex : usbRegexs) {
                        if (tiface.matches(regex)) {
                            if (DEBUG) log("isTetheringViaMobileNetwork: USB tethering enabled and WiFi not connected");
                            return true;
                        }
                    }
                    for (String regex : btRegexes) {
                        if (tiface.matches(regex)) {
                            if (DEBUG) log("isTetheringViaMobileNetwork: BT tethering enabled and WiFi not connected");
                            return true;
                        }
                    }
                }
            }

            if (DEBUG) log("isTetheringViaMobileNetwork: nope");
            return false;
        } catch (Throwable t) {
            log("isTetheringViaMobileNetwork: " + t.getMessage());
            return false;
        }
    }

    private static boolean isKeyguardLocked() {
        try {
            return mKeyguardManager.isKeyguardLocked();
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean shouldSwitchToNormalState() {
        // basic rules
        boolean shouldSwitch = isMobileNetworkAvailable() && 
                                   isMobileDataEnabled() &&
                                   !isWifiConnected();
        // additional rules
        if (mPowerSaveWhenScreenOff && !isTetheringViaMobileNetwork()) {
            shouldSwitch &= !mIsScreenOff;
            shouldSwitch &= !(isKeyguardLocked() && mIgnoreWhileLocked);
        }

        return shouldSwitch;
    }

    private static void switchToState(State newState) {
        switchToState(newState, false);
    }

    private static void switchToState(State newState, boolean force) {
        if (!mSmartRadioEnabled) {
            if (DEBUG) log("switchToState: Smart Radio is disabled - ignoring");
            return;
        } else if (mCurrentState == newState && !force) {
            if (DEBUG) log("switchToState: new state == previous state - ignoring");
            return;
        } else if (!isMobileNetworkAvailable()) {
            // force power saving state no matter what so we start with it when mobile network is available again
            if (DEBUG) log("switchToState: mobile network unavailable - resetting to POWER_SAVING state");
            newState = State.POWER_SAVING;
        } else if (DEBUG) {
            log("Switching to state: " + newState);
        }

        try {
            int networkMode = -1;
            switch (newState) {
                case NORMAL: networkMode = mNormalMode; break;
                case POWER_SAVING: networkMode = mPowerSavingMode; break;
                default: break;
            }
            mCurrentState = newState;
            Settings.System.putString(mContext.getContentResolver(),
                    SETTING_SMART_RADIO_STATE, mCurrentState.toString());
            mNetworkModeChanger.changeNetworkMode(networkMode);
        } catch (Throwable t) {
            log("switchToState: " + t.getMessage());
        }
    }

    private static void setNewModeValue(State state, int mode) {
        int currentMode = state == State.NORMAL ? mNormalMode : mPowerSavingMode;
        if (mode != currentMode) {
            if (state == State.NORMAL) {
                mNormalMode = mode; 
            } else {
                mPowerSavingMode = mode;
            }
            if (mCurrentState == state) {
                switchToState(state, true);
            }
        }
    }

    private static void toggleSmartRadio() {
        try {
            mSmartRadioEnabled = !mSmartRadioEnabled;
            Settings.System.putInt(mContext.getContentResolver(),
                    SETTING_SMART_RADIO_ENABLED, mSmartRadioEnabled ? 1 : 0);
            if (mSmartRadioEnabled) {
                if (shouldSwitchToNormalState()) {
                    switchToState(State.NORMAL);
                } else {
                    switchToState(State.POWER_SAVING);
                }
            }
            if (DEBUG) log("mSmartRadioEnabled=" + mSmartRadioEnabled);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static class NetworkModeChanger implements Runnable, BroadcastSubReceiver {
        public static final String ACTION_CHANGE_MODE_ALARM = "gravitybox.smartradio.intent.action.CHANGE_MODE_ALARM";

        private Context mContext;
        private Handler mHandler;
        private int mNextNetworkMode;
        private int mCurrentNetworkMode;
        private WakeLock mWakeLock;
        private AlarmManager mAlarmManager;
        private PendingIntent mPendingIntent;

        public NetworkModeChanger(Context context) {
            mContext = context;
            mHandler = new Handler();
            mNextNetworkMode = -1;
            mCurrentNetworkMode = -1;
            mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GB:SmartRadio");
        }

        @Override
        public void run() {
            if (mContext == null || mNextNetworkMode == -1) return;
            if (DEBUG) log("NetworkModeChanger: sending intent");
            Intent intent = new Intent(PhoneWrapper.ACTION_CHANGE_NETWORK_TYPE);
            intent.putExtra(PhoneWrapper.EXTRA_NETWORK_TYPE, mNextNetworkMode);
            mContext.sendBroadcast(intent);
            mCurrentNetworkMode = mNextNetworkMode;
            releaseWakeLockIfHeld();
        }

        public void changeNetworkMode(int networkMode) {
            mHandler.removeCallbacks(this);
            releaseWakeLockIfHeld();
            cancelPendingAlarm();
            if (networkMode == -1 || networkMode == mCurrentNetworkMode) return;
            mNextNetworkMode = networkMode;
            if (mIsScreenOff && mNextNetworkMode == mPowerSavingMode && mScreenOffDelay != 0) {
                if (DEBUG) log("NetworkModeChanger: scheduling alarm for switching to power saving mode");
                Intent intent = new Intent(ACTION_CHANGE_MODE_ALARM);
                mPendingIntent = PendingIntent.getBroadcast(mContext, 1, intent, PendingIntent.FLAG_ONE_SHOT);
                long triggerAtMillis = System.currentTimeMillis() + mScreenOffDelay*60*1000;
                mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, mPendingIntent);
            } else {
                if (mModeChangeDelay == 0) {
                    run();
                } else {
                    if (DEBUG) log("NetworkModeChanger: scheduling network mode change");
                    if (mIsScreenOff) {
                        mWakeLock.acquire(mModeChangeDelay*1000+1000);
                        if (DEBUG) log("NetworkModeChanger: Wake Lock acquired");
                    }
                    mHandler.postDelayed(this, mModeChangeDelay*1000);
                }
            }
        }

        private void releaseWakeLockIfHeld() {
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
                if (DEBUG) log("NetworkModeChanger: Wake Lock released");
            }
        }

        private void cancelPendingAlarm() {
            if (mAlarmManager != null && mPendingIntent != null) {
                mAlarmManager.cancel(mPendingIntent);
            }
        }

        @Override
        public void onBroadcastReceived(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_CHANGE_MODE_ALARM)) {
                if (DEBUG) log("ACTION_CHANGE_MODE_ALARM received");
                run();
                mPendingIntent = null;
            }
        }
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> classSystemUIService = XposedHelpers.findClass(
                    "com.android.systemui.SystemUIService", classLoader);

            mNormalMode = prefs.getInt(GravityBoxSettings.PREF_KEY_SMART_RADIO_NORMAL_MODE, -1);
            mPowerSavingMode = prefs.getInt(GravityBoxSettings.PREF_KEY_SMART_RADIO_POWER_SAVING_MODE, -1);
            mPowerSaveWhenScreenOff = prefs.getBoolean(GravityBoxSettings.PREF_KEY_SMART_RADIO_SCREEN_OFF, false);
            mIgnoreWhileLocked = prefs.getBoolean(GravityBoxSettings.PREF_KEY_SMART_RADIO_IGNORE_LOCKED, true);
            mModeChangeDelay = prefs.getInt(GravityBoxSettings.PREF_KEY_SMART_RADIO_MODE_CHANGE_DELAY, 5);
            mScreenOffDelay = prefs.getInt(GravityBoxSettings.PREF_KEY_SMART_RADIO_SCREEN_OFF_DELAY, 0);
            mIgnoreMobileDataAvailability = prefs.getBoolean(GravityBoxSettings.PREF_KEY_SMART_RADIO_MDA_IGNORE, false);

            XposedHelpers.findAndHookMethod(classSystemUIService, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mContext = (Context) param.thisObject;
                    if (mContext != null) {
                        if (DEBUG) log("Initializing SmartRadio");

                        mSmartRadioEnabled = Settings.System.getInt(mContext.getContentResolver(),
                                SETTING_SMART_RADIO_ENABLED, 1) == 1;
                        mConnManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                        mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
                        mNetworkModeChanger = new NetworkModeChanger(mContext);
                        Settings.System.putString(mContext.getContentResolver(), 
                                SETTING_SMART_RADIO_STATE, mCurrentState.toString());

                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(GravityBoxSettings.ACTION_PREF_SMART_RADIO_CHANGED);
                        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
                        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
                        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
                        intentFilter.addAction(NetworkModeChanger.ACTION_CHANGE_MODE_ALARM);
                        intentFilter.addAction(ACTION_TOGGLE_SMART_RADIO);
                        mContext.registerReceiver(mBroadcastReceiver, intentFilter);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    } 
}
