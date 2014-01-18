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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ceco.kitkat.gravitybox.GlowPadHelper.AppInfo;
import com.ceco.kitkat.gravitybox.shortcuts.ShortcutActivity;

import android.app.Activity;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;

public class ModLockscreen {
    private static final String CLASS_PATH = "com.android.keyguard";
    private static final String TAG = "GB:ModLockscreen";
    public static final String PACKAGE_NAME = "com.android.keyguard";

    private static final String CLASS_KGVIEW_MANAGER = CLASS_PATH + ".KeyguardViewManager";
    private static final String CLASS_KGVIEW_MANAGER_HOST = CLASS_KGVIEW_MANAGER + ".ViewManagerHost";
    private static final String CLASS_KG_HOSTVIEW = CLASS_PATH + ".KeyguardHostView";
    private static final String CLASS_KG_SELECTOR_VIEW = CLASS_PATH + ".KeyguardSelectorView";
    private static final String CLASS_TRIGGER_LISTENER = CLASS_PATH + ".KeyguardSelectorView$1";
    private static final String CLASS_KG_ABS_KEY_INPUT_VIEW = CLASS_PATH + ".KeyguardAbsKeyInputView";
    private static final String CLASS_KGVIEW_MEDIATOR = CLASS_PATH + ".KeyguardViewMediator";
    private static final String CLASS_KG_UPDATE_MONITOR = CLASS_PATH + ".KeyguardUpdateMonitor";
    private static final String CLASS_KG_UPDATE_MONITOR_BATTERY_STATUS = 
            CLASS_PATH + ".KeyguardUpdateMonitor.BatteryStatus";
    private static final String CLASS_KG_WIDGET_PAGER = CLASS_PATH + ".KeyguardWidgetPager";
    private static final String CLASS_KG_ACTIVITY_LAUNCHER = CLASS_PATH + ".KeyguardActivityLauncher";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_ARC = false;
    private static final boolean DEBUG_KIS = false;

    private static final int STATUSBAR_DISABLE_RECENT = 0x01000000;
    private static final int STATUSBAR_DISABLE_NOTIFICATION_TICKER = 0x00080000;
    private static final int STATUSBAR_DISABLE_EXPAND = 0x00010000;
    private static final int STATUSBAR_DISABLE_SEARCH = 0x02000000;
    private static final int STATUSBAR_DISABLE_CLOCK = 0x00800000;

    private static final List<String> CLOCK_WIDGETS = new ArrayList<String>(Arrays.asList(
            "com.android.deskclock",
            "com.google.android.deskclock",
            "com.dvtonder.chronus",
            "net.nurik.roman.dashclock",
            "com.roymam.android.notificationswidget",
            "org.zooper.zwfree",
            "com.devexpert.weather",
            "ch.bitspin.timely"
    ));

    private static XSharedPreferences mPrefs;
    private static Class<?>[] mLaunchActivityArgs = new Class<?>[] 
            { Intent.class, boolean.class, boolean.class, Handler.class, Runnable.class };
    private static Object mKeyguardHostView;
    private static Handler mHandler;
    private static Context mGbContext;
    private static boolean mTorchEnabled;
    private static int mPrevGlowPadState;
    private static PointF mStartGlowPadPoint;
    private static float mDisplayDensity;
    private static boolean mReceiverRegistered;
    private static Class<?> mKgUpdateMonitorClass;
    private static boolean mBackgroundAlreadySet;
    private static boolean mIsLastScreenBackground;

    // Battery Arc
    private static HandleDrawable mHandleDrawable;
    private static Paint mArcPaint;
    private static RectF mArcRect;
    private static float mArcAngle = 0f;
    private static float mBatteryLevel;
    private static boolean mArcVisible;
    private static boolean mArcEnabled;
    private static View mGlowPadView;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(KeyguardImageService.ACTION_KEYGUARD_IMAGE_UPDATED)) {
                if (DEBUG_KIS) log("ACTION_KEYGUARD_IMAGE_UPDATED received");
                setLastScreenBackground(context);
            }
        }
    };

    public static void initPackageResources(final XSharedPreferences prefs, final InitPackageResourcesParam resparam) {
        try {
            boolean enableMenuKey = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_LOCKSCREEN_MENU_KEY, false);
            resparam.res.setReplacement(PACKAGE_NAME, "bool", "config_disableMenuKeyInLockScreen", !enableMenuKey);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            mPrefs = prefs;
            final Class<?> kgViewManagerClass = XposedHelpers.findClass(CLASS_KGVIEW_MANAGER, classLoader);
            final Class<?> kgHostViewClass = XposedHelpers.findClass(CLASS_KG_HOSTVIEW, classLoader);
            final Class<?> kgSelectorViewClass = XposedHelpers.findClass(CLASS_KG_SELECTOR_VIEW, classLoader);
            final Class<?> triggerListenerClass = XposedHelpers.findClass(CLASS_TRIGGER_LISTENER, classLoader);
            final Class<?> kgAbsKeyInputViewClass = XposedHelpers.findClass(CLASS_KG_ABS_KEY_INPUT_VIEW, classLoader);
            final Class<?> kgViewMediatorClass = XposedHelpers.findClass(CLASS_KGVIEW_MEDIATOR, classLoader);
            mKgUpdateMonitorClass = XposedHelpers.findClass(CLASS_KG_UPDATE_MONITOR, classLoader);
            final Class<?> kgWidgetPagerClass = XposedHelpers.findClass(CLASS_KG_WIDGET_PAGER, classLoader);
            final Class<?> kgActivityLauncherClass = XposedHelpers.findClass(CLASS_KG_ACTIVITY_LAUNCHER, classLoader);
            final Class<?> kgViewManagerHostClass = XposedHelpers.findClass(CLASS_KGVIEW_MANAGER_HOST, classLoader);

            XposedHelpers.findAndHookMethod(kgViewManagerClass, "maybeCreateKeyguardLocked", 
                    boolean.class, boolean.class, Bundle.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mPrefs.reload();

                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    if (!mReceiverRegistered) {
                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(KeyguardImageService.ACTION_KEYGUARD_IMAGE_UPDATED);
                        context.registerReceiver(mBroadcastReceiver, intentFilter);
                    }
                    if (mGbContext == null) {
                        mGbContext = context.createPackageContext(GravityBox.PACKAGE_NAME, 0);
                        if (DEBUG) log("mGbContext created");
                    }

                    final String bgType = prefs.getString(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND,
                            GravityBoxSettings.LOCKSCREEN_BG_DEFAULT);

                    Bitmap customBg = null;
                    mBackgroundAlreadySet = false;
                    if (bgType.equals(GravityBoxSettings.LOCKSCREEN_BG_COLOR)) {
                        int color = mPrefs.getInt(
                                GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR, Color.BLACK);
                        customBg = Utils.drawableToBitmap(new ColorDrawable(color));
                    } else if (bgType.equals(GravityBoxSettings.LOCKSCREEN_BG_IMAGE)) {
                        String wallpaperFile = mGbContext.getFilesDir() + "/lockwallpaper";
                        customBg = BitmapFactory.decodeFile(wallpaperFile);
                    } else if (bgType.equals(GravityBoxSettings.LOCKSCREEN_BG_LAST_SCREEN)) {
                        setLastScreenBackground(context);
                    }

                    if (customBg != null) {
                        if (mPrefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND_BLUR_EFFECT, false)) {
                            customBg = Utils.blurBitmap(context, customBg);
                        }
                        Object kgUpdateMonitor = XposedHelpers.callStaticMethod(mKgUpdateMonitorClass, 
                                "getInstance", context);
                        XposedHelpers.callMethod(kgUpdateMonitor, "dispatchSetBackground", customBg);
                        if (DEBUG) log("maybeCreateKeyguardLocked: custom wallpaper set");
                    }
                }
            });

            XposedHelpers.findAndHookMethod(kgViewManagerHostClass, "setCustomBackground",
                    Drawable.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final Drawable d = (Drawable) param.args[0];
                    if (d != null) {
                        mBackgroundAlreadySet = !mIsLastScreenBackground;
                        d.clearColorFilter();
                        final int alpha = (int) ((1 - prefs.getInt(
                                GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND_OPACITY, 50) / 100f) * 255);
                        final int overlayColor = Color.argb(alpha, 0, 0, 0);
                        d.setColorFilter(overlayColor, PorterDuff.Mode.SRC_OVER);
                        ((View)param.thisObject).invalidate();
                        if (DEBUG) log("setCustomBackground: custom background opacity set");
                    }
                    mIsLastScreenBackground = false;
                }
            });

            XposedHelpers.findAndHookMethod(kgViewManagerClass, 
                    "shouldEnableScreenRotation", new XC_MethodReplacement() {

                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            prefs.reload();
                            return prefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_ROTATION, false);
                        }
            });

            XposedHelpers.findAndHookMethod(kgHostViewClass, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mKeyguardHostView = param.thisObject; 
                    Object slidingChallenge = XposedHelpers.getObjectField(
                            param.thisObject, "mSlidingChallengeLayout");
                    minimizeChallengeIfDesired(slidingChallenge);

                    if (slidingChallenge != null) {
                        try {
                            // find lock button and assign long click listener
                            // we assume there's only 1 ImageButton in sliding challenge layout
                            // we have to do it this way since there's no ID assigned in layout XML
                            final ViewGroup vg = (ViewGroup) slidingChallenge;
                            final int childCount = vg.getChildCount();
                            for (int i = 0; i < childCount; i++) {
                                View v = vg.getChildAt(i);
                                if (v instanceof ImageButton) {
                                    v.setOnLongClickListener(mLockButtonLongClickListener);
                                    break;
                                }
                            }
                        } catch (Throwable t) {
                            XposedBridge.log(t);
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(kgHostViewClass, "onScreenTurnedOn", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Object slidingChallenge = XposedHelpers.getObjectField(
                            param.thisObject, "mSlidingChallengeLayout");
                    minimizeChallengeIfDesired(slidingChallenge);
                }
            });

            XposedHelpers.findAndHookMethod(kgSelectorViewClass, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (DEBUG) log("KeyGuardSelectorView onFinishInflate()");
                    prefs.reload();

                    final Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    final Resources res = context.getResources();
                    mGlowPadView = (View) XposedHelpers.getObjectField(param.thisObject, "mGlowPadView");
                    XposedHelpers.findAndHookMethod(mGlowPadView.getClass(), "showTargets",
                            boolean.class, glowPadViewShowTargetsHook);
                    XposedHelpers.findAndHookMethod(mGlowPadView.getClass(), "hideTargets",
                            boolean.class, boolean.class, glowPadViewHideTargetsHook);
                    XposedHelpers.findAndHookMethod(mGlowPadView.getClass(), "switchToState", 
                            int.class, float.class, float.class, glowPadViewSwitchToStateHook);
                    mHandler = new Handler();

                    // apply custom bottom/right margin to shift unlock ring upwards/left
                    try {
                        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mGlowPadView.getLayoutParams();
                        final int bottomMarginOffsetPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                                prefs.getInt(GravityBoxSettings.PREF_KEY_LOCKSCREEN_TARGETS_VERTICAL_OFFSET, 0),
                                res.getDisplayMetrics());
                        final int rightMarginOffsetPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                                prefs.getInt(GravityBoxSettings.PREF_KEY_LOCKSCREEN_TARGETS_HORIZONTAL_OFFSET, 0),
                                res.getDisplayMetrics());
                        lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin - rightMarginOffsetPx, 
                                lp.bottomMargin - bottomMarginOffsetPx);
                        mGlowPadView.setLayoutParams(lp);
                    } catch (Throwable t) {
                        log("Lockscreen targets: error while trying to modify GlowPadView layout" + t.getMessage());
                    }

                    mTorchEnabled = prefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_RING_TORCH, false);

                    mArcEnabled = prefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_BATTERY_ARC, false);
                    // prepare Battery Arc
                    if (mArcEnabled) {
                        mArcVisible = true;
                        if (mHandleDrawable == null) {
                            mHandleDrawable = new HandleDrawable(
                                    XposedHelpers.getObjectField(mGlowPadView, "mHandleDrawable"));

                            mArcPaint = new Paint();
                            mArcPaint.setStrokeWidth(10.0f);
                            mArcPaint.setStyle(Paint.Style.STROKE); 
                            mArcRect = new RectF(mHandleDrawable.getPositionX() - mHandleDrawable.getWidth()/2, 
                                    mHandleDrawable.getPositionY() - mHandleDrawable.getHeight()/2,
                                    mHandleDrawable.getPositionX() + mHandleDrawable.getWidth()/2, 
                                    mHandleDrawable.getPositionY() + mHandleDrawable.getHeight()/2);

                            XposedHelpers.findAndHookMethod(
                                    mGlowPadView.getClass(), "onDraw", Canvas.class, glowPadViewOnDrawHook);
                            if (DEBUG_ARC) log("Battery Arc initialized");
                        }
                        if (DEBUG_ARC) log("Battery Arc ready");
                    }

                    // finish if lockscreen targets disabled
                    if (!prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_TARGETS_ENABLE, false)) return;

                    @SuppressWarnings("unchecked")
                    final ArrayList<Object> targets = (ArrayList<Object>) XposedHelpers.getObjectField(
                            mGlowPadView, "mTargetDrawables");
                    final ArrayList<Object> newTargets = new ArrayList<Object>();
                    final ArrayList<String> newDescriptions = new ArrayList<String>();
                    final ArrayList<String> newDirections = new ArrayList<String>();
                    final ArrayList<AppInfo> appInfoList = new ArrayList<AppInfo>(5);
                    final int unlockDescResId = res.getIdentifier("description_target_unlock", 
                            "string", PACKAGE_NAME);
                    final int unlockDirecResId = res.getIdentifier("description_direction_right", 
                            "string", PACKAGE_NAME);

                    // fill appInfoList helper with apps from preferences
                    for (int i=0; i<=4; i++) {
                        String app = prefs.getString(
                                GravityBoxSettings.PREF_KEY_LOCKSCREEN_TARGETS_APP[i], null);
                        if (app != null) {
                            AppInfo appInfo = GlowPadHelper.getAppInfo(context, app);
                            if (appInfo != null) {
                                appInfoList.add(appInfo);
                                if (DEBUG) log("appInfoList.add: " + app);
                            }
                        }
                    }

                    // get target from position 0 supposing it's unlock ring
                    newTargets.add(targets.get(0));
                    newDescriptions.add(unlockDescResId == 0 ? null : res.getString(unlockDescResId));
                    newDirections.add(unlockDirecResId == 0 ? null : res.getString(unlockDirecResId));

                    // create and fill custom targets with proper layout based on number of targets
                    switch(appInfoList.size()) {
                        case 1:
                            newTargets.add(GlowPadHelper.createTargetDrawable(res, null));
                            newDescriptions.add(null);
                            newDirections.add(null);
                            newTargets.add(GlowPadHelper.createTargetDrawable(res, appInfoList.get(0)));
                            newDescriptions.add(appInfoList.get(0).name);
                            newDirections.add(null);
                            newTargets.add(GlowPadHelper.createTargetDrawable(res, null));
                            newDescriptions.add(null);
                            newDirections.add(null);
                            break;
                        case 2:
                            newTargets.add(GlowPadHelper.createTargetDrawable(res, appInfoList.get(0)));
                            newDescriptions.add(appInfoList.get(0).name);
                            newDirections.add(null);
                            newTargets.add(GlowPadHelper.createTargetDrawable(res, appInfoList.get(1)));
                            newDescriptions.add(appInfoList.get(1).name);
                            newDirections.add(null);
                            newTargets.add(GlowPadHelper.createTargetDrawable(res, null));
                            newDescriptions.add(null);
                            newDirections.add(null);
                            break;
                        case 3:
                        case 4:
                        case 5:
                            for (int i=0; i<=4; i++) {
                                if (i >= appInfoList.size()) {
                                    newTargets.add(GlowPadHelper.createTargetDrawable(res, null));
                                    newDescriptions.add(null);
                                } else {
                                    newTargets.add(GlowPadHelper.createTargetDrawable(res, appInfoList.get(i)));
                                    newDescriptions.add(appInfoList.get(i).name);
                                }
                                newDirections.add(null);
                            }
                            break;
                    }
                    XposedHelpers.setObjectField(mGlowPadView, "mTargetDrawables", newTargets);
                    XposedHelpers.setObjectField(mGlowPadView, "mTargetDescriptions", newDescriptions);
                    XposedHelpers.setObjectField(mGlowPadView, "mDirectionDescriptions", newDirections);
                    if (res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        XposedHelpers.setFloatField(mGlowPadView, "mFirstItemOffset", 0);
                    }
                    mGlowPadView.requestLayout();
                }
            });

            XposedHelpers.findAndHookMethod(triggerListenerClass, "onTrigger", 
                    View.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (DEBUG) log("GlowPadView.OnTriggerListener; index=" + ((Integer) param.args[1]));
                    prefs.reload();
                    if (!prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_TARGETS_ENABLE, false)) return;

                    final int index = (Integer) param.args[1];
                    @SuppressWarnings("unchecked")
                    final ArrayList<Object> targets = (ArrayList<Object>) XposedHelpers.getObjectField(
                            mGlowPadView, "mTargetDrawables");
                    final Object td = targets.get(index);

                    AppInfo appInfo = (AppInfo) XposedHelpers.getAdditionalInstanceField(td, "mGbAppInfo");
                    if (appInfo != null) {
                        // if intent is a GB action of broadcast type, handle it directly here
                        if (ShortcutActivity.isGbBroadcastShortcut(appInfo.intent)) {
                            Intent newIntent = new Intent(appInfo.intent.getStringExtra(ShortcutActivity.EXTRA_ACTION));
                            newIntent.putExtras(appInfo.intent);
                            mGlowPadView.getContext().sendBroadcast(newIntent);
                            XposedHelpers.callMethod(mGlowPadView, "doFinish");
                        // otherwise start activity
                        } else {
                            final Object activityLauncher = XposedHelpers.getObjectField(
                                    XposedHelpers.getSurroundingThis(param.thisObject), "mActivityLauncher");
                            XposedHelpers.callMethod(activityLauncher, "launchActivity", mLaunchActivityArgs,
                                    appInfo.intent, false, true, null, null);
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(kgAbsKeyInputViewClass, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final TextView passwordEntry = 
                            (TextView) XposedHelpers.getObjectField(param.thisObject, "mPasswordEntry");
                    if (passwordEntry != null) {
                        passwordEntry.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void afterTextChanged(Editable s) {
                                if (!mPrefs.getBoolean(
                                        GravityBoxSettings.PREF_KEY_LOCKSCREEN_QUICK_UNLOCK, false)) return;

                                final Object callback = 
                                        XposedHelpers.getObjectField(param.thisObject, "mCallback");
                                final Object lockPatternUtils = 
                                        XposedHelpers.getObjectField(param.thisObject, "mLockPatternUtils");
                                String entry = passwordEntry.getText().toString();

                                if (callback != null && lockPatternUtils != null &&
                                        entry.length() > 3 && 
                                        (Boolean) XposedHelpers.callMethod(
                                                lockPatternUtils, "checkPassword", entry)) {
                                    XposedHelpers.callMethod(callback, "reportSuccessfulUnlockAttempt");
                                    XposedHelpers.callMethod(callback, "dismiss", true);
                                }
                            }
                            @Override
                            public void beforeTextChanged(CharSequence arg0,
                                    int arg1, int arg2, int arg3) {
                            }
                            @Override
                            public void onTextChanged(CharSequence arg0,
                                    int arg1, int arg2, int arg3) {
                            }
                        });
                    }
                }
            });

            XposedHelpers.findAndHookMethod(kgViewMediatorClass, "adjustStatusBarLocked", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    int policy = GravityBoxSettings.SBL_POLICY_DEFAULT;
                    try {
                        policy = Integer.valueOf(prefs.getString(
                            GravityBoxSettings.PREF_KEY_STATUSBAR_LOCK_POLICY, "0"));
                    } catch (NumberFormatException nfe) {
                        //
                    }
                    if (DEBUG) log("Statusbar lock policy = " + policy);
                    if (policy == GravityBoxSettings.SBL_POLICY_DEFAULT) return;

                    final Object sbManager = 
                            XposedHelpers.getObjectField(param.thisObject, "mStatusBarManager");
                    final Context context = 
                            (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    final boolean showing = XposedHelpers.getBooleanField(param.thisObject, "mShowing");

                    if (showing && sbManager != null && !(context instanceof Activity)) {
                        int flags = STATUSBAR_DISABLE_RECENT;
                        if ((Boolean) XposedHelpers.callMethod(param.thisObject, "isSecure")) {
                            flags |= STATUSBAR_DISABLE_EXPAND;
                            flags |= STATUSBAR_DISABLE_NOTIFICATION_TICKER;
                        } else if (policy == GravityBoxSettings.SBL_POLICY_LOCKED) {
                            flags |= STATUSBAR_DISABLE_EXPAND;
                        }

                        try {
                            Method m = XposedHelpers.findMethodExact(
                                    kgViewMediatorClass, "isAssistantAvailable", new Object[0]);
                            if (!(Boolean) m.invoke(param.thisObject)) {
                                flags |= STATUSBAR_DISABLE_SEARCH;
                            }
                        } catch(NoSuchMethodError nme) {
                            if (DEBUG) log("isAssistantAvailable method doesn't exist (Android < 4.2.2?)");
                        }

                        XposedHelpers.callMethod(sbManager, "disable", flags);
                        if (DEBUG) log("adjustStatusBarLocked: new flags = " + flags);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(kgHostViewClass, "numWidgets", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_WIDGET_LIMIT_DISABLE, false)) {
                        param.setResult(0);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(mKgUpdateMonitorClass, "handleBatteryUpdate",
                    CLASS_KG_UPDATE_MONITOR_BATTERY_STATUS, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mArcEnabled) {
                        updateLockscreenBattery(param.args[0]);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(kgWidgetPagerClass, "onPageSwitched",
                    View.class, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final View v = (View) param.thisObject;
                    final int visibilityMode = Integer.valueOf(mPrefs.getString(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_STATUSBAR_CLOCK, "0"));

                    if (visibilityMode != 0) {
                        v.setSystemUiVisibility(visibilityMode == 1 ?
                                v.getSystemUiVisibility() | STATUSBAR_DISABLE_CLOCK :
                                    v.getSystemUiVisibility() & ~STATUSBAR_DISABLE_CLOCK);
                    } else if (param.args[0] instanceof ViewGroup) {
                        final ViewGroup vg = (ViewGroup) param.args[0];
                        if (vg.getChildAt(0) instanceof AppWidgetHostView) {
                            final AppWidgetProviderInfo info = 
                                    ((AppWidgetHostView) vg.getChildAt(0)).getAppWidgetInfo();
                            final String widgetPackage = info.provider.getPackageName();
                            if (DEBUG) log("onPageSwitched: widget package = " + widgetPackage);
                            final boolean disableClock = CLOCK_WIDGETS.contains(widgetPackage);
                            v.setSystemUiVisibility(v.getSystemUiVisibility() |
                                    (disableClock ? STATUSBAR_DISABLE_CLOCK : 0));
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(kgActivityLauncherClass, "launchActivity",
                    Intent.class, boolean.class, boolean.class, Handler.class, Runnable.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.args[0];
                    if ("android.appwidget.action.KEYGUARD_APPWIDGET_PICK".equals(intent.getAction()) &&
                            prefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_ALLOW_ANY_WIDGET, false)) {
                        intent.removeExtra("categoryFilter");
                        intent.putExtra("categoryFilter", AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
                        Bundle options = new Bundle();
                        options.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
                        intent.removeExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS);
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
                    }
                }
            });

        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static boolean shouldDrawBatteryArc() {
        return (mArcEnabled && mHandleDrawable != null && 
                mArcVisible && (mArcAngle > 0));
    }

    private static XC_MethodHook glowPadViewOnDrawHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            Canvas canvas = (Canvas) param.args[0];
            if (canvas != null && shouldDrawBatteryArc()) {
                try {
                    mHandleDrawable.set(XposedHelpers.getObjectField(param.thisObject, "mHandleDrawable"));
                    mArcRect.set(mHandleDrawable.getPositionX() - mHandleDrawable.getWidth()/3,
                            mHandleDrawable.getPositionY() - mHandleDrawable.getHeight()/3,
                            mHandleDrawable.getPositionX() + mHandleDrawable.getWidth()/3,
                            mHandleDrawable.getPositionY() + mHandleDrawable.getHeight()/3);
                    canvas.drawArc(mArcRect, -90, mArcAngle, false, mArcPaint);
                } catch (Throwable t) {
                    log("GlowPadView onDraw exception: " + t.getMessage());
                }
            }
        }
    };

    private static XC_MethodHook glowPadViewShowTargetsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            mArcVisible = false;
        }
    };

    private static XC_MethodHook glowPadViewHideTargetsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            mArcVisible = true;
        }
    };

    private static XC_MethodHook glowPadViewSwitchToStateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            if (mTorchEnabled && mHandler != null && param.thisObject == mGlowPadView) {
                final int state = (Integer) param.args[0];
                final float x = (Float) param.args[1];
                final float y = (Float) param.args[2];
                if (DEBUG) log("state=" + state + "; x=" + x + "; y=" + y);

                if (state == 2) {
                    mHandler.postDelayed(mToggleTorchRunnable, 1000);
                } else if (state == 3) {
                    if (mPrevGlowPadState == 2) {
                        mStartGlowPadPoint = new PointF(x,y);
                        mDisplayDensity = mGlowPadView.getResources().getDisplayMetrics().density;
                    } else {
                        double distance = Math.sqrt(Math.pow(x - mStartGlowPadPoint.x,2) +
                                Math.pow(y - mStartGlowPadPoint.y, 2)) / mDisplayDensity;
                        if (DEBUG) log("distance=" + distance);
                        if (distance > 15) {
                            mHandler.removeCallbacks(mToggleTorchRunnable);
                        }
                    }
                } else {
                    mHandler.removeCallbacks(mToggleTorchRunnable);
                }
                mPrevGlowPadState = state;
            }
        }
    };

    private static Runnable mToggleTorchRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                Intent intent = new Intent(mGbContext, TorchService.class);
                intent.setAction(TorchService.ACTION_TOGGLE_TORCH);
                mGbContext.startService(intent);
            } catch (Throwable t) {
                log("Error toggling Torch: " + t.getMessage());
            }
        }
    };

    private static void minimizeChallengeIfDesired(Object challenge) {
        if (challenge == null) return;

        if (mPrefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_MAXIMIZE_WIDGETS, false)) {
            if (DEBUG) log("minimizeChallengeIfDesired: challenge minimized");
            XposedHelpers.callMethod(challenge, "showChallenge", false);
        }
    }

    private static final OnLongClickListener mLockButtonLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            if (mKeyguardHostView == null) return false;
            try {
                XposedHelpers.callMethod(mKeyguardHostView, "showNextSecurityScreenOrFinish", false);
                return true;
            } catch (Throwable t) {
                XposedBridge.log(t);
                return false;
            }
        }
    };

    static class HandleDrawable {
        Object mHd;

        public HandleDrawable(Object handleDrawable) {
            mHd = handleDrawable;
        }

        public void set(Object handleDrawable) {
            mHd = handleDrawable;
        }

        public float getAlpha() {
            return (Float) XposedHelpers.callMethod(mHd, "getAlpha");
        }

        public float getPositionX() {
            return (Float) XposedHelpers.callMethod(mHd, "getPositionX");
        }

        public float getPositionY() {
            return (Float) XposedHelpers.callMethod(mHd, "getPositionY");
        }

        public int getWidth() {
            return (Integer) XposedHelpers.callMethod(mHd, "getWidth");
        }

        public int getHeight() {
            return (Integer) XposedHelpers.callMethod(mHd, "getHeight");
        }
    }

    private static void updateLockscreenBattery(Object status) {
        if (status != null) { 
            mBatteryLevel = XposedHelpers.getFloatField(status, "level");
            if (DEBUG_ARC) log("BatteryStatus: mBatteryLevel = " + mBatteryLevel);
        }

        float cappedBattery = mBatteryLevel;
        if (mBatteryLevel < 15) {
            cappedBattery = 15;
        } else if (mBatteryLevel > 90) {
            cappedBattery = 90;
        }

        final float hue = (cappedBattery - 15) * 1.6f;
        mArcAngle = mBatteryLevel * 3.6f;
        if (mArcPaint != null) {
            mArcPaint.setColor(Color.HSVToColor(0x80, new float[]{ hue, 1.f, 1.f }));
        }
        if (mGlowPadView != null) {
            mGlowPadView.invalidate();
        }
        if (DEBUG_ARC) log("Lockscreen battery arc updated");
    }

    private static void setLastScreenBackground(Context context) {
        if (mBackgroundAlreadySet) {
            if (DEBUG_KIS) log("setLastScreenBackground: Background has been already set (album art?)");
            return;
        }
        try {
            String kisImageFile = mGbContext.getFilesDir() + "/kis_image.png";
            Bitmap customBg = BitmapFactory.decodeFile(kisImageFile);
            if (customBg != null) {
                if (mPrefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND_BLUR_EFFECT, false)) {
                    customBg = Utils.blurBitmap(context, customBg);
                }
                Object kgUpdateMonitor = XposedHelpers.callStaticMethod(mKgUpdateMonitorClass, 
                        "getInstance", context);
                mIsLastScreenBackground = true;
                XposedHelpers.callMethod(kgUpdateMonitor, "dispatchSetBackground", customBg);
                if (DEBUG_KIS) log("setLastScreenBackground: Last screen background updated");
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
