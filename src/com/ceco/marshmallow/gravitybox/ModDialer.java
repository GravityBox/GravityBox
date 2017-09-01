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

package com.ceco.marshmallow.gravitybox;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ceco.marshmallow.gravitybox.ledcontrol.QuietHours;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModDialer {
    private static final String TAG = "GB:ModDialer";
    public static final List<String> PACKAGE_NAMES = new ArrayList<String>(Arrays.asList(
        "com.google.android.dialer", "com.android.dialer"));

    private static final String CLASS_CALL_CARD_FRAGMENT = "com.android.incallui.CallCardFragment";
    private static final String CLASS_DIALTACTS_ACTIVITY = "com.android.dialer.DialtactsActivity";
    private static final String CLASS_DIALTACTS_ACTIVITY_GOOGLE = 
            "com.google.android.dialer.extensions.GoogleDialtactsActivity";
    private static final String CLASS_CALL_BUTTON_FRAGMENT = "com.android.incallui.CallButtonFragment";
    private static final String CLASS_ANSWER_FRAGMENT = "com.android.incallui.AnswerFragment";
    private static final String CLASS_DIALPAD_FRAGMENT = "com.android.dialer.dialpad.DialpadFragment";
    private static final boolean DEBUG = false;

    private static QuietHours mQuietHours;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, ClassLoader classLoader, final String packageName) {

        try {
            final Class<?> classAnswerFragment = XposedHelpers.findClass(CLASS_ANSWER_FRAGMENT, classLoader);
            final Class<?> classCallButtonFragment = XposedHelpers.findClass(CLASS_CALL_BUTTON_FRAGMENT, classLoader); 

            XposedHelpers.findAndHookMethod(classCallButtonFragment, "setEnabled", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final View view = ((Fragment)param.thisObject).getView();
                    final boolean fsc = prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_CALLER_FULLSCREEN_PHOTO, false);
                    if (fsc & view != null) {
                        view.setVisibility(!(Boolean)param.args[0] ? View.GONE : View.VISIBLE);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classAnswerFragment, "onShowAnswerUi", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!(Boolean) param.args[0]) return;

                    prefs.reload();
                    if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_CALLER_FULLSCREEN_PHOTO, false)) { 
                    final View v = ((Fragment)param.thisObject).getView();
                        v.setBackgroundColor(0);
                        if (Utils.isMtkDevice()) {
                            final View gpView = (View) XposedHelpers.getObjectField(param.thisObject, "mGlowpad");
                            gpView.setBackgroundColor(0);
                        }
                        if (DEBUG) log("AnswerFragment showAnswerUi: background color set");
                    }
                }
            });
        } catch (Throwable t) {
            //XposedBridge.log(t);
        }

        try {
            final Class<?> classCallCardFragment = XposedHelpers.findClass(CLASS_CALL_CARD_FRAGMENT, classLoader);

            XC_MethodHook unknownCallerHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_CALLER_UNKNOWN_PHOTO_ENABLE, false)) return;

                    int idx = param.args.length-1;
                    boolean shouldShowUnknownPhoto = param.args[idx] == null;
                    final Fragment frag = (Fragment) param.thisObject;
                    final Resources res = frag.getResources();
                    if (param.args[idx] != null) {
                        String resName = "img_no_image_automirrored";
                        Drawable picUnknown = res.getDrawable(res.getIdentifier(resName, "drawable",
                                        res.getResourcePackageName(frag.getId())), null);
                        shouldShowUnknownPhoto = ((Drawable)param.args[idx]).getConstantState().equals(
                                                    picUnknown.getConstantState());
                    }

                    if (shouldShowUnknownPhoto) {
                        final String path = Utils.getGbContext(frag.getContext()).getFilesDir() + "/caller_photo";
                        File f = new File(path);
                        if (f.exists() && f.canRead()) {
                            Bitmap b = BitmapFactory.decodeFile(path);
                            if (b != null) {
                                param.args[idx] = new BitmapDrawable(res, b);
                                if (DEBUG) log("Unknow caller photo set");
                            }
                        }
                    }
                }
            };
            try {
                XposedHelpers.findAndHookMethod(classCallCardFragment, "setDrawableToImageView",
                        ImageView.class, Drawable.class, unknownCallerHook);
            } catch (Throwable t) {
                XposedHelpers.findAndHookMethod(classCallCardFragment, "setDrawableToImageViews",
                        Drawable.class, unknownCallerHook);
            }
        } catch (Throwable t) {
            //XposedBridge.log(t);
        }

        try {
            final Class<?> classDialtactsActivity = XposedHelpers.findClass(CLASS_DIALTACTS_ACTIVITY, classLoader);

            XposedHelpers.findAndHookMethod(classDialtactsActivity, "displayFragment", Intent.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    prefs.reload();
                    if (!prefs.getBoolean(GravityBoxSettings.PREF_KEY_DIALER_SHOW_DIALPAD, false)) return;

                    final String realClassName = param.thisObject.getClass().getName();
                    if (realClassName.equals(CLASS_DIALTACTS_ACTIVITY)) {
                        XposedHelpers.callMethod(param.thisObject, "showDialpadFragment", false);
                        if (DEBUG) log("showDialpadFragment() called within " + realClassName);
                    } else if (realClassName.equals(CLASS_DIALTACTS_ACTIVITY_GOOGLE)) {
                        final Class<?> superc = param.thisObject.getClass().getSuperclass();
                        Method m = XposedHelpers.findMethodExact(superc, "showDialpadFragment", boolean.class);
                        m.invoke(param.thisObject, false);
                        if (DEBUG) log("showDialpadFragment() called within " + realClassName);
                    }
                }
            });
        } catch (Throwable t) {
            //XposedBridge.log(t);
        }

        try {
            XposedHelpers.findAndHookMethod(CLASS_DIALPAD_FRAGMENT, classLoader, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param2) throws Throwable {
                    XSharedPreferences qhPrefs = new XSharedPreferences(GravityBox.PACKAGE_NAME, "quiet_hours");
                    mQuietHours = new QuietHours(qhPrefs);
                }
            });

            XposedHelpers.findAndHookMethod(CLASS_DIALPAD_FRAGMENT, classLoader, "playTone",
                    int.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (mQuietHours.isSystemSoundMuted(QuietHours.SystemSound.DIALPAD)) {
                        param.setResult(null);
                    }
                }
            });
        } catch (Throwable t) {
            //XposedBridge.log(t);
        }
    }
}
