/*
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

package com.ceco.kitkat.gravitybox;

import com.ceco.kitkat.gravitybox.StatusBarIconManager.ColorInfo;
import com.ceco.kitkat.gravitybox.StatusBarIconManager.IconManagerListener;

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
                        implements BroadcastSubReceiver, IconManagerListener {
    protected static final String PACKAGE_NAME = "com.android.systemui";
    protected static final String TAG = "GB:NetworkTraffic";
    protected static final boolean DEBUG = false;

    public static enum TrafficMeterMode { OFF, SIMPLE, OMNI };

    protected Context mGbContext;
    protected boolean mAttached;
    protected int mInterval = 1000;
    protected int mPosition;
    protected int mSize;
    protected int mMargin;
    protected boolean mIsScreenOn = true;
    protected boolean mShowOnlyWhenDownloadActive;
    protected boolean mIsDownloadActive;
    private PhoneStateListener mPhoneStateListener;
    private TelephonyManager mPhone;
    protected boolean mMobileDataConnected;
    protected boolean mShowOnlyForMobileData;

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
        setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

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
    }

    public void initialize(XSharedPreferences prefs) {
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

        mShowOnlyWhenDownloadActive = prefs.getBoolean(
                GravityBoxSettings.PREF_KEY_DATA_TRAFFIC_ACTIVE_DL_ONLY, false);

        if (mPhone != null) {
            mShowOnlyForMobileData = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_DATA_TRAFFIC_ACTIVE_MOBILE_ONLY, false);
        }

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
                if (mShowOnlyWhenDownloadActive) {
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

    @Override
    public void onScreenStateChanged(int screenState) {
        mIsScreenOn = screenState == View.SCREEN_STATE_ON;
        updateState();
        super.onScreenStateChanged(screenState);
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_DATA_TRAFFIC_CHANGED)) {
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
            if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_ACTIVE_DL_ONLY)) {
                mShowOnlyWhenDownloadActive = intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_DT_ACTIVE_DL_ONLY, false);
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_ACTIVE_MOBILE_ONLY)) {
                mShowOnlyForMobileData = intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_DT_ACTIVE_MOBILE_ONLY, false);
            }

            onPreferenceChanged(intent);
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
        if (mShowOnlyWhenDownloadActive) {
            shouldStart &= mIsDownloadActive;
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

    protected abstract void onInitialize(XSharedPreferences prefs);
    protected abstract void onPreferenceChanged(Intent intent);
    protected abstract void startTrafficUpdates();
    protected abstract void stopTrafficUpdates();
}
