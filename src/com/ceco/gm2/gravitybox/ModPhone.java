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

package com.ceco.gm2.gravitybox;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModPhone {
    private static final String TAG = "GB:ModPhone";
    public static final String PACKAGE_NAME = "com.android.phone";
    private static final String CLASS_IN_CALL_SCREEN = "com.android.phone.InCallScreen";
    private static final String ENUM_PHONE_STATE = Build.VERSION.SDK_INT > 16 ?
            "com.android.internal.telephony.PhoneConstants$State" :
            "com.android.internal.telephony.Phone$State";
    private static final String ENUM_CALL_STATE = "com.android.internal.telephony.Call$State";
    private static final String CLASS_ASYNC_RESULT = "android.os.AsyncResult";
    private static final String CLASS_CALL_NOTIFIER = "com.android.phone.CallNotifier";
    private static final String CLASS_SERVICE_STATE_EXT = "com.mediatek.op.telephony.ServiceStateExt";
    private static final String CLASS_GSM_SERVICE_STATE_TRACKER = 
            "com.android.internal.telephony.gsm.GsmServiceStateTracker";
    private static final String CLASS_PHONE_UTILS = "com.android.phone.PhoneUtils";
    private static final boolean DEBUG = false;

    private static final int VIBRATE_45_SEC = 2828;

    private static SensorManager mSensorManager;
    private static boolean mSensorListenerAttached = false;
    private static int mFlipAction = GravityBoxSettings.PHONE_FLIP_ACTION_NONE;
    private static Object mInCallScreen;
    private static Unhook mVibrateHook;
    private static Vibrator mVibrator;
    private static Handler mCallNotifier;
    private static Class<?> mPhoneUtilsClass;
    private static XSharedPreferences mPrefsPhone;
    private static Set<String> mCallVibrations;
    private static WakeLock mWakeLock;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static PhoneSensorEventListener mPhoneSensorEventListener = 
            new PhoneSensorEventListener(new PhoneSensorEventListener.ActionHandler() {

                @Override
                public void onFaceUp() {
                    if (DEBUG) log("PhoneSensorEventListener.onFaceUp");
                    // do nothing
                }

                @Override
                public void onFaceDown() {
                    if (DEBUG) log("PhoneSensorEventListener.onFaceDown");
                    if (mInCallScreen == null) return;

                    try {
                        switch (mFlipAction) {
                            case GravityBoxSettings.PHONE_FLIP_ACTION_MUTE:
                                if (DEBUG) log("Muting call");
                                XposedHelpers.callMethod(mInCallScreen, "internalSilenceRinger");
                                break;
                            case GravityBoxSettings.PHONE_FLIP_ACTION_DISMISS:
                                if (DEBUG) log("Dismissing call");
                                XposedHelpers.callMethod(mInCallScreen, "internalHangup");
                                break;
                            case GravityBoxSettings.PHONE_FLIP_ACTION_NONE:
                            default:
                                // do nothing
                        }
                    } catch (Exception e) {
                        XposedBridge.log(e);
                    }
                }
            });

    private static void attachSensorListener() {
        if (mSensorManager == null || 
                mSensorListenerAttached ||
                mFlipAction == GravityBoxSettings.PHONE_FLIP_ACTION_NONE) return;

        mPhoneSensorEventListener.reset();
        mSensorManager.registerListener(mPhoneSensorEventListener, 
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorListenerAttached = true;

        if (DEBUG) log("Sensor listener attached");
    }

    private static void detachSensorListener() {
        if (mSensorManager == null || !mSensorListenerAttached) return;

        mSensorManager.unregisterListener(mPhoneSensorEventListener);
        mSensorListenerAttached = false;

        if (DEBUG) log("Sensor listener detached");
    }

    public static void initZygote(final XSharedPreferences prefs) {
        try {
            if (Utils.hasGeminiSupport()) {
                final Class<?> classServiceStateExt = XposedHelpers.findClass(CLASS_SERVICE_STATE_EXT, null);

                XposedHelpers.findAndHookMethod(classServiceStateExt, "ignoreDomesticRoaming", 
                        new XC_MethodReplacement() {

                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        prefs.reload();
                        boolean mvno = prefs.getBoolean(GravityBoxSettings.PREF_KEY_NATIONAL_ROAMING, false);
                        if (DEBUG) log("ignoreDomesticRoaming: " + mvno);
                        return mvno;
                    }
                });
            } else {
                final Class<?> classGsmServiceStateTracker = XposedHelpers.findClass(
                        CLASS_GSM_SERVICE_STATE_TRACKER, null);

                XposedHelpers.findAndHookMethod(classGsmServiceStateTracker, "isRoamingBetweenOperators", 
                        boolean.class, "android.telephony.ServiceState", new XC_MethodHook() {

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        prefs.reload();
                        boolean mvno = prefs.getBoolean(GravityBoxSettings.PREF_KEY_NATIONAL_ROAMING, false);
                        final Class<?> classSystemProperties = 
                                XposedHelpers.findClass("android.os.SystemProperties", null);
                        String simNumeric = (String) XposedHelpers.callStaticMethod(
                                classSystemProperties, "get", "gsm.sim.operator.numeric", "");
                        String operatorNumeric = (String) XposedHelpers.callMethod(
                                param.args[1], "getOperatorNumeric");
                        boolean equalsMcc = true;
                        try {
                            equalsMcc = simNumeric.substring(0, 3).
                                    equals(operatorNumeric.substring(0, 3));
                        } catch (Exception e) { }

                        boolean result = (Boolean) param.getResult();
                        result = result && !(equalsMcc && mvno);
                        if (DEBUG) log("isRoamingBetweenOperators: " + result);
                        param.setResult(result);
                    }
                });
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    @SuppressWarnings("unchecked")
    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            mPrefsPhone = prefs;

            final Class<?> classInCallScreen = XposedHelpers.findClass(CLASS_IN_CALL_SCREEN, classLoader);
            final Class<? extends Enum> enumPhoneState = (Class<? extends Enum>) Class.forName(ENUM_PHONE_STATE);
            final Class<?> classCallNotifier = XposedHelpers.findClass(CLASS_CALL_NOTIFIER, classLoader);
            final Class<? extends Enum> enumCallState = (Class<? extends Enum>) Class.forName(ENUM_CALL_STATE);
            mPhoneUtilsClass = XposedHelpers.findClass(CLASS_PHONE_UTILS, classLoader);

            XposedHelpers.findAndHookMethod(classInCallScreen, "onCreate", Bundle.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mInCallScreen = param.thisObject;
                    Context context = (Context) mInCallScreen;
                    if (context == null) {
                        log("Context is null. Skipping...");
                        return;
                    }

                    mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                }
            });

            XposedHelpers.findAndHookMethod(classInCallScreen, 
                    "onPhoneStateChanged", CLASS_ASYNC_RESULT, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object mCM = XposedHelpers.getObjectField(param.thisObject, "mCM");
                    if (mCM == null) return;

                    if (XposedHelpers.callMethod(mCM, "getState") == Enum.valueOf(enumPhoneState, "RINGING")) {
                        if (DEBUG) log("PHONE_STATE is RINGING - attaching sensor listener (if not attached yet)");
                        refreshPhonePrefs();
                        attachSensorListener();
                    } else {
                        if (DEBUG) log("PHONE_STATE is NOT RINGING - detaching sensor listener (if is attached)");
                        detachSensorListener();
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classInCallScreen, "internalSilenceRinger", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (DEBUG) log("internalSilenceRinger - detaching sensor listener (if is attached)");
                    detachSensorListener();
                }
            });

            XposedBridge.hookAllConstructors(classCallNotifier, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mCallNotifier = (Handler) param.thisObject;
                    Object app = XposedHelpers.getObjectField(param.thisObject, "mApplication");
                    if (app != null) {
                        Method m = app.getClass().getMethod("getSystemService", String.class);
                        mVibrator = (Vibrator) m.invoke(app, Context.VIBRATOR_SERVICE);
                        PowerManager pm = (PowerManager) m.invoke(app, Context.POWER_SERVICE);
                        mWakeLock  = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GbModPhone");
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classCallNotifier, 
                    "onPhoneStateChanged", CLASS_ASYNC_RESULT, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    refreshPhonePrefs();
                    if (mVibrator == null) return;

                    if (DEBUG ) log("CallNotifier: onPhoneStateChanged ENTERED");
                    mVibrateHook = XposedHelpers.findAndHookMethod(
                            mVibrator.getClass(), "vibrate", long.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param2) throws Throwable {
                            if (DEBUG) log("Vibrator: vibrate called from CallNotifier - ignoring");
                            param2.setResult(null);
                        }
                    });
                    if (DEBUG) log("CallNotifier: Vibrator.vibrate() method hooked");
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mVibrateHook != null) {
                        if (DEBUG) log("CallNotifier: unhooking vibrate hook");
                        mVibrateHook.unhook();
                        mVibrateHook = null;
                    }

                    if (mVibrator == null || 
                            (!mCallVibrations.contains(GravityBoxSettings.CV_CONNECTED)
                             && !mCallVibrations.contains(GravityBoxSettings.CV_PERIODIC))) {
                        return;
                    }

                    final Object cm = XposedHelpers.getObjectField(param.thisObject, "mCM");
                    final Object state = XposedHelpers.callMethod(cm, "getState");

                    if (state == Enum.valueOf(enumPhoneState, "OFFHOOK")) {
                        final Object fgPhone = XposedHelpers.callMethod(cm, "getFgPhone");
                        final Object call = getCurrentCall(fgPhone);
                        if (DEBUG) log("getCurrentCall returned: " + call);
                        final Object conn = getConnection(fgPhone, call);
                        if (XposedHelpers.callMethod(call, "getState") == 
                                Enum.valueOf(enumCallState, "ACTIVE") &&
                                !(Boolean) XposedHelpers.callMethod(conn, "isIncoming")) {
                            long callDurationMsec = 
                                    (Long) XposedHelpers.callMethod(conn, "getDurationMillis");
                            if (mCallVibrations.contains(GravityBoxSettings.CV_CONNECTED) 
                                    && callDurationMsec < 200) {
                                vibrate(100, 0, 0);
                                if (DEBUG) log("Executed vibrate on call connected");
                            }
                            if (mCallVibrations.contains(GravityBoxSettings.CV_PERIODIC)) {
                                callDurationMsec = callDurationMsec % 60000;
                                start45SecondVibration(callDurationMsec);
                                if (DEBUG) log("Started handler for periodic vibrations");
                            }
                        }
                    }

                    if (DEBUG ) log("CallNotifier: onPhoneStateChanged EXITED");
                }
            });

            XposedHelpers.findAndHookMethod(classCallNotifier, "handleMessage",
                    Message.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Message msg = (Message) param.args[0];
                    if (msg.what == VIBRATE_45_SEC &&
                            mCallVibrations.contains(GravityBoxSettings.CV_PERIODIC)) {
                        vibrate(70, 0, 0);
                        if (mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                        mWakeLock.acquire(61000);
                        mCallNotifier.sendEmptyMessageDelayed(VIBRATE_45_SEC, 60000);
                        if (DEBUG) log("Executed vibrate on receiving VIBRATE_45_SEC message");
                    }
                }
            });

            if (Utils.hasGeminiSupport()) {
                XposedHelpers.findAndHookMethod(classCallNotifier, "onDisconnect",
                        CLASS_ASYNC_RESULT, int.class, onDisconnectHook);
            } else {
                XposedHelpers.findAndHookMethod(classCallNotifier, "onDisconnect",
                        CLASS_ASYNC_RESULT, onDisconnectHook);
            }
            if (Utils.hasGeminiSupport() && !(Utils.isMt65x2Device())) {
                XposedHelpers.findAndHookMethod(classCallNotifier, "onNewRingingConnection",
                        CLASS_ASYNC_RESULT, int.class, onNewRingingConnectionHook);
            } else {
                XposedHelpers.findAndHookMethod(classCallNotifier, "onNewRingingConnection",
                        CLASS_ASYNC_RESULT, onNewRingingConnectionHook);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static XC_MethodHook onDisconnectHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (mVibrator == null) return;

            try {
                refreshPhonePrefs();
                Object conn = XposedHelpers.getObjectField(param.args[0], "result");
                if (conn != null) {
                    long durationMillis =
                            (Long) XposedHelpers.callMethod(conn, "getDurationMillis");
                    if (mCallVibrations.contains(GravityBoxSettings.CV_DISCONNECTED) 
                            && durationMillis > 0) {
                        vibrate(50, 100, 50);
                        if (DEBUG) log("Executed vibrate on call disconnected");
                    }
                    mCallNotifier.removeMessages(VIBRATE_45_SEC);
                }
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static XC_MethodHook onNewRingingConnectionHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (mVibrator == null) return;

            try {
                refreshPhonePrefs();
                if (!mCallVibrations.contains(GravityBoxSettings.CV_WAITING)) {
                    return;
                }

                Object conn = XposedHelpers.getObjectField(param.args[0], "result");
                Object ringing = XposedHelpers.callMethod(conn, "getCall");
                Object phone = XposedHelpers.callMethod(ringing, "getPhone");
                Object state = XposedHelpers.callMethod(conn, "getState");
                if (!(Boolean) XposedHelpers.callMethod(param.thisObject,
                                    "ignoreAllIncomingCalls", phone)
                        && (Boolean) XposedHelpers.callMethod(conn, "isRinging")
                        && !(Boolean) XposedHelpers.callStaticMethod(mPhoneUtilsClass,
                                "isRealIncomingCall", state)) {
                    vibrate(200, 300, 500);
                    if (DEBUG) log("Executed vibrate on call waiting");
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static void vibrate(int v1, int p1, int v2) {
        if (mVibrator == null) return;

        long[] pattern = new long[] { 0, v1, p1, v2 };
        mVibrator.vibrate(pattern, -1);
    }

    private static Object getCurrentCall(Object phone) {
        try {
            Object ringing = XposedHelpers.callMethod(phone, "getRingingCall");
            Object fg = XposedHelpers.callMethod(phone, "getForegroundCall");
            Object bg = XposedHelpers.callMethod(phone, "getBackgroundCall");
            if (!(Boolean) XposedHelpers.callMethod(ringing, "isIdle")) {
                return ringing;
            }
            if (!(Boolean) XposedHelpers.callMethod(fg, "isIdle")) {
                return fg;
            }
            if (!(Boolean) XposedHelpers.callMethod(bg, "isIdle")) {
                return bg;
            }
            return fg;
        } catch (Throwable t) {
            XposedBridge.log(t);
            return null;
        }
    }

    private static Object getConnection(Object phone, Object call) {
        if (call == null) return null;

        try {
            if ((Integer)XposedHelpers.callMethod(phone, "getPhoneType") ==
                    TelephonyManager.PHONE_TYPE_CDMA) {
                return XposedHelpers.callMethod(call, "getLatestConnection");
            }
            return XposedHelpers.callMethod(call, "getEarliestConnection");
        } catch (Throwable t) {
            XposedBridge.log(t);
            return null;
        }
    }

    private static void start45SecondVibration(long callDurationMsec) {
        if (mCallNotifier == null) return;

        try {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            mCallNotifier.removeMessages(VIBRATE_45_SEC);
            long timer;
            if (callDurationMsec > 45000) {
                // Schedule the alarm at the next minute + 45 secs
                timer = 45000 + 60000 - callDurationMsec;
            } else {
                // Schedule the alarm at the first 45 second mark
                timer = 45000 - callDurationMsec;
            }
            mWakeLock.acquire(timer + 1000);
            mCallNotifier.sendEmptyMessageDelayed(VIBRATE_45_SEC, timer);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void refreshPhonePrefs() {
        if (mPrefsPhone != null) {
            mPrefsPhone.reload();
            mCallVibrations = mPrefsPhone.getStringSet(
                    GravityBoxSettings.PREF_KEY_CALL_VIBRATIONS, new HashSet<String>());
            if (DEBUG) log("mCallVibrations = " + mCallVibrations.toString());

            mFlipAction = GravityBoxSettings.PHONE_FLIP_ACTION_NONE;
            try {
                mFlipAction = Integer.valueOf(mPrefsPhone.getString(
                        GravityBoxSettings.PREF_KEY_PHONE_FLIP, "0"));
                if (DEBUG) log("mFlipAction = " + mFlipAction);
            } catch (NumberFormatException e) {
                XposedBridge.log(e);
            }
        }
    }
}