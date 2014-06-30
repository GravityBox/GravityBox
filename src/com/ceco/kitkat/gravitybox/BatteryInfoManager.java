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

import com.ceco.kitkat.gravitybox.ledcontrol.QuietHours;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.telephony.TelephonyManager;

public class BatteryInfoManager {
    private BatteryData mBatteryData;
    private ArrayList<BatteryStatusListener> mListeners;
    private Context mContext;
    private Context mGbContext;
    private boolean mChargedSoundEnabled;
    private boolean mPluggedSoundEnabled;
    private TelephonyManager mTelephonyManager;

    class BatteryData {
        boolean charging;
        int level;
        int powerSource;
        int temperature;
        int voltage;

        public float getTempCelsius() {
            return ((float)temperature/10f);
        }

        public float getTempFahrenheit() {
            return (((float)temperature/10f)*(9f/5f)+32);
        }
    }

    public interface BatteryStatusListener {
        void onBatteryStatusChanged(BatteryData batteryData);
    }

    public BatteryInfoManager(Context context, Context gbContext) {
        mContext = context;
        mGbContext = gbContext;
        mBatteryData = new BatteryData();
        mListeners = new ArrayList<BatteryStatusListener>();
        mChargedSoundEnabled = false;
        mPluggedSoundEnabled = false;
    }

    public void registerListener(BatteryStatusListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
            listener.onBatteryStatusChanged(mBatteryData);
        }
    }

    private void notifyListeners() {
        for (BatteryStatusListener listener : mListeners) {
            listener.onBatteryStatusChanged(mBatteryData);
        }
    }

    public void updateBatteryInfo(Intent intent) {
        if (intent == null) return;

        int newLevel = (int)(100f
                * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
        int newPowerSource = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean newCharging = newPowerSource != 0;
        int newTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        int newVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);

        if (mBatteryData.level != newLevel || mBatteryData.charging != newCharging ||
                mBatteryData.powerSource != newPowerSource ||
                mBatteryData.temperature != newTemp || 
                mBatteryData.voltage != newVoltage) {
            if (mChargedSoundEnabled && newLevel == 100 && mBatteryData.level == 99) {
                playSound("battery_charged.ogg");
            }

            if (mPluggedSoundEnabled && mBatteryData.powerSource != newPowerSource) {
                if (newPowerSource == 0) {
                    playSound("charger_unplugged.ogg");
                } else if (newPowerSource != BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                    playSound("charger_plugged.ogg");
                }
            }

            mBatteryData.level = newLevel;
            mBatteryData.charging = newCharging;
            mBatteryData.powerSource = newPowerSource;
            mBatteryData.temperature = newTemp;
            mBatteryData.voltage = newVoltage;

            notifyListeners();
        }
    }

    public BatteryData getCurrentBatteryData() {
        return mBatteryData;
    }

    public void setChargedSoundEnabled(boolean enabled) {
        mChargedSoundEnabled = enabled;
    }

    public void setPluggedSoundEnabled(boolean enabled) {
        mPluggedSoundEnabled = enabled;
    }

    private void playSound(String fileName) {
        if (!isPhoneIdle() || quietHoursActive()) return;
        try {
            final String filePath = mGbContext.getFilesDir().getAbsolutePath() + "/" + fileName;
            final Uri soundUri = Uri.parse("file://" + filePath);
            if (soundUri != null) {
                final Ringtone sfx = RingtoneManager.getRingtone(mGbContext, soundUri);
                if (sfx != null) {
                    sfx.setStreamType(AudioManager.STREAM_NOTIFICATION);
                    sfx.play();
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private boolean isPhoneIdle() {
        if (Utils.isWifiOnly(mContext)) return true;
        try {
            if (mTelephonyManager == null) {
                mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            }
            return (mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE);
        } catch (Throwable t) {
            XposedBridge.log(t);
            return true;
        }
    }

    private boolean quietHoursActive() {
        QuietHours qh = new QuietHours(
                new XSharedPreferences(GravityBox.PACKAGE_NAME, "ledcontrol"));
        return qh.quietHoursActive();
    }
}
