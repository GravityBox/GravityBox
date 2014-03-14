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

import static de.robv.android.xposed.XposedHelpers.findClass;

import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModSignalIconHide {
    private static final String TAG = "GB:ModSignalIconHide";
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String CLASS_SIGNAL_CLUSTER_VIEW = "com.android.systemui.statusbar.SignalClusterViewGemini";
    private static final String CLASS_SIM_HELPER = "com.android.systemui.statusbar.util.SIMHelper";
    private static final String CLASS_UICC_CONTROLLER = Build.VERSION.SDK_INT > 16 ?
            "com.android.internal.telephony.uicc.UiccController" :
            "com.android.internal.telephony.IccCard";
    private static final boolean DEBUG = false;

    private static boolean autohideSlot1;
    private static boolean autohideSlot2;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, ClassLoader classLoader) {
        if (DEBUG) log("init");

        try {

            Set<String> autoHidePrefs = prefs.getStringSet(GravityBoxSettings.PREF_KEY_SIGNAL_ICON_AUTOHIDE, null);
            autohideSlot1 = (autoHidePrefs != null && autoHidePrefs.contains("sim1"));
            autohideSlot2 = (autoHidePrefs != null && autoHidePrefs.contains("sim2"));
            if (DEBUG) log("autohideSlot1 = " + autohideSlot1 + "; autohideSlot2 = " + autohideSlot2);

            final Class<?> signalClusterViewClass = XposedHelpers.findClass(CLASS_SIGNAL_CLUSTER_VIEW, classLoader);
            final Class<?> SIMHelperClass = XposedHelpers.findClass(CLASS_SIM_HELPER, classLoader);

            XposedBridge.hookAllConstructors(signalClusterViewClass, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Context context = ((LinearLayout) param.thisObject).getContext();
                    BroadcastReceiver br = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_SIGNAL_ICON_AUTOHIDE_CHANGED)) {
                                if (DEBUG) log("ACTION_PREF_SIGNAL_ICON_AUTOHIDE_CHANGED broadcast received");
                                String[] autohidePrefs = intent.getStringArrayExtra("autohidePrefs");
                                autohideSlot1 = autohideSlot2 = false;
                                for (String str: autohidePrefs) {
                                    autohideSlot1 |= str.equals("sim1");
                                    autohideSlot2 |= str.equals("sim2");
                                }
                                if (DEBUG) {
                                    log("autohideSlot1 = " + autohideSlot1 + "; autohideSlot2 = " + autohideSlot2);
                                    log("invoking apply() method");
                                }
                                XposedHelpers.callMethod(param.thisObject, "apply");
                            }
                        }
                    };

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_SIGNAL_ICON_AUTOHIDE_CHANGED);
                    context.registerReceiver(br, intentFilter);
                }
            });

            XposedHelpers.findAndHookMethod(signalClusterViewClass, "apply", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object mobileGroup = XposedHelpers.getObjectField(param.thisObject, "mMobileGroup");
                    ViewGroup vgSlot1, vgSlot2;
                    if (mobileGroup instanceof ViewGroup[]) {
                        vgSlot1 = (ViewGroup) ((ViewGroup[])mobileGroup)[0];
                        vgSlot2 = (ViewGroup) ((ViewGroup[])mobileGroup)[1];
                    } else {
                        vgSlot1 = (ViewGroup) mobileGroup;
                        vgSlot2 = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mMobileGroupGemini");
                    }
                    boolean isSim1Inserted = false;
                    boolean isSim2Inserted = false;

                    if (vgSlot1 != null) {
                        try {
                            isSim1Inserted = (Boolean)XposedHelpers.callMethod(param.thisObject, "isSimInserted", 0);
                        } catch (NoSuchMethodError nme) {
                            isSim1Inserted = (Boolean)XposedHelpers.callStaticMethod(SIMHelperClass, "isSimInserted", 0);
                        }
                        if (!isSim1Inserted && autohideSlot1)
                            vgSlot1.setVisibility(View.GONE);
                        else
                            vgSlot1.setVisibility(View.VISIBLE);
                    }

                    if (vgSlot2 != null) { 
                        try {
                            isSim2Inserted = (Boolean)XposedHelpers.callMethod(param.thisObject, "isSimInserted", 1);
                        } catch (NoSuchMethodError nme) {
                            isSim2Inserted = (Boolean)XposedHelpers.callStaticMethod(SIMHelperClass, "isSimInserted", 1);
                        }
                        if (!isSim2Inserted && autohideSlot2)
                            vgSlot2.setVisibility(View.GONE);
                        else
                            vgSlot2.setVisibility(View.VISIBLE);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static void initZygote(final XSharedPreferences prefs) {
        if (DEBUG) log("initZygote");

        try {
            Class<?> uiccControllerClass = findClass(CLASS_UICC_CONTROLLER, null);
    
            XposedHelpers.findAndHookMethod(uiccControllerClass, "setNotification", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (DEBUG) log("UiccController.setNotification(" + param.args[0] + ")");
                    prefs.reload();
                    Set<String> autohidePrefs = prefs.getStringSet(GravityBoxSettings.PREF_KEY_SIGNAL_ICON_AUTOHIDE, null);
                    if (autohidePrefs != null && autohidePrefs.contains("notifications_disabled")) {
                        if (DEBUG) log("SIM not inserted notifications disabled - skipping method");
                        param.setResult(null);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}