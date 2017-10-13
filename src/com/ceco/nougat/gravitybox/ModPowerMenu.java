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

package com.ceco.nougat.gravitybox;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XResources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import com.ceco.nougat.gravitybox.R;
import com.ceco.nougat.gravitybox.adapters.*;

public class ModPowerMenu {
    private static final String TAG = "GB:ModPowerMenu";
    public static final String PACKAGE_NAME = "android";
    public static final String CLASS_GLOBAL_ACTIONS = "com.android.server.policy.GlobalActions";
    public static final String CLASS_ACTION = "com.android.server.policy.GlobalActions.Action";
    private static final boolean DEBUG = false;

    private static Context mContext;
    private static String mRebootStr;
    private static String mRebootSoftStr;
    private static String mRecoveryStr;
    private static String mBootloaderStr;
    private static Drawable mRebootIcon;
    private static Drawable mRebootSoftIcon;
    private static Drawable mRecoveryIcon;
    private static Drawable mBootloaderIcon;
    private static Drawable mExpandedDesktopIcon;
    private static Drawable mScreenshotIcon;
    private static Drawable mScreenrecordIcon;
    private static boolean mIconsTinted;
    private static List<IIconListAdapterItem> mRebootItemList;
    private static String mRebootConfirmStr;
    private static String mRebootConfirmRecoveryStr;
    private static String mRebootConfirmBootloaderStr;
    private static String mExpandedDesktopStr;
    private static String mExpandedDesktopOnStr;
    private static String mExpandedDesktopOffStr;
    private static String mScreenshotStr;
    private static String mScreenrecordStr;
    private static Unhook mRebootActionHook;
    private static Unhook mRebootActionShowHook;
    private static Object mRebootActionItem;
    private static boolean mRebootActionItemStockExists;
    private static Object mScreenshotAction;
    private static Object mScreenrecordAction;
    private static Object mExpandedDesktopAction;
    private static boolean mRebootConfirmRequired;
    private static boolean mRebootAllowOnLockscreen;
    private static boolean mAllowSoftReboot;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void initAndroid(final XSharedPreferences prefs, final ClassLoader classLoader) {

        try {
            final Class<?> globalActionsClass = XposedHelpers.findClass(CLASS_GLOBAL_ACTIONS, classLoader);
            final Class<?> actionClass = XposedHelpers.findClass(CLASS_ACTION, classLoader);

            XposedBridge.hookAllConstructors(globalActionsClass, new XC_MethodHook() {
               @Override
               protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                   mContext = (Context) param.args[0];
                   Resources res = mContext.getResources();
                   Context gbContext = Utils.getGbContext(mContext, res.getConfiguration());

                   int rebootStrId = res.getIdentifier("factorytest_reboot", "string", PACKAGE_NAME);
                   int rebootSoftStrId = R.string.reboot_soft;
                   int recoveryStrId = R.string.poweroff_recovery;
                   int bootloaderStrId = R.string.poweroff_bootloader;
                   mRebootStr  = (rebootStrId == 0) ? "Reboot" : res.getString(rebootStrId);
                   mRebootSoftStr = gbContext.getString(rebootSoftStrId);
                   mRecoveryStr = gbContext.getString(recoveryStrId);
                   mBootloaderStr = gbContext.getString(bootloaderStrId);
                   mExpandedDesktopStr = gbContext.getString(R.string.action_expanded_desktop_title);
                   mExpandedDesktopOnStr = gbContext.getString(R.string.action_expanded_desktop_on);
                   mExpandedDesktopOffStr = gbContext.getString(R.string.action_expanded_desktop_off);
                   mScreenshotStr = gbContext.getString(R.string.screenshot);
                   mScreenrecordStr = gbContext.getString(R.string.action_screenrecord);

                   mRebootIcon = gbContext.getDrawable(R.drawable.ic_lock_reboot);
                   mRebootSoftIcon = gbContext.getDrawable(R.drawable.ic_lock_reboot_soft);
                   mRecoveryIcon = gbContext.getDrawable(R.drawable.ic_lock_recovery);
                   mBootloaderIcon = gbContext.getDrawable(R.drawable.ic_lock_bootloader);
                   mExpandedDesktopIcon = gbContext.getDrawable(Utils.hasLenovoVibeUI() ?
                           R.drawable.ic_lock_expanded_desktop_vibeui : R.drawable.ic_lock_expanded_desktop);
                   mScreenshotIcon = gbContext.getDrawable(Utils.hasLenovoVibeUI() ?
                           R.drawable.ic_lock_screenshot_vibeui : R.drawable.ic_lock_screenshot);
                   mScreenrecordIcon = gbContext.getDrawable(Utils.hasLenovoVibeUI() ?
                           R.drawable.ic_lock_screen_record_vibeui : R.drawable.ic_lock_screen_record);

                   mRebootItemList = new ArrayList<IIconListAdapterItem>();
                   mRebootItemList.add(new BasicIconListItem(mRebootStr, null, mRebootIcon, null));
                   mRebootItemList.add(new BasicIconListItem(mRebootSoftStr, null, mRebootSoftIcon, null));
                   mRebootItemList.add(new BasicIconListItem(mRecoveryStr, null, mRecoveryIcon, null));
                   mRebootItemList.add(new BasicIconListItem(mBootloaderStr, null, mBootloaderIcon, null));

                   mRebootConfirmStr = String.format(gbContext.getString(R.string.reboot_confirm),
                           gbContext.getString(Utils.isTablet() ? R.string.device_tablet : R.string.device_phone));
                   mRebootConfirmRecoveryStr = String.format(gbContext.getString(R.string.reboot_confirm_recovery),
                           gbContext.getString(Utils.isTablet() ? R.string.device_tablet : R.string.device_phone));
                   mRebootConfirmBootloaderStr = String.format(gbContext.getString(R.string.reboot_confirm_bootloader),
                           gbContext.getString(Utils.isTablet() ? R.string.device_tablet : R.string.device_phone));

                   if (DEBUG) log("GlobalActions constructed, resources set.");
               }
            });

            XposedHelpers.findAndHookMethod(globalActionsClass, "createDialog", new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mRebootActionHook != null) {
                        if (DEBUG) log("Unhooking previous hook of reboot action item");
                        mRebootActionHook.unhook();
                        mRebootActionHook = null;
                    }
                    if (mRebootActionShowHook != null) {
                        mRebootActionShowHook.unhook();
                        mRebootActionShowHook = null;
                    }
                }

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mContext == null) return;

                    // tint icons with colorControlNormal of current power dialog theme
                    if (!mIconsTinted) {
                        Dialog dlg = (Dialog) param.getResult();
                        TypedValue value = new TypedValue();
                        int[] attribute = new int[] { android.R.attr.colorControlNormal };
                        TypedArray array = dlg.getContext().obtainStyledAttributes(value.resourceId, attribute);
                        int iconColor = array.getColor(0, Color.GRAY);
                        array.recycle();
                        mRebootIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                        mRebootSoftIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                        mRecoveryIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                        mBootloaderIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                        mExpandedDesktopIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                        mScreenshotIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                        mScreenrecordIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                        mIconsTinted = true;
                    }

                    mRebootConfirmRequired = prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_REBOOT_CONFIRM_REQUIRED, true);
                    mRebootAllowOnLockscreen = prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_REBOOT_ALLOW_ON_LOCKSCREEN, false);
                    mAllowSoftReboot = prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_REBOOT_ALLOW_SOFTREBOOT, false);

                    @SuppressWarnings("unchecked")
                    List<Object> mItems = (List<Object>) XposedHelpers.getObjectField(param.thisObject, "mItems");
                    BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
                    int index = 1;

                    // try to find out if reboot action item already exists in the list of GlobalActions items
                    // strategy:
                    // 1) check if Action has mIconResId field or mMessageResId field
                    // 2) check if the name of the corresponding resource contains "reboot" or "restart" substring
                    if (mRebootActionItem == null) {
                        if (DEBUG) log("Searching for existing reboot action item...");
                        Resources res = mContext.getResources();
                        for (Object o : mItems) {
                            // search for drawable
                            try {
                                Field f = XposedHelpers.findField(o.getClass(), "mIconResId");
                                String resName = res.getResourceEntryName((Integer) f.get(o)).toLowerCase(Locale.US);
                                if (DEBUG) log("Drawable resName = " + resName);
                                if (resName.contains("reboot") || resName.contains("restart")) {
                                    mRebootActionItem = o;
                                    break;
                                }
                            } catch (NoSuchFieldError nfe) {
                                // continue
                            } catch (Resources.NotFoundException resnfe) { 
                                // continue
                            } catch (IllegalArgumentException iae) {
                                // continue
                            }

                            if (mRebootActionItem == null) {
                                // search for text
                                try {
                                    Field f = XposedHelpers.findField(o.getClass(), "mMessageResId");
                                    String resName = res.getResourceEntryName((Integer) f.get(o)).toLowerCase(Locale.US);
                                    if (DEBUG) log("Text resName = " + resName);
                                    if (resName.contains("reboot") || resName.contains("restart")) {
                                        mRebootActionItem = o;
                                        break;
                                    }
                                } catch (NoSuchFieldError nfe) {
                                    // continue
                                } catch (Resources.NotFoundException resnfe) { 
                                    // continue
                                } catch (IllegalArgumentException iae) {
                                    // continue
                                }
                            }
                        }

                        if (mRebootActionItem == null) {
                            if (DEBUG) log("Existing Reboot action item NOT found! Creating new RebootAction item");
                            mRebootActionItemStockExists = false;
                            mRebootActionItem = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass }, 
                                    new RebootAction());
                        } else {
                            if (DEBUG) log("Existing Reboot action item found!");
                            mRebootActionItemStockExists = true;
                        }
                    }

                    // Add/hook reboot action if enabled
                    if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_POWEROFF_ADVANCED, false)) {
                        if (mRebootActionItemStockExists) {
                            mRebootActionHook = XposedHelpers.findAndHookMethod(mRebootActionItem.getClass(), 
                                    "onPress", new XC_MethodReplacement () {
                                @Override
                                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                    RebootAction.showRebootDialog(mContext);
                                    return null;
                                }
                            });
                            mRebootActionShowHook = XposedHelpers.findAndHookMethod(mRebootActionItem.getClass(),
                                    "showDuringKeyguard", new XC_MethodReplacement() {
                                @Override
                                protected Object replaceHookedMethod(MethodHookParam param)throws Throwable {
                                    return mRebootAllowOnLockscreen;
                                }
                            });
                        } else {
                            // add to the second position
                            mItems.add(index, mRebootActionItem);
                        }
                        index++;
                    } else if (mRebootActionItemStockExists) {
                        index++;
                    }

                    // Add screenshot action if enabled
                    if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_POWERMENU_SCREENSHOT, false)) {
                        if (mScreenshotAction == null) {
                            mScreenshotAction = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass },
                                new ScreenshotAction());
                            if (DEBUG) log("mScreenshotAction created");
                        }
                        mItems.add(index++, mScreenshotAction);
                    }

                    // Add screenrecord action if enabled
                    if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_POWERMENU_SCREENRECORD, false)) {
                        if (mScreenrecordAction == null) {
                            mScreenrecordAction = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass },
                                new ScreenrecordAction());
                            if (DEBUG) log("mScreenrecordAction created");
                        }
                        mItems.add(index++, mScreenrecordAction);
                    }

                    // Add Expanded Desktop action if enabled
                    if (ExpandedDesktopAction.isExpandedDesktopEnabled(prefs) &&
                            prefs.getBoolean(GravityBoxSettings.PREF_KEY_POWERMENU_EXPANDED_DESKTOP, true)) {
                        if (mExpandedDesktopAction == null) {
                            mExpandedDesktopAction = Proxy.newProxyInstance(classLoader, 
                                    new Class<?>[] { actionClass },
                                        new ExpandedDesktopAction());
                            if (DEBUG) log("mExpandedDesktopAction created");
                        }
                        mItems.add(index++, mExpandedDesktopAction);
                    }

                    mAdapter.notifyDataSetChanged();
                }
            });

            XC_MethodHook showDialogHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_POWERMENU_DISABLE_ON_LOCKSCREEN, false)) {
                        boolean locked = (Boolean) param.args[0];
                        if (!locked) {
                            // double-check using keyguard manager
                            try {
                                Context context = (Context) XposedHelpers.getObjectField(
                                        param.thisObject, "mContext");
                                KeyguardManager km = (KeyguardManager) context.getSystemService(
                                        Context.KEYGUARD_SERVICE);
                                locked = km.isKeyguardLocked();
                            } catch (Throwable t) { }
                        }

                        if (locked) {
                            Dialog d = (Dialog) XposedHelpers.getObjectField(param.thisObject, "mDialog");
                            if (d == null) {
                                XposedHelpers.callMethod(param.thisObject, "createDialog");
                            }
                            param.setResult(null);
                        }
                    }
                }
            };
            if (Utils.isParanoidRom()) {
                XposedHelpers.findAndHookMethod(globalActionsClass, "showDialog", 
                        boolean.class, boolean.class, boolean.class, showDialogHook);
            } else {
                XposedHelpers.findAndHookMethod(globalActionsClass, "showDialog", 
                        boolean.class, boolean.class, showDialogHook);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static class RebootAction implements InvocationHandler {
        private Context mContext;

        public RebootAction() {
        }

        public static void showRebootDialog(final Context context) {
            if (context == null) {
                if (DEBUG) log("Context is null - aborting");
                return;
            }

            try {
                if (DEBUG) log("about to build reboot dialog");

                List<IIconListAdapterItem> list = 
                        new ArrayList<IIconListAdapterItem>(mRebootItemList);
                if (!mAllowSoftReboot) {
                    list.remove(1);
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle(mRebootStr)
                    .setAdapter(new IconListAdapter(context, list), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            if (DEBUG) log("onClick() item = " + which);
                            handleReboot(context, mRebootStr, 
                                    (which > 0 && !mAllowSoftReboot) ? (which+1) : which);
                        }
                    })
                    .setNegativeButton(android.R.string.no,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                AlertDialog dialog = builder.create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
                dialog.show();
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }

        private static void doReboot(Context context, int mode) {
            final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (mode == 0) {
                pm.reboot(null);
            } else if (mode == 1) {
                Utils.performSoftReboot();
            } else if (mode == 2) {
                replaceRecoveryMessage();
                pm.reboot("recovery");
            } else if (mode == 3) {
                pm.reboot("bootloader");
            }
        }

        private static void replaceRecoveryMessage() {
            try {
                Resources res = XResources.getSystem();
                XResources.setSystemWideReplacement(
                        res.getIdentifier("reboot_to_reset_title", "string", "android"),
                        mRecoveryStr);
            } catch (Throwable t) { /* ignore */ }
        }

        private static void handleReboot(final Context context, String caption, final int mode) {
            try {
                if (!mRebootConfirmRequired) {
                    doReboot(context, mode);
                } else {
                    String message = mRebootConfirmStr;
                    if (mode == 2) {
                        message = mRebootConfirmRecoveryStr;
                    } else if (mode == 3) {
                        message = mRebootConfirmBootloaderStr;
                    }
    
                    AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setTitle(caption)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
    
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                doReboot(context, mode);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                    AlertDialog dialog = builder.create();
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
                    dialog.show();
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if (methodName.equals("create")) {
                mContext = (Context) args[0];
                Resources res = mContext.getResources();
                LayoutInflater li = (LayoutInflater) args[3];
                int layoutId = res.getIdentifier(
                        "global_actions_item", "layout", "android");
                View v = li.inflate(layoutId, (ViewGroup) args[2], false);

                ImageView icon = (ImageView) v.findViewById(res.getIdentifier(
                        "icon", "id", "android"));
                icon.setImageDrawable(mRebootIcon);

                TextView messageView = (TextView) v.findViewById(res.getIdentifier(
                        "message", "id", "android"));
                messageView.setText(mRebootStr);

                TextView statusView = (TextView) v.findViewById(res.getIdentifier(
                        "status", "id", "android"));
                statusView.setVisibility(View.GONE);

                return v;
            } else if (methodName.equals("onPress")) {
                showRebootDialog(mContext);
                return null;
            } else if (methodName.equals("onLongPress")) {
                handleReboot(mContext, mRebootStr, 0);
                return true;
            } else if (methodName.equals("showDuringKeyguard")) {
                return mRebootAllowOnLockscreen;
            } else if (methodName.equals("showBeforeProvisioning")) {
                return true;
            } else if (methodName.equals("isEnabled")) {
                return true;
            } else if (methodName.equals("showConditional")) {
                return true;
            } else if (methodName.equals("getLabelForAccessibility")) {
                return null;
            } else {
                log("RebootAction: Unhandled invocation method: " + methodName);
                return null;
            }
        }
    }

    private static class ExpandedDesktopAction implements InvocationHandler {
        private Context mContext;
        private TextView mStatus;
        private Handler mHandler;

        public ExpandedDesktopAction() {
        }

        public static boolean isExpandedDesktopEnabled(XSharedPreferences prefs) {
            int edMode = GravityBoxSettings.ED_DISABLED;
            try {
                edMode = Integer.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_EXPANDED_DESKTOP, "0"));
            } catch(NumberFormatException nfe) {
                log("Invalid value for PREF_KEY_EXPANDED_DESKTOP preference");
            }
            return (edMode != GravityBoxSettings.ED_DISABLED);
        }

        public static boolean isExpandedDesktopOn(Context context) {
            return (Settings.Global.getInt(context.getContentResolver(),
                    ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE, 0) == 1);
        }

        private void updateStatus() {
            mStatus.setText(isExpandedDesktopOn(mContext) ? 
                    mExpandedDesktopOnStr : mExpandedDesktopOffStr);
        }

        private void toggleStatus() {
            try {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Settings.Global.putInt(mContext.getContentResolver(),
                                ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE,
                                isExpandedDesktopOn(mContext) ? 0 : 1);
                        updateStatus();
                    }
                }, 200);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        };

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if (methodName.equals("create")) {
                mContext = (Context) args[0];
                mHandler = new Handler();

                Resources res = mContext.getResources();
                LayoutInflater li = (LayoutInflater) args[3];
                int layoutId = res.getIdentifier(
                        "global_actions_item", "layout", "android");
                View v = li.inflate(layoutId, (ViewGroup) args[2], false);

                ImageView icon = (ImageView) v.findViewById(res.getIdentifier(
                        "icon", "id", "android"));
                icon.setImageDrawable(mExpandedDesktopIcon);

                TextView messageView = (TextView) v.findViewById(res.getIdentifier(
                        "message", "id", "android"));
                messageView.setText(mExpandedDesktopStr);

                mStatus = (TextView) v.findViewById(res.getIdentifier(
                        "status", "id", "android"));
                mStatus.setVisibility(View.VISIBLE);
                updateStatus();

                return v;
            } else if (methodName.equals("onPress")) {
                toggleStatus();
                return null;
            } else if (methodName.equals("onLongPress")) {
                return false;
            } else if (methodName.equals("showDuringKeyguard")) {
                return true;
            } else if (methodName.equals("showBeforeProvisioning")) {
                return true;
            } else if (methodName.equals("isEnabled")) {
                return true;
            } else if (methodName.equals("showConditional")) {
                return true;
            } else if (methodName.equals("getLabelForAccessibility")) {
                return null;
            } else {
                log("ExpandedDesktopAction: Unhandled invocation method: " + methodName);
                return null;
            }
        }
    }

    private static class ScreenshotAction implements InvocationHandler {
        private Context mContext;

        private void takeScreenshot() {
            Intent intent = new Intent(ModHwKeys.ACTION_SCREENSHOT);
            intent.putExtra(ModHwKeys.EXTRA_SCREENSHOT_DELAY_MS, 300L);
            mContext.sendBroadcast(intent);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if (methodName.equals("create")) {
                mContext = (Context) args[0];
                Resources res = mContext.getResources();
                LayoutInflater li = (LayoutInflater) args[3];
                int layoutId = res.getIdentifier(
                        "global_actions_item", "layout", "android");
                View v = li.inflate(layoutId, (ViewGroup) args[2], false);

                ImageView icon = (ImageView) v.findViewById(res.getIdentifier(
                        "icon", "id", "android"));
                icon.setImageDrawable(mScreenshotIcon);

                TextView messageView = (TextView) v.findViewById(res.getIdentifier(
                        "message", "id", "android"));
                messageView.setText(mScreenshotStr);

                TextView statusView = (TextView) v.findViewById(res.getIdentifier(
                        "status", "id", "android"));
                statusView.setVisibility(View.GONE);

                return v;
            } else if (methodName.equals("onPress")) {
                takeScreenshot();
                return null;
            } else if (methodName.equals("onLongPress")) {
                return true;
            } else if (methodName.equals("showDuringKeyguard")) {
                return true;
            } else if (methodName.equals("showBeforeProvisioning")) {
                return true;
            } else if (methodName.equals("isEnabled")) {
                return true;
            } else if (methodName.equals("showConditional")) {
                return true;
            } else if (methodName.equals("getLabelForAccessibility")) {
                return null;
            } else {
                log("ScreenshotAction: Unhandled invocation method: " + methodName);
                return null;
            }
        }
    }

    private static class ScreenrecordAction implements InvocationHandler {
        private Context mContext;

        public ScreenrecordAction() {
        }

        private void takeScreenrecord() {
            try {
                Context gbContext = Utils.getGbContext(mContext);
                Intent intent = new Intent(gbContext, ScreenRecordingService.class);
                intent.setAction(ScreenRecordingService.ACTION_SCREEN_RECORDING_START);
                gbContext.startService(intent);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if (methodName.equals("create")) {
                mContext = (Context) args[0];
                Resources res = mContext.getResources();
                LayoutInflater li = (LayoutInflater) args[3];
                int layoutId = res.getIdentifier(
                        "global_actions_item", "layout", "android");
                View v = li.inflate(layoutId, (ViewGroup) args[2], false);

                ImageView icon = (ImageView) v.findViewById(res.getIdentifier(
                        "icon", "id", "android"));
                icon.setImageDrawable(mScreenrecordIcon);

                TextView messageView = (TextView) v.findViewById(res.getIdentifier(
                        "message", "id", "android"));
                messageView.setText(mScreenrecordStr);

                TextView statusView = (TextView) v.findViewById(res.getIdentifier(
                        "status", "id", "android"));
                statusView.setVisibility(View.GONE);

                return v;
            } else if (methodName.equals("onPress")) {
                takeScreenrecord();
                return null;
            } else if (methodName.equals("onLongPress")) {
                return true;
            } else if (methodName.equals("showDuringKeyguard")) {
                return true;
            } else if (methodName.equals("showBeforeProvisioning")) {
                return true;
            } else if (methodName.equals("isEnabled")) {
                return true;
            } else if (methodName.equals("showConditional")) {
                return true;
            } else if (methodName.equals("getLabelForAccessibility")) {
                return null;
            } else {
                log("ScreenrecordAction: Unhandled invocation method: " + methodName);
                return null;
            }
        }
    }
}
