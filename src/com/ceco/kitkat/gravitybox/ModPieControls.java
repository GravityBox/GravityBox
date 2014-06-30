/*
 * Copyright (C) 2013 The CyanogenMod Project (Jens Doll)
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
 * This code is loosely based on portions of the ParanoidAndroid Project source, Copyright (C) 2012.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ceco.kitkat.gravitybox;

import java.util.HashSet;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.ceco.kitkat.gravitybox.R;
import com.ceco.kitkat.gravitybox.pie.PieController;
import com.ceco.kitkat.gravitybox.pie.PieLayout;
import com.ceco.kitkat.gravitybox.pie.PieController.Position;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModPieControls {
    private static final String TAG = "GB:ModPieController";
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_INPUT = false;

    private static final String CLASS_BASE_STATUSBAR = "com.android.systemui.statusbar.BaseStatusBar";
    private static final String CLASS_SYSTEM_UI = "com.android.systemui.SystemUI";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";

    public static final int STATUS_BAR_DISABLE_HOME = 0x00200000;
    public static final int STATUS_BAR_DISABLE_SEARCH = 0x02000000;
    public static final int STATUS_BAR_DISABLE_RECENT = 0x01000000;
    public static final int STATUS_BAR_DISABLE_BACK = 0x00400000;

    public static final int PIE_DISABLED = 0;
    public static final int PIE_ENABLED_ALWAYS = 1;
    public static final int PIE_ENABLED_ED = 2;
    public static final int PIE_ENABLED_ED_NAVBAR_HIDDEN = 3;

    private static PieController mPieController;
    private static PieLayout mPieContainer;
    private static int mPieTriggerSlots;
    private static View[] mPieTrigger = new View[PieController.Position.values().length];
    private static Context mContext;
    private static Context mGbContext;
    private static WindowManager mWindowManager;
    private static PieSettingsObserver mSettingsObserver;
    private static boolean mShowMenuItem;
    private static boolean mAlwaysShowMenuItem;
    private static int mPieMode;
    private static int mPieSize;
    private static int mPieTriggerSize;
    private static int mExpandedDesktopMode;
    private static boolean mCenterTrigger;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("Broadcast received: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_PIE_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_ENABLE)) {
                    mPieMode = intent.getIntExtra(GravityBoxSettings.EXTRA_PIE_ENABLE, 0);
                    attachPie();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_CUSTOM_KEY_MODE)) {
                    mPieController.setCustomKeyMode(intent.getIntExtra(
                            GravityBoxSettings.EXTRA_PIE_CUSTOM_KEY_MODE,
                            GravityBoxSettings.PIE_CUSTOM_KEY_OFF));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_TRIGGERS)) {
                    String[] triggers = intent.getStringArrayExtra(
                            GravityBoxSettings.EXTRA_PIE_TRIGGERS);
                    mPieTriggerSlots = getTriggerSlotsFromArray(triggers);
                    if (mPieContainer != null) {
                        mPieContainer.setTriggerSlots(mPieTriggerSlots);
                    }
                    attachPie();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_TRIGGER_SIZE)) {
                    mPieTriggerSize = intent.getIntExtra(GravityBoxSettings.EXTRA_PIE_TRIGGER_SIZE, 5);
                    attachPie();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_SIZE)) {
                    mPieSize = intent.getIntExtra(GravityBoxSettings.EXTRA_PIE_SIZE, 1000);
                    if (mPieContainer != null) {
                        mPieContainer.setPieSize(mPieSize);
                    }
                    attachPie();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_HWKEYS_DISABLE)) {
                    mShowMenuItem = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_PIE_HWKEYS_DISABLE, false);
                    mPieController.setMenuVisibility(mShowMenuItem | mAlwaysShowMenuItem);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_MENU)) {
                    mAlwaysShowMenuItem = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_PIE_MENU, false);
                    mPieController.setMenuVisibility(mShowMenuItem | mAlwaysShowMenuItem);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_COLOR_BG)) {
                    mPieController.setBackgroundColor(intent.getIntExtra(GravityBoxSettings.EXTRA_PIE_COLOR_BG,
                            mGbContext.getResources().getColor(R.color.pie_background_color)));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_COLOR_FG)) {
                    mPieController.setForegroundColor(intent.getIntExtra(GravityBoxSettings.EXTRA_PIE_COLOR_FG,
                            mGbContext.getResources().getColor(R.color.pie_foreground_color)));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_COLOR_OUTLINE)) {
                    mPieController.setOutlineColor(intent.getIntExtra(GravityBoxSettings.EXTRA_PIE_COLOR_OUTLINE,
                            mGbContext.getResources().getColor(R.color.pie_outline_color)));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_COLOR_SELECTED)) {
                    mPieController.setSelectedColor(intent.getIntExtra(GravityBoxSettings.EXTRA_PIE_COLOR_SELECTED,
                            mGbContext.getResources().getColor(R.color.pie_selected_color)));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_COLOR_TEXT)) {
                    mPieController.setTextColor(intent.getIntExtra(GravityBoxSettings.EXTRA_PIE_COLOR_TEXT,
                            mGbContext.getResources().getColor(R.color.pie_text_color)));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_BUTTON) &&
                        intent.hasExtra(GravityBoxSettings.EXTRA_PIE_LONGPRESS_ACTION)) {
                    mPieController.setLongPressAction(intent.getStringExtra(GravityBoxSettings.EXTRA_PIE_BUTTON),
                            intent.getIntExtra(GravityBoxSettings.EXTRA_PIE_LONGPRESS_ACTION, 0));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_SYSINFO_DISABLE)) {
                    mPieController.setSysinfoDisabled(intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_PIE_SYSINFO_DISABLE, false));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_LONGPRESS_DELAY)) {
                    mPieController.setLongpressDelay(intent.getIntExtra(
                            GravityBoxSettings.EXTRA_PIE_LONGPRESS_DELAY, 0));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_MIRRORED_KEYS)) {
                    mPieController.setMirroredKeys(intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_PIE_MIRRORED_KEYS, false));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_CENTER_TRIGGER)) {
                    mCenterTrigger = intent.getBooleanExtra(GravityBoxSettings.EXTRA_PIE_CENTER_TRIGGER, false);
                    attachPie();
                }
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_EXPANDED_DESKTOP_MODE_CHANGED)) {
                mExpandedDesktopMode = intent.getIntExtra(
                        GravityBoxSettings.EXTRA_ED_MODE, GravityBoxSettings.ED_DISABLED);
                attachPie();
            }
        }
    };

    private static int getTriggerSlotsFromArray(String[] triggers) {
        int tslots = 0;
        for (String s : triggers) {
            try {
                tslots |= Integer.valueOf(s);
            } catch (NumberFormatException e) {
                XposedBridge.log(e);
            }
        }
        return tslots;
    }

    private static View.OnTouchListener mPieTriggerOnTouchHandler = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            final PieController.Tracker tracker = (PieController.Tracker)v.getTag();

            if (tracker == null) {
                if (DEBUG_INPUT) {
                    log("Pie trigger onTouch: action: " + action + ", ("
                            + event.getAxisValue(MotionEvent.AXIS_X) + ","
                            + event.getAxisValue(MotionEvent.AXIS_Y) + ") position: NULL returning: false");
                }
                return false;
            }

            if (!mPieController.isShowing()) {
                if (event.getPointerCount() > 1) {
                    if (DEBUG_INPUT) {
                        log("Pie trigger onTouch: action: " + action
                                + ", (to many pointers) position: " + tracker.position.name()
                                + " returning: false");
                    }
                    return false;
                }

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        tracker.start(event);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (tracker.move(event)) {
                            if (DEBUG) {
                                log("Pie control activated on: ("
                                        + event.getAxisValue(MotionEvent.AXIS_X) + ","
                                        + event.getAxisValue(MotionEvent.AXIS_Y) + ") with position: "
                                        + tracker.position.name());
                            }
                            // send the activation to the controller
                            mPieController.activateFromTrigger(v, event, tracker.position);
                            // forward a spoofed ACTION_DOWN event
                            MotionEvent echo = (MotionEvent) XposedHelpers.callMethod(event, "copy");
                            echo.setAction(MotionEvent.ACTION_DOWN);
                            return mPieContainer.onTouch(v, echo);
                        }
                        break;
                    default:
                        // whatever it was, we are giving up on this one
                        tracker.active = false;
                        break;
                }
            } else {
                if (DEBUG_INPUT) {
                    log("Pie trigger onTouch: action: " + action + ", ("
                            + event.getAxisValue(MotionEvent.AXIS_X) + ","
                            + event.getAxisValue(MotionEvent.AXIS_Y)
                            + ") position: " + tracker.position.name() + " delegating");
                }
                return mPieContainer.onTouch(v, event);
            }
            if (DEBUG_INPUT) {
                log("Pie trigger onTouch: action: " + action + ", ("
                        + event.getAxisValue(MotionEvent.AXIS_X) + ","
                        + event.getAxisValue(MotionEvent.AXIS_Y) + ") position: "
                        + tracker.position.name() + " returning: " + tracker.active);
            }
            return tracker.active;
        }

    };

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> baseStatusBarClass = XposedHelpers.findClass(CLASS_BASE_STATUSBAR, classLoader);
            final Class<?> systemUiClass = XposedHelpers.findClass(CLASS_SYSTEM_UI, classLoader);
            final Class<?> phoneStatusBarClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);

            mPieMode = PIE_DISABLED;
            try {
                mPieMode = Integer.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_PIE_CONTROL_ENABLE, "0"));
            } catch (NumberFormatException nfe) {
                log("Invalid preference value for Pie Mode");
            }
            mShowMenuItem = prefs.getBoolean(GravityBoxSettings.PREF_KEY_HWKEYS_DISABLE, false);
            mAlwaysShowMenuItem = prefs.getBoolean(GravityBoxSettings.PREF_KEY_PIE_CONTROL_MENU, false);
            Set<String> triggerSet = prefs.getStringSet(
                    GravityBoxSettings.PREF_KEY_PIE_CONTROL_TRIGGERS, new HashSet<String>());
            mPieTriggerSlots = getTriggerSlotsFromArray(triggerSet.toArray(new String[triggerSet.size()]));
            mPieSize = prefs.getInt(GravityBoxSettings.PREF_KEY_PIE_CONTROL_SIZE, 1000);
            mPieTriggerSize = prefs.getInt(GravityBoxSettings.PREF_KEY_PIE_CONTROL_TRIGGER_SIZE, 5);
            mCenterTrigger = prefs.getBoolean(GravityBoxSettings.PREF_KEY_PIE_CENTER_TRIGGER, false);

            mExpandedDesktopMode = GravityBoxSettings.ED_DISABLED;
            try {
                mExpandedDesktopMode = Integer.valueOf(prefs.getString(
                        GravityBoxSettings.PREF_KEY_EXPANDED_DESKTOP, "0"));
            } catch (NumberFormatException nfe) {
                log("Invalid value for PREF_KEY_EXPANDED_DESKTOP preference");
            }

            XposedHelpers.findAndHookMethod(baseStatusBarClass, "start", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (DEBUG) log("BaseStatusBar starting...");
                    mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    mGbContext = mContext.createPackageContext(GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
                    mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                    mPieController = new PieController(mContext, mGbContext, prefs);

                    int customKeyMode = GravityBoxSettings.PIE_CUSTOM_KEY_OFF;
                    try {
                        customKeyMode = Integer.valueOf(prefs.getString(
                            GravityBoxSettings.PREF_KEY_PIE_CONTROL_CUSTOM_KEY, "0"));
                    } catch (NumberFormatException nfe) {
                        log("Invalid value for PREF_KEY_PIE_CONTROL_CUSTOM_KEY preference");
                    }
                    mPieController.setCustomKeyMode(customKeyMode);
                    mPieController.setMirroredKeys(prefs.getBoolean(GravityBoxSettings.PREF_KEY_PIE_MIRRORED_KEYS, false));

                    mPieController.attachTo(param.thisObject);

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_PIE_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_EXPANDED_DESKTOP_MODE_CHANGED);
                    mContext.registerReceiver(mBroadcastReceiver, intentFilter);

                    mSettingsObserver = new PieSettingsObserver(new Handler());
                    mSettingsObserver.onChange(true);
                    mSettingsObserver.observe();
                }
            });

            XposedHelpers.findAndHookMethod(systemUiClass, 
                    "onConfigurationChanged", Configuration.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    attachPie();
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarClass, "disable", int.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (mPieController == null) return;

                    final int old = XposedHelpers.getIntField(param.thisObject, "mDisabled");
                    final int state = (Integer) param.args[0];
                    final int diff = state ^ old;
                    if ((diff & (STATUS_BAR_DISABLE_HOME
                            | STATUS_BAR_DISABLE_RECENT
                            | STATUS_BAR_DISABLE_BACK
                            | STATUS_BAR_DISABLE_SEARCH)) != 0) {
                        mPieController.setDisabledFlags(state);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarClass, 
                    "setNavigationIconHints", int.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mPieController == null) return;

                    mPieController.setNavigationIconHints((Integer)param.args[0]);
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarClass,
                    "topAppWindowChanged", boolean.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mPieController == null) return;

                    mPieController.setMenuVisibility((Boolean)param.args[0] 
                            | mShowMenuItem
                            | mAlwaysShowMenuItem);
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static final class PieSettingsObserver extends ContentObserver {
        PieSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Global.getUriFor(
                    ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (DEBUG) log("PieSettingsObserver onChange()");

            attachPie();
        }
    }

    public static boolean isPieEnabled(Context context, int pieMode, int expandedDesktopMode) {
        if (context == null) return false;

        final ContentResolver cr = context.getContentResolver();
        if (DEBUG) log("isPieEnabled: mPieMode = " + pieMode);

        switch(pieMode) {
            case PIE_DISABLED: return false;
            case PIE_ENABLED_ALWAYS: return true;
            case PIE_ENABLED_ED:
            case PIE_ENABLED_ED_NAVBAR_HIDDEN:
                if (DEBUG) log("isPieEnabled: SETTING_EXPANDED_DESKTOP_MODE = " + expandedDesktopMode);
                final boolean edEnabled = Settings.Global.getInt(
                        cr, ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE, 0) == 1;
                if (DEBUG) log("isPieEnabled: SETTING_EXPANDED_DESKTOP_STATE = " + edEnabled);
                return edEnabled && (pieMode == PIE_ENABLED_ED ||
                        (pieMode == PIE_ENABLED_ED_NAVBAR_HIDDEN 
                            && (expandedDesktopMode == GravityBoxSettings.ED_HIDE_NAVBAR ||
                                    expandedDesktopMode == GravityBoxSettings.ED_SEMI_IMMERSIVE)));
            default: return false;
        }
    }

    private static void attachPie() {
        if (isPieEnabled(mContext, mPieMode, mExpandedDesktopMode)) {

            // Create our container, if it does not exist already
            if (mPieContainer == null) {
                mPieContainer = new PieLayout(mContext, mGbContext, mPieTriggerSlots, mPieSize);
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        PixelFormat.TRANSLUCENT);
                // This title is for debugging only. See: dumpsys window
                lp.setTitle("PieControlPanel");
                lp.windowAnimations = android.R.style.Animation;
                lp.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND;

                mWindowManager.addView(mPieContainer, lp);
                mPieController.attachTo(mPieContainer);
            }

            // add or update pie triggers
            if (DEBUG) log("AttachPie with trigger position flags: " + mPieTriggerSlots);

            refreshPieTriggers();

        } else {
            for (int i = 0; i < mPieTrigger.length; i++) {
                if (mPieTrigger[i] != null) {
                    mWindowManager.removeView(mPieTrigger[i]);
                    mPieTrigger[i] = null;
                }
            }
        }
    }

    private static void refreshPieTriggers() {
        for (Position g : Position.values()) {
            View trigger = mPieTrigger[g.INDEX];
            if (trigger == null && (mPieTriggerSlots & g.FLAG) != 0) {
                trigger = new View(mContext);
                trigger.setClickable(false);
                trigger.setLongClickable(false);
                trigger.setTag(mPieController.buildTracker(g));
                trigger.setOnTouchListener(mPieTriggerOnTouchHandler);

                if (DEBUG) {
                    trigger.setVisibility(View.VISIBLE);
                    trigger.setBackgroundColor(0x77ff0000);
                    log("addPieTrigger on " + g.INDEX
                            + " with position: " + g + " : " + trigger.toString());
                }
                mWindowManager.addView(trigger, getPieTriggerLayoutParams(g));
                mPieTrigger[g.INDEX] = trigger;
            } else if (trigger != null && (mPieTriggerSlots & g.FLAG) == 0) {
                mWindowManager.removeView(trigger);
                mPieTrigger[g.INDEX] = null;
            } else if (trigger != null) {
                mWindowManager.updateViewLayout(trigger, getPieTriggerLayoutParams(g));
            }
        }
    }

    private static WindowManager.LayoutParams getPieTriggerLayoutParams(Position position) {
        final Resources res = mContext.getResources();

        int triggerLength = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, 
                res.getDisplayMetrics());
        int width = mCenterTrigger ? triggerLength : (int) (res.getDisplayMetrics().widthPixels * 0.8f);
        int height = mCenterTrigger ? triggerLength : (int) (res.getDisplayMetrics().heightPixels * 0.8f);

        int triggerThickness = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, mPieTriggerSize, 
                        res.getDisplayMetrics());
        if (DEBUG) log("Pie trigger thickness: " + triggerThickness);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                (position == Position.TOP || position == Position.BOTTOM
                        ? width : triggerThickness),
                (position == Position.LEFT || position == Position.RIGHT
                        ? height : triggerThickness),
                WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                        /* | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM */,
                PixelFormat.TRANSLUCENT);
        // This title is for debugging only. See: dumpsys window
        lp.setTitle("PieTrigger" + position.name());
        if (position == Position.LEFT || position == Position.RIGHT) {
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        } else {
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
        }
        lp.gravity = position.ANDROID_GRAVITY;
        return lp;
    }

    public static void onPieSnapped(int positionFlagOrig, int positionFlagNew) {
        mPieTriggerSlots = mPieTriggerSlots & ~positionFlagOrig | positionFlagNew;
        mPieContainer.setTriggerSlots(mPieTriggerSlots);
        attachPie();
    }
}