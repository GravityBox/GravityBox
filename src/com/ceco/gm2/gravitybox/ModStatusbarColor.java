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

import java.util.ArrayList;
import java.util.List;

import com.ceco.gm2.gravitybox.StatusBarIconManager.ColorInfo;
import com.ceco.gm2.gravitybox.StatusBarIconManager.IconManagerListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XCallback;

public class ModStatusbarColor {
    private static final String TAG = "GB:ModStatusbarColor";
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String CLASS_PHONE_WINDOW_MANAGER = "com.android.internal.policy.impl.PhoneWindowManager";
    private static final String CLASS_PHONE_STATUSBAR_VIEW = "com.android.systemui.statusbar.phone.PhoneStatusBarView";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final String CLASS_SIGNAL_CLUSTER_VIEW = Utils.hasGeminiSupport() ? 
            "com.android.systemui.statusbar.SignalClusterViewGemini" :
            "com.android.systemui.statusbar.SignalClusterView";
    private static final String CLASS_BATTERY_CONTROLLER = "com.android.systemui.statusbar.policy.BatteryController";
    private static final String CLASS_NOTIF_PANEL_VIEW = "com.android.systemui.statusbar.phone.NotificationPanelView";
    private static final String CLASS_POLICY_WINDOW_STATE = "android.view.WindowManagerPolicy$WindowState";
    private static final String CLASS_WINDOW_STATE = "com.android.server.wm.WindowState";
    private static final String CLASS_WINDOW_MANAGER_SERVICE = "com.android.server.wm.WindowManagerService";
    private static final String CLASS_STATUSBAR_ICON_VIEW = "com.android.systemui.statusbar.StatusBarIconView";
    private static final String CLASS_STATUSBAR_ICON = "com.android.internal.statusbar.StatusBarIcon";
    private static final boolean DEBUG = false;

    public static final String ACTION_PHONE_STATUSBAR_VIEW_MADE = "gravitybox.intent.action.PHONE_STATUSBAR_VIEW_MADE";

    private static View mPanelBar;
    private static StatusBarIconManager mIconManager;
    private static View mBattery;
    private static int mBatteryLevel;
    private static boolean mBatteryPlugged;
    private static Object mBatteryController;
    private static TransparencyManager mTransparencyManager;
    private static Context mContextPwm;
    private static int[] mTransparencyValuesPwm = new int[] { 0, 0, 0, 0};
    private static int mTransparencyModePwm = TransparencyManager.MODE_FULL;
    private static List<BroadcastSubReceiver> mBroadcastSubReceivers;
    private static Unhook mDisplayContentHook;
    private static Object mPhoneWindowManager;
    private static Object mPhoneStatusBar;
    private static StatusbarSignalCluster mSignalCluster;

    static {
        mBatteryLevel = 0;
        mBatteryPlugged = false;
    }

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void setBattery(View battery) {
        mBattery = battery;
    }

    public static void registerIconManagerListener(IconManagerListener listener) {
        if (mIconManager != null) {
            mIconManager.registerListener(listener);
        }
    }

    private static BroadcastReceiver mBroadcastReceiverPwm = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("PhoneWindowManager received broadcast: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_STATUSBAR_COLOR_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_SB_LAUNCHER)) {
                    mTransparencyValuesPwm[0] = intent.getIntExtra(GravityBoxSettings.EXTRA_TM_SB_LAUNCHER, 0);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_SB_LOCKSCREEN)) {
                    mTransparencyValuesPwm[1] = intent.getIntExtra(GravityBoxSettings.EXTRA_TM_SB_LOCKSCREEN, 0);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_NB_LAUNCHER)) {
                    mTransparencyValuesPwm[2] = intent.getIntExtra(GravityBoxSettings.EXTRA_TM_NB_LAUNCHER, 0);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_NB_LOCKSCREEN)) {
                    mTransparencyValuesPwm[3] = intent.getIntExtra(GravityBoxSettings.EXTRA_TM_NB_LOCKSCREEN, 0);
                }
            }
        }
    };

    public static void unregisterIconManagerListener(IconManagerListener listener) {
        if (mIconManager != null) {
            mIconManager.unregisterListener(listener);
        }
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("received broadcast: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_STATUSBAR_COLOR_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_BG_COLOR)) {
                    int bgColor = intent.getIntExtra(GravityBoxSettings.EXTRA_SB_BG_COLOR, Color.BLACK);
                    setStatusbarBgColor(bgColor);
                }
            }

            for (BroadcastSubReceiver bsr : mBroadcastSubReceivers) {
                bsr.onBroadcastReceived(context, intent);
            }
        }
    };

    // in Zygote hooks
    public static void initZygote(final XSharedPreferences prefs) {
        try {
            final Class<?> phoneWindowManagerClass = XposedHelpers.findClass(CLASS_PHONE_WINDOW_MANAGER, null);
            final Class<?> windowStateClass = XposedHelpers.findClass(CLASS_WINDOW_STATE, null);
            final Class<?> windowManagerServiceClass = XposedHelpers.findClass(CLASS_WINDOW_MANAGER_SERVICE, null);

            try {
                mTransparencyModePwm = Integer.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_TM_MODE, "3"));
            } catch (NumberFormatException nfe) {
                //
            }

            if (mTransparencyModePwm != TransparencyManager.MODE_DISABLED) {
                if (DEBUG) log("replacing getSystemDecorRectLw method");
                XposedHelpers.findAndHookMethod(phoneWindowManagerClass,
                        "getSystemDecorRectLw", Rect.class, new XC_MethodReplacement() {
    
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (mContextPwm == null) {
                            if (DEBUG) log("getSystemDecorRectLw: registering transparency settings receiver");
                            mTransparencyValuesPwm[0] = prefs.getInt(GravityBoxSettings.PREF_KEY_TM_STATUSBAR_LAUNCHER, 0);
                            mTransparencyValuesPwm[1] = prefs.getInt(GravityBoxSettings.PREF_KEY_TM_STATUSBAR_LOCKSCREEN, 0);
                            mTransparencyValuesPwm[2] = prefs.getInt(GravityBoxSettings.PREF_KEY_TM_NAVBAR_LAUNCHER, 0);
                            mTransparencyValuesPwm[3] = prefs.getInt(GravityBoxSettings.PREF_KEY_TM_NAVBAR_LOCKSCREEN, 0);
                            mPhoneWindowManager = param.thisObject;
                            mContextPwm = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                            IntentFilter intentFilter = new IntentFilter();
                            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                            mContextPwm.registerReceiver(mBroadcastReceiverPwm, intentFilter);
                        }
    
                        if (!isTransparencyEnabled()) {
                            if (DEBUG) log("getSystemDecorRectLw: calling original method");
                            return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                        } else {
                            if (DEBUG) log("getSystemDecorRectLw: overriding original method");
                            Rect rect = (Rect) param.args[0];
                            rect.left = XposedHelpers.getIntField(param.thisObject, "mSystemLeft");
                            rect.top = XposedHelpers.getIntField(param.thisObject, "mSystemTop");
                            rect.right = XposedHelpers.getIntField(param.thisObject, "mSystemRight");
                            rect.bottom = XposedHelpers.getIntField(param.thisObject, "mSystemBottom");
                            return 0;
                        }
                    }
                });
    
                XposedHelpers.findAndHookMethod(phoneWindowManagerClass, "layoutWindowLw",
                        CLASS_POLICY_WINDOW_STATE, WindowManager.LayoutParams.class, 
                        CLASS_POLICY_WINDOW_STATE, new XC_MethodHook(XC_MethodHook.PRIORITY_LOWEST) {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        final boolean isDefaultDisplay = Build.VERSION.SDK_INT > 16 ?
                                (Boolean) XposedHelpers.callMethod(param.args[0], "isDefaultDisplay") : true;
                        if (!isNavbarTransparencyEnabled() || !isDefaultDisplay) return; 
    
                        final WindowManager.LayoutParams attrs = (WindowManager.LayoutParams) param.args[1];
                        if (attrs.type == WindowManager.LayoutParams.TYPE_WALLPAPER) {
                            final int fl = attrs.flags;
                            final int sysUiFl = (Integer) XposedHelpers.callMethod(param.args[0], "getSystemUiVisibility");
                            final Rect pf = (Rect) XposedHelpers.getObjectField(param.thisObject, "mTmpParentFrame");
                            final Rect df = (Rect) XposedHelpers.getObjectField(param.thisObject, "mTmpDisplayFrame");
                            final Rect cf = (Rect) XposedHelpers.getObjectField(param.thisObject, "mTmpContentFrame");
                            final Rect vf = (Rect) XposedHelpers.getObjectField(param.thisObject, "mTmpVisibleFrame");
                            final Rect of = Build.VERSION.SDK_INT > 17 ?
                                (Rect) XposedHelpers.getObjectField(param.thisObject, "mTmpOverscanFrame") : null;

                            pf.top = df.top = cf.top = vf.top = 
                                    XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenTop");
                            pf.bottom = df.bottom = cf.bottom = vf.bottom = pf.top + 
                                    XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenHeight");

                            if (Build.VERSION.SDK_INT > 17) {
                                of.set(pf);
                            }

                            XposedHelpers.callMethod(param.thisObject, "applyStableConstraints",
                                    sysUiFl, fl, cf);
                            if (Build.VERSION.SDK_INT > 17) {
                                XposedHelpers.callMethod(param.args[0], "computeFrameLw", pf, df, of, cf, vf);
                            } else {
                                XposedHelpers.callMethod(param.args[0], "computeFrameLw", pf, df, cf, vf);
                            }

                            if (DEBUG) log("layoutWindowLw recomputing frame");
                        }
                    }
                });
    
                if (Build.VERSION.SDK_INT > 17) {
                    XposedHelpers.findAndHookMethod(windowStateClass, "computeFrameLw",
                            Rect.class, Rect.class, Rect.class, Rect.class, Rect.class, windowStateComputeFrameLw);
                } else {
                    XposedHelpers.findAndHookMethod(windowStateClass, "computeFrameLw",
                            Rect.class, Rect.class, Rect.class, Rect.class, windowStateComputeFrameLw);
                }
    
                XposedHelpers.findAndHookMethod(windowManagerServiceClass, "adjustWallpaperWindowsLocked",
                        adjustWallpaperHook);
    
                XposedHelpers.findAndHookMethod(windowManagerServiceClass, "updateWallpaperOffsetLocked",
                        CLASS_WINDOW_STATE, boolean.class, adjustWallpaperHook);
            }

        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static XC_MethodHook windowStateComputeFrameLw = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            if (isNavbarTransparencyEnabled() && 
                    XposedHelpers.getBooleanField(param.thisObject, "mIsWallpaper")) {
                try {
                    final int width = getWallpaperWidth();
                    final int height = getWallpaperHeight();
                    if (width > 0 && height > 0) {
                        XposedHelpers.callMethod(
                                XposedHelpers.getObjectField(param.thisObject, "mService"),
                                "updateWallpaperOffsetLocked",
                                param.thisObject, width, height, false);
                        if (DEBUG) log("updateWallpaperOffsetLocked: width=" + width +
                                "; height=" + height);
                    }
                } catch (Throwable t) {
                    XposedBridge.log(t);
                }
            }
        }
    };

    private static XC_MethodHook adjustWallpaperHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
            if (!isNavbarTransparencyEnabled()) return;

            try {
                if (Build.VERSION.SDK_INT > 16) {
                    final Class<?> displayContentClass = 
                            XposedHelpers.findClass("com.android.server.wm.DisplayContent", null);
                    if (DEBUG) log ("adjustWallpaperWindowsLocked: hooking getDisplayInfo");
                    mDisplayContentHook = XposedHelpers.findAndHookMethod(displayContentClass, "getDisplayInfo",
                            new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param2) throws Throwable {
                            Object di = XposedHelpers.getObjectField(param2.thisObject, "mDisplayInfo");
                            final int width = getWallpaperWidth();
                            final int height = getWallpaperHeight();
                            if (width > 0 && height > 0) {
                                XposedHelpers.setIntField(di, "appWidth", width);
                                XposedHelpers.setIntField(di, "appHeight", height);
                                param.setResult(di);
                                if (DEBUG) log("adjustWallpaperWindowsLocked: getDisplayInfo appWidth=" + width 
                                        + "; appHeight=" + height);
                            }
                        }
                    });
                } else {
                    final int width = getWallpaperWidth();
                    final int height = getWallpaperHeight();
                    if (width > 0 && height > 0) {
                        int appWidth = XposedHelpers.getIntField(param.thisObject, "mAppDisplayWidth");
                        int appHeight = XposedHelpers.getIntField(param.thisObject, "mAppDisplayHeight");
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "mAppDisplayWidthOrig", appWidth);
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "mAppDisplayHeightOrig", appHeight);
                        XposedHelpers.setIntField(param.thisObject, "mAppDisplayWidth", width);
                        XposedHelpers.setIntField(param.thisObject, "mAppDisplayHeight", height);
                    }
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            if (!isNavbarTransparencyEnabled()) return;

            try {
                if (Build.VERSION.SDK_INT > 16) {
                    if (mDisplayContentHook != null) {
                        mDisplayContentHook.unhook();
                        mDisplayContentHook = null;
                        if (DEBUG) log ("adjustWallpaperWindowsLocked: unhooking getDisplayInfo");
                    }
                } else {
                    Integer appWidth = (Integer) XposedHelpers.getAdditionalInstanceField(
                            param.thisObject, "mAppDisplayWidthOrig");
                    Integer appHeight = (Integer) XposedHelpers.getAdditionalInstanceField(
                            param.thisObject, "mAppDisplayHeightOrig");
                    if (appWidth != null && appHeight != null) {
                        XposedHelpers.setIntField(param.thisObject, "mAppDisplayWidth", (int)appWidth);
                        XposedHelpers.setIntField(param.thisObject, "mAppDisplayHeight", (int)appHeight);
                    }
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static boolean isTransparencyEnabled() {
        return (isStatusbarTransparencyEnabled() ||
                isNavbarTransparencyEnabled());
    }

    private static boolean isStatusbarTransparencyEnabled() {
        return (TransparencyManager.isStatusbarEnabled(mTransparencyModePwm) &&
                (mTransparencyValuesPwm[0] != 0 || mTransparencyValuesPwm[1] != 0));
    }

    private static boolean isNavbarTransparencyEnabled() {
        return (TransparencyManager.isNavbarEnabled(mTransparencyModePwm) &&
                (mTransparencyValuesPwm[2] != 0 || mTransparencyValuesPwm[3] != 0));
    }

    private static int getWallpaperWidth() {
        if (mPhoneWindowManager != null) {
            final int left = XposedHelpers.getIntField(mPhoneWindowManager, "mUnrestrictedScreenLeft");
            final int width = XposedHelpers.getIntField(mPhoneWindowManager, "mUnrestrictedScreenWidth");
            return left + (left + width);
        }
        return 0;
    }

    private static int getWallpaperHeight() {
        if (mPhoneWindowManager != null) {
            final int top = XposedHelpers.getIntField(mPhoneWindowManager, "mUnrestrictedScreenTop");
            final int height = XposedHelpers.getIntField(mPhoneWindowManager, "mUnrestrictedScreenHeight");
            return top + (top + height);
        }
        return 0;
    }

    // in process hooks
    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> phoneStatusbarViewClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR_VIEW, classLoader);
            final Class<?> phoneStatusbarClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);
            final Class<?> signalClusterViewClass = XposedHelpers.findClass(CLASS_SIGNAL_CLUSTER_VIEW, classLoader);
            final Class<?> batteryControllerClass = XposedHelpers.findClass(CLASS_BATTERY_CONTROLLER, classLoader);
            final Class<?> notifPanelViewClass = Build.VERSION.SDK_INT > 16 ?
                    XposedHelpers.findClass(CLASS_NOTIF_PANEL_VIEW, classLoader) : null;
            final Class<?> statusbarIconViewClass = XposedHelpers.findClass(CLASS_STATUSBAR_ICON_VIEW, classLoader);

            mBroadcastSubReceivers = new ArrayList<BroadcastSubReceiver>();

            XposedBridge.hookAllConstructors(phoneStatusbarViewClass, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mPanelBar = (View) param.thisObject;

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_NOTIF_BACKGROUND_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_DISABLE_ROAMING_INDICATORS_CHANGED);
                    intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_BATTERY_CHARGED_SOUND_CHANGED);
                    mPanelBar.getContext().registerReceiver(mBroadcastReceiver, intentFilter);

                    Context gbContext = mPanelBar.getContext().createPackageContext(GravityBox.PACKAGE_NAME,
                            Context.CONTEXT_IGNORE_SECURITY);
                    mIconManager = new StatusBarIconManager(mPanelBar.getContext(), gbContext);
                    mIconManager.setIconColor(
                            prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_ICON_COLOR,
                                    mIconManager.getDefaultIconColor()));
                    try {
                        int iconStyle = Integer.valueOf(
                                prefs.getString(GravityBoxSettings.PREF_KEY_STATUS_ICON_STYLE, "0"));
                        mIconManager.setIconStyle(iconStyle);
                    } catch(NumberFormatException nfe) {
                        log("Invalid value for PREF_KEY_STATUS_ICON_STYLE preference");
                    }
                    mIconManager.setIconColor(1,
                            prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_ICON_COLOR_SECONDARY,
                                    mIconManager.getDefaultIconColor()));
                    mIconManager.setDataActivityColor(
                            prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR, 
                                    StatusBarIconManager.DEFAULT_DATA_ACTIVITY_COLOR));
                    mIconManager.setDataActivityColor(1,
                            prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR_SECONDARY, 
                                    StatusBarIconManager.DEFAULT_DATA_ACTIVITY_COLOR));
                    mIconManager.setFollowStockBatteryColor(prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_STATUSBAR_COLOR_FOLLOW_STOCK_BATTERY, false));
                    try {
                        int signalIconMode = Integer.valueOf(prefs.getString(
                                GravityBoxSettings.PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE, "0"));
                        mIconManager.setSignalIconMode(signalIconMode);
                    } catch (NumberFormatException nfe) {
                        log("Invalid value for PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE preference");
                    }
                    mIconManager.setSkipBatteryIcon(prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_STATUSBAR_COLOR_SKIP_BATTERY, false));
                    mIconManager.setColoringEnabled(prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE, false));
                    mIconManager.getBatteryInfoManager().setChargedSoundEnabled(prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_BATTERY_CHARGED_SOUND, false));
                    mIconManager.getBatteryInfoManager().setPluggedSoundEnabled(prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_CHARGER_PLUGGED_SOUND, false));
                    mBroadcastSubReceivers.add(mIconManager);
                }
            });

            XposedBridge.hookAllConstructors(signalClusterViewClass, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    LinearLayout view = (LinearLayout) param.thisObject;
                    mSignalCluster = StatusbarSignalCluster.create(view, mIconManager);
                    mSignalCluster.initPreferences(prefs);
                    mBroadcastSubReceivers.add(mSignalCluster);
                    if (DEBUG) log("SignalClusterView constructed - mSignalClusterView set");
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, 
                    "makeStatusBarView", new XC_MethodHook(XCallback.PRIORITY_LOWEST) {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    mPhoneStatusBar = param.thisObject;
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

                    int tmMode = TransparencyManager.MODE_FULL;
                    try {
                        tmMode = Integer.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_TM_MODE, "3"));
                    } catch (NumberFormatException nfe) {
                        log("Invalid value for PREF_KEY_TM_MODE preference");
                    }
                    if (tmMode != TransparencyManager.MODE_DISABLED) {
                        mTransparencyManager = new TransparencyManager(context, tmMode);
                        mTransparencyManager.setStatusbar(XposedHelpers.getObjectField(param.thisObject, "mStatusBarView"));
                        mTransparencyManager.setNavbar(XposedHelpers.getObjectField(param.thisObject, "mNavigationBarView"));
                        mTransparencyManager.initPreferences(prefs);
                        mBroadcastSubReceivers.add(mTransparencyManager);
                    }

                    mBatteryController = XposedHelpers.getObjectField(param.thisObject, "mBatteryController");
                    int bgColor = prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_BGCOLOR, Color.BLACK);
                    setStatusbarBgColor(bgColor);
                    if (mIconManager != null) {
                        mIconManager.registerListener(mIconManagerListener);
                    }

                    Intent i = new Intent(ACTION_PHONE_STATUSBAR_VIEW_MADE);
                    context.sendBroadcast(i);
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, "getNavigationBarLayoutParams", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    WindowManager.LayoutParams lp = (WindowManager.LayoutParams) param.getResult();
                    if (lp != null) {
                        lp.format = PixelFormat.TRANSLUCENT;
                        param.setResult(lp);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, "disable", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mTransparencyManager != null) {
                        mTransparencyManager.update();
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, "topAppWindowChanged",
                    boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mTransparencyManager != null) {
                        mTransparencyManager.update();
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, "setStatusBarLowProfile",
                    boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final boolean lightsOut = (Boolean) param.args[0];
                    if (mIconManager != null) {
                        mIconManager.setLowProfile(lightsOut);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(batteryControllerClass, "onReceive",
                    Context.class, Intent.class, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.args[1];
                    if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                        mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                        if (Build.VERSION.SDK_INT > 17) {
                            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 
                                    BatteryManager.BATTERY_STATUS_UNKNOWN);
                            mBatteryPlugged = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                    status == BatteryManager.BATTERY_STATUS_FULL);
                        } else {
                            mBatteryPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
                        }
                        if (mIconManager != null && mIconManager.isColoringEnabled() && 
                                !mIconManager.shouldSkipBatteryIcon() && mBattery != null &&
                                (mBattery instanceof ImageView)) {
                            Drawable d = mIconManager.getBatteryIcon(mBatteryLevel, mBatteryPlugged);
                            if (d != null) {
                                ((ImageView)mBattery).setImageDrawable(d);
                            }
                        }
                    }
                }
            });

            if (notifPanelViewClass != null) {
                XposedHelpers.findAndHookMethod(notifPanelViewClass, "onFinishInflate", new XC_MethodHook() {
    
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ViewGroup vg = (ViewGroup) param.thisObject;
                        int resId = vg.getResources().getIdentifier("exclude_close_handle", "id", PACKAGE_NAME);
                        ViewGroup ech = (ViewGroup) vg.findViewById(resId);

                        NotificationWallpaper nw = 
                                new NotificationWallpaper((resId == 0) ? vg : ech, prefs);
                        mBroadcastSubReceivers.add(nw);
                    }
                });
            }

            XposedHelpers.findAndHookMethod(statusbarIconViewClass, "getIcon",
                    CLASS_STATUSBAR_ICON, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (mIconManager != null && mIconManager.isColoringEnabled()) {
                        final String iconPackage = 
                                (String) XposedHelpers.getObjectField(param.args[0], "iconPackage");
                        if (DEBUG) log("statusbarIconView.getIcon: iconPackage=" + iconPackage);
                        if (iconPackage == null || iconPackage.equals(PACKAGE_NAME)) {
                            final int iconId = XposedHelpers.getIntField(param.args[0], "iconId");
                            Drawable d = mIconManager.getBasicIcon(iconId);
                            if (d != null) {
                                param.setResult(d);
                                return;
                            }
                        }
                    }
                }
            });

        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void setStatusbarBgColor(int color) {
        if (mPanelBar != null) {
            if (!Utils.isXperiaDevice()) {
                if (!(mPanelBar.getBackground() instanceof BackgroundAlphaColorDrawable)) {
                    BackgroundAlphaColorDrawable colorDrawable = new BackgroundAlphaColorDrawable(color);
                    mPanelBar.setBackground(colorDrawable);
                    if (DEBUG) log("statusbar view backround replaced with BackgroundAlphaColorDrawable");
                } else {
                    ((BackgroundAlphaColorDrawable) mPanelBar.getBackground()).setBgColor(color);
                }
                if (DEBUG) log("statusbar background color set to: " + color);
            }
        }
    }

    private static IconManagerListener mIconManagerListener = new IconManagerListener() {
        @Override
        public void onIconManagerStatusChanged(int flags, ColorInfo colorInfo) {
            final boolean updateBattery = (flags & 
                    (StatusBarIconManager.FLAG_ICON_COLOR_CHANGED |
                            StatusBarIconManager.FLAG_SKIP_BATTERY_ICON_CHANGED)) != 0;
            final boolean updateStatusIcons = (flags & 
                    (StatusBarIconManager.FLAG_ICON_COLOR_CHANGED |
                            StatusBarIconManager.FLAG_ICON_STYLE_CHANGED)) != 0;
            if (updateBattery) {
                updateBattery();
            }
            if (updateStatusIcons) {
                updateStatusIcons();
            }
        }
    };

    private static void updateBattery() {
        if (mBatteryController != null && mBattery != null) {
            Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
            intent.putExtra(BatteryManager.EXTRA_LEVEL, mBatteryLevel);
            if (Build.VERSION.SDK_INT > 17) {
                intent.putExtra(BatteryManager.EXTRA_STATUS, mBatteryPlugged ? 
                        BatteryManager.BATTERY_STATUS_CHARGING :
                            BatteryManager.BATTERY_STATUS_UNKNOWN);
            } else {
                intent.putExtra(BatteryManager.EXTRA_PLUGGED, mBatteryPlugged ? 1 : 0);
            }
            try {
                XposedHelpers.callMethod(mBatteryController, "onReceive", mBattery.getContext(), intent);
            } catch (Throwable t) {
                log("Incompatible battery controller: " + t.getMessage());
            }
        }
    }

    private static void updateStatusIcons() {
        if (mPhoneStatusBar == null) return;
        try {
            ViewGroup vg = (ViewGroup) XposedHelpers.getObjectField(mPhoneStatusBar, "mStatusIcons");
            final int childCount = vg.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (!vg.getChildAt(i).getClass().getName().equals(CLASS_STATUSBAR_ICON_VIEW)) {
                    continue;
                }
                ImageView v = (ImageView) vg.getChildAt(i);
                final Object sbIcon = XposedHelpers.getObjectField(v, "mIcon");
                if (sbIcon != null) {
                    final String iconPackage =
                            (String) XposedHelpers.getObjectField(sbIcon, "iconPackage");
                    if (iconPackage == null || iconPackage.equals(PACKAGE_NAME)) {
                        final int resId = XposedHelpers.getIntField(sbIcon, "iconId");
                        Drawable d = mIconManager.getBasicIcon(resId);
                        if (d != null) {
                            v.setImageDrawable(d);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
