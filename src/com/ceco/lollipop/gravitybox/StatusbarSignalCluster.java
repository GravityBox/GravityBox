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

package com.ceco.lollipop.gravitybox;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import com.ceco.lollipop.gravitybox.ModStatusBar.ContainerType;
import com.ceco.lollipop.gravitybox.R;
import com.ceco.lollipop.gravitybox.managers.StatusBarIconManager;
import com.ceco.lollipop.gravitybox.managers.SysUiManagers;
import com.ceco.lollipop.gravitybox.managers.StatusBarIconManager.ColorInfo;
import com.ceco.lollipop.gravitybox.managers.StatusBarIconManager.IconManagerListener;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.drawable.Drawable;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class StatusbarSignalCluster implements BroadcastSubReceiver, IconManagerListener {
    public static final String TAG = "GB:StatusbarSignalCluster";
    protected static final boolean DEBUG = false;

    protected static XSharedPreferences sPrefs;

    // HSPA+
    protected static int sQsHpResId;
    protected static int sSbHpResId;
    protected static int[][] DATA_HP;
    protected static int[] QS_DATA_HP;

    protected ContainerType mContainerType;
    protected LinearLayout mView;
    protected StatusBarIconManager mIconManager;
    protected Resources mResources;
    protected Resources mGbResources;
    protected Field mFldWifiGroup;
    private Field mFldMobileGroup;
    private Field mFldMobileView;
    private Field mFldMobileTypeView;
    private Field mFldWifiView;
    private Field mFldAirplaneView;
    private List<String> mErrorsLogged = new ArrayList<String>();
    
    private boolean mHideVPN;
    private boolean mHideWifi;
    private boolean mHideSignal;
    private boolean mHideAirplane;

    // Data activity
    protected boolean mDataActivityEnabled;
    protected Object mNetworkControllerCallback;
    protected SignalActivity mWifiActivity;
    protected SignalActivity mMobileActivity;

    protected static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    protected void logAndMute(String key, Throwable t) {
        if (!mErrorsLogged.contains(key)) {
            XposedBridge.log(t);
            mErrorsLogged.add(key);
        }
    }

    public static String getClassName(ClassLoader classLoader) {
        if (PhoneWrapper.hasMsimSupport()) {
            return "com.android.systemui.statusbar.MSimSignalClusterView";
        } else if (Utils.hasGeminiSupport()) {
            try {
                XposedHelpers.findClass("com.android.systemui.statusbar.SignalClusterViewGemini", 
                        classLoader);
                return "com.android.systemui.statusbar.SignalClusterViewGemini";
            } catch(Throwable t) { }
        }

        return "com.android.systemui.statusbar.SignalClusterView";
    }

    // Signal activity
    enum SignalType { WIFI, MOBILE };
    class SignalActivity {
        boolean enabled;
        boolean activityIn;
        boolean activityOut;
        Drawable imageDataIn;
        Drawable imageDataOut;
        Drawable imageDataInOut;
        ImageView activityView;
        SignalType signalType;

        public SignalActivity(ViewGroup container, SignalType type) {
            this(container, type, Gravity.BOTTOM | Gravity.CENTER);
        }

        public SignalActivity(ViewGroup container, SignalType type, int gravity) {
            signalType = type;
            if (mDataActivityEnabled) {
                activityView = new ImageView(container.getContext());
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                lp.gravity = gravity;
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

        public void update() {
            try {
                update(enabled, activityIn, activityOut);
            } catch (Throwable t) {
                logAndMute("SignalActivity.update", t);
            }
        }

        public void update(boolean enabled, boolean in, boolean out) throws Throwable {
            this.enabled = enabled;
            activityIn = in;
            activityOut = out;

            // in/out activity
            if (mDataActivityEnabled) {
                if (activityIn && activityOut) {
                    activityView.setImageDrawable(imageDataInOut);
                } else if (activityIn) {
                    activityView.setImageDrawable(imageDataIn);
                } else if (activityOut) {
                    activityView.setImageDrawable(imageDataOut);
                } else {
                    activityView.setImageDrawable(null);
                }
                activityView.setVisibility(activityIn || activityOut ?
                        View.VISIBLE : View.GONE);
                if (DEBUG) log("SignalActivity: " + signalType + ": data activity indicators updated");
            }
        }

        public void updateDataActivityColor() {
            if (mIconManager == null) return;

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

    public static void initResources(XSharedPreferences prefs, InitPackageResourcesParam resparam) {
        XModuleResources modRes = XModuleResources.createInstance(GravityBox.MODULE_PATH, resparam.res);

        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_SIGNAL_CLUSTER_HPLUS, false) &&
                !Utils.isMotoXtDevice() && !Utils.isMtkDevice()) {

            sQsHpResId = XResources.getFakeResId(modRes, R.drawable.ic_qs_signal_hp);
            sSbHpResId = XResources.getFakeResId(modRes, R.drawable.stat_sys_data_fully_connected_hp);
    
            resparam.res.setReplacement(sQsHpResId, modRes.fwd(R.drawable.ic_qs_signal_hp));
            resparam.res.setReplacement(sSbHpResId, modRes.fwd(R.drawable.stat_sys_data_fully_connected_hp));
    
            DATA_HP = new int[][] {
                    { sSbHpResId, sSbHpResId, sSbHpResId, sSbHpResId },
                    { sSbHpResId, sSbHpResId, sSbHpResId, sSbHpResId }
            };
            QS_DATA_HP = new int[] { sQsHpResId, sQsHpResId };
            if (DEBUG) log("H+ icon resources initialized");
        }

        if (!Utils.isMtkDevice()) {
            String lteStyle = prefs.getString(GravityBoxSettings.PREF_KEY_SIGNAL_CLUSTER_LTE_STYLE, "DEFAULT");
            if (!lteStyle.equals("DEFAULT")) {
                resparam.res.setReplacement(ModStatusBar.PACKAGE_NAME, "bool", "config_show4GForLTE",
                        lteStyle.equals("4G"));
            }
        }
    }

    public static StatusbarSignalCluster create(ContainerType containerType,
            LinearLayout view, XSharedPreferences prefs) {
        sPrefs = prefs;
        if (PhoneWrapper.hasMsimSupport()) {
            return new StatusbarSignalClusterMsim(containerType, view);
        } else if (Utils.isMtkDevice()) {
            return new StatusbarSignalClusterMtk(containerType, view);
        } else {
            return new StatusbarSignalCluster(containerType, view);
        }
    }

    public StatusbarSignalCluster(ContainerType containerType, LinearLayout view) {
        mContainerType = containerType;
        mView = view;
        mIconManager = SysUiManagers.IconManager;
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
        
        mHideVPN = sPrefs.getBoolean(GravityBoxSettings.PREF_KEY_STATUSBAR_HIDE_VPN, false);
        mHideWifi = sPrefs.getBoolean(GravityBoxSettings.PREF_KEY_STATUSBAR_HIDE_WIFI, false);
        mHideSignal = sPrefs.getBoolean(GravityBoxSettings.PREF_KEY_STATUSBAR_HIDE_SIGNAL, false);
        mHideAirplane = sPrefs.getBoolean(GravityBoxSettings.PREF_KEY_STATUSBAR_HIDE_AIRPLANE, false);

        initPreferences();
        createHooks();

        if (mIconManager != null) {
            mIconManager.registerListener(this);
        }
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

    protected void createHooks() {
        try {
            XposedHelpers.findAndHookMethod(mView.getClass(), "apply", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mView != param.thisObject) return;

                    apply();
                }
            });
        } catch (Throwable t) {
            log("Error hooking apply() method: " + t.getMessage());
        }

        if (mDataActivityEnabled) {
            try {
                XposedHelpers.findAndHookMethod(mView.getClass(), "onAttachedToWindow", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (mView != param.thisObject) return;

                        ViewGroup wifiGroup = (ViewGroup) mFldWifiGroup.get(param.thisObject);
                        if (wifiGroup != null) {
                            mWifiActivity = new SignalActivity(wifiGroup, SignalType.WIFI);
                            if (DEBUG) log("onAttachedToWindow: mWifiActivity created");
                        }
    
                        ViewGroup mobileGroup = (ViewGroup) mFldMobileGroup.get(param.thisObject);
                        if (mobileGroup != null) {
                            mMobileActivity = new SignalActivity(mobileGroup, SignalType.MOBILE,
                                    Gravity.BOTTOM | Gravity.END);
                            if (DEBUG) log("onAttachedToWindow: mMobileActivity created");
                        }
                    }
                });
    
                XposedHelpers.findAndHookMethod(mView.getClass(), "onDetachedFromWindow", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (mView != param.thisObject) return;

                        mWifiActivity = null;
                        mMobileActivity = null;
                        if (DEBUG) log("onDetachedFromWindow: signal activities destoyed");
                    }
                });
            } catch (Throwable t) {
                log("Error hooking SignalActivity related methods: " + t.getMessage());
            }
        }

        if (sPrefs.getBoolean(GravityBoxSettings.PREF_KEY_SIGNAL_CLUSTER_HPLUS, false) &&
                !Utils.isMotoXtDevice()) {
            try {
                final Class<?> networkCtrlClass = XposedHelpers.findClass(
                        "com.android.systemui.statusbar.policy.NetworkControllerImpl", 
                        mView.getContext().getClassLoader());
                XposedHelpers.findAndHookMethod(networkCtrlClass, "updateDataNetType", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG) log("NetworkController: updateDataNetType");
                        if (!(XposedHelpers.getBooleanField(param.thisObject, "mIsWimaxEnabled") &&
                                XposedHelpers.getBooleanField(param.thisObject, "mWimaxConnected")) &&
                                XposedHelpers.getIntField(param.thisObject, "mDataNetType") ==
                                    TelephonyManager.NETWORK_TYPE_HSPAP) {
                            int inetCondition = XposedHelpers.getIntField(param.thisObject, "mInetCondition");
                            XposedHelpers.setObjectField(param.thisObject, "mDataIconList", DATA_HP[inetCondition]);
                            boolean isCdmaEri = (Boolean) XposedHelpers.callMethod(param.thisObject, "isCdma") &&
                                    (Boolean) XposedHelpers.callMethod(param.thisObject, "isCdmaEri");
                            boolean isRoaming = ((TelephonyManager) XposedHelpers.getObjectField(
                                    param.thisObject, "mPhone")).isNetworkRoaming();
                            if (!isCdmaEri && !isRoaming) {
                                XposedHelpers.setIntField(param.thisObject, "mDataTypeIconId", sSbHpResId);
                                XposedHelpers.setIntField(param.thisObject, "mQSDataTypeIconId",
                                        QS_DATA_HP[inetCondition]);
                                if (DEBUG) {
                                    log("H+ inet condition: " + inetCondition);
                                    log("H+ data type: " + sSbHpResId);
                                    log("H+ QS data type: " + QS_DATA_HP[inetCondition]);
                                }
                            }
                        }
                    }
                });
            } catch (Throwable t) {
                logAndMute("updateDataNetType", t);
            }
        }
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
    	if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_STATUSBAR_ICON_VISIBILITY_CHANGED)) {
        	
        	if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_VPN_VISIBILITY)) {
        		mHideVPN = intent.getBooleanExtra(GravityBoxSettings.EXTRA_SB_VPN_VISIBILITY, false);
        	}
        	
        	if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_WIFI_VISIBILITY)) {
        		mHideWifi = intent.getBooleanExtra(GravityBoxSettings.EXTRA_SB_WIFI_VISIBILITY, false);
        	}
        	
        	if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_SIGNAL_VISIBILITY)) {
        		mHideSignal = intent.getBooleanExtra(GravityBoxSettings.EXTRA_SB_SIGNAL_VISIBILITY, false);
        	}
        	
        	if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_AIRPLANE_VISIBILITY)) {
        		mHideAirplane = intent.getBooleanExtra(GravityBoxSettings.EXTRA_SB_AIRPLANE_VISIBILITY, false);
        	}
        	
        	update();
        }
    }

    protected void initPreferences() { 
        mDataActivityEnabled = mContainerType != ContainerType.HEADER && 
                sPrefs.getBoolean(GravityBoxSettings.PREF_KEY_SIGNAL_CLUSTER_DATA_ACTIVITY, false);
    }

    protected void setNetworkController(Object networkController) {
        final ClassLoader classLoader = mView.getClass().getClassLoader();
        final Class<?> networkCtrlCbClass = XposedHelpers.findClass(
                "com.android.systemui.statusbar.policy.NetworkController.NetworkSignalChangedCallback", 
                classLoader);
        mNetworkControllerCallback = Proxy.newProxyInstance(classLoader, 
                new Class<?>[] { networkCtrlCbClass }, new NetworkControllerCallback());
            XposedHelpers.callMethod(networkController, "addNetworkSignalChangedCallback",
                    mNetworkControllerCallback);
        if (DEBUG) log("setNetworkController: callback registered");
    }

    protected void update() {
        if (mView != null) {
            try {
                XposedHelpers.callMethod(mView, "apply");
            } catch (Throwable t) {
                logAndMute("invokeApply", t);
            }
        }
    }

    protected void apply() {
        apply(0);
    }

    protected void apply(int simSlot) {
        try {
            boolean doApply = true;
            if (mFldWifiGroup != null) {
                doApply = mFldWifiGroup.get(mView) != null;
            }
            if (doApply) {
                if (mIconManager != null && mIconManager.isColoringEnabled()) {
                    updateWiFiIcon();
                    if (!XposedHelpers.getBooleanField(mView, "mIsAirplaneMode")) {
                        updateMobileIcon(simSlot);
                    }
                    if (DEBUG) log("Signal icon colors updated");
                }
                updateAirplaneModeIcon();
            }
        } catch (Throwable t) {
            logAndMute("apply", t);
        }
        
        Object mVpn = XposedHelpers.getObjectField(mView, "mVpn");
        Object mWifiGroup = XposedHelpers.getObjectField(mView, "mWifiGroup");
        Object mMobileGroup = XposedHelpers.getObjectField(mView, "mMobileGroup");
        Object mAirplane = XposedHelpers.getObjectField(mView, "mAirplane");
        
        if (mVpn != null && mHideVPN) {
            XposedHelpers.callMethod(mVpn, "setVisibility", View.GONE);
        }
        
        if (mWifiGroup != null && mHideWifi) {
            XposedHelpers.callMethod(mWifiGroup, "setVisibility", View.GONE);
        }
        
        if (mMobileGroup != null &&  mHideSignal) {
            XposedHelpers.callMethod(mMobileGroup, "setVisibility", View.GONE);
        }
        
        if (mAirplane != null &&  mHideAirplane) {
            XposedHelpers.callMethod(mAirplane, "setVisibility", View.GONE);
        }
    }

    protected void updateWiFiIcon() {
        try {
            if (XposedHelpers.getBooleanField(mView, "mWifiVisible") && mIconManager != null &&
                    mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED) {
                ImageView wifiIcon = (ImageView) mFldWifiView.get(mView);
                if (wifiIcon != null) {
                    int resId = XposedHelpers.getIntField(mView, "mWifiStrengthId");
                    Drawable d = mIconManager.getWifiIcon(resId);
                    if (d != null) wifiIcon.setImageDrawable(d);
                }
            }
        } catch (Throwable t) {
            logAndMute("updateWiFiIcon", t);
        }
    }

    protected void updateMobileIcon(int simSlot) {
        try {
            if (XposedHelpers.getBooleanField(mView, "mMobileVisible") && mIconManager != null &&
                    mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED) {
                ImageView mobile = (ImageView) mFldMobileView.get(mView);
                if (mobile != null) {
                    int resId = XposedHelpers.getIntField(mView, "mMobileStrengthId");
                    Drawable d = mIconManager.getMobileIcon(resId, true);
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
                if (mIconManager != null && mIconManager.isColoringEnabled()) {
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
                    if (DEBUG) {
                        log("WiFi enabled: " + args[0]);
                        log("WiFi activity in: " + (Boolean)args[3]);
                        log("WiFi activity out: " + (Boolean)args[4]);
                    }
                    if (mWifiActivity != null) {
                        mWifiActivity.update((Boolean)args[0],
                                (Boolean)args[3], (Boolean)args[4]);
                    }
                } else if (methodName.equals("onMobileDataSignalChanged")) {
                    if (DEBUG) {
                        log("Mobile data enabled: " + args[0]);
                        log("Mobile data activity in: " + (Boolean)args[4]);
                        log("Mobile data activity out: " + (Boolean)args[5]);
                    }
                    if (mMobileActivity != null) {
                        mMobileActivity.update((Boolean)args[0], 
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
