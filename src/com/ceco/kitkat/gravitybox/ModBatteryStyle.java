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

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

public class ModBatteryStyle {
    private static final String TAG = "GB:ModBatteryStyle";
    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    public static final String CLASS_BATTERY_CONTROLLER = 
            "com.android.systemui.statusbar.policy.BatteryController";
    private static final boolean DEBUG = false;

    private static final String ACTION_MTK_BATTERY_PERCENTAGE_SWITCH = 
            "mediatek.intent.action.BATTERY_PERCENTAGE_SWITCH";
    public static final String EXTRA_MTK_BATTERY_PERCENTAGE_STATE = "state";
    public static final String SETTING_MTK_BATTERY_PERCENTAGE = "battery_percentage";

    private static int mBatteryStyle;
    private static boolean mBatteryPercentTextEnabled;
    private static boolean mMtkPercentTextEnabled;
    private static StatusbarBatteryPercentage mPercentText;
    private static CmCircleBattery mCircleBattery;
    private static View mStockBattery;
    private static KitKatBattery mKitKatBattery;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_BATTERY_STYLE_CHANGED) &&
                    intent.hasExtra(GravityBoxSettings.EXTRA_BATTERY_STYLE)) {
                        mBatteryStyle = intent.getIntExtra(GravityBoxSettings.EXTRA_BATTERY_STYLE, 1);
                        if (DEBUG) log("mBatteryStyle changed to: " + mBatteryStyle);
                        updateBatteryStyle();
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_BATTERY_PERCENT_TEXT_CHANGED) &&
                    intent.hasExtra(GravityBoxSettings.EXTRA_BATTERY_PERCENT_TEXT)) {
                        mBatteryPercentTextEnabled = intent.getBooleanExtra(GravityBoxSettings.EXTRA_BATTERY_PERCENT_TEXT, false);
                        if (DEBUG) log("mPercentText changed to: " + mBatteryPercentTextEnabled);
                        updateBatteryStyle();
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_BATTERY_PERCENT_TEXT_SIZE_CHANGED) &&
                    intent.hasExtra(GravityBoxSettings.EXTRA_BATTERY_PERCENT_TEXT_SIZE) && mPercentText != null) {
                        int textSize = intent.getIntExtra(GravityBoxSettings.EXTRA_BATTERY_PERCENT_TEXT_SIZE, 16);
                        mPercentText.setTextSize(textSize);
                        if (DEBUG) log("PercentText size changed to: " + textSize);
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_BATTERY_PERCENT_TEXT_STYLE_CHANGED)
                           && mPercentText != null) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_BATTERY_PERCENT_TEXT_STYLE)) {
                        String percentSign = intent.getStringExtra(GravityBoxSettings.EXTRA_BATTERY_PERCENT_TEXT_STYLE);
                        mPercentText.setPercentSign(percentSign);
                        if (DEBUG) log("PercentText sign changed to: " + percentSign);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_BATTERY_PERCENT_TEXT_CHARGING)) {
                    int chargingStyle = intent.getIntExtra(GravityBoxSettings.EXTRA_BATTERY_PERCENT_TEXT_CHARGING,
                            StatusbarBatteryPercentage.CHARGING_STYLE_NONE);
                    mPercentText.setChargingStyle(chargingStyle);
                    if (DEBUG) log("PercentText charging style changed to: " + chargingStyle);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_BATTERY_PERCENT_TEXT_CHARGING_COLOR)) {
                    int chargingColor = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_BATTERY_PERCENT_TEXT_CHARGING_COLOR, Color.GREEN);
                    mPercentText.setChargingColor(chargingColor);
                    if (DEBUG) log("PercentText charging color changed to: " + chargingColor);
                }
            } else if (intent.getAction().equals(ACTION_MTK_BATTERY_PERCENTAGE_SWITCH)) {
                mMtkPercentTextEnabled = intent.getIntExtra(EXTRA_MTK_BATTERY_PERCENTAGE_STATE, 0) == 1;
                if (DEBUG) log("mMtkPercentText changed to: " + mMtkPercentTextEnabled);
                updateBatteryStyle();
            }
        }
    };

    public static void initResources(final XSharedPreferences prefs, InitPackageResourcesParam resparam) {
        try {
            // Before anything else, let's make sure we're not dealing with a Lenovo device
            // Lenovo is known for doing some deep customizations into UI, so let's just check
            // if is possible to hook a specific layout and work with it in that case
            String layout = "lenovo_gemini_super_status_bar";
            try{
                resparam.res.hookLayout(PACKAGE_NAME, "layout", layout, new XC_LayoutInflated() {

                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        if (DEBUG) log("Lenovo custom layout found");
                    }
                });
            } catch (Throwable t) {
                // Specific layout not found, so let's work with standard layout 
                layout = Utils.hasGeminiSupport() ? "gemini_super_status_bar" : "super_status_bar";
            } 
            final String[] batteryPercentTextIds = new String[] { "percentage", "battery_text" };

            resparam.res.hookLayout(PACKAGE_NAME, "layout", layout, new XC_LayoutInflated() {

                @Override
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {

                    ViewGroup vg = (ViewGroup) liparam.view.findViewById(
                            liparam.res.getIdentifier("signal_battery_cluster", "id", PACKAGE_NAME));

                    // inject percent text if it doesn't exist
                    for (String bptId : batteryPercentTextIds) {
                        final int bptResId = liparam.res.getIdentifier(
                                bptId, "id", PACKAGE_NAME);
                        if (bptResId != 0) {
                            View v = vg.findViewById(bptResId);
                            if (v != null && v instanceof TextView) {
                                mPercentText = new StatusbarBatteryPercentage((TextView) v);
                                mPercentText.getView().setTag("percentage");
                                if (DEBUG) log("Battery percent text found as: " + bptId);
                                break;
                            }
                        }
                    }
                    if (mPercentText == null) {
                        TextView percentTextView = new TextView(vg.getContext());
                        percentTextView.setTag("percentage");
                        LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                        percentTextView.setLayoutParams(lParams);
                        percentTextView.setPadding(6, 0, 0, 0);
                        percentTextView.setTextSize(1, 16);
                        percentTextView.setTextColor(Color.WHITE);
                        percentTextView.setVisibility(View.GONE);
                        mPercentText = new StatusbarBatteryPercentage(percentTextView);
                        vg.addView(mPercentText.getView(), vg.getChildCount()-1);
                        if (DEBUG) log("Battery percent text injected");
                    }
                    mPercentText.setTextSize(Integer.valueOf(prefs.getString(
                            GravityBoxSettings.PREF_KEY_BATTERY_PERCENT_TEXT_SIZE, "16")));
                    mPercentText.setPercentSign(prefs.getString(
                            GravityBoxSettings.PREF_KEY_BATTERY_PERCENT_TEXT_STYLE, "%"));
                    mPercentText.setChargingStyle(Integer.valueOf(prefs.getString(
                            GravityBoxSettings.PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING, "0")));
                    mPercentText.setChargingColor(prefs.getInt(
                            GravityBoxSettings.PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING_COLOR, Color.GREEN));
                    ModStatusbarColor.registerIconManagerListener(mPercentText);

                    // inject circle battery view
                    mCircleBattery = new CmCircleBattery(vg.getContext());
                    mCircleBattery.setTag("circle_battery");
                    LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                    lParams.gravity = Gravity.CENTER_VERTICAL;
                    mCircleBattery.setLayoutParams(lParams);
                    mCircleBattery.setPadding(6, 0, 0, 0);
                    mCircleBattery.setVisibility(View.GONE);
                    ModStatusbarColor.registerIconManagerListener(mCircleBattery);
                    vg.addView(mCircleBattery);
                    if (DEBUG) log("CmCircleBattery injected");

                    // inject KitKat battery view
                    mKitKatBattery = new KitKatBattery(vg.getContext());
                    mKitKatBattery.setTag("kitkat_battery");
                    final float density = liparam.res.getDisplayMetrics().density;
                    lParams = new LinearLayout.LayoutParams((int)(density * 10.5f), 
                            (int)(density * 16));
                    lParams.setMarginStart((int)(density * 4));
                    lParams.bottomMargin = Math.round(density * 0.33f);
                    mKitKatBattery.setLayoutParams(lParams);
                    mKitKatBattery.setVisibility(View.GONE);
                    ModStatusbarColor.registerIconManagerListener(mKitKatBattery);
                    vg.addView(mKitKatBattery);

                    // find battery
                    mStockBattery = vg.findViewById(
                            liparam.res.getIdentifier("battery", "id", PACKAGE_NAME));
                    if (mStockBattery != null) {
                        mStockBattery.setTag("stock_battery");
                        StatusbarBattery sbb = new StatusbarBattery(mStockBattery);
                        ModStatusbarColor.registerIconManagerListener(sbb);
                    }
                }
                
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static void init(final XSharedPreferences prefs, ClassLoader classLoader) {
        if (DEBUG) log("init");

        try {
            Class<?> batteryControllerClass = XposedHelpers.findClass(CLASS_BATTERY_CONTROLLER, classLoader);

            XposedBridge.hookAllConstructors(batteryControllerClass, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    mBatteryStyle = Integer.valueOf(prefs.getString(
                            GravityBoxSettings.PREF_KEY_BATTERY_STYLE, "1"));
                    mBatteryPercentTextEnabled = prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_BATTERY_PERCENT_TEXT, false);

                    Context context = (Context) param.args[0];
                    mMtkPercentTextEnabled = Utils.isMtkDevice() ?
                            Settings.Secure.getInt(context.getContentResolver(), 
                                    SETTING_MTK_BATTERY_PERCENTAGE, 0) == 1 : false;

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_BATTERY_STYLE_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_BATTERY_PERCENT_TEXT_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_BATTERY_PERCENT_TEXT_SIZE_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_BATTERY_PERCENT_TEXT_STYLE_CHANGED);
                    if (Utils.isMtkDevice()) {
                        intentFilter.addAction(ACTION_MTK_BATTERY_PERCENTAGE_SWITCH);
                    }
                    context.registerReceiver(mBroadcastReceiver, intentFilter);

                    updateBatteryStyle();
                    if (DEBUG) log("BatteryController constructed");
                }
            });
        }
        catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void updateBatteryStyle() {
        try {
            if (mStockBattery != null) {
                mStockBattery.setVisibility((mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_STOCK) ?
                             View.VISIBLE : View.GONE);
            }

            if (mCircleBattery != null) {
                mCircleBattery.setVisibility((mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_CIRCLE ||
                        mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_CIRCLE_PERCENT ||
                        mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_CIRCLE_DASHED ||
                        mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_CIRCLE_DASHED_PERCENT) ?
                                View.VISIBLE : View.GONE);
                mCircleBattery.setPercentage(
                        mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_CIRCLE_PERCENT ||
                        mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_CIRCLE_DASHED_PERCENT);
                mCircleBattery.setStyle(
                        mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_CIRCLE_DASHED ||
                        mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_CIRCLE_DASHED_PERCENT ?
                                CmCircleBattery.Style.DASHED : CmCircleBattery.Style.SOLID);
            }

            if (mKitKatBattery != null) {
                mKitKatBattery.setVisibility((mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_KITKAT ||
                        mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_KITKAT_PERCENT) ?
                                View.VISIBLE : View.GONE);
                mKitKatBattery.setShowPercent(
                        mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_KITKAT_PERCENT);
            }

            if (mPercentText != null) {
                mPercentText.setVisibility(
                        (mBatteryPercentTextEnabled || mMtkPercentTextEnabled) ?
                                View.VISIBLE : View.GONE);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
