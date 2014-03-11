/*
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

import android.content.Context;
import android.content.Intent;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModDownloadProvider {
    private static final String TAG = "GB:ModDownloadProvider";
    public static final String PACKAGE_NAME = "com.android.providers.downloads";

    private static final String CLASS_DOWNLOAD_SERVICE = "com.android.providers.downloads.DownloadService";
    private static final boolean DEBUG = false;

    public static final String ACTION_DOWNLOAD_STATE_CHANGED = "gravitybox.intent.action.DOWNLOAD_STATE_CHANGED";
    public static final String EXTRA_ACTIVE = "isActive";

    private static boolean mIsActive;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> classDownloadService = XposedHelpers.findClass(CLASS_DOWNLOAD_SERVICE, classLoader);

            XposedHelpers.findAndHookMethod(classDownloadService, "updateLocked", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final boolean isActive = (Boolean) param.getResult();
                    if (mIsActive != isActive) { 
                        mIsActive = isActive;
                        if (DEBUG) log("Download state changed; active=" + mIsActive);
                        final Context context = (Context) param.thisObject;
                        Intent intent = new Intent(ACTION_DOWNLOAD_STATE_CHANGED);
                        intent.putExtra(EXTRA_ACTIVE, mIsActive);
                        context.sendBroadcast(intent);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
