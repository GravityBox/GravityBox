/*
 * Copyright (C) 2013 AOKP Project
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

import java.io.File;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.Surface;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

class NotificationWallpaper extends FrameLayout implements BroadcastSubReceiver {
    private static final String PACKAGE_NAME = "com.android.systemui";
    private static final String TAG = "GB:NotificationWallpaper";

    private FrameLayout mNotificationPanelView;
    private ImageView mNotificationWallpaperImage;
    private String mNotifBgImagePathPortrait;
    private String mNotifBgImagePathLandscape;
    private String mBgType;
    private int mColor;
    private String mColorMode;
    private float mAlpha;
    private Context mContext;
    Bitmap mBitmapWallpaper = null;

    public NotificationWallpaper(FrameLayout container, XSharedPreferences prefs) {
        super(container.getContext());
        mNotificationPanelView = container;
        mContext = mNotificationPanelView.getContext();

        try {
            Context gbContext = mContext.createPackageContext(GravityBox.PACKAGE_NAME, 0);
            mNotifBgImagePathPortrait = gbContext.getFilesDir() + "/notifwallpaper";
            mNotifBgImagePathLandscape = gbContext.getFilesDir() + "/notifwallpaper_landscape";
        } catch (NameNotFoundException e) {
            mNotifBgImagePathPortrait = "";
            mNotifBgImagePathLandscape = "";
            XposedBridge.log(e);
        }

        mBgType = GravityBoxSettings.NOTIF_BG_DEFAULT;
        mColorMode = GravityBoxSettings.NOTIF_BG_COLOR_MODE_OVERLAY;
        mAlpha = 0.6f;

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        setLayoutParams(lp);
        setType(prefs.getString(
                GravityBoxSettings.PREF_KEY_NOTIF_BACKGROUND,
                GravityBoxSettings.NOTIF_BG_DEFAULT));
        setColor(prefs.getInt(
                GravityBoxSettings.PREF_KEY_NOTIF_COLOR, Color.BLACK));
        setColorMode(prefs.getString(
                GravityBoxSettings.PREF_KEY_NOTIF_COLOR_MODE,
                GravityBoxSettings.NOTIF_BG_COLOR_MODE_OVERLAY));
        setAlpha(prefs.getInt(
                GravityBoxSettings.PREF_KEY_NOTIF_BACKGROUND_ALPHA, 0));
        mNotificationPanelView.addView(this);
        updateNotificationPanelBackground();
    }

    public String getType() {
        return mBgType;
    }

    public void setType(String type) {
        mBgType = type;
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        mColor = color; 
    }

    public String getColorMode() {
        return mColorMode;
    }

    public void setColorMode(String colorMode) {
        mColorMode = colorMode;
    }

    public float getAlpha() {
        return mAlpha;
    }

    public void setAlpha(float alpha) {
        if (alpha < 0) alpha = 0;
        if (alpha > 1) alpha = 1;
        mAlpha = alpha;
    }

    public void setAlpha(int alpha) {
        if (alpha < 0) alpha = 0;
        if (alpha > 100) alpha = 100;
        mAlpha = (float)alpha / (float)100;
    }

    public void updateNotificationPanelBackground() {
        mNotificationPanelView.setBackgroundResource(0);
        mNotificationPanelView.setBackgroundResource(
                mNotificationPanelView.getResources().getIdentifier(
                        "notification_panel_bg", "drawable", PACKAGE_NAME));
        Drawable background = mNotificationPanelView.getBackground();
        background.setAlpha(mAlpha == 0 ? 255 : 
            (int)(1-mAlpha * 255));

        updateNotificationWallpaper();
    }

    private void updateNotificationWallpaper() {
        if (mNotificationWallpaperImage != null) {
            removeView(mNotificationWallpaperImage);
            mNotificationWallpaperImage = null;
        }

        if (mBgType.equals(GravityBoxSettings.NOTIF_BG_DEFAULT)) return;

        boolean isLandscape = false;
        File file = new File(mNotifBgImagePathPortrait);
        File fileLandscape = new File(mNotifBgImagePathLandscape);
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = display.getRotation();
        switch(orientation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                isLandscape = false;
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                isLandscape = true;
                break;
        }

        Drawable d = null;
        if (mBgType.equals(GravityBoxSettings.NOTIF_BG_IMAGE) && file.exists()) {
            
            if (isLandscape && fileLandscape.exists()) {
                mBitmapWallpaper = BitmapFactory.decodeFile(mNotifBgImagePathLandscape);
            } else {
                mBitmapWallpaper = BitmapFactory.decodeFile(mNotifBgImagePathPortrait);
            }
            if (mBitmapWallpaper != null) {
                d = new BitmapDrawable(getResources(), mBitmapWallpaper);
            }
        } else if (mBgType.equals(GravityBoxSettings.NOTIF_BG_COLOR)) {
            d = new ColorDrawable();
            ((ColorDrawable)d).setColor(mColor);
        }

        if (d != null) {
            d.setAlpha(mAlpha == 0 ? 255 : (int) ((1-mAlpha) * 255));
            if (mColorMode.equals(GravityBoxSettings.NOTIF_BG_COLOR_MODE_UNDERLAY)) {
                ViewParent parent = getParent();
                if (parent != null && parent instanceof FrameLayout) {
                    ((FrameLayout)parent).setBackground(d);
                }
            } else if (mColorMode.equals(GravityBoxSettings.NOTIF_BG_COLOR_MODE_OVERLAY)) {
                mNotificationWallpaperImage = new ImageView(getContext());
                if (mBgType.equals(GravityBoxSettings.NOTIF_BG_IMAGE) &&
                        isLandscape && !fileLandscape.exists()) {
                    mNotificationWallpaperImage.setScaleType(ScaleType.CENTER_CROP);
                } else {
                    mNotificationWallpaperImage.setScaleType(ScaleType.CENTER);
                }
                mNotificationWallpaperImage.setImageDrawable(d);
                addView(mNotificationWallpaperImage, -1, -1);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mBitmapWallpaper != null)
            mBitmapWallpaper.recycle();

        System.gc();
        super.onDetachedFromWindow();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
            updateNotificationWallpaper();
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        if (intent.getAction().equals(GravityBoxSettings.ACTION_NOTIF_BACKGROUND_CHANGED)) {
            if (intent.hasExtra(GravityBoxSettings.EXTRA_BG_TYPE)) {
                setType(intent.getStringExtra(GravityBoxSettings.EXTRA_BG_TYPE));
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_BG_COLOR)) {
                try {
                	setColor((int)Long.parseLong(intent.getStringExtra(
                			GravityBoxSettings.EXTRA_BG_COLOR).replaceFirst("#", ""), 16));
                } catch(Exception e) {             	
                	setColor(intent.getIntExtra(GravityBoxSettings.EXTRA_BG_COLOR, Color.BLACK));
                }
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_BG_ALPHA)) {
                setAlpha(intent.getIntExtra(GravityBoxSettings.EXTRA_BG_ALPHA, 0));
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_BG_COLOR_MODE)) {
                setColorMode(intent.getStringExtra(GravityBoxSettings.EXTRA_BG_COLOR_MODE));
            }
            updateNotificationPanelBackground();
        }
    }
}
