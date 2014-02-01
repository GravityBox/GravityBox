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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModLauncher {
    public static final List<String> PACKAGE_NAMES = new ArrayList<String>(Arrays.asList(
            "com.android.launcher3", "com.google.android.googlequicksearchbox"));
    private static final String TAG = "GB:ModLauncher";

    private static final String CLASS_DYNAMIC_GRID = "com.android.launcher3.DynamicGrid";
    private static final String CLASS_APP_WIDGET_HOST_VIEW = "android.appwidget.AppWidgetHostView";
    
    private static final boolean DEBUG = false;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> classDynamicGrid = XposedHelpers.findClass(CLASS_DYNAMIC_GRID, classLoader);
            final Class<?> classAppWidgetHostView = XposedHelpers.findClass(CLASS_APP_WIDGET_HOST_VIEW, classLoader);
            
            XposedBridge.hookAllConstructors(classDynamicGrid, new XC_MethodHook() { 
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    Object profile = XposedHelpers.getObjectField(param.thisObject, "mProfile");
                    if (profile != null) {
                        final int rows = Integer.valueOf(prefs.getString(
                                GravityBoxSettings.PREF_KEY_LAUNCHER_DESKTOP_GRID_ROWS, "0"));
                        if (rows != 0) {
                            XposedHelpers.setIntField(profile, "numRows", rows);
                            if (DEBUG) log("Launcher rows set to: " + rows);
                        }
                        final int cols = Integer.valueOf(prefs.getString(
                                GravityBoxSettings.PREF_KEY_LAUNCHER_DESKTOP_GRID_COLS, "0"));
                        if (cols != 0) {
                            XposedHelpers.setIntField(profile, "numColumns", cols);
                            if (DEBUG) log("Launcher cols set to: " + cols);
                        }
                    }
                }
            });
			
            XposedHelpers.findAndHookMethod(classAppWidgetHostView, "getAppWidgetInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_LAUNCHER_RESIZE_WIDGET, false)) {
                        Object info = XposedHelpers.getObjectField(param.thisObject, "mInfo");
                        if (info != null) {
                            XposedHelpers.setIntField(info, "resizeMode", 3);
                            XposedHelpers.setIntField(info, "minResizeWidth", 40);
                            XposedHelpers.setIntField(info, "minResizeHeight", 40);
                        }
                    }
                }
            });
			
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
