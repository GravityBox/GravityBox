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
import java.lang.reflect.Constructor;
import java.util.Hashtable;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.View;

import com.ceco.gm2.gravitybox.preference.AppPickerPreference;
import com.ceco.gm2.gravitybox.shortcuts.ShortcutActivity;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class GlowPadHelper {
    private static final String TAG = "GB:GlowPadHelper";
    private static final boolean DEBUG = false;

    private static String CLASS_TARGET_DRAWABLE =
            "com.android.internal.widget.multiwaveview.TargetDrawable";
    private static final String CLASS_TARGET_DRAWABLE_MTK =
            "com.android.internal.policy.impl.keyguard.TargetDrawable";

    private static Hashtable<String, AppInfo> mAppInfoCache = new Hashtable<String, AppInfo>();
    private static Constructor<?> mTargetDrawableConstructor;
    private static Constructor<?> mTargetDrawableConstructorMtk;
    private static Resources mGbResources;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static class AppInfo {
        public String key;
        public Intent intent;
        public Drawable icon;
        public String name;
    }

    public enum BgStyle {
        NONE("NONE"),
        LIGHT("LIGHT"),
        DARK("DARK"),
        BLACK("BLACK");

        private String mValue;
        BgStyle(String value) {
            mValue = value;
        }
    }

    public static void clearAppInfoCache() {
        mAppInfoCache.clear();
    }

    public static AppInfo getAppInfo(Context context, String app) {
        return getAppInfo(context, app, 50, BgStyle.NONE);
    }

    public static AppInfo getAppInfo(Context context, String app, int iconSizeDp, BgStyle bgStyle) {
        if (context == null || app == null) return null;

        try {
            if (mGbResources == null) {
                mGbResources = context.createPackageContext(GravityBox.PACKAGE_NAME, 0).getResources();
            }
            final String key = app + "_" + bgStyle.toString();
            if (mAppInfoCache.containsKey(key)) {
                if (DEBUG) log("AppInfo: returning from cache for " + key);
                return mAppInfoCache.get(key);
            }

            AppInfo appInfo = new AppInfo();
            appInfo.key = key;
            appInfo.intent = Intent.parseUri(app, 0);
            if (!appInfo.intent.hasExtra("mode")) {
                return null;
            }
            final int mode = appInfo.intent.getIntExtra("mode", AppPickerPreference.MODE_APP);
            final int iconResId = appInfo.intent.getStringExtra("iconResName") != null ?
                    mGbResources.getIdentifier(appInfo.intent.getStringExtra("iconResName"),
                    "drawable", GravityBox.PACKAGE_NAME) : 0;
            Bitmap appIcon = null;
            final Resources res = context.getResources();
            boolean isGbShortcut = false;
            if (mode == AppPickerPreference.MODE_APP) {
                PackageManager pm = context.getPackageManager();
                ActivityInfo ai = pm.getActivityInfo(appInfo.intent.getComponent(), 0);
                appInfo.name = (String) ai.loadLabel(pm);
                if (iconResId != 0) {
                    appIcon = Utils.drawableToBitmap(mGbResources.getDrawable(iconResId));
                } else {
                    appIcon = Utils.drawableToBitmap(ai.loadIcon(pm));
                }
            } else if (mode == AppPickerPreference.MODE_SHORTCUT) {
                appInfo.name = appInfo.intent.getStringExtra("label");
                if (iconResId != 0) {
                    appIcon = Utils.drawableToBitmap(mGbResources.getDrawable(iconResId));
                } else {
                    final String appIconPath = appInfo.intent.getStringExtra("icon");
                    if (appIconPath != null) {
                        File f = new File(appIconPath);
                        FileInputStream fis = new FileInputStream(f);
                        appIcon = BitmapFactory.decodeStream(fis);
                        fis.close();
                    }
                }
                isGbShortcut = appInfo.intent.getAction().equals(ShortcutActivity.ACTION_LAUNCH_ACTION);
            }
            if (appIcon != null) {
                int sizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        isGbShortcut ? 60 : iconSizeDp, res.getDisplayMetrics());
                appIcon = createStyledBitmap(appIcon, sizePx, bgStyle, isGbShortcut);
                appInfo.icon = new BitmapDrawable(res, appIcon);
            }

            mAppInfoCache.put(appInfo.key, appInfo);
            if (DEBUG) log("AppInfo: storing to cache for " + appInfo.key);
            return appInfo;
        } catch (Throwable t) {
            log("Error getting app info for " + app + "! Error: " + t.getMessage());
            return null;
        }
    }

    private static Bitmap createStyledBitmap(Bitmap bitmap, int sizePx, BgStyle bgStyle, boolean isGbShortcut) {
        bitmap = Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true);

        switch (bgStyle) {
            case LIGHT:
            case DARK:
            case BLACK:
                int bitmapSize = Math.max(bitmap.getWidth(), bitmap.getHeight());
                int marginSize = Math.round(bitmapSize / (isGbShortcut ? 16f : 2.4f));
                int size = bitmapSize + marginSize;

                Bitmap b = Bitmap.createBitmap(size, size, Config.ARGB_8888);
                Canvas canvas = new Canvas(b);
                final Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setColor(bgStyle == BgStyle.LIGHT ?
                        Color.argb(230, 255, 255, 255) : bgStyle == BgStyle.DARK ?
                            Color.argb(230, 60, 60, 60) : Color.argb(230, 0, 0, 0));
                paint.setFilterBitmap(true);
                canvas.drawCircle(size/2, size/2, size/2, paint);
                canvas.drawBitmap(bitmap, marginSize/2f, marginSize/2f, null);
                return b;
            default:
                return bitmap;
        }
    }

    public static Object createTargetDrawable(Context context, AppInfo appInfo,
            Class<? extends View> glowPadViewClass) throws Throwable {
        try {
            final Object td = getTargetDrawableConstructor(glowPadViewClass).newInstance(context.getResources(), 0);
            if (appInfo != null) {
                StateListDrawable sld = createStateListDrawable(context, appInfo);
                XposedHelpers.setObjectField(td, "mDrawable", sld);
                XposedHelpers.callMethod(td, "resizeDrawables");
                XposedHelpers.setAdditionalInstanceField(td, "mGbAppInfo", appInfo);
            }

            return td;
        } catch (Throwable t) {
            XposedBridge.log(t);
            return null;
        }
    }

    private static Constructor<?> getTargetDrawableConstructor(Class<? extends View> glowPadViewClass) throws Throwable {
        try {
            if (glowPadViewClass.getName().contains("Mediatek")) {
                if (mTargetDrawableConstructorMtk != null) return mTargetDrawableConstructorMtk;

                return mTargetDrawableConstructorMtk = XposedHelpers.findConstructorExact(
                        XposedHelpers.findClass(CLASS_TARGET_DRAWABLE_MTK, null), 
                        Resources.class, int.class);
            } else {
                if (mTargetDrawableConstructor != null) return mTargetDrawableConstructor;

                return mTargetDrawableConstructor = XposedHelpers.findConstructorExact(
                        XposedHelpers.findClass(CLASS_TARGET_DRAWABLE, null), 
                        Resources.class, int.class);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
            return null;
        }
    }

    public static StateListDrawable createStateListDrawable(Context context, AppInfo appInfo) {
        if (appInfo.icon == null) return null;

        try {
            if (mGbResources == null) {
                mGbResources = context.createPackageContext(GravityBox.PACKAGE_NAME, 0).getResources();
            }

            Paint paint = null;
            if (appInfo.intent.getStringExtra("iconResName") != null) {
                paint = new Paint();
                paint.setColorFilter(new LightingColorFilter(0xFF585858, 1));
            }

            Bitmap bg = Utils.drawableToBitmap(mGbResources.getDrawable(R.drawable.target_background));
            Bitmap fg = Utils.drawableToBitmap(appInfo.icon.mutate());
            float left = (bg.getWidth() - fg.getWidth()) / 2f;
            float top = (bg.getHeight() - fg.getHeight()) / 2f;
            RectF fgRect = new RectF(left, top, left+fg.getWidth(), top+fg.getHeight());

            Bitmap bNormal = Bitmap.createBitmap(bg.getWidth(), bg.getHeight(), Config.ARGB_8888);
            Canvas canvasNormal = new Canvas(bNormal);
            canvasNormal.drawBitmap(fg, null, fgRect, null);
            Drawable normalDrawable = new BitmapDrawable(context.getResources(), bNormal);

            Bitmap bActive = Bitmap.createBitmap(bg.getWidth(), bg.getHeight(), Config.ARGB_8888);
            Canvas canvasActive = new Canvas(bActive);
            canvasActive.drawBitmap(bg, 0, 0, null);
            canvasActive.drawBitmap(fg, null, fgRect, paint);
            Drawable activeDrawable = new BitmapDrawable(context.getResources(), bActive);

            StateListDrawable sld = new StateListDrawable();
            sld.addState(new int[] { android.R.attr.state_enabled, -android.R.attr.state_active, 
                    -android.R.attr.state_focused }, normalDrawable);
            sld.addState(new int[] { android.R.attr.state_enabled, android.R.attr.state_active, 
                    -android.R.attr.state_focused }, activeDrawable);
            sld.addState(new int[] { android.R.attr.state_enabled, -android.R.attr.state_active, 
                    android.R.attr.state_focused}, activeDrawable);

            return sld;
        } catch (Throwable t) {
            XposedBridge.log(t);
            return null;
        }
    }
}
