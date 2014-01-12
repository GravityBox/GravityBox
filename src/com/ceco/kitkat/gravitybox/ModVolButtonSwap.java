/*
 * Copyright (C) 2013 rovo89@xda
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

import android.content.Context;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.WindowManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodsByExactParameters;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class ModVolButtonSwap {
    private static final String TAG = "GB:ModVolumeKeySkipTrack";
    private static final boolean DEBUG = false;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    static void initZygote(final XSharedPreferences prefs) {
        try {
            if (DEBUG) log("init");

            Class<?> classAudioService = findClass("android.media.AudioService", null);

            HandleChangeVolume handleChangeVolume = new HandleChangeVolume(prefs);

            findAndHookMethod(classAudioService, "adjustMasterVolume", int.class, int.class, String.class, handleChangeVolume);
            findAndHookMethod(classAudioService, "adjustSuggestedStreamVolume", int.class, int.class, int.class, String.class, handleChangeVolume);
        } catch (Throwable t) { XposedBridge.log(t); }
    }

    private static class HandleChangeVolume extends XC_MethodHook {
        private XSharedPreferences mPrefs;

        public HandleChangeVolume(XSharedPreferences prefs) {
            mPrefs = prefs;
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            mPrefs.reload();
            if (mPrefs.getBoolean(GravityBoxSettings.KEY_SWAP_VOLUME_BUTTONS, false)) {
                if ((Integer) param.args[0] != 0) {
                    if (DEBUG) log("Original direction = " + param.args[0]);
                    int orientation = getScreenOrientation(param.thisObject);
                    param.args[0] = orientation * (Integer) param.args[0];
                    if (DEBUG) log("Modified direction = " + param.args[0]);
                }
            }
        }

        private int getScreenOrientation(Object object) {
            Context context = (Context) getObjectField(object, "mContext");
            int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            int orientation = 1;
            switch (rotation) {
                case Surface.ROTATION_0:
                    if (DEBUG) log("Rotation = 0");
                    break;
                case Surface.ROTATION_90:
                    if (DEBUG) log("Rotation = 90");
                    orientation = -1;
                    break;
                case Surface.ROTATION_180:
                    if (DEBUG) log("Rotation = 180");
                    orientation = -1;
                    break;
                default: // Surface.ROTATION_270
                    if (DEBUG) log("Rotation = 270");
                    orientation = 1;
                    break;
            }
            return orientation;
        }

    }
}
