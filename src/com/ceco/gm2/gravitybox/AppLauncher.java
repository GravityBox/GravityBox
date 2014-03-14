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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import com.ceco.gm2.gravitybox.preference.AppPickerPreference;
import com.ceco.gm2.gravitybox.shortcuts.ShortcutActivity;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class AppLauncher {
    private static final String TAG = "GB:AppLauncher";
    private static final boolean DEBUG = false;

    private Context mContext;
    private Context mGbContext;
    private Resources mResources;
    private Resources mGbResources;
    private Dialog mDialog;
    private Handler mHandler;
    private PackageManager mPm;
    private List<AppInfo> mAppSlots;
    private View mAppView;
    private XSharedPreferences mPrefs;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private Runnable mDismissAppDialogRunnable = new Runnable() {
        @Override
        public void run() {
            dismissDialog();
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("Broadcast received: " + intent.toString());

            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_APP_LAUNCHER_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_APP_LAUNCHER_SLOT) &&
                        intent.hasExtra(GravityBoxSettings.EXTRA_APP_LAUNCHER_APP)) {
                    int slot = intent.getIntExtra(GravityBoxSettings.EXTRA_APP_LAUNCHER_SLOT, -1);
                    String app = intent.getStringExtra(GravityBoxSettings.EXTRA_APP_LAUNCHER_APP);
                    if (DEBUG) log("appSlot=" + slot + "; app=" + app);
                    updateAppSlot(slot, app);
                }
            } else if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                for (int i = 0; i < GravityBoxSettings.PREF_KEY_APP_LAUNCHER_SLOT.size(); i++) {
                    updateAppSlot(i, mPrefs.getString(
                            GravityBoxSettings.PREF_KEY_APP_LAUNCHER_SLOT.get(i), null));
                }
            }
        }
    };

    public AppLauncher(Context context, XSharedPreferences prefs) {
        mContext = context;
        mResources = mContext.getResources();
        mPrefs = prefs;
        try {
            mGbContext = mContext.createPackageContext(
                    GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            mGbResources = mGbContext.getResources();
        } catch (NameNotFoundException e) {
            log("Error creating GB context: " + e.getMessage());
        }
        mHandler = new Handler();
        mPm = mContext.getPackageManager();

        mAppSlots = new ArrayList<AppInfo>();
        mAppSlots.add(new AppInfo(R.id.quickapp1));
        mAppSlots.add(new AppInfo(R.id.quickapp2));
        mAppSlots.add(new AppInfo(R.id.quickapp3));
        mAppSlots.add(new AppInfo(R.id.quickapp4));
        mAppSlots.add(new AppInfo(R.id.quickapp5));
        mAppSlots.add(new AppInfo(R.id.quickapp6));
        mAppSlots.add(new AppInfo(R.id.quickapp7));
        mAppSlots.add(new AppInfo(R.id.quickapp8));

        for (int i = 0; i < GravityBoxSettings.PREF_KEY_APP_LAUNCHER_SLOT.size(); i++) {
            updateAppSlot(i, mPrefs.getString(
                    GravityBoxSettings.PREF_KEY_APP_LAUNCHER_SLOT.get(i), null));
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GravityBoxSettings.ACTION_PREF_APP_LAUNCHER_CHANGED);
        intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    public boolean dismissDialog() {
        boolean dismissed = false;
        mHandler.removeCallbacks(mDismissAppDialogRunnable);
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            dismissed = true;
        }
        return dismissed;
    }

    public void showDialog() {
        try {
            if (dismissDialog()) {
                return;
            }

            if (mDialog == null) {
                LayoutInflater inflater = LayoutInflater.from(mGbContext);
                mAppView = inflater.inflate(R.layout.navbar_app_dialog, null);
                mDialog = new Dialog(mContext);
                mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                mDialog.setContentView(mAppView);
                mDialog.setCanceledOnTouchOutside(true);
                mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);
                int pf = XposedHelpers.getIntField(mDialog.getWindow().getAttributes(), "privateFlags");
                pf |= 0x00000010;
                XposedHelpers.setIntField(mDialog.getWindow().getAttributes(), "privateFlags", pf);
                mDialog.getWindow().getAttributes().gravity = Gravity.BOTTOM;
            }

            View appRow1 = mAppView.findViewById(R.id.appRow1);
            View appRow2 = mAppView.findViewById(R.id.appRow2);
            View separator = mAppView.findViewById(R.id.separator);
            int appCount = 0;
            boolean appRow1Visible = false;
            boolean appRow2Visible = false;
            TextView lastVisible = null;
            for (AppInfo ai : mAppSlots) {
                TextView tv = (TextView) mAppView.findViewById(ai.getResId());
                if (ai.getValue() == null) {
                    tv.setVisibility(View.GONE);
                    continue;
                }

                tv.setText(ai.getAppName());
                tv.setCompoundDrawablesWithIntrinsicBounds(null, ai.getAppIcon(), null, null);
                tv.setOnClickListener(mAppOnClick);
                tv.setVisibility(View.VISIBLE);
                lastVisible = tv;

                appRow1Visible |= ai.getResId() == R.id.quickapp1 || ai.getResId() == R.id.quickapp2 || 
                        ai.getResId() == R.id.quickapp3 || ai.getResId() == R.id.quickapp4;
                appRow2Visible |= ai.getResId() == R.id.quickapp5 || ai.getResId() == R.id.quickapp6 || 
                        ai.getResId() == R.id.quickapp7 || ai.getResId() == R.id.quickapp8;

                appCount++;
            }

            if (appCount == 0) {
                Toast.makeText(mContext, mGbContext.getString(R.string.app_launcher_no_apps),
                        Toast.LENGTH_LONG).show();
            } else if (appCount == 1) {
                mAppOnClick.onClick(lastVisible);
            } else {
                appRow1.setVisibility(appRow1Visible ? View.VISIBLE : View.GONE);
                appRow2.setVisibility(appRow2Visible ? View.VISIBLE : View.GONE);
                separator.setVisibility(appRow1Visible && appRow2Visible ?
                        View.VISIBLE : View.GONE);
                mDialog.show();
                mHandler.postDelayed(mDismissAppDialogRunnable, 4000);
            }
        } catch (Throwable t) {
            log("Error opening app launcher dialog: " + t.getMessage());
        }
    }

    private View.OnClickListener mAppOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dismissDialog();

            AppInfo aiProcessing = null;
            try {
                for(AppInfo ai : mAppSlots) {
                    aiProcessing = ai;
                    if (v.getId() == ai.getResId()) {
                        startActivity(v.getContext(), ai.getIntent());
                        return;
                    }
                }
                aiProcessing = null;
            } catch (Exception e) {
                log("Unable to start activity: " + e.getMessage());
                if (aiProcessing != null) {
                    aiProcessing.initAppInfo(null);
                }
            }
        }
    };

    private void startActivity(Context context, Intent intent) {
        // if intent is a GB action of broadcast type, handle it directly here
        if (ShortcutActivity.isGbBroadcastShortcut(intent)) {
            Intent newIntent = new Intent(intent.getStringExtra(ShortcutActivity.EXTRA_ACTION));
            newIntent.putExtras(intent);
            context.sendBroadcast(newIntent);
        // otherwise start activity
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        }
    }

    private void updateAppSlot(int slot, String value) {
        AppInfo ai = mAppSlots.get(slot);
        if (ai.getValue() == null || !ai.getValue().equals(value)) {
            ai.initAppInfo(value);
        }
    }

    private final class AppInfo {
        private String mAppName;
        private Drawable mAppIcon;
        private String mValue;
        private int mResId;
        private Intent mIntent;

        public AppInfo(int resId) {
            mResId = resId;
        }

        public int getResId() {
            return mResId;
        }

        public String getAppName() {
            return mAppName;
        }

        public Drawable getAppIcon() {
            return (mAppIcon == null ? 
                    mResources.getDrawable(android.R.drawable.ic_menu_help) : mAppIcon);
        }

        public String getValue() {
            return mValue;
        }

        public Intent getIntent() {
            return mIntent;
        }

        private void reset() {
            mValue = mAppName = null;
            mAppIcon = null;
            mIntent = null;
        }

        public void initAppInfo(String value) {
            mValue = value;
            if (mValue == null) {
                reset();
                return;
            }

            try {
                mIntent = Intent.parseUri(value, 0);
                if (!mIntent.hasExtra("mode")) {
                    reset();
                    return;
                }
                final int mode = mIntent.getIntExtra("mode", AppPickerPreference.MODE_APP);
                final int iconResId = mIntent.getStringExtra("iconResName") != null ?
                        mGbResources.getIdentifier(mIntent.getStringExtra("iconResName"),
                        "drawable", mGbContext.getPackageName()) : 0;
                Bitmap appIcon = null;
                if (mode == AppPickerPreference.MODE_APP) {
                    ActivityInfo ai = mPm.getActivityInfo(mIntent.getComponent(), 0);
                    mAppName = ai.loadLabel(mPm).toString();
                    if (iconResId != 0) {
                        appIcon = Utils.drawableToBitmap(mGbResources.getDrawable(iconResId));
                    } else {
                        appIcon = Utils.drawableToBitmap(ai.loadIcon(mPm));
                    }
                } else if (mode == AppPickerPreference.MODE_SHORTCUT) {
                    mAppName = mIntent.getStringExtra("label");
                    if (iconResId != 0) {
                        appIcon = Utils.drawableToBitmap(mGbResources.getDrawable(iconResId));
                    } else {
                        final String appIconPath = mIntent.getStringExtra("icon");
                        if (appIconPath != null) {
                            File f = new File(appIconPath);
                            FileInputStream fis = new FileInputStream(f);
                            appIcon = BitmapFactory.decodeStream(fis);
                            fis.close();
                        }
                    }
                }
                if (appIcon != null) {
                    int sizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, 
                            mResources.getDisplayMetrics());
                    Bitmap scaledIcon = Bitmap.createScaledBitmap(appIcon, sizePx, sizePx, true);
                    mAppIcon = new BitmapDrawable(mResources, scaledIcon);
                }
                if (DEBUG) log("AppInfo initialized for: " + getAppName());
            } catch (NameNotFoundException e) {
                log("App not found: " + mIntent);
                reset();
            } catch (Exception e) {
                log("Unexpected error: " + e.getMessage());
                reset();
            }
        }
    }
}
