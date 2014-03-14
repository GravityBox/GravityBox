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

import java.lang.reflect.Field;
import java.util.Map;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModVolumePanel {
    private static final String TAG = "GB:ModVolumePanel";
    public static final String PACKAGE_NAME = "android";
    private static final String CLASS_VOLUME_PANEL = "android.view.VolumePanel";
    private static final String CLASS_STREAM_CONTROL = "android.view.VolumePanel$StreamControl";
    private static final String CLASS_AUDIO_SERVICE = "android.media.AudioService";
    private static final String CLASS_VIEW_GROUP = "android.view.ViewGroup";
    private static final boolean DEBUG = false;

    private static final int STREAM_RING = 2;
    private static final int STREAM_NOTIFICATION = 5;
    private static final int MSG_TIMEOUT = 5;

    private static Object mVolumePanel;
    private static Object mAudioService;
    private static boolean mVolumesLinked;
    private static Unhook mViewGroupAddViewHook;
    private static boolean mVolumeAdjustMuted;
    private static boolean mVolumeAdjustVibrateMuted;
    private static boolean mVoiceCapable;
    private static boolean mExpandable;
    private static boolean mExpandFully;
    private static boolean mAutoExpand;
    private static int mTimeout;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBrodcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_VOLUME_PANEL_MODE_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_EXPANDABLE)) {
                    mExpandable = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_EXPANDABLE, false);
                    updateVolumePanelMode();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_EXPANDABLE_FULLY)) {
                    mExpandFully = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_EXPANDABLE_FULLY, false);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_AUTOEXPAND)) {
                    mAutoExpand = intent.getBooleanExtra(GravityBoxSettings.EXTRA_AUTOEXPAND, false);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_MUTED)) {
                    mVolumeAdjustMuted = intent.getBooleanExtra(GravityBoxSettings.EXTRA_MUTED, false);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_VIBRATE_MUTED)) {
                    mVolumeAdjustVibrateMuted = intent.getBooleanExtra(GravityBoxSettings.EXTRA_VIBRATE_MUTED, false);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_TIMEOUT)) {
                    mTimeout = intent.getIntExtra(GravityBoxSettings.EXTRA_TIMEOUT, 3000);
                }
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_LINK_VOLUMES_CHANGED)) {
                mVolumesLinked = intent.getBooleanExtra(GravityBoxSettings.EXTRA_LINKED, true);
                if (DEBUG) log("mVolumesLinked set to: " + mVolumesLinked);
                updateStreamVolumeAlias();
            }
        }
        
    };

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> classVolumePanel = XposedHelpers.findClass(CLASS_VOLUME_PANEL, classLoader);
            final Class<?> classStreamControl = XposedHelpers.findClass(CLASS_STREAM_CONTROL, classLoader);
            final Class<?> classAudioService = XposedHelpers.findClass(CLASS_AUDIO_SERVICE, classLoader);
            final Class<?> classViewGroup = XposedHelpers.findClass(CLASS_VIEW_GROUP, classLoader);

            mVolumeAdjustMuted = prefs.getBoolean(GravityBoxSettings.PREF_KEY_VOLUME_ADJUST_MUTE, false);
            mVolumeAdjustVibrateMuted = prefs.getBoolean(GravityBoxSettings.PREF_KEY_VOLUME_ADJUST_VIBRATE_MUTE, false);

            XposedBridge.hookAllConstructors(classVolumePanel, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mVolumePanel = param.thisObject;
                    mVoiceCapable = XposedHelpers.getBooleanField(param.thisObject, "mVoiceCapable");
                    Context context = (Context) XposedHelpers.getObjectField(mVolumePanel, "mContext");
                    if (DEBUG) log("VolumePanel constructed; mVolumePanel set");

                    mExpandable = prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_VOLUME_PANEL_EXPANDABLE, false);
                    mExpandFully = prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_VOLUME_PANEL_FULLY_EXPANDABLE, false);

                    updateVolumePanelMode();

                    mAutoExpand = prefs.getBoolean(GravityBoxSettings.PREF_KEY_VOLUME_PANEL_AUTOEXPAND, false);
                    mVolumesLinked = prefs.getBoolean(GravityBoxSettings.PREF_KEY_LINK_VOLUMES, true);

                    mTimeout = 3000;
                    try {
                        mTimeout = Integer.valueOf(prefs.getString(
                                GravityBoxSettings.PREF_KEY_VOLUME_PANEL_TIMEOUT, "3000"));
                    } catch (NumberFormatException nfe) {
                        log("Invalid value for PREF_KEY_VOLUME_PANEL_TIMEOUT preference");
                    }

                    Object[] streams = (Object[]) XposedHelpers.getStaticObjectField(classVolumePanel, "STREAMS");
                    XposedHelpers.setBooleanField(streams[1], "show", 
                            (Boolean) XposedHelpers.getBooleanField(param.thisObject, "mVoiceCapable"));
                    XposedHelpers.setBooleanField(streams[5], "show", true);

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_LINK_VOLUMES_CHANGED);
                    context.registerReceiver(mBrodcastReceiver, intentFilter);
                }
            });
            
            if (Utils.isXperiaDevice()) {
                try {
                    XposedHelpers.findAndHookMethod(classVolumePanel, "inflateUi", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            mVoiceCapable = XposedHelpers.getBooleanField(param.thisObject, "mVoiceCapable");
                            updateVolumePanelMode();
                        }
                    });
                } catch (Throwable t) {
                    if (DEBUG) log("We might want to fix our Xperia detection...");
                }
            }

            XposedHelpers.findAndHookMethod(classVolumePanel, "createSliders", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    final boolean voiceCapableOrig = XposedHelpers.getBooleanField(param.thisObject, "mVoiceCapable");
                    if (DEBUG) log("createSliders: original mVoiceCapable = " + voiceCapableOrig);
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "mGbVoiceCapableOrig", voiceCapableOrig);
                    XposedHelpers.setBooleanField(param.thisObject, "mVoiceCapable", false);
                }
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final Boolean voiceCapableOrig =  (Boolean)XposedHelpers.getAdditionalInstanceField(
                            param.thisObject, "mGbVoiceCapableOrig");
                    if (voiceCapableOrig != null) {
                        if (DEBUG) log("createSliders: restoring original mVoiceCapable");
                        XposedHelpers.setBooleanField(param.thisObject, "mVoiceCapable", voiceCapableOrig);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classVolumePanel, "expand", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    hideNotificationSliderIfLinked();
                    
                    if (!mExpandFully)
                        return;
                    View mMoreButton = (View) XposedHelpers.getObjectField(mVolumePanel, "mMoreButton");
                    View mDivider = (View) XposedHelpers.getObjectField(mVolumePanel, "mDivider");
                    
                    if (mMoreButton != null) {
                        mMoreButton.setVisibility(View.GONE);
                    }
                    
                    if (mDivider != null) {
                    	mDivider.setVisibility(View.GONE);
                    }
                }
            });

            try {
                final Field fldVolTitle = XposedHelpers.findField(classStreamControl, "volTitle");
                if (DEBUG) log("Hooking StreamControl constructor for volTitle field initialization");
                XposedBridge.hookAllConstructors(classStreamControl, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        Context context = (Context) XposedHelpers.getObjectField(
                                XposedHelpers.getSurroundingThis(param.thisObject), "mContext");
                        if (context != null) {
                            TextView tv = new TextView(context);
                            fldVolTitle.set(param.thisObject, tv);
                            if (DEBUG) log("StreamControl: volTitle field initialized");
                        }
                    }
                });
            } catch(Throwable t) {
                if (DEBUG) log("StreamControl: exception while initializing volTitle field: " + t.getMessage());
            }

            // Samsung bug workaround
            XposedHelpers.findAndHookMethod(classVolumePanel, "addOtherVolumes", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (DEBUG) log("addOtherVolumes: hooking ViewGroup.addViewInner");

                    mViewGroupAddViewHook = XposedHelpers.findAndHookMethod(classViewGroup, "addViewInner", 
                            View.class, int.class, ViewGroup.LayoutParams.class, 
                            boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param2) throws Throwable {
                            if (DEBUG) log("ViewGroup.addViewInner called from VolumePanel.addOtherVolumes()");
                            View child = (View) param2.args[0];
                            if (child.getParent() != null) {
                                if (DEBUG) log("Ignoring addView for child: " + child.toString());
                                param2.setResult(null);
                                return;
                            }
                        }
                    });
                }
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mViewGroupAddViewHook != null) {
                        if (DEBUG) log("addOtherVolumes: unhooking ViewGroup.addViewInner");
                        mViewGroupAddViewHook.unhook();
                        mViewGroupAddViewHook = null;
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classVolumePanel, "onPlaySound",
                    int.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mVolumeAdjustMuted) {
                        param.setResult(null);
                    }
                }
            });

            XposedBridge.hookAllConstructors(classAudioService, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mAudioService = param.thisObject;
                    mVolumesLinked = prefs.getBoolean(GravityBoxSettings.PREF_KEY_LINK_VOLUMES, true);
                    if (DEBUG) log("AudioService constructed: mAudioService set");
                    updateStreamVolumeAlias();
                }
            });

            XposedHelpers.findAndHookMethod(classVolumePanel, "onShowVolumeChanged", 
                    int.class, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mExpandable && mAutoExpand) {
                        final Dialog dialog = (Dialog) XposedHelpers.getObjectField(param.thisObject, "mDialog");
                        if (dialog != null && dialog.isShowing() &&
                                !(Boolean) XposedHelpers.callMethod(param.thisObject, "isExpanded")) {
                            XposedHelpers.callMethod(param.thisObject, "expand");
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classVolumePanel, "resetTimeout", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Handler h = (Handler) param.thisObject;
                        h.removeMessages(MSG_TIMEOUT);
                        h.sendMessageDelayed(h.obtainMessage(MSG_TIMEOUT), mTimeout);
                        return null;
                    } catch(Throwable t) {
                        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                    }
                }
                
            });

            XposedHelpers.findAndHookMethod(classVolumePanel, "onVibrate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mVolumeAdjustVibrateMuted) {
                        param.setResult(null);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void updateVolumePanelMode() {
        if (mVolumePanel == null) return;

        View mMoreButton = (View) XposedHelpers.getObjectField(mVolumePanel, "mMoreButton");
        View mDivider = (View) XposedHelpers.getObjectField(mVolumePanel, "mDivider");

        if (mMoreButton != null) {
            mMoreButton.setVisibility(mExpandable ? View.VISIBLE : View.GONE);
            if (!mMoreButton.hasOnClickListeners()) {
                mMoreButton.setOnClickListener((OnClickListener) mVolumePanel);
            }
        }

        if (mDivider != null) {
            mDivider.setVisibility(mExpandable ? View.VISIBLE : View.GONE);
        }

        XposedHelpers.setBooleanField(mVolumePanel, "mShowCombinedVolumes", mExpandable);
        XposedHelpers.setObjectField(mVolumePanel, "mStreamControls", null);
        if (DEBUG) log("VolumePanel mode changed to: " + ((mExpandable) ? "EXPANDABLE" : "SIMPLE"));
    }

    private static boolean shouldLinkVolumes() {
        return mVolumesLinked && mVoiceCapable;
    }

    private static void hideNotificationSliderIfLinked() {
        if (mVolumePanel == null || !shouldLinkVolumes()) return;

        @SuppressWarnings("unchecked")
        Map<Integer, Object> streamControls = 
                (Map<Integer, Object>) XposedHelpers.getObjectField(mVolumePanel, "mStreamControls");
        if (streamControls == null) return;

        for (Object o : streamControls.values()) {
            if ((Integer) XposedHelpers.getIntField(o, "streamType") == STREAM_NOTIFICATION) {
                View v = (View) XposedHelpers.getObjectField(o, "group");
                if (v != null) {
                    v.setVisibility(View.GONE);
                    if (DEBUG) log("Notification volume slider hidden");
                    break;
                }
            }
        }
    }

    private static void updateStreamVolumeAlias() {
        if (mAudioService == null) return;

        int[] streamVolumeAlias = (int[]) XposedHelpers.getObjectField(mAudioService, "mStreamVolumeAlias");
        streamVolumeAlias[STREAM_NOTIFICATION] = shouldLinkVolumes() ? STREAM_RING : STREAM_NOTIFICATION;
        XposedHelpers.setObjectField(mAudioService, "mStreamVolumeAlias", streamVolumeAlias);
        if (DEBUG) log("AudioService mStreamVolumeAlias updated, STREAM_NOTIFICATION set to: " + 
                streamVolumeAlias[STREAM_NOTIFICATION]);
    }
}