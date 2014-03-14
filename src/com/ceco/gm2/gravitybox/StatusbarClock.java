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

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import com.ceco.gm2.gravitybox.StatusBarIconManager.ColorInfo;
import com.ceco.gm2.gravitybox.StatusBarIconManager.IconManagerListener;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.TextView;

public class StatusbarClock implements IconManagerListener, BroadcastSubReceiver {
    private static final String TAG = "GB:StatusbarClock";
    private static final boolean DEBUG = false;

    private TextView mClock;
    private TextView mClockExpanded;
    private int mDefaultClockColor;
    private int mOriginalPaddingLeft;
    private boolean mAmPmHide;
    private int mClockShowDow = GravityBoxSettings.DOW_DISABLED;
    private boolean mClockHidden;
    private float mDowSize;
    private float mAmPmSize;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public StatusbarClock(XSharedPreferences prefs) {
        mClockShowDow = Integer.valueOf(
                prefs.getString(GravityBoxSettings.PREF_KEY_STATUSBAR_CLOCK_DOW, "0"));
        mAmPmHide = prefs.getBoolean(GravityBoxSettings.PREF_KEY_STATUSBAR_CLOCK_AMPM_HIDE, false);
        mClockHidden = prefs.getBoolean(GravityBoxSettings.PREF_KEY_STATUSBAR_CLOCK_HIDE, false);
        mDowSize = prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_CLOCK_DOW_SIZE, 70) / 100f;
        mAmPmSize = prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_CLOCK_AMPM_SIZE, 70) / 100f;
    }

    public TextView getClock() {
        return mClock;
    }

    public TextView getExpandedClock() {
        return mClockExpanded;
    }

    public void resetOriginalPaddingLeft() {
        if (mClock != null) {
            mClock.setPadding(mOriginalPaddingLeft, 0, 0, 0);
        }
    }

    public void setClock(TextView clock) {
        if (clock == null) throw new IllegalArgumentException("Clock cannot be null");

        mClock = clock;
        mDefaultClockColor = mClock.getCurrentTextColor();
        mOriginalPaddingLeft = mClock.getPaddingLeft();

        // use this additional field to identify the instance of Clock that resides in status bar
        XposedHelpers.setAdditionalInstanceField(mClock, "sbClock", true);

        // hide explicitly if desired
        if (mClockHidden) {
            mClock.setVisibility(View.GONE);
        }

        hookGetSmallTime();
    }

    public void setExpandedClock(TextView clock) {
        if (clock == null) throw new IllegalArgumentException("Clock cannot be null");

        mClockExpanded = clock;
    }

    private void updateClock() {
        try {
            XposedHelpers.callMethod(mClock, "updateClock");
        } catch (Throwable t) {
            log("Error in updateClock: " + t.getMessage());
        }
    }

    private void updateExpandedClock() {
        try {
            XposedHelpers.callMethod(mClockExpanded, "updateClock");
        } catch (Throwable t) {
            log("Error in updateExpandedClock: " + t.getMessage());
        }
    }

    public void setClockVisibility(boolean show) {
        if (mClock != null) {
            mClock.setVisibility(show && !mClockHidden ? View.VISIBLE : View.GONE);
        }
    }

    public void setClockVisibility() {
        setClockVisibility(true);
    }

    private void hookGetSmallTime() {
        try {
            XposedHelpers.findAndHookMethod(mClock.getClass(), "getSmallTime", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // is this a status bar Clock instance?
                    // yes, if it contains our additional sbClock field
                    if (DEBUG) log("getSmallTime() called. mAmPmHide=" + mAmPmHide);
                    Object sbClock = XposedHelpers.getAdditionalInstanceField(param.thisObject, "sbClock");
                    if (DEBUG) log("Is statusbar clock: " + (sbClock == null ? "false" : "true"));
                    Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                    String clockText = param.getResult().toString();
                    if (DEBUG) log("Original clockText: '" + clockText + "'");
                    String amPm = calendar.getDisplayName(
                            Calendar.AM_PM, Calendar.SHORT, Locale.getDefault());
                    if (DEBUG) log("Locale specific AM/PM string: '" + amPm + "'");
                    int amPmIndex = clockText.indexOf(amPm);
                    if (DEBUG) log("Original AM/PM index: " + amPmIndex);
                    if (mAmPmHide && amPmIndex != -1) {
                        clockText = clockText.replace(amPm, "").trim();
                        if (DEBUG) log("AM/PM removed. New clockText: '" + clockText + "'");
                        amPmIndex = -1;
                    } else if (!mAmPmHide 
                                && !DateFormat.is24HourFormat(mClock.getContext()) 
                                && amPmIndex == -1) {
                        // insert AM/PM if missing
                        clockText += " " + amPm;
                        amPmIndex = clockText.indexOf(amPm);
                        if (DEBUG) log("AM/PM added. New clockText: '" + clockText + "'; New AM/PM index: " + amPmIndex);
                    }
                    CharSequence dow = "";
                    // apply day of week only to statusbar clock, not the notification panel clock
                    if (mClockShowDow != GravityBoxSettings.DOW_DISABLED && sbClock != null) {
                        dow = getFormattedDow(calendar.getDisplayName(
                                Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())) + " ";
                    }
                    clockText = dow + clockText;
                    SpannableStringBuilder sb = new SpannableStringBuilder(clockText);
                    sb.setSpan(new RelativeSizeSpan(mDowSize), 0, dow.length(), 
                            Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    if (amPmIndex > -1) {
                        int offset = Character.isWhitespace(clockText.charAt(dow.length() + amPmIndex - 1)) ?
                                1 : 0;
                        sb.setSpan(new RelativeSizeSpan(mAmPmSize), dow.length() + amPmIndex - offset, 
                                dow.length() + amPmIndex + amPm.length(), 
                                Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    }
                    if (DEBUG) log("Final clockText: '" + sb + "'");
                    param.setResult(sb);
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private String getFormattedDow(String inDow) {
        switch (mClockShowDow) {
            case GravityBoxSettings.DOW_LOWERCASE: 
                return inDow.toLowerCase(Locale.getDefault());
            case GravityBoxSettings.DOW_UPPERCASE:
                return inDow.toUpperCase(Locale.getDefault());
            case GravityBoxSettings.DOW_STANDARD:
            default: return inDow;
        }
    }

    @Override
    public void onIconManagerStatusChanged(int flags, ColorInfo colorInfo) {
        if (mClock != null && (flags & StatusBarIconManager.FLAG_ICON_COLOR_CHANGED) != 0) {
            if (colorInfo.coloringEnabled) {
                mClock.setTextColor(colorInfo.iconColor[0]);
            } else {
                if (colorInfo.followStockBatteryColor && colorInfo.stockBatteryColor != null) {
                    mClock.setTextColor(colorInfo.stockBatteryColor);
                } else {
                    mClock.setTextColor(mDefaultClockColor);
                }
            }
        }
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_CLOCK_CHANGED)) {
            if (intent.hasExtra(GravityBoxSettings.EXTRA_CLOCK_DOW)) {
                mClockShowDow = intent.getIntExtra(GravityBoxSettings.EXTRA_CLOCK_DOW,
                        GravityBoxSettings.DOW_DISABLED);
                updateClock();
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_AMPM_HIDE)) {
                mAmPmHide = intent.getBooleanExtra(GravityBoxSettings.EXTRA_AMPM_HIDE, false);
                updateClock();
                updateExpandedClock();
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_CLOCK_HIDE)) {
                mClockHidden = intent.getBooleanExtra(GravityBoxSettings.EXTRA_CLOCK_HIDE, false);
                setClockVisibility();
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_CLOCK_DOW_SIZE)) {
                mDowSize = intent.getIntExtra(GravityBoxSettings.EXTRA_CLOCK_DOW_SIZE, 70) / 100f;
                updateClock();
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_AMPM_SIZE)) {
                mAmPmSize = intent.getIntExtra(GravityBoxSettings.EXTRA_AMPM_SIZE, 70) / 100f;
                updateClock();
                updateExpandedClock();
            }
        }
    }
}
