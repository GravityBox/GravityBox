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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import com.ceco.kitkat.gravitybox.StatusBarIconManager.ColorInfo;
import com.ceco.kitkat.gravitybox.StatusBarIconManager.IconManagerListener;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class StatusbarSignalCluster implements BroadcastSubReceiver, IconManagerListener {
    public static final String TAG = "GB:StatusbarSignalCluster";
    private static final boolean DEBUG = false;

    protected static XSharedPreferences sPrefs;

    protected LinearLayout mView;
    protected StatusBarIconManager mIconManager;
    protected Resources mResources;
    protected Resources mGbResources;
    private Field mFldWifiGroup;
    private Field mFldMobileGroup;
    private Field mFldMobileView;
    private Field mFldMobileTypeView;
    private Field mFldWifiView;
    private Field mFldAirplaneView;
    private List<String> mErrorsLogged = new ArrayList<String>();

    // Connection state and data activity
    protected boolean mConnectionStateEnabled;
    protected boolean mDataActivityEnabled;
    protected Object mNetworkControllerCallback;
    protected SignalActivity mWifiActivity;
    protected SignalActivity mMobileActivity;

    protected static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private void logAndMute(String key, Throwable t) {
        if (!mErrorsLogged.contains(key)) {
            XposedBridge.log(t);
            mErrorsLogged.add(key);
        }
    }

    // Signal activity
    enum SignalType { WIFI, MOBILE };
    class SignalActivity {
        boolean enabled;
        boolean fullyConnected = true;
        boolean activityIn;
        boolean activityOut;
        Drawable imageDataIn;
        Drawable imageDataOut;
        Drawable imageDataInOut;
        ImageView activityView;
        SignalType signalType;

        public SignalActivity(ViewGroup container, SignalType type) {
            signalType = type;
            if (mDataActivityEnabled) {
                activityView = new ImageView(container.getContext());
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                lp.gravity = Gravity.CENTER | Gravity.BOTTOM;
                activityView.setLayoutParams(lp);
                container.addView(activityView);
                if (type == SignalType.WIFI) {
                    imageDataIn = mGbResources.getDrawable(R.drawable.stat_sys_wifi_in);
                    imageDataOut = mGbResources.getDrawable(R.drawable.stat_sys_wifi_out);
                    imageDataInOut = mGbResources.getDrawable(R.drawable.stat_sys_wifi_inout);
                } else if (type == SignalType.MOBILE) {
                    imageDataIn = mGbResources.getDrawable(R.drawable.stat_sys_signal_in);
                    imageDataOut = mGbResources.getDrawable(R.drawable.stat_sys_signal_out);
                    imageDataInOut = mGbResources.getDrawable(R.drawable.stat_sys_signal_inout);
                }
                updateDataActivityColor();
            }
        }

        public void update() throws Throwable {
            update(enabled, fullyConnected, activityIn, activityOut);
        }

        public void update(boolean enabled, boolean fully, boolean in, boolean out) throws Throwable {
            this.enabled = enabled;
            fullyConnected = fully;
            activityIn = in;
            activityOut = out;

            // partially/fully connected state
            if (mConnectionStateEnabled) {
                if (!(mIconManager.isColoringEnabled() &&
                        mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED)) {
                    ImageView signalIcon = signalType == SignalType.WIFI ?
                            (ImageView) mFldWifiView.get(mView) : (ImageView) mFldMobileView.get(mView);
                    if (signalIcon != null && signalIcon.getDrawable() != null) {
                        Drawable d = signalIcon.getDrawable().mutate();
                        if (!fullyConnected) {
                            d.setColorFilter(Color.rgb(244, 145, 85), PorterDuff.Mode.SRC_ATOP);
                        } else {
                            d.clearColorFilter();
                        }
                        signalIcon.setImageDrawable(d);
                    }
                    if (signalType == SignalType.MOBILE) {
                        ImageView dataTypeIcon = (ImageView) mFldMobileTypeView.get(mView);
                        if (dataTypeIcon != null && dataTypeIcon.getDrawable() != null) {
                            Drawable dti = dataTypeIcon.getDrawable().mutate();
                            if (!fullyConnected) {
                                dti.setColorFilter(Color.rgb(244, 145, 85), PorterDuff.Mode.SRC_ATOP);
                            } else {
                                dti.clearColorFilter();
                            }
                            dataTypeIcon.setImageDrawable(dti);
                        }
                    }
                    if (DEBUG) log("SignalActivity: " + signalType + ": connection state updated");
                } else {
                    apply();
                }
            }

            // in/out activity
            if (mDataActivityEnabled) {
                if (activityIn && activityOut) {
                    activityView.setImageDrawable(imageDataInOut);
                } else if (activityIn) {
                    activityView.setImageDrawable(imageDataIn);
                } else if (activityOut) {
                    activityView.setImageDrawable(imageDataOut);
                }
                activityView.setVisibility(activityIn || activityOut ?
                        View.VISIBLE : View.GONE);
                if (DEBUG) log("SignalActivity: " + signalType + ": data activity indicators updated");
            }
        }

        public void updateDataActivityColor() {
            if (imageDataIn != null) {
                imageDataIn = mIconManager.applyDataActivityColorFilter(imageDataIn);
            }
            if (imageDataOut != null) {
                imageDataOut = mIconManager.applyDataActivityColorFilter(imageDataInOut);
            }
            if (imageDataInOut != null) {
                imageDataInOut = mIconManager.applyDataActivityColorFilter(imageDataInOut);
            }
        }
    } 

    public static StatusbarSignalCluster create(LinearLayout view, StatusBarIconManager iconManager,
            XSharedPreferences prefs) {
        sPrefs = prefs;
        if (Utils.isMt6572Device()) {
            return new StatusbarSignalClusterMt6572(view, iconManager);
        } else if (Utils.isMtkDevice()) {
            return new StatusbarSignalClusterMtk(view, iconManager);
        } else {
            return new StatusbarSignalCluster(view, iconManager);
        }
    }

    public StatusbarSignalCluster(LinearLayout view, StatusBarIconManager iconManager) {
        mView = view;
        mIconManager = iconManager;
        mResources = mView.getResources();
        try {
            mGbResources = mView.getContext().createPackageContext(
                    GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY).getResources();
        } catch (NameNotFoundException e) {
            XposedBridge.log(e);
        }

        mFldWifiGroup = resolveField("mWifiGroup", "mWifiViewGroup");
        mFldMobileGroup = resolveField("mMobileGroup", "mMobileViewGroup");
        mFldMobileView = resolveField("mMobile", "mMobileStrengthView");
        mFldMobileTypeView = resolveField("mMobileType", "mMobileTypeView");
        mFldWifiView = resolveField("mWifi", "mWifiStrengthView");
        mFldAirplaneView = resolveField("mAirplane", "mAirplaneView");

        initPreferences();
        createHooks();

        mIconManager.registerListener(this);
    }

    private Field resolveField(String... fieldNames) {
        Field field = null;
        for (String fieldName : fieldNames) {
            try {
                field = XposedHelpers.findField(mView.getClass(), fieldName);
                if (DEBUG) log(fieldName + " field found");
                break;
            } catch (NoSuchFieldError nfe) {
                if (DEBUG) log(fieldName + " field NOT found");
            }
        }
        return field;
    }

    private void createHooks() {
        try {
            XposedHelpers.findAndHookMethod(mView.getClass(), "apply", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    apply();
                    if (mConnectionStateEnabled) {
                        if (mWifiActivity != null) {
                            mWifiActivity.update();
                        }
                        if (mMobileActivity != null) {
                            mMobileActivity.update();
                        }
                    }
                }
            });
        } catch (Throwable t) {
            log("Error hooking apply() method: " + t.getMessage());
        }

        if (mConnectionStateEnabled || mDataActivityEnabled) {
            try {
                final ClassLoader classLoader = mView.getContext().getClassLoader();
                final Class<?> networkCtrlClass = XposedHelpers.findClass(
                        "com.android.systemui.statusbar.policy.NetworkController", classLoader);
                final Class<?> networkCtrlCbClass = XposedHelpers.findClass(
                        "com.android.systemui.statusbar.policy.NetworkController.NetworkSignalChangedCallback", 
                            classLoader);
                XposedHelpers.findAndHookMethod(mView.getClass(), "setNetworkController",
                        networkCtrlClass, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mNetworkControllerCallback = Proxy.newProxyInstance(classLoader, 
                            new Class<?>[] { networkCtrlCbClass }, new NetworkControllerCallback());
                        XposedHelpers.callMethod(param.args[0], "addNetworkSignalChangedCallback",
                                mNetworkControllerCallback);
                        if (DEBUG) log("setNetworkController: callback registered");
                    }
                });
    
                XposedHelpers.findAndHookMethod(mView.getClass(), "onAttachedToWindow", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ViewGroup wifiGroup = (ViewGroup) mFldWifiGroup.get(param.thisObject);
                        if (wifiGroup != null) {
                            mWifiActivity = new SignalActivity(wifiGroup, SignalType.WIFI);
                            if (DEBUG) log("onAttachedToWindow: mWifiActivity created");
                        }
    
                        ViewGroup mobileGroup = (ViewGroup) mFldMobileGroup.get(param.thisObject);
                        if (mobileGroup != null) {
                            mMobileActivity = new SignalActivity(mobileGroup, SignalType.MOBILE);
                            if (DEBUG) log("onAttachedToWindow: mMobileActivity created");
                        }
                    }
                });
    
                XposedHelpers.findAndHookMethod(mView.getClass(), "onDetachedFromWindow", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mWifiActivity = null;
                        mMobileActivity = null;
                        if (DEBUG) log("onDetachedFromWindow: signal activities destoyed");
                    }
                });
            } catch (Throwable t) {
                log("Error hooking SignalActivity related methods: " + t.getMessage());
            }
        }
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) { }

    protected void initPreferences() { 
        mConnectionStateEnabled = sPrefs.getBoolean(
                GravityBoxSettings.PREF_KEY_SIGNAL_CLUSTER_CONNECTION_STATE, false);
        mDataActivityEnabled = sPrefs.getBoolean(
                GravityBoxSettings.PREF_KEY_SIGNAL_CLUSTER_DATA_ACTIVITY, false);
    }

    private void update() {
        if (mView != null) {
            try {
                XposedHelpers.callMethod(mView, "apply");
            } catch (Throwable t) {
                logAndMute("invokeApply", t);
            }
        }
    }

    protected void apply() {
        try {
            boolean doApply = true;
            if (mFldWifiGroup != null) {
                doApply = mFldWifiGroup.get(mView) != null;
            }
            if (doApply) {
                if (mIconManager.isColoringEnabled()) {
                    updateWiFiIcon();
                    if (!XposedHelpers.getBooleanField(mView, "mIsAirplaneMode")) {
                        updateMobileIcon();
                    }
                    if (DEBUG) log("Signal icon colors updated");
                }
                updateAirplaneModeIcon();
            }
        } catch (Throwable t) {
            logAndMute("apply", t);
        }
    }

    protected void updateWiFiIcon() {
        try {
            if (XposedHelpers.getBooleanField(mView, "mWifiVisible") &&
                    mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED) {
                ImageView wifiIcon = (ImageView) mFldWifiView.get(mView);
                if (wifiIcon != null) {
                    int resId = XposedHelpers.getIntField(mView, "mWifiStrengthId");
                    Drawable d = mIconManager.getWifiIcon(resId, 
                            mWifiActivity != null ? mWifiActivity.fullyConnected : true);
                    if (d != null) wifiIcon.setImageDrawable(d);
                }
            }
        } catch (Throwable t) {
            logAndMute("updateWiFiIcon", t);
        }
    }

    protected void updateMobileIcon() {
        try {
            if (XposedHelpers.getBooleanField(mView, "mMobileVisible") &&
                    mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED) {
                ImageView mobile = (ImageView) mFldMobileView.get(mView);
                if (mobile != null) {
                    int resId = XposedHelpers.getIntField(mView, "mMobileStrengthId");
                    Drawable d = mIconManager.getMobileIcon(resId,
                            mMobileActivity != null ? mMobileActivity.fullyConnected : true);
                    if (d != null) mobile.setImageDrawable(d);
                }
                if (mIconManager.isMobileIconChangeAllowed()) {
                    ImageView mobileType = (ImageView) mFldMobileTypeView.get(mView);
                    if (mobileType != null) {
                        try {
                            int resId = XposedHelpers.getIntField(mView, "mMobileTypeId");
                            Drawable d = mResources.getDrawable(resId).mutate();
                            d = mIconManager.applyColorFilter(d);
                            mobileType.setImageDrawable(d);
                        } catch (Resources.NotFoundException e) { 
                            mobileType.setImageDrawable(null);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logAndMute("updateMobileIcon", t);
        }
    }

    protected void updateAirplaneModeIcon() {
        try {
            ImageView airplaneModeIcon = (ImageView) mFldAirplaneView.get(mView);
            if (airplaneModeIcon != null) {
                Drawable d = airplaneModeIcon.getDrawable();
                if (mIconManager.isColoringEnabled()) {
                    d = mIconManager.applyColorFilter(d);
                } else if (d != null) {
                    d.setColorFilter(null);
                }
                airplaneModeIcon.setImageDrawable(d);
            }
        } catch (Throwable t) {
            logAndMute("updateAirplaneModeIcon", t);
        }
    }

    @Override
    public void onIconManagerStatusChanged(int flags, ColorInfo colorInfo) {
        if ((flags & (StatusBarIconManager.FLAG_ICON_COLOR_CHANGED |
                StatusBarIconManager.FLAG_DATA_ACTIVITY_COLOR_CHANGED |
                StatusBarIconManager.FLAG_ICON_COLOR_SECONDARY_CHANGED |
                StatusBarIconManager.FLAG_SIGNAL_ICON_MODE_CHANGED)) != 0) {
            if ((flags & StatusBarIconManager.FLAG_DATA_ACTIVITY_COLOR_CHANGED) != 0 &&
                    mDataActivityEnabled) {
                if (mWifiActivity != null) {
                    mWifiActivity.updateDataActivityColor();
                }
                if (mMobileActivity != null) {
                    mMobileActivity.updateDataActivityColor();
                }
            }
            update();
        }
    }

    protected class NetworkControllerCallback implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();

            try {
                if (methodName.equals("onWifiSignalChanged")) {
                    if (mWifiActivity != null) {
                        boolean fullyConnected = true;
                        final int wifiIconId = (Integer)args[1];
                        if (mConnectionStateEnabled && wifiIconId != 0) {
                            String wifiIconName = mResources.getResourceEntryName(wifiIconId);
                            fullyConnected = wifiIconName.contains("full") ||
                                    wifiIconName.equals("ic_qs_wifi_0");
                        }
                        mWifiActivity.update((Boolean)args[0], fullyConnected, 
                                (Boolean)args[2], (Boolean)args[3]);
                    }
                } else if (methodName.equals("onMobileDataSignalChanged")) {
                    if (mMobileActivity != null) {
                        boolean fullyConnected = true;
                        final int mobileIconId = (Integer)args[1];
                        final int dataActivityIconId = (Integer)args[3];
                        if (mConnectionStateEnabled && mobileIconId != 0 && dataActivityIconId != 0) {
                            String mobileIconName = mResources.getResourceEntryName(mobileIconId);
                            fullyConnected = mWifiActivity.enabled || mobileIconName.contains("full");
                        }
                        mMobileActivity.update((Boolean)args[0], fullyConnected, 
                                (Boolean)args[4], (Boolean)args[5]);
                    }
                }
            } catch (Throwable t) {
                logAndMute("NetworkControllerCallback", t);
            }

            return null;
        }
    }
}
