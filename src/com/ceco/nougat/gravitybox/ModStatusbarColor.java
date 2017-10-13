/*
 * Copyright (C) 2016 Peter Gregus for GravityBox Project (C3C076@xda)
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

package com.ceco.nougat.gravitybox;

import com.ceco.nougat.gravitybox.managers.StatusBarIconManager;
import com.ceco.nougat.gravitybox.managers.SysUiManagers;
import com.ceco.nougat.gravitybox.managers.StatusBarIconManager.ColorInfo;
import com.ceco.nougat.gravitybox.managers.StatusBarIconManager.IconManagerListener;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XCallback;

public class ModStatusbarColor {
    private static final String TAG = "GB:ModStatusbarColor";
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final String CLASS_STATUSBAR_ICON_VIEW = "com.android.systemui.statusbar.StatusBarIconView";
    private static final String CLASS_STATUSBAR_ICON = "com.android.internal.statusbar.StatusBarIcon";
    private static final String CLASS_SB_TRANSITIONS = "com.android.systemui.statusbar.phone.PhoneStatusBarTransitions";
    private static final String CLASS_SB_ICON_CTRL = "com.android.systemui.statusbar.phone.StatusBarIconController";
    private static final boolean DEBUG = false;

    public static final String ACTION_PHONE_STATUSBAR_VIEW_MADE = "gravitybox.intent.action.PHONE_STATUSBAR_VIEW_MADE";

    private static Object mPhoneStatusBar;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    // in process hooks
    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> phoneStatusbarClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);
            final Class<?> statusbarIconViewClass = XposedHelpers.findClass(CLASS_STATUSBAR_ICON_VIEW, classLoader);
            final Class<?> sbTransitionsClass = XposedHelpers.findClass(CLASS_SB_TRANSITIONS, classLoader);

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, 
                    "makeStatusBarView", new XC_MethodHook(XCallback.PRIORITY_LOWEST) {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
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
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (SysUiManagers.IconManager != null && SysUiManagers.IconManager.isColoringEnabled() &&
                            XposedHelpers.getObjectField(param.thisObject, "mNotification") == null) {
                        final String iconPackage = 
                                (String) XposedHelpers.getObjectField(param.args[0], "pkg");
                        if (DEBUG) log("statusbarIconView.getIcon: iconPackage=" + iconPackage);
                        Drawable d = getColoredDrawable(((View)param.thisObject).getContext(),
                                iconPackage, (Icon) XposedHelpers.getObjectField(param.args[0], "icon"));
                        if (d != null) {
                            param.setResult(d);
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

    private static Drawable getColoredDrawable(Context ctx, String pkg, Icon icon) {
        if (icon == null) return null;

        Drawable d = null;
        if (pkg == null || PACKAGE_NAME.equals(pkg)) {
            final int iconId = (int) XposedHelpers.callMethod(icon, "getResId");
            d = SysUiManagers.IconManager.getBasicIcon(iconId);
            if (d != null) {
                return d;
            }
        }
        d = icon.loadDrawable(ctx);
        if (d != null) {
            if (SysUiManagers.IconManager.isColoringEnabled()) {
                d = SysUiManagers.IconManager.applyColorFilter(d.mutate(),
                        PorterDuff.Mode.SRC_IN);
            } else {
                d.clearColorFilter();
            }
        }
        return d;
    }

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
                    Drawable d = getColoredDrawable(v.getContext(), iconPackage,
                            (Icon) XposedHelpers.getObjectField(sbIcon, "icon"));
                    if (d != null) {
                        v.setImageDrawable(d);
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
            ImageView settingsButton = (ImageView) XposedHelpers.getObjectField(
                    header, Utils.isSamsungRom() ? "mSettingButton" : "mSettingsButton");
            if (SysUiManagers.IconManager.isColoringEnabled()) {
                settingsButton.setColorFilter(SysUiManagers.IconManager.getIconColor(),
                        PorterDuff.Mode.SRC_IN);
            } else {
                settingsButton.clearColorFilter();
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
