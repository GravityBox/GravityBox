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

package com.ceco.marshmallow.gravitybox;

import java.util.ArrayList;
import java.util.List;

import com.ceco.marshmallow.gravitybox.managers.StatusBarIconManager;
import com.ceco.marshmallow.gravitybox.managers.SysUiManagers;
import com.ceco.marshmallow.gravitybox.managers.StatusBarIconManager.ColorInfo;
import com.ceco.marshmallow.gravitybox.managers.StatusBarIconManager.IconManagerListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
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
    private static final String CLASS_STATUSBAR_ICON_VIEW = "com.android.systemui.statusbar.StatusBarIconView";
    private static final String CLASS_STATUSBAR_ICON = "com.android.internal.statusbar.StatusBarIcon";
    private static final String CLASS_SB_TRANSITIONS = "com.android.systemui.statusbar.phone.PhoneStatusBarTransitions";
    private static final String CLASS_SB_ICON_CTRL = "com.android.systemui.statusbar.phone.StatusBarIconController";
    private static final boolean DEBUG = false;

    public static final String ACTION_PHONE_STATUSBAR_VIEW_MADE = "gravitybox.intent.action.PHONE_STATUSBAR_VIEW_MADE";

    private static View mPanelBar;
    private static List<BroadcastSubReceiver> mBroadcastSubReceivers;
    private static Object mPhoneStatusBar;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("received broadcast: " + intent.toString());

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
            final Class<?> statusbarIconViewClass = XposedHelpers.findClass(CLASS_STATUSBAR_ICON_VIEW, classLoader);
            final Class<?> sbTransitionsClass = XposedHelpers.findClass(CLASS_SB_TRANSITIONS, classLoader);

            mBroadcastSubReceivers = new ArrayList<BroadcastSubReceiver>();

            XposedBridge.hookAllConstructors(phoneStatusbarViewClass, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mPanelBar = (View) param.thisObject;

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_DISABLE_ROAMING_INDICATORS_CHANGED);
                    intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_BATTERY_SOUND_CHANGED);
                    mPanelBar.getContext().registerReceiver(mBroadcastReceiver, intentFilter);
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, 
                    "makeStatusBarView", new XC_MethodHook(XCallback.PRIORITY_LOWEST) {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    mPhoneStatusBar = param.thisObject;
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

                    if (SysUiManagers.IconManager != null) {
                        SysUiManagers.IconManager.registerListener(mIconManagerListener);
                    }

                    Intent i = new Intent(ACTION_PHONE_STATUSBAR_VIEW_MADE);
                    context.sendBroadcast(i);
                }
            });

            XposedHelpers.findAndHookMethod(statusbarIconViewClass, "getIcon",
                    CLASS_STATUSBAR_ICON, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (SysUiManagers.IconManager != null && SysUiManagers.IconManager.isColoringEnabled()) {
                        final String iconPackage = 
                                (String) XposedHelpers.getObjectField(param.args[0], "pkg");
                        if (DEBUG) log("statusbarIconView.getIcon: iconPackage=" + iconPackage);
                        if (iconPackage == null || iconPackage.equals(PACKAGE_NAME)) {
                            final Icon icon = (Icon) XposedHelpers.getObjectField(param.args[0], "icon");
                            final int iconId = (int) XposedHelpers.callMethod(icon, "getResId");
                            Drawable d = SysUiManagers.IconManager.getBasicIcon(iconId);
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
                    if (SysUiManagers.IconManager != null) {
                        final float signalClusterAlpha = (Float) XposedHelpers.callMethod(
                                param.thisObject, "getNonBatteryClockAlphaFor", (Integer) param.args[0]);
                        final float textAndBatteryAlpha = (Float) XposedHelpers.callMethod(
                                param.thisObject, "getBatteryClockAlpha", (Integer) param.args[0]);
                        SysUiManagers.IconManager.setIconAlpha(signalClusterAlpha, textAndBatteryAlpha);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(CLASS_SB_ICON_CTRL, classLoader, "applyIconTint", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (SysUiManagers.IconManager != null) {
                        SysUiManagers.IconManager.setIconTint(
                                XposedHelpers.getIntField(param.thisObject, "mIconTint"));
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static IconManagerListener mIconManagerListener = new IconManagerListener() {
        @Override
        public void onIconManagerStatusChanged(int flags, ColorInfo colorInfo) {
            if ((flags & (StatusBarIconManager.FLAG_ICON_COLOR_CHANGED |
                    StatusBarIconManager.FLAG_ICON_STYLE_CHANGED)) != 0) {
                updateStatusIcons("mStatusIcons");
                updateStatusIcons("mStatusIconsKeyguard");
                updateSettingsButton();
            }
        }
    };

    private static void updateStatusIcons(String statusIcons) {
        if (mPhoneStatusBar == null) return;
        try {
            Object icCtrl = XposedHelpers.getObjectField(mPhoneStatusBar, "mIconController");
            ViewGroup vg = (ViewGroup) XposedHelpers.getObjectField(icCtrl, statusIcons);
            final int childCount = vg.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (!vg.getChildAt(i).getClass().getName().equals(CLASS_STATUSBAR_ICON_VIEW)) {
                    continue;
                }
                ImageView v = (ImageView) vg.getChildAt(i);
                final Object sbIcon = XposedHelpers.getObjectField(v, "mIcon");
                if (sbIcon != null) {
                    final String iconPackage =
                            (String) XposedHelpers.getObjectField(sbIcon, "pkg");
                    if (iconPackage == null || iconPackage.equals(PACKAGE_NAME)) {
                        final Icon icon = (Icon) XposedHelpers.getObjectField(sbIcon, "icon");
                        final int resId = (int) XposedHelpers.callMethod(icon, "getResId");
                        Drawable d = null;
                        if (SysUiManagers.IconManager != null) {
                            d = SysUiManagers.IconManager.getBasicIcon(resId);
                        }
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

    private static void updateSettingsButton() {
        if (mPhoneStatusBar == null || SysUiManagers.IconManager == null) return;
        try {
            Object header = XposedHelpers.getObjectField(mPhoneStatusBar, "mHeader");
			if (Utils.isSamsungRom()) {
				ImageView settingsButton = (ImageView) XposedHelpers.getObjectField(header, "mSettingButton");
				if (SysUiManagers.IconManager.isColoringEnabled()) {
					settingsButton.setColorFilter(SysUiManagers.IconManager.getIconColor(), PorterDuff.Mode.SRC_IN);
				} else {
					settingsButton.clearColorFilter();
				}
			} else {
				ImageButton settingsButton = (ImageButton) XposedHelpers.getObjectField(header, "mSettingsButton");
				if (SysUiManagers.IconManager.isColoringEnabled()) {
					settingsButton.setColorFilter(SysUiManagers.IconManager.getIconColor(), PorterDuff.Mode.SRC_IN);
				} else {
					settingsButton.clearColorFilter();
				}
			}
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
