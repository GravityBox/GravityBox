/*
 * Copyright (C) 2015 Peter Gregus for GravityBox Project (C3C076@xda)
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

package com.ceco.marshmallow.gravitybox;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ceco.marshmallow.gravitybox.ledcontrol.QuietHours;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.os.PowerManager.WakeLock;
import android.telecom.TelecomManager;
import android.view.View;
import android.widget.ImageView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModDialer {
    private static final String TAG = "GB:ModDialer";
    public static final List<String> PACKAGE_NAMES = new ArrayList<String>(Arrays.asList(
        "com.google.android.dialer", "com.android.dialer"));

    private static final String CLASS_CALL_CARD_FRAGMENT = "com.android.incallui.CallCardFragment";
    private static final String CLASS_DIALTACTS_ACTIVITY = "com.android.dialer.DialtactsActivity";
    private static final String CLASS_DIALTACTS_ACTIVITY_GOOGLE = 
            "com.google.android.dialer.extensions.GoogleDialtactsActivity";
    private static final String CLASS_IN_CALL_PRESENTER = "com.android.incallui.InCallPresenter";
    private static final String ENUM_IN_CALL_STATE = "com.android.incallui.InCallPresenter$InCallState";
    private static final String CLASS_CALL_LIST = "com.android.incallui.CallList";
    private static final String CLASS_TELECOM_ADAPTER = "com.android.incallui.TelecomAdapter";
    private static final String CLASS_CALL_BUTTON_FRAGMENT = "com.android.incallui.CallButtonFragment";
    private static final String CLASS_ANSWER_FRAGMENT = "com.android.incallui.AnswerFragment";
    private static final String CLASS_DIALPAD_FRAGMENT = "com.android.dialer.dialpad.DialpadFragment";
    private static final boolean DEBUG = false;

    private static final int CALL_STATE_ACTIVE = 3;
    private static final int CALL_STATE_INCOMING = 4;
    private static final int CALL_STATE_WAITING = 5;

    private static XSharedPreferences mPrefsPhone;
    private static int mFlipAction = GravityBoxSettings.PHONE_FLIP_ACTION_NONE;
    private static Set<String> mCallVibrations;
    private static Context mContext;
    private static SensorManager mSensorManager;
    private static boolean mSensorListenerAttached = false;
    private static Object mIncomingCall;
    private static Class<?> mClassTelecomAdapter;
    private static Object mPreviousCallState;
    private static Vibrator mVibrator;
    private static Handler mHandler;
    private static WakeLock mWakeLock;
    private static Class<?> mClassInCallPresenter;
    private static QuietHours mQuietHours;

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

            try {
                switch (mFlipAction) {
                    case GravityBoxSettings.PHONE_FLIP_ACTION_MUTE:
                        if (DEBUG) log("Muting call");
                        silenceRinger();
                        break;
                    case GravityBoxSettings.PHONE_FLIP_ACTION_DISMISS:
                        if (DEBUG) log("Rejecting call");
                        rejectCall(mIncomingCall);
                        break;
                    case GravityBoxSettings.PHONE_FLIP_ACTION_NONE:
                    default:
                        // do nothing
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
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

    private static void silenceRinger() {
        try {
            TelecomManager tm = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
            XposedHelpers.callMethod(tm, "silenceRinger");
        } catch(Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void rejectCall(Object call) {
        if (call == null || 
                mClassTelecomAdapter == null) return;

        try {
            final Object callCmdClient = 
                    XposedHelpers.callStaticMethod(mClassTelecomAdapter, "getInstance");
            final String callId = (String) XposedHelpers.callMethod(call, "getId");
            if (callCmdClient != null && callId != null) {
                Class<?>[] pArgs = new Class<?>[] { String.class, boolean.class, String.class };
                XposedHelpers.callMethod(callCmdClient, "rejectCall", pArgs,
                        callId, false, null);
                if (DEBUG) log("Call rejected");
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void vibrate(int v1, int p1, int v2) {
        if (mVibrator == null) return;

        long[] pattern = new long[] { 0, v1, p1, v2 };
        mVibrator.vibrate(pattern, -1);
    }

    private static Runnable mPeriodicVibrator = new Runnable() {
        @Override
        public void run() {
            if (mWakeLock != null) {
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
                mWakeLock.acquire(61000);
                if (DEBUG) log("Partial Wake Lock timeout extended");
            }
            vibrate(50, 0, 0);
            mHandler.postDelayed(this, 60000);
        }
    };

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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void init(final XSharedPreferences prefs, ClassLoader classLoader, final String packageName) {
        mPrefsPhone = prefs;

        try {
            final Class<?> classAnswerFragment = XposedHelpers.findClass(CLASS_ANSWER_FRAGMENT, classLoader);
            final Class<?> classCallButtonFragment = XposedHelpers.findClass(CLASS_CALL_BUTTON_FRAGMENT, classLoader); 

            XposedHelpers.findAndHookMethod(classCallButtonFragment, "setEnabled", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final View view = ((Fragment)param.thisObject).getView();
                    final boolean fsc = prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_CALLER_FULLSCREEN_PHOTO, false);
                    if (fsc & view != null) {
                        view.setVisibility(!(Boolean)param.args[0] ? View.GONE : View.VISIBLE);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classAnswerFragment, "onShowAnswerUi", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!(Boolean) param.args[0]) return;

                    refreshPhonePrefs();
                    if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_CALLER_FULLSCREEN_PHOTO, false)) { 
                    final View v = ((Fragment)param.thisObject).getView();
                        v.setBackgroundColor(0);
                        if (Utils.isMtkDevice()) {
                            final View gpView = (View) XposedHelpers.getObjectField(param.thisObject, "mGlowpad");
                            gpView.setBackgroundColor(0);
                        }
                        if (DEBUG) log("AnswerFragment showAnswerUi: background color set");
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            final Class<?> classCallCardFragment = XposedHelpers.findClass(CLASS_CALL_CARD_FRAGMENT, classLoader);

            try {
                XposedHelpers.findAndHookMethod(classCallCardFragment, "setDrawableToImageView",
                        ImageView.class, Drawable.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!prefs.getBoolean(
                                GravityBoxSettings.PREF_KEY_CALLER_UNKNOWN_PHOTO_ENABLE, false)) return;

                        boolean shouldShowUnknownPhoto = param.args[1] == null;
                        if (param.args[1] != null) {
                            final Fragment frag = (Fragment) param.thisObject;
                            final Resources res = frag.getResources();
                            String resName = "img_no_image_automirrored";
                            Drawable picUnknown = res.getDrawable(res.getIdentifier(resName, "drawable",
                                            res.getResourcePackageName(frag.getId())), null);
                            shouldShowUnknownPhoto = ((Drawable)param.args[1]).getConstantState().equals(
                                                        picUnknown.getConstantState());
                        }

                        if (shouldShowUnknownPhoto) {
                            final ImageView iv = (ImageView) param.args[0];
                            final Context context = iv.getContext();
                            final String path = Utils.getGbContext(context).getFilesDir() + "/caller_photo";
                            File f = new File(path);
                            if (f.exists() && f.canRead()) {
                                Bitmap b = BitmapFactory.decodeFile(path);
                                if (b != null) {
                                    param.args[1] = new BitmapDrawable(context.getResources(), b);
                                    if (DEBUG) log("Unknow caller photo set");
                                }
                            }
                        }
                    }
                });
            } catch (Throwable t) {
                XposedHelpers.findAndHookMethod(classCallCardFragment, "setDrawableToImageViews",
                        Drawable.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!prefs.getBoolean(
                                GravityBoxSettings.PREF_KEY_CALLER_UNKNOWN_PHOTO_ENABLE, false)) return;

                        boolean shouldShowUnknownPhoto = param.args[0] == null;
                        if (param.args[0] != null) {
                            final Fragment frag = (Fragment) param.thisObject;
                            final Resources res = frag.getResources();
                            String resName = "img_no_image_automirrored";
                            Drawable picUnknown = res.getDrawable(res.getIdentifier(resName, "drawable",
                                            res.getResourcePackageName(frag.getId())), null);
                            shouldShowUnknownPhoto = ((Drawable)param.args[0]).getConstantState().equals(
                                                        picUnknown.getConstantState());
                        }

                        if (shouldShowUnknownPhoto) {
                            final View v = ((Fragment) param.thisObject).getView();
                            final Context context = v.getContext();
                            final String path = Utils.getGbContext(context).getFilesDir() + "/caller_photo";
                            File f = new File(path);
                            if (f.exists() && f.canRead()) {
                                Bitmap b = BitmapFactory.decodeFile(path);
                                if (b != null) {
                                    param.args[0] = new BitmapDrawable(context.getResources(), b);
                                    if (DEBUG) log("Unknow caller photo set");
                                }
                            }
                        }
                    }
                });
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            final Class<?> classDialtactsActivity = XposedHelpers.findClass(CLASS_DIALTACTS_ACTIVITY, classLoader);

            XposedHelpers.findAndHookMethod(classDialtactsActivity, "displayFragment", Intent.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    refreshPhonePrefs();
                    if (!prefs.getBoolean(GravityBoxSettings.PREF_KEY_DIALER_SHOW_DIALPAD, false)) return;

                    final String realClassName = param.thisObject.getClass().getName();
                    if (realClassName.equals(CLASS_DIALTACTS_ACTIVITY)) {
                        XposedHelpers.callMethod(param.thisObject, "showDialpadFragment", false);
                        if (DEBUG) log("showDialpadFragment() called within " + realClassName);
                    } else if (realClassName.equals(CLASS_DIALTACTS_ACTIVITY_GOOGLE)) {
                        final Class<?> superc = param.thisObject.getClass().getSuperclass();
                        Method m = XposedHelpers.findMethodExact(superc, "showDialpadFragment", boolean.class);
                        m.invoke(param.thisObject, false);
                        if (DEBUG) log("showDialpadFragment() called within " + realClassName);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            mClassInCallPresenter = XposedHelpers.findClass(CLASS_IN_CALL_PRESENTER, classLoader);
            final Class<? extends Enum> enumInCallState = (Class<? extends Enum>)
                    XposedHelpers.findClass(ENUM_IN_CALL_STATE, classLoader);
            mClassTelecomAdapter = XposedHelpers.findClass(CLASS_TELECOM_ADAPTER, classLoader);

            XposedBridge.hookAllMethods(mClassInCallPresenter, "setUp", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mContext = (Context) param.args[0];
                    if (mSensorManager == null) {
                        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
                        if (DEBUG) log("InCallPresenter.setUp(); mSensorManager created");
                    }
                    if (mVibrator == null) {
                        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                        if (DEBUG) log("InCallPresenter.setUp(); mVibrator created");
                    }
                    if (mHandler == null) {
                        mHandler = new Handler();
                    }
                    if (mWakeLock == null) {
                        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                        mWakeLock  = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
                    }
                    mPreviousCallState = null;
                }
            });

            XposedBridge.hookAllMethods(mClassInCallPresenter, "onIncomingCall", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Integer state = (Integer) XposedHelpers.callMethod(param.args[0], "getState");
                    if (DEBUG) log("onIncomingCall: state = " + state);
                    if (state == CALL_STATE_INCOMING) {
                        mIncomingCall = param.args[0];
                        attachSensorListener();
                    }
                    if (state == CALL_STATE_WAITING ||
                            (state == CALL_STATE_INCOMING && mPreviousCallState == 
                                Enum.valueOf(enumInCallState, "INCALL") &&
                            mCallVibrations.contains(GravityBoxSettings.CV_WAITING))) {
                        vibrate(200, 300, 500);
                    }
                }
            });

            XposedBridge.hookAllMethods(mClassInCallPresenter, "onDisconnect", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mCallVibrations.contains(GravityBoxSettings.CV_DISCONNECTED)) {
                        if (DEBUG) log("Call disconnected; executing vibrate on call disconnected");
                        vibrate(50, 100, 50);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(mClassInCallPresenter, "getPotentialStateFromCallList",
                    CLASS_CALL_LIST, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object state = param.getResult();
                    if (DEBUG) log("InCallPresenter.getPotentialStateFromCallList(); InCallState = " + state);
                    if (mPreviousCallState == null || 
                            mPreviousCallState == Enum.valueOf(enumInCallState, "NO_CALLS")) {
                        refreshPhonePrefs();
                    }

                    if (state != Enum.valueOf(enumInCallState, "INCOMING")) {
                        mIncomingCall = null;
                        detachSensorListener();
                    } 

                    if (state == Enum.valueOf(enumInCallState, "INCALL")) {
                        Object activeCall = XposedHelpers.callMethod(param.args[0], "getActiveCall");
                        if (activeCall != null) {
                            final int callState = (Integer) XposedHelpers.callMethod(activeCall, "getState");
                            if (DEBUG) log("Call state is: " + callState);
                            final boolean activeOutgoing = (callState == CALL_STATE_ACTIVE &&
                                    mPreviousCallState == Enum.valueOf(enumInCallState, "OUTGOING"));
                            if (activeOutgoing) {
                                if (mCallVibrations.contains(GravityBoxSettings.CV_CONNECTED)) {
                                    if (DEBUG) log("Outgoing call connected; executing vibrate on call connected");
                                    vibrate(100, 0, 0);
                                }
                                if (mCallVibrations.contains(GravityBoxSettings.CV_PERIODIC) &&
                                        mHandler != null) {
                                    if (DEBUG) log("Outgoing call connected; starting periodic vibrations");
                                    mHandler.postDelayed(mPeriodicVibrator, 45000);
                                    if (mWakeLock != null) {
                                        mWakeLock.acquire(46000);
                                        if (DEBUG) log("Partial Wake Lock acquired");
                                    }
                                }
                            }
                        }
                    } else if (state == Enum.valueOf(enumInCallState, "NO_CALLS")) {
                        if (mHandler != null) {
                            mHandler.removeCallbacks(mPeriodicVibrator);
                        }
                        if (mWakeLock != null && mWakeLock.isHeld()) {
                            mWakeLock.release();
                            if (DEBUG) log("Partial Wake Lock released");
                        }
                    }

                    mPreviousCallState = state;
                }
            });
        } catch(Throwable t) {
            XposedBridge.log(t);
        }

        try {
            XposedHelpers.findAndHookMethod(CLASS_DIALPAD_FRAGMENT, classLoader, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param2) throws Throwable {
                    XSharedPreferences qhPrefs = new XSharedPreferences(GravityBox.PACKAGE_NAME, "quiet_hours");
                    mQuietHours = new QuietHours(qhPrefs);
                }
            });

            XposedHelpers.findAndHookMethod(CLASS_DIALPAD_FRAGMENT, classLoader, "playTone",
                    int.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (mQuietHours.isSystemSoundMuted(QuietHours.SystemSound.DIALPAD)) {
                        param.setResult(null);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
