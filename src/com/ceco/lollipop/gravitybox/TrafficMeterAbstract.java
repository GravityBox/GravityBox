/*
 * Copyright (C) 2015 Peter Gregus for GravityBox Project (C3C076@xda)
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.ceco.lollipop.gravitybox.ProgressBarController.Mode;
import com.ceco.lollipop.gravitybox.ProgressBarController.ProgressInfo;
import com.ceco.lollipop.gravitybox.managers.StatusBarIconManager;
import com.ceco.lollipop.gravitybox.managers.StatusBarIconManager.ColorInfo;
import com.ceco.lollipop.gravitybox.managers.StatusBarIconManager.IconManagerListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public abstract class TrafficMeterAbstract extends TextView 
                        implements BroadcastSubReceiver, IconManagerListener,
                                   ProgressBarController.ProgressStateListener {
    protected static final String PACKAGE_NAME = "com.android.systemui";
    protected static final String TAG = "GB:NetworkTraffic";
    protected static final boolean DEBUG = false;

    public static enum TrafficMeterMode { OFF, SIMPLE, OMNI };
    public static enum DisplayMode { ALWAYS, DOWNLOAD_MANAGER, PROGRESS_TRACKING };

    protected Context mGbContext;
    protected boolean mAttached;
    protected int mInterval = 1000;
    protected int mPosition;
    protected int mSize;
    protected int mMargin;
    protected boolean mIsScreenOn = true;
    protected DisplayMode mDisplayMode;
    protected boolean mIsDownloadActive;
    private PhoneStateListener mPhoneStateListener;
    private TelephonyManager mPhone;
    protected boolean mMobileDataConnected;
    protected boolean mShowOnlyForMobileData;
    protected boolean mIsTrackingProgress;
    protected boolean mAllowInLockscreen;
    protected boolean mCanReadFromFile;

    protected static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static TrafficMeterAbstract create(Context context, TrafficMeterMode mode) {
        if (mode == TrafficMeterMode.SIMPLE) {
            return new TrafficMeter(context);
        } else if (mode == TrafficMeterMode.OMNI) {
            return new TrafficMeterOmni(context);
        } else {
            throw new IllegalArgumentException("Invalid traffic meter mode supplied");
        }
    }

    protected TrafficMeterAbstract(Context context) {
        super(context);

        LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        mMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                context.getResources().getDisplayMetrics());
        lParams.setMarginStart(mMargin);
        lParams.setMarginEnd(mMargin);
        setLayoutParams(lParams);
        setTextAppearance(context, context.getResources().getIdentifier(
                "TextAppearance.StatusBar.Clock", "style", PACKAGE_NAME));
        setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        if (!Utils.isWifiOnly(getContext())) {
            mPhone = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onDataConnectionStateChanged(int state, int networkType) {
                    final boolean connected = state == TelephonyManager.DATA_CONNECTED;
                    if (mMobileDataConnected != connected) {
                        mMobileDataConnected = connected;
                        if (DEBUG) log("onDataConnectionStateChanged: mMobileDataConnected=" + mMobileDataConnected);
                        updateState();
                    }
                    
                }
            };
        }
        
        mCanReadFromFile = canReadFromFile();
    }

    public void initialize(XSharedPreferences prefs) throws Throwable {
        prefs.reload();
        try {
            mSize = Integer.valueOf(prefs.getString(
                    GravityBoxSettings.PREF_KEY_DATA_TRAFFIC_SIZE, "14"));
        } catch (NumberFormatException nfe) {
            log("Invalid preference value for PREF_KEY_DATA_TRAFFIC_SIZE");
        }

        try {
            mPosition = Integer.valueOf(prefs.getString(
                    GravityBoxSettings.PREF_KEY_DATA_TRAFFIC_POSITION, "0"));
        } catch (NumberFormatException nfe) {
            log("Invalid preference value for PREF_KEY_DATA_TRAFFIC_POSITION");
        }

        mDisplayMode = DisplayMode.valueOf(prefs.getString(
                GravityBoxSettings.PREF_KEY_DATA_TRAFFIC_DISPLAY_MODE, "ALWAYS"));

        if (mPhone != null) {
            mShowOnlyForMobileData = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_DATA_TRAFFIC_ACTIVE_MOBILE_ONLY, false);
        }

        mAllowInLockscreen = prefs.getBoolean(
                GravityBoxSettings.PREF_KEY_DATA_TRAFFIC_LOCKSCREEN, true);

        onInitialize(prefs);
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                updateState();
            } else if (ModDownloadProvider.ACTION_DOWNLOAD_STATE_CHANGED.equals(action)
                    && intent.hasExtra(ModDownloadProvider.EXTRA_ACTIVE)) {
                mIsDownloadActive = intent.getBooleanExtra(ModDownloadProvider.EXTRA_ACTIVE, false);
                if (DEBUG) log("ACTION_DOWNLOAD_STATE_CHANGED; active=" + mIsDownloadActive);
                if (mDisplayMode == DisplayMode.DOWNLOAD_MANAGER) {
                    updateState();
                }
            }
        }
    };

    protected boolean getConnectAvailable() {
        ConnectivityManager connManager =
                (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = (connManager != null) ? connManager.getActiveNetworkInfo() : null;
        return network != null && network.isConnected();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            if (DEBUG) log("attached to window");
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(ModDownloadProvider.ACTION_DOWNLOAD_STATE_CHANGED);
            getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
 
            if (mPhone != null) {
                mPhone.listen(mPhoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
            }

            updateState();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mAttached = false;
            if (DEBUG) log("detached from window");
            getContext().unregisterReceiver(mIntentReceiver);

            if (mPhone != null) {
                mPhone.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            }

            updateState();
        }
    }

    public int getTrafficMeterPosition() {
        return mPosition;
    }

    public boolean isAllowedInLockscreen() {
        return mAllowInLockscreen;
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(GravityBoxSettings.ACTION_PREF_DATA_TRAFFIC_CHANGED)) {
            if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_MODE)) {
                return;
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_POSITION)) {
                mPosition = intent.getIntExtra(GravityBoxSettings.EXTRA_DT_POSITION,
                        GravityBoxSettings.DT_POSITION_AUTO);
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_SIZE)) {
                mSize = intent.getIntExtra(GravityBoxSettings.EXTRA_DT_SIZE, 14);
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_DISPLAY_MODE)) {
                mDisplayMode = DisplayMode.valueOf(intent.getStringExtra(
                        GravityBoxSettings.EXTRA_DT_DISPLAY_MODE));
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_ACTIVE_MOBILE_ONLY)) {
                mShowOnlyForMobileData = intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_DT_ACTIVE_MOBILE_ONLY, false);
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_LOCKSCREEN)) {
                mAllowInLockscreen = intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_DT_LOCKSCREEN, false);
            }

            onPreferenceChanged(intent);
            updateState();
        } else if (action.equals(Intent.ACTION_SCREEN_ON) ||
                action.equals(Intent.ACTION_SCREEN_OFF)) {
            mIsScreenOn = action.equals(Intent.ACTION_SCREEN_ON);
            updateState();
        }
    }

    @Override
    public void onIconManagerStatusChanged(int flags, ColorInfo colorInfo) {
        if ((flags & StatusBarIconManager.FLAG_ICON_COLOR_CHANGED) != 0) {
            setTextColor(colorInfo.coloringEnabled ?
                    colorInfo.iconColor[0] : colorInfo.defaultIconColor);
        } else if ((flags & StatusBarIconManager.FLAG_ICON_ALPHA_CHANGED) != 0) {
            setAlpha(colorInfo.alphaSignalCluster);
        }
    }

    private boolean shoudStartTrafficUpdates() {
        boolean shouldStart = mAttached && mIsScreenOn && getConnectAvailable();
        if (mDisplayMode == DisplayMode.DOWNLOAD_MANAGER) {
            shouldStart &= mIsDownloadActive;
        } else if (mDisplayMode == DisplayMode.PROGRESS_TRACKING) {
            shouldStart &= mIsTrackingProgress;
        }
        if (mShowOnlyForMobileData) {
            shouldStart &= mMobileDataConnected;
        }
        return shouldStart;
    }

    protected void updateState() {
        if (shoudStartTrafficUpdates()) {
            startTrafficUpdates();
            setVisibility(View.VISIBLE);
            if (DEBUG) log("traffic updates started");
        } else {
            stopTrafficUpdates();
            setVisibility(View.GONE);
            setText("");
            if (DEBUG) log("traffic updates stopped");
        }
    }

    @Override
    public void onProgressTrackingStarted(boolean isBluetooth,
            ProgressBarController.Mode mode) {
        mIsTrackingProgress = !isBluetooth;
        if (mDisplayMode == DisplayMode.PROGRESS_TRACKING) {
            updateState();
        }
    }

    @Override
    public void onProgressTrackingStopped() {
        mIsTrackingProgress = false;
        if (mDisplayMode == DisplayMode.PROGRESS_TRACKING) {
            updateState();
        }
    }

    @Override
    public void onProgressUpdated(ProgressInfo pInfo) { }

    @Override
    public void onModeChanged(Mode mode) { }

    @Override
    public void onPreferencesChanged(Intent intent) { }

    protected abstract void onInitialize(XSharedPreferences prefs) throws Throwable;
    protected abstract void onPreferenceChanged(Intent intent);
    protected abstract void startTrafficUpdates();
    protected abstract void stopTrafficUpdates();
	
    protected static boolean isCountedInterface(String iface) {
        return (iface != "lo") && (!iface.startsWith("tun"));
    }
    
    private static long tryParseLong(String obj) {
        try {
            return Long.parseLong(obj);
        } catch (Exception e) {
            return 0;
        }
    }
    
    protected static long[] getTotalRxTxBytes() {
        String line;
        String[] segs;
        String iface;
        long rxBytes = 0;
        long txBytes = 0;
        try {
            FileReader fr = new FileReader("/proc/net/dev");
            BufferedReader in = new BufferedReader(fr);
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.contains(":")) {
                    segs = line.split(":");
                    iface = segs[0];
                    if (isCountedInterface(iface)) {
                        segs = segs[1].split(" ");
                        rxBytes += tryParseLong(segs[0]);
                        txBytes += tryParseLong(segs[8]);
                    }
                }
            }
            in.close();
        } catch (IOException e) {
            return new long[]{0, 0};
        }
        return new long[]{rxBytes, txBytes};
    }

    private boolean canReadFromFile() {
        return new File("/proc/net/dev").exists();
    }
}
