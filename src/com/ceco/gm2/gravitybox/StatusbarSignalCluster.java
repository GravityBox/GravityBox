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

import java.lang.reflect.Field;

import com.ceco.gm2.gravitybox.StatusBarIconManager.ColorInfo;
import com.ceco.gm2.gravitybox.StatusBarIconManager.IconManagerListener;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class StatusbarSignalCluster implements BroadcastSubReceiver, IconManagerListener {
    public static final String TAG = "GB:StatusbarSignalCluster";
    private static final boolean DEBUG = false;

    protected LinearLayout mView;
    protected StatusBarIconManager mIconManager;
    protected Resources mResources;
    private Field mFldWifiGroup;
    private ImageView mAirplaneModeIcon;

    protected static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static StatusbarSignalCluster create(LinearLayout view, StatusBarIconManager iconManager) {
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
            mFldWifiGroup = XposedHelpers.findField(mView.getClass(), "mWifiGroup");
            if (DEBUG) log("mWifiGroup field found");
        } catch (NoSuchFieldError nfe) {
            try {
                mFldWifiGroup = XposedHelpers.findField(mView.getClass(), "mWifiViewGroup");
                if (DEBUG) log("mWifiViewGroup field found");
            } catch (NoSuchFieldError nfe2) {
                log("Couldn't find WifiGroup field");
            }
        }
        
        try{
        	mAirplaneModeIcon = (ImageView) XposedHelpers.getObjectField(mView, "mAirplane");
        	if(DEBUG) log("mAirplane found");
        }
        catch (NoSuchFieldError nfe){
        	log("Couldn't find mAirplane field");
        }
        

        if (mView != null) {
            try {
                XposedHelpers.findAndHookMethod(mView.getClass(), "apply", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        apply();
                    }
                });
            } catch (Throwable t) {
                log("Error hooking apply() method: " + t.getMessage());
            }
        }

        mIconManager.registerListener(this);
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) { }

    public void initPreferences(XSharedPreferences prefs) { }

    private void update() {
        if (mView != null) {
            try {
                XposedHelpers.callMethod(mView, "apply");
            } catch (Throwable t) {
                log("Error invoking apply() method: " + t.getMessage());
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
                }
                updateAirplaneModeIcon();
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    protected void updateWiFiIcon() {
        try {
            if (XposedHelpers.getBooleanField(mView, "mWifiVisible") &&
                    mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED) {
                ImageView wifiIcon = (ImageView) XposedHelpers.getObjectField(mView, "mWifi");
                if (wifiIcon != null) {
                    int resId = XposedHelpers.getIntField(mView, "mWifiStrengthId");
                    Drawable d = mIconManager.getWifiIcon(resId);
                    if (d != null) wifiIcon.setImageDrawable(d);
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    protected void updateMobileIcon() {
        try {
            if (XposedHelpers.getBooleanField(mView, "mMobileVisible") &&
                    mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED) {
                ImageView mobile = (ImageView) XposedHelpers.getObjectField(mView, "mMobile");
                if (mobile != null) {
                    int resId = XposedHelpers.getIntField(mView, "mMobileStrengthId");
                    Drawable d = mIconManager.getMobileIcon(resId);
                    if (d != null) mobile.setImageDrawable(d);
                }
                if (mIconManager.isMobileIconChangeAllowed()) {
                    ImageView mobileType = (ImageView) XposedHelpers.getObjectField(mView, "mMobileType");
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
            XposedBridge.log(t);
        }
    }

    protected void updateAirplaneModeIcon() {
        try {
            if (mAirplaneModeIcon != null) {
                Drawable d = mAirplaneModeIcon.getDrawable();
                if (mIconManager.isColoringEnabled()) {
                    d = mIconManager.applyColorFilter(d);
                } else if (d != null) {
                    d.setColorFilter(null);
                }
                mAirplaneModeIcon.setImageDrawable(d);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    @Override
    public void onIconManagerStatusChanged(int flags, ColorInfo colorInfo) {
        if ((flags & (StatusBarIconManager.FLAG_ICON_COLOR_CHANGED |
                StatusBarIconManager.FLAG_DATA_ACTIVITY_COLOR_CHANGED |
                StatusBarIconManager.FLAG_ICON_COLOR_SECONDARY_CHANGED |
                StatusBarIconManager.FLAG_SIGNAL_ICON_MODE_CHANGED)) != 0) {
            update();
        }
    }
}
