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

package com.ceco.gm2.gravitybox.quicksettings;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import com.ceco.gm2.gravitybox.GravityBoxSettings;
import com.ceco.gm2.gravitybox.R;
import com.ceco.gm2.gravitybox.Utils;
import com.ceco.gm2.gravitybox.preference.AppPickerPreference;
import com.ceco.gm2.gravitybox.shortcuts.ShortcutActivity;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils.TruncateAt;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

public class QuickAppTile extends BasicTile {
    private static final String TAG = "GB:QuickAppTile";
    private static final boolean DEBUG = false;

    private static int[] TILEVIEW_ID = new int[] { R.id.quickapp_tileview, R.id.quickapp_tileview_2 };
    private static int[] LAYOUT_ID = new int[] { 
        R.layout.quick_settings_tile_quick_app, R.layout.quick_settings_tile_quick_app_2 };

    private String KEY_QUICKAPP_DEFAULT = GravityBoxSettings.PREF_KEY_QUICKAPP_DEFAULT;
    private String KEY_QUICKAPP_SLOT1 = GravityBoxSettings.PREF_KEY_QUICKAPP_SLOT1;
    private String KEY_QUICKAPP_SLOT2 = GravityBoxSettings.PREF_KEY_QUICKAPP_SLOT2;
    private String KEY_QUICKAPP_SLOT3 = GravityBoxSettings.PREF_KEY_QUICKAPP_SLOT3;
    private String KEY_QUICKAPP_SLOT4 = GravityBoxSettings.PREF_KEY_QUICKAPP_SLOT4;
    private String ACTION_PREF_QUICKAPP_CHANGED = GravityBoxSettings.ACTION_PREF_QUICKAPP_CHANGED;

    private AppInfo mMainApp;
    private List<AppInfo> mAppSlots;
    private PackageManager mPm;
    private Dialog mDialog;
    private Handler mHandler;
    private int mId = 1;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private final class AppInfo {
        private String mAppName;
        private Drawable mAppIcon;
        private Drawable mAppIconSmall;
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
            return (mAppName == null ? 
                    mGbContext.getString(R.string.qs_tile_quickapp) : mAppName);
        }

        public Drawable getAppIcon() {
            return (mAppIcon == null ? 
                    mResources.getDrawable(android.R.drawable.ic_menu_help) : mAppIcon);
        }

        public Drawable getAppIconSmall() {
            return (mAppIconSmall == null ? 
                    mResources.getDrawable(android.R.drawable.ic_menu_help) : mAppIconSmall);
        }

        public String getValue() {
            return mValue;
        }

        public Intent getIntent() {
            return mIntent;
        }

        private void reset() {
            mValue = mAppName = null;
            mAppIcon = mAppIconSmall = null;
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
                    int sizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, 
                            mResources.getDisplayMetrics());
                    int sizePxSmall = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 35, 
                            mResources.getDisplayMetrics());
                    Bitmap scaledIcon;
                    scaledIcon = Bitmap.createScaledBitmap(appIcon, sizePx, sizePx, true);
                    mAppIcon = new BitmapDrawable(mResources, scaledIcon);
                    scaledIcon = Bitmap.createScaledBitmap(appIcon, sizePxSmall, sizePxSmall, true);
                    mAppIconSmall = new BitmapDrawable(mResources, scaledIcon);
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

    private Runnable mDismissDialogRunnable = new Runnable() {

        @Override
        public void run() {
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
                mDialog = null;
            }
        }
    };

    public QuickAppTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        this(context, gbContext, statusBar, panelBar, 1);
    }

    public QuickAppTile(Context context, Context gbContext, Object statusBar, Object panelBar, int id) {
        super(context, gbContext, statusBar, panelBar);

        mHandler = new Handler();

        mId = id;
        if (mId > 1) {
            KEY_QUICKAPP_DEFAULT += "_" + mId;
            KEY_QUICKAPP_SLOT1 += "_" + mId;
            KEY_QUICKAPP_SLOT2 += "_" + mId;
            KEY_QUICKAPP_SLOT3 += "_" + mId;
            KEY_QUICKAPP_SLOT4 += "_" + mId;
            ACTION_PREF_QUICKAPP_CHANGED += "_" + mId;
        }

        mPm = context.getPackageManager();
        mMainApp = new AppInfo(TILEVIEW_ID[mId-1]);
        mAppSlots = new ArrayList<AppInfo>();
        mAppSlots.add(new AppInfo(R.id.quickapp1));
        mAppSlots.add(new AppInfo(R.id.quickapp2));
        mAppSlots.add(new AppInfo(R.id.quickapp3));
        mAppSlots.add(new AppInfo(R.id.quickapp4));

        mOnClick = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacks(mDismissDialogRunnable);
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.dismiss();
                    mDialog = null;
                }

                AppInfo aiProcessing = null;
                try {
                    for(AppInfo ai : mAppSlots) {
                        aiProcessing = ai;
                        if (v.getId() == ai.getResId()) {
                            startActivity(ai.getIntent());
                            return;
                        }
                    }

                    aiProcessing = null;
                    startActivity(mMainApp.getIntent());
                } catch (Exception e) {
                    log("Unable to start activity: " + e.getMessage());
                    if (aiProcessing != null) {
                        aiProcessing.initAppInfo(null);
                    }
                }
            }
        };

        mOnLongClick = new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                LayoutInflater inflater = LayoutInflater.from(mGbContext);
                View appv = inflater.inflate(R.layout.quick_settings_app_dialog, null);
                boolean atLeastOne = false;
                for (AppInfo ai : mAppSlots) {
                    TextView tv = (TextView) appv.findViewById(ai.getResId());
                    if (ai.getValue() == null) {
                        tv.setVisibility(View.GONE);
                        continue;
                    }

                    tv.setText(ai.getAppName());
                    tv.setTextSize(1, 10);
                    tv.setMaxLines(2);
                    tv.setEllipsize(TruncateAt.END);
                    tv.setCompoundDrawablesWithIntrinsicBounds(null, ai.getAppIcon(), null, null);
                    tv.setClickable(true);
                    tv.setOnClickListener(mOnClick);
                    atLeastOne = true;
                }
                if (!atLeastOne) return true;

                mDialog = new Dialog(mContext);
                mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                mDialog.setContentView(appv);
                mDialog.setCanceledOnTouchOutside(true);
                mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);
                int pf = XposedHelpers.getIntField(mDialog.getWindow().getAttributes(), "privateFlags");
                pf |= 0x00000010;
                XposedHelpers.setIntField(mDialog.getWindow().getAttributes(), "privateFlags", pf);
                mDialog.getWindow().clearFlags(LayoutParams.FLAG_DIM_BEHIND);
                mDialog.show();
                mHandler.removeCallbacks(mDismissDialogRunnable);
                mHandler.postDelayed(mDismissDialogRunnable, 4000);
                return true;
            }
        };
    }

    @Override
    protected int onGetLayoutId() {
        return LAYOUT_ID[mId-1];
    }

    @Override
    protected synchronized void updateTile() {
        mLabel = mMainApp.getAppName();
        mDrawable = mMainApp.getAppIconSmall();

        mTextView.setText(mLabel);
        mTextView.setCompoundDrawablesWithIntrinsicBounds(null, mDrawable, null, null);
    }

    @Override
    protected void startActivity(Intent intent) {
        // if intent is a GB action of broadcast type, handle it directly here
        if (ShortcutActivity.isGbBroadcastShortcut(intent)) {
            Intent newIntent = new Intent(intent.getStringExtra(ShortcutActivity.EXTRA_ACTION));
            newIntent.putExtras(intent);
            mContext.sendBroadcast(newIntent);
        // otherwise let super class handle it
        } else {
            super.startActivity(intent);
        }
    }

    @Override
    protected void onPreferenceInitialize(XSharedPreferences prefs) {
        updateMainApp(prefs.getString(KEY_QUICKAPP_DEFAULT, null));
        updateSubApp(0, prefs.getString(KEY_QUICKAPP_SLOT1, null));
        updateSubApp(1, prefs.getString(KEY_QUICKAPP_SLOT2, null));
        updateSubApp(2, prefs.getString(KEY_QUICKAPP_SLOT3, null));
        updateSubApp(3, prefs.getString(KEY_QUICKAPP_SLOT4, null));
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        super.onBroadcastReceived(context, intent);
        if (DEBUG) log("onBroadcastReceived: " + intent.toString());

        if (intent.getAction().equals(ACTION_PREF_QUICKAPP_CHANGED)) {
            if (intent.hasExtra(GravityBoxSettings.EXTRA_QUICKAPP_DEFAULT)) {
                updateMainApp(intent.getStringExtra(GravityBoxSettings.EXTRA_QUICKAPP_DEFAULT));
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_QUICKAPP_SLOT1)) {
                updateSubApp(0, intent.getStringExtra(GravityBoxSettings.EXTRA_QUICKAPP_SLOT1));
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_QUICKAPP_SLOT2)) {
                updateSubApp(1, intent.getStringExtra(GravityBoxSettings.EXTRA_QUICKAPP_SLOT2));
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_QUICKAPP_SLOT3)) {
                updateSubApp(2, intent.getStringExtra(GravityBoxSettings.EXTRA_QUICKAPP_SLOT3));
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_QUICKAPP_SLOT4)) {
                updateSubApp(3, intent.getStringExtra(GravityBoxSettings.EXTRA_QUICKAPP_SLOT4));
            }

            updateResources();
        }
    }

    private void updateMainApp(String value) {
        if (mMainApp.getValue() == null || !mMainApp.getValue().equals(value)) {
            mMainApp.initAppInfo(value);
        }
    }

    private void updateSubApp(int slot, String value) {
        AppInfo ai;
        ai = mAppSlots.get(slot);
        if (ai.getValue() == null || !ai.getValue().equals(value)) {
            ai.initAppInfo(value);
        }
    }
}
