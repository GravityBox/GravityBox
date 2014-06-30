/*
 * Copyright (C) 2014 Peter Gregus for GravityBox Project (C3C076@xda)
 *
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

import com.ceco.kitkat.gravitybox.preference.IncreasingRingPreference;
import com.ceco.kitkat.gravitybox.preference.IncreasingRingPreference.ConfigStore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModRinger {
    public static final String PACKAGE_NAME = "com.android.phone";
    private static final String TAG = "GB:ModRinger";
    private static final boolean DEBUG = false;

    private static final String CLASS_RINGER = "com.android.phone.Ringer";
    private static final int PLAY_RING_ONCE = 1;
    private static final int INCREASE_RING_VOLUME = 4;

    private static Handler mHandler;
    private static int mRingerVolume = -1;
    private static ConfigStore mRingerConfig;
    private static AudioManager mAm;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
           if (intent.getAction().equals(IncreasingRingPreference.ACTION_INCREASING_RING_CHANGED) &&
                   intent.getIntExtra(IncreasingRingPreference.EXTRA_STREAM_TYPE, -1) ==
                       AudioManager.STREAM_RING) {
               mRingerConfig.enabled = intent.getBooleanExtra(
                       IncreasingRingPreference.EXTRA_ENABLED, false);
               mRingerConfig.minVolume = intent.getIntExtra(
                       IncreasingRingPreference.EXTRA_MIN_VOLUME, 1);
               mRingerConfig.interval = intent.getIntExtra(
                       IncreasingRingPreference.EXTRA_INTERVAL, 0);
               if (DEBUG) log(mRingerConfig.toString());
           }
        }
    };

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> clsRinger = XposedHelpers.findClass(CLASS_RINGER, classLoader);

            mRingerConfig = new ConfigStore(prefs.getString(
                    GravityBoxSettings.PREF_KEY_INCREASING_RING, null));
            if (DEBUG) log(mRingerConfig.toString());

            XposedBridge.hookAllConstructors(clsRinger, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args.length > 0 && param.args[0] instanceof Context) {
                        Context context = (Context) param.args[0];
                        mAm = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(IncreasingRingPreference.ACTION_INCREASING_RING_CHANGED);
                        context.registerReceiver(mBroadcastReceiver, intentFilter);
                        if (DEBUG) log("Ringer created; broadcast receiver registered");
                    }
                }
            });

            XposedHelpers.findAndHookMethod(clsRinger, "ring", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!mRingerConfig.enabled) return;

                    if (mHandler == null) {
                        mHandler = new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                if (msg.what == INCREASE_RING_VOLUME) {
                                    int ringerVolume = mAm.getStreamVolume(AudioManager.STREAM_RING);
                                    if (mRingerVolume > 0 && ringerVolume < mRingerVolume) {
                                        ringerVolume++;
                                        if (DEBUG) log("increasing ring volume to " +
                                                       ringerVolume + "/" + mRingerVolume);
                                        mAm.setStreamVolume(AudioManager.STREAM_RING,
                                                ringerVolume, 0);
                                        if (mRingerConfig.interval > 0) {
                                            sendEmptyMessageDelayed(INCREASE_RING_VOLUME,
                                                    mRingerConfig.interval);
                                        }
                                    }
                                }
                            }
                        };
                    }

                    long firstRingEventTime = XposedHelpers.getLongField(param.thisObject, "mFirstRingEventTime");
                    if (firstRingEventTime < 0) {
                        int ringerVolume = mAm.getStreamVolume(AudioManager.STREAM_RING);
                        if (mRingerConfig.minVolume < ringerVolume) {
                            mRingerVolume = ringerVolume;
                            mAm.setStreamVolume(AudioManager.STREAM_RING, mRingerConfig.minVolume, 0);
                            if (DEBUG) log("increasing ring is enabled, starting at " +
                                    mRingerConfig.minVolume + "/" + ringerVolume);
                            if (mRingerConfig.interval > 0) {
                                mHandler.sendEmptyMessageDelayed(INCREASE_RING_VOLUME, 
                                        mRingerConfig.interval);
                            }
                            if (mRingerConfig.minVolume == 0) {
                                XposedHelpers.callMethod(param.thisObject, "makeLooper");
                                XposedHelpers.setLongField(param.thisObject, "mFirstRingEventTime",
                                        SystemClock.elapsedRealtime());
                                ((Handler) XposedHelpers.getObjectField(param.thisObject, "mRingHandler"))
                                    .sendEmptyMessage(PLAY_RING_ONCE);
                            }
                        } else {
                            mRingerVolume = -1;
                        }
                    } else {
                        long firstRingStartTime = XposedHelpers.getLongField(param.thisObject, "mFirstRingStartTime");
                        if (firstRingStartTime > 0) {
                            if (mRingerVolume > 0 && mRingerConfig.interval == 0) {
                                mHandler.sendEmptyMessage(INCREASE_RING_VOLUME);
                            }
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(clsRinger, "stopRing", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!mRingerConfig.enabled) return;

                    if (mHandler != null) {
                        mHandler.removeCallbacksAndMessages(null);
                        mHandler = null;
                    }
                    if (mRingerVolume >= 0) {
                        if (DEBUG) log("stopRing: resetting ring volume to " + mRingerVolume);
                        mAm.setStreamVolume(AudioManager.STREAM_RING, mRingerVolume, 0);
                        mRingerVolume = -1;
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
