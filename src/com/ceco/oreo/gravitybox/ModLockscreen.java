/*
 * Copyright (C) 2018 Peter Gregus for GravityBox Project (C3C076@xda)
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

package com.ceco.oreo.gravitybox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ceco.oreo.gravitybox.ModStatusBar.StatusBarState;
import com.ceco.oreo.gravitybox.ledcontrol.QuietHours;
import com.ceco.oreo.gravitybox.ledcontrol.QuietHoursActivity;
import com.ceco.oreo.gravitybox.managers.AppLauncher;
import com.ceco.oreo.gravitybox.managers.KeyguardStateMonitor;
import com.ceco.oreo.gravitybox.managers.SysUiManagers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    public static final String PACKAGE_NAME = "com.android.systemui";

    private static final String CLASS_KG_PASSWORD_VIEW = CLASS_PATH + ".KeyguardPasswordView";
    private static final String CLASS_KG_PIN_VIEW = CLASS_PATH + ".KeyguardPINView";
    private static final String CLASS_KG_PASSWORD_TEXT_VIEW = CLASS_PATH + ".PasswordTextView";
    private static final String CLASS_KG_PASSWORD_TEXT_VIEW_PIN = CLASS_PATH + ".PasswordTextViewForPin";
    private static final String CLASS_KGVIEW_MEDIATOR = "com.android.systemui.keyguard.KeyguardViewMediator";
    private static final String CLASS_LOCK_PATTERN_VIEW = "com.android.internal.widget.LockPatternView";
    private static final String ENUM_DISPLAY_MODE = "com.android.internal.widget.LockPatternView.DisplayMode";
    private static final String CLASS_SB_WINDOW_MANAGER = "com.android.systemui.statusbar.phone.StatusBarWindowManager";
    private static final String CLASS_KG_VIEW_MANAGER = "com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager";
    private static final String CLASS_CARRIER_TEXT = CLASS_PATH + ".CarrierText";
    private static final String CLASS_NOTIF_ROW = "com.android.systemui.statusbar.ExpandableNotificationRow";
    private static final String CLASS_KG_BOTTOM_AREA_VIEW = "com.android.systemui.statusbar.phone.KeyguardBottomAreaView";
    private static final String CLASS_SCRIM_CONTROLLER = "com.android.systemui.statusbar.phone.ScrimController";

    private static final boolean DEBUG = false;
    private static final boolean DEBUG_KIS = false;

    private static int MSG_SMART_UNLOCK = 1;
    private static int MSG_DIRECT_UNLOCK = 2;

    private static enum DirectUnlock { OFF, STANDARD, SEE_THROUGH };
    private static enum UnlockPolicy { DEFAULT, NOTIF_NONE, NOTIF_ONGOING };

    private static XSharedPreferences mPrefs;
    private static Context mContext;
    private static Context mGbContext;
    private static Bitmap mCustomBg;
    private static QuietHours mQuietHours;
    private static Object mStatusBar;
    private static DirectUnlock mDirectUnlock = DirectUnlock.OFF;
    private static UnlockPolicy mDirectUnlockPolicy = UnlockPolicy.DEFAULT;
    private static LockscreenAppBar mAppBar;
    private static boolean mSmartUnlock;
    private static UnlockPolicy mSmartUnlockPolicy;
    private static UnlockHandler mUnlockHandler;
    private static GestureDetector mGestureDetector;
    private static List<TextView> mCarrierTextViews = new ArrayList<>();
    private static KeyguardStateMonitor mKgMonitor;
    private static LockscreenPinScrambler mPinScrambler;
    private static AppLauncher.AppInfo mLeftAction;
    private static AppLauncher.AppInfo mRightAction;
    private static Drawable mLeftActionDrawableOrig;
    private static Drawable mRightActionDrawableOrig;
    private static boolean mLeftActionHidden;
    private static boolean mRightActionHidden;

    private static boolean mInStealthMode;
    private static Object mPatternDisplayMode; 

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(GravityBoxSettings.ACTION_LOCKSCREEN_SETTINGS_CHANGED)
                 || action.equals(GravityBoxSettings.ACTION_PREF_LOCKSCREEN_BG_CHANGED)) {
                mPrefs.reload();
                prepareCustomBackground();
                prepareBottomActions();
                if (DEBUG) log("Settings reloaded");
            } else if (action.equals(KeyguardImageService.ACTION_KEYGUARD_IMAGE_UPDATED)) {
                if (DEBUG_KIS) log("ACTION_KEYGUARD_IMAGE_UPDATED received");
                setLastScreenBackground(true);
            } else if (action.equals(QuietHoursActivity.ACTION_QUIET_HOURS_CHANGED)) {
                mQuietHours = new QuietHours(intent.getExtras());
                if (DEBUG) log("QuietHours settings reloaded");
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_LOCKSCREEN_SHORTCUT_CHANGED)) {
                if (mAppBar != null) {
                    if (intent.hasExtra(GravityBoxSettings.EXTRA_LS_SHORTCUT_SLOT)) {
                        mAppBar.updateAppSlot(intent.getIntExtra(GravityBoxSettings.EXTRA_LS_SHORTCUT_SLOT, 0),
                            intent.getStringExtra(GravityBoxSettings.EXTRA_LS_SHORTCUT_VALUE));
                    }
                    if (intent.hasExtra(GravityBoxSettings.EXTRA_LS_SAFE_LAUNCH)) {
                        mAppBar.setSafeLaunchEnabled(intent.getBooleanExtra(
                                GravityBoxSettings.EXTRA_LS_SAFE_LAUNCH, false));
                    }
                    if (intent.hasExtra(GravityBoxSettings.EXTRA_LS_SHOW_BADGES)) {
                        mAppBar.setShowBadges(intent.getBooleanExtra(
                                GravityBoxSettings.EXTRA_LS_SHOW_BADGES, false));
                    }
                }
            } else if (action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
                if (mAppBar != null)
                    mAppBar.initAppSlots();
                prepareBottomActions();
            }
        }
    };

    public static String getUmcInsecureFieldName() {
        switch (Build.VERSION.SDK_INT) {
            default: return "mCanSkipBouncer";
        }
    }

    public static void initResources(final XSharedPreferences prefs, final InitPackageResourcesParam resparam) {
        try {
            // Lockscreen: disable menu key in lock screen
            Utils.TriState triState = Utils.TriState.valueOf(prefs.getString(
                    GravityBoxSettings.PREF_KEY_LOCKSCREEN_MENU_KEY, "DEFAULT"));
            if (DEBUG) log(GravityBoxSettings.PREF_KEY_LOCKSCREEN_MENU_KEY + ": " + triState);
            if (triState != Utils.TriState.DEFAULT) {
                resparam.res.setReplacement(PACKAGE_NAME, "bool", "config_disableMenuKeyInLockScreen",
                        triState == Utils.TriState.DISABLED);
                if (DEBUG) log("config_disableMenuKeyInLockScreen: " + (triState == Utils.TriState.DISABLED));
            }
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void init(final XSharedPreferences prefs, final XSharedPreferences qhPrefs, final ClassLoader classLoader) {
        try {
            mPrefs = prefs;
            mQuietHours = new QuietHours(qhPrefs);

            final Class<?> kgPasswordViewClass = XposedHelpers.findClass(CLASS_KG_PASSWORD_VIEW, classLoader);
            final Class<?> kgPINViewClass = XposedHelpers.findClass(CLASS_KG_PIN_VIEW, classLoader);
            final Class<?> kgPasswordTextViewClass = XposedHelpers.findClass(CLASS_KG_PASSWORD_TEXT_VIEW, classLoader);
            final Class<?> kgViewMediatorClass = XposedHelpers.findClass(CLASS_KGVIEW_MEDIATOR, classLoader);
            final Class<?> lockPatternViewClass = XposedHelpers.findClass(CLASS_LOCK_PATTERN_VIEW, classLoader);
            final Class<? extends Enum> displayModeEnum = (Class<? extends Enum>) XposedHelpers.findClass(ENUM_DISPLAY_MODE, classLoader);
            final Class<?> sbWindowManagerClass = XposedHelpers.findClass(CLASS_SB_WINDOW_MANAGER, classLoader); 

            String setupMethodName = "setupLocked";
            XposedHelpers.findAndHookMethod(kgViewMediatorClass, setupMethodName, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    mGbContext = Utils.getGbContext(mContext);
                    if (SysUiManagers.KeyguardMonitor == null) {
                        SysUiManagers.createKeyguardMonitor(mContext, mPrefs);
                    }
                    mKgMonitor = SysUiManagers.KeyguardMonitor;
                    mKgMonitor.setMediator(param.thisObject);
                    mKgMonitor.setUpdateMonitor(XposedHelpers.getObjectField(param.thisObject, "mUpdateMonitor"));

                    prepareCustomBackground();
                    prepareGestureDetector();

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_LOCKSCREEN_SETTINGS_CHANGED);
                    intentFilter.addAction(KeyguardImageService.ACTION_KEYGUARD_IMAGE_UPDATED);
                    intentFilter.addAction(QuietHoursActivity.ACTION_QUIET_HOURS_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_LOCKSCREEN_BG_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_LOCKSCREEN_SHORTCUT_CHANGED);
                    intentFilter.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED);
                    mContext.registerReceiver(mBroadcastReceiver, intentFilter);
                    if (DEBUG) log("Keyguard mediator constructed");
                }
            });

            XposedHelpers.findAndHookMethod(ModStatusBar.CLASS_STATUSBAR, classLoader,
                    "updateMediaMetaData", boolean.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mStatusBar == null) {
                        mStatusBar = param.thisObject;
                    }

                    int state = XposedHelpers.getIntField(mStatusBar, "mState");
                    if (state != StatusBarState.KEYGUARD && state != StatusBarState.SHADE_LOCKED) {
                        if (DEBUG) log("updateMediaMetaData: Invalid status bar state: " + state);
                        return;
                    }

                    View backDrop = (View) XposedHelpers.getObjectField(mStatusBar, "mBackdrop");
                    ImageView backDropBack = (ImageView) XposedHelpers.getObjectField(
                            mStatusBar, "mBackdropBack");
                    if (backDrop == null || backDropBack == null) {
                        if (DEBUG) log("updateMediaMetaData: called too early");
                        return;
                    }

                    boolean hasArtwork = false;
                    MediaMetadata mm = (MediaMetadata) XposedHelpers.getObjectField(
                            mStatusBar, "mMediaMetadata");
                    if (mm != null) {
                        hasArtwork = mm.getBitmap(MediaMetadata.METADATA_KEY_ART) != null ||
                                mm.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null;
                    }
                    if (DEBUG) log("updateMediaMetaData: hasArtwork=" + hasArtwork);

                    // custom background
                    if (!hasArtwork && mCustomBg != null) {
                        backDrop.animate().cancel();
                        backDropBack.animate().cancel();
                        backDropBack.setImageBitmap(mCustomBg);
                        if ((Boolean) XposedHelpers.getBooleanField(
                                mStatusBar, "mScrimSrcModeEnabled")) {
                            PorterDuffXfermode xferMode = (PorterDuffXfermode) XposedHelpers
                                    .getObjectField(mStatusBar, "mSrcXferMode");
                            XposedHelpers.callMethod(backDropBack.getDrawable().mutate(),
                                    "setXfermode", xferMode);
                        }
                        backDrop.setVisibility(View.VISIBLE);
                        backDrop.animate().alpha(1f);
                        if (DEBUG) log("updateMediaMetaData: showing custom background");
                    }

                    // opacity
                    if (hasArtwork || mCustomBg != null) {
                        backDropBack.getDrawable().clearColorFilter();
                        final int opacity = mPrefs.getInt(
                                GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND_OPACITY, 100);
                        if (opacity != 100) {
                            final int alpha = (int) ((1 - opacity / 100f) * 255);
                            final int overlayColor = Color.argb(alpha, 0, 0, 0);
                            backDropBack.getDrawable().mutate()
                                .setColorFilter(overlayColor, PorterDuff.Mode.SRC_OVER);
                            if (DEBUG) log("updateMediaMetaData: opacity set");
                        }
                    }
                }
            });

            final Utils.TriState triState = Utils.TriState.valueOf(prefs.getString(
                    GravityBoxSettings.PREF_KEY_LOCKSCREEN_ROTATION, "DEFAULT"));
            if (triState != Utils.TriState.DEFAULT) {
                XposedHelpers.findAndHookMethod(sbWindowManagerClass, "shouldEnableKeyguardScreenRotation",
                        new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG) log("shouldEnableKeyguardScreenRotation called");
                        try {
                            if (Utils.isMtkDevice()) {
                                return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                            } else {
                                return (triState == Utils.TriState.ENABLED);
                            }
                        } catch (Throwable t) {
                            GravityBox.log(TAG, t);
                            return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                        }
                    }
                });
            }

            XposedHelpers.findAndHookMethod(kgPasswordViewClass, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (!mPrefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_QUICK_UNLOCK, false)) return;

                    final TextView passwordEntry = 
                            (TextView) XposedHelpers.getObjectField(param.thisObject, "mPasswordEntry");
                    if (passwordEntry == null) return;

                    passwordEntry.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void afterTextChanged(Editable s) {
                            doQuickUnlock(param.thisObject, passwordEntry.getText().toString());
                        }
                        @Override
                        public void beforeTextChanged(CharSequence arg0,int arg1, int arg2, int arg3) { }
                        @Override
                        public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
                    });
                }
            });

            XposedHelpers.findAndHookMethod(kgPINViewClass, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_PIN_SCRAMBLE, false)) {
                        mPinScrambler = new LockscreenPinScrambler((ViewGroup)param.thisObject);
                        if (Utils.isXperiaDevice()) {
                            mPinScrambler.scramble();
                        }
                    }
                    if (mPrefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_QUICK_UNLOCK, false)) {
                        final View passwordEntry = 
                                (View) XposedHelpers.getObjectField(param.thisObject, "mPasswordEntry");
                        if (passwordEntry != null) {
                            XposedHelpers.setAdditionalInstanceField(passwordEntry, "gbPINView",
                                    param.thisObject);
                        }
                    }
                }
            });

            if (!Utils.isXperiaDevice()) {
                XposedHelpers.findAndHookMethod(kgPINViewClass, "resetState", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_PIN_SCRAMBLE, false) &&
                                mPinScrambler != null) {
                            mPinScrambler.scramble();
                        }
                    }
                });
            }

            XposedHelpers.findAndHookMethod(kgPasswordTextViewClass, "append", char.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (!mPrefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_QUICK_UNLOCK, false)) return;

                    Object pinView = XposedHelpers.getAdditionalInstanceField(param.thisObject, "gbPINView");
                    if (pinView != null) {
                        if (DEBUG) log("quickUnlock: PasswordText belongs to PIN view");
                        String entry = (String) XposedHelpers.getObjectField(param.thisObject, "mText");
                        doQuickUnlock(pinView, entry);
                    }
                }
            });

            if (Utils.isOxygenOsRom()) {
                XposedHelpers.findAndHookMethod(CLASS_KG_PASSWORD_TEXT_VIEW_PIN, classLoader,
                        "append", char.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        if (!mPrefs.getBoolean(
                                GravityBoxSettings.PREF_KEY_LOCKSCREEN_QUICK_UNLOCK, false)) return;
                        Object pinView = XposedHelpers.getAdditionalInstanceField(param.thisObject, "gbPINView");
                        if (pinView != null) {
                            if (DEBUG) log("quickUnlock: OnePlus3T PasswordTextViewForPin");
                            String entry = (String) XposedHelpers.getObjectField(param.thisObject, "mText");
                            doQuickUnlock(pinView, entry);
                        }
                    }
                });
            }

            XposedHelpers.findAndHookMethod(lockPatternViewClass, "onDraw", Canvas.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    beforeLockPatternDraw(displayModeEnum, param.thisObject);
                }

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    afterLockPatternDraw(param.thisObject);
                }
            });

            XposedHelpers.findAndHookMethod(kgViewMediatorClass, "playSounds", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mQuietHours.isSystemSoundMuted(QuietHours.SystemSound.SCREEN_LOCK)) {
                        param.setResult(null);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(CLASS_KG_VIEW_MANAGER, classLoader, "onScreenTurnedOff",
                    new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mKgMonitor.unregisterListener(mKgStateListener);
                    mDirectUnlock = DirectUnlock.valueOf(prefs.getString(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_DIRECT_UNLOCK, "OFF"));
                    mDirectUnlockPolicy = UnlockPolicy.valueOf(prefs.getString(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_DIRECT_UNLOCK_POLICY, "DEFAULT"));
                    mSmartUnlock = prefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_SMART_UNLOCK, false);
                    mSmartUnlockPolicy = UnlockPolicy.valueOf(prefs.getString(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_SMART_UNLOCK_POLICY, "DEFAULT"));
                    if (mUnlockHandler == null) {
                        mUnlockHandler = new UnlockHandler();
                    } else {
                        mUnlockHandler.removeMessages(MSG_DIRECT_UNLOCK);
                        mUnlockHandler.removeMessages(MSG_SMART_UNLOCK);
                    }
                    updateCarrierText();
                }
            });

            XposedHelpers.findAndHookMethod(CLASS_KG_VIEW_MANAGER, classLoader, "onStartedWakingUp",
                    new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (!mKgMonitor.isSecured() || mUnlockHandler == null) {
                        if (DEBUG) log("onScreenTurnedOn: noop as keyguard is not secured");
                        return;
                    }

                    if (!mKgMonitor.isTrustManaged()) {
                        if (mDirectUnlock != DirectUnlock.OFF) {
                            mUnlockHandler.sendEmptyMessageDelayed(MSG_DIRECT_UNLOCK, 300);
                        }
                    } else if (mSmartUnlock) {
                        mKgMonitor.registerListener(mKgStateListener);
                        if (!mKgMonitor.isLocked()) {
                            // previous state is insecure so we rather wait a second as smart lock can still
                            // decide to make it secure after a while. Seems to be necessary only for
                            // on-body detection. Other smart lock methods seem to always start with secured state
                            if (DEBUG) log("onScreenTurnedOn: Scheduling smart unlock");
                            mUnlockHandler.sendEmptyMessageDelayed(MSG_SMART_UNLOCK, 1000);
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(ModStatusBar.CLASS_NOTIF_PANEL_VIEW, classLoader,
                    "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    ViewGroup kgStatusView = (ViewGroup) XposedHelpers.getObjectField(
                            param.thisObject, "mKeyguardStatusView");
                    Resources res = kgStatusView.getResources();
                    // try mtk container first
                    int containerId = res.getIdentifier("mtk_keyguard_clock_container",
                            "id", PACKAGE_NAME);
                    if (containerId == 0) {
                        // fallback to AOSP container
                        containerId = res.getIdentifier("keyguard_clock_container", "id", PACKAGE_NAME);
                    }
                    if (containerId != 0) {
                        ViewGroup container = (ViewGroup) kgStatusView.findViewById(containerId);
                        if (Build.VERSION.SDK_INT >= 27) {
                            container = (ViewGroup) container.getParent();
                        }
                        if (container != null) {
                            mAppBar = new LockscreenAppBar(mContext, mGbContext, container,
                                    param.thisObject, prefs);
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(ModStatusBar.CLASS_NOTIF_PANEL_VIEW, classLoader,
                    "onTouchEvent", MotionEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mPrefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_D2TS, false) &&
                            mGestureDetector != null &&
                            (int) XposedHelpers.callMethod(
                                XposedHelpers.getObjectField(param.thisObject, "mStatusBar"),
                                "getBarState") == StatusBarState.KEYGUARD) {
                        mGestureDetector.onTouchEvent((MotionEvent) param.args[0]);
                    }
                }
            });

            if (!Utils.isXperiaDevice()) {
                XC_MethodHook carrierTextHook = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                        if (Utils.isOxygenOsRom()) {
                            String text = mPrefs.getString(GravityBoxSettings.PREF_KEY_LOCKSCREEN_CARRIER_TEXT, "");
                            if (!text.isEmpty()) {
                                param.setResult(text.trim().isEmpty() ? "" : text);
                            }
                        }
                    }
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        if (!mCarrierTextViews.contains(param.thisObject)) {
                            mCarrierTextViews.add((TextView) param.thisObject);
                        }
                        if (!Utils.isOxygenOsRom()) {
                            String text = mPrefs.getString(GravityBoxSettings.PREF_KEY_LOCKSCREEN_CARRIER_TEXT, "");
                            if (text.isEmpty()) {
                                return;
                            } else {
                                ((TextView)param.thisObject).setText(text.trim().isEmpty() ? "" : text);
                            }
                        }
                    }
                };

                if (Utils.isOxygenOsRom()) {
                    XposedHelpers.findAndHookMethod(CLASS_CARRIER_TEXT,
                            classLoader, "updateCarrierTextInternal", carrierTextHook);
                } else {
                    XposedHelpers.findAndHookMethod(CLASS_CARRIER_TEXT,
                            classLoader, "updateCarrierText", carrierTextHook);
                }
            }

            // bottom actions
            try {
                if (!Utils.isSamsungRom()) {
                    XposedHelpers.findAndHookMethod(CLASS_KG_BOTTOM_AREA_VIEW, classLoader,
                            "updateLeftAffordanceIcon", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                            ImageView v = (ImageView) XposedHelpers.getObjectField(
                                    param.thisObject, "mLeftAffordanceView");
                            if (mLeftActionHidden) {
                                v.setVisibility(View.GONE);
                            } else if (mLeftAction != null) {
                                v.setVisibility(View.VISIBLE);
                                if (mLeftActionDrawableOrig == null) {
                                    mLeftActionDrawableOrig = v.getDrawable();
                                }
                                v.setImageDrawable(mLeftAction.getAppIcon());
                                v.setContentDescription(mLeftAction.getAppName());
                            } else if (mLeftActionDrawableOrig != null) {
                                v.setImageDrawable(mLeftActionDrawableOrig);
                                mLeftActionDrawableOrig = null;
                            }
                        }
                    });

                    XposedHelpers.findAndHookMethod(CLASS_KG_BOTTOM_AREA_VIEW, classLoader,
                            "launchLeftAffordance", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            if (mLeftAction != null) {
                                SysUiManagers.AppLauncher.startActivity(mContext, mLeftAction.getIntent());
                                param.setResult(null);
                            }
                        }
                    });
                } else {
                    XposedHelpers.findAndHookMethod(CLASS_KG_BOTTOM_AREA_VIEW, classLoader,
                            "launchPhone", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            if (mLeftAction != null) {
                                SysUiManagers.AppLauncher.startActivity(mContext, mLeftAction.getIntent());
                                param.setResult(null);
                            }
                        }
                    });
                }

                XposedHelpers.findAndHookMethod(CLASS_KG_BOTTOM_AREA_VIEW, classLoader,
                         "updateCameraVisibility", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        ImageView v;
                        if (Utils.isSamsungRom()) {
                            v = (ImageView) XposedHelpers.getObjectField(
                                    param.thisObject, "mLeftAffordanceView");
                            if (mLeftActionHidden) {
                                v.setVisibility(View.GONE);
                            } else if (mLeftAction != null) {
                                v.setVisibility(View.VISIBLE);
                                if (mLeftActionDrawableOrig == null) {
                                    mLeftActionDrawableOrig = v.getDrawable();
                                }
                                v.setImageDrawable(mLeftAction.getAppIcon());
                                v.setContentDescription(mLeftAction.getAppName());
                            } else if (mLeftActionDrawableOrig != null) {
                                v.setImageDrawable(mLeftActionDrawableOrig);
                                mLeftActionDrawableOrig = null;
                            }
                            v = (ImageView) XposedHelpers.getObjectField(
                                    param.thisObject, "mRightAffordanceView");
                        } else {
                            v = (ImageView) XposedHelpers.getObjectField(
                                   param.thisObject, "mRightAffordanceView");
                        }
                        if (mRightActionHidden) {
                            v.setVisibility(View.GONE);
                        } else if (mRightAction != null) {
                            v.setVisibility(View.VISIBLE);
                            if (mRightActionDrawableOrig == null) {
                                mRightActionDrawableOrig = v.getDrawable();
                            }
                            v.setImageDrawable(mRightAction.getAppIcon());
                            v.setContentDescription(mRightAction.getAppName());
                        } else if (mRightActionDrawableOrig != null) {
                            v.setImageDrawable(mRightActionDrawableOrig);
                            mRightActionDrawableOrig = null;
                        }
                    }
                });

                XposedBridge.hookAllMethods(XposedHelpers.findClass(CLASS_KG_BOTTOM_AREA_VIEW, classLoader),
                         "launchCamera", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                        if (mRightAction != null) {
                            SysUiManagers.AppLauncher.startActivity(mContext, mRightAction.getIntent());
                            param.setResult(null);
                        }
                    }
                });

                XposedHelpers.findAndHookMethod(CLASS_KG_BOTTOM_AREA_VIEW, classLoader,
                        "onVisibilityChanged", View.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        if (param.thisObject == param.args[0] &&
                                (int)param.args[1] == View.VISIBLE &&
                                (mLeftAction != null || mLeftActionDrawableOrig != null)) {
                            XposedHelpers.callMethod(param.thisObject, Utils.isSamsungRom() ? "updateCameraVisibility" : "updateLeftAffordanceIcon");
                        }
                    }
                });

            } catch (Throwable t) {
                GravityBox.log(TAG, t);
            }

            // Direct unlock see through transparency level
            if (Build.VERSION.SDK_INT < 27) {
                try {
                    XposedHelpers.findAndHookMethod(CLASS_SCRIM_CONTROLLER, classLoader,
                            "updateScrimKeyguard", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            if (mDirectUnlock == DirectUnlock.SEE_THROUGH &&
                                !(XposedHelpers.getBooleanField(param.thisObject, "mDarkenWhileDragging")) &&
                                XposedHelpers.getBooleanField(param.thisObject, "mBouncerShowing")) {
                                float alpha = 1 - (float)mPrefs.getInt(GravityBoxSettings
                                        .PREF_KEY_LOCKSCREEN_DIRECT_UNLOCK_TRANS_LEVEL, 75) / 100f;
                                XposedHelpers.callMethod(param.thisObject, "setScrimInFrontColor", alpha);
                                XposedHelpers.callMethod(param.thisObject, "setScrimBehindColor", 0f);
                                param.setResult(null);
                            }
                        }
                    });
                } catch (Throwable t) {
                    GravityBox.log(TAG, t);
                }
            }
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
        }
    } 

    private static KeyguardStateMonitor.Listener mKgStateListener = new KeyguardStateMonitor.Listener() {
        @Override
        public void onKeyguardStateChanged() {
            final boolean trustManaged = mKgMonitor.isTrustManaged();
            final boolean insecure = !mKgMonitor.isLocked();
            if (DEBUG) log("updateMethodSecure: trustManaged=" + trustManaged +
                    "; insecure=" + insecure);
            if (trustManaged && insecure) {
                // either let already queued message to be handled or handle new one immediately
                if (!mUnlockHandler.hasMessages(MSG_SMART_UNLOCK)) {
                    mUnlockHandler.sendEmptyMessage(MSG_SMART_UNLOCK);
                }
            } else if (mUnlockHandler.hasMessages(MSG_SMART_UNLOCK)) {
                // smart lock decided to make it secure so remove any pending dismiss keyguard messages
                mUnlockHandler.removeMessages(MSG_SMART_UNLOCK);
                if (DEBUG) log("updateMethodSecure: pending smart unlock cancelled");
            }
            if (mKgMonitor.isShowing()) {
                mKgMonitor.unregisterListener(this);
            }
        }

        @Override
        public void onScreenStateChanged(boolean interactive) { }
    };

    private static boolean canTriggerDirectUnlock() {
        return (mDirectUnlock != DirectUnlock.OFF &&
                    canTriggerUnlock(mDirectUnlockPolicy));
    }

    private static boolean canTriggerSmartUnlock() {
        return (mSmartUnlock && canTriggerUnlock(mSmartUnlockPolicy));
    }

    private static boolean canTriggerUnlock(UnlockPolicy policy) {
        if (policy == UnlockPolicy.DEFAULT) return true;

        try {
            ViewGroup stack = (ViewGroup) XposedHelpers.getObjectField(mStatusBar, "mStackScroller");
            int childCount = stack.getChildCount();
            int notifCount = 0;
            int notifClearableCount = 0;
            for (int i=0; i<childCount; i++) {
                View v = stack.getChildAt(i);
                if (v.getVisibility() != View.VISIBLE ||
                        !v.getClass().getName().equals(CLASS_NOTIF_ROW))
                    continue;
                notifCount++;
                if ((boolean) XposedHelpers.callMethod(v, "isClearable")) {
                    notifClearableCount++;
                }
            }
            return (policy == UnlockPolicy.NOTIF_NONE) ?
                    notifCount == 0 : notifClearableCount == 0;
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
            return true;
        }
    }

    private static class UnlockHandler extends Handler {
        public UnlockHandler() {
            super();
        }

        @Override
        public void handleMessage(Message msg) { 
            if (msg.what == MSG_SMART_UNLOCK) {
                if (canTriggerSmartUnlock()) {
                    mKgMonitor.dismissKeyguard();
                }
            } else if (msg.what == MSG_DIRECT_UNLOCK) {
                if (canTriggerDirectUnlock()) {
                    if (mDirectUnlock == DirectUnlock.SEE_THROUGH) {
                        showBouncer();
                    } else {
                        makeExpandedInvisible();
                    }
                }
            }
        }
    };

    private static void showBouncer() {
        try {
            XposedHelpers.callMethod(mStatusBar, "showBouncer");
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
        }
    }

    private static void makeExpandedInvisible() {
        try {
            XposedHelpers.callMethod(mStatusBar, "makeExpandedInvisible");
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
        }
    }

    private static void doQuickUnlock(final Object securityView, final String entry) {
        if (entry.length() != mPrefs.getInt(
                GravityBoxSettings.PREF_KEY_LOCKSCREEN_PIN_LENGTH, 4)) return;

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Object lockPatternUtils = XposedHelpers.getObjectField(securityView, "mLockPatternUtils");
                    final int userId = mKgMonitor.getCurrentUserId();
                    final boolean valid = (boolean) XposedHelpers.callMethod(lockPatternUtils, "checkPassword", entry, userId);
                    if (valid) {
                        final Object callback = XposedHelpers.getObjectField(securityView, "mCallback");
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    XposedHelpers.callMethod(callback, "reportUnlockAttempt", userId, true, 0);
                                    XposedHelpers.callMethod(callback, "dismiss", true, userId);
                                } catch (Throwable t) {
                                    GravityBox.log(TAG, "Error dimissing keyguard: ", t);
                                }
                            }
                        });
                    }
                } catch (Throwable t) {
                    GravityBox.log(TAG, t);;
                }
            }
        });
    }

    private static synchronized void prepareCustomBackground() {
        try {
            if (mCustomBg != null) {
                mCustomBg = null;
            }
            final String bgType = mPrefs.getString(
                  GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND,
                  GravityBoxSettings.LOCKSCREEN_BG_DEFAULT);
    
            if (bgType.equals(GravityBoxSettings.LOCKSCREEN_BG_COLOR)) {
                int color = mPrefs.getInt(
                      GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR, Color.BLACK);
                mCustomBg = Utils.drawableToBitmap(new ColorDrawable(color));
            } else if (bgType.equals(GravityBoxSettings.LOCKSCREEN_BG_IMAGE)) {
                String wallpaperFile = mGbContext.getFilesDir() + "/lockwallpaper";
                mCustomBg = BitmapFactory.decodeFile(wallpaperFile);
            } else if (bgType.equals(GravityBoxSettings.LOCKSCREEN_BG_LAST_SCREEN)) {
                setLastScreenBackground(false);
            }
    
            if (!bgType.equals(GravityBoxSettings.LOCKSCREEN_BG_LAST_SCREEN) &&
                    mCustomBg != null && mPrefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND_BLUR_EFFECT, false)) {
                mCustomBg = Utils.blurBitmap(mContext, mCustomBg, mPrefs.getInt(
                          GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND_BLUR_INTENSITY, 14));
            }
            if (DEBUG) log("prepareCustomBackground: type=" + bgType);
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
        }
    }

    private static synchronized void setLastScreenBackground(boolean refresh) {
        try {
            String kisImageFile = mGbContext.getFilesDir() + "/kis_image.png";
            mCustomBg = BitmapFactory.decodeFile(kisImageFile);
            if (refresh && mStatusBar != null) {
                XposedHelpers.callMethod(mStatusBar, "updateMediaMetaData", false, false);
            }
            if (DEBUG_KIS) log("setLastScreenBackground: Last screen background updated");
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void beforeLockPatternDraw(final Class<? extends Enum> displayModeEnum, final Object thisObject) {
        final Object patternDisplayMode = XposedHelpers.getObjectField(thisObject, "mPatternDisplayMode");
        final Boolean inStealthMode = XposedHelpers.getBooleanField(thisObject, "mInStealthMode");  

        if (!mPrefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_SHOW_PATTERN_ERROR, true) &&
                    mPatternDisplayMode == null && patternDisplayMode == Enum.valueOf(displayModeEnum, "Wrong")) {
            mInStealthMode = inStealthMode;
            mPatternDisplayMode = patternDisplayMode;
            XposedHelpers.setBooleanField(thisObject, "mInStealthMode", true);
            XposedHelpers.setObjectField(thisObject, "mPatternDisplayMode", Enum.valueOf(displayModeEnum, "Correct"));
        } else {
            mPatternDisplayMode = null;
        }
    }

    private static void afterLockPatternDraw(final Object thisObject) {
        if (null != mPatternDisplayMode) {
            XposedHelpers.setBooleanField(thisObject, "mInStealthMode", mInStealthMode);
            XposedHelpers.setObjectField(thisObject, "mPatternDisplayMode", mPatternDisplayMode);
            mInStealthMode = false;
            mPatternDisplayMode = null;
        }
    }

    private static void prepareGestureDetector() {
        try {
            mGestureDetector = new GestureDetector(mContext, 
                    new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    Intent intent = new Intent(ModHwKeys.ACTION_SLEEP);
                    mContext.sendBroadcast(intent);
                    return true;
                }
            });
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
        }
    }

    private static void updateCarrierText() {
        for (TextView tv : mCarrierTextViews) {
            try {
                XposedHelpers.callMethod(tv, "updateCarrierText");
            } catch (Throwable t) {
                GravityBox.log(TAG, t);
            }
        }
    }

    private static void prepareBottomActions() {
        Set<String> hiddenActions = mPrefs.getStringSet(
                GravityBoxSettings.PREF_KEY_LOCKSCREEN_BOTTOM_ACTIONS_HIDE,
                new HashSet<String>());
        mLeftActionHidden = hiddenActions.contains("LEFT");
        mRightActionHidden = hiddenActions.contains("RIGHT");
        prepareLeftAction(mLeftActionHidden ? null : mPrefs.getString(
                GravityBoxSettings.PREF_KEY_LOCKSCREEN_BLEFT_ACTION_CUSTOM, null));
        prepareRightAction(mRightActionHidden ? null :  mPrefs.getString(
                GravityBoxSettings.PREF_KEY_LOCKSCREEN_BRIGHT_ACTION_CUSTOM, null));
    }

    private static void prepareLeftAction(String action) {
        if (action == null || action.isEmpty()) {
            mLeftAction = null;
        } else if (SysUiManagers.AppLauncher != null &&
                (mLeftAction == null || !action.equals(mLeftAction.getValue()))) {
            mLeftAction = SysUiManagers.AppLauncher.createAppInfo();
            mLeftAction.setSizeDp(32);
            mLeftAction.initAppInfo(action);
            String pkg = mLeftAction.getPackageName();
            if (pkg != null && pkg.equals(Utils.getDefaultDialerPackageName(mContext))) {
                mLeftAction.setAppIcon(tryGetStockPhoneIcon(
                        mLeftAction.getAppIcon()));
            }
        }
    }

    private static void prepareRightAction(String action) {
        if (action == null || action.isEmpty()) {
            mRightAction = null;
        } else if (SysUiManagers.AppLauncher != null &&
                (mRightAction == null || !action.equals(mRightAction.getValue()))) {
            mRightAction = SysUiManagers.AppLauncher.createAppInfo();
            mRightAction.setSizeDp(32);
            mRightAction.initAppInfo(action);
            String pkg = mRightAction.getPackageName();
            if (pkg != null && pkg.equals(Utils.getDefaultDialerPackageName(mContext))) {
                mRightAction.setAppIcon(tryGetStockPhoneIcon(
                        mRightAction.getAppIcon()));
            }
        }
    }

    private static Drawable tryGetStockPhoneIcon(Drawable def) {
        try {
            int resId = mContext.getResources().getIdentifier(
                    "ic_phone_24dp", "drawable", PACKAGE_NAME);
            return (resId == 0 ? def : mContext.getDrawable(resId));
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
            return def;
        }
    }
}
