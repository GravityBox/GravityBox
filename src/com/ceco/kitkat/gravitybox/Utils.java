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

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Vibrator;
import android.renderscript.Allocation;
import android.renderscript.Allocation.MipmapControl;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.Map.Entry;

import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class Utils {
    private static final String TAG = "GB:Utils";
    private static final boolean DEBUG = false;

    // Device types
    private static final int DEVICE_PHONE = 0;
    private static final int DEVICE_HYBRID = 1;
    private static final int DEVICE_TABLET = 2;

    // Device type reference
    private static int mDeviceType = -1;
    private static Boolean mIsMtkDevice = null;
    private static Boolean mIsXperiaDevice = null;
    private static Boolean mIsMotoXtDevice = null;
    private static Boolean mIsExynosDevice = null;
    private static Boolean mHasLenovoCustomUI = null;
    private static Boolean mHasLenovoVibeUI = null;
    private static Boolean mIsLenovoROW = null;
    private static Boolean mIsWifiOnly = null;
    private static String mDeviceCharacteristics = null;
    
    // Device features
    private static Boolean mHasGeminiSupport = null;
    private static Boolean mHasTelephonySupport = null;
    private static Boolean mHasVibrator = null;
    private static Boolean mHasFlash = null;
    private static Boolean mHasGPS = null;
    private static Boolean mHasNfc = null;
    private static Boolean mHasCompass;
    private static Boolean mHasProximitySensor = null;

    // Supported MTK devices
    private static final Set<String> MTK_DEVICES = new HashSet<String>(Arrays.asList(
        new String[] {
                // Single-core SoC
                "mt6575",
                // Dual-core SoC
                "mt6572", "mt6577", "mt8377",
                // Quad-core SoC
                "mt6582", "mt6589", "mt8389", "mt8121",
                // Octa-core SoC
                "mt6592", "mt6595"
        }
    ));

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static int getScreenType(Context con) {
        if (mDeviceType == -1) {
            WindowManager wm = (WindowManager)con.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(outMetrics);
            int shortSize = Math.min(outMetrics.heightPixels, outMetrics.widthPixels);
            int shortSizeDp = shortSize * DisplayMetrics.DENSITY_DEFAULT / outMetrics.densityDpi;
            if (shortSizeDp < 600) {
                // 0-599dp: "phone" UI with a separate status & navigation bar
                mDeviceType =  DEVICE_PHONE;
            } else if (shortSizeDp < 720) {
                // 600-719dp: "phone" UI with modifications for larger screens
                mDeviceType = DEVICE_HYBRID;
            } else {
                // 720dp: "tablet" UI with a single combined status & navigation bar
                mDeviceType = DEVICE_TABLET;
            }
        }
        return mDeviceType;
    }

    public static boolean isPhoneUI(Context con) {
        return (getScreenType(con) == DEVICE_PHONE && !isTablet());
    }

    public static boolean isHybridUI(Context con) {
        return getScreenType(con) == DEVICE_HYBRID;
    }

    public static boolean isTabletUI(Context con) {
        return getScreenType(con) == DEVICE_TABLET;
    }

    public static boolean isTablet() {
        String deviceCharacteristics = getDeviceCharacteristics();
        return (deviceCharacteristics != null
                    && deviceCharacteristics.contains("tablet"));
    }

    public static enum MethodState {
        UNKNOWN,
        METHOD_ENTERED,
        METHOD_EXITED
    }

    public static boolean isMtkDevice() {
        if (mIsMtkDevice != null) return mIsMtkDevice;

        mIsMtkDevice = MTK_DEVICES.contains(Build.HARDWARE.toLowerCase());
        return mIsMtkDevice;
    }

    public static boolean isXperiaDevice() {
        if (mIsXperiaDevice != null) return mIsXperiaDevice;

        boolean isXperiaDevice = Build.MANUFACTURER.equalsIgnoreCase("sony")
                && !isMtkDevice();

        if (isXperiaDevice) {
            // doublecheck - hacky workaround to determine whether device is running GPE ROM
            try {
                XposedHelpers.findMethodExact("com.android.internal.widget.multiwaveview.GlowPadView",
                        null, "showTargets", boolean.class, int.class);
            } catch (Throwable t) {
                isXperiaDevice = false;
            }
        }

        return (mIsXperiaDevice = isXperiaDevice);
    }

    public static boolean isMotoXtDevice() {
        if (mIsMotoXtDevice != null) return mIsMotoXtDevice;

        mIsMotoXtDevice = Build.MANUFACTURER.equalsIgnoreCase("motorola") &&
                (Build.MODEL.toLowerCase().startsWith("xt") ||
                 Build.MODEL.toLowerCase().contains("razr"));
        return mIsMotoXtDevice;
    }

    public static boolean isExynosDevice() {
        if (mIsExynosDevice != null) return mIsExynosDevice;

        mIsExynosDevice = Build.HARDWARE.toLowerCase().contains("smdk");
        return mIsExynosDevice;
    }

    public static boolean hasLenovoCustomUI() {
        if (mHasLenovoCustomUI != null) return mHasLenovoCustomUI;

        File f = new File("/system/framework/lenovo-res.apk");
        mHasLenovoCustomUI = f.exists();
        return mHasLenovoCustomUI;
    }

    public static boolean hasLenovoVibeUI() {
        if (mHasLenovoVibeUI != null) return mHasLenovoVibeUI;

        File f = new File("/system/framework/lenovosystemuiadapter.jar");
        mHasLenovoVibeUI = hasLenovoCustomUI() && f.exists();
        return mHasLenovoVibeUI;
    }

    public static boolean isLenovoROW() {
        // Lenovo releases ROW (Rest Of the World) firmware for European versions of their devices
        if (mIsLenovoROW != null) return mIsLenovoROW;

        mIsLenovoROW = hasLenovoCustomUI() &&
                SystemProp.get("ro.product.device").toUpperCase(Locale.US).endsWith("_ROW");
        return mIsLenovoROW;
    }

    public static boolean hasGeminiSupport() {
        if (mHasGeminiSupport != null) return mHasGeminiSupport;

        mHasGeminiSupport = SystemProp.getBoolean("ro.mediatek.gemini_support", false);
        return mHasGeminiSupport;
    }

    public static boolean isWifiOnly(Context con) {
        // returns true if device doesn't support mobile data (is wifi only)
        if (mIsWifiOnly != null) return mIsWifiOnly;

        try {
            ConnectivityManager cm = (ConnectivityManager) con.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
            mIsWifiOnly = (cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) == null);
            return mIsWifiOnly;
        } catch (Throwable t) {
            mIsWifiOnly = null;
            return false;
        }
    }

    // to be called from settings or other user activities
    public static boolean hasTelephonySupport(Context con) {
        // returns false if device has no phone radio (no telephony support)
        if (mHasTelephonySupport != null) return mHasTelephonySupport;

        try {
            TelephonyManager tm = (TelephonyManager) con.getSystemService(
                Context.TELEPHONY_SERVICE);
            mHasTelephonySupport = (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE);
            return mHasTelephonySupport;
        } catch (Throwable t) {
            mHasTelephonySupport = null;
            return false;
        }
    }

    // to be called from system context only
    public static boolean hasTelephonySupport() {
        try {
            Resources res = XResources.getSystem();
            return res.getBoolean(res.getIdentifier("config_voice_capable", "bool", "android"));
        } catch (Throwable t) {
            log("hasTelephonySupport(): " + t.getMessage());
            return false;
        }
    }

    public static boolean hasVibrator(Context con) {
        if (mHasVibrator != null) return mHasVibrator;

        try {
            Vibrator v = (Vibrator) con.getSystemService(Context.VIBRATOR_SERVICE);
            mHasVibrator = v.hasVibrator();
            return mHasVibrator;
        } catch (Throwable t) {
            mHasVibrator = null;
            return false;
        }
    }

    public static boolean hasFlash(Context con) {
        if (mHasFlash != null) return mHasFlash;

        try {
            PackageManager pm = con.getPackageManager();
            mHasFlash = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
            return mHasFlash;
        } catch (Throwable t) {
            mHasFlash = null;
            return false;
        }
    }

    public static boolean hasGPS(Context con) {
        if (mHasGPS != null) return mHasGPS;

        try {
            PackageManager pm = con.getPackageManager();
            mHasGPS = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
            return mHasGPS;
        } catch (Throwable t) {
            mHasGPS = null;
            return false;
        }
    }

    public static boolean hasNfc(Context con) {
        if (mHasNfc != null) return mHasNfc;

        try {
            PackageManager pm = con.getPackageManager();
            mHasNfc = pm.hasSystemFeature(PackageManager.FEATURE_NFC);
            return mHasNfc;
        } catch (Throwable t) {
            mHasNfc = null;
            return false;
        }
    }

    public static boolean hasCompass(Context context) {
        if (mHasCompass != null) return mHasCompass;

        try {
            SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            mHasCompass = (sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null &&
                        sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null);
            return mHasCompass;
        } catch (Throwable t) {
            mHasCompass = null;
            return false;
        }
    }

    public static boolean hasProximitySensor(Context con) {
        if (mHasProximitySensor != null) return mHasProximitySensor;

        try {
            PackageManager pm = con.getPackageManager();
            mHasProximitySensor = pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY);
            return mHasProximitySensor;
        } catch (Throwable t) {
            mHasProximitySensor = null;
            return false;
        }
    }

    public static String getDeviceCharacteristics() {
        if (mDeviceCharacteristics != null) return mDeviceCharacteristics;

        mDeviceCharacteristics = SystemProp.get("ro.build.characteristics");
        return mDeviceCharacteristics;
    }

    public static boolean shouldAllowMoreVolumeSteps() {
        return !("GT-I9505G".equals(Build.MODEL) &&
                    !isMtkDevice());
    }

    public static String join(String[] stringArray, String separator) {
        String buf = "";
        for (String s : stringArray) {
            if (!buf.isEmpty()) buf += separator;
            buf += s;
        }
        return buf;
    }

    public static boolean isAppInstalled(Context context, String appUri) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(appUri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void copyFile(File source, File dest) throws IOException { 
        InputStream input = null; 
        OutputStream output = null; 
        try { 
            input = new FileInputStream(source); 
            output = new FileOutputStream(dest); 
            byte[] buf = new byte[1024]; 
            int bytesRead; 
            while ((bytesRead = input.read(buf)) > 0) { 
                output.write(buf, 0, bytesRead); 
            }
        } finally {
            if (input != null) input.close(); 
            if (output != null) output.close(); 
        } 
    } 

    public static boolean writeAssetToFile(Context context, String assetName, File outFile) {
        try {
            AssetManager am = context.getAssets();
            InputStream input = am.open(assetName);
            FileOutputStream output = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > 0){
                output.write(buffer, 0, len);
            }
            input.close();
            output.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable == null) return null;

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap); 
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static Bitmap blurBitmap(Context context, Bitmap bmp) {
        return blurBitmap(context, bmp, 14);
    }

    public static Bitmap blurBitmap(Context context, Bitmap bmp, float radius) {
        Bitmap out = Bitmap.createBitmap(bmp);
        RenderScript rs = RenderScript.create(context);
        radius = Math.min(Math.max(radius, 0), 25);

        Allocation input = Allocation.createFromBitmap(
                rs, bmp, MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        Allocation output = Allocation.createTyped(rs, input.getType());

        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setInput(input);
        script.setRadius(radius);
        script.forEach(output);

        output.copyTo(out);

        rs.destroy();
        return out;
    }

    @SuppressLint("UseSparseArrays")
    public static int getBitmapPredominantColor(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int[] pixels = new int[width * height];

        bmp.getPixels(pixels, 0, width, 0, 0, width, height);

        Map<Integer, Integer> tmpMap = new HashMap<Integer, Integer>();
        for (int i=0; i < pixels.length ; i++) {
            Integer counter = (Integer) tmpMap.get(pixels[i]);
            if (counter == null) counter = 0;
            counter++;
            tmpMap.put(pixels[i], counter);
        }

        Entry<Integer, Integer> maxEntry = null;
        for (Entry<Integer, Integer> entry : tmpMap.entrySet()) {
            // discard transparent pixels
            if (entry.getKey() == Color.TRANSPARENT) continue;

            if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
                maxEntry = entry;
            }
        }

        return maxEntry.getKey();
    }

    public static void performSoftReboot() {
        try {
            SystemProp.set("ctl.restart", "surfaceflinger");
            SystemProp.set("ctl.restart", "zygote");
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static String intArrayToString(int[] array) {
        String buf = "[";
        for (int i = 0; i < array.length; i++) {
            if (buf.length() > 1) buf += ",";
            buf += array[i];
        }
        buf += "]";
        return buf;
    }

    public static long[] csvToLongArray(String csv) throws Exception {
        String[] vals = csv.split(",");
        long[] arr = new long[vals.length];
        for (int i=0; i<arr.length; i++) {
            arr[i] = Long.valueOf(vals[i]);
        }
        return arr;
    }

    public static boolean isTimeOfDayInRange(long timeMs, int startMin, int endMin) {
        Calendar c = new GregorianCalendar();

        c.setTimeInMillis(timeMs);
        int timeMin = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);

        if (startMin == endMin) {
            return false;
        } else if (startMin > endMin) {
            return (timeMin >= startMin || timeMin < endMin);
        } else {
            return (timeMin >= startMin && timeMin < endMin);
        }
    }

    public static int alphaPercentToInt(int percentAlpha) {
        percentAlpha = Math.min(Math.max(percentAlpha, 0), 100);
        float alpha = (float)percentAlpha / 100f;
        return (alpha == 0 ? 255 : (int)(1-alpha * 255));
    }

    static class SystemProp extends Utils {
        
        private SystemProp() {

        }

        // Get the value for the given key
        // @param key: key to lookup
        // @return null if the key isn't found
        public static String get(String key) {
            String ret;

            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                ret = (String) callStaticMethod(classSystemProperties, "get", key);
            } catch (Throwable t) {
                log("SystemProp.get failed: " + t.getMessage());
                ret = null;
            }
            return ret;
        }

        // Get the value for the given key
        // @param key: key to lookup
        // @param def: default value to return
        // @return if the key isn't found, return def if it isn't null, or an empty string otherwise
        public static String get(String key, String def) {
            String ret = def;
            
            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                ret = (String) callStaticMethod(classSystemProperties, "get", key, def);
            } catch (Throwable t) {
                log("SystemProp.get failed: " + t.getMessage());
                ret = def;
            }
            return ret;
        }

        // Get the value for the given key, and return as an integer
        // @param key: key to lookup
        // @param def: default value to return
        // @return the key parsed as an integer, or def if the key isn't found or cannot be parsed
        public static Integer getInt(String key, Integer def) {
            Integer ret = def;
            
            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                ret = (Integer) callStaticMethod(classSystemProperties, "getInt", key, def);
            } catch (Throwable t) {
                log("SystemProp.getInt failed: " + t.getMessage());
                ret = def;
            }
            return ret;
        }

        // Get the value for the given key, and return as a long
        // @param key: key to lookup
        // @param def: default value to return
        // @return the key parsed as a long, or def if the key isn't found or cannot be parsed
        public static Long getLong(String key, Long def) {
            Long ret = def;
            
            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                ret = (Long) callStaticMethod(classSystemProperties, "getLong", key, def);
            } catch (Throwable t) {
                log("SystemProp.getLong failed: " + t.getMessage());
                ret = def;
            }
            return ret;
        }

        // Get the value (case insensitive) for the given key, returned as a boolean
        // Values 'n', 'no', '0', 'false' or 'off' are considered false
        // Values 'y', 'yes', '1', 'true' or 'on' are considered true
        // If the key does not exist, or has any other value, then the default result is returned
        // @param key: key to lookup
        // @param def: default value to return
        // @return the key parsed as a boolean, or def if the key isn't found or cannot be parsed
        public static Boolean getBoolean(String key, boolean def) {
            Boolean ret = def;
            
            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                ret = (Boolean) callStaticMethod(classSystemProperties, "getBoolean", key, def);
            } catch (Throwable t) {
                log("SystemProp.getBoolean failed: " + t.getMessage());
                ret = def;
            }
            return ret;
        }

        // Set the value for the given key
        public static void set(String key, String val) {
            try{
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                callStaticMethod(classSystemProperties, "set", key, val);
            } catch (Throwable t) {
                log("SystemProp.set failed: " + t.getMessage());
            }
        }
    }
}
