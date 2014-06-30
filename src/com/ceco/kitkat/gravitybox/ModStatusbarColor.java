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
import java.util.ArrayList;
import java.util.List;

import com.ceco.kitkat.gravitybox.StatusBarIconManager.ColorInfo;
import com.ceco.kitkat.gravitybox.StatusBarIconManager.IconManagerListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XCallback;

public class ModStatusbarColor {
    private static final String TAG = "GB:ModStatusbarColor";
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String CLASS_PHONE_STATUSBAR_VIEW = "com.android.systemui.statusbar.phone.PhoneStatusBarView";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final String CLASS_SIGNAL_CLUSTER_VIEW = Utils.hasGeminiSupport() ? 
            "com.android.systemui.statusbar.SignalClusterViewGemini" :
            "com.android.systemui.statusbar.SignalClusterView";
    private static final String CLASS_NOTIF_PANEL_VIEW = "com.android.systemui.statusbar.phone.NotificationPanelView";
    private static final String CLASS_STATUSBAR_ICON_VIEW = "com.android.systemui.statusbar.StatusBarIconView";
    private static final String CLASS_STATUSBAR_ICON = "com.android.internal.statusbar.StatusBarIcon";
    private static final String CLASS_SB_TRANSITIONS = "com.android.systemui.statusbar.phone.PhoneStatusBarTransitions";
    private static final String CLASS_BAR_TRANSITIONS = "com.android.systemui.statusbar.phone.BarTransitions";
    private static final boolean DEBUG = false;

    public static final String ACTION_PHONE_STATUSBAR_VIEW_MADE = "gravitybox.intent.action.PHONE_STATUSBAR_VIEW_MADE";

    private static View mPanelBar;
    private static StatusBarIconManager mIconManager;
    private static List<BroadcastSubReceiver> mBroadcastSubReceivers;
    private static Object mPhoneStatusBar;
    private static StatusbarSignalCluster mSignalCluster;
    private static int mStatusbarBgColor;
    private static Object mBarBackground;
    private static Integer mStatusbarBgColorOriginal;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void registerIconManagerListener(IconManagerListener listener) {
        if (mIconManager != null) {
            mIconManager.registerListener(listener);
        }
    }

    public static void unregisterIconManagerListener(IconManagerListener listener) {
        if (mIconManager != null) {
            mIconManager.unregisterListener(listener);
        }
    }

    public static StatusBarIconManager getIconManager() {
        return mIconManager;
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("received broadcast: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_STATUSBAR_COLOR_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_BG_COLOR)) {
                    mStatusbarBgColor = intent.getIntExtra(GravityBoxSettings.EXTRA_SB_BG_COLOR, Color.BLACK);
                    setStatusbarBgColor();
                }
            }

            for (BroadcastSubReceiver bsr : mBroadcastSubReceivers) {
                bsr.onBroadcastReceived(context, intent);
            }
        }
    };

    // in process hooks
    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> phoneStatusbarViewClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR_VIEW, classLoader);
            final Class<?> phoneStatusbarClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);
            final Class<?> signalClusterViewClass = XposedHelpers.findClass(CLASS_SIGNAL_CLUSTER_VIEW, classLoader);
            final Class<?> notifPanelViewClass = XposedHelpers.findClass(CLASS_NOTIF_PANEL_VIEW, classLoader);
            final Class<?> statusbarIconViewClass = XposedHelpers.findClass(CLASS_STATUSBAR_ICON_VIEW, classLoader);
            final Class<?> sbTransitionsClass = XposedHelpers.findClass(CLASS_SB_TRANSITIONS, classLoader);
            final Class<?> barTransitionsClass = XposedHelpers.findClass(CLASS_BAR_TRANSITIONS, classLoader);

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
                                prefs.getString(GravityBoxSettings.PREF_KEY_STATUS_ICON_STYLE, "1"));
                        mIconManager.setIconStyle(iconStyle);
                    } catch(NumberFormatException nfe) {
                        log("Invalid value for PREF_KEY_STATUS_ICON_STYLE preference");
                    }
                    mIconManager.setIconColor(1,
                            prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_ICON_COLOR_SECONDARY,
                                    mIconManager.getDefaultIconColor()));
                    mIconManager.setDataActivityColor(
                            prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR, 
                                    gbContext.getResources().getInteger(
                                            R.integer.signal_cluster_data_activity_icon_color)));
                    mIconManager.setDataActivityColor(1,
                            prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR_SECONDARY, 
                                    gbContext.getResources().getInteger(
                                            R.integer.signal_cluster_data_activity_icon_color)));
                    try {
                        int signalIconMode = Integer.valueOf(prefs.getString(
                                GravityBoxSettings.PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE, "1"));
                        mIconManager.setSignalIconMode(signalIconMode);
                    } catch (NumberFormatException nfe) {
                        log("Invalid value for PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE preference");
                    }
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
                    if (mSignalCluster == null) {
                        mSignalCluster = StatusbarSignalCluster.create(view, mIconManager, prefs);
                        mBroadcastSubReceivers.add(mSignalCluster);
                        if (DEBUG) log("SignalClusterView constructed - mSignalClusterView set");
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, 
                    "makeStatusBarView", new XC_MethodHook(XCallback.PRIORITY_LOWEST) {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    mPhoneStatusBar = param.thisObject;
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

                    mStatusbarBgColor = prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_BGCOLOR, Color.BLACK);
                    if (mIconManager != null) {
                        mIconManager.registerListener(mIconManagerListener);
                    }
                    setStatusbarBgColor();

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

            XposedHelpers.findAndHookMethod(notifPanelViewClass, "onFinishInflate", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    NotificationWallpaper nw = 
                            new NotificationWallpaper((FrameLayout) param.thisObject, prefs);
                    mBroadcastSubReceivers.add(nw);
                }
            });

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

            XposedHelpers.findAndHookMethod(sbTransitionsClass, "applyMode",
                    int.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mIconManager != null) {
                        final float signalClusterAlpha = (Float) XposedHelpers.callMethod(
                                param.thisObject, "getNonBatteryClockAlphaFor", (Integer) param.args[0]);
                        final float textAndBatteryAlpha = (Float) XposedHelpers.callMethod(
                                param.thisObject, "getBatteryClockAlpha", (Integer) param.args[0]);
                        mIconManager.setIconAlpha(signalClusterAlpha, textAndBatteryAlpha);
                    }
                }
            });

            XposedBridge.hookAllConstructors(barTransitionsClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (XposedHelpers.getObjectField(param.thisObject, "mView") == mPanelBar) {
                        try {
                            Field barBg = XposedHelpers.findField(param.thisObject.getClass(), "mBarBackground");
                            mBarBackground = barBg.get(param.thisObject);
                            if (DEBUG) log("BarTransitions appear to be Android 4.4.1");
                        } catch (NoSuchFieldError nfe) {
                            mBarBackground = param.thisObject;
                            if (DEBUG) log("BarTransitions appear to be Android 4.4");
                        }
                        
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void setStatusbarBgColor() {
        if (mPanelBar == null || 
                mBarBackground == null ||
                mIconManager == null) return;

        try {
            if (mStatusbarBgColorOriginal == null) {
                mStatusbarBgColorOriginal = XposedHelpers.getIntField(mBarBackground, "mOpaque");
                if (DEBUG) log("Saved original statusbar background color");
            }
            int bgColor = mIconManager.isColoringEnabled() ? 
                    mStatusbarBgColor : mStatusbarBgColorOriginal;
            XposedHelpers.setIntField(mBarBackground, "mOpaque", bgColor);
            if (mBarBackground instanceof Drawable) {
                ((Drawable) mBarBackground).invalidateSelf();
            } else {
                final Object barTransitions = XposedHelpers.getObjectField(mPanelBar, "mBarTransitions");
                final int currentMode = (Integer) XposedHelpers.callMethod(barTransitions, "getMode");
                XposedHelpers.callMethod(barTransitions, "applyModeBackground", -1, currentMode, false);
            }
        } catch (Throwable t) {
            log("Error setting statusbar background color: " + t.getMessage());
        }
    }

    private static IconManagerListener mIconManagerListener = new IconManagerListener() {
        @Override
        public void onIconManagerStatusChanged(int flags, ColorInfo colorInfo) {
            if ((flags & (StatusBarIconManager.FLAG_ICON_COLOR_CHANGED |
                    StatusBarIconManager.FLAG_ICON_STYLE_CHANGED)) != 0) {
                updateStatusIcons();
            }
            if ((flags & StatusBarIconManager.FLAG_COLORING_ENABLED_CHANGED) != 0) {
                setStatusbarBgColor();
            }
        }
    };

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
