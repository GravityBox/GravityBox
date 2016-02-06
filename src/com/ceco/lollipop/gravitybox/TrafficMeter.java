/*
 * Copyright (C) 2013 CyanKang Project
 * Copyright (C) 2014 Peter Gregus for GravityBox Project (C3C076@xda)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.lollipop.gravitybox;

import android.content.Context;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.View;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.ceco.lollipop.gravitybox.R;

import de.robv.android.xposed.XSharedPreferences;

public class TrafficMeter extends TrafficMeterAbstract {
    public static final int INACTIVITY_MODE_DEFAULT = 0;
    public static final int INACTIVITY_MODE_HIDDEN = 1;
    public static final int INACTIVITY_MODE_SUMMARY = 2;

    boolean mTrafficMeterHide;
    int mTrafficMeterSummaryTime;
    long mTotalRxBytes;
    long mLastUpdateTime;
    long mTrafficBurstStartTime;
    long mTrafficBurstStartBytes;
    long mKeepOnUntil = Long.MIN_VALUE;
    String mB = "B";
    String mKB = "KB";
    String mMB = "MB";
    String mS = "s";

    NumberFormat mDecimalFormat = new DecimalFormat("##0.0");
    NumberFormat mIntegerFormat = NumberFormat.getIntegerInstance();

    public TrafficMeter(Context context) {
        super(context);
    }

    @Override
    protected void onInitialize(XSharedPreferences prefs) throws Throwable {
        Context gbContext = Utils.getGbContext(getContext());
        mB = gbContext.getString(R.string.byte_abbr);
        mKB = gbContext.getString(R.string.kilobyte_abbr);
        mMB = gbContext.getString(R.string.megabyte_abbr);
        mS = gbContext.getString(R.string.second_abbr);

        try {
            int inactivityMode = Integer.valueOf(prefs.getString(
                    GravityBoxSettings.PREF_KEY_DATA_TRAFFIC_INACTIVITY_MODE, "0"));
            setInactivityMode(inactivityMode);
        } catch (NumberFormatException nfe) {
            log("Invalid preference value for PREF_KEY_DATA_TRAFFIC_INACTIVITY_MODE");
        }

        setTextSize(TypedValue.COMPLEX_UNIT_DIP, mSize);
    }

    @Override
    protected void onPreferenceChanged(Intent intent) {
        if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_SIZE)) {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, mSize);
        }
        if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_INACTIVITY_MODE)) {
            setInactivityMode(intent.getIntExtra(
                    GravityBoxSettings.EXTRA_DT_INACTIVITY_MODE, 0));
        }
    }

    @Override
    protected void startTrafficUpdates() {
        mTotalRxBytes = getTotalReceivedBytes();
        mLastUpdateTime = SystemClock.elapsedRealtime();
        mTrafficBurstStartTime = Long.MIN_VALUE;

        getHandler().removeCallbacks(mRunnable);
        getHandler().post(mRunnable);
    }

    @Override
    protected void stopTrafficUpdates() {
        final Handler h = getHandler();
        if (h != null && mRunnable != null) {
            h.removeCallbacks(mRunnable);
        }
    }

    private String formatTraffic(long bytes, boolean speed) {
        if (bytes > 10485760) { // 1024 * 1024 * 10
            return (speed ? "" : "(")
                    + mIntegerFormat.format(bytes / 1048576)
                    + (speed ? mMB + "/" + mS : mMB + ")");
        } else if (bytes > 1048576) { // 1024 * 1024
            return (speed ? "" : "(")
                    + mDecimalFormat.format(((float) bytes) / 1048576f)
                    + (speed ? mMB + "/" + mS : mMB + ")");
        } else if (bytes > 10240) { // 1024 * 10
            return (speed ? "" : "(")
                    + mIntegerFormat.format(bytes / 1024)
                    + (speed ? mKB + "/" + mS : mKB + ")");
        } else if (bytes > 1024) { // 1024
            return (speed ? "" : "(")
                    + mDecimalFormat.format(((float) bytes) / 1024f)
                    + (speed ? mKB + "/" + mS : mKB + ")");
        } else {
            return (speed ? "" : "(")
                    + mIntegerFormat.format(bytes)
                    + (speed ? mB + "/" + mS : mB + ")");
        }
    }

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            long td = SystemClock.elapsedRealtime() - mLastUpdateTime;

            if (!mAttached) {
                return;
            }

            long currentRxBytes = mCanReadFromFile ? getTotalReceivedBytes() : TrafficStats.getTotalRxBytes();
            long newBytes = currentRxBytes - mTotalRxBytes;

            boolean disconnected = false;
            if (mCanReadFromFile && newBytes < 0) {
                // It's impossible to get a speed under 0
                currentRxBytes = 0;
                newBytes = 0;
                disconnected = true;
            }

            if (mTrafficMeterHide && newBytes == 0) {
                long trafficBurstBytes = (mCanReadFromFile && disconnected) ? mTotalRxBytes - mTrafficBurstStartBytes : currentRxBytes - mTrafficBurstStartBytes;

                if (trafficBurstBytes != 0 && mTrafficMeterSummaryTime != 0) {
                    setText(formatTraffic(trafficBurstBytes, false));

                    if (DEBUG) log("Traffic burst ended: " + trafficBurstBytes + "B in "
                                    + (SystemClock.elapsedRealtime() - mTrafficBurstStartTime)
                                    / 1000 + "s");
                    mKeepOnUntil = SystemClock.elapsedRealtime() + mTrafficMeterSummaryTime;
                    mTrafficBurstStartTime = Long.MIN_VALUE;
                    mTrafficBurstStartBytes = currentRxBytes;
                }
            } else {
                if (mTrafficMeterHide && mTrafficBurstStartTime == Long.MIN_VALUE) {
                    mTrafficBurstStartTime = mLastUpdateTime;
                    mTrafficBurstStartBytes = mTotalRxBytes;
                }
                if (td > 0) {
                    setText(formatTraffic(newBytes * 1000 / td, true));
                }
            }

            // Hide if there is no traffic
            if (mTrafficMeterHide && newBytes == 0) {
                if (getVisibility() != GONE
                        && mKeepOnUntil < SystemClock.elapsedRealtime()) {
                    setText("");
                    setVisibility(View.GONE);
                }
            } else {
                if (getVisibility() != VISIBLE) {
                    setVisibility(View.VISIBLE);
                }
            }

            mTotalRxBytes = (mCanReadFromFile && disconnected) ? mTotalRxBytes : currentRxBytes;
            mLastUpdateTime = SystemClock.elapsedRealtime();
            getHandler().postDelayed(mRunnable, mInterval);
        }
    };

    private long getTotalReceivedBytes() {
        return getTotalRxTxBytes()[0];
    }

    private void setInactivityMode(int mode) {
        switch (mode) {
            case INACTIVITY_MODE_HIDDEN:
                mTrafficMeterHide = true;
                mTrafficMeterSummaryTime = 0;
                break;
            case INACTIVITY_MODE_SUMMARY:
                mTrafficMeterHide = true;
                mTrafficMeterSummaryTime = 3000;
                break;
            case INACTIVITY_MODE_DEFAULT:
            default:
                mTrafficMeterHide = false;
                mTrafficMeterSummaryTime = 0;
                break;
        }
    }
}
