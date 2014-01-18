/*
 * Copyright (C) 2013 CyanKang Project
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
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

package com.ceco.kitkat.gravitybox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

import com.ceco.kitkat.gravitybox.R;
import com.ceco.kitkat.gravitybox.StatusBarIconManager.ColorInfo;
import com.ceco.kitkat.gravitybox.StatusBarIconManager.IconManagerListener;

import de.robv.android.xposed.XposedBridge;

public class TrafficMeter extends TextView implements IconManagerListener {
    public static final String TAG = "GB:TrafficMeter";
    private static final boolean DEBUG = false;

    public static final int INACTIVITY_MODE_DEFAULT = 0;
    public static final int INACTIVITY_MODE_HIDDEN = 1;
    public static final int INACTIVITY_MODE_SUMMARY = 2;

    Context mContext;
    boolean mAttached;
    boolean mTrafficMeterEnable;
    boolean mTrafficMeterHide = false;
    int mTrafficMeterSummaryTime = 0;
    long mTotalRxBytes;
    long mLastUpdateTime;
    long mTrafficBurstStartTime;
    long mTrafficBurstStartBytes;
    long mKeepOnUntil = Long.MIN_VALUE;
    int mPosition = GravityBoxSettings.DT_POSITION_AUTO;
    String mB = "B";
    String mKB = "KB";
    String mMB = "MB";
    String mS = "s";

    NumberFormat mDecimalFormat = new DecimalFormat("##0.0");
    NumberFormat mIntegerFormat = NumberFormat.getIntegerInstance();

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public TrafficMeter(Context context) {
        this(context, null);
    }

    public TrafficMeter(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrafficMeter(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;

        try {
            Context gbContext = mContext.createPackageContext(
                    GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            mB = gbContext.getString(R.string.byte_abbr);
            mKB = gbContext.getString(R.string.kilobyte_abbr);
            mMB = gbContext.getString(R.string.megabyte_abbr);
            mS = gbContext.getString(R.string.second_abbr);
        } catch (Exception e) {
            log(e.getMessage());
        }

        updateState();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            getContext().registerReceiver(mIntentReceiver, filter, null,
                    getHandler());
            if (DEBUG) log("attached to window");
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            stopTrafficUpdates();
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
            if (DEBUG) log("detached from window");
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                updateState();
            }
        }
    };

    @Override
    public void onScreenStateChanged(int screenState) {
        if (screenState == SCREEN_STATE_OFF) {
            stopTrafficUpdates();
        } else {
            startTrafficUpdates();
        }
        super.onScreenStateChanged(screenState);
    }

    private void stopTrafficUpdates() {
        if (mAttached) {
            getHandler().removeCallbacks(mRunnable);
            setText("");
            if (DEBUG) log("traffic updates stopped");
        }
    }

    public void startTrafficUpdates() {
        if (mAttached && getConnectAvailable()) {
            mTotalRxBytes = TrafficStats.getTotalRxBytes();
            if (mTotalRxBytes == 0) {
                // On some old devices, the traffic status will stick on 0B/s
                // So let's check by another way
                mTotalRxBytes = getTotalReceivedBytes();
			}
            mLastUpdateTime = SystemClock.elapsedRealtime();
            mTrafficBurstStartTime = Long.MIN_VALUE;

            getHandler().removeCallbacks(mRunnable);
            getHandler().post(mRunnable);
            if (DEBUG) log("traffic updates started");
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

    private boolean getConnectAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) mContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            return connectivityManager.getActiveNetworkInfo().isConnected();
        } catch (Exception ignored) {
        }
        return false;
    }

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            long td = SystemClock.elapsedRealtime() - mLastUpdateTime;

            if (!mTrafficMeterEnable) {
                return;
            }

            long currentRxBytes = TrafficStats.getTotalRxBytes();
            long newBytes = currentRxBytes - mTotalRxBytes;

            if (newBytes <= 0) {
                // On some old devices, the traffic status will stick on 0B/s
                // So let's check by another way
                currentRxBytes = getTotalReceivedBytes();
                newBytes = currentRxBytes - mTotalRxBytes;
            }

            if (newBytes < 0) {
                currentRxBytes = 0;
                newBytes = 0;
            }

            if (mTrafficMeterHide && newBytes == 0) {
                long trafficBurstBytes = currentRxBytes - mTrafficBurstStartBytes;

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

            mTotalRxBytes = currentRxBytes;
            mLastUpdateTime = SystemClock.elapsedRealtime();
            getHandler().postDelayed(mRunnable, 1000);
        }
    };

    private void updateState() {
        if (DEBUG) log("updating state");

        if (mTrafficMeterEnable && getConnectAvailable()) {
            setVisibility(View.VISIBLE);
            startTrafficUpdates();
        } else {
            stopTrafficUpdates();
            setVisibility(View.GONE);
            setText("");
        }
    }

	private long getTotalReceivedBytes() {
        String line;
        String[] segs;
        long received = 0;
        int i;
        long tmp = 0;
        boolean isNum;
        try {
            FileReader fr = new FileReader("/proc/net/dev");
            BufferedReader in = new BufferedReader(fr, 500);
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("rmnet") || line.startsWith("eth") || line.startsWith("wlan")) {
                    segs = line.split(":")[1].split(" ");
                    for (i = 0; i < segs.length; i++) {
                        isNum = true;
                        try {
                            tmp = Long.parseLong(segs[i]);
                        } catch (Exception e) {
                            isNum = false;
                        }
                        if (isNum == true) {
                            received = received + tmp;
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            return -1;
        }
        return received;
    }
	
    public void setTrafficMeterEnabled(boolean enabled) {
        mTrafficMeterEnable = enabled;
        updateState();
    }

    public void setTrafficMeterPosition(int position) {
        mPosition = position;
    }

    public int getTrafficMeterPosition() {
        return mPosition;
    }

    public void setInactivityMode(int mode) {
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

    @Override
    public void onIconManagerStatusChanged(int flags, ColorInfo colorInfo) {
        if ((flags & StatusBarIconManager.FLAG_ICON_COLOR_CHANGED) != 0) {
            setTextColor(colorInfo.coloringEnabled ?
                    colorInfo.iconColor[0] : colorInfo.defaultIconColor);
        } else if ((flags & StatusBarIconManager.FLAG_LOW_PROFILE_CHANGED) != 0) {
            setAlpha(colorInfo.lowProfile ? 0 : 1);
        } else if ((flags & StatusBarIconManager.FLAG_ICON_ALPHA_CHANGED) != 0) {
            setAlpha(colorInfo.alphaTextAndBattery);
        }
    }
}
