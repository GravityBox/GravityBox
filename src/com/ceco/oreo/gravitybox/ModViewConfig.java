/*
 * Copyright (C) 2015 Peter Gregus for GravityBox Project (C3C076@xda)
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

package com.ceco.oreo.gravitybox;

import android.content.res.Configuration;
import android.view.ViewConfiguration;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

public class ModViewConfig {
    private static final String TAG = "GB:ModViewConfig";
    private static final String CLASS_ACTIONBAR_POLICY = "com.android.internal.view.ActionBarPolicy";
    private static final String CLASS_ACTIVITY_MANAGER_SERVICE = "com.android.server.am.ActivityManagerService";
    private static final String CLASS_ACTIVITY_RECORD = "com.android.server.am.ActivityRecord";

    public static void initAndroid(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final String mode = prefs.getString(GravityBoxSettings.PREF_KEY_FORCE_OVERFLOW_MENU_BUTTON, "default");
            if (!"default".equals(mode)) {
                XposedHelpers.findAndHookMethod(ViewConfiguration.class, "hasPermanentMenuKey",
                        XC_MethodReplacement.returnConstant(!"enabled".equals(mode)));

                final Class<?> actionBarPolicyClass = XposedHelpers.findClass(CLASS_ACTIONBAR_POLICY, null);
                XposedHelpers.findAndHookMethod(actionBarPolicyClass, "showsOverflowMenuButton",
                        XC_MethodReplacement.returnConstant("enabled".equals(mode)));
            }
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
        }

        try {
            if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FORCE_LTR_DIRECTION, false)) {
                final Class<?> activityManagerSvcClass = XposedHelpers.findClass(CLASS_ACTIVITY_MANAGER_SERVICE, classLoader);
                    XposedHelpers.findAndHookMethod(activityManagerSvcClass, "updateConfigurationLocked", 
                            Configuration.class, CLASS_ACTIVITY_RECORD, boolean.class, boolean.class,
                            int.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            if (param.args[0] != null) {
                                ((Configuration) param.args[0]).setLayoutDirection(null);
                            }
                        }
                    });
            }
        } catch (Throwable t) {
            GravityBox.log(TAG, t);
        }
    }
}
