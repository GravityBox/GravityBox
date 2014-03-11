/*
 * Copyright (C) 2013 OmniROM Project
 * Copyright (C) 2014 Peter Gregus for GravityBox Project (C3C076@xda)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.kitkat.gravitybox;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import com.ceco.kitkat.gravitybox.StatusBarIconManager.ColorInfo;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

public class TrafficMeterOmni extends TrafficMeterAbstract {
    private static final int KILOBIT = 1000;
    private static final int KILOBYTE = 1024;

    private static DecimalFormat decimalFormat = new DecimalFormat("##0.#");
    private static final Map<String,String> SYMBOLS;
    static {
        decimalFormat.setMaximumIntegerDigits(3);
        decimalFormat.setMaximumFractionDigits(1);

        SYMBOLS = new HashMap<String,String>();
        SYMBOLS.put("b/s", "b/s");
        SYMBOLS.put("B/s", "B/s");
        SYMBOLS.put("k", "k");
        SYMBOLS.put("M", "M");
        SYMBOLS.put("G", "G");
    }

    private enum Mode { IN, OUT, IN_OUT };

    private long totalRxBytes;
    private long totalTxBytes;
    private long lastUpdateTime;
    private int txtSizeSingle;
    private int txtSizeMulti;
    private int KB = KILOBYTE;
    private int MB = KB * KB;
    private int GB = MB * KB;
    private Mode mMode;
    private Integer mIconColor;
    private boolean mShowIcon;

    private Handler mTrafficHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long timeDelta = SystemClock.elapsedRealtime() - lastUpdateTime;

            if (timeDelta < mInterval * .95) {
                if (msg.what != 1) {
                    // we just updated the view, nothing further to do
                    return;
                }
                if (timeDelta < 1) {
                    // Can't div by 0 so make sure the value displayed is minimal
                    timeDelta = Long.MAX_VALUE;
                }
            }
            lastUpdateTime = SystemClock.elapsedRealtime();

            // Calculate the data rate from the change in total bytes and time
            long newTotalRxBytes = TrafficStats.getTotalRxBytes();
            long newTotalTxBytes = TrafficStats.getTotalTxBytes();
            long rxData = newTotalRxBytes - totalRxBytes;
            long txData = newTotalTxBytes - totalTxBytes;

            // If bit/s convert from Bytes to bits
            String symbol;
            if (KB == KILOBYTE) {
                symbol = SYMBOLS.get("B/s");
            } else {
                symbol = SYMBOLS.get("b/s");
                rxData = rxData * 8;
                txData = txData * 8;
            }

            // Get information for uplink ready so the line return can be added
            String output = "";
            if (mMode == Mode.OUT || mMode == Mode.IN_OUT) {
                output = formatOutput(timeDelta, txData, symbol);
            }

            // Ensure text size is where it needs to be
            int textSize;
            if (mMode == Mode.IN_OUT) {
                output += "\n";
                textSize = txtSizeMulti;
            } else {
                textSize = txtSizeSingle;
            }

            // Add information for downlink if it's called for
            if (mMode == Mode.IN || mMode == Mode.IN_OUT) {
                output += formatOutput(timeDelta, rxData, symbol);
            }

            // Update view if there's anything new to show
            if (!output.contentEquals(getText()) || msg.what == 1) {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)textSize);
                setText(output);
            }

            // Post delayed message to refresh in ~1000ms
            totalRxBytes = newTotalRxBytes;
            totalTxBytes = newTotalTxBytes;
            clearHandlerCallbacks();
            mTrafficHandler.postDelayed(mRunnable, mInterval);
        }

        private String formatOutput(long timeDelta, long data, String symbol) {
            long speed = (long)(data / (timeDelta / 1000F));
            if (speed < KB) {
                return decimalFormat.format(speed) + symbol;
            } else if (speed < MB) {
                return decimalFormat.format(speed / (float)KB) + SYMBOLS.get("k") + symbol;
            } else if (speed < GB) {
                return decimalFormat.format(speed / (float)MB) + SYMBOLS.get("M") + symbol;
            }
            return decimalFormat.format(speed / (float)GB) + SYMBOLS.get("G") + symbol;
        }
    };

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mTrafficHandler.sendEmptyMessage(0);
        }
    };

    public TrafficMeterOmni(Context context) {
        super(context);
    }

    @Override
    protected void onInitialize(XSharedPreferences prefs) {
        try {
            mGbContext = getContext().createPackageContext(GravityBox.PACKAGE_NAME, 
                    Context.CONTEXT_IGNORE_SECURITY);
            SYMBOLS.put("b/s", mGbContext.getString(R.string.bit_per_sec_abbr));
            SYMBOLS.put("B/s", mGbContext.getString(R.string.byte_per_sec_abbr));
            SYMBOLS.put("k", mGbContext.getString(R.string.kilo_abbr));
            SYMBOLS.put("M", mGbContext.getString(R.string.mega_abbr));
            SYMBOLS.put("G", mGbContext.getString(R.string.giga_abbr));
        } catch (NameNotFoundException e) {
            XposedBridge.log(e);
        }

        mMode = Mode.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_DATA_TRAFFIC_OMNI_MODE, "IN_OUT"));
        mShowIcon = prefs.getBoolean(GravityBoxSettings.PREF_KEY_DATA_TRAFFIC_OMNI_SHOW_ICON, true);
        setSize();
    }

    @Override
    protected void onPreferenceChanged(Intent intent) {
        if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_OMNI_MODE)) {
            mMode = Mode.valueOf(intent.getStringExtra(GravityBoxSettings.EXTRA_DT_OMNI_MODE));
        }
        if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_OMNI_SHOW_ICON)) {
            mShowIcon = intent.getBooleanExtra(GravityBoxSettings.EXTRA_DT_OMNI_SHOW_ICON, true);
        }
        if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_SIZE)) {
            setSize();
        }
    }

    private void setSize() {
        final Resources resources = getResources();
        txtSizeSingle = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mSize,
                resources.getDisplayMetrics()));
        txtSizeMulti = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mSize-4,
                resources.getDisplayMetrics()));
    }

    @Override
    protected void startTrafficUpdates() {
        totalRxBytes = TrafficStats.getTotalRxBytes();
        lastUpdateTime = SystemClock.elapsedRealtime();
        mTrafficHandler.sendEmptyMessage(1);
        updateTrafficDrawable();
    }

    @Override
    protected void stopTrafficUpdates() {
        clearHandlerCallbacks();
    }

    private void clearHandlerCallbacks() {
        mTrafficHandler.removeCallbacks(mRunnable);
        mTrafficHandler.removeMessages(0);
        mTrafficHandler.removeMessages(1);
    }

    private void updateTrafficDrawable() {
        if (mGbContext == null) return;

        final Resources res = mGbContext.getResources();
        Drawable d = null;
        if (mShowIcon) {
            if (mMode == Mode.IN_OUT) {
                d = res.getDrawable(R.drawable.stat_sys_network_traffic_updown).mutate();
            } else if (mMode == Mode.OUT) {
                d = res.getDrawable(R.drawable.stat_sys_network_traffic_up).mutate();
            } else if (mMode == Mode.IN) {
                d = res.getDrawable(R.drawable.stat_sys_network_traffic_down).mutate();
            };
            if (d != null && mIconColor != null) {
                d.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
            }
        }

        setCompoundDrawablesWithIntrinsicBounds(null, null, d, null);

        LinearLayout.LayoutParams lParams = (LinearLayout.LayoutParams) getLayoutParams();
        lParams.setMarginEnd(mShowIcon ? 0 : mMargin);
        setLayoutParams(lParams);
    }

    @Override
    public void onIconManagerStatusChanged(int flags, ColorInfo colorInfo) {
        super.onIconManagerStatusChanged(flags, colorInfo);
        if ((flags & StatusBarIconManager.FLAG_ICON_COLOR_CHANGED) != 0) {
            mIconColor = colorInfo.coloringEnabled ? colorInfo.iconColor[0] : null;
            updateTrafficDrawable();
        }
    }
}
