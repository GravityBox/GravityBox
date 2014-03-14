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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.widget.Toast;

import com.ceco.kitkat.gravitybox.R;
import com.ceco.kitkat.gravitybox.shortcuts.ShortcutActivity;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XCallback;

public class ModHwKeys {
    private static final String TAG = "GB:ModHwKeys";
    private static final String CLASS_PHONE_WINDOW_MANAGER = "com.android.internal.policy.impl.PhoneWindowManager";
    private static final String CLASS_ACTIVITY_MANAGER_NATIVE = "android.app.ActivityManagerNative";
    private static final String CLASS_WINDOW_STATE = "android.view.WindowManagerPolicy$WindowState";
    private static final String CLASS_WINDOW_MANAGER_FUNCS = "android.view.WindowManagerPolicy.WindowManagerFuncs";
    private static final String CLASS_IWINDOW_MANAGER = "android.view.IWindowManager";
    private static final boolean DEBUG = false;

    private static final int FLAG_WAKE = 0x00000001;
    private static final int FLAG_WAKE_DROPPED = 0x00000002;
    public static final String ACTION_SCREENSHOT = "gravitybox.intent.action.SCREENSHOT";
    public static final String ACTION_SHOW_POWER_MENU = "gravitybox.intent.action.SHOW_POWER_MENU";
    public static final String ACTION_TOGGLE_EXPANDED_DESKTOP = 
            "gravitybox.intent.action.TOGGLE_EXPANDED_DESKTOP";
    public static final String ACTION_EXPAND_NOTIFICATIONS = "gravitybox.intent.action.EXPAND_NOTIFICATIONS";
    public static final String ACTION_EXPAND_QUICKSETTINGS = "gravitybox.intent.action.EXPAND_QUICKSETTINGS";
    public static final String ACTION_TOGGLE_TORCH = "gravitybox.intent.action.TOGGLE_TORCH";
    public static final String ACTION_SHOW_RECENT_APPS = "gravitybox.intent.action.SHOW_RECENT_APPS";
    public static final String ACTION_SHOW_APP_LAUCNHER = "gravitybox.intent.action.SHOW_APP_LAUNCHER";
    public static final String ACTION_TOGGLE_ROTATION_LOCK = "gravitybox.intent.action.TOGGLE_ROTATION_LOCK";
    public static final String ACTION_SLEEP = "gravitybox.intent.action.SLEEP";
    public static final String ACTION_MEDIA_CONTROL = "gravitybox.intent.action.MEDIA_CONTROL";
    public static final String EXTRA_MEDIA_CONTROL = "mediaControl";
    public static final String ACTION_KILL_FOREGROUND_APP = "gravitybox.intent.action.KILL_FOREGROUND_APP";
    public static final String ACTION_SWITCH_PREVIOUS_APP = "gravitybox.intent.action.SWICTH_PREVIOUS_APP";
    public static final String ACTION_SEARCH = "gravitybox.intent.action.SEARCH";
    public static final String ACTION_VOICE_SEARCH = "gravitybox.intent.action.VOICE_SEARCH";
    public static final String ACTION_LAUNCH_APP = "gravitybox.intent.action.LAUNCH_APP";
    public static final String EXTRA_APP_ID = "launchAppId";
    public static final String ACTION_SHOW_VOLUME_PANEL = "gravitybox.intent.action.SHOW_VOLUME_PANEL";
    public static final String ACTION_SHOW_BRIGHTNESS_DIALOG = "gravitybox.intent.action.SHOW_BRIGHTNESS_DIALOG";

    public static final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";
    public static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";

    private static Class<?> classActivityManagerNative;
    private static Object mPhoneWindowManager;
    private static Context mContext;
    private static Context mGbContext;
    private static String mStrAppKilled;
    private static String mStrNothingToKill;
    private static String mStrNoPrevApp;
    private static String mStrCustomAppNone;
    private static String mStrCustomAppMissing;
    private static String mStrExpandedDesktopDisabled;
    private static String mStrAutoRotationEnabled;
    private static String mStrAutoRotationDisabled;
    private static boolean mIsMenuLongPressed = false;
    private static boolean mIsMenuDoubleTap = false;
    private static boolean mIsBackLongPressed = false;
    private static boolean mIsBackDoubleTap = false;
    private static boolean mWasBackDoubleTap = false;
    private static boolean mIsRecentsLongPressed = false;
    private static boolean mIsHomeLongPressed = false;
    private static int mMenuLongpressAction = 0;
    private static int mMenuDoubletapAction = 0;
    private static int mHomeLongpressAction = 0;
    private static int mLockscreenTorch = 0;
    private static boolean mHomeDoubletapDisabled;
    private static int mHomeDoubletapDefaultAction;
    private static int mHomeDoubletapAction = 0;
    private static int mBackLongpressAction = 0;
    private static int mBackDoubletapAction = 0;
    private static int mRecentsSingletapAction = 0;
    private static int mRecentsLongpressAction = 0;
    private static int mDoubletapSpeed = GravityBoxSettings.HWKEY_DOUBLETAP_SPEED_DEFAULT;
    private static int mKillDelay = GravityBoxSettings.HWKEY_KILL_DELAY_DEFAULT;
    private static String mVolumeRockerWake = "default";
    private static boolean mHwKeysEnabled = true;
    private static XSharedPreferences mPrefs;
    private static AppLauncher mAppLauncher;
    private static int mPieMode;
    private static int mExpandedDesktopMode;
    private static boolean mMenuKeyPressed;
    private static boolean mBackKeyPressed;
    private static int mCustomKeySingletapAction = GravityBoxSettings.HWKEY_ACTION_APP_LAUNCHER;
    private static int mCustomKeyLongpressAction = GravityBoxSettings.HWKEY_ACTION_DEFAULT;
    private static int mCustomKeyDoubletapAction = GravityBoxSettings.HWKEY_ACTION_DEFAULT;
    private static boolean mIsCustomKeyLongPressed = false;
    private static boolean mCustomKeyDoubletapPending = false;
    private static boolean mWasCustomKeyDoubletap = false;
    private static boolean mCustomKeyPressed = false;

    private static List<String> mKillIgnoreList = new ArrayList<String>(Arrays.asList(
            "com.android.systemui",
            "com.mediatek.bluetooth",
            "android.process.acore",
            "com.google.process.gapps",
            "com.android.smspush"
    ));

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static enum HwKey {
        MENU,
        HOME,
        BACK,
        RECENTS,
        CUSTOM
    }

    private static enum HwKeyTrigger {
        MENU_LONGPRESS,
        MENU_DOUBLETAP,
        HOME_LONGPRESS,
        HOME_DOUBLETAP,
        BACK_LONGPRESS,
        BACK_DOUBLETAP,
        RECENTS_SINGLETAP,
        RECENTS_LONGPRESS,
        CUSTOM_SINGLETAP,
        CUSTOM_LONGPRESS,
        CUSTOM_DOUBLETAP
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("Broadcast received: " + intent.toString());

            String action = intent.getAction();
            int value = GravityBoxSettings.HWKEY_ACTION_DEFAULT;
            if (intent.hasExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE)) {
                value = intent.getIntExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE, value);
            }

            if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_MENU_LONGPRESS_CHANGED)) {
                mMenuLongpressAction = value;
                if (DEBUG) log("Menu long-press action set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_MENU_DOUBLETAP_CHANGED)) {
                mMenuDoubletapAction = value;
                if (DEBUG) log("Menu double-tap action set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE)) {
                    mHomeLongpressAction = value;
                    if (DEBUG) log("Home long-press action set to: " + value);
                }
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_HOME_DOUBLETAP_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_HWKEY_HOME_DOUBLETAP_DISABLE)) {
                    mHomeDoubletapDisabled = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_HWKEY_HOME_DOUBLETAP_DISABLE, false);
                    if (mPhoneWindowManager != null) {
                        try {
                            XposedHelpers.setIntField(mPhoneWindowManager, "mDoubleTapOnHomeBehavior",
                                    mHomeDoubletapDisabled ? 0 : 
                                        mHomeDoubletapAction == 0 ? mHomeDoubletapDefaultAction : 1);
                        } catch (Throwable t) {
                            log("PhoneWindowManager: Error settings mDoubleTapOnHomeBehavior: " +
                                    t.getMessage());
                        }
                    }
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_HWKEY_HOME_DOUBLETAP)) {
                    mHomeDoubletapAction = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_HWKEY_HOME_DOUBLETAP, 0);
                    if (mPhoneWindowManager != null) {
                        try {
                            XposedHelpers.setIntField(mPhoneWindowManager, "mDoubleTapOnHomeBehavior",
                                    mHomeDoubletapAction == 0 ? mHomeDoubletapDefaultAction : 1);
                        } catch (Throwable t) {
                            log("PhoneWindowManager: Error settings mDoubleTapOnHomeBehavior: " +
                                    t.getMessage());
                        }
                    }
                    if (DEBUG) log("Home double-tap action set to: " + mHomeDoubletapAction);
                }
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_BACK_LONGPRESS_CHANGED)) {
                mBackLongpressAction = value;
                if (DEBUG) log("Back long-press action set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_BACK_DOUBLETAP_CHANGED)) {
                mBackDoubletapAction = value;
                if (DEBUG) log("Back double-tap action set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_SINGLETAP_CHANGED)) {
                mRecentsSingletapAction = value;
                if (DEBUG) log("Recents single-tap action set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_LONGPRESS_CHANGED)) {
                mRecentsLongpressAction = value;
                if (DEBUG) log("Recents long-press action set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_DOUBLETAP_SPEED_CHANGED)) {
                mDoubletapSpeed = value;
                if (DEBUG) log("Doubletap speed set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_KILL_DELAY_CHANGED)) {
                mKillDelay = value;
                if (DEBUG) log("Kill delay set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_VOLUME_ROCKER_WAKE_CHANGED)) {
                mVolumeRockerWake = intent.getStringExtra(GravityBoxSettings.EXTRA_VOLUME_ROCKER_WAKE);
                if (DEBUG) log("mVolumeRockerWake set to: " + mVolumeRockerWake);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_LOCKSCREEN_TORCH_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_HWKEY_TORCH)) {
                    mLockscreenTorch = intent.getIntExtra(GravityBoxSettings.EXTRA_HWKEY_TORCH,
                            GravityBoxSettings.HWKEY_TORCH_DISABLED);
                    if (DEBUG) log("Lockscreen torch set to: " + mLockscreenTorch);
                }
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_PIE_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_HWKEYS_DISABLE)) {
                    mHwKeysEnabled = !intent.getBooleanExtra(GravityBoxSettings.EXTRA_PIE_HWKEYS_DISABLE, false);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_ENABLE)) {
                    mPieMode = intent.getIntExtra(GravityBoxSettings.EXTRA_PIE_ENABLE, 0);
                }
            } else if (action.equals(ACTION_SCREENSHOT) && mPhoneWindowManager != null) {
                takeScreenshot();
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_DISPLAY_ALLOW_ALL_ROTATIONS_CHANGED)) {
                final boolean allowAllRotations = intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_ALLOW_ALL_ROTATIONS, false);
                try {
                    XposedHelpers.setIntField(mPhoneWindowManager, "mAllowAllRotations",
                            allowAllRotations ? 1 : 0);
                } catch (Throwable t) {
                    log("Error settings PhoneWindowManager.mAllowAllRotations: " + t.getMessage());
                }
            } else if (action.equals(ACTION_SHOW_POWER_MENU) && mPhoneWindowManager != null) {
                showGlobalActionsDialog();
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_EXPANDED_DESKTOP_MODE_CHANGED)) {
                mExpandedDesktopMode = intent.getIntExtra(
                        GravityBoxSettings.EXTRA_ED_MODE, GravityBoxSettings.ED_DISABLED);
            } else if (action.equals(ACTION_TOGGLE_EXPANDED_DESKTOP) && mPhoneWindowManager != null) {
                toggleExpandedDesktop(value);
            } else if (action.equals(ScreenRecordingService.ACTION_TOGGLE_SCREEN_RECORDING)) {
                toggleScreenRecording();
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_NAVBAR_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_CUSTOM_KEY_SINGLETAP)) {
                    mCustomKeySingletapAction = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_CUSTOM_KEY_SINGLETAP,
                                GravityBoxSettings.HWKEY_ACTION_APP_LAUNCHER);
                    if (DEBUG) log("mCustomKeySingletapAction set to: " + mCustomKeySingletapAction);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_CUSTOM_KEY_LONGPRESS)) {
                    mCustomKeyLongpressAction = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_CUSTOM_KEY_LONGPRESS,
                                GravityBoxSettings.HWKEY_ACTION_DEFAULT);
                    if (DEBUG) log("mCustomKeyLongpressAction set to: " + mCustomKeyLongpressAction);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_CUSTOM_KEY_DOUBLETAP)) {
                    mCustomKeyDoubletapAction = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_CUSTOM_KEY_DOUBLETAP,
                                GravityBoxSettings.HWKEY_ACTION_DEFAULT);
                    if (DEBUG) log("mCustomKeyDoubletapAction set to: " + mCustomKeyDoubletapAction);
                }
            } else if (action.equals(ACTION_EXPAND_NOTIFICATIONS) && mPhoneWindowManager != null) {
                expandNotificationsPanel();
            } else if (action.equals(ACTION_EXPAND_QUICKSETTINGS) && mPhoneWindowManager != null) {
                expandSettingsPanel();
            } else if (action.equals(ACTION_TOGGLE_TORCH)) {
                toggleTorch();
            } else if (action.equals(ACTION_SHOW_RECENT_APPS)) {
                toggleRecentApps();
            } else if (action.equals(ACTION_SHOW_APP_LAUCNHER)) {
                showAppLauncher();
            } else if (action.equals(ACTION_TOGGLE_ROTATION_LOCK)) {
                toggleAutoRotation();
            } else if (action.equals(ACTION_SLEEP)) {
                goToSleep();
            } else if (action.equals(ACTION_MEDIA_CONTROL) && intent.hasExtra(EXTRA_MEDIA_CONTROL)) {
                final int keyCode = intent.getIntExtra(EXTRA_MEDIA_CONTROL, 0);
                if (DEBUG) log("MEDIA CONTROL: keycode=" + keyCode);
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                        keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS ||
                        keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                    injectKey(keyCode);
                }
            } else if (action.equals(ACTION_KILL_FOREGROUND_APP) && mPhoneWindowManager != null) {
                killForegroundApp();
            } else if (action.equals(ACTION_SWITCH_PREVIOUS_APP) && mPhoneWindowManager != null) {
                switchToLastApp();
            } else if (action.equals(ACTION_SEARCH)) {
                launchSearchActivity();
            } else if (action.equals(ACTION_VOICE_SEARCH)) {
                launchVoiceSearchActivity();
            } else if (action.equals(ACTION_LAUNCH_APP) && intent.hasExtra(EXTRA_APP_ID)) {
                launchCustomApp(intent.getIntExtra(EXTRA_APP_ID, GravityBoxSettings.HWKEY_ACTION_CUSTOM_APP));
            } else if (action.equals(ACTION_SHOW_VOLUME_PANEL)) {
                showVolumePanel();
            } else if (action.equals(ACTION_SHOW_BRIGHTNESS_DIALOG)) {
                showBrightnessDialog();
            }
        }
    };

    public static void initZygote(final XSharedPreferences prefs) {
        try {
            mPrefs = prefs;
            try {
                mMenuLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_MENU_LONGPRESS, "0"));
                mMenuDoubletapAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_MENU_DOUBLETAP, "0"));
                mHomeLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_HOME_LONGPRESS, "0"));
                mHomeDoubletapAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_HOME_DOUBLETAP, "0"));
                mBackLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_BACK_LONGPRESS, "0"));
                mBackDoubletapAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_BACK_DOUBLETAP, "0"));
                mRecentsSingletapAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_RECENTS_SINGLETAP, "0"));
                mRecentsLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_RECENTS_LONGPRESS, "0"));
                mDoubletapSpeed = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_DOUBLETAP_SPEED, "400"));
                mKillDelay = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_KILL_DELAY, "1000"));
                mLockscreenTorch = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_LOCKSCREEN_TORCH, "0"));
                mCustomKeySingletapAction = Integer.valueOf(prefs.getString(
                        GravityBoxSettings.PREF_KEY_NAVBAR_CUSTOM_KEY_SINGLETAP, "12"));
                mCustomKeyLongpressAction = Integer.valueOf(prefs.getString(
                        GravityBoxSettings.PREF_KEY_NAVBAR_CUSTOM_KEY_LONGPRESS, "0"));
                mCustomKeyDoubletapAction = Integer.valueOf(prefs.getString(
                        GravityBoxSettings.PREF_KEY_NAVBAR_CUSTOM_KEY_DOUBLETAP, "0"));
            } catch (NumberFormatException e) {
                XposedBridge.log(e);
            }

            mHomeDoubletapDisabled = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_HWKEY_HOME_DOUBLETAP_DISABLE, false);
            mVolumeRockerWake = prefs.getString(GravityBoxSettings.PREF_KEY_VOLUME_ROCKER_WAKE, "default");
            mHwKeysEnabled = !prefs.getBoolean(GravityBoxSettings.PREF_KEY_HWKEYS_DISABLE, false);

            mPieMode = ModPieControls.PIE_DISABLED;
            try {
                mPieMode = Integer.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_PIE_CONTROL_ENABLE, "0"));
            } catch (NumberFormatException nfe) {
                log("Invalid preference value for Pie Mode");
            }

            mExpandedDesktopMode = GravityBoxSettings.ED_DISABLED;
            try {
                mExpandedDesktopMode = Integer.valueOf(prefs.getString(
                        GravityBoxSettings.PREF_KEY_EXPANDED_DESKTOP, "0"));
            } catch (NumberFormatException nfe) {
                log("Invalid value for PREF_KEY_EXPANDED_DESKTOP preference");
            }

            final Class<?> classPhoneWindowManager = XposedHelpers.findClass(CLASS_PHONE_WINDOW_MANAGER, null);
            classActivityManagerNative = XposedHelpers.findClass(CLASS_ACTIVITY_MANAGER_NATIVE, null);

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, "init",
                Context.class, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS, phoneWindowManagerInitHook);

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, "interceptKeyBeforeQueueing", 
                    KeyEvent.class, int.class, boolean.class, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    KeyEvent event = (KeyEvent) param.args[0];
                    int keyCode = event.getKeyCode();
                    boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
                    boolean keyguardOn = (Boolean) XposedHelpers.callMethod(mPhoneWindowManager, "keyguardOn");
                    boolean isFromSystem = (event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) != 0;
                    Handler handler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                    if (DEBUG) log("interceptKeyBeforeQueueing: keyCode=" + keyCode +
                            "; action=" + event.getAction() + "; repeatCount=" + event.getRepeatCount());

                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        if (!down) {
                            handler.removeCallbacks(mResetBrightnessRunnable);
                        } else {
                            if (event.getRepeatCount() == 0) {
                                handler.postDelayed(mResetBrightnessRunnable, 7000);
                            }
                        }
                    }

                    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && keyguardOn &&
                            mLockscreenTorch == GravityBoxSettings.HWKEY_TORCH_VOLDOWN_LONGPRESS &&
                            !(Boolean) XposedHelpers.callMethod(param.thisObject, "isMusicActive")) {
                        if (!down) {
                            handler.removeCallbacks(mLockscreenTorchRunnable);
                        } else {
                            if (event.getRepeatCount() == 0) {
                                handler.postDelayed(mLockscreenTorchRunnable, 
                                        ViewConfiguration.getLongPressTimeout());
                            }
                        }
                    }

                    if (!mVolumeRockerWake.equals("default") && 
                            (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                                    keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                        int policyFlags = (Integer) param.args[1];
                        if (mVolumeRockerWake.equals("enabled")) {
                            policyFlags |= FLAG_WAKE;
                            policyFlags |= FLAG_WAKE_DROPPED;
                        } else if (mVolumeRockerWake.equals("disabled")) {
                            policyFlags &= ~FLAG_WAKE;
                            policyFlags &= ~FLAG_WAKE_DROPPED;
                        }
                        param.args[1] = policyFlags;
                        return;
                    }

                    if (keyCode == KeyEvent.KEYCODE_HOME) {
                        if (!down) {
                            handler.removeCallbacks(mLockscreenTorchRunnable);
                            if (mIsHomeLongPressed) {
                                mIsHomeLongPressed = false;
                                param.setResult(0);
                                return;
                            }
                            if (!areHwKeysEnabled() && 
                                    event.getRepeatCount() == 0 &&
                                    (event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) != 0) {
                               if (DEBUG) log("HOME KeyEvent coming from HW key and keys disabled. Ignoring.");
                               param.setResult(0);
                               return;
                           }
                        } else if (keyguardOn) {
                            if (event.getRepeatCount() == 0) {
                                mIsHomeLongPressed = false;
                                if (mLockscreenTorch == GravityBoxSettings.HWKEY_TORCH_HOME_LONGPRESS) {
                                    handler.postDelayed(mLockscreenTorchRunnable, 
                                            getLongpressTimeoutForAction(GravityBoxSettings.HWKEY_ACTION_TORCH));
                                }
                            } else {
                                if (mLockscreenTorch == GravityBoxSettings.HWKEY_TORCH_HOME_LONGPRESS) {
                                    param.setResult(0);
                                }
                                return;
                            }
                        }
                    }

                    if (keyCode == KeyEvent.KEYCODE_SOFT_LEFT) {
                        if (!down) {
                            mCustomKeyPressed = false;
                            if (!mIsCustomKeyLongPressed && 
                                    !mCustomKeyDoubletapPending && !mWasCustomKeyDoubletap) {
                                if (DEBUG) log("Custom key singletap action");
                                performAction(HwKeyTrigger.CUSTOM_SINGLETAP);
                            }
                            mIsCustomKeyLongPressed = false;
                        } else {
                            mCustomKeyPressed = true;
                            if (event.getRepeatCount() == 0) {
                                if (mCustomKeyDoubletapPending) {
                                    handler.removeCallbacks(mCustomKeyDoubletapReset);
                                    mWasCustomKeyDoubletap = true;
                                    mCustomKeyDoubletapPending = false;
                                    if (DEBUG) log("Custom key double-tap action");
                                    performAction(HwKeyTrigger.CUSTOM_DOUBLETAP);
                                } else if (mCustomKeyDoubletapAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT
                                            && isFromSystem) {
                                    mCustomKeyDoubletapPending = true;
                                    mWasCustomKeyDoubletap = false;
                                    handler.postDelayed(mCustomKeyDoubletapReset, mDoubletapSpeed);
                                }
                                if (isFromSystem) {
                                    XposedHelpers.callMethod(param.thisObject, "performHapticFeedbackLw",
                                        new Class<?> [] { Class.forName(CLASS_WINDOW_STATE), int.class, boolean.class },
                                        null, HapticFeedbackConstants.VIRTUAL_KEY, false);
                                }
                            } else {
                                handler.removeCallbacks(mCustomKeyDoubletapReset);
                                mCustomKeyDoubletapPending = false;
                                mIsCustomKeyLongPressed = true;
                                if (DEBUG) log("Custom key long-press action");
                                performAction(HwKeyTrigger.CUSTOM_LONGPRESS);
                                XposedHelpers.callMethod(param.thisObject, "performHapticFeedbackLw",
                                        new Class<?> [] { Class.forName(CLASS_WINDOW_STATE), int.class, boolean.class },
                                        null, HapticFeedbackConstants.LONG_PRESS, false);
                            }
                        }
                        param.setResult(0);
                        return;
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, "interceptKeyBeforeDispatching", 
                    CLASS_WINDOW_STATE, KeyEvent.class, int.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if ((Boolean) XposedHelpers.callMethod(mPhoneWindowManager, "keyguardOn")) return;

                    KeyEvent event = (KeyEvent) param.args[1];
                    int keyCode = event.getKeyCode();
                    boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
                    boolean isFromSystem = (event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) != 0;
                    Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                    if (DEBUG) log("interceptKeyBeforeDispatching: keyCode=" + keyCode +
                            "; isInjected=" + (((Integer)param.args[2] & 0x01000000) != 0) +
                            "; fromSystem=" + isFromSystem);

                    if (keyCode == KeyEvent.KEYCODE_MENU && isFromSystem &&
                        (hasAction(HwKey.MENU) || !areHwKeysEnabled())) {

                        if (!down) {
                            mMenuKeyPressed = false;
                            mHandler.removeCallbacks(mMenuLongPress);
                            if (mIsMenuLongPressed) {
                                mIsMenuLongPressed = false;
                                param.setResult(-1);
                                return;
                            }
                            if (event.getRepeatCount() == 0) {
                                if (!areHwKeysEnabled()) {
                                    if (DEBUG) log("MENU KeyEvent coming from HW key and keys disabled. Ignoring.");
                                    param.setResult(-1);
                                    return;
                                } else if (mIsMenuDoubleTap) {
                                    // we are still waiting for double-tap
                                    if (DEBUG) log("MENU doubletap pending. Ignoring.");
                                    param.setResult(-1);
                                    return;
                                }
                            }
                        } else {
                            if (event.getRepeatCount() == 0) {
                                mMenuKeyPressed = true;
                                if (mIsMenuDoubleTap) {
                                    performAction(HwKeyTrigger.MENU_DOUBLETAP);
                                    mHandler.removeCallbacks(mMenuDoubleTapReset);
                                    mIsMenuDoubleTap = false;
                                    param.setResult(-1);
                                    return;
                                } else {
                                    mIsMenuLongPressed = false;
                                    mIsMenuDoubleTap = false;
                                    if (mMenuDoubletapAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                                        mIsMenuDoubleTap = true;
                                        mHandler.postDelayed(mMenuDoubleTapReset, mDoubletapSpeed);
                                        param.setResult(-1);
                                    }
                                    if (mMenuLongpressAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                                        mHandler.postDelayed(mMenuLongPress, 
                                                getLongpressTimeoutForAction(mMenuLongpressAction));
                                    }
                                }
                            } else {
                                if (mMenuLongpressAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                                    param.setResult(-1);
                                }
                                return;
                            }
                        }
                    }

                    if (keyCode == KeyEvent.KEYCODE_BACK && isFromSystem &&
                            (hasAction(HwKey.BACK) || !areHwKeysEnabled())) {

                        if (!down) {
                            mBackKeyPressed = false;
                            mHandler.removeCallbacks(mBackLongPress);
                            if (mIsBackLongPressed) {
                                mIsBackLongPressed = false;
                            } else if (areHwKeysEnabled() && !mIsBackDoubleTap && !mWasBackDoubleTap) {
                                // inject BACK key event as it was previously eaten by us
                                if (DEBUG) log("Triggering original DOWN/UP events for BACK key");
                                if (!event.isCanceled()) {
                                    injectKey(KeyEvent.KEYCODE_BACK);
                                }
                            } else {
                                if (DEBUG) log("BACK KeyEvent coming from HW key and keys disabled; " +
                                		"or pending double-tap. Ignoring.");
                            }
                        } else {
                            if (event.getRepeatCount() == 0) {
                                mBackKeyPressed = true;
                                mWasBackDoubleTap = mIsBackDoubleTap;
                                if (mIsBackDoubleTap) {
                                    performAction(HwKeyTrigger.BACK_DOUBLETAP);
                                    mHandler.removeCallbacks(mBackDoubleTapReset);
                                    mIsBackDoubleTap = false;
                                    param.setResult(-1);
                                    return;
                                } else {
                                    mIsBackLongPressed = false;
                                    mIsBackDoubleTap = false;
                                    if (mBackDoubletapAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                                        mIsBackDoubleTap = true;
                                        mHandler.postDelayed(mBackDoubleTapReset, mDoubletapSpeed);
                                    }
                                    if (mBackLongpressAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                                        mHandler.postDelayed(mBackLongPress, 
                                                getLongpressTimeoutForAction(mBackLongpressAction));
                                    }
                                }
                            }
                        }
                        param.setResult(-1);
                        return;
                    }

                    if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
                        if (!hasAction(HwKey.RECENTS) && areHwKeysEnabled()) return;

                        if (!down) {
                            cancelPreloadRecentApps();
                            mHandler.removeCallbacks(mRecentsLongPress);
                            if (!mIsRecentsLongPressed) {
                                if (!areHwKeysEnabled() &&
                                        event.getRepeatCount() == 0 && 
                                        ((event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) != 0)) {
                                    if (DEBUG) log("APP_SWITCH KeyEvent coming from HW key and keys disabled. Ignoring.");
                                    param.setResult(-1);
                                    return;
                                } else if (!event.isCanceled()) {
                                    if (mRecentsSingletapAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                                        performAction(HwKeyTrigger.RECENTS_SINGLETAP);
                                    } else {
                                        toggleRecentApps();
                                    }
                                }
                            }
                            mIsRecentsLongPressed = false;
                        } else {
                            if (event.getRepeatCount() == 0) {
                                mIsRecentsLongPressed = false;
                                if (mRecentsSingletapAction == GravityBoxSettings.HWKEY_ACTION_DEFAULT ||
                                        mRecentsSingletapAction == GravityBoxSettings.HWKEY_ACTION_RECENT_APPS ||
                                        mRecentsLongpressAction == GravityBoxSettings.HWKEY_ACTION_RECENT_APPS) {
                                    preloadRecentApps();
                                }
                                if (mRecentsLongpressAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                                    mHandler.postDelayed(mRecentsLongPress, 
                                            getLongpressTimeoutForAction(mRecentsLongpressAction));
                                }
                            }
                        }
                        param.setResult(-1);
                        return;
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, "handleLongPressOnHome", new XC_MethodReplacement() {

                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    if (mHomeLongpressAction == GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                        XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                        return null;
                    }

                    XposedHelpers.setBooleanField(param.thisObject, "mHomeConsumed", true);
                    performAction(HwKeyTrigger.HOME_LONGPRESS);

                    return null;
                }
            });

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, 
                    "isWakeKeyWhenScreenOff", int.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int keyCode = (Integer) param.args[0];
                    if (!mVolumeRockerWake.equals("default") && 
                            (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                             keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
                        param.setResult(mVolumeRockerWake.equals("enabled") ? true : false);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, 
                    "readConfigurationDependentBehaviors", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mHomeDoubletapDefaultAction = XposedHelpers.getIntField(
                            param.thisObject, "mDoubleTapOnHomeBehavior");
                    if (mHomeDoubletapDisabled) {
                        XposedHelpers.setIntField(param.thisObject, "mDoubleTapOnHomeBehavior", 0);
                    } else if (mHomeDoubletapAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                        XposedHelpers.setIntField(param.thisObject, "mDoubleTapOnHomeBehavior", 1);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classPhoneWindowManager,
                    "handleDoubleTapOnHome", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (mHomeDoubletapAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                        XposedHelpers.setBooleanField(param.thisObject, "mHomeConsumed", true);
                        performAction(HwKeyTrigger.HOME_DOUBLETAP);
                        param.setResult(null);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static XC_MethodHook phoneWindowManagerInitHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mPhoneWindowManager = param.thisObject;
            mContext = (Context) XposedHelpers.getObjectField(mPhoneWindowManager, "mContext");
            mGbContext = mContext.createPackageContext(GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            XposedHelpers.setIntField(mPhoneWindowManager, "mAllowAllRotations", 
                    mPrefs.getBoolean(GravityBoxSettings.PREF_KEY_DISPLAY_ALLOW_ALL_ROTATIONS, false) ? 1 : 0);

            Resources res = mGbContext.getResources();
            mStrAppKilled = res.getString(R.string.app_killed);
            mStrNothingToKill = res.getString(R.string.nothing_to_kill);
            mStrNoPrevApp = res.getString(R.string.no_previous_app_found);
            mStrCustomAppNone = res.getString(R.string.hwkey_action_custom_app_none);
            mStrCustomAppMissing = res.getString(R.string.hwkey_action_custom_app_missing);
            mStrExpandedDesktopDisabled = res.getString(R.string.hwkey_action_expanded_desktop_disabled);
            mStrAutoRotationEnabled = res.getString(R.string.hwkey_action_auto_rotation_enabled);
            mStrAutoRotationDisabled =  res.getString(R.string.hwkey_action_auto_rotation_disabled);

            mAppLauncher = new AppLauncher(mContext, mPrefs);

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_MENU_LONGPRESS_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_MENU_DOUBLETAP_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_HOME_DOUBLETAP_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_BACK_LONGPRESS_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_BACK_DOUBLETAP_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_SINGLETAP_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_LONGPRESS_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_DOUBLETAP_SPEED_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_KILL_DELAY_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_VOLUME_ROCKER_WAKE_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_PIE_CHANGED);
            intentFilter.addAction(ACTION_SCREENSHOT);
            intentFilter.addAction(ACTION_SHOW_POWER_MENU);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_DISPLAY_ALLOW_ALL_ROTATIONS_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_EXPANDED_DESKTOP_MODE_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_LOCKSCREEN_TORCH_CHANGED);
            intentFilter.addAction(ACTION_TOGGLE_EXPANDED_DESKTOP);
            intentFilter.addAction(ScreenRecordingService.ACTION_TOGGLE_SCREEN_RECORDING);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_NAVBAR_CHANGED);
            intentFilter.addAction(ACTION_EXPAND_NOTIFICATIONS);
            intentFilter.addAction(ACTION_EXPAND_QUICKSETTINGS);
            intentFilter.addAction(ACTION_TOGGLE_TORCH);
            intentFilter.addAction(ACTION_SHOW_RECENT_APPS);
            intentFilter.addAction(ACTION_SHOW_APP_LAUCNHER);
            intentFilter.addAction(ACTION_TOGGLE_ROTATION_LOCK);
            intentFilter.addAction(ACTION_SLEEP);
            intentFilter.addAction(ACTION_MEDIA_CONTROL);
            intentFilter.addAction(ACTION_KILL_FOREGROUND_APP);
            intentFilter.addAction(ACTION_SWITCH_PREVIOUS_APP);
            intentFilter.addAction(ACTION_SEARCH);
            intentFilter.addAction(ACTION_VOICE_SEARCH);
            intentFilter.addAction(ACTION_LAUNCH_APP);
            intentFilter.addAction(ACTION_SHOW_VOLUME_PANEL);
            intentFilter.addAction(ACTION_SHOW_BRIGHTNESS_DIALOG);
            mContext.registerReceiver(mBroadcastReceiver, intentFilter);

            if (DEBUG) log("Phone window manager initialized");
        }
    };

    private static boolean areHwKeysEnabled() {
        return (mHwKeysEnabled ||
                  !ModPieControls.isPieEnabled(mContext, mPieMode, mExpandedDesktopMode));
    }

    private static Runnable mMenuLongPress = new Runnable() {

        @Override
        public void run() {
            if (DEBUG) log("mMenuLongPress runnable launched");
            mIsMenuLongPressed = true;
            performAction(HwKeyTrigger.MENU_LONGPRESS);
        }
    };

    private static Runnable mMenuDoubleTapReset = new Runnable() {

        @Override
        public void run() {
            mIsMenuDoubleTap = false;
            // doubletap timed out and since we blocked default MENU key action while waiting for doubletap
            // let's inject it now additionally, but only in case it's not still pressed as we might still be waiting
            // for long-press action
            if (!mMenuKeyPressed && areHwKeysEnabled()) {
                if (DEBUG) log("MENU key double tap timed out and key not pressed; injecting MENU key");
                injectKey(KeyEvent.KEYCODE_MENU);
            }
        }
    };

    private static Runnable mBackLongPress = new Runnable() {

        @Override
        public void run() {
            if (DEBUG) log("mBackLongPress runnable launched");
            mIsBackLongPressed = true;
            performAction(HwKeyTrigger.BACK_LONGPRESS);
        }
    };

    private static Runnable mBackDoubleTapReset = new Runnable() {

        @Override
        public void run() {
            mIsBackDoubleTap = false;
            // doubletap timed out and since we blocked default BACK key action while waiting for doubletap
            // let's inject it now additionally, but only in case it's not still pressed as we might still be waiting
            // for long-press action
            if (!mBackKeyPressed && areHwKeysEnabled()) {
                if (DEBUG) log("BACK key double tap timed out and key not pressed; injecting BACK key");
                injectKey(KeyEvent.KEYCODE_BACK);
            }
        }
    };

    private static Runnable mRecentsLongPress = new Runnable() {

        @Override
        public void run() {
            if (DEBUG) log("mRecentsLongPress runnable launched");
            mIsRecentsLongPressed = true;
            performAction(HwKeyTrigger.RECENTS_LONGPRESS);
        }
    };

    private static Runnable mCustomKeyDoubletapReset = new Runnable() {
        @Override
        public void run() {
            mCustomKeyDoubletapPending = false;
            // doubletap timed out and since we blocked single-tap action while waiting for doubletap
            // let's inject it now additionally, but only in case it's not still pressed as we might still be waiting
            // for long-press action
            if (!mCustomKeyPressed) {
                if (DEBUG) log("Custom key double tap timed out and key not pressed; injecting key");
                injectKey(KeyEvent.KEYCODE_SOFT_LEFT);
            }
        }
    };

    private static Runnable mLockscreenTorchRunnable = new Runnable() {

        @Override
        public void run() {
            if (DEBUG) log("mLockscreenTorchRunnable runnable launched");
            if (mLockscreenTorch == GravityBoxSettings.HWKEY_TORCH_HOME_LONGPRESS) {
                mIsHomeLongPressed = true;
            }
            toggleTorch();
        }
    };

    private static Runnable mResetBrightnessRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                Class<?> classSm = XposedHelpers.findClass("android.os.ServiceManager", null);
                Class<?> classIpm = XposedHelpers.findClass("android.os.IPowerManager.Stub", null);
                IBinder b = (IBinder) XposedHelpers.callStaticMethod(
                        classSm, "getService", Context.POWER_SERVICE);
                Object power = XposedHelpers.callStaticMethod(classIpm, "asInterface", b);
                if (power != null) {
                    Settings.System.putInt(mContext.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
                    XposedHelpers.callMethod(power, "setTemporaryScreenBrightnessSettingOverride", 100);
                    Settings.System.putInt(mContext.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS, 100);
                    if (DEBUG) log("Screen brightness reset to manual with level set to 100");
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static int getActionForHwKeyTrigger(HwKeyTrigger keyTrigger) {
        int action = GravityBoxSettings.HWKEY_ACTION_DEFAULT;

        if (keyTrigger == HwKeyTrigger.MENU_LONGPRESS) {
            action = mMenuLongpressAction;
        } else if (keyTrigger == HwKeyTrigger.MENU_DOUBLETAP) {
            action = mMenuDoubletapAction;
        } else if (keyTrigger == HwKeyTrigger.HOME_LONGPRESS) {
            action = mHomeLongpressAction;
        } else if (keyTrigger == HwKeyTrigger.BACK_LONGPRESS) {
            action = mBackLongpressAction;
        } else if (keyTrigger == HwKeyTrigger.BACK_DOUBLETAP) {
            action = mBackDoubletapAction;
        } else if (keyTrigger == HwKeyTrigger.RECENTS_SINGLETAP) {
            action = mRecentsSingletapAction;
        } else if (keyTrigger == HwKeyTrigger.RECENTS_LONGPRESS) {
            action = mRecentsLongpressAction;
        } else if (keyTrigger == HwKeyTrigger.CUSTOM_SINGLETAP) {
            action = mCustomKeySingletapAction;
        } else if (keyTrigger == HwKeyTrigger.CUSTOM_LONGPRESS) {
            action = mCustomKeyLongpressAction;
        } else if (keyTrigger == HwKeyTrigger.CUSTOM_DOUBLETAP) {
            action = mCustomKeyDoubletapAction;
        } else if (keyTrigger == HwKeyTrigger.HOME_DOUBLETAP) {
            action = mHomeDoubletapAction;
        }

        if (DEBUG) log("Action for HWKEY trigger " + keyTrigger + " = " + action);
        return action;
    }

    private static boolean hasAction(HwKey key) {
        boolean retVal = false;
        if (key == HwKey.MENU) {
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.MENU_LONGPRESS) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.MENU_DOUBLETAP) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
        } else if (key == HwKey.HOME) {
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.HOME_LONGPRESS) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
        } else if (key == HwKey.BACK) {
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.BACK_LONGPRESS) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.BACK_DOUBLETAP) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
        } else if (key == HwKey.RECENTS) {
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.RECENTS_SINGLETAP) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.RECENTS_LONGPRESS) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
        } else if (key == HwKey.CUSTOM) {
            retVal = true;
        }

        if (DEBUG) log("HWKEY " + key + " has action = " + retVal);
        return retVal;
    }

    private static int getLongpressTimeoutForAction(int action) {
        return (action == GravityBoxSettings.HWKEY_ACTION_KILL) ?
                mKillDelay : ViewConfiguration.getLongPressTimeout();
    }

    private static void performAction(HwKeyTrigger keyTrigger) {
        int action = getActionForHwKeyTrigger(keyTrigger);
        if (DEBUG) log("Performing action " + action + " for HWKEY trigger " + keyTrigger.toString());

        if (action == GravityBoxSettings.HWKEY_ACTION_DEFAULT) return;

        if (action == GravityBoxSettings.HWKEY_ACTION_SEARCH) {
            launchSearchActivity();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_VOICE_SEARCH) {
            launchVoiceSearchActivity();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_PREV_APP) {
            switchToLastApp();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_KILL) {
            killForegroundApp();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_SLEEP) {
            goToSleep();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_RECENT_APPS) {
            toggleRecentApps();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_CUSTOM_APP
                    || action == GravityBoxSettings.HWKEY_ACTION_CUSTOM_APP2) {
            launchCustomApp(action);
        } else if (action == GravityBoxSettings.HWKEY_ACTION_MENU) {
            injectKey(KeyEvent.KEYCODE_MENU);
        } else if (action == GravityBoxSettings.HWKEY_ACTION_EXPANDED_DESKTOP) {
            toggleExpandedDesktop();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_TORCH) {
            toggleTorch();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_APP_LAUNCHER) {
            showAppLauncher();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_HOME) {
            injectKey(KeyEvent.KEYCODE_HOME);
        } else if (action == GravityBoxSettings.HWKEY_ACTION_BACK) {
            injectKey(KeyEvent.KEYCODE_BACK);
        } else if (action == GravityBoxSettings.HWKEY_ACTION_SCREEN_RECORDING) {
            toggleScreenRecording();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_AUTO_ROTATION) {
            toggleAutoRotation();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_SHOW_POWER_MENU) {
            showGlobalActionsDialog();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_EXPAND_NOTIFICATIONS) {
            expandNotificationsPanel();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_EXPAND_QUICKSETTINGS) {
            expandSettingsPanel();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_SCREENSHOT) {
            takeScreenshot();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_VOLUME_PANEL) {
            showVolumePanel();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_LAUNCHER_DRAWER) {
            showLauncherDrawer();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_BRIGHTNESS_DIALOG) {
            showBrightnessDialog();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_CLEAR_ALL_RECENTS_SINGLETAP) {
            clearAllRecents(false);
        } else if (action == GravityBoxSettings.HWKEY_ACTION_CLEAR_ALL_RECENTS_LONGPRESS) {
            clearAllRecents(true);
        }
    }

    private static void launchSearchActivity() {
        try {
            XposedHelpers.callMethod(mPhoneWindowManager, "launchAssistAction");
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void launchVoiceSearchActivity() {
        try {
            XposedHelpers.callMethod(mPhoneWindowManager, "launchAssistLongPressAction");
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void killForegroundApp() {
        Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null) return;

        handler.post(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        final Intent intent = new Intent(Intent.ACTION_MAIN);
                        final PackageManager pm = mContext.getPackageManager();
                        String defaultHomePackage = "com.android.launcher";
                        intent.addCategory(Intent.CATEGORY_HOME);
                        
                        final ResolveInfo res = pm.resolveActivity(intent, 0);
                        if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
                            defaultHomePackage = res.activityInfo.packageName;
                        }
        
                        Object mgr = XposedHelpers.callStaticMethod(classActivityManagerNative, "getDefault");
        
                        @SuppressWarnings("unchecked")
                        List<RunningAppProcessInfo> apps = (List<RunningAppProcessInfo>) 
                                XposedHelpers.callMethod(mgr, "getRunningAppProcesses");
        
                        String targetKilled = null;
                        for (RunningAppProcessInfo appInfo : apps) {  
                            int uid = appInfo.uid;  
                            // Make sure it's a foreground user application (not system,  
                            // root, phone, etc.)  
                            if (uid >= Process.FIRST_APPLICATION_UID && uid <= Process.LAST_APPLICATION_UID  
                                    && appInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                                    !mKillIgnoreList.contains(appInfo.processName) &&
                                    !appInfo.processName.equals(defaultHomePackage)) {  
                                if (DEBUG) log("Killing process ID " + appInfo.pid + ": " + appInfo.processName);
                                Process.killProcess(appInfo.pid);
                                targetKilled = appInfo.processName;
                                try {
                                    targetKilled = (String) pm.getApplicationLabel(
                                            pm.getApplicationInfo(targetKilled, 0));
                                } catch (PackageManager.NameNotFoundException nfe) {
                                    //
                                }
                                break;
                            }  
                        }
        
                        if (targetKilled != null) {
                            Class<?>[] paramArgs = new Class<?>[3];
                            paramArgs[0] = XposedHelpers.findClass(CLASS_WINDOW_STATE, null);
                            paramArgs[1] = int.class;
                            paramArgs[2] = boolean.class;
                            XposedHelpers.callMethod(mPhoneWindowManager, "performHapticFeedbackLw",
                                    paramArgs, null, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING, true);
                            Toast.makeText(mContext, 
                                    String.format(mStrAppKilled, targetKilled), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, mStrNothingToKill, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {  
                        XposedBridge.log(e);  
                    }
                }
            }
         );
    }

    private static void switchToLastApp() {
        Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null) return;

        handler.post(
            new Runnable() {
                @Override
                public void run() {
                    int lastAppId = 0;
                    int looper = 1;
                    String packageName;
                    final Intent intent = new Intent(Intent.ACTION_MAIN);
                    final ActivityManager am = (ActivityManager) mContext
                            .getSystemService(Context.ACTIVITY_SERVICE);
                    String defaultHomePackage = "com.android.launcher";
                    intent.addCategory(Intent.CATEGORY_HOME);
                    final ResolveInfo res = mContext.getPackageManager().resolveActivity(intent, 0);
                    if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
                        defaultHomePackage = res.activityInfo.packageName;
                    }
                    List <ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(5);
                    // lets get enough tasks to find something to switch to
                    // Note, we'll only get as many as the system currently has - up to 5
                    while ((lastAppId == 0) && (looper < tasks.size())) {
                        packageName = tasks.get(looper).topActivity.getPackageName();
                        if (!packageName.equals(defaultHomePackage) && !packageName.equals("com.android.systemui")) {
                            lastAppId = tasks.get(looper).id;
                        }
                        looper++;
                    }
                    if (lastAppId != 0) {
                        am.moveTaskToFront(lastAppId, ActivityManager.MOVE_TASK_NO_USER_ACTION);
                    } else {
                        Toast.makeText(mContext, mStrNoPrevApp, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }

    private static void goToSleep() {
        try {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            pm.goToSleep(SystemClock.uptimeMillis());
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void toggleRecentApps() {
        try {
            XposedHelpers.callMethod(mPhoneWindowManager, "sendCloseSystemWindows", 
                    SYSTEM_DIALOG_REASON_RECENT_APPS);
        } catch (Throwable t) {
            log("Error executing sendCloseSystemWindows(SYSTEM_DIALOG_REASON_RECENT_APPS): " + t.getMessage());
        }
        try {
            final Object sbService = XposedHelpers.callMethod(mPhoneWindowManager, "getStatusBarService"); 
            XposedHelpers.callMethod(sbService, "toggleRecentApps");
        } catch (Throwable t) {
            log("Error executing toggleRecentApps(): " + t.getMessage());
        }
    }

    private static void launchCustomApp(final int action) {
        Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null) return;
        mPrefs.reload();

        handler.post(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        String appInfo = (action == GravityBoxSettings.HWKEY_ACTION_CUSTOM_APP) ?
                                mPrefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_CUSTOM_APP, null) :
                                    mPrefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_CUSTOM_APP2, null);
                        if (appInfo == null) {
                            Toast.makeText(mContext, mStrCustomAppNone, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Intent i = Intent.parseUri(appInfo, 0);
                        // if intent is a GB action of broadcast type, handle it directly here
                        if (ShortcutActivity.isGbBroadcastShortcut(i)) {
                            Intent newIntent = new Intent(i.getStringExtra(ShortcutActivity.EXTRA_ACTION));
                            newIntent.putExtras(i);
                            mContext.sendBroadcast(newIntent);
                        // otherwise start activity
                        } else {
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            mContext.startActivity(i);
                        }
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(mContext, mStrCustomAppMissing, Toast.LENGTH_SHORT).show();
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }
                }
            }
        );
    }

    private static void injectKey(final int keyCode) {
        Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null) return;

        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final long eventTime = SystemClock.uptimeMillis();
                    final InputManager inputManager = (InputManager)
                            mContext.getSystemService(Context.INPUT_SERVICE);
                    XposedHelpers.callMethod(inputManager, "injectInputEvent",
                            new KeyEvent(eventTime - 50, eventTime - 50, KeyEvent.ACTION_DOWN, 
                                    keyCode, 0), 0);
                    XposedHelpers.callMethod(inputManager, "injectInputEvent",
                            new KeyEvent(eventTime - 50, eventTime - 25, KeyEvent.ACTION_UP, 
                                    keyCode, 0), 0);
                } catch (Throwable t) {
                        XposedBridge.log(t);
                }
            }
        });
    }

    private static void toggleExpandedDesktop() {
        toggleExpandedDesktop(GravityBoxSettings.HWKEY_ACTION_DEFAULT);
    }

    private static void toggleExpandedDesktop(final int value) {
        Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null) return;

        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final ContentResolver resolver = mContext.getContentResolver();
                    final int edMode = Integer.valueOf(mPrefs.getString(
                            GravityBoxSettings.PREF_KEY_EXPANDED_DESKTOP, "0"));
                    if (edMode == GravityBoxSettings.ED_DISABLED) {
                        Toast.makeText(mContext, mStrExpandedDesktopDisabled, Toast.LENGTH_SHORT).show();
                    } else {
                        if (value == GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                            final int edState = Settings.Global.getInt(resolver,
                                    ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE, 0);
                            Settings.Global.putInt(resolver, 
                                    ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE,
                                    (edState == 1) ? 0 : 1);
                        } else {
                            // action = "gravitybox.intent.action.TOGGLE_EXPANDED_DESKTOP"
                            // extra = "hwKeyValue:0" (ED toggle)
                            // extra = "hwKeyValue:1" (ED on)
                            // extra = "hwKeyValue:-1" (ED off), (any value not 0 or 1),
                            // no extra = (ED toggle, like GB shortcut)
                            Settings.Global.putInt(resolver, 
                                    ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE,
                                        (value == 1) ? 0 : 1);
                        }
                    }
                } catch (Throwable t) {
                        XposedBridge.log(t);
                }
            }
        });
    }

    private static void toggleTorch() {
        try {
            Intent intent = new Intent(mGbContext, TorchService.class);
            intent.setAction(TorchService.ACTION_TOGGLE_TORCH);
            mGbContext.startService(intent);
        } catch (Throwable t) {
            log("Error toggling Torch: " + t.getMessage());
        }
    }

    private static void showAppLauncher() {
        Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null || mAppLauncher == null) return;

        handler.post(new Runnable() {
            @Override
            public void run() {
                mAppLauncher.showDialog();
            }
        });
    }

    private static void toggleScreenRecording() {
        try {
            Intent intent = new Intent(mGbContext, ScreenRecordingService.class);
            intent.setAction(ScreenRecordingService.ACTION_TOGGLE_SCREEN_RECORDING);
            mGbContext.startService(intent);
        } catch (Throwable t) {
            log("Error toggling screen recording: " + t.getMessage());
        }
    }

    private static void toggleAutoRotation() {
        try {
            Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    final Class<?> rlPolicyClass = XposedHelpers.findClass(
                            "com.android.internal.view.RotationPolicy", null);
                    final boolean locked = (Boolean) XposedHelpers.callStaticMethod(
                            rlPolicyClass, "isRotationLocked", mContext);
                    XposedHelpers.callStaticMethod(rlPolicyClass, "setRotationLock", mContext, !locked);
                    if (locked) {
                        Toast.makeText(mContext, mStrAutoRotationEnabled, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mContext, mStrAutoRotationDisabled, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Throwable t) {
            log("Error toggling auto rotation: " + t.getMessage());
        }
    }

    private static final Object mScreenshotLock = new Object();
    private static ServiceConnection mScreenshotConnection = null;  
    private static void takeScreenshot() {
        final Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null) return;

        synchronized (mScreenshotLock) {  
            if (mScreenshotConnection != null) {  
                return;  
            }  
            ComponentName cn = new ComponentName("com.android.systemui",  
                    "com.android.systemui.screenshot.TakeScreenshotService");  
            Intent intent = new Intent();  
            intent.setComponent(cn);  
            ServiceConnection conn = new ServiceConnection() {  
                @Override  
                public void onServiceConnected(ComponentName name, IBinder service) {  
                    synchronized (mScreenshotLock) {  
                        if (mScreenshotConnection != this) {  
                            return;  
                        }  
                        final Messenger messenger = new Messenger(service);  
                        final Message msg = Message.obtain(null, 1);  
                        final ServiceConnection myConn = this;  
                                                
                        Handler h = new Handler(handler.getLooper()) {  
                            @Override  
                            public void handleMessage(Message msg) {  
                                synchronized (mScreenshotLock) {  
                                    if (mScreenshotConnection == myConn) {  
                                        mContext.unbindService(mScreenshotConnection);  
                                        mScreenshotConnection = null;  
                                        handler.removeCallbacks(mScreenshotTimeout);  
                                    }  
                                }  
                            }  
                        };  
                        msg.replyTo = new Messenger(h);  
                        msg.arg1 = msg.arg2 = 0;  
                        h.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    messenger.send(msg);
                                } catch (RemoteException e) {
                                    XposedBridge.log(e);
                                }
                            }
                        }, 1000);
                    }  
                }  
                @Override  
                public void onServiceDisconnected(ComponentName name) {}  
            };  
            if (mContext.bindService(intent, conn, Context.BIND_AUTO_CREATE)) {  
                mScreenshotConnection = conn;  
                handler.postDelayed(mScreenshotTimeout, 10000);  
            }  
        }
    }
    
    private static final Runnable mScreenshotTimeout = new Runnable() {
        @Override
        public void run() {
            synchronized (mScreenshotLock) {
                if (mScreenshotConnection != null) {
                    mContext.unbindService(mScreenshotConnection);
                    mScreenshotConnection = null;
                }
            }
        }
    };

    private static void showGlobalActionsDialog() {
        try {
            Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    XposedHelpers.callMethod(mPhoneWindowManager, "sendCloseSystemWindows", 
                            SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS);
                    XposedHelpers.callMethod(mPhoneWindowManager, "showGlobalActionsDialog");
                }
            });
        } catch (Throwable t) {
            log("Error executing PhoneWindowManager.showGlobalActionsDialog(): " + t.getMessage());
        }
    }

    private static void expandNotificationsPanel() {
        try {
            final Object sbService = XposedHelpers.callMethod(mPhoneWindowManager, "getStatusBarService"); 
            XposedHelpers.callMethod(sbService, "expandNotificationsPanel");
        } catch (Throwable t) {
            log("Error executing expandNotificationsPanel(): " + t.getMessage());
        }
    }

    private static void expandSettingsPanel() {
        try {
            final Object sbService = XposedHelpers.callMethod(mPhoneWindowManager, "getStatusBarService"); 
            XposedHelpers.callMethod(sbService, "expandSettingsPanel");
        } catch (Throwable t) {
            log("Error executing expandSettingsPanel(): " + t.getMessage());
        }
    }

    private static void showVolumePanel() {
        try {
            Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                    am.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
                }
            });
        } catch (Throwable t) {
            log("Error executing showVolumePanel: " + t.getMessage());
        }
    }

    private static void showLauncherDrawer() {
        try {
            Intent intent = new Intent(ModLauncher.ACTION_SHOW_APP_DRAWER);
            mContext.sendBroadcast(intent);
        } catch (Throwable t) {
            log("Error executing showLauncherDrawer: " + t.getMessage());
        }
    }

    private static void clearAllRecents(boolean longPress) {
        try {
            Intent intent;
            if (!longPress) {
                intent = new Intent(GravityBoxSettings.ACTION_RECENTS_CLEAR_ALL_SINGLETAP);
            } else {
                intent = new Intent(GravityBoxSettings.ACTION_RECENTS_CLEAR_ALL_LONGPRESS);
            }
            mContext.sendBroadcast(intent);
        } catch (Throwable t) {
            log("Error executing clearAllRecents(" + longPress + "): " + t.getMessage());
        }
    }

    private static void showBrightnessDialog() {
        try {
            Intent intent = new Intent("android.intent.action.SHOW_BRIGHTNESS_DIALOG");
            mContext.sendBroadcast(intent);
        } catch (Throwable t) {
            log("Error executing showBrightnessDialog: " + t.getMessage());
        }
    }

    private static boolean mPreloadedRecentApps;
    private static void preloadRecentApps() {
        mPreloadedRecentApps = true;
        try {
            final Object sbService = XposedHelpers.callMethod(mPhoneWindowManager, "getStatusBarService");
            if (sbService != null) {
                XposedHelpers.callMethod(sbService, "preloadRecentApps");
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void cancelPreloadRecentApps() {
        if (mPreloadedRecentApps) {
            mPreloadedRecentApps = false;
            try {
                final Object sbService = XposedHelpers.callMethod(mPhoneWindowManager, "getStatusBarService");
                if (sbService != null) {
                    XposedHelpers.callMethod(sbService, "cancelPreloadRecentApps");
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    }
}
