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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class ModLauncher {
	public static final List<String> PACKAGE_NAMES;
    private static final String TAG = "GB:ModLauncher";

    private static final List<String> CLASS_DEVICE_PROFILE; 
    private static final String CLASS_LAUNCHER = "com.android.launcher3.Launcher";
    private static final String CLASS_APP_WIDGET_HOST_VIEW = "android.appwidget.AppWidgetHostView";
    private static final boolean DEBUG = false;

    public static final String ACTION_SHOW_APP_DRAWER = "gravitybox.launcher.intent.action.SHOW_APP_DRAWER";
    
    // http://androidxref.com/4.4.2_r1/xref/packages/apps/Launcher3/src/com/android/launcher3/DynamicGrid.java#99
    // DeviceProfile(String n, float w, float h, float r, float c, float is, float its, float hs, float his)
    public static int NUMROWS = 3;
	public static int NUMCOLUMNS = 4;

	static {
		CLASS_DEVICE_PROFILE = Arrays.asList(
				"com.android.launcher3.DeviceProfile",
				"mz");
		PACKAGE_NAMES = new ArrayList<String>(Arrays.asList(
				"com.android.launcher2", "com.android.launcher3",
				"com.google.android.googlequicksearchbox",
				"com.cyanogenmod.trebuchet"));
	}

    private static boolean mShouldShowAppDrawer;
    private static boolean mReceiverRegistered;

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            i.putExtra("showAppDrawer", true);
            context.startActivity(i);
        }
    };

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
    	
		Class<?> cls = null;
		for (String className : CLASS_DEVICE_PROFILE) {

			try {
				cls = XposedHelpers.findClass(className, classLoader);
				if (DEBUG) log("Found DeviceProfile class as: " + className);
			} catch (Throwable t) {
				continue;
			}

			try {
				XposedBridge.hookAllConstructors(cls, new XC_MethodHook() {

					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

						// making sure to only hook to the appropriate constructor
						if (!(param.args[0] instanceof Context)) return;

						prefs.reload();

						final int rows = Integer.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_LAUNCHER_DESKTOP_GRID_ROWS, "0"));
						if (rows != 0) {
							param.args[NUMROWS] = rows;
							if (DEBUG) log("Launcher rows set to: " + rows);
						}
						final int cols = Integer.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_LAUNCHER_DESKTOP_GRID_COLS, "0"));
						if (cols != 0) {
							param.args[NUMCOLUMNS] = cols;
							if (DEBUG) log("Launcher cols set to: " + cols);
						}
					}
				});
            } catch (Throwable t) {
                XposedBridge.log(t);
            }

            break;
        }

        try {
            Class<?> classLauncher = null;
            try {
                classLauncher = XposedHelpers.findClass(CLASS_LAUNCHER, classLoader);
            } catch (ClassNotFoundError e) { 
                log("Launcher3.Launcher not found");
            }

            if (classLauncher != null) {
                XposedHelpers.findAndHookMethod(classLauncher, "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        IntentFilter intentFilter = new IntentFilter(ACTION_SHOW_APP_DRAWER);
                        ((Activity)param.thisObject).registerReceiver(mBroadcastReceiver, intentFilter);
                        mReceiverRegistered = true;
                    }
                });
    
                XposedHelpers.findAndHookMethod(classLauncher, "onDestroy", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                        if (mReceiverRegistered) {
                            ((Activity)param.thisObject).unregisterReceiver(mBroadcastReceiver);
                            mReceiverRegistered = false;
                        }
                    }
                });
    
                XposedHelpers.findAndHookMethod(classLauncher, "onNewIntent", Intent.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        Intent i = (Intent) param.args[0];
                        mShouldShowAppDrawer = (i != null && i.hasExtra("showAppDrawer"));
                    }
                });
    
                XposedHelpers.findAndHookMethod(classLauncher, "onResume", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        if (mShouldShowAppDrawer) {
                            mShouldShowAppDrawer = false;
                            XposedHelpers.callMethod(param.thisObject, "onClickAllAppsButton", 
                                    new Class<?>[] { View.class }, (Object)null);
                        }
                    }
                });
            }

            Class<?> classAppWidgetHostView = null;
            try {
                classAppWidgetHostView = XposedHelpers.findClass(CLASS_APP_WIDGET_HOST_VIEW, classLoader);
            } catch (ClassNotFoundError e) {
                log("AppWidgetHostView not found");
            }

            if (classAppWidgetHostView != null) {
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
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
