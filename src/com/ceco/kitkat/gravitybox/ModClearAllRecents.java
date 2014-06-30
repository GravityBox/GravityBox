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
import java.util.List;



import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ImageView.ScaleType;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ceco.kitkat.gravitybox.R;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class ModClearAllRecents {
    private static final String TAG = "GB:ModClearAllRecents";
    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final String CLASS_RECENT_VERTICAL_SCROLL_VIEW = "com.android.systemui.recent.RecentsVerticalScrollView";
    public static final String CLASS_RECENT_HORIZONTAL_SCROLL_VIEW = "com.android.systemui.recent.RecentsHorizontalScrollView";
    public static final String CLASS_RECENT_PANEL_VIEW = "com.android.systemui.recent.RecentsPanelView";
    public static final String CLASS_RECENT_ACTIVITY = "com.android.systemui.recent.RecentsActivity";
    private static final boolean DEBUG = false;

    public static final String NAVBAR_RECENTS_CLEAR_ALL = "navbarRecentsClearAll";
    public static final String EXTRA_NAVBAR_RECENTS_CLEAR_ALL = "navbarRecentsClearAllShow";

    private static XSharedPreferences mPrefs;
    private static ImageView mRecentsClearButton;
    private static int mClearRecentsMode;
    private static Activity mRecentsActivity;
    private static Handler mFinishHandler;

    // RAM bar
    private static TextView mBackgroundProcessText;
    private static TextView mForegroundProcessText;
    private static ActivityManager mAm;
    private static MemInfoReader mMemInfoReader;
    private static Context mGbContext;
    private static LinearColorBar mRamUsageBar;
    private static Handler mHandler;
    private static int[] mRamUsageBarPaddings;
    private static int mClearAllRecentsSizePx;
    private static int mRamUsageBarVerticalMargin;
    private static int mRamUsageBarHorizontalMargin;
    private static boolean mPreserveCurrentTask;
    private static boolean mNavbarAlwaysOnBottom;
    private static View mRecentsPanelView;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("Broadcast received: " + intent.toString());
            if (intent.getAction().equals(ModHwKeys.ACTION_RECENTS_CLEAR_ALL_SINGLETAP)) {
                clearAll(false);
            } else if (intent.getAction().equals(ModHwKeys.ACTION_RECENTS_CLEAR_ALL_LONGPRESS)) {
                clearAll(true);
            }
        }
    };

    private static Runnable mFinishRunnable = new Runnable() {
        @Override
        public void run() {
            if (mRecentsActivity != null) {
                setRecentsClearAll(false, mRecentsActivity);
                mRecentsActivity.finish();
                mRecentsActivity = null;
            }
            mFinishHandler = null;
        }
    };

    public static void init(final XSharedPreferences prefs, ClassLoader classLoader) {
        try {
            mPrefs = prefs;
            Class<?> recentPanelViewClass = XposedHelpers.findClass(CLASS_RECENT_PANEL_VIEW, classLoader);
            Class<?> recentActivityClass = XposedHelpers.findClass(CLASS_RECENT_ACTIVITY, classLoader);
            Class<?> recentVerticalScrollView = XposedHelpers.findClass(CLASS_RECENT_VERTICAL_SCROLL_VIEW, classLoader);
            Class<?> recentHorizontalScrollView = XposedHelpers.findClass(CLASS_RECENT_HORIZONTAL_SCROLL_VIEW, classLoader);

            mNavbarAlwaysOnBottom = mPrefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_ALWAYS_ON_BOTTOM, false);

            mMemInfoReader = new MemInfoReader();

            XposedHelpers.findAndHookMethod(recentPanelViewClass, "showImpl", 
                    boolean.class, recentsPanelViewShowHook);

            XposedBridge.hookAllConstructors(recentPanelViewClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mRecentsPanelView = (View) param.thisObject;
                    Context context = mRecentsPanelView.getContext();
                    mGbContext = context.createPackageContext(GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
                    mHandler = new Handler();
                    mAm = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

                    final Resources res = context.getResources();
                    mRamUsageBarPaddings = new int[4];
                    mRamUsageBarPaddings[0] = mRamUsageBarPaddings[2] = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 4, res.getDisplayMetrics());
                    mRamUsageBarPaddings[1] = mRamUsageBarPaddings[3] = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 1, res.getDisplayMetrics());
                    mClearAllRecentsSizePx = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 50, res.getDisplayMetrics());
                    mRamUsageBarVerticalMargin = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 15, res.getDisplayMetrics());
                    mRamUsageBarHorizontalMargin = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 10, res.getDisplayMetrics());
                    if (DEBUG) log("Recents panel view constructed");
                }
            });

            XposedHelpers.findAndHookMethod(recentPanelViewClass, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    int caGravity = Integer.valueOf(mPrefs.getString(
                            GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_ALL, "53"));
                    final int rbGravity = Integer.valueOf(mPrefs.getString(
                            GravityBoxSettings.PREF_KEY_RAMBAR, "0"));
                    Boolean createClearAll = (caGravity != GravityBoxSettings.RECENT_CLEAR_OFF && caGravity != GravityBoxSettings.RECENT_CLEAR_NAVIGATION_BAR);
                    Boolean createRamBar = (rbGravity != 0);
                    if (!createClearAll && !createRamBar) {
                        return;
                    }

                    View view = (View) param.thisObject;
                    Resources res = view.getResources();
                    ViewGroup vg = (ViewGroup) view.findViewById(res.getIdentifier("recents_bg_protect", "id", PACKAGE_NAME));

                    // create and inject new ImageView and set onClick listener to handle action
                    if (createClearAll) {
                        mRecentsClearButton = new ImageView(vg.getContext());
                        mRecentsClearButton.setImageDrawable(res.getDrawable(res.getIdentifier(
                                "ic_notify_clear", "drawable", PACKAGE_NAME)));
                        mRecentsClearButton.setBackground(mGbContext.getResources().getDrawable(
                                R.drawable.image_view_button_bg));
                        FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(
                                mClearAllRecentsSizePx, mClearAllRecentsSizePx);
                        mRecentsClearButton.setLayoutParams(lParams);
                        mRecentsClearButton.setScaleType(ScaleType.CENTER);
                        mRecentsClearButton.setClickable(true);
                        mRecentsClearButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                               clearAll(false);
                            }
                        });
                        mRecentsClearButton.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                clearAll(true);
                                return true;
                            }
                        });
                        mRecentsClearButton.setVisibility(View.GONE);
                        vg.addView(mRecentsClearButton);
                        if (DEBUG) log("clearAllButton ImageView injected");
                    }

                    if (createRamBar) {
                        // create and inject RAM bar
                        mRamUsageBar = new LinearColorBar(vg.getContext(), null);
                        mRamUsageBar.setOrientation(LinearLayout.HORIZONTAL);
                        mRamUsageBar.setClipChildren(false);
                        mRamUsageBar.setClipToPadding(false);
                        mRamUsageBar.setPadding(mRamUsageBarPaddings[0], mRamUsageBarPaddings[1],
                                mRamUsageBarPaddings[2], mRamUsageBarPaddings[3]);
                        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                        mRamUsageBar.setLayoutParams(flp);
                        LayoutInflater inflater = LayoutInflater.from(mGbContext);
                        inflater.inflate(R.layout.linear_color_bar, mRamUsageBar, true);
                        vg.addView(mRamUsageBar);
                        mForegroundProcessText = (TextView) mRamUsageBar.findViewById(R.id.foregroundText);
                        mBackgroundProcessText = (TextView) mRamUsageBar.findViewById(R.id.backgroundText);
                        mRamUsageBar.setVisibility(View.GONE);
                        if (DEBUG) log("RAM bar injected");
                    }
                }
            });

            // for portrait mode
            XposedHelpers.findAndHookMethod(recentVerticalScrollView, "dismissChild", View.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    handleDismissChild(param);
                }
            });

            // for landscape mode
            XposedHelpers.findAndHookMethod(recentHorizontalScrollView, "dismissChild", View.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    handleDismissChild(param);
                }
            });

            // When to update RAM bar values
            XposedHelpers.findAndHookMethod(recentPanelViewClass, "clearRecentTasksList", 
                    updateRambarHook);
            XposedHelpers.findAndHookMethod(recentPanelViewClass, "handleSwipe",
                    View.class, updateRambarHook);
            XposedHelpers.findAndHookMethod(recentPanelViewClass, "refreshViews", 
                    updateRambarHook);

            XposedHelpers.findAndHookMethod(recentActivityClass, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(ModHwKeys.ACTION_RECENTS_CLEAR_ALL_SINGLETAP);
                    intentFilter.addAction(ModHwKeys.ACTION_RECENTS_CLEAR_ALL_LONGPRESS);
                    mRecentsActivity = ((Activity)param.thisObject);
                    mRecentsActivity.registerReceiver(mBroadcastReceiver, intentFilter);
                    if (mFinishHandler != null) {
                        mFinishHandler.removeCallbacks(mFinishRunnable);
                        mFinishHandler = null;
                    }
                    mFinishHandler = new Handler();
                    if (DEBUG) log("Broadcast receiver registered");
                }
            });

            XposedHelpers.findAndHookMethod(recentActivityClass, "onPause", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    mRecentsActivity.unregisterReceiver(mBroadcastReceiver);
                    mFinishHandler.postDelayed(mFinishRunnable, 500);
                    if (DEBUG) log("Broadcast receiver unregistered");
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static XC_MethodHook updateRambarHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
            if (XposedHelpers.getObjectField(param.thisObject, "mRecentTaskDescriptions") == null ||
                    XposedHelpers.getObjectField(param.thisObject, "mRecentTasksLoader") == null) {
                param.setResult(null);
            }
        }
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            if (mRamUsageBar != null && mRamUsageBar.getVisibility() == View.VISIBLE && mHandler != null) {
                mHandler.post(updateRamBarTask);
            }
        }
    };

    private static XC_MethodHook recentsPanelViewShowHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            try {
                mClearRecentsMode = Integer.valueOf(mPrefs.getString(GravityBoxSettings.PREF_KEY_CLEAR_RECENTS_MODE, "0"));
                Boolean show = (Boolean) param.args[0];
                if (show) {
                    mPrefs.reload();
                    updateButtonLayout((View) param.thisObject);
                    updateRamBarLayout();
                }
                List<?> recentTaskDescriptions = (List<?>) XposedHelpers.getObjectField(param.thisObject, "mRecentTaskDescriptions");
                boolean visible = (recentTaskDescriptions != null && recentTaskDescriptions.size() > 0);
                int gravity = Integer.valueOf(mPrefs.getString(GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_ALL, "53"));
                setRecentsClearAll(show && visible && gravity == GravityBoxSettings.RECENT_CLEAR_NAVIGATION_BAR, 
                        ((View) param.thisObject).getContext());
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static void setRecentsClearAll(Boolean show, Context context) {
        Intent intent = new Intent(NAVBAR_RECENTS_CLEAR_ALL);
        intent.putExtra(EXTRA_NAVBAR_RECENTS_CLEAR_ALL, show);
        context.sendBroadcast(intent);
        if (DEBUG) log("setRecentsClearAll broadcast sent");
    }

    private static void updateButtonLayout(View container) {
        if (mRecentsClearButton == null) return;

        final Context context = mRecentsClearButton.getContext();
        int gravity = Integer.valueOf(mPrefs.getString(
                GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_ALL, "53"));
        List<?> recentTaskDescriptions = (List<?>) XposedHelpers.getObjectField(
                container, "mRecentTaskDescriptions");
        boolean visible = (recentTaskDescriptions != null && recentTaskDescriptions.size() > 0);
        if (gravity != GravityBoxSettings.RECENT_CLEAR_OFF && gravity != GravityBoxSettings.RECENT_CLEAR_NAVIGATION_BAR && visible) {
            final Resources res = mRecentsClearButton.getResources();
            final int orientation = res.getConfiguration().orientation;
            FrameLayout.LayoutParams lparams = 
                    (FrameLayout.LayoutParams) mRecentsClearButton.getLayoutParams();
            lparams.gravity = gravity;
            if (gravity == 51 || gravity == 53) {
                int marginTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                        mPrefs.getInt(GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_MARGIN_TOP, 0), 
                        res.getDisplayMetrics());
                int marginRight = (gravity == 53 && orientation == Configuration.ORIENTATION_LANDSCAPE
                        && Utils.isPhoneUI(context) && !mNavbarAlwaysOnBottom) ?
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                                mPrefs.getInt(GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_MARGIN_BOTTOM, 0), 
                                res.getDisplayMetrics()): 0;
                lparams.setMargins(0, marginTop, marginRight, 0);
            } else {
                int marginBottom = (orientation == Configuration.ORIENTATION_PORTRAIT || 
                                        !Utils.isPhoneUI(context) || mNavbarAlwaysOnBottom) ?
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                        mPrefs.getInt(GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_MARGIN_BOTTOM, 0), 
                        res.getDisplayMetrics()) : 0;
                int marginRight = (gravity == 85 && orientation == Configuration.ORIENTATION_LANDSCAPE
                        && Utils.isPhoneUI(context) && !mNavbarAlwaysOnBottom) ?
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                                mPrefs.getInt(GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_MARGIN_BOTTOM, 0), 
                                res.getDisplayMetrics()): 0;
                lparams.setMargins(0, 0, marginRight, marginBottom);
            }
            mRecentsClearButton.setLayoutParams(lparams);
            mRecentsClearButton.setVisibility(View.VISIBLE);
        } else {
            mRecentsClearButton.setVisibility(View.GONE);
        }
        if (DEBUG) log("Clear all recents button layout updated");
    }

    private static void handleDismissChild(final MethodHookParam param) {
        // skip if non-null view passed - fall back to original method
        if (param.args[0] != null)
            return;

        if (DEBUG) log("handleDismissChild - removing all views");

        // scroll recents tasks view to show the first task clearing will start from
        if (param.thisObject instanceof ScrollView) {
            ((ScrollView) param.thisObject).smoothScrollTo(0, 0);
        } else if (param.thisObject instanceof HorizontalScrollView) {
            ((HorizontalScrollView)param.thisObject).smoothScrollTo(0, 0);
        }

        // start clearing task after 200ms delay to give some time for smooth scroll to finish
        final LinearLayout mLinearLayout = (LinearLayout) XposedHelpers.getObjectField(param.thisObject, "mLinearLayout");
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final int count = Utils.isXperiaDevice() ? (mLinearLayout.getChildCount() - 1) :
                    mLinearLayout.getChildCount();
                for (int i = 0; i < count; i++) {
                    final View child = mLinearLayout.getChildAt(i);
                    final int index = i;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (mPreserveCurrentTask && index == (count-1)) {
                                    Object callback = XposedHelpers.getObjectField(param.thisObject, "mCallback");
                                    if (callback != null) {
                                        XposedHelpers.callMethod(callback, "handleOnClick", child);
                                    }
                                } else {
                                    Object[] newArgs = new Object[] { child };
                                    XposedBridge.invokeOriginalMethod(param.method, param.thisObject, newArgs);
                                }
                            } catch (Throwable t) {
                                XposedBridge.log(t);
                            }
                        }
                    }, 150 * i);
                }
            }
        }, 200);

        if (mRamUsageBar != null && mRamUsageBar.getVisibility() == View.VISIBLE && mHandler != null) {
            mHandler.post(updateRamBarTask);
        }

        // don't call original method
        param.setResult(null);
    }

    private static void updateRamBarLayout() {
        if (mRamUsageBar == null) return;

        final int rbGravity = Integer.valueOf(mPrefs.getString(
                GravityBoxSettings.PREF_KEY_RAMBAR, "0"));
        if (rbGravity == 0) {
            mRamUsageBar.setVisibility(View.GONE);
        } else {
            final Context context = mRamUsageBar.getContext();
            final Resources res = mRamUsageBar.getResources();
            final int orientation = res.getConfiguration().orientation;
            final int caGravity = Integer.valueOf(mPrefs.getString(
                    GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_ALL, "53"));
            final boolean caOnTop = (caGravity & Gravity.TOP) == Gravity.TOP;
            final boolean caOnLeft = (caGravity & Gravity.LEFT) == Gravity.LEFT;
            final boolean rbOnTop = (rbGravity == Gravity.TOP);
            final boolean sibling = (mRecentsClearButton != null && 
                    mRecentsClearButton.getVisibility() == View.VISIBLE) && 
                    ((caOnTop && rbOnTop) || (!caOnTop && !rbOnTop));
            final int marginTop = rbOnTop ? (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                    mPrefs.getInt(GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_MARGIN_TOP, 0), 
                    res.getDisplayMetrics()) : 0;
            final int marginBottom = (!rbOnTop && (orientation == Configuration.ORIENTATION_PORTRAIT ||
                                                    !Utils.isPhoneUI(context) || mNavbarAlwaysOnBottom)) ? 
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                    mPrefs.getInt(GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_MARGIN_BOTTOM, 0), 
                    res.getDisplayMetrics()) : 0;
            final int marginRight = orientation == Configuration.ORIENTATION_LANDSCAPE && 
                                                        Utils.isPhoneUI(context) && !mNavbarAlwaysOnBottom ?
                            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                                    mPrefs.getInt(GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_MARGIN_BOTTOM, 0), 
                                    res.getDisplayMetrics()) : 0;

            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) mRamUsageBar.getLayoutParams();
            flp.gravity = rbGravity;
            flp.setMargins(
                sibling && caOnLeft ? mClearAllRecentsSizePx : mRamUsageBarHorizontalMargin, 
                rbOnTop ? (mRamUsageBarVerticalMargin + marginTop) : 0, 
                sibling && !caOnLeft ? (mClearAllRecentsSizePx + marginRight) : 
                    (mRamUsageBarHorizontalMargin + marginRight), 
                rbOnTop ? 0 : (mRamUsageBarVerticalMargin + marginBottom)
            );
            mRamUsageBar.setLayoutParams(flp);
            mRamUsageBar.setVisibility(View.VISIBLE);
            mHandler.post(updateRamBarTask);
        }
        if (DEBUG) log("RAM bar layout updated");
    }

    private static final Runnable updateRamBarTask = new Runnable() {
        @Override
        public void run() {
            if (mRamUsageBar == null || mRamUsageBar.getVisibility() == View.GONE) {
                return;
            }

            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            mAm.getMemoryInfo(memInfo);
            long secServerMem = 0;//XposedHelpers.getLongField(memInfo, "secondaryServerThreshold");
            mMemInfoReader.readMemInfo();
            long availMem = mMemInfoReader.getFreeSize() + mMemInfoReader.getCachedSize() -
                    secServerMem;
            long totalMem = mMemInfoReader.getTotalSize();

            String sizeStr = Formatter.formatShortFileSize(mGbContext, totalMem-availMem);
            mForegroundProcessText.setText(mGbContext.getResources().getString(
                    R.string.service_foreground_processes, sizeStr));
            sizeStr = Formatter.formatShortFileSize(mGbContext, availMem);
            mBackgroundProcessText.setText(mGbContext.getResources().getString(
                    R.string.service_background_processes, sizeStr));

            float fTotalMem = totalMem;
            float fAvailMem = availMem;
            mRamUsageBar.setRatios((fTotalMem - fAvailMem) / fTotalMem, 0, 0);
            if (DEBUG) log("RAM bar values updated");
        }
    };

    private static final void clearAll(boolean longPress) {
        try {
            if (mRecentsPanelView != null) {
                Method m = XposedHelpers.findMethodExact(mRecentsPanelView.getClass(), "isShowing");
                Boolean isShowing = (Boolean) m.invoke(mRecentsPanelView);
                if (isShowing) {
                    ViewGroup recentsContainer = (ViewGroup) XposedHelpers.getObjectField(mRecentsPanelView, "mRecentsContainer");
                    if (!longPress) {
                        mPreserveCurrentTask = (mClearRecentsMode == 1);
                    } else {
                        mPreserveCurrentTask = (mClearRecentsMode == 0);
                   }
                   recentsContainer.removeViewInLayout(null);
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
