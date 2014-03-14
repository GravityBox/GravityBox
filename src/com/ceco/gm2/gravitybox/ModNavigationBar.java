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
import java.util.Collections;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ImageView.ScaleType;
import android.widget.Space;

import com.ceco.gm2.gravitybox.GlowPadHelper.AppInfo;
import com.ceco.gm2.gravitybox.GlowPadHelper.BgStyle;
import com.ceco.gm2.gravitybox.shortcuts.ShortcutActivity;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModNavigationBar {
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String TAG = "GB:ModNavigationBar";
    private static final boolean DEBUG = false;

    private static final String CLASS_NAVBAR_VIEW = "com.android.systemui.statusbar.phone.NavigationBarView";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final String CLASS_KEY_BUTTON_VIEW = "com.android.systemui.statusbar.policy.KeyButtonView";
    private static final String CLASS_SEARCH_PANEL_VIEW = "com.android.systemui.SearchPanelView";
    private static final String CLASS_GLOWPAD_TRIGGER_LISTENER = CLASS_SEARCH_PANEL_VIEW + "$GlowPadTriggerListener";
    private static final String CLASS_PHONE_WINDOW_MANAGER = "com.android.internal.policy.impl.PhoneWindowManager";
    private static final String CLASS_GLOWPAD_VIEW = "com.android.internal.widget.multiwaveview.GlowPadView";

    private static final int NAVIGATION_HINT_BACK_ALT = 1 << 3;
    private static final int STATUS_BAR_DISABLE_RECENT = 0x01000000;

    private static XSharedPreferences mPrefs;
    private static boolean mAlwaysShowMenukey;
    private static View mNavigationBarView;
    private static Object[] mRecentsKeys;
    private static HomeKeyInfo[] mHomeKeys;
    private static int mRecentsSingletapAction = 0;
    private static int mRecentsLongpressAction = 0;
    private static int mHomeLongpressAction = 0;
    private static boolean mHwKeysEnabled;
    private static boolean mCursorControlEnabled;
    private static boolean mDpadKeysVisible;
    private static boolean mAlwaysOnBottom;
    private static boolean mNavbarVertical;
    private static boolean mNavbarRingDisabled;

    // Custom key
    private static boolean mCustomKeyEnabled;
    private static Resources mResources;
    private static Context mGbContext;
    private static NavbarViewInfo[] mNavbarViewInfo = new NavbarViewInfo[2];
    private static boolean mCustomKeySwapEnabled;

    // Colors
    private static boolean mNavbarColorsEnabled;
    private static int mKeyDefaultColor = 0xe8ffffff;
    private static int mKeyDefaultGlowColor = 0x40ffffff;
    private static int mNavbarDefaultBgColor = 0xff000000;
    private static int mKeyColor;
    private static int mKeyGlowColor;
    private static int mNavbarBgColor;
    private static Integer mNavbarBgColorOriginal;

    // Ring targets
    private static enum RingHapticFeedback { DEFAULT, ENABLED, DISABLED };
    private static boolean mRingTargetsEnabled;
    private static View mGlowPadView;
    private static Class<? extends View> mGlowPadViewClass;
    private static BgStyle mRingTargetsBgStyle;
    private static RingHapticFeedback mRingHapticFeedback;
    private static Integer mRingVibrateDurationOrig;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    static class HomeKeyInfo {
        public ImageView homeKey;
        public boolean supportsLongPressDefault;
    }

    static class NavbarViewInfo {
        ViewGroup navButtons;
        View originalView;
        KeyButtonView customKey;
        KeyButtonView dpadLeft;
        KeyButtonView dpadRight;
        int customKeyPosition;
        boolean visible;
        boolean menuCustomSwapped;
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("Broadcast received: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_NAVBAR_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_MENUKEY)) {
                    mAlwaysShowMenukey = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_MENUKEY, false);
                    if (DEBUG) log("mAlwaysShowMenukey = " + mAlwaysShowMenukey);
                    setMenuKeyVisibility();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_CUSTOM_KEY_ENABLE)) {
                    mCustomKeyEnabled = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_CUSTOM_KEY_ENABLE, false);
                    setCustomKeyVisibility();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_KEY_COLOR)) {
                    mKeyColor = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_KEY_COLOR, mKeyDefaultColor);
                    setKeyColor();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_KEY_GLOW_COLOR)) {
                    mKeyGlowColor = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_KEY_GLOW_COLOR, mKeyDefaultGlowColor);
                    setKeyColor();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_BG_COLOR)) {
                    mNavbarBgColor = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_BG_COLOR, mNavbarDefaultBgColor);
                    setNavbarBgColor();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_COLOR_ENABLE)) {
                    mNavbarColorsEnabled = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_COLOR_ENABLE, false);
                    setNavbarBgColor();
                    setKeyColor();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_CURSOR_CONTROL)) {
                    mCursorControlEnabled = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_CURSOR_CONTROL, false);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_CUSTOM_KEY_SWAP)) {
                    mCustomKeySwapEnabled = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_CUSTOM_KEY_SWAP, false);
                    setCustomKeyVisibility();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_RING_DISABLE)) {
                    mNavbarRingDisabled = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_RING_DISABLE, false);
                }
            } else if (intent.getAction().equals(
                    GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_SINGLETAP_CHANGED)) {
                mRecentsSingletapAction = intent.getIntExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE, 0);
                updateRecentsKeyCode();
            } else if (intent.getAction().equals(
                    GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_LONGPRESS_CHANGED)) {
                mRecentsLongpressAction = intent.getIntExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE, 0);
                updateRecentsKeyCode();
            } else if (intent.getAction().equals(
                    GravityBoxSettings.ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED)) {
                mHomeLongpressAction = intent.getIntExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE, 0);
                updateHomeKeyLongpressSupport();
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_PIE_CHANGED) && 
                    intent.hasExtra(GravityBoxSettings.EXTRA_PIE_HWKEYS_DISABLE)) {
                mHwKeysEnabled = !intent.getBooleanExtra(GravityBoxSettings.EXTRA_PIE_HWKEYS_DISABLE, false);
                updateRecentsKeyCode();
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_NAVBAR_SWAP_KEYS)) {
                swapBackAndRecents();
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_NAVBAR_RING_TARGET_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_RING_TARGET_INDEX) &&
                        intent.hasExtra(GravityBoxSettings.EXTRA_RING_TARGET_APP)) {
                    updateRingTarget(intent.getIntExtra(GravityBoxSettings.EXTRA_RING_TARGET_INDEX, -1),
                            intent.getStringExtra(GravityBoxSettings.EXTRA_RING_TARGET_APP));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_RING_TARGET_BG_STYLE)) {
                    mRingTargetsBgStyle = BgStyle.valueOf(
                            intent.getStringExtra(GravityBoxSettings.EXTRA_RING_TARGET_BG_STYLE));
                    GlowPadHelper.clearAppInfoCache();
                    setRingTargets();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_RING_HAPTIC_FEEDBACK)) {
                    mRingHapticFeedback = RingHapticFeedback.valueOf(
                            intent.getStringExtra(GravityBoxSettings.EXTRA_RING_HAPTIC_FEEDBACK));
                    setRingHapticFeedback();
                }
            }
        }
    };

    public static void initZygote(final XSharedPreferences prefs) {
        try {
            if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_ALWAYS_ON_BOTTOM, false)) {
                final Class<?> phoneWindowManagerClass = XposedHelpers.findClass(CLASS_PHONE_WINDOW_MANAGER, null);

                XposedHelpers.findAndHookMethod(phoneWindowManagerClass, "setInitialDisplaySize",
                        Display.class, int.class, int.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.setBooleanField(param.thisObject, "mNavigationBarCanMove", false);
                    }
                });

                XposedHelpers.findAndHookMethod(Resources.class, "loadXmlResourceParser",
                        String.class, int.class, int.class, String.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        final String originalResPath = (String) param.args[0];
                        try {
                            if ("res/layout/navigation_bar.xml".equals(param.args[0])) {
                                param.args[0] = "res/layout-sw600dp/navigation_bar.xml";
                            } else if ("res/layout/status_bar_search_panel.xml".equals(param.args[0]) ||
                                    "res/layout-land/status_bar_search_panel.xml".equals(param.args[0])) {
                                param.args[0] = "res/layout-sw600dp/status_bar_search_panel.xml";
                            }
                            return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                        } catch (Throwable t) {
                            log("loadXmlResourceParser throwing exception. Invoking original method.");
                            param.args[0] = originalResPath;
                            return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            mPrefs = prefs;

            final Class<?> navbarViewClass = XposedHelpers.findClass(CLASS_NAVBAR_VIEW, classLoader);
            final Class<?> phoneStatusbarClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);

            mAlwaysShowMenukey = prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_MENUKEY, false);
            mRingTargetsEnabled = prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_RING_TARGETS_ENABLE, false);

            try {
                mRecentsSingletapAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_RECENTS_SINGLETAP, "0"));
                mRecentsLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_RECENTS_LONGPRESS, "0"));
                mHomeLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_HOME_LONGPRESS, "0"));
            } catch (NumberFormatException nfe) {
                XposedBridge.log(nfe);
            }

            mCustomKeyEnabled = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_NAVBAR_CUSTOM_KEY_ENABLE, false);
            mHwKeysEnabled = !prefs.getBoolean(GravityBoxSettings.PREF_KEY_HWKEYS_DISABLE, false);
            mCursorControlEnabled = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_NAVBAR_CURSOR_CONTROL, false);
            mAlwaysOnBottom = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_NAVBAR_ALWAYS_ON_BOTTOM, false);
            mCustomKeySwapEnabled = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_NAVBAR_CUSTOM_KEY_SWAP, false);
            mNavbarRingDisabled = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_NAVBAR_RING_DISABLE, false);

            XposedBridge.hookAllConstructors(navbarViewClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    if (context == null) return;

                    mResources = context.getResources();

                    mGbContext = context.createPackageContext(
                            GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
                    final Resources res = mGbContext.getResources();
                    mNavbarColorsEnabled = prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_COLOR_ENABLE, false);
                    mKeyDefaultColor = res.getColor(R.color.navbar_key_color);
                    mKeyColor = prefs.getInt(GravityBoxSettings.PREF_KEY_NAVBAR_KEY_COLOR, mKeyDefaultColor);
                    mKeyDefaultGlowColor = res.getColor(R.color.navbar_key_glow_color);
                    mKeyGlowColor = prefs.getInt(
                            GravityBoxSettings.PREF_KEY_NAVBAR_KEY_GLOW_COLOR, mKeyDefaultGlowColor);
                    mNavbarDefaultBgColor = res.getColor(R.color.navbar_bg_color);
                    mNavbarBgColor = prefs.getInt(
                            GravityBoxSettings.PREF_KEY_NAVBAR_BG_COLOR, mNavbarDefaultBgColor);

                    mNavigationBarView = (View) param.thisObject;
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_NAVBAR_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_SINGLETAP_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_LONGPRESS_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_PIE_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_NAVBAR_SWAP_KEYS);
                    if (mRingTargetsEnabled) {
                        intentFilter.addAction(GravityBoxSettings.ACTION_PREF_NAVBAR_RING_TARGET_CHANGED);
                    }
                    context.registerReceiver(mBroadcastReceiver, intentFilter);
                    if (DEBUG) log("NavigationBarView constructed; Broadcast receiver registered");
                }
            });

            XposedHelpers.findAndHookMethod(navbarViewClass, "setMenuVisibility",
                    boolean.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    setMenuKeyVisibility();
                }
            });

            XposedHelpers.findAndHookMethod(navbarViewClass, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final Context context = ((View) param.thisObject).getContext();
                    final Resources gbRes = mGbContext.getResources();
                    final int backButtonResId = mResources.getIdentifier("back", "id", PACKAGE_NAME);
                    final int recentAppsResId = mResources.getIdentifier("recent_apps", "id", PACKAGE_NAME);
                    final int homeButtonResId = mResources.getIdentifier("home", "id", PACKAGE_NAME);
                    final View[] rotatedViews = 
                            (View[]) XposedHelpers.getObjectField(param.thisObject, "mRotatedViews");

                    if (rotatedViews != null) {
                        mRecentsKeys = new Object[rotatedViews.length];
                        mHomeKeys = new HomeKeyInfo[rotatedViews.length];
                        int index = 0;
                        for(View v : rotatedViews) {
                            if (mAlwaysOnBottom && v.getId() == mResources.getIdentifier("rot0", "id", PACKAGE_NAME)) {
                                adjustPortraitLayout(v);
                            }
                            if (backButtonResId != 0) { 
                                ImageView backButton = (ImageView) v.findViewById(backButtonResId);
                                if (backButton != null) {
                                    backButton.setScaleType(ScaleType.FIT_CENTER);
                                }
                            }
                            if (recentAppsResId != 0) {
                                ImageView recentAppsButton = (ImageView) v.findViewById(recentAppsResId);
                                mRecentsKeys[index] = recentAppsButton;
                            }
                            if (homeButtonResId != 0) { 
                                HomeKeyInfo hkInfo = new HomeKeyInfo();
                                hkInfo.homeKey = (ImageView) v.findViewById(homeButtonResId);
                                if (hkInfo.homeKey != null) {
                                    hkInfo.supportsLongPressDefault = 
                                        XposedHelpers.getBooleanField(hkInfo.homeKey, "mSupportsLongpress");
                                }
                                mHomeKeys[index] = hkInfo;
                            }
                            index++;
                        }
                    }

                    // prepare app, dpad left, dpad right keys
                    ViewGroup vRot, navButtons;

                    // prepare keys for rot0 view
                    vRot = (ViewGroup) ((ViewGroup) param.thisObject).findViewById(
                            mResources.getIdentifier("rot0", "id", PACKAGE_NAME));
                    if (vRot != null) {
                        KeyButtonView appKey = new KeyButtonView(context);
                        appKey.setScaleType(ScaleType.FIT_CENTER);
                        appKey.setClickable(true);
                        appKey.setImageDrawable(gbRes.getDrawable(R.drawable.ic_sysbar_apps));
                        appKey.setKeyCode(KeyEvent.KEYCODE_SOFT_LEFT);

                        KeyButtonView dpadLeft = new KeyButtonView(context);
                        dpadLeft.setScaleType(ScaleType.FIT_CENTER);
                        dpadLeft.setClickable(true);
                        dpadLeft.setImageDrawable(gbRes.getDrawable(R.drawable.ic_sysbar_ime_left));
                        dpadLeft.setVisibility(View.GONE);
                        dpadLeft.setKeyCode(KeyEvent.KEYCODE_DPAD_LEFT);

                        KeyButtonView dpadRight = new KeyButtonView(context);
                        dpadRight.setScaleType(ScaleType.FIT_CENTER);
                        dpadRight.setClickable(true);
                        dpadRight.setImageDrawable(gbRes.getDrawable(R.drawable.ic_sysbar_ime_right));
                        dpadRight.setVisibility(View.GONE);
                        dpadRight.setKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);

                        navButtons = (ViewGroup) vRot.findViewById(
                                mResources.getIdentifier("nav_buttons", "id", PACKAGE_NAME));
                        prepareNavbarViewInfo(navButtons, 0, appKey, dpadLeft, dpadRight);
                    }

                    // prepare keys for rot90 view
                    vRot = (ViewGroup) ((ViewGroup) param.thisObject).findViewById(
                            mResources.getIdentifier("rot90", "id", PACKAGE_NAME));
                    if (vRot != null) {
                        KeyButtonView appKey = new KeyButtonView(context);
                        appKey.setClickable(true);
                        appKey.setImageDrawable(gbRes.getDrawable(R.drawable.ic_sysbar_apps));
                        appKey.setKeyCode(KeyEvent.KEYCODE_SOFT_LEFT);

                        KeyButtonView dpadLeft = new KeyButtonView(context);
                        dpadLeft.setClickable(true);
                        dpadLeft.setImageDrawable(gbRes.getDrawable(R.drawable.ic_sysbar_ime_left));
                        dpadLeft.setVisibility(View.GONE);
                        dpadLeft.setKeyCode(KeyEvent.KEYCODE_DPAD_LEFT);

                        KeyButtonView dpadRight = new KeyButtonView(context);
                        dpadRight.setClickable(true);
                        dpadRight.setImageDrawable(gbRes.getDrawable(R.drawable.ic_sysbar_ime_right));
                        dpadRight.setVisibility(View.GONE);
                        dpadRight.setKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);

                        navButtons = (ViewGroup) vRot.findViewById(
                                mResources.getIdentifier("nav_buttons", "id", PACKAGE_NAME));
                        prepareNavbarViewInfo(navButtons, 1, appKey, dpadLeft, dpadRight);
                    }

                    updateRecentsKeyCode();
                    updateHomeKeyLongpressSupport();
                    setNavbarBgColor();

                    if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_SWAP_KEYS, false)) {
                        swapBackAndRecents();
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, 
                    "shouldDisableNavbarGestures", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (mNavbarRingDisabled || mHomeLongpressAction != 0) {
                        param.setResult(true);
                        return;
                    }
                }
            });

            XposedHelpers.findAndHookMethod(navbarViewClass, "setDisabledFlags",
                    int.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    setCustomKeyVisibility();
                    setMenuKeyVisibility();
                }
            });

            XposedHelpers.findAndHookMethod(navbarViewClass, "setNavigationIconHints",
                    int.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mNavbarColorsEnabled) {
                        final int navigationIconHints = XposedHelpers.getIntField(
                                param.thisObject, "mNavigationIconHints");
                        if ((Integer) param.args[0] != navigationIconHints || (Boolean)param.args[1]) {
                            setKeyColor();
                        }
                    }
                    setDpadKeyVisibility();
                }
            });

            if (mRingTargetsEnabled) {
                final Class<?> searchPanelViewClass = XposedHelpers.findClass(CLASS_SEARCH_PANEL_VIEW, classLoader);
                final Class<?> glowPadTriggerListenerClass = XposedHelpers.findClass(CLASS_GLOWPAD_TRIGGER_LISTENER, classLoader);
                final Class<?> glowPasViewClass = XposedHelpers.findClass(CLASS_GLOWPAD_VIEW, classLoader);

                mRingTargetsBgStyle = BgStyle.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_NAVBAR_RING_TARGETS_BG_STYLE, "NONE"));
                mRingHapticFeedback = RingHapticFeedback.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_NAVBAR_RING_HAPTIC_FEEDBACK, "DEFAULT"));

                XposedHelpers.findAndHookMethod(searchPanelViewClass, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        prefs.reload();
                        mGlowPadView = (View) XposedHelpers.getObjectField(param.thisObject, "mGlowPadView");
                        mGlowPadViewClass = mGlowPadView.getClass();
                        setRingHapticFeedback();
                        setRingTargets();
                    }
                });

                XposedHelpers.findAndHookMethod(glowPasViewClass, "showTargets", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.thisObject == mGlowPadView) {
                            if (mNavbarVertical && !isGlowPadVertical()) {
                                rotateRingTargets();
                            }
                        }
                    }
                });

                XposedHelpers.findAndHookMethod(navbarViewClass, "reorient", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG) log("Navigation bar view reorient");
                        if (mGlowPadView != null) {
                            mNavbarVertical = XposedHelpers.getBooleanField(param.thisObject, "mVertical");
                            if (mNavbarVertical && !isGlowPadVertical()) {
                                rotateRingTargets();
                            }
                        }
                    }
                });

                XposedHelpers.findAndHookMethod(glowPadTriggerListenerClass, "onTrigger",
                        View.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG) log("GlowPadView.OnTriggerListener; index=" + ((Integer) param.args[1]));
                        final int index = (Integer) param.args[1];
                        @SuppressWarnings("unchecked")
                        final ArrayList<Object> targets = (ArrayList<Object>) XposedHelpers.getObjectField(
                                mGlowPadView, "mTargetDrawables");
                        final Object td = targets.get(index);
    
                        AppInfo appInfo = (AppInfo) XposedHelpers.getAdditionalInstanceField(td, "mGbAppInfo");
                        if (appInfo != null) {
                            try {
                                Object activityManagerNative = XposedHelpers.callStaticMethod(
                                    XposedHelpers.findClass("android.app.ActivityManagerNative", null),
                                        "getDefault");
                                XposedHelpers.callMethod(activityManagerNative, "dismissKeyguardOnNextActivity");
                            } catch (Throwable t) {}
                            Intent intent = appInfo.intent;
                            // if intent is a GB action of broadcast type, handle it directly here
                            if (ShortcutActivity.isGbBroadcastShortcut(intent)) {
                                Intent newIntent = new Intent(intent.getStringExtra(ShortcutActivity.EXTRA_ACTION));
                                newIntent.putExtras(intent);
                                mGlowPadView.getContext().sendBroadcast(newIntent);
                            // otherwise start activity
                            } else {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                mGlowPadView.getContext().startActivity(intent);
                                mGlowPadView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            }
                        }
                    }
                });
            }
        } catch(Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void adjustPortraitLayout(View rView) {
        // we loaded navbar layout from layout-sw600dp which portrait mode
        // is not suitable for small screens, thus we have to make some adjustments
        try {
            ViewGroup vg = (ViewGroup) rView.findViewById(mResources.getIdentifier(
                    "nav_buttons", "id", PACKAGE_NAME));
            int keyWidth = mResources.getDimensionPixelSize(
                    mResources.getIdentifier("navigation_key_width", "dimen", PACKAGE_NAME));
            int menuKeyWidth = mResources.getDimensionPixelSize(
                    mResources.getIdentifier("navigation_menu_key_width", "dimen", PACKAGE_NAME));
            int backKeyResId = mResources.getIdentifier("back", "id", PACKAGE_NAME);
            int homeKeyResId = mResources.getIdentifier("home", "id", PACKAGE_NAME);
            int menuKeyResId = mResources.getIdentifier("menu", "id", PACKAGE_NAME);
            int otherViewWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    40, mResources.getDisplayMetrics());

            // remove all space widgets first
            int childCount = vg.getChildCount();
            for (int i = (childCount-1); i <=0 ; i--) {
                View v = vg.getChildAt(i);
                if (v instanceof Space) {
                    vg.removeView(v);
                }
            }
            // adjust layout
            for (int i = 0; i < vg.getChildCount(); i++) {
                View v = vg.getChildAt(i);
                MarginLayoutParams mlp = (MarginLayoutParams) v.getLayoutParams();
                if (v.getClass().getName().equals(CLASS_KEY_BUTTON_VIEW)) {
                    final int resId = v.getId();
                    mlp.width = resId == menuKeyResId ? menuKeyWidth : keyWidth;
                    v.setPadding(0, 0, 0, 0);
                    if (resId == backKeyResId || resId == homeKeyResId) {
                        View space = new View(rView.getContext());
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 
                                LinearLayout.LayoutParams.MATCH_PARENT);
                        lp.weight=1;
                        space.setLayoutParams(lp);
                        vg.addView(space, i+1);
                    }
                } else if (mlp.width != MarginLayoutParams.MATCH_PARENT) {
                    mlp.width = otherViewWidth;
                }
                v.setLayoutParams(mlp);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void prepareNavbarViewInfo(ViewGroup navButtons, int index, 
            KeyButtonView appView, KeyButtonView dpadLeft, KeyButtonView dpadRight) {
        try {
            final int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    40, navButtons.getResources().getDisplayMetrics());
            if (DEBUG) log("App key view minimum size=" + size);

            mNavbarViewInfo[index] = new NavbarViewInfo();
            mNavbarViewInfo[index].navButtons = navButtons;
            mNavbarViewInfo[index].customKey = appView;
            mNavbarViewInfo[index].dpadLeft = dpadLeft;
            mNavbarViewInfo[index].dpadRight = dpadRight;
            mNavbarViewInfo[index].navButtons.addView(dpadLeft, 0);
            mNavbarViewInfo[index].navButtons.addView(dpadRight);

            int searchPosition = index == 0 ? 1 : navButtons.getChildCount()-2;
            View v = navButtons.getChildAt(searchPosition);
            if (v.getId() == -1 && !v.getClass().getName().equals(CLASS_KEY_BUTTON_VIEW)) {
                mNavbarViewInfo[index].originalView = v;
            } else {
                searchPosition = searchPosition == 1 ? navButtons.getChildCount()-2 : 1;
                v = navButtons.getChildAt(searchPosition);
                if (v.getId() == -1 && !v.getClass().getName().equals(CLASS_KEY_BUTTON_VIEW)) {
                    mNavbarViewInfo[index].originalView = v;
                }
            }
            mNavbarViewInfo[index].customKeyPosition = searchPosition;

            // determine app key layout
            LinearLayout.LayoutParams lp = null;
            if (mNavbarViewInfo[index].originalView != null) {
                // determine layout from layout of placeholder view we found
                ViewGroup.LayoutParams ovlp = mNavbarViewInfo[index].originalView.getLayoutParams();
                if (DEBUG) log("originalView: lpWidth=" + ovlp.width + "; lpHeight=" + ovlp.height);
                if (ovlp.width >= 0) {
                    lp = new LinearLayout.LayoutParams(size, LinearLayout.LayoutParams.MATCH_PARENT, 0);
                } else if (ovlp.height >= 0) {
                    lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, size, 0);
                } else {
                    log("Weird layout of placeholder view detected");
                }
            } else {
                // determine layout from Back key
                final int resId = navButtons.getResources().getIdentifier("back", "id", PACKAGE_NAME);
                if (resId != 0) {
                    View back = navButtons.findViewById(resId);
                    if (back != null) {
                        ViewGroup.LayoutParams blp = back.getLayoutParams();
                        if (blp.width >= 0) {
                            lp = new LinearLayout.LayoutParams(size, LinearLayout.LayoutParams.MATCH_PARENT, 0);
                        } else if (blp.height >= 0) {
                            lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, size, 0);
                        } else {
                            log("Weird layout of back button view detected");
                        }
                    } else {
                        log("Could not find back button view");
                    }
                } else {
                    log("Could not find back button resource ID");
                }
            }
            // worst case scenario (should never happen, but just to make sure)
            if (lp == null) {
                lp = new LinearLayout.LayoutParams(size, size, 0);
            }
            if (DEBUG) log("appView: lpWidth=" + lp.width + "; lpHeight=" + lp.height);
            mNavbarViewInfo[index].customKey.setLayoutParams(lp);
            mNavbarViewInfo[index].dpadLeft.setLayoutParams(lp);
            mNavbarViewInfo[index].dpadRight.setLayoutParams(lp);
        } catch (Throwable t) {
            log("Error preparing NavbarViewInfo: " + t.getMessage());
        }
    }

    private static void setCustomKeyVisibility() {
        try {
            final int disabledFlags = XposedHelpers.getIntField(mNavigationBarView, "mDisabledFlags");
            final boolean visible = mCustomKeyEnabled &&
                    !((disabledFlags & STATUS_BAR_DISABLE_RECENT) != 0);
            for (int i = 0; i <= 1; i++) {
                // swap / unswap with menu key if necessary
                if ((!mCustomKeyEnabled || !mCustomKeySwapEnabled) && 
                        mNavbarViewInfo[i].menuCustomSwapped) {
                    swapMenuAndCustom(mNavbarViewInfo[i]);
                } else if (mCustomKeyEnabled && mCustomKeySwapEnabled && 
                        !mNavbarViewInfo[i].menuCustomSwapped) {
                    swapMenuAndCustom(mNavbarViewInfo[i]);
                }

                if (mNavbarViewInfo[i].visible == visible) continue;

                if (mNavbarViewInfo[i].originalView != null) {
                    mNavbarViewInfo[i].navButtons.removeViewAt(mNavbarViewInfo[i].customKeyPosition);
                    mNavbarViewInfo[i].navButtons.addView(visible ?
                            mNavbarViewInfo[i].customKey : mNavbarViewInfo[i].originalView,
                            mNavbarViewInfo[i].customKeyPosition);
                } else {
                    if (visible) {
                        mNavbarViewInfo[i].navButtons.addView(mNavbarViewInfo[i].customKey,
                                mNavbarViewInfo[i].customKeyPosition);
                    } else {
                        mNavbarViewInfo[i].navButtons.removeView(mNavbarViewInfo[i].customKey);
                    }
                }
                mNavbarViewInfo[i].visible = visible;
                mNavbarViewInfo[i].navButtons.requestLayout();
                if (DEBUG) log("setAppKeyVisibility: visible=" + visible);
            }
        } catch (Throwable t) {
            log("Error setting app key visibility: " + t.getMessage());
        }
    }

    private static void setMenuKeyVisibility() {
        try {
            final boolean showMenu = XposedHelpers.getBooleanField(mNavigationBarView, "mShowMenu");
            final int disabledFlags = XposedHelpers.getIntField(mNavigationBarView, "mDisabledFlags");
            final boolean visible = (showMenu || mAlwaysShowMenukey) &&
                    !((disabledFlags & STATUS_BAR_DISABLE_RECENT) != 0);
            int menuResId = mResources.getIdentifier("menu", "id", PACKAGE_NAME);
            for (int i = 0; i <= 1; i++) {
                View v = mNavbarViewInfo[i].navButtons.findViewById(menuResId);
                if (v != null) {
                    v.setVisibility(visible ? View.VISIBLE : 
                        mDpadKeysVisible ? View.GONE : View.INVISIBLE);
                }
            }
        } catch (Throwable t) {
            log("Error setting menu key visibility");
        }
        
    }

    private static void setDpadKeyVisibility() {
        if (!mCursorControlEnabled) return;
        try {
            final int iconHints = XposedHelpers.getIntField(mNavigationBarView, "mNavigationIconHints");
            final int disabledFlags = XposedHelpers.getIntField(mNavigationBarView, "mDisabledFlags");
            mDpadKeysVisible = !((disabledFlags & STATUS_BAR_DISABLE_RECENT) != 0) && 
                    (iconHints & NAVIGATION_HINT_BACK_ALT) != 0;

            for (int i = 0; i <= 1; i++) {
                // hide/unhide app key or whatever view at that position
                View v = mNavbarViewInfo[i].navButtons.getChildAt(mNavbarViewInfo[i].customKeyPosition);
                if (v != null) {
                    v.setVisibility(mDpadKeysVisible ? View.GONE : View.VISIBLE);
                }
                // hide/unhide menu key
                int menuResId = mResources.getIdentifier("menu", "id", PACKAGE_NAME);
                v = mNavbarViewInfo[i].navButtons.findViewById(menuResId);
                if (v != null) {
                    if (mDpadKeysVisible) {
                        v.setVisibility(View.GONE);
                    } else {
                        setMenuKeyVisibility();
                    }
                }
    
                mNavbarViewInfo[i].dpadLeft.setVisibility(mDpadKeysVisible ? View.VISIBLE : View.GONE);
                mNavbarViewInfo[i].dpadRight.setVisibility(mDpadKeysVisible ? View.VISIBLE : View.GONE);
                if (DEBUG) log("setDpadKeyVisibility: visible=" + mDpadKeysVisible);
            }
        } catch (Throwable t) {
            log("Error setting dpad key visibility: " + t.getMessage());
        }
    }

    private static void updateRecentsKeyCode() {
        if (mRecentsKeys == null) return;

        try {
            final boolean hasAction = recentsKeyHasAction();
            for (Object o : mRecentsKeys) {
                if (o != null) {
                    XposedHelpers.setIntField(o, "mCode", hasAction ? KeyEvent.KEYCODE_APP_SWITCH : 0);
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static boolean recentsKeyHasAction() {
        return (mRecentsSingletapAction != 0 ||
                mRecentsLongpressAction != 0 ||
                !mHwKeysEnabled);
    }

    private static void updateHomeKeyLongpressSupport() {
        if (mHomeKeys == null) return;

        try {
            for (HomeKeyInfo hkInfo : mHomeKeys) {
                if (hkInfo.homeKey != null) {
                    XposedHelpers.setBooleanField(hkInfo.homeKey, "mSupportsLongpress",
                            mHomeLongpressAction == 0 ? hkInfo.supportsLongPressDefault : true);
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void setKeyColor() {
        try {
            View v = (View) XposedHelpers.getObjectField(mNavigationBarView, "mCurrentView");
            ViewGroup navButtons = (ViewGroup) v.findViewById(
                    mResources.getIdentifier("nav_buttons", "id", PACKAGE_NAME));
            final int childCount = navButtons.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (navButtons.getChildAt(i) instanceof ImageView) {
                    ImageView imgv = (ImageView)navButtons.getChildAt(i);
                    if (mNavbarColorsEnabled) {
                        imgv.setColorFilter(mKeyColor, PorterDuff.Mode.SRC_ATOP);
                    } else {
                        imgv.clearColorFilter();
                    }
                    if (imgv.getClass().getName().equals(CLASS_KEY_BUTTON_VIEW)) {
                        Drawable d = (Drawable) XposedHelpers.getObjectField(imgv, "mGlowBG");
                        if (d != null) {
                            if (mNavbarColorsEnabled) {
                                d.setColorFilter(mKeyGlowColor, PorterDuff.Mode.SRC_ATOP);
                            } else {
                                d.clearColorFilter();
                            }
                        }
                    } else if (imgv instanceof KeyButtonView) {
                        ((KeyButtonView) imgv).setGlowColor(mKeyGlowColor);
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void setNavbarBgColor() {
        try {
            if (!mNavbarColorsEnabled) {
                if (mNavbarBgColorOriginal != null) {
                    ColorDrawable cd = new ColorDrawable(mNavbarBgColorOriginal);
                    mNavigationBarView.setBackground(cd);
                    if (DEBUG) log("Restored navbar background original color");
                }
            } else {
                if (mNavbarBgColorOriginal == null && 
                        (mNavigationBarView.getBackground() instanceof ColorDrawable)) {
                    mNavbarBgColorOriginal = 
                            ((ColorDrawable) mNavigationBarView.getBackground()).getColor();
                    if (DEBUG) log("Saved navbar background original color");
                }
                if (Utils.isXperiaDevice()) {
                    if (!(mNavigationBarView.getBackground() instanceof ColorDrawable)) {
                        ColorDrawable colorDrawable = new ColorDrawable(mNavbarBgColor);
                        mNavigationBarView.setBackground(colorDrawable);
                    } else {
                        ((ColorDrawable) mNavigationBarView.getBackground()).setColor(mNavbarBgColor);
                    }
                } else {
                    if (!(mNavigationBarView.getBackground() instanceof BackgroundAlphaColorDrawable)) {
                        BackgroundAlphaColorDrawable colorDrawable = new BackgroundAlphaColorDrawable(mNavbarBgColor);
                        mNavigationBarView.setBackground(colorDrawable);
                    } else {
                        ((BackgroundAlphaColorDrawable) mNavigationBarView.getBackground()).setBgColor(mNavbarBgColor);
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void swapBackAndRecents() {
        try {
            final int backButtonResId = mResources.getIdentifier("back", "id", PACKAGE_NAME);
            final int recentAppsResId = mResources.getIdentifier("recent_apps", "id", PACKAGE_NAME);
            for (int i = 0; i < 2; i++) {
                if (mNavbarViewInfo[i].navButtons == null) continue;
                View backKey = mNavbarViewInfo[i].navButtons.findViewById(backButtonResId);
                View recentsKey = mNavbarViewInfo[i].navButtons.findViewById(recentAppsResId);
                int backPos = mNavbarViewInfo[i].navButtons.indexOfChild(backKey);
                int recentsPos = mNavbarViewInfo[i].navButtons.indexOfChild(recentsKey);
                mNavbarViewInfo[i].navButtons.removeView(backKey);
                mNavbarViewInfo[i].navButtons.removeView(recentsKey);
                if (backPos < recentsPos) {
                    mNavbarViewInfo[i].navButtons.addView(recentsKey, backPos);
                    mNavbarViewInfo[i].navButtons.addView(backKey, recentsPos);
                } else {
                    mNavbarViewInfo[i].navButtons.addView(backKey, recentsPos);
                    mNavbarViewInfo[i].navButtons.addView(recentsKey, backPos);
                }
            }
        }
        catch (Throwable t) {
            log("Error swapping back and recents key: " + t.getMessage());
        }
    }

    private static void swapMenuAndCustom(NavbarViewInfo nvi) {
        if (nvi.customKey.getParent() == null) return;

        try {
            final int menuButtonResId = mResources.getIdentifier("menu", "id", PACKAGE_NAME);
            View menuKey = nvi.navButtons.findViewById(menuButtonResId);
            View customKey = nvi.customKey;
            int menuPos = nvi.navButtons.indexOfChild(menuKey);
            int customPos = nvi.customKeyPosition;
            nvi.navButtons.removeView(menuKey);
            nvi.navButtons.removeView(customKey);
            if (menuPos < customPos) {
                nvi.navButtons.addView(customKey, menuPos);
                nvi.navButtons.addView(menuKey, customPos);
            } else {
                nvi.navButtons.addView(menuKey, customPos);
                nvi.navButtons.addView(customKey, menuPos);
            }
            nvi.customKeyPosition = menuPos;
            nvi.menuCustomSwapped = !nvi.menuCustomSwapped;
        }
        catch (Throwable t) {
            log("Error swapping menu and custom key: " + t.getMessage());
        }
    }

    private static void setRingTargets() {
        if (mGlowPadView == null) return;

        try {
            Context context = mGlowPadView.getContext();

            final ArrayList<Object> newTargets = new ArrayList<Object>();
            final ArrayList<String> newDescriptions = new ArrayList<String>();
            final ArrayList<String> newDirections = new ArrayList<String>();
            final int iconSizeDp = mRingTargetsBgStyle == BgStyle.NONE ? 50 : 45;

            final int dummySlotCount = isGlowPadVertical() ? 4 : 1;
            for (int i = 0; i < dummySlotCount; i++) {
                newTargets.add(GlowPadHelper.createTargetDrawable(context, null, mGlowPadViewClass));
                newDescriptions.add(null);
                newDirections.add(null);
            }

            for (int i = 0; i < (12 - dummySlotCount); i++) {
                if (i < GravityBoxSettings.PREF_KEY_NAVBAR_RING_TARGET.size()) {
                    String app = mPrefs.getString(
                            GravityBoxSettings.PREF_KEY_NAVBAR_RING_TARGET.get(i), null);
                    AppInfo ai = GlowPadHelper.getAppInfo(context, app, iconSizeDp, mRingTargetsBgStyle);
                    newTargets.add(GlowPadHelper.createTargetDrawable(context, ai, mGlowPadViewClass));
                    newDescriptions.add(ai == null ? null : ai.name);
                    newDirections.add(null);
                } else {
                    newTargets.add(GlowPadHelper.createTargetDrawable(context, null, mGlowPadViewClass));
                    newDescriptions.add(null);
                    newDirections.add(null);
                }
            }

            XposedHelpers.setObjectField(mGlowPadView, "mTargetDrawables", newTargets);
            XposedHelpers.setObjectField(mGlowPadView, "mTargetDescriptions", newDescriptions);
            XposedHelpers.setObjectField(mGlowPadView, "mDirectionDescriptions", newDirections);
            mGlowPadView.requestLayout();
        } catch(Throwable t) {
            XposedBridge.log(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static void updateRingTarget(int index, String app) {
        if (mGlowPadView == null || index < 0) return;

        try {
            final Context context = mGlowPadView.getContext();
            final ArrayList<Object> targets = 
                    (ArrayList<Object>) XposedHelpers.getObjectField(mGlowPadView, "mTargetDrawables");
            final ArrayList<String> descs = 
                    (ArrayList<String>)XposedHelpers.getObjectField(mGlowPadView, "mTargetDescriptions");
            final int iconSizeDp = mRingTargetsBgStyle == BgStyle.NONE ? 50 : 45;
            index++; // take dummy drawable at position 0 into account
            if (isGlowPadVertical()) {
                index += 3;
            }

            AppInfo ai = GlowPadHelper.getAppInfo(context, app, iconSizeDp, mRingTargetsBgStyle);
            if (targets != null && targets.size() > index) {
                targets.set(index, GlowPadHelper.createTargetDrawable(context, ai, mGlowPadViewClass));
                if (DEBUG) log("Ring target at index " + index + " set to: " + (ai == null ? "null" : ai.name));
            }
            if (descs != null && descs.size() > index) {
                descs.set(index, ai == null ? null : ai.name);
            }
            mGlowPadView.requestLayout();
        } catch(Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static boolean isGlowPadVertical() {
        Boolean vertical = (Boolean) XposedHelpers.getAdditionalInstanceField(mGlowPadView, "mGbVertical");
        return (vertical != null && vertical.booleanValue());
    }

    private static void rotateRingTargets() {
        try {
            final ArrayList<Object> targets = 
                    (ArrayList<Object>) XposedHelpers.getObjectField(mGlowPadView, "mTargetDrawables");
            final ArrayList<String> descs = 
                    (ArrayList<String>)XposedHelpers.getObjectField(mGlowPadView, "mTargetDescriptions");

            if (targets != null) Collections.rotate(targets, 3);
            if (descs != null) Collections.rotate(descs, 3);
            mGlowPadView.requestLayout();
            XposedHelpers.setAdditionalInstanceField(mGlowPadView, "mGbVertical", true);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void setRingHapticFeedback() {
        if (mGlowPadView == null) return;

        try {
            if (mRingVibrateDurationOrig == null) {
                mRingVibrateDurationOrig = XposedHelpers.getIntField(mGlowPadView, "mVibrationDuration");
                if (mRingHapticFeedback == RingHapticFeedback.DEFAULT) return;
            }
            int vibrateDuration;
            switch (mRingHapticFeedback) {
                case ENABLED: vibrateDuration = 20; break;
                case DISABLED: vibrateDuration = 0; break;
                default:
                case DEFAULT: vibrateDuration = mRingVibrateDurationOrig; break;
            }
            XposedHelpers.setIntField(mGlowPadView, "mVibrationDuration", vibrateDuration);
            XposedHelpers.callMethod(mGlowPadView, "setVibrateEnabled", vibrateDuration > 0);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
