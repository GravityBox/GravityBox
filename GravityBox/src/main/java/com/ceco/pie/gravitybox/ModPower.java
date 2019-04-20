/*
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2019 Peter Gregus for GravityBox Project (C3C076@xda)
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
package com.ceco.pie.gravitybox;

import com.ceco.pie.gravitybox.ledcontrol.QuietHours;
import com.ceco.pie.gravitybox.ledcontrol.QuietHoursActivity;
import com.ceco.pie.gravitybox.managers.BatteryInfoManager;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.telephony.TelephonyManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModPower {
    private static final String TAG = "GB:ModPower";
    private static final String CLASS_PM_SERVICE = "com.android.server.power.PowerManagerService";
    private static final String CLASS_PM_HANDLER = "com.android.server.power.PowerManagerService$PowerManagerHandler";
    private static final String CLASS_PM_NOTIFIER = "com.android.server.power.Notifier";
    private static final String CLASS_SHUTDOWN_THREAD = "com.android.server.power.ShutdownThread";
    private static final boolean DEBUG = false;

    private static final int MSG_WAKE_UP = 100;
    private static final int MSG_UNREGISTER_PROX_SENSOR_LISTENER = 101;
    public static final int MAX_PROXIMITY_WAIT = 500;
    private static final int MAX_PROXIMITY_TTL = MAX_PROXIMITY_WAIT * 2;

    private static Context mContext;
    private static Handler mHandler;
    private static SensorManager mSensorManager;
    private static Sensor mProxSensor;
    private static Object mLock;
    private static Runnable mWakeUpRunnable;
    private static boolean mProxSensorCovered;
    private static WakeLock mWakeLock;
    private static boolean mIgnoreIncomingCall;
    private static boolean mIsChargingSoundCustom;
    private static boolean mMotoHooksCreated;
    private static int mLockscreenTorch = 0;
    private static QuietHours mQh;
    private static boolean mAdvancedPowerMenuEnabled;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_POWER_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_POWER_PROXIMITY_WAKE)) {
                    toggleWakeUpWithProximityFeature(intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_POWER_PROXIMITY_WAKE, false));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_POWER_PROXIMITY_WAKE_IGNORE_CALL)) {
                    mIgnoreIncomingCall = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_POWER_PROXIMITY_WAKE_IGNORE_CALL, false);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_POWER_ADVANCED)) {
                    mAdvancedPowerMenuEnabled = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_POWER_ADVANCED, false);
                }
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_BATTERY_SOUND_CHANGED) &&
                    intent.getIntExtra(GravityBoxSettings.EXTRA_BATTERY_SOUND_TYPE, -1) == 
                        BatteryInfoManager.SOUND_PLUGGED) {
                updateIsChargingSoundCustom(intent.getStringExtra(
                        GravityBoxSettings.EXTRA_BATTERY_SOUND_URI));
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_HWKEY_LOCKSCREEN_TORCH_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_HWKEY_TORCH)) {
                    mLockscreenTorch = intent.getIntExtra(GravityBoxSettings.EXTRA_HWKEY_TORCH,
                            GravityBoxSettings.HWKEY_TORCH_DISABLED);
                }
            } else if (intent.getAction().equals(QuietHoursActivity.ACTION_QUIET_HOURS_CHANGED)) {
                mQh = new QuietHours(intent.getExtras());
            }
        }
    };

    public static void initAndroid(final XSharedPreferences prefs, final XSharedPreferences qhPrefs,
                                   final ClassLoader classLoader) {
        mQh = new QuietHours(qhPrefs);

        Class<?> pmServiceClass = null;
        try {
            pmServiceClass = XposedHelpers.findClass(CLASS_PM_SERVICE, classLoader);
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
        }

        // wake up with proximity feature
        try {
            Class<?> pmHandlerClass = XposedHelpers.findClass(CLASS_PM_HANDLER, classLoader);

            mIgnoreIncomingCall = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_POWER_PROXIMITY_WAKE_IGNORE_CALL, false);
            mLockscreenTorch = Integer.valueOf(
                    prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_LOCKSCREEN_TORCH, "0"));

            XposedBridge.hookAllMethods(pmServiceClass, "systemReady", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                    mLock = XposedHelpers.getObjectField(param.thisObject, "mLock");
                    toggleWakeUpWithProximityFeature(prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_POWER_PROXIMITY_WAKE, false));

                    IntentFilter intentFilter = new IntentFilter(GravityBoxSettings.ACTION_PREF_POWER_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_BATTERY_SOUND_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_LOCKSCREEN_TORCH_CHANGED);
                    intentFilter.addAction(QuietHoursActivity.ACTION_QUIET_HOURS_CHANGED);
                    mContext.registerReceiver(mBroadcastReceiver, intentFilter);
                }
            });

            XposedHelpers.findAndHookMethod(pmServiceClass, "wakeUpInternal",
                    long.class, String.class, int.class, String.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) {
                    if (Utils.isMotoXtDevice()) {
                        createMotoSpecificHooks(classLoader);
                    }
                    if (!shouldRunProximityCheck())
                        return;

                    //noinspection SynchronizeOnNonFinalField
                    synchronized (mLock) {
                        if (mHandler.hasMessages(MSG_WAKE_UP)) {
                            if (DEBUG) log("wakeUpInternal: Wake up message already queued");
                            param.setResult(null);
                            return;
                        }

                        mWakeUpRunnable = () -> {
                            final long ident = Binder.clearCallingIdentity();
                            try {
                                if (DEBUG) log("Waking up...");
                                XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                            } catch (Throwable ignored) {
                            } finally {
                                Binder.restoreCallingIdentity(ident);
                            }
                        };
                        runWithProximityCheck();
                        param.setResult(null);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(pmHandlerClass, "handleMessage",
                    Message.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) {
                    Message msg = (Message) param.args[0];
                    if (msg.what == MSG_WAKE_UP) {
                        mWakeUpRunnable.run();
                        unregisterProxSensorListener();
                    } else if (msg.what == MSG_UNREGISTER_PROX_SENSOR_LISTENER) {
                        unregisterProxSensorListener();
                    }
                }
            });
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
        }

        // Charging started
        try {
            updateIsChargingSoundCustom(prefs.getString(
                    GravityBoxSettings.PREF_KEY_CHARGER_PLUGGED_SOUND, null));

            XposedHelpers.findAndHookMethod(CLASS_PM_NOTIFIER, classLoader,
                    "playChargingStartedSound", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) {
                    if (mIsChargingSoundCustom || mQh.isSystemSoundMuted(QuietHours.SystemSound.CHARGER)) {
                        param.setResult(null);
                    }
                }
            });
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
        }

        // Wake on plug for TouchWiz
        try {
            if (!prefs.getBoolean(GravityBoxSettings.PREF_KEY_UNPLUG_TURNS_ON_SCREEN, true)) {
                XposedHelpers.findAndHookMethod(pmServiceClass, "shouldWakeUpWhenPluggedOrUnpluggedLocked",
                    boolean.class, int.class, boolean.class, XC_MethodReplacement.returnConstant(false));
            }
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
        }

        // Advanced power menu: Adjust reboot dialog titles
        try {
            mAdvancedPowerMenuEnabled = prefs.getBoolean(GravityBoxSettings.PREF_KEY_POWEROFF_ADVANCED, false);
            final Class<?> classShutdownThread = XposedHelpers.findClass(CLASS_SHUTDOWN_THREAD, classLoader);
            XposedHelpers.findAndHookMethod(classShutdownThread, "showShutdownDialog",
                    Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (mAdvancedPowerMenuEnabled) {
                        String reason = (String) XposedHelpers.getStaticObjectField(classShutdownThread, "mReason");
                        Dialog d = (Dialog) param.getResult();
                        if (d != null) {
                            if ("recovery".equals(reason)) {
                                d.setTitle("Recovery");
                            } else if ("bootloader".equals(reason)) {
                                d.setTitle("Bootloader");
                            }
                            if (DEBUG) log("showShutdownDialog: mReason=" + reason + "; dialog title replaced");
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classShutdownThread, "showSysuiReboot", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (mAdvancedPowerMenuEnabled) {
                        String reason = (String) XposedHelpers.getStaticObjectField(classShutdownThread, "mReason");
                        if ("recovery".equals(reason) || "bootloader".equals(reason)) {
                            if (DEBUG) log("showSysuiReboot: mReason=" + reason + "; SysUI dialog disabled");
                            param.setResult(false);
                        }
                    }
                }
            });
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
        }
    }

    private static void toggleWakeUpWithProximityFeature(boolean enabled) {
        try {
            if (enabled) {
                mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
                mProxSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                mWakeLock = ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE))
                        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            } else {
                unregisterProxSensorListener();
                mProxSensor = null;
                mSensorManager = null;
                mWakeLock = null;
            }
            if (DEBUG) log("toggleWakeUpWithProximityFeature: " + enabled);
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
        }
    }

    private static boolean shouldRunProximityCheck() {
        return (mSensorManager != null && mProxSensor != null &&
                !(mIgnoreIncomingCall && isIncomingCall()));
    }

    private static boolean isIncomingCall() {
        try {
            TelephonyManager phone = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            return (phone.getCallState() == TelephonyManager.CALL_STATE_RINGING);
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
            return false;
        }
    }

    private static void runWithProximityCheck() {
        if (mHandler.hasMessages(MSG_WAKE_UP)) {
            if (DEBUG) log("runWithProximityCheck: Wake up message already queued");
        } else if (mHandler.hasMessages(MSG_UNREGISTER_PROX_SENSOR_LISTENER)) {
            mHandler.removeMessages(MSG_UNREGISTER_PROX_SENSOR_LISTENER);
            mHandler.sendEmptyMessageDelayed(MSG_UNREGISTER_PROX_SENSOR_LISTENER, MAX_PROXIMITY_TTL);
            if (DEBUG) log("Proximity sensor listener still alive; mProxSensorCovered=" + mProxSensorCovered);
            if (!mProxSensorCovered) {
                mWakeUpRunnable.run();
            }
        } else {
            mHandler.sendEmptyMessageDelayed(MSG_WAKE_UP, MAX_PROXIMITY_WAIT);
            mSensorManager.registerListener(mProxSensorListener, mProxSensor, 
                    SensorManager.SENSOR_DELAY_FASTEST);
            mWakeLock.acquire();
            if (DEBUG) log("Proximity sensor listener resgistered");
        }
    }

    private static void unregisterProxSensorListener() {
        if (mSensorManager != null && mProxSensor != null) {
            mSensorManager.unregisterListener(mProxSensorListener, mProxSensor);
            if (DEBUG) log("Proximity sensor listener unregistered");
        }
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private static SensorEventListener mProxSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mProxSensorCovered = event.values[0] != mProxSensor.getMaximumRange();
            if (DEBUG) log("onSensorChanged:  mProxSensorCovered=" + mProxSensorCovered);
            if (!mHandler.hasMessages(MSG_UNREGISTER_PROX_SENSOR_LISTENER)) {
                if (DEBUG) log("Proximity sensor listener was not alive; scheduling unreg");
                mHandler.sendEmptyMessageDelayed(MSG_UNREGISTER_PROX_SENSOR_LISTENER, MAX_PROXIMITY_TTL);
                if (!mHandler.hasMessages(MSG_WAKE_UP)) {
                    if (DEBUG) log("Prox sensor status received too late. Wake up already triggered");
                    return;
                }
                mHandler.removeMessages(MSG_WAKE_UP);
                if (!mProxSensorCovered) {
                    mWakeUpRunnable.run();
                }
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private static void updateIsChargingSoundCustom(String value) {
        mIsChargingSoundCustom = (value != null &&
                !value.equals("content://settings/system/notification_sound"));
        if (DEBUG) log("mIsChargingSoundCustom: " + mIsChargingSoundCustom +
                " [" + (value != null && value.isEmpty() ? "silent" : value) + "]");
    }

    private static void createMotoSpecificHooks(ClassLoader cl) {
        if (mMotoHooksCreated)
            return;

        try {
            Class<?> classSm = XposedHelpers.findClass("android.os.ServiceManager", cl);
            IBinder b = (IBinder) XposedHelpers.callStaticMethod(classSm, "checkService", "motodisplay_int_service");
            Class<?> stub = XposedHelpers.findClass("android.app.IMotoDisplayIntService$Stub", cl);
            Object mds = XposedHelpers.callStaticMethod(stub, "asInterface", b);
            if (mds != null) {
                if (DEBUG) log("createMotoSpecificHooks: got MotoDisplayService: " + mds);
                XposedHelpers.findAndHookMethod(mds.getClass().getName(), cl,
                        "notifyPowerKeyWakeup", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(final MethodHookParam param) {
                        if (shouldRunProximityCheck()) {
                            param.setResult(null);
                            if (DEBUG) log("notifyPowerKeyWakeup: suppressed due to proximity wake up");
                        } else if (mLockscreenTorch == GravityBoxSettings.HWKEY_TORCH_POWER_LONGPRESS) {
                            param.setResult(null);
                            if (DEBUG) log("notifyPowerKeyWakeup: suppressed due to power long-press torch");
                        }
                    }
                });
                mMotoHooksCreated = true;
            }
        } catch (Throwable t) {
            GravityBox.log(TAG, "createMotoSpecificHooks:", t);
        }
    }
}
