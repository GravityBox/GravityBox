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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.ceco.kitkat.gravitybox.R;
import com.ceco.kitkat.gravitybox.ledcontrol.LedControlActivity;
import com.ceco.kitkat.gravitybox.ledcontrol.LedSettings;
import com.ceco.kitkat.gravitybox.preference.AppPickerPreference;
import com.ceco.kitkat.gravitybox.preference.AutoBrightnessDialogPreference;
import com.ceco.kitkat.gravitybox.preference.SeekBarPreference;
import com.ceco.kitkat.gravitybox.quicksettings.TileOrderActivity;
import com.ceco.kitkat.gravitybox.webserviceclient.RequestParams;
import com.ceco.kitkat.gravitybox.webserviceclient.TransactionResult;
import com.ceco.kitkat.gravitybox.webserviceclient.TransactionResult.TransactionStatus;
import com.ceco.kitkat.gravitybox.webserviceclient.WebServiceClient;
import com.ceco.kitkat.gravitybox.webserviceclient.WebServiceClient.WebServiceTaskListener;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class GravityBoxSettings extends Activity implements GravityBoxResultReceiver.Receiver {
    public static final String PREF_KEY_QUICK_SETTINGS_ENABLE = "pref_qs_management_enable";
    public static final String PREF_KEY_QUICK_SETTINGS = "pref_quick_settings2";
    public static final String PREF_KEY_QUICK_SETTINGS_TILE_ORDER = "pref_qs_tile_order";
    public static final String PREF_KEY_QUICK_SETTINGS_TILES_PER_ROW = "pref_qs_tiles_per_row";
    public static final String PREF_KEY_QUICK_SETTINGS_TILE_LABEL_STYLE = "pref_qs_tile_label_style";
    public static final String PREF_KEY_QUICK_SETTINGS_HIDE_ON_CHANGE = "pref_qs_hide_on_change";
    public static final String PREF_KEY_QUICK_SETTINGS_AUTOSWITCH = "pref_auto_switch_qs2";
    public static final String PREF_KEY_QUICK_PULLDOWN = "pref_quick_pulldown";
    public static final String PREF_KEY_QUICK_PULLDOWN_SIZE = "pref_quick_pulldown_size";
    public static final int QUICK_PULLDOWN_OFF = 0;
    public static final int QUICK_PULLDOWN_RIGHT = 1;
    public static final int QUICK_PULLDOWN_LEFT = 2;

    public static final String PREF_KEY_BATTERY_STYLE = "pref_battery_style";
    public static final String PREF_KEY_BATTERY_PERCENT_TEXT = "pref_battery_percent_text";
    public static final String PREF_KEY_BATTERY_PERCENT_TEXT_SIZE = "pref_battery_percent_text_size";
    public static final String PREF_KEY_BATTERY_PERCENT_TEXT_STYLE = "pref_battery_percent_text_style";
    public static final String PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING = "battery_percent_text_charging";
    public static final String PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING_COLOR = "pref_battery_percent_text_charging_color";
    public static final int BATTERY_STYLE_STOCK = 1;
    public static final int BATTERY_STYLE_CIRCLE = 2;
    public static final int BATTERY_STYLE_CIRCLE_PERCENT = 3;
    public static final int BATTERY_STYLE_KITKAT = 4;
    public static final int BATTERY_STYLE_KITKAT_PERCENT = 5;
    public static final int BATTERY_STYLE_NONE = 0;

    public static final String PREF_KEY_LOW_BATTERY_WARNING_POLICY = "pref_low_battery_warning_policy";
    public static final int BATTERY_WARNING_POPUP = 1;
    public static final int BATTERY_WARNING_SOUND = 2;
    public static final String PREF_KEY_BATTERY_CHARGED_SOUND = "pref_battery_charged_sound";
    public static final String PREF_KEY_CHARGER_PLUGGED_SOUND = "pref_charger_plugged_sound";
    public static final String ACTION_PREF_BATTERY_CHARGED_SOUND_CHANGED = 
            "gravitybox.intent.action.BATTERY_CHARGED_SOUND_CHANGED";
    public static final String EXTRA_BATTERY_CHARGED_SOUND = "batteryChargedSound";
    public static final String EXTRA_CHARGER_PLUGGED_SOUND = "chargerPluggedSound";

    public static final String PREF_KEY_DISABLE_DATA_NETWORK_TYPE_ICONS = "pref_disable_data_network_type_icons";
    public static final String ACTION_DISABLE_DATA_NETWORK_TYPE_ICONS_CHANGED = "gravitybox.intent.action.DISABLE_DATA_NETWORK_TYPE_ICONS_CHANGED";
    public static final String EXTRA_DATA_NETWORK_TYPE_ICONS_DISABLED = "dataNetworkTypeIconsDisabled";
    public static final String PREF_KEY_DISABLE_ROAMING_INDICATORS = "pref_disable_roaming_indicators";
    public static final String ACTION_DISABLE_ROAMING_INDICATORS_CHANGED = "gravitybox.intent.action.DISABLE_ROAMING_INDICATORS_CHANGED";
    public static final String EXTRA_INDICATORS_DISABLED = "indicatorsDisabled";
    public static final String PREF_KEY_POWEROFF_ADVANCED = "pref_poweroff_advanced";
    public static final String PREF_KEY_REBOOT_ALLOW_ON_LOCKSCREEN = "pref_reboot_allow_on_lockscreen";
    public static final String PREF_KEY_REBOOT_CONFIRM_REQUIRED = "pref_reboot_confirm_required";
    public static final String PREF_KEY_POWERMENU_SCREENSHOT = "pref_powermenu_screenshot";
    public static final String PREF_KEY_POWERMENU_SCREENRECORD = "pref_powermenu_screenrecord";
    public static final String PREF_KEY_POWERMENU_DISABLE_ON_LOCKSCREEN = "pref_powermenu_disable_on_lockscreen";

    public static final String PREF_KEY_VOL_KEY_CURSOR_CONTROL = "pref_vol_key_cursor_control";
    public static final int VOL_KEY_CURSOR_CONTROL_OFF = 0;
    public static final int VOL_KEY_CURSOR_CONTROL_ON = 1;
    public static final int VOL_KEY_CURSOR_CONTROL_ON_REVERSE = 2;

    public static final String PREF_KEY_RECENTS_CLEAR_ALL = "pref_recents_clear_all2";
    public static final String PREF_KEY_CLEAR_RECENTS_MODE = "pref_clear_recents_mode";
    public static final String PREF_KEY_RAMBAR = "pref_rambar";
    public static final String PREF_KEY_RECENTS_CLEAR_MARGIN_TOP = "pref_recent_clear_margin_top";
    public static final String PREF_KEY_RECENTS_CLEAR_MARGIN_BOTTOM = "pref_recent_clear_margin_bottom";
    public static final int RECENT_CLEAR_OFF = 0;
    public static final int RECENT_CLEAR_TOP_LEFT = 51;
    public static final int RECENT_CLEAR_TOP_RIGHT = 53;
    public static final int RECENT_CLEAR_BOTTOM_LEFT = 83;
    public static final int RECENT_CLEAR_BOTTOM_RIGHT = 85;
    public static final int RECENT_CLEAR_NAVIGATION_BAR = 1;

    public static final String PREF_CAT_KEY_PHONE = "pref_cat_phone";
    public static final String PREF_KEY_CALLER_FULLSCREEN_PHOTO = "pref_caller_fullscreen_photo2";
    public static final String PREF_KEY_CALLER_UNKNOWN_PHOTO_ENABLE = "pref_caller_unknown_photo_enable";
    public static final String PREF_KEY_CALLER_UNKNOWN_PHOTO = "pref_caller_unknown_photo";
    public static final String PREF_KEY_DIALER_SHOW_DIALPAD = "pref_dialer_show_dialpad";
    public static final String PREF_KEY_PHONE_NONINTRUSIVE_INCOMING_CALL = "pref_phone_nonintrusive_incoming_call";
    public static final String PREF_KEY_NATIONAL_ROAMING = "pref_national_roaming";
    public static final String PREF_CAT_KEY_STATUSBAR = "pref_cat_statusbar";
    public static final String PREF_CAT_KEY_STATUSBAR_QS = "pref_cat_statusbar_qs";
    public static final String PREF_CAT_KEY_QS_TILE_SETTINGS = "pref_cat_qs_tile_settings";
    public static final String PREF_CAT_KEY_QS_NM_TILE_SETTINGS = "pref_cat_qs_nm_tile_settings";
    public static final String PREF_CAT_KEY_STATUSBAR_COLORS = "pref_cat_statusbar_colors";
    public static final String PREF_KEY_STATUSBAR_BGCOLOR = "pref_statusbar_bgcolor2";
    public static final String PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE = "pref_statusbar_icon_color_enable";
    public static final String PREF_KEY_STATUSBAR_ICON_COLOR = "pref_statusbar_icon_color";
    public static final String PREF_KEY_STATUS_ICON_STYLE = "pref_status_icon_style";
    public static final String PREF_KEY_STATUSBAR_ICON_COLOR_SECONDARY = "pref_statusbar_icon_color_secondary";
    public static final String PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR = "pref_signal_cluster_data_activity_color";
    public static final String PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR_SECONDARY = 
            "pref_signal_cluster_data_activity_color_secondary";
    public static final String PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE = "pref_statusbar_signal_color_mode";
    public static final String PREF_KEY_STATUSBAR_CENTER_CLOCK = "pref_statusbar_center_clock";
    public static final String PREF_KEY_STATUSBAR_CLOCK_DOW = "pref_statusbar_clock_dow2";
    public static final int DOW_DISABLED = 0;
    public static final int DOW_STANDARD = 1;
    public static final int DOW_LOWERCASE = 2;
    public static final int DOW_UPPERCASE = 3;
    public static final String PREF_KEY_STATUSBAR_CLOCK_DOW_SIZE = "pref_sb_clock_dow_size";
    public static final String PREF_KEY_STATUSBAR_CLOCK_AMPM_HIDE = "pref_clock_ampm_hide";
    public static final String PREF_KEY_STATUSBAR_CLOCK_AMPM_SIZE = "pref_sb_clock_ampm_size";
    public static final String PREF_KEY_STATUSBAR_CLOCK_HIDE = "pref_clock_hide";
    public static final String PREF_KEY_STATUSBAR_CLOCK_LINK = "pref_clock_link_app";
    public static final String PREF_KEY_STATUSBAR_CLOCK_LONGPRESS_LINK = "pref_clock_longpress_link";
    public static final String PREF_KEY_STATUSBAR_CLOCK_MASTER_SWITCH = "pref_sb_clock_masterswitch";
    public static final String PREF_KEY_ALARM_ICON_HIDE = "pref_alarm_icon_hide";
    public static final String PREF_CAT_KEY_TRANSPARENCY_MANAGER = "pref_cat_transparency_manager";
    public static final String PREF_KEY_TM_MODE = "pref_tm_mode";
    public static final String PREF_KEY_TM_STATUSBAR_LAUNCHER = "pref_tm_statusbar_launcher";
    public static final String PREF_KEY_TM_STATUSBAR_LOCKSCREEN = "pref_tm_statusbar_lockscreen";
    public static final String PREF_KEY_TM_NAVBAR_LAUNCHER = "pref_tm_navbar_launcher";
    public static final String PREF_KEY_TM_NAVBAR_LOCKSCREEN = "pref_tm_navbar_lockscreen";
    public static final String PREF_KEY_ABOUT_GRAVITYBOX = "pref_about_gb";
    public static final String PREF_KEY_ABOUT_GPLUS = "pref_about_gplus";
    public static final String PREF_KEY_ABOUT_XPOSED = "pref_about_xposed";
    public static final String PREF_KEY_ABOUT_DONATE = "pref_about_donate";
    public static final String PREF_KEY_CRT_OFF_EFFECT = "pref_crt_off_effect2";
    public static final String PREF_KEY_UNPLUG_TURNS_ON_SCREEN = "pref_unplug_turns_on_screen";
    public static final String PREF_KEY_ENGINEERING_MODE = "pref_engineering_mode";
    public static final String APP_MESSAGING = "com.android.mms";
    public static final String APP_STOCK_LAUNCHER = "com.android.launcher3";
    public static final String APP_GOOGLE_HOME = "com.google.android.launcher";
    public static final String APP_GOOGLE_NOW = "com.google.android.googlequicksearchbox";
    public static final String APP_ENGINEERING_MODE = "com.mediatek.engineermode";
    public static final String APP_ENGINEERING_MODE_CLASS = "com.mediatek.engineermode.EngineerMode";
    public static final String PREF_KEY_DUAL_SIM_RINGER = "pref_dual_sim_ringer";
    public static final String APP_DUAL_SIM_RINGER = "dualsim.ringer";
    public static final String APP_DUAL_SIM_RINGER_CLASS = "dualsim.ringer.main";
    public static final String ACTION_PREF_TELEPHONY_CHANGED = "gravity.intent.action.TELEPHONY_CHANGED";
    public static final String EXTRA_TELEPHONY_NATIONAL_ROAMING = "nationalRoaming";

    public static final String PREF_CAT_KEY_LOCKSCREEN = "pref_cat_lockscreen";
    public static final String PREF_CAT_KEY_LOCKSCREEN_BACKGROUND = "pref_cat_lockscreen_background";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND = "pref_lockscreen_background";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR = "pref_lockscreen_bg_color";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND_IMAGE = "pref_lockscreen_bg_image";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND_OPACITY = "pref_lockscreen_bg_opacity";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND_BLUR_EFFECT = "pref_lockscreen_bg_blur_effect";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND_BLUR_INTENSITY = "pref_lockscreen_bg_blur_intensity";
    public static final String LOCKSCREEN_BG_DEFAULT = "default";
    public static final String LOCKSCREEN_BG_COLOR = "color";
    public static final String LOCKSCREEN_BG_IMAGE = "image";
    public static final String LOCKSCREEN_BG_LAST_SCREEN = "last_screen";
    public static final String ACTION_PREF_LOCKSCREEN_BG_CHANGED = "gravitybox.intent.action.LOCKSCREEN_BG_CHANGED";
    public static final String EXTRA_LOCKSCREEN_BG = "lockscreenBg";

    public static final String PREF_CAT_KEY_LOCKSCREEN_SLIDING_CHALLENGE = "pref_cat_lockscreen_sliding_challenge";
    public static final String PREF_CAT_KEY_LOCKSCREEN_OTHER = "pref_cat_lockscreen_other";
    public static final String PREF_KEY_LOCKSCREEN_BATTERY_ARC = "pref_lockscreen_battery_arc";
    public static final String PREF_KEY_LOCKSCREEN_RING_TORCH = "pref_lockscreen_ring_torch";
    public static final String PREF_KEY_LOCKSCREEN_MAXIMIZE_WIDGETS = "pref_lockscreen_maximize_widgets";
    public static final String PREF_KEY_LOCKSCREEN_WIDGET_LIMIT_DISABLE = "pref_lockscreen_widget_limit_disable";
    public static final String PREF_KEY_LOCKSCREEN_ROTATION = "pref_lockscreen_rotation";
    public static final String PREF_KEY_LOCKSCREEN_SHOW_PATTERN_ERROR = "pref_lockscreen_show_pattern_error";
    public static final String PREF_KEY_LOCKSCREEN_MENU_KEY = "pref_lockscreen_menu_key";
    public static final String PREF_KEY_LOCKSCREEN_QUICK_UNLOCK = "pref_lockscreen_quick_unlock";
    public static final String PREF_KEY_LOCKSCREEN_STATUSBAR_CLOCK = "pref_lockscreen_statusbar_clock";
    public static final String PREF_KEY_LOCKSCREEN_CARRIER_TEXT = "pref_lockscreen_carrier_text";
    public static final String PREF_KEY_LOCKSCREEN_SLIDE_BEFORE_UNLOCK = "pref_lockscreen_slide_before_unlock";
    public static final String PREF_KEY_LOCKSCREEN_RING_DT2S = "pref_lockscreen_ring_dt2s";
    public static final String PREF_KEY_STATUSBAR_LOCK_POLICY = "pref_statusbar_lock_policy";
    public static final int SBL_POLICY_DEFAULT = 0;
    public static final int SBL_POLICY_UNLOCKED = 1;
    public static final int SBL_POLICY_LOCKED = 2;

    public static final String PREF_KEY_FLASHING_LED_DISABLE = "pref_flashing_led_disable";
    public static final String PREF_KEY_CHARGING_LED_DISABLE = "pref_charging_led_disable";

    public static final String PREF_CAT_KEY_DISPLAY = "pref_cat_display";
    public static final String PREF_KEY_EXPANDED_DESKTOP = "pref_expanded_desktop";
    public static final int ED_DISABLED = 0;
    public static final int ED_HIDE_NAVBAR = 1;
    public static final int ED_SEMI_IMMERSIVE = 2;
    public static final int ED_IMMERSIVE = 3;
    public static final int ED_IMMERSIVE_STATUSBAR = 4;
    public static final int ED_IMMERSIVE_NAVBAR = 5;
    public static final String ACTION_PREF_EXPANDED_DESKTOP_MODE_CHANGED = "gravitybox.intent.action.EXPANDED_DESKTOP_MODE_CHANGED";
    public static final String EXTRA_ED_MODE = "expandedDesktopMode";
    public static final String PREF_CAT_KEY_BRIGHTNESS = "pref_cat_brightness";
    public static final String PREF_KEY_BRIGHTNESS_MASTER_SWITCH = "pref_brightness_master_switch";
    public static final String PREF_KEY_BRIGHTNESS_MIN = "pref_brightness_min2";
    public static final String PREF_KEY_SCREEN_DIM_LEVEL = "pref_screen_dim_level";
    public static final String PREF_KEY_AUTOBRIGHTNESS = "pref_autobrightness";
    public static final String PREF_KEY_HOLO_BG_SOLID_BLACK = "pref_holo_bg_solid_black";
    public static final String PREF_KEY_HOLO_BG_DITHER = "pref_holo_bg_dither";
    public static final String PREF_KEY_TRANSLUCENT_DECOR = "pref_translucent_decor";

    public static final String PREF_CAT_KEY_MEDIA = "pref_cat_media";
    public static final String PREF_KEY_VOL_MUSIC_CONTROLS = "pref_vol_music_controls";
    public static final String PREF_KEY_MUSIC_VOLUME_STEPS = "pref_music_volume_steps";
    public static final String PREF_KEY_VOL_FORCE_MUSIC_CONTROL = "pref_vol_force_music_control";
    public static final String PREF_KEY_SAFE_MEDIA_VOLUME = "pref_safe_media_volume";
    public static final String PREF_KEY_VOL_SWAP_KEYS = "pref_vol_swap_keys";
    public static final String PREF_KEY_VOLUME_PANEL_EXPANDABLE = "pref_volume_panel_expandable";
    public static final String PREF_KEY_VOLUME_PANEL_FULLY_EXPANDABLE = "pref_volume_panel_expand_fully";
    public static final String PREF_KEY_VOLUME_PANEL_AUTOEXPAND = "pref_volume_panel_autoexpand";
    public static final String PREF_KEY_VOLUME_ADJUST_MUTE = "pref_volume_adjust_mute";
    public static final String PREF_KEY_VOLUME_ADJUST_VIBRATE_MUTE = "pref_volume_adjust_vibrate_mute";
    public static final String PREF_KEY_VOLUME_PANEL_TIMEOUT = "pref_volume_panel_timeout";
    public static final String PREF_KEY_VOLUME_PANEL_TRANSPARENCY = "pref_volume_panel_transparency";
    public static final String ACTION_PREF_VOLUME_PANEL_MODE_CHANGED = "gravitybox.intent.action.VOLUME_PANEL_MODE_CHANGED";
    public static final String EXTRA_EXPANDABLE = "expandable";
    public static final String EXTRA_EXPANDABLE_FULLY = "expandable_fully";
    public static final String EXTRA_AUTOEXPAND = "autoExpand";
    public static final String EXTRA_MUTED = "muted";
    public static final String EXTRA_VIBRATE_MUTED = "vibrate_muted";
    public static final String EXTRA_TIMEOUT = "timeout";
    public static final String EXTRA_TRANSPARENCY = "volPanelTransparency";
    public static final String PREF_KEY_LINK_VOLUMES = "pref_link_volumes";
    public static final String ACTION_PREF_LINK_VOLUMES_CHANGED = "gravitybox.intent.action.LINK_VOLUMES_CHANGED";
    public static final String EXTRA_LINKED = "linked";
    public static final String ACTION_PREF_VOL_FORCE_MUSIC_CONTROL_CHANGED = 
            "gravitybox.intent.action.VOL_FORCE_MUSIC_CONTROL_CHANGED";
    public static final String EXTRA_VOL_FORCE_MUSIC_CONTROL = "volForceMusicControl";
    public static final String ACTION_PREF_VOL_SWAP_KEYS_CHANGED = 
            "gravitybox.intent.action.VOL_SWAP_KEYS_CHANGED";
    public static final String EXTRA_VOL_SWAP_KEYS = "volKeysSwap";

    public static final String PREF_CAT_HWKEY_ACTIONS = "pref_cat_hwkey_actions";
    public static final String PREF_CAT_HWKEY_MENU = "pref_cat_hwkey_menu";
    public static final String PREF_KEY_HWKEY_MENU_LONGPRESS = "pref_hwkey_menu_longpress";
    public static final String PREF_KEY_HWKEY_MENU_DOUBLETAP = "pref_hwkey_menu_doubletap";
    public static final String PREF_CAT_HWKEY_HOME = "pref_cat_hwkey_home";
    public static final String PREF_KEY_HWKEY_HOME_LONGPRESS = "pref_hwkey_home_longpress";
    public static final String PREF_KEY_HWKEY_HOME_DOUBLETAP_DISABLE = "pref_hwkey_home_doubletap_disable";
    public static final String PREF_KEY_HWKEY_HOME_DOUBLETAP = "pref_hwkey_home_doubletap";
    public static final String PREF_KEY_HWKEY_HOME_LONGPRESS_KEYGUARD = "pref_hwkey_home_longpress_keyguard";
    public static final String PREF_CAT_HWKEY_BACK = "pref_cat_hwkey_back";
    public static final String PREF_KEY_HWKEY_BACK_LONGPRESS = "pref_hwkey_back_longpress";
    public static final String PREF_KEY_HWKEY_BACK_DOUBLETAP = "pref_hwkey_back_doubletap";
    public static final String PREF_CAT_HWKEY_RECENTS = "pref_cat_hwkey_recents";
    public static final String PREF_KEY_HWKEY_RECENTS_SINGLETAP = "pref_hwkey_recents_singletap";
    public static final String PREF_KEY_HWKEY_RECENTS_LONGPRESS = "pref_hwkey_recents_longpress";
    public static final String PREF_KEY_HWKEY_CUSTOM_APP = "pref_hwkey_custom_app";
    public static final String PREF_KEY_HWKEY_CUSTOM_APP2 = "pref_hwkey_custom_app2";
    public static final String PREF_KEY_HWKEY_DOUBLETAP_SPEED = "pref_hwkey_doubletap_speed";
    public static final String PREF_KEY_HWKEY_KILL_DELAY = "pref_hwkey_kill_delay";
    public static final String PREF_CAT_HWKEY_VOLUME = "pref_cat_hwkey_volume";
    public static final String PREF_KEY_VOLUME_ROCKER_WAKE = "pref_volume_rocker_wake";
    public static final String PREF_KEY_HWKEY_LOCKSCREEN_TORCH = "pref_hwkey_lockscreen_torch";
    public static final String PREF_CAT_KEY_HWKEY_ACTIONS_OTHERS = "pref_cat_hwkey_actions_others";
    public static final String PREF_KEY_VK_VIBRATE_PATTERN = "pref_virtual_key_vibrate_pattern";
    public static final int HWKEY_ACTION_DEFAULT = 0;
    public static final int HWKEY_ACTION_SEARCH = 1;
    public static final int HWKEY_ACTION_VOICE_SEARCH = 2;
    public static final int HWKEY_ACTION_PREV_APP = 3;
    public static final int HWKEY_ACTION_KILL = 4;
    public static final int HWKEY_ACTION_SLEEP = 5;
    public static final int HWKEY_ACTION_RECENT_APPS = 6;
    public static final int HWKEY_ACTION_CUSTOM_APP = 7;
    public static final int HWKEY_ACTION_CUSTOM_APP2 = 8;
    public static final int HWKEY_ACTION_MENU = 9;
    public static final int HWKEY_ACTION_EXPANDED_DESKTOP = 10;
    public static final int HWKEY_ACTION_TORCH = 11;
    public static final int HWKEY_ACTION_APP_LAUNCHER = 12;
    public static final int HWKEY_ACTION_HOME = 13;
    public static final int HWKEY_ACTION_BACK = 14;
    public static final int HWKEY_ACTION_SCREEN_RECORDING = 15;
    public static final int HWKEY_ACTION_AUTO_ROTATION = 16;
    public static final int HWKEY_ACTION_SHOW_POWER_MENU = 17;
    public static final int HWKEY_ACTION_EXPAND_NOTIFICATIONS = 18;
    public static final int HWKEY_ACTION_EXPAND_QUICKSETTINGS = 19;
    public static final int HWKEY_ACTION_SCREENSHOT = 20;
    public static final int HWKEY_ACTION_VOLUME_PANEL = 21;
    public static final int HWKEY_ACTION_LAUNCHER_DRAWER = 22;
    public static final int HWKEY_ACTION_BRIGHTNESS_DIALOG = 23;
    public static final int HWKEY_ACTION_CLEAR_ALL_RECENTS_SINGLETAP = 101;
    public static final int HWKEY_ACTION_CLEAR_ALL_RECENTS_LONGPRESS = 102;
    public static final int HWKEY_DOUBLETAP_SPEED_DEFAULT = 400;
    public static final int HWKEY_KILL_DELAY_DEFAULT = 1000;
    public static final int HWKEY_TORCH_DISABLED = 0;
    public static final int HWKEY_TORCH_HOME_LONGPRESS = 1;
    public static final int HWKEY_TORCH_VOLDOWN_LONGPRESS = 2;
    public static final String ACTION_PREF_HWKEY_MENU_LONGPRESS_CHANGED = "gravitybox.intent.action.HWKEY_MENU_LONGPRESS_CHANGED";
    public static final String ACTION_PREF_HWKEY_MENU_DOUBLETAP_CHANGED = "gravitybox.intent.action.HWKEY_MENU_DOUBLETAP_CHANGED";
    public static final String ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED = "gravitybox.intent.action.HWKEY_HOME_LONGPRESS_CHANGED";
    public static final String ACTION_PREF_HWKEY_HOME_DOUBLETAP_CHANGED = "gravitybox.intent.action.HWKEY_HOME_DOUBLETAP_CHANGED";
    public static final String ACTION_PREF_HWKEY_BACK_LONGPRESS_CHANGED = "gravitybox.intent.action.HWKEY_BACK_LONGPRESS_CHANGED";
    public static final String ACTION_PREF_HWKEY_BACK_DOUBLETAP_CHANGED = "gravitybox.intent.action.HWKEY_BACK_DOUBLETAP_CHANGED";
    public static final String ACTION_PREF_HWKEY_RECENTS_SINGLETAP_CHANGED = "gravitybox.intent.action.HWKEY_RECENTS_SINGLETAP_CHANGED";
    public static final String ACTION_PREF_HWKEY_RECENTS_LONGPRESS_CHANGED = "gravitybox.intent.action.HWKEY_RECENTS_LONGPRESS_CHANGED";
    public static final String ACTION_PREF_HWKEY_DOUBLETAP_SPEED_CHANGED = "gravitybox.intent.action.HWKEY_DOUBLETAP_SPEED_CHANGED";
    public static final String ACTION_PREF_HWKEY_KILL_DELAY_CHANGED = "gravitybox.intent.action.HWKEY_KILL_DELAY_CHANGED";
    public static final String ACTION_PREF_VOLUME_ROCKER_WAKE_CHANGED = "gravitybox.intent.action.VOLUME_ROCKER_WAKE_CHANGED";
    public static final String ACTION_PREF_HWKEY_LOCKSCREEN_TORCH_CHANGED = "gravitybox.intent.action.HWKEY_LOCKSCREEN_TORCH_CHANGED";
    public static final String ACTION_PREF_VK_VIBRATE_PATTERN_CHANGED = "gravitybox.intent.action.VK_VIBRATE_PATTERN_CHANGED";
    public static final String EXTRA_HWKEY_VALUE = "hwKeyValue";
    public static final String EXTRA_HWKEY_HOME_DOUBLETAP_DISABLE = "hwKeyHomeDoubletapDisable";
    public static final String EXTRA_HWKEY_HOME_DOUBLETAP = "hwKeyHomeDoubletap";
    public static final String EXTRA_HWKEY_HOME_LONGPRESS_KG = "hwKeyHomeLongpressKeyguard";
    public static final String EXTRA_VOLUME_ROCKER_WAKE = "volumeRockerWake";
    public static final String EXTRA_HWKEY_TORCH = "hwKeyTorch";
    public static final String EXTRA_VK_VIBRATE_PATTERN = "virtualKeyVubratePattern";

    public static final String PREF_KEY_PHONE_FLIP = "pref_phone_flip";
    public static final int PHONE_FLIP_ACTION_NONE = 0;
    public static final int PHONE_FLIP_ACTION_MUTE = 1;
    public static final int PHONE_FLIP_ACTION_DISMISS = 2;
    public static final String PREF_KEY_CALL_VIBRATIONS = "pref_call_vibrations";
    public static final String CV_CONNECTED = "connected";
    public static final String CV_DISCONNECTED = "disconnected";
    public static final String CV_WAITING = "waiting";
    public static final String CV_PERIODIC = "periodic";

    public static final String PREF_CAT_KEY_NOTIF_DRAWER_STYLE = "pref_cat_notification_drawer_style";
    public static final String PREF_KEY_NOTIF_BACKGROUND = "pref_notif_background";
    public static final String PREF_KEY_NOTIF_COLOR = "pref_notif_color";
    public static final String PREF_KEY_NOTIF_COLOR_MODE = "pref_notif_color_mode";
    public static final String PREF_KEY_NOTIF_IMAGE_PORTRAIT = "pref_notif_image_portrait";
    public static final String PREF_KEY_NOTIF_IMAGE_LANDSCAPE = "pref_notif_image_landscape";
    public static final String PREF_KEY_NOTIF_BACKGROUND_ALPHA = "pref_notif_background_alpha";
    public static final String PREF_KEY_NOTIF_CARRIER_TEXT = "pref_notif_carrier_text";
    public static final String PREF_KEY_NOTIF_EXPAND_ALL = "pref_notif_expand_all";
    public static final String NOTIF_BG_DEFAULT = "default";
    public static final String NOTIF_BG_COLOR = "color";
    public static final String NOTIF_BG_IMAGE = "image";
    public static final String NOTIF_BG_COLOR_MODE_OVERLAY = "overlay";
    public static final String NOTIF_BG_COLOR_MODE_UNDERLAY = "underlay";
    public static final String ACTION_NOTIF_BACKGROUND_CHANGED = "gravitybox.intent.action.NOTIF_BACKGROUND_CHANGED";
    public static final String ACTION_NOTIF_CARRIER_TEXT_CHANGED = "gravitybox.intent.action.NOTIF_CARRIER_TEXT_CHANGED";
    public static final String ACTION_NOTIF_EXPAND_ALL_CHANGED = "gravitybox.intent.action.NOTIF_EXPAND_ALL_CHANGED";
    public static final String EXTRA_BG_TYPE = "bgType";
    public static final String EXTRA_BG_COLOR = "bgColor";
    public static final String EXTRA_BG_COLOR_MODE = "bgColorMode";
    public static final String EXTRA_BG_ALPHA = "bgAlpha";
    public static final String EXTRA_NOTIF_CARRIER_TEXT = "notifCarrierText";
    public static final String EXTRA_NOTIF_EXPAND_ALL = "notifExpandAll";

    public static final String PREF_KEY_PIE_CONTROL_ENABLE = "pref_pie_control_enable2";
    public static final String PREF_KEY_PIE_CONTROL_CUSTOM_KEY = "pref_pie_control_custom_key";
    public static final String PREF_KEY_PIE_CONTROL_MENU = "pref_pie_control_menu";
    public static final String PREF_KEY_PIE_CONTROL_TRIGGERS = "pref_pie_control_trigger_positions";
    public static final String PREF_KEY_PIE_CONTROL_TRIGGER_SIZE = "pref_pie_control_trigger_size";
    public static final String PREF_KEY_PIE_CONTROL_SIZE = "pref_pie_control_size";
    public static final String PREF_KEY_HWKEYS_DISABLE = "pref_hwkeys_disable";
    public static final String PREF_KEY_PIE_COLOR_BG = "pref_pie_color_bg";
    public static final String PREF_KEY_PIE_COLOR_FG = "pref_pie_color_fg";
    public static final String PREF_KEY_PIE_COLOR_OUTLINE = "pref_pie_color_outline";
    public static final String PREF_KEY_PIE_COLOR_SELECTED = "pref_pie_color_selected";
    public static final String PREF_KEY_PIE_COLOR_TEXT = "pref_pie_color_text";
    public static final String PREF_KEY_PIE_COLOR_RESET = "pref_pie_color_reset";
    public static final String PREF_KEY_PIE_BACK_LONGPRESS = "pref_pie_back_longpress";
    public static final String PREF_KEY_PIE_HOME_LONGPRESS = "pref_pie_home_longpress";
    public static final String PREF_KEY_PIE_RECENTS_LONGPRESS = "pref_pie_recents_longpress";
    public static final String PREF_KEY_PIE_SEARCH_LONGPRESS = "pref_pie_search_longpress";
    public static final String PREF_KEY_PIE_MENU_LONGPRESS = "pref_pie_menu_longpress";
    public static final String PREF_KEY_PIE_APP_LONGPRESS = "pref_pie_app_longpress";
    public static final String PREF_KEY_PIE_SYSINFO_DISABLE = "pref_pie_sysinfo_disable";
    public static final String PREF_KEY_PIE_LONGPRESS_DELAY = "pref_pie_longpress_delay";
    public static final int PIE_CUSTOM_KEY_OFF = 0;
    public static final int PIE_CUSTOM_KEY_SEARCH = 1;
    public static final int PIE_CUSTOM_KEY_APP_LAUNCHER = 2;
    public static final String ACTION_PREF_PIE_CHANGED = "gravitybox.intent.action.PREF_PIE_CHANGED";
    public static final String EXTRA_PIE_ENABLE = "pieEnable";
    public static final String EXTRA_PIE_CUSTOM_KEY_MODE = "pieCustomKeyMode";
    public static final String EXTRA_PIE_MENU = "pieMenu";
    public static final String EXTRA_PIE_TRIGGERS = "pieTriggers";
    public static final String EXTRA_PIE_TRIGGER_SIZE = "pieTriggerSize";
    public static final String EXTRA_PIE_SIZE = "pieSize";
    public static final String EXTRA_PIE_HWKEYS_DISABLE = "hwKeysDisable";
    public static final String EXTRA_PIE_COLOR_BG = "pieColorBg";
    public static final String EXTRA_PIE_COLOR_FG = "pieColorFg";
    public static final String EXTRA_PIE_COLOR_OUTLINE = "pieColorOutline";
    public static final String EXTRA_PIE_COLOR_SELECTED = "pieColorSelected";
    public static final String EXTRA_PIE_COLOR_TEXT = "pieColorText";
    public static final String EXTRA_PIE_BUTTON = "pieButton";
    public static final String EXTRA_PIE_LONGPRESS_ACTION = "pieLongpressAction";
    public static final String EXTRA_PIE_SYSINFO_DISABLE = "pieSysinfoDisable";
    public static final String EXTRA_PIE_LONGPRESS_DELAY = "pieLongpressDelay";

    public static final String PREF_KEY_BUTTON_BACKLIGHT_MODE = "pref_button_backlight_mode";
    public static final String PREF_KEY_BUTTON_BACKLIGHT_NOTIFICATIONS = "pref_button_backlight_notifications";
    public static final String ACTION_PREF_BUTTON_BACKLIGHT_CHANGED = "gravitybox.intent.action.BUTTON_BACKLIGHT_CHANGED";
    public static final String EXTRA_BB_MODE = "bbMode";
    public static final String EXTRA_BB_NOTIF = "bbNotif";
    public static final String BB_MODE_DEFAULT = "default";
    public static final String BB_MODE_DISABLE = "disable";
    public static final String BB_MODE_ALWAYS_ON = "always_on";

    public static final String PREF_KEY_QUICKAPP_DEFAULT = "pref_quickapp_default";
    public static final String PREF_KEY_QUICKAPP_SLOT1 = "pref_quickapp_slot1";
    public static final String PREF_KEY_QUICKAPP_SLOT2 = "pref_quickapp_slot2";
    public static final String PREF_KEY_QUICKAPP_SLOT3 = "pref_quickapp_slot3";
    public static final String PREF_KEY_QUICKAPP_SLOT4 = "pref_quickapp_slot4";
    public static final String PREF_KEY_QUICKAPP_DEFAULT_2 = "pref_quickapp_default_2";
    public static final String PREF_KEY_QUICKAPP_SLOT1_2 = "pref_quickapp_slot1_2";
    public static final String PREF_KEY_QUICKAPP_SLOT2_2 = "pref_quickapp_slot2_2";
    public static final String PREF_KEY_QUICKAPP_SLOT3_2 = "pref_quickapp_slot3_2";
    public static final String PREF_KEY_QUICKAPP_SLOT4_2 = "pref_quickapp_slot4_2";
    public static final String ACTION_PREF_QUICKAPP_CHANGED = "gravitybox.intent.action.QUICKAPP_CHANGED";
    public static final String ACTION_PREF_QUICKAPP_CHANGED_2 = "gravitybox.intent.action.QUICKAPP_CHANGED_2";
    public static final String EXTRA_QUICKAPP_DEFAULT = "quickAppDefault";
    public static final String EXTRA_QUICKAPP_SLOT1 = "quickAppSlot1";
    public static final String EXTRA_QUICKAPP_SLOT2 = "quickAppSlot2";
    public static final String EXTRA_QUICKAPP_SLOT3 = "quickAppSlot3";
    public static final String EXTRA_QUICKAPP_SLOT4 = "quickAppSlot4";

    public static final String PREF_KEY_GB_THEME_DARK = "pref_gb_theme_dark";
    public static final String FILE_THEME_DARK_FLAG = "theme_dark";

    public static final String ACTION_PREF_BATTERY_STYLE_CHANGED = "gravitybox.intent.action.BATTERY_STYLE_CHANGED";
    public static final String EXTRA_BATTERY_STYLE = "batteryStyle";
    public static final String ACTION_PREF_BATTERY_PERCENT_TEXT_CHANGED =
            "gravitybox.intent.action.BATTERY_PERCENT_TEXT_CHANGED";
    public static final String EXTRA_BATTERY_PERCENT_TEXT = "batteryPercentText";
    public static final String ACTION_PREF_BATTERY_PERCENT_TEXT_SIZE_CHANGED =
            "gravitybox.intent.action.BATTERY_PERCENT_TEXT_SIZE_CHANGED";
    public static final String EXTRA_BATTERY_PERCENT_TEXT_SIZE = "batteryPercentTextSize";
    public static final String ACTION_PREF_BATTERY_PERCENT_TEXT_STYLE_CHANGED =
            "gravitybox.intent.action.BATTERY_PERCENT_TEXT_SIZE_CHANGED";
    public static final String EXTRA_BATTERY_PERCENT_TEXT_STYLE = "batteryPercentTextStyle";
    public static final String EXTRA_BATTERY_PERCENT_TEXT_CHARGING = "batteryPercentTextCharging";
    public static final String EXTRA_BATTERY_PERCENT_TEXT_CHARGING_COLOR = "batteryPercentTextChargingColor";

    public static final String ACTION_PREF_STATUSBAR_COLOR_CHANGED = "gravitybox.intent.action.STATUSBAR_COLOR_CHANGED";
    public static final String EXTRA_SB_BG_COLOR = "bgColor";
    public static final String EXTRA_SB_ICON_COLOR_ENABLE = "iconColorEnable";
    public static final String EXTRA_SB_ICON_COLOR = "iconColor";
    public static final String EXTRA_SB_ICON_STYLE = "iconStyle";
    public static final String EXTRA_SB_ICON_COLOR_SECONDARY = "iconColorSecondary";
    public static final String EXTRA_SB_DATA_ACTIVITY_COLOR = "dataActivityColor";
    public static final String EXTRA_SB_DATA_ACTIVITY_COLOR_SECONDARY = "dataActivityColorSecondary";
    public static final String EXTRA_SB_SIGNAL_COLOR_MODE = "signalColorMode";
    public static final String EXTRA_TM_SB_LAUNCHER = "tmSbLauncher";
    public static final String EXTRA_TM_SB_LOCKSCREEN = "tmSbLockscreen";
    public static final String EXTRA_TM_NB_LAUNCHER = "tmNbLauncher";
    public static final String EXTRA_TM_NB_LOCKSCREEN = "tmNbLockscreen";

    public static final String ACTION_PREF_QUICKSETTINGS_CHANGED = "gravitybox.intent.action.QUICKSETTINGS_CHANGED";
    public static final String EXTRA_QS_PREFS = "qsPrefs";
    public static final String EXTRA_QS_COLS = "qsCols";
    public static final String EXTRA_QS_AUTOSWITCH = "qsAutoSwitch";
    public static final String EXTRA_QUICK_PULLDOWN = "quickPulldown";
    public static final String EXTRA_QUICK_PULLDOWN_SIZE = "quickPulldownSize";
    public static final String EXTRA_QS_TILE_STYLE = "qsTileStyle";
    public static final String EXTRA_QS_HIDE_ON_CHANGE = "qsHideOnChange";
    public static final String EXTRA_QS_TILE_LABEL_STYLE = "qsTileLabelStyle";

    public static final String ACTION_PREF_CLOCK_CHANGED = "gravitybox.intent.action.CENTER_CLOCK_CHANGED";
    public static final String EXTRA_CENTER_CLOCK = "centerClock";
    public static final String EXTRA_CLOCK_DOW = "clockDow";
    public static final String EXTRA_CLOCK_DOW_SIZE = "clockDowSize";
    public static final String EXTRA_AMPM_HIDE = "ampmHide";
    public static final String EXTRA_AMPM_SIZE = "ampmSize";
    public static final String EXTRA_CLOCK_HIDE = "clockHide";
    public static final String EXTRA_CLOCK_LINK = "clockLink";
    public static final String EXTRA_CLOCK_LONGPRESS_LINK = "clockLongpressLink";
    public static final String EXTRA_ALARM_HIDE = "alarmHide";

    public static final String ACTION_PREF_SAFE_MEDIA_VOLUME_CHANGED = "gravitybox.intent.action.SAFE_MEDIA_VOLUME_CHANGED";
    public static final String EXTRA_SAFE_MEDIA_VOLUME_ENABLED = "enabled";

    public static final String PREF_CAT_KEY_NAVBAR_KEYS = "pref_cat_navbar_keys";
    public static final String PREF_CAT_KEY_NAVBAR_RING = "pref_cat_navbar_ring";
    public static final String PREF_CAT_KEY_NAVBAR_COLOR = "pref_cat_navbar_color";
    public static final String PREF_CAT_KEY_NAVBAR_DIMEN = "pref_cat_navbar_dimen";
    public static final String PREF_KEY_NAVBAR_OVERRIDE = "pref_navbar_override";
    public static final String PREF_KEY_NAVBAR_ENABLE = "pref_navbar_enable";
    public static final String PREF_KEY_NAVBAR_ALWAYS_ON_BOTTOM = "pref_navbar_always_on_bottom";
    public static final String PREF_KEY_NAVBAR_HEIGHT = "pref_navbar_height";
    public static final String PREF_KEY_NAVBAR_HEIGHT_LANDSCAPE = "pref_navbar_height_landscape";
    public static final String PREF_KEY_NAVBAR_WIDTH = "pref_navbar_width";
    public static final String PREF_KEY_NAVBAR_MENUKEY = "pref_navbar_menukey";
    public static final String PREF_CAT_KEY_NAVBAR_CUSTOM_KEY = "pref_cat_navbar_custom_key";
    public static final String PREF_KEY_NAVBAR_CUSTOM_KEY_ENABLE = "pref_navbar_custom_key_enable";
    public static final String PREF_KEY_NAVBAR_CUSTOM_KEY_SINGLETAP = "pref_navbar_custom_key_singletap";
    public static final String PREF_KEY_NAVBAR_CUSTOM_KEY_LONGPRESS = "pref_navbar_custom_key_longpress";
    public static final String PREF_KEY_NAVBAR_CUSTOM_KEY_DOUBLETAP = "pref_navbar_custom_key_doubletap";
    public static final String PREF_KEY_NAVBAR_CUSTOM_KEY_SWAP = "pref_navbar_custom_key_swap";
    public static final String PREF_KEY_NAVBAR_SWAP_KEYS = "pref_navbar_swap_keys";
    public static final String PREF_KEY_NAVBAR_CURSOR_CONTROL = "pref_navbar_cursor_control";
    public static final String PREF_KEY_NAVBAR_COLOR_ENABLE = "pref_navbar_color_enable";
    public static final String PREF_KEY_NAVBAR_KEY_COLOR = "pref_navbar_key_color";
    public static final String PREF_KEY_NAVBAR_KEY_GLOW_COLOR = "pref_navbar_key_glow_color";
    public static final String PREF_KEY_NAVBAR_BG_COLOR = "pref_navbar_bg_color";
    public static final String PREF_KEY_NAVBAR_RING_DISABLE = "pref_navbar_ring_disable";
    public static final String PREF_KEY_NAVBAR_CAMERA_KEY_DISABLE = "pref_navbar_camera_key_disable";
    public static final String ACTION_PREF_NAVBAR_CHANGED = "gravitybox.intent.action.ACTION_NAVBAR_CHANGED";
    public static final String ACTION_PREF_NAVBAR_SWAP_KEYS = "gravitybox.intent.action.ACTION_NAVBAR_SWAP_KEYS";
    public static final String EXTRA_NAVBAR_HEIGHT = "navbarHeight";
    public static final String EXTRA_NAVBAR_HEIGHT_LANDSCAPE = "navbarHeightLandscape";
    public static final String EXTRA_NAVBAR_WIDTH = "navbarWidth";
    public static final String EXTRA_NAVBAR_MENUKEY = "navbarMenukey";
    public static final String EXTRA_NAVBAR_CUSTOM_KEY_ENABLE = "navbarCustomKeyEnable";
    public static final String EXTRA_NAVBAR_COLOR_ENABLE = "navbarColorEnable";
    public static final String EXTRA_NAVBAR_KEY_COLOR = "navbarKeyColor";
    public static final String EXTRA_NAVBAR_KEY_GLOW_COLOR = "navbarKeyGlowColor";
    public static final String EXTRA_NAVBAR_BG_COLOR = "navbarBgColor";
    public static final String EXTRA_NAVBAR_CURSOR_CONTROL = "navbarCursorControl";
    public static final String EXTRA_NAVBAR_CUSTOM_KEY_SINGLETAP = "navbarCustomKeySingletap";
    public static final String EXTRA_NAVBAR_CUSTOM_KEY_LONGPRESS = "navbarCustomKeyLongpress";
    public static final String EXTRA_NAVBAR_CUSTOM_KEY_DOUBLETAP = "navbarCustomKeyDoubletap";
    public static final String EXTRA_NAVBAR_CUSTOM_KEY_SWAP = "navbarCustomKeySwap";
    public static final String EXTRA_NAVBAR_RING_DISABLE = "navbarRingDisable";
    public static final String EXTRA_NAVBAR_CAMERA_KEY_DISABLE = "navbarCameraKeyDisable";

    public static final String PREF_KEY_LOCKSCREEN_TARGETS_ENABLE = "pref_lockscreen_ring_targets_enable";
    public static final String PREF_KEY_LOCKSCREEN_TARGETS_APP[] = new String[] {
        "pref_lockscreen_ring_targets_app0", "pref_lockscreen_ring_targets_app1", "pref_lockscreen_ring_targets_app2",
        "pref_lockscreen_ring_targets_app3", "pref_lockscreen_ring_targets_app4", "pref_lockscreen_ring_targets_app5",
        "pref_lockscreen_ring_targets_app6"
    };
    public static final String PREF_KEY_LOCKSCREEN_TARGETS_VERTICAL_OFFSET = "pref_lockscreen_ring_targets_vertical_offset";
    public static final String PREF_KEY_LOCKSCREEN_TARGETS_HORIZONTAL_OFFSET = "pref_lockscreen_ring_targets_horizontal_offset";

    public static final String PREF_KEY_STATUSBAR_BRIGHTNESS = "pref_statusbar_brightness";
    public static final String PREF_KEY_STATUSBAR_DT2S = "pref_statusbar_dt2s";
    public static final String ACTION_PREF_STATUSBAR_BRIGHTNESS_CHANGED = "gravitybox.intent.action.STATUSBAR_BRIGHTNESS_CHANGED";
    public static final String ACTION_PREF_STATUSBAR_DT2S_CHANGED = "gravitybox.intent.action.STATUSBAR_DT2S_CHANGED";
    public static final String EXTRA_SB_BRIGHTNESS = "sbBrightness";
    public static final String EXTRA_SB_DT2S = "sbDt2s";

    public static final String PREF_CAT_KEY_PHONE_TELEPHONY = "pref_cat_phone_telephony";
    public static final String PREF_CAT_KEY_PHONE_MESSAGING = "pref_cat_phone_messaging";
    public static final String PREF_CAT_KEY_PHONE_MOBILE_DATA = "pref_cat_phone_mobile_data";

    public static final String PREF_KEY_NETWORK_MODE_TILE_MODE = "pref_network_mode_tile_mode";
    public static final String PREF_KEY_NETWORK_MODE_TILE_LTE = "pref_network_mode_tile_lte";
    public static final String PREF_KEY_NETWORK_MODE_TILE_CDMA = "pref_network_mode_tile_cdma";
    public static final String PREF_KEY_RINGER_MODE_TILE_MODE = "pref_qs_ringer_mode";
    public static final String PREF_STAY_AWAKE_TILE_MODE = "pref_qs_stay_awake";
    public static final String PREF_KEY_QS_TILE_SPAN_DISABLE = "pref_qs_tile_span_disable";
    public static final String PREF_KEY_QS_ALARM_SINGLETAP_APP = "pref_qs_alarm_singletap_app";
    public static final String PREF_KEY_QS_ALARM_LONGPRESS_APP = "pref_qs_alarm_longpress_app";
    public static final String EXTRA_NMT_MODE = "networkModeTileMode";
    public static final String EXTRA_NMT_LTE = "networkModeTileLte";
    public static final String EXTRA_NMT_CDMA = "networkModeTileCdma";
    public static final String EXTRA_RMT_MODE = "ringerModeTileMode";
    public static final String EXTRA_SA_MODE = "stayAwakeTileMode";
    public static final String EXTRA_QS_TILE_SPAN_DISABLE = "qsTileSpanDisable";
    public static final String EXTRA_QS_ALARM_SINGLETAP_APP = "qsAlarmSingletapApp";
    public static final String EXTRA_QS_ALARM_LONGPRESS_APP = "qsAlarmLongpressApp";

    public static final String PREF_KEY_DISPLAY_ALLOW_ALL_ROTATIONS = "pref_display_allow_all_rotations";
    public static final String ACTION_PREF_DISPLAY_ALLOW_ALL_ROTATIONS_CHANGED = 
            "gravitybox.intent.action.DISPLAY_ALLOW_ALL_ROTATIONS_CHANGED";
    public static final String EXTRA_ALLOW_ALL_ROTATIONS = "allowAllRotations";

    public static final String PREF_KEY_QS_TILE_BEHAVIOUR_OVERRIDE = "pref_qs_tile_behaviour_override";

    public static final String PREF_KEY_QS_NETWORK_MODE_SIM_SLOT = "pref_qs_network_mode_sim_slot";
    public static final String ACTION_PREF_QS_NETWORK_MODE_SIM_SLOT_CHANGED =
            "gravitybox.intent.action.QS_NETWORK_MODE_SIM_SLOT_CHANGED";
    public static final String EXTRA_SIM_SLOT = "simSlot";

    public static final String PREF_KEY_ONGOING_NOTIFICATIONS = "pref_ongoing_notifications";
    public static final String ACTION_PREF_ONGOING_NOTIFICATIONS_CHANGED = 
            "gravitybox.intent.action.ONGOING_NOTIFICATIONS_CHANGED";
    public static final String EXTRA_ONGOING_NOTIF = "ongoingNotif";
    public static final String EXTRA_ONGOING_NOTIF_RESET = "ongoingNotifReset";

    public static final String PREF_CAT_KEY_DATA_TRAFFIC = "pref_cat_data_traffic";
    public static final String PREF_KEY_DATA_TRAFFIC_MODE = "pref_data_traffic_mode";
    public static final String PREF_KEY_DATA_TRAFFIC_ACTIVE_DL_ONLY = "pref_data_traffic_active_dl_only";
    public static final String PREF_KEY_DATA_TRAFFIC_POSITION = "pref_data_traffic_position";
    public static final int DT_POSITION_AUTO = 0;
    public static final int DT_POSITION_LEFT = 1;
    public static final int DT_POSITION_RIGHT = 2;
    public static final String PREF_KEY_DATA_TRAFFIC_SIZE = "pref_data_traffic_size";
    public static final String PREF_KEY_DATA_TRAFFIC_INACTIVITY_MODE = "pref_data_traffic_inactivity_mode";
    public static final String PREF_KEY_DATA_TRAFFIC_OMNI_MODE = "pref_data_traffic_omni_mode";
    public static final String PREF_KEY_DATA_TRAFFIC_OMNI_SHOW_ICON = "pref_data_traffic_omni_show_icon";
    public static final String PREF_KEY_DATA_TRAFFIC_OMNI_AUTOHIDE = "pref_data_traffic_omni_autohide";
    public static final String PREF_KEY_DATA_TRAFFIC_OMNI_AUTOHIDE_TH = "pref_data_traffic_omni_autohide_threshold";
    public static final String ACTION_PREF_DATA_TRAFFIC_CHANGED = 
            "gravitybox.intent.action.DATA_TRAFFIC_CHANGED";
    public static final String EXTRA_DT_MODE = "dtMode";
    public static final String EXTRA_DT_ACTIVE_DL_ONLY = "dtActiveDownloadOnly";
    public static final String EXTRA_DT_POSITION = "dtPosition";
    public static final String EXTRA_DT_SIZE = "dtSize";
    public static final String EXTRA_DT_INACTIVITY_MODE = "dtInactivityMode";
    public static final String EXTRA_DT_OMNI_MODE = "dtOmniMode";
    public static final String EXTRA_DT_OMNI_SHOW_ICON = "dtOmniShowIcon";
    public static final String EXTRA_DT_OMNI_AUTOHIDE = "dtOmniAutohide";
    public static final String EXTRA_DT_OMNI_AUTOHIDE_TH = "dtOmniAutohideTh";

    public static final String PREF_CAT_KEY_APP_LAUNCHER = "pref_cat_app_launcher";
    public static final List<String> PREF_KEY_APP_LAUNCHER_SLOT = new ArrayList<String>(Arrays.asList(
            "pref_app_launcher_slot0", "pref_app_launcher_slot1", "pref_app_launcher_slot2",
            "pref_app_launcher_slot3", "pref_app_launcher_slot4", "pref_app_launcher_slot5",
            "pref_app_launcher_slot6", "pref_app_launcher_slot7"));
    public static final String ACTION_PREF_APP_LAUNCHER_CHANGED = "gravitybox.intent.action.APP_LAUNCHER_CHANGED";
    public static final String EXTRA_APP_LAUNCHER_SLOT = "appLauncherSlot";
    public static final String EXTRA_APP_LAUNCHER_APP = "appLauncherApp";

    public static final String PREF_CAT_LAUNCHER_TWEAKS = "pref_cat_launcher_tweaks";
    public static final String PREF_KEY_LAUNCHER_DESKTOP_GRID_ROWS = "pref_launcher_desktop_grid_rows";
    public static final String PREF_KEY_LAUNCHER_DESKTOP_GRID_COLS = "pref_launcher_desktop_grid_cols";
    public static final String PREF_KEY_LAUNCHER_RESIZE_WIDGET = "pref_launcher_resize_widget";

    public static final String PREF_CAT_KEY_SIGNAL_CLUSTER = "pref_cat_signal_cluster";
    public static final String PREF_KEY_SIGNAL_CLUSTER_CONNECTION_STATE = "pref_signal_cluster_connection_state";
    public static final String PREF_KEY_SIGNAL_CLUSTER_DATA_ACTIVITY = "pref_signal_cluster_data_activity";

    public static final String PREF_CAT_KEY_NAVBAR_RING_TARGETS = "pref_cat_navbar_ring_targets";
    public static final String PREF_KEY_NAVBAR_RING_TARGETS_ENABLE = "pref_navbar_ring_targets_enable";
    public static final List<String> PREF_KEY_NAVBAR_RING_TARGET = new ArrayList<String>(Arrays.asList(
            "pref_navbar_ring_target0", "pref_navbar_ring_target1", "pref_navbar_ring_target2",
            "pref_navbar_ring_target3", "pref_navbar_ring_target4"));
    public static final String PREF_KEY_NAVBAR_RING_TARGETS_BG_STYLE = "pref_navbar_ring_targets_bg_style";
    public static final String PREF_KEY_NAVBAR_RING_HAPTIC_FEEDBACK = "pref_navbar_ring_haptic_feedback";
    public static final String ACTION_PREF_NAVBAR_RING_TARGET_CHANGED = "gravitybox.intent.action.NAVBAR_RING_TARGET_CHANGED";
    public static final String EXTRA_RING_TARGET_INDEX = "ringTargetIndex";
    public static final String EXTRA_RING_TARGET_APP = "ringTargetApp";
    public static final String EXTRA_RING_TARGET_BG_STYLE = "ringTargetBgStyle";
    public static final String EXTRA_RING_HAPTIC_FEEDBACK = "ringHapticFeedback";

    public static final String PREF_KEY_SMART_RADIO_ENABLE = "pref_smart_radio_enable";
    public static final String PREF_KEY_SMART_RADIO_NORMAL_MODE = "pref_smart_radio_normal_mode";
    public static final String PREF_KEY_SMART_RADIO_POWER_SAVING_MODE = "pref_smart_radio_power_saving_mode";
    public static final String PREF_KEY_SMART_RADIO_SCREEN_OFF = "pref_smart_radio_screen_off";
    public static final String PREF_KEY_SMART_RADIO_SCREEN_OFF_DELAY = "pref_smart_radio_screen_off_delay";
    public static final String PREF_KEY_SMART_RADIO_IGNORE_LOCKED = "pref_smart_radio_ignore_locked";
    public static final String PREF_KEY_SMART_RADIO_MODE_CHANGE_DELAY = "pref_smart_radio_mode_change_delay";
    public static final String ACTION_PREF_SMART_RADIO_CHANGED = "gravitybox.intent.action.SMART_RADIO_CHANGED";
    public static final String EXTRA_SR_NORMAL_MODE = "smartRadioNormalMode";
    public static final String EXTRA_SR_POWER_SAVING_MODE = "smartRadioPowerSavingMode";
    public static final String EXTRA_SR_SCREEN_OFF = "smartRadioScreenOff";
    public static final String EXTRA_SR_SCREEN_OFF_DELAY = "smartRadioScreenOffDelay";
    public static final String EXTRA_SR_IGNORE_LOCKED = "smartRadioIgnoreLocked";
    public static final String EXTRA_SR_MODE_CHANGE_DELAY = "smartRadioModeChangeDelay";

    public static final String PREF_KEY_IME_FULLSCREEN_DISABLE = "pref_ime_fullscreen_disable";
    public static final String PREF_KEY_TORCH_AUTO_OFF = "pref_torch_auto_off";
    public static final String PREF_KEY_FORCE_OVERFLOW_MENU_BUTTON = "pref_force_overflow_menu_button2";

    public static final String PREF_CAT_KEY_MISC_OTHER = "pref_cat_misc_other";
    public static final String PREF_KEY_PULSE_NOTIFICATION_DELAY = "pref_pulse_notification_delay";

    private static final String PREF_KEY_SETTINGS_BACKUP = "pref_settings_backup";
    private static final String PREF_KEY_SETTINGS_RESTORE = "pref_settings_restore";

    private static final String PREF_KEY_TRANS_VERIFICATION = "pref_trans_verification"; 

    private static final String PREF_LED_CONTROL = "pref_led_control";

    public static final String PREF_KEY_SCREENRECORD_SIZE = "pref_screenrecord_size";
    public static final String PREF_KEY_SCREENRECORD_BITRATE = "pref_screenrecord_bitrate";
    public static final String PREF_KEY_SCREENRECORD_TIMELIMIT = "pref_screenrecord_timelimit";
    public static final String PREF_KEY_SCREENRECORD_ROTATE = "pref_screenrecord_rotate";
    public static final String PREF_KEY_SCREENRECORD_MICROPHONE = "pref_screenrecord_microphone";
    public static final String PREF_KEY_SCREENRECORD_USE_STOCK = "pref_screenrecord_use_stock";

    public static final String PREF_KEY_FORCE_ENGLISH_LOCALE = "pref_force_english_locale";

    private static final int REQ_LOCKSCREEN_BACKGROUND = 1024;
    private static final int REQ_NOTIF_BG_IMAGE_PORTRAIT = 1025;
    private static final int REQ_NOTIF_BG_IMAGE_LANDSCAPE = 1026;
    private static final int REQ_CALLER_PHOTO = 1027;
    private static final int REQ_OBTAIN_SHORTCUT = 1028;

    private static final List<String> rebootKeys = new ArrayList<String>(Arrays.asList(
            PREF_KEY_BRIGHTNESS_MIN,
            PREF_KEY_LOCKSCREEN_MENU_KEY,
            PREF_KEY_MUSIC_VOLUME_STEPS,
            PREF_KEY_HOLO_BG_SOLID_BLACK,
            PREF_KEY_HOLO_BG_DITHER,
            PREF_KEY_TRANSLUCENT_DECOR,
            PREF_KEY_SCREEN_DIM_LEVEL,
            PREF_KEY_BRIGHTNESS_MASTER_SWITCH,
            PREF_KEY_NAVBAR_OVERRIDE,
            PREF_KEY_NAVBAR_ENABLE,
            PREF_KEY_QS_TILE_BEHAVIOUR_OVERRIDE,
            PREF_KEY_UNPLUG_TURNS_ON_SCREEN,
            PREF_KEY_TM_MODE,
            PREF_KEY_QUICK_SETTINGS_ENABLE,
            PREF_KEY_SIGNAL_CLUSTER_CONNECTION_STATE,
            PREF_KEY_SIGNAL_CLUSTER_DATA_ACTIVITY,
            PREF_KEY_NAVBAR_RING_TARGETS_ENABLE,
            PREF_KEY_FORCE_OVERFLOW_MENU_BUTTON,
            PREF_KEY_NAVBAR_ALWAYS_ON_BOTTOM,
            PREF_KEY_SMART_RADIO_ENABLE,
            PREF_KEY_PULSE_NOTIFICATION_DELAY,
            PREF_KEY_CRT_OFF_EFFECT,
            PREF_KEY_STATUSBAR_CLOCK_MASTER_SWITCH
    ));

    private static final class SystemProperties {
        public boolean hasGeminiSupport;
        public boolean isTablet;
        public boolean hasNavigationBar;
        public boolean unplugTurnsOnScreen;
        public int defaultNotificationLedOff;
        public boolean uuidRegistered;
        public int uncTrialCountdown;

        public SystemProperties(Bundle data) {
            if (data.containsKey("hasGeminiSupport")) {
                hasGeminiSupport = data.getBoolean("hasGeminiSupport");
            }
            if (data.containsKey("isTablet")) {
                isTablet = data.getBoolean("isTablet");
            }
            if (data.containsKey("hasNavigationBar")) {
                hasNavigationBar = data.getBoolean("hasNavigationBar");
            }
            if (data.containsKey("unplugTurnsOnScreen")) {
                unplugTurnsOnScreen = data.getBoolean("unplugTurnsOnScreen");
            }
            if (data.containsKey("defaultNotificationLedOff")) {
                defaultNotificationLedOff = data.getInt("defaultNotificationLedOff");
            }
            if (data.containsKey("uuidRegistered")) {
                uuidRegistered = data.getBoolean("uuidRegistered");
            }
            if (data.containsKey("uncTrialCountdown")) {
                uncTrialCountdown = data.getInt("uncTrialCountdown");
            }
        }
    }

    private GravityBoxResultReceiver mReceiver;
    private Handler mHandler;
    private static SystemProperties sSystemProperties;
    private Dialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    private Runnable mGetSystemPropertiesTimeout = new Runnable() {
        @Override
        public void run() {
            dismissProgressDialog();
            AlertDialog.Builder builder = new AlertDialog.Builder(GravityBoxSettings.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.gb_startup_error)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
            mAlertDialog = builder.create();
            mAlertDialog.show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set Holo Dark theme if flag file exists
        File file = new File(getFilesDir() + "/" + FILE_THEME_DARK_FLAG);
        if (file.exists()) {
            this.setTheme(android.R.style.Theme_Holo);
        }

        super.onCreate(savedInstanceState);

        // refuse to run if there's GB with old package name still installed
        // try to copy old preferences and uninstall previous package
        if (Utils.isAppInstalled(this, "com.ceco.gm2.gravitybox")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle("ATTENTION")
            .setMessage(R.string.gb_new_package_dialog)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    boolean copied = false;
                    // write dummy pref to force prefs file creation
                    PreferenceManager.getDefaultSharedPreferences(GravityBoxSettings.this)
                    .edit().putBoolean("dummy_pref", true).commit();
                    // replace our prefs file with file from old GB
                    String oldPrefsPath = "/data/data/com.ceco.gm2.gravitybox/shared_prefs/" +
                    		"com.ceco.gm2.gravitybox_preferences.xml";
                    File oldPrefsFile = new File(oldPrefsPath);
                    if (oldPrefsFile.exists() && oldPrefsFile.canRead()) {
                        File newFile = new File(getFilesDir() + "/../shared_prefs/" + 
                                getPackageName() + "_preferences.xml");
                        if (newFile.exists() && newFile.canWrite()) {
                            try {
                                Utils.copyFile(oldPrefsFile, newFile);
                                copied = true;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    Toast.makeText(GravityBoxSettings.this, copied ? 
                            getString(R.string.gb_new_package_settings_transfer_ok) : 
                                getString(R.string.gb_new_package_settings_transfer_failed),
                            Toast.LENGTH_LONG).show();
                    // try to uninstall old package
                    Uri oldGbUri = Uri.parse("package:com.ceco.gm2.gravitybox");
                    Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, oldGbUri);
                    startActivity(uninstallIntent);
                    finish();
                }
            });
            mAlertDialog = builder.create();
            mAlertDialog.show();
            return;
        }

        if (savedInstanceState == null || sSystemProperties == null) {
            mReceiver = new GravityBoxResultReceiver(new Handler());
            mReceiver.setReceiver(this);
            Intent intent = new Intent();
            intent.setAction(SystemPropertyProvider.ACTION_GET_SYSTEM_PROPERTIES);
            intent.putExtra("receiver", mReceiver);
            intent.putExtra("settings_uuid", SettingsManager.getInstance(this).getOrCreateUuid());
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setTitle(R.string.app_name);
            mProgressDialog.setMessage(getString(R.string.gb_startup_progress));
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            mHandler = new Handler();
            mHandler.postDelayed(mGetSystemPropertiesTimeout, 5000);
            sendBroadcast(intent);
        }
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mGetSystemPropertiesTimeout);
            mHandler = null;
        }
        dismissProgressDialog();
        dismissAlertDialog();

        super.onDestroy();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (mHandler != null) {
            mHandler.removeCallbacks(mGetSystemPropertiesTimeout);
            mHandler = null;
        }
        dismissProgressDialog();
        Log.d("GravityBox", "result received: resultCode=" + resultCode);
        if (resultCode == SystemPropertyProvider.RESULT_SYSTEM_PROPERTIES) {
            sSystemProperties = new SystemProperties(resultData);
            getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
        } else {
            finish();
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;
    }

    private void dismissAlertDialog() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        mAlertDialog = null;
    }

    public static class PrefsFragment extends PreferenceFragment 
                                      implements OnSharedPreferenceChangeListener,
                                                 OnPreferenceChangeListener {
        private ListPreference mBatteryStyle;
        private CheckBoxPreference mPrefBatteryPercent;
        private ListPreference mPrefBatteryPercentCharging;
        private ListPreference mLowBatteryWarning;
        private SharedPreferences mPrefs;
        private AlertDialog mDialog;
        private MultiSelectListPreference mQuickSettings;
        private Preference mPrefAboutGb;
        private Preference mPrefAboutGplus;
        private Preference mPrefAboutXposed;
        private Preference mPrefAboutDonate;
        private Preference mPrefEngMode;
        private Preference mPrefDualSimRinger;
        private PreferenceCategory mPrefCatLockscreenBg;
        private ListPreference mPrefLockscreenBg;
        private ColorPickerPreference mPrefLockscreenBgColor;
        private Preference mPrefLockscreenBgImage;
        private SeekBarPreference mPrefLockscreenBgOpacity;
        private CheckBoxPreference mPrefLockscreenBgBlurEffect;
        private SeekBarPreference mPrefLockscreenBlurIntensity;
        private EditTextPreference mPrefLockscreenCarrierText;
        private File wallpaperImage;
        private File wallpaperTemporary;
        private File notifBgImagePortrait;
        private File notifBgImageLandscape;
        private PreferenceScreen mPrefCatHwKeyActions;
        private PreferenceCategory mPrefCatHwKeyMenu;
        private ListPreference mPrefHwKeyMenuLongpress;
        private ListPreference mPrefHwKeyMenuDoubletap;
        private PreferenceCategory mPrefCatHwKeyHome;
        private ListPreference mPrefHwKeyHomeLongpress;
        private ListPreference mPrefHwKeyHomeDoubletap;
        private CheckBoxPreference mPrefHwKeyHomeLongpressKeyguard;
        private PreferenceCategory mPrefCatHwKeyBack;
        private ListPreference mPrefHwKeyBackLongpress;
        private ListPreference mPrefHwKeyBackDoubletap;
        private PreferenceCategory mPrefCatHwKeyRecents;
        private ListPreference mPrefHwKeyRecentsSingletap;
        private ListPreference mPrefHwKeyRecentsLongpress;
        private PreferenceCategory mPrefCatHwKeyVolume;
        private ListPreference mPrefHwKeyDoubletapSpeed;
        private ListPreference mPrefHwKeyKillDelay;
        private ListPreference mPrefPhoneFlip;
        private SwitchPreference mPrefSbIconColorEnable;
        private ColorPickerPreference mPrefSbIconColor;
        private ColorPickerPreference mPrefSbDaColor;
        private PreferenceScreen mPrefCatStatusbar;
        private PreferenceScreen mPrefCatStatusbarQs;
        private ListPreference mPrefAutoSwitchQs;
        private ListPreference mPrefQuickPulldown;
        private SeekBarPreference mPrefQuickPulldownSize;
        private PreferenceScreen mPrefCatNotifDrawerStyle;
        private ListPreference mPrefNotifBackground;
        private ColorPickerPreference mPrefNotifColor;
        private Preference mPrefNotifImagePortrait;
        private Preference mPrefNotifImageLandscape;
        private ListPreference mPrefNotifColorMode;
        private CheckBoxPreference mPrefDisableDataNetworkTypeIcons;
        private EditTextPreference mPrefNotifCarrierText;
        private CheckBoxPreference mPrefNotifExpandAll;
        private CheckBoxPreference mPrefDisableRoamingIndicators;
        private ListPreference mPrefButtonBacklightMode;
        private CheckBoxPreference mPrefButtonBacklightNotif;
        private ListPreference mPrefPieEnabled;
        private ListPreference mPrefPieCustomKey;
        private CheckBoxPreference mPrefPieHwKeysDisabled;
        private ColorPickerPreference mPrefPieColorBg;
        private ColorPickerPreference mPrefPieColorFg;
        private ColorPickerPreference mPrefPieColorOutline;
        private ColorPickerPreference mPrefPieColorSelected;
        private ColorPickerPreference mPrefPieColorText;
        private Preference mPrefPieColorReset;
        private ListPreference mPrefPieBackLongpress;
        private ListPreference mPrefPieHomeLongpress;
        private ListPreference mPrefPieRecentsLongpress;
        private ListPreference mPrefPieSearchLongpress;
        private ListPreference mPrefPieMenuLongpress;
        private ListPreference mPrefPieAppLongpress;
        private ListPreference mPrefPieLongpressDelay;
        private CheckBoxPreference mPrefGbThemeDark;
        private ListPreference mPrefRecentClear;
        private ListPreference mPrefClearRecentMode;
        private ListPreference mPrefRambar;
        private PreferenceScreen mPrefCatPhone;
        private SeekBarPreference mPrefBrightnessMin;
        private SeekBarPreference mPrefScreenDimLevel;
        private AutoBrightnessDialogPreference mPrefAutoBrightness;
        private PreferenceScreen mPrefCatLockscreen;
        private PreferenceScreen mPrefCatDisplay;
        private PreferenceScreen mPrefCatBrightness;
        private ListPreference mPrefCrtOff;
        private ListPreference mPrefTranclucentDecor;
        private PreferenceScreen mPrefCatMedia;
        private CheckBoxPreference mPrefSafeMediaVolume;
        private ListPreference mPrefExpandedDesktop;
        private PreferenceCategory mPrefCatNavbarKeys;
        private PreferenceCategory mPrefCatNavbarRing;
        private PreferenceCategory mPrefCatNavbarColor;
        private PreferenceCategory mPrefCatNavbarDimen;
        private CheckBoxPreference mPrefNavbarEnable;
        private CheckBoxPreference mPrefMusicVolumeSteps;
        private AppPickerPreference[] mPrefLockscreenTargetsApp;
        private ListPreference mPrefLockscreenSbClock;
        private PreferenceCategory mPrefCatPhoneTelephony;
        private PreferenceCategory mPrefCatPhoneMessaging;
        private PreferenceCategory mPrefCatPhoneMobileData;
        private ListPreference mPrefNetworkModeTileMode;
        private CheckBoxPreference mPrefNetworkModeTileLte;
        private CheckBoxPreference mPrefNetworkModeTileCdma;
        private MultiSelectListPreference mPrefQsTileBehaviourOverride;
        private ListPreference mPrefQsNetworkModeSimSlot;
        private ListPreference mPrefSbSignalColorMode;
        private CheckBoxPreference mPrefUnplugTurnsOnScreen;
        private MultiSelectListPreference mPrefCallVibrations;
        private Preference mPrefQsTileOrder;
        private ListPreference mPrefQsTileLabelStyle;
        private ListPreference mPrefSbClockDow;
        private SeekBarPreference mPrefSbClockDowSize;
        private ListPreference mPrefSbLockPolicy;
        private PreferenceScreen mPrefCatDataTraffic;
        private ListPreference mPrefDataTrafficPosition;
        private ListPreference mPrefDataTrafficSize;
        private ListPreference mPrefDataTrafficMode;
        private ListPreference mPrefDataTrafficInactivityMode;
        private ListPreference mPrefDataTrafficOmniMode;
        private CheckBoxPreference mPrefDataTrafficOmniShowIcon;
        private CheckBoxPreference mPrefDataTrafficOmniAutohide;
        private SeekBarPreference mPrefDataTrafficOmniAutohideTh;
        private CheckBoxPreference mPrefDataTrafficActiveDlOnly;
        private CheckBoxPreference mPrefLinkVolumes;
        private CheckBoxPreference mPrefVolumePanelExpandable;
        private CheckBoxPreference mPrefVolumePanelFullyExpandable;
        private CheckBoxPreference mPrefVolumePanelAutoexpand;
        private ListPreference mPrefVolumePanelTimeout;
        private CheckBoxPreference mPrefHomeDoubletapDisable;
        private PreferenceScreen mPrefCatAppLauncher;
        private AppPickerPreference[] mPrefAppLauncherSlot;
        private File callerPhotoFile;
        private CheckBoxPreference mPrefCallerUnknownPhotoEnable;
        private Preference mPrefCallerUnknownPhoto;
        private ListPreference mPrefCallerFullscreenPhoto;
        private SeekBarPreference mPrefTmSbLauncher;
        private SeekBarPreference mPrefTmSbLockscreen;
        private SeekBarPreference mPrefTmNbLauncher;
        private SeekBarPreference mPrefTmNbLockscreen;
        private PreferenceScreen mPrefCatStatusbarColors;
        private ColorPickerPreference mPrefSbIconColorSecondary;
        private ColorPickerPreference mPrefSbDaColorSecondary;
        private PreferenceScreen mPrefCatTransparencyManager;
        private ListPreference mPrefHwKeyLockscreenTorch;
        private PreferenceCategory mPrefCatHwKeyOthers;
        private PreferenceCategory mPrefCatLsSlidingChallenge;
        private PreferenceCategory mPrefCatLsOther;
        private CheckBoxPreference mPrefLsRingTorch;
        private PreferenceScreen mPrefCatLauncherTweaks;
        private ListPreference mPrefLauncherDesktopGridRows;
        private ListPreference mPrefLauncherDesktopGridCols;
        private ListPreference mPrefVolumeRockerWake;
        private PreferenceScreen mPrefCatNavbarCustomKey;
        private ListPreference mPrefNavbarCustomKeySingletap;
        private ListPreference mPrefNavbarCustomKeyLongpress;
        private ListPreference mPrefNavbarCustomKeyDoubletap;
        private PreferenceScreen mPrefCatNavbarRingTargets;
        private SwitchPreference mPrefNavbarRingTargetsEnable;
        private AppPickerPreference[] mPrefNavbarRingTarget;
        private ListPreference mPrefNavbarRingTargetsBgStyle;
        private ListPreference mPrefNavbarRingHapticFeedback;
        private SeekBarPreference mPrefPulseNotificationDelay;
        private PreferenceCategory mPrefCatMiscOther;
        private SeekBarPreference mPrefTorchAutoOff;
        private WebServiceClient<TransactionResult> mTransWebServiceClient;
        private Preference mPrefBackup;
        private Preference mPrefRestore;
        private EditTextPreference mPrefTransVerification;
        private ListPreference mPrefScreenrecordSize;
        private PreferenceScreen mPrefCatSignalCluster;
        private PreferenceScreen mPrefCatQsTileSettings;
        private PreferenceScreen mPrefCatQsNmTileSettings;
        private Preference mPrefLedControl;
        private EditTextPreference mPrefVkVibratePattern;

        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // this is important because although the handler classes that read these settings
            // are in the same package, they are executed in the context of the hooked package
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.gravitybox);

            mPrefs = getPreferenceScreen().getSharedPreferences();
            AppPickerPreference.sPrefsFragment = this;

            mBatteryStyle = (ListPreference) findPreference(PREF_KEY_BATTERY_STYLE);
            mPrefBatteryPercent = (CheckBoxPreference) findPreference(PREF_KEY_BATTERY_PERCENT_TEXT);
            mPrefBatteryPercentCharging = (ListPreference) findPreference(PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING);
            mLowBatteryWarning = (ListPreference) findPreference(PREF_KEY_LOW_BATTERY_WARNING_POLICY);
            mQuickSettings = (MultiSelectListPreference) findPreference(PREF_KEY_QUICK_SETTINGS);

            mPrefAboutGb = (Preference) findPreference(PREF_KEY_ABOUT_GRAVITYBOX);
            
            String version = "";
            try {
                PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                version = " v" + pInfo.versionName;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            } finally {
                mPrefAboutGb.setTitle(getActivity().getTitle() + version);
            }

            mPrefAboutGplus = (Preference) findPreference(PREF_KEY_ABOUT_GPLUS);
            mPrefAboutXposed = (Preference) findPreference(PREF_KEY_ABOUT_XPOSED);
            mPrefAboutDonate = (Preference) findPreference(PREF_KEY_ABOUT_DONATE);

            mPrefEngMode = (Preference) findPreference(PREF_KEY_ENGINEERING_MODE);
            if (!Utils.isAppInstalled(getActivity(), APP_ENGINEERING_MODE)) {
                getPreferenceScreen().removePreference(mPrefEngMode);
            }

            mPrefDualSimRinger = (Preference) findPreference(PREF_KEY_DUAL_SIM_RINGER);
            if (!Utils.isAppInstalled(getActivity(), APP_DUAL_SIM_RINGER)) {
                getPreferenceScreen().removePreference(mPrefDualSimRinger);
            }

            mPrefCatLockscreenBg = 
                    (PreferenceCategory) findPreference(PREF_CAT_KEY_LOCKSCREEN_BACKGROUND);
            mPrefLockscreenBg = (ListPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND);
            mPrefLockscreenBgColor = 
                    (ColorPickerPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR);
            mPrefLockscreenBgImage = 
                    (Preference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_IMAGE);
            mPrefLockscreenBgOpacity = 
                    (SeekBarPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_OPACITY);
            mPrefLockscreenBgBlurEffect =
                    (CheckBoxPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_BLUR_EFFECT);
            mPrefLockscreenBlurIntensity =
                    (SeekBarPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_BLUR_INTENSITY);
            mPrefLockscreenCarrierText = 
                    (EditTextPreference) findPreference(PREF_KEY_LOCKSCREEN_CARRIER_TEXT);

            wallpaperImage = new File(getActivity().getFilesDir() + "/lockwallpaper"); 
            wallpaperTemporary = new File(getActivity().getCacheDir() + "/lockwallpaper.tmp");
            notifBgImagePortrait = new File(getActivity().getFilesDir() + "/notifwallpaper");
            notifBgImageLandscape = new File(getActivity().getFilesDir() + "/notifwallpaper_landscape");
            callerPhotoFile = new File(getActivity().getFilesDir() + "/caller_photo");

            mPrefCatHwKeyActions = (PreferenceScreen) findPreference(PREF_CAT_HWKEY_ACTIONS);
            mPrefCatHwKeyMenu = (PreferenceCategory) findPreference(PREF_CAT_HWKEY_MENU);
            mPrefHwKeyMenuLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_MENU_LONGPRESS);
            mPrefHwKeyMenuDoubletap = (ListPreference) findPreference(PREF_KEY_HWKEY_MENU_DOUBLETAP);
            mPrefCatHwKeyHome = (PreferenceCategory) findPreference(PREF_CAT_HWKEY_HOME);
            mPrefHwKeyHomeLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_HOME_LONGPRESS);
            //mPrefHwKeyHomeLongpressKeyguard = (CheckBoxPreference) findPreference(PREF_KEY_HWKEY_HOME_LONGPRESS_KEYGUARD);
            mPrefHwKeyHomeDoubletap = (ListPreference) findPreference(PREF_KEY_HWKEY_HOME_DOUBLETAP);
            mPrefCatHwKeyBack = (PreferenceCategory) findPreference(PREF_CAT_HWKEY_BACK);
            mPrefHwKeyBackLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_BACK_LONGPRESS);
            mPrefHwKeyBackDoubletap = (ListPreference) findPreference(PREF_KEY_HWKEY_BACK_DOUBLETAP);
            mPrefCatHwKeyRecents = (PreferenceCategory) findPreference(PREF_CAT_HWKEY_RECENTS);
            mPrefHwKeyRecentsSingletap = (ListPreference) findPreference(PREF_KEY_HWKEY_RECENTS_SINGLETAP);
            mPrefHwKeyRecentsLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_RECENTS_LONGPRESS);
            mPrefHwKeyDoubletapSpeed = (ListPreference) findPreference(PREF_KEY_HWKEY_DOUBLETAP_SPEED);
            mPrefHwKeyKillDelay = (ListPreference) findPreference(PREF_KEY_HWKEY_KILL_DELAY);
            mPrefCatHwKeyVolume = (PreferenceCategory) findPreference(PREF_CAT_HWKEY_VOLUME);
            mPrefHomeDoubletapDisable = (CheckBoxPreference) findPreference(PREF_KEY_HWKEY_HOME_DOUBLETAP_DISABLE);
            mPrefHwKeyLockscreenTorch = (ListPreference) findPreference(PREF_KEY_HWKEY_LOCKSCREEN_TORCH);
            mPrefCatHwKeyOthers = (PreferenceCategory) findPreference(PREF_CAT_KEY_HWKEY_ACTIONS_OTHERS);

            mPrefPhoneFlip = (ListPreference) findPreference(PREF_KEY_PHONE_FLIP);

            mPrefSbIconColorEnable = (SwitchPreference) findPreference(PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE);
            mPrefSbIconColor = (ColorPickerPreference) findPreference(PREF_KEY_STATUSBAR_ICON_COLOR);
            mPrefSbDaColor = (ColorPickerPreference) findPreference(PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR);
            mPrefSbSignalColorMode = (ListPreference) findPreference(PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE);

            mPrefCatStatusbar = (PreferenceScreen) findPreference(PREF_CAT_KEY_STATUSBAR);
            mPrefCatStatusbarQs = (PreferenceScreen) findPreference(PREF_CAT_KEY_STATUSBAR_QS);
            mPrefCatQsTileSettings = (PreferenceScreen) findPreference(PREF_CAT_KEY_QS_TILE_SETTINGS);
            mPrefCatQsNmTileSettings = (PreferenceScreen) findPreference(PREF_CAT_KEY_QS_NM_TILE_SETTINGS);
            mPrefCatStatusbarColors = (PreferenceScreen) findPreference(PREF_CAT_KEY_STATUSBAR_COLORS);
            mPrefAutoSwitchQs = (ListPreference) findPreference(PREF_KEY_QUICK_SETTINGS_AUTOSWITCH);
            mPrefQuickPulldown = (ListPreference) findPreference(PREF_KEY_QUICK_PULLDOWN);
            mPrefQuickPulldownSize = (SeekBarPreference) findPreference(PREF_KEY_QUICK_PULLDOWN_SIZE);

            mPrefCatNotifDrawerStyle = (PreferenceScreen) findPreference(PREF_CAT_KEY_NOTIF_DRAWER_STYLE);
            mPrefNotifBackground = (ListPreference) findPreference(PREF_KEY_NOTIF_BACKGROUND);
            mPrefNotifColor = (ColorPickerPreference) findPreference(PREF_KEY_NOTIF_COLOR);
            mPrefNotifImagePortrait = (Preference) findPreference(PREF_KEY_NOTIF_IMAGE_PORTRAIT);
            mPrefNotifImageLandscape = (Preference) findPreference(PREF_KEY_NOTIF_IMAGE_LANDSCAPE);
            mPrefNotifColorMode = (ListPreference) findPreference(PREF_KEY_NOTIF_COLOR_MODE);
            mPrefNotifCarrierText = (EditTextPreference) findPreference(PREF_KEY_NOTIF_CARRIER_TEXT);
            mPrefNotifExpandAll = (CheckBoxPreference) findPreference(PREF_KEY_NOTIF_EXPAND_ALL);

            mPrefDisableDataNetworkTypeIcons = (CheckBoxPreference) findPreference(PREF_KEY_DISABLE_DATA_NETWORK_TYPE_ICONS);
            mPrefDisableRoamingIndicators = (CheckBoxPreference) findPreference(PREF_KEY_DISABLE_ROAMING_INDICATORS);
            mPrefButtonBacklightMode = (ListPreference) findPreference(PREF_KEY_BUTTON_BACKLIGHT_MODE);
            mPrefButtonBacklightNotif = (CheckBoxPreference) findPreference(PREF_KEY_BUTTON_BACKLIGHT_NOTIFICATIONS);

            mPrefPieEnabled = (ListPreference) findPreference(PREF_KEY_PIE_CONTROL_ENABLE);
            mPrefPieHwKeysDisabled = (CheckBoxPreference) findPreference(PREF_KEY_HWKEYS_DISABLE);
            mPrefPieCustomKey = (ListPreference) findPreference(PREF_KEY_PIE_CONTROL_CUSTOM_KEY);
            mPrefPieColorBg = (ColorPickerPreference) findPreference(PREF_KEY_PIE_COLOR_BG);
            mPrefPieColorFg = (ColorPickerPreference) findPreference(PREF_KEY_PIE_COLOR_FG);
            mPrefPieColorOutline = (ColorPickerPreference) findPreference(PREF_KEY_PIE_COLOR_OUTLINE);
            mPrefPieColorSelected = (ColorPickerPreference) findPreference(PREF_KEY_PIE_COLOR_SELECTED);
            mPrefPieColorText = (ColorPickerPreference) findPreference(PREF_KEY_PIE_COLOR_TEXT);
            mPrefPieColorReset = (Preference) findPreference(PREF_KEY_PIE_COLOR_RESET);
            mPrefPieBackLongpress = (ListPreference) findPreference(PREF_KEY_PIE_BACK_LONGPRESS);
            mPrefPieHomeLongpress = (ListPreference) findPreference(PREF_KEY_PIE_HOME_LONGPRESS);
            mPrefPieRecentsLongpress = (ListPreference) findPreference(PREF_KEY_PIE_RECENTS_LONGPRESS);
            mPrefPieSearchLongpress = (ListPreference) findPreference(PREF_KEY_PIE_SEARCH_LONGPRESS);
            mPrefPieMenuLongpress = (ListPreference) findPreference(PREF_KEY_PIE_MENU_LONGPRESS);
            mPrefPieAppLongpress = (ListPreference) findPreference(PREF_KEY_PIE_APP_LONGPRESS);
            mPrefPieLongpressDelay = (ListPreference) findPreference(PREF_KEY_PIE_LONGPRESS_DELAY);

            mPrefGbThemeDark = (CheckBoxPreference) findPreference(PREF_KEY_GB_THEME_DARK);
            File file = new File(getActivity().getFilesDir() + "/" + FILE_THEME_DARK_FLAG);
            mPrefGbThemeDark.setChecked(file.exists());

            mPrefRecentClear = (ListPreference) findPreference(PREF_KEY_RECENTS_CLEAR_ALL);
            mPrefClearRecentMode = (ListPreference) findPreference(PREF_KEY_CLEAR_RECENTS_MODE);
            mPrefRambar = (ListPreference) findPreference(PREF_KEY_RAMBAR);

            mPrefCatPhone = (PreferenceScreen) findPreference(PREF_CAT_KEY_PHONE);

            mPrefBrightnessMin = (SeekBarPreference) findPreference(PREF_KEY_BRIGHTNESS_MIN);
            mPrefBrightnessMin.setMinimum(getResources().getInteger(R.integer.screen_brightness_min));
            mPrefScreenDimLevel = (SeekBarPreference) findPreference(PREF_KEY_SCREEN_DIM_LEVEL);
            mPrefScreenDimLevel.setMinimum(getResources().getInteger(R.integer.screen_brightness_dim_min));
            mPrefAutoBrightness = (AutoBrightnessDialogPreference) findPreference(PREF_KEY_AUTOBRIGHTNESS);

            mPrefCatLockscreen = (PreferenceScreen) findPreference(PREF_CAT_KEY_LOCKSCREEN);
            mPrefCatDisplay = (PreferenceScreen) findPreference(PREF_CAT_KEY_DISPLAY);
            mPrefCatBrightness = (PreferenceScreen) findPreference(PREF_CAT_KEY_BRIGHTNESS);
            mPrefCrtOff = (ListPreference) findPreference(PREF_KEY_CRT_OFF_EFFECT);
            mPrefUnplugTurnsOnScreen = (CheckBoxPreference) findPreference(PREF_KEY_UNPLUG_TURNS_ON_SCREEN);
            mPrefCatMedia = (PreferenceScreen) findPreference(PREF_CAT_KEY_MEDIA);
            mPrefSafeMediaVolume = (CheckBoxPreference) findPreference(PREF_KEY_SAFE_MEDIA_VOLUME);
            mPrefMusicVolumeSteps = (CheckBoxPreference) findPreference(PREF_KEY_MUSIC_VOLUME_STEPS);
            mPrefLinkVolumes = (CheckBoxPreference) findPreference(PREF_KEY_LINK_VOLUMES);
            mPrefVolumePanelExpandable = (CheckBoxPreference) findPreference(PREF_KEY_VOLUME_PANEL_EXPANDABLE);
            mPrefVolumePanelFullyExpandable = (CheckBoxPreference) findPreference(PREF_KEY_VOLUME_PANEL_FULLY_EXPANDABLE);
            mPrefVolumePanelAutoexpand = (CheckBoxPreference) findPreference(PREF_KEY_VOLUME_PANEL_AUTOEXPAND);
            mPrefVolumePanelTimeout = (ListPreference) findPreference(PREF_KEY_VOLUME_PANEL_TIMEOUT);
            mPrefTranclucentDecor =  (ListPreference) findPreference(PREF_KEY_TRANSLUCENT_DECOR);

            mPrefExpandedDesktop = (ListPreference) findPreference(PREF_KEY_EXPANDED_DESKTOP);

            mPrefCatNavbarKeys = (PreferenceCategory) findPreference(PREF_CAT_KEY_NAVBAR_KEYS);
            mPrefCatNavbarRing = (PreferenceCategory) findPreference(PREF_CAT_KEY_NAVBAR_RING);
            mPrefCatNavbarColor = (PreferenceCategory) findPreference(PREF_CAT_KEY_NAVBAR_COLOR);
            mPrefCatNavbarDimen = (PreferenceCategory) findPreference(PREF_CAT_KEY_NAVBAR_DIMEN);
            mPrefNavbarEnable = (CheckBoxPreference) findPreference(PREF_KEY_NAVBAR_ENABLE);
            mPrefCatNavbarCustomKey = (PreferenceScreen) findPreference(PREF_CAT_KEY_NAVBAR_CUSTOM_KEY);
            mPrefNavbarCustomKeySingletap = (ListPreference) findPreference(PREF_KEY_NAVBAR_CUSTOM_KEY_SINGLETAP);
            mPrefNavbarCustomKeyLongpress = (ListPreference) findPreference(PREF_KEY_NAVBAR_CUSTOM_KEY_LONGPRESS);
            mPrefNavbarCustomKeyDoubletap = (ListPreference) findPreference(PREF_KEY_NAVBAR_CUSTOM_KEY_DOUBLETAP);

            mPrefLockscreenTargetsApp = new AppPickerPreference[PREF_KEY_LOCKSCREEN_TARGETS_APP.length];
            for (int i=0; i<PREF_KEY_LOCKSCREEN_TARGETS_APP.length; i++) {
                mPrefLockscreenTargetsApp[i] = (AppPickerPreference) findPreference(
                        PREF_KEY_LOCKSCREEN_TARGETS_APP[i]);
                String title = String.format(
                        getString(R.string.pref_lockscreen_ring_targets_app_title), (i+1));
                mPrefLockscreenTargetsApp[i].setTitle(title);
                mPrefLockscreenTargetsApp[i].setDialogTitle(title);
            }
            mPrefLockscreenSbClock = (ListPreference) findPreference(PREF_KEY_LOCKSCREEN_STATUSBAR_CLOCK);

            mPrefCatPhoneTelephony = (PreferenceCategory) findPreference(PREF_CAT_KEY_PHONE_TELEPHONY);
            mPrefCatPhoneMessaging = (PreferenceCategory) findPreference(PREF_CAT_KEY_PHONE_MESSAGING);
            mPrefCatPhoneMobileData = (PreferenceCategory) findPreference(PREF_CAT_KEY_PHONE_MOBILE_DATA);
            mPrefCallVibrations = (MultiSelectListPreference) findPreference(PREF_KEY_CALL_VIBRATIONS);
            mPrefCallerUnknownPhotoEnable = (CheckBoxPreference) findPreference(PREF_KEY_CALLER_UNKNOWN_PHOTO_ENABLE);
            mPrefCallerUnknownPhoto = (Preference) findPreference(PREF_KEY_CALLER_UNKNOWN_PHOTO);
            mPrefCallerFullscreenPhoto = (ListPreference) findPreference(PREF_KEY_CALLER_FULLSCREEN_PHOTO);

            mPrefNetworkModeTileMode = (ListPreference) findPreference(PREF_KEY_NETWORK_MODE_TILE_MODE);
            mPrefNetworkModeTileLte = (CheckBoxPreference) findPreference(PREF_KEY_NETWORK_MODE_TILE_LTE);
            mPrefNetworkModeTileCdma = (CheckBoxPreference) findPreference(PREF_KEY_NETWORK_MODE_TILE_CDMA);
            mPrefQsTileBehaviourOverride = 
                    (MultiSelectListPreference) findPreference(PREF_KEY_QS_TILE_BEHAVIOUR_OVERRIDE);
            mPrefQsNetworkModeSimSlot = (ListPreference) findPreference(PREF_KEY_QS_NETWORK_MODE_SIM_SLOT);
            mPrefQsTileOrder = (Preference) findPreference(PREF_KEY_QUICK_SETTINGS_TILE_ORDER);
            mPrefQsTileLabelStyle = (ListPreference) findPreference(PREF_KEY_QUICK_SETTINGS_TILE_LABEL_STYLE);

            mPrefSbClockDow = (ListPreference) findPreference(PREF_KEY_STATUSBAR_CLOCK_DOW);
            mPrefSbClockDowSize = (SeekBarPreference) findPreference(PREF_KEY_STATUSBAR_CLOCK_DOW_SIZE);
            mPrefSbLockPolicy = (ListPreference) findPreference(PREF_KEY_STATUSBAR_LOCK_POLICY);

            mPrefCatDataTraffic = (PreferenceScreen) findPreference(PREF_CAT_KEY_DATA_TRAFFIC);
            mPrefDataTrafficPosition = (ListPreference) findPreference(PREF_KEY_DATA_TRAFFIC_POSITION);
            mPrefDataTrafficSize = (ListPreference) findPreference(PREF_KEY_DATA_TRAFFIC_SIZE);
            mPrefDataTrafficMode = (ListPreference) findPreference(PREF_KEY_DATA_TRAFFIC_MODE);
            mPrefDataTrafficInactivityMode = (ListPreference) findPreference(PREF_KEY_DATA_TRAFFIC_INACTIVITY_MODE);
            mPrefDataTrafficOmniMode = (ListPreference) findPreference(PREF_KEY_DATA_TRAFFIC_OMNI_MODE);
            mPrefDataTrafficOmniShowIcon = (CheckBoxPreference) findPreference(PREF_KEY_DATA_TRAFFIC_OMNI_SHOW_ICON);
            mPrefDataTrafficActiveDlOnly = (CheckBoxPreference) findPreference(PREF_KEY_DATA_TRAFFIC_ACTIVE_DL_ONLY);
            mPrefDataTrafficOmniAutohide = (CheckBoxPreference) findPreference(PREF_KEY_DATA_TRAFFIC_OMNI_AUTOHIDE);
            mPrefDataTrafficOmniAutohideTh = (SeekBarPreference) findPreference(PREF_KEY_DATA_TRAFFIC_OMNI_AUTOHIDE_TH);

            mPrefCatAppLauncher = (PreferenceScreen) findPreference(PREF_CAT_KEY_APP_LAUNCHER);
            mPrefAppLauncherSlot = new AppPickerPreference[PREF_KEY_APP_LAUNCHER_SLOT.size()];
            for (int i = 0; i < mPrefAppLauncherSlot.length; i++) {
                AppPickerPreference appPref = new AppPickerPreference(getActivity(), null);
                appPref.setKey(PREF_KEY_APP_LAUNCHER_SLOT.get(i));
                appPref.setTitle(String.format(
                        getActivity().getString(R.string.pref_app_launcher_slot_title), i + 1));
                appPref.setDialogTitle(appPref.getTitle());
                appPref.setDefaultSummary(getActivity().getString(R.string.app_picker_none));
                appPref.setSummary(getActivity().getString(R.string.app_picker_none));
                mPrefAppLauncherSlot[i] = appPref;
                mPrefCatAppLauncher.addPreference(mPrefAppLauncherSlot[i]);
            }

            mPrefCatTransparencyManager = (PreferenceScreen) findPreference(PREF_CAT_KEY_TRANSPARENCY_MANAGER);
            mPrefTmSbLauncher = (SeekBarPreference) findPreference(PREF_KEY_TM_STATUSBAR_LAUNCHER);
            mPrefTmSbLockscreen = (SeekBarPreference) findPreference(PREF_KEY_TM_STATUSBAR_LOCKSCREEN);
            mPrefTmNbLauncher = (SeekBarPreference) findPreference(PREF_KEY_TM_NAVBAR_LAUNCHER);
            mPrefTmNbLockscreen = (SeekBarPreference) findPreference(PREF_KEY_TM_NAVBAR_LOCKSCREEN);

            mPrefSbIconColorSecondary = (ColorPickerPreference) findPreference(PREF_KEY_STATUSBAR_ICON_COLOR_SECONDARY);
            mPrefSbDaColorSecondary = (ColorPickerPreference) findPreference(PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR_SECONDARY);

            mPrefCatLsSlidingChallenge = (PreferenceCategory) findPreference(PREF_CAT_KEY_LOCKSCREEN_SLIDING_CHALLENGE);
            mPrefCatLsOther = (PreferenceCategory) findPreference(PREF_CAT_KEY_LOCKSCREEN_OTHER);
            mPrefLsRingTorch = (CheckBoxPreference) findPreference(PREF_KEY_LOCKSCREEN_RING_TORCH);

            mPrefCatLauncherTweaks = (PreferenceScreen) findPreference(PREF_CAT_LAUNCHER_TWEAKS);
            mPrefLauncherDesktopGridRows = (ListPreference) findPreference(PREF_KEY_LAUNCHER_DESKTOP_GRID_ROWS);
            mPrefLauncherDesktopGridCols = (ListPreference) findPreference(PREF_KEY_LAUNCHER_DESKTOP_GRID_COLS);

            mPrefVolumeRockerWake = (ListPreference) findPreference(PREF_KEY_VOLUME_ROCKER_WAKE);

            mPrefCatNavbarRingTargets = (PreferenceScreen) findPreference(PREF_CAT_KEY_NAVBAR_RING_TARGETS);
            mPrefNavbarRingTargetsEnable = (SwitchPreference) findPreference(PREF_KEY_NAVBAR_RING_TARGETS_ENABLE);
            mPrefNavbarRingTargetsBgStyle = (ListPreference) findPreference(PREF_KEY_NAVBAR_RING_TARGETS_BG_STYLE);
            mPrefNavbarRingHapticFeedback = (ListPreference) findPreference(PREF_KEY_NAVBAR_RING_HAPTIC_FEEDBACK);
            mPrefNavbarRingTarget = new AppPickerPreference[PREF_KEY_NAVBAR_RING_TARGET.size()];
            for (int i = 0; i < mPrefNavbarRingTarget.length; i++) {
                AppPickerPreference appPref = new AppPickerPreference(getActivity(), null);
                appPref.setKey(PREF_KEY_NAVBAR_RING_TARGET.get(i));
                appPref.setTitle(String.format(
                        getActivity().getString(R.string.pref_navbar_ring_target_title), i + 1));
                appPref.setDialogTitle(appPref.getTitle());
                appPref.setDefaultSummary(getActivity().getString(R.string.app_picker_none));
                appPref.setSummary(getActivity().getString(R.string.app_picker_none));
                mPrefNavbarRingTarget[i] = appPref;
                mPrefCatNavbarRingTargets.addPreference(mPrefNavbarRingTarget[i]);
            }

            mPrefPulseNotificationDelay = (SeekBarPreference) findPreference(PREF_KEY_PULSE_NOTIFICATION_DELAY);

            mPrefCatMiscOther = (PreferenceCategory) findPreference(PREF_CAT_KEY_MISC_OTHER);
            mPrefTorchAutoOff = (SeekBarPreference) findPreference(PREF_KEY_TORCH_AUTO_OFF);

            mPrefBackup = findPreference(PREF_KEY_SETTINGS_BACKUP);
            mPrefRestore = findPreference(PREF_KEY_SETTINGS_RESTORE);

            mPrefTransVerification = (EditTextPreference) findPreference(PREF_KEY_TRANS_VERIFICATION);

            mPrefScreenrecordSize = (ListPreference) findPreference(PREF_KEY_SCREENRECORD_SIZE);

            mPrefCatSignalCluster = (PreferenceScreen) findPreference(PREF_CAT_KEY_SIGNAL_CLUSTER);

            mPrefLedControl = findPreference(PREF_LED_CONTROL);

            mPrefVkVibratePattern = (EditTextPreference) findPreference(PREF_KEY_VK_VIBRATE_PATTERN);
            mPrefVkVibratePattern.setOnPreferenceChangeListener(this);

            // Remove Phone specific preferences on Tablet devices
            if (sSystemProperties.isTablet) {
                mPrefCatStatusbarQs.removePreference(mPrefAutoSwitchQs);
                mPrefCatStatusbarQs.removePreference(mPrefQuickPulldown);
                mPrefCatStatusbarQs.removePreference(mPrefQuickPulldownSize);
            }

            // Filter preferences according to feature availability 
            if (!Utils.hasFlash(getActivity())) {
                mPrefCatHwKeyOthers.removePreference(mPrefHwKeyLockscreenTorch);
                mPrefCatLsSlidingChallenge.removePreference(mPrefLsRingTorch);
                mPrefCatMiscOther.removePreference(mPrefTorchAutoOff);
            }
            if (!Utils.hasVibrator(getActivity())) {
                mPrefCatPhoneTelephony.removePreference(mPrefCallVibrations);
            }
            if (!Utils.hasTelephonySupport(getActivity())) {
                mPrefCatPhone.removePreference(mPrefCatPhoneTelephony);
                mPrefCatMedia.removePreference(mPrefLinkVolumes);
            }
            if (!Utils.isAppInstalled(getActivity(), APP_MESSAGING) && mPrefCatPhoneMessaging != null) {
                mPrefCatPhone.removePreference(mPrefCatPhoneMessaging);
            }
            if (!(Utils.isAppInstalled(getActivity(), APP_GOOGLE_NOW) &&
                    Utils.isAppInstalled(getActivity(), APP_GOOGLE_HOME) ||
                    Utils.isAppInstalled(getActivity(), APP_STOCK_LAUNCHER))) {
                getPreferenceScreen().removePreference(mPrefCatLauncherTweaks);
            }
            if (Utils.isWifiOnly(getActivity())) {
                // Remove preferences that don't apply to wifi-only devices
                getPreferenceScreen().removePreference(mPrefCatPhone);
                mPrefCatQsTileSettings.removePreference(mPrefCatQsNmTileSettings);
                mPrefCatStatusbar.removePreference(mPrefDisableRoamingIndicators);
                mPrefCatStatusbarQs.removePreference(mPrefQsNetworkModeSimSlot);
                mPrefCatNotifDrawerStyle.removePreference(mPrefNotifCarrierText);
                mPrefCatLsOther.removePreference(mPrefLockscreenCarrierText);
           }

            // Remove MTK specific preferences for non-MTK devices
            if (!Utils.isMtkDevice()) {
                mPrefCatStatusbar.removePreference(mPrefDisableDataNetworkTypeIcons);
                mPrefCatStatusbar.removePreference(mPrefDisableRoamingIndicators);
                mPrefCatStatusbarQs.removePreference(mPrefQsNetworkModeSimSlot);
                mPrefCatStatusbarColors.removePreference(mPrefSbIconColorSecondary);
                mPrefCatSignalCluster.removePreference(mPrefSbDaColorSecondary);
            } else {
                // Remove Gemini specific preferences for non-Gemini MTK devices
                if (!sSystemProperties.hasGeminiSupport) {
                    mPrefCatStatusbar.removePreference(mPrefDisableDataNetworkTypeIcons);
                    mPrefCatStatusbar.removePreference(mPrefDisableRoamingIndicators);
                    mPrefCatStatusbarQs.removePreference(mPrefQsNetworkModeSimSlot);
                    mPrefCatStatusbarColors.removePreference(mPrefSbIconColorSecondary);
                    mPrefCatSignalCluster.removePreference(mPrefSbDaColorSecondary);
                }
                mPrefCatQsTileSettings.removePreference(mPrefQsTileBehaviourOverride);
            }

            // Remove preferences not compatible with Lenovo VibeUI ROMs
            if (Utils.hasLenovoVibeUI()) {
                getPreferenceScreen().removePreference(mPrefCatLockscreen);
                mPrefCatStatusbar.removePreference(mPrefCatStatusbarQs);
                mPrefCatNotifDrawerStyle.removePreference(mPrefNotifExpandAll);
            }

            // TODO: rework for KitKat compatibility
            getPreferenceScreen().removePreference(mPrefCatTransparencyManager);
            mPrefCatDisplay.removePreference(mPrefButtonBacklightNotif);

            // Features not relevant for KitKat but keep them for potential future use
            mPrefCatSignalCluster.removePreference(mPrefSbDaColorSecondary);

            // Remove more music volume steps option if necessary
            if (!Utils.shouldAllowMoreVolumeSteps()) {
                mPrefs.edit().putBoolean(PREF_KEY_MUSIC_VOLUME_STEPS, false).commit();
                mPrefCatMedia.removePreference(mPrefMusicVolumeSteps);
            }

            // Remove tiles based on device features
            List<CharSequence> qsEntries = new ArrayList<CharSequence>(Arrays.asList(
                    mQuickSettings.getEntries()));
            List<CharSequence> qsEntryValues = new ArrayList<CharSequence>(Arrays.asList(
                    mQuickSettings.getEntryValues()));
            Set<String> qsPrefs = mPrefs.getStringSet(PREF_KEY_QUICK_SETTINGS, null);
            if (!Utils.hasFlash(getActivity())) {
                qsEntries.remove(getString(R.string.qs_tile_torch));
                qsEntryValues.remove("torch_tileview");
                if (qsPrefs != null && qsPrefs.contains("torch_tileview")) {
                    qsPrefs.remove("torch_tileview");
                }
            }
            if (!Utils.hasGPS(getActivity())) {
                qsEntries.remove(getString(R.string.qs_tile_gps));
                qsEntryValues.remove("gps_tileview");
                qsEntryValues.remove("location_tileview");
                if (Utils.isMtkDevice()) {
                    qsEntries.remove(getString(R.string.qs_tile_gps_alt));
                    qsEntries.remove(getString(R.string.qs_tile_gps_slimkat));
                    qsEntryValues.remove("gps_textview");
                }
                if (qsPrefs != null) {
                    if (qsPrefs.contains("gps_tileview")) qsPrefs.remove("gps_tileview");
                    if (qsPrefs.contains("gps_textview")) qsPrefs.remove("gps_textview");
                    if (qsPrefs.contains("location_textview")) qsPrefs.remove("location_textview");
                }
            }
            if (Utils.isWifiOnly(getActivity())) {
                qsEntries.remove(getString(R.string.qs_tile_mobile_data));
                qsEntries.remove(getString(R.string.qs_tile_network_mode));
                qsEntries.remove(getString(R.string.qs_tile_smart_radio));
                qsEntryValues.remove("data_conn_textview");
                qsEntryValues.remove("network_mode_tileview");
                qsEntryValues.remove("smart_radio_tileview");
                if (qsPrefs != null) {
                    if (qsPrefs.contains("data_conn_textview")) qsPrefs.remove("data_conn_textview");
                    if (qsPrefs.contains("network_mode_tileview")) qsPrefs.remove("network_mode_tileview");
                    if (qsPrefs.contains("smart_radio_tileview")) qsPrefs.remove("smart_radio_tileview");
                }
            }
            if (!Utils.hasNfc(getActivity())) {
                qsEntries.remove(getString(R.string.qs_tile_nfc));
                qsEntryValues.remove("nfc_tileview");
                if (qsPrefs != null && qsPrefs.contains("nfc_tileview")) {
                    qsPrefs.remove("nfc_tileview");
                }
            }
            // and update saved prefs in case it was previously checked in previous versions
            mPrefs.edit().putStringSet(PREF_KEY_QUICK_SETTINGS, qsPrefs).commit();
            mQuickSettings.setEntries(qsEntries.toArray(new CharSequence[qsEntries.size()]));
            mQuickSettings.setEntryValues(qsEntryValues.toArray(new CharSequence[qsEntryValues.size()]));

            // Remove actions for HW keys based on device features
            mPrefHwKeyMenuLongpress.setEntries(R.array.hwkey_action_entries);
            mPrefHwKeyMenuLongpress.setEntryValues(R.array.hwkey_action_values);
            List<CharSequence> actEntries = new ArrayList<CharSequence>(Arrays.asList(
                    mPrefHwKeyMenuLongpress.getEntries()));
            List<CharSequence> actEntryValues = new ArrayList<CharSequence>(Arrays.asList(
                    mPrefHwKeyMenuLongpress.getEntryValues()));
            if (!Utils.hasFlash(getActivity())) {
                actEntries.remove(getString(R.string.hwkey_action_torch));
                actEntryValues.remove("11");
            }
            CharSequence[] actionEntries = actEntries.toArray(new CharSequence[actEntries.size()]);
            CharSequence[] actionEntryValues = actEntryValues.toArray(new CharSequence[actEntryValues.size()]);
            mPrefHwKeyMenuLongpress.setEntries(actionEntries);
            mPrefHwKeyMenuLongpress.setEntryValues(actionEntryValues);
            // other preferences have the exact same entries and entry values
            mPrefHwKeyMenuDoubletap.setEntries(actionEntries);
            mPrefHwKeyMenuDoubletap.setEntryValues(actionEntryValues);
            mPrefHwKeyHomeLongpress.setEntries(actionEntries);
            mPrefHwKeyHomeLongpress.setEntryValues(actionEntryValues);
            mPrefHwKeyHomeDoubletap.setEntries(actionEntries);
            mPrefHwKeyHomeDoubletap.setEntryValues(actionEntryValues);
            mPrefHwKeyBackLongpress.setEntries(actionEntries);
            mPrefHwKeyBackLongpress.setEntryValues(actionEntryValues);
            mPrefHwKeyBackDoubletap.setEntries(actionEntries);
            mPrefHwKeyBackDoubletap.setEntryValues(actionEntryValues);
            mPrefHwKeyRecentsSingletap.setEntries(actionEntries);
            mPrefHwKeyRecentsSingletap.setEntryValues(actionEntryValues);
            mPrefHwKeyRecentsLongpress.setEntries(actionEntries);
            mPrefHwKeyRecentsLongpress.setEntryValues(actionEntryValues);
            mPrefNavbarCustomKeySingletap.setEntries(actionEntries);
            mPrefNavbarCustomKeySingletap.setEntryValues(actionEntryValues);
            mPrefNavbarCustomKeyLongpress.setEntries(actionEntries);
            mPrefNavbarCustomKeyLongpress.setEntryValues(actionEntryValues);
            mPrefNavbarCustomKeyDoubletap.setEntries(actionEntries);
            mPrefNavbarCustomKeyDoubletap.setEntryValues(actionEntryValues);

            // remove unsupported actions for pie keys
            actEntries.remove(getString(R.string.hwkey_action_back));
            actEntryValues.remove(String.valueOf(HWKEY_ACTION_BACK));
            actEntries.remove(getString(R.string.hwkey_action_home));
            actEntryValues.remove(String.valueOf(HWKEY_ACTION_HOME));
            actEntries.remove(getString(R.string.hwkey_action_menu));
            actEntryValues.remove(String.valueOf(HWKEY_ACTION_MENU));
            actEntries.remove(getString(R.string.hwkey_action_recent_apps));
            actEntryValues.remove(String.valueOf(HWKEY_ACTION_RECENT_APPS));
            actionEntries = actEntries.toArray(new CharSequence[actEntries.size()]);
            actionEntryValues = actEntryValues.toArray(new CharSequence[actEntryValues.size()]);
            mPrefPieBackLongpress.setEntries(actionEntries);
            mPrefPieBackLongpress.setEntryValues(actionEntryValues);
            mPrefPieHomeLongpress.setEntries(actionEntries);
            mPrefPieHomeLongpress.setEntryValues(actionEntryValues);
            mPrefPieRecentsLongpress.setEntries(actionEntries);
            mPrefPieRecentsLongpress.setEntryValues(actionEntryValues);
            mPrefPieSearchLongpress.setEntries(actionEntries);
            mPrefPieSearchLongpress.setEntryValues(actionEntryValues);
            mPrefPieMenuLongpress.setEntries(actionEntries);
            mPrefPieMenuLongpress.setEntryValues(actionEntryValues);
            mPrefPieAppLongpress.setEntries(actionEntries);
            mPrefPieAppLongpress.setEntryValues(actionEntryValues);

            setDefaultValues();
        }

        @Override
        public void onResume() {
            super.onResume();

            updatePreferences(null);
            mPrefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);

            if (mTransWebServiceClient != null) {
                mTransWebServiceClient.abortTaskIfRunning();
            }

            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
                mDialog = null;
            }

            super.onPause();
        }

        private void setDefaultValues() {
            boolean value = mPrefs.getBoolean(PREF_KEY_NAVBAR_ENABLE, sSystemProperties.hasNavigationBar);
            mPrefs.edit().putBoolean(PREF_KEY_NAVBAR_ENABLE, value).commit();
            mPrefNavbarEnable.setChecked(value);

            value = mPrefs.getBoolean(PREF_KEY_UNPLUG_TURNS_ON_SCREEN, sSystemProperties.unplugTurnsOnScreen);
            mPrefs.edit().putBoolean(PREF_KEY_UNPLUG_TURNS_ON_SCREEN, value).commit();
            mPrefUnplugTurnsOnScreen.setChecked(value);

            if (!mPrefs.getBoolean(PREF_KEY_PULSE_NOTIFICATION_DELAY + "_set", false)) {
                int delay = Math.min(Math.max(sSystemProperties.defaultNotificationLedOff/1000, 1), 20);
                Editor editor = mPrefs.edit();
                editor.putInt(PREF_KEY_PULSE_NOTIFICATION_DELAY, delay);
                editor.putBoolean(PREF_KEY_PULSE_NOTIFICATION_DELAY + "_set", true);
                editor.commit();
                mPrefPulseNotificationDelay.setDefaultValue(delay);
                mPrefPulseNotificationDelay.setValue(delay);
            }

            if (!sSystemProperties.uuidRegistered) {
                mPrefBackup.setEnabled(false);
                mPrefBackup.setSummary(R.string.wsc_trans_required_summary);
                mPrefRestore.setEnabled(false);
                mPrefRestore.setSummary(R.string.wsc_trans_required_summary);
                if (sSystemProperties.uncTrialCountdown == 0) {
                    mPrefLedControl.setEnabled(false);
                    mPrefLedControl.setSummary(String.format("%s (%s)", mPrefLedControl.getSummary(),
                        getString(R.string.wsc_trans_required_summary)));
                    SharedPreferences uncPrefs = getActivity().getSharedPreferences(
                            "ledcontrol", Context.MODE_WORLD_READABLE);
                    uncPrefs.edit().putBoolean(LedSettings.PREF_KEY_LOCKED, true).commit();
                }
                mPrefs.edit().putString(PREF_KEY_TRANS_VERIFICATION, null).commit();
                mPrefTransVerification.getEditText().setText(null);
            } else {
                SharedPreferences uncPrefs = getActivity().getSharedPreferences(
                        "ledcontrol", Context.MODE_WORLD_READABLE);
                uncPrefs.edit().putBoolean(LedSettings.PREF_KEY_LOCKED, false).commit();
                mPrefTransVerification.setEnabled(false);
                mPrefTransVerification.setSummary(mPrefs.getString(PREF_KEY_TRANS_VERIFICATION,
                        getString(R.string.pref_trans_verification_summary)));
            }

            // fix for potential invalid alpha value stored for data activity indicator color introduced in v2.9.9
            int color = mPrefs.getInt(PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR,
                    getResources().getInteger(R.integer.signal_cluster_data_activity_icon_color));
            if (Color.alpha(color) == 0) {
                color = Color.argb(0xff, Color.red(color), Color.green(color), Color.blue(color));
                mPrefs.edit().putInt(PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR, color).commit();
            }

            WebServiceClient.getAppSignatureHash(getActivity());
        }

        private void updatePreferences(String key) {
            if (key == null || key.equals(PREF_KEY_BATTERY_STYLE)) {
                mBatteryStyle.setSummary(mBatteryStyle.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_LOW_BATTERY_WARNING_POLICY)) {
                mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_LOCKSCREEN_BACKGROUND)) {
                mPrefLockscreenBg.setSummary(mPrefLockscreenBg.getEntry());
                mPrefCatLockscreenBg.removePreference(mPrefLockscreenBgColor);
                mPrefCatLockscreenBg.removePreference(mPrefLockscreenBgImage);
                mPrefCatLockscreenBg.removePreference(mPrefLockscreenBgBlurEffect);
                mPrefCatLockscreenBg.removePreference(mPrefLockscreenBlurIntensity);
                String option = mPrefs.getString(PREF_KEY_LOCKSCREEN_BACKGROUND, LOCKSCREEN_BG_DEFAULT);
                if (!option.equals(LOCKSCREEN_BG_DEFAULT)) {
                    mPrefCatLockscreenBg.addPreference(mPrefLockscreenBgBlurEffect);
                    mPrefCatLockscreenBg.addPreference(mPrefLockscreenBlurIntensity);
                }
                if (option.equals(LOCKSCREEN_BG_COLOR)) {
                    mPrefCatLockscreenBg.addPreference(mPrefLockscreenBgColor);
                } else if (option.equals(LOCKSCREEN_BG_IMAGE)) {
                    mPrefCatLockscreenBg.addPreference(mPrefLockscreenBgImage);
                }
            }

            if (key == null || key.equals(PREF_KEY_LOCKSCREEN_STATUSBAR_CLOCK)) {
                mPrefLockscreenSbClock.setSummary(mPrefLockscreenSbClock.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_MENU_LONGPRESS)) {
                mPrefHwKeyMenuLongpress.setSummary(mPrefHwKeyMenuLongpress.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_MENU_DOUBLETAP)) {
                mPrefHwKeyMenuDoubletap.setSummary(mPrefHwKeyMenuDoubletap.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_HOME_LONGPRESS)) {
                mPrefHwKeyHomeLongpress.setSummary(mPrefHwKeyHomeLongpress.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_BACK_LONGPRESS)) {
                mPrefHwKeyBackLongpress.setSummary(mPrefHwKeyBackLongpress.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_BACK_DOUBLETAP)) {
                mPrefHwKeyBackDoubletap.setSummary(mPrefHwKeyBackDoubletap.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_RECENTS_SINGLETAP)) {
                mPrefHwKeyRecentsSingletap.setSummary(mPrefHwKeyRecentsSingletap.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_RECENTS_LONGPRESS)) {
                mPrefHwKeyRecentsLongpress.setSummary(mPrefHwKeyRecentsLongpress.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_DOUBLETAP_SPEED)) {
                mPrefHwKeyDoubletapSpeed.setSummary(getString(R.string.pref_hwkey_doubletap_speed_summary)
                        + " (" + mPrefHwKeyDoubletapSpeed.getEntry() + ")");
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_KILL_DELAY)) {
                mPrefHwKeyKillDelay.setSummary(getString(R.string.pref_hwkey_kill_delay_summary)
                        + " (" + mPrefHwKeyKillDelay.getEntry() + ")");
            }

            if (key == null || key.equals(PREF_KEY_PHONE_FLIP)) {
                mPrefPhoneFlip.setSummary(getString(R.string.pref_phone_flip_summary)
                        + " (" + mPrefPhoneFlip.getEntry() + ")");
            }

            if (key == null || key.equals(PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE)) {
                mPrefSbIconColor.setEnabled(mPrefSbIconColorEnable.isChecked());
                mPrefSbSignalColorMode.setEnabled(mPrefSbIconColorEnable.isChecked());
                mPrefSbIconColorSecondary.setEnabled(mPrefSbIconColorEnable.isChecked());
            }

            if (key == null || key.equals(PREF_KEY_NOTIF_BACKGROUND)) {
                mPrefNotifBackground.setSummary(mPrefNotifBackground.getEntry());
                mPrefCatNotifDrawerStyle.removePreference(mPrefNotifColor);
                mPrefCatNotifDrawerStyle.removePreference(mPrefNotifColorMode);
                mPrefCatNotifDrawerStyle.removePreference(mPrefNotifImagePortrait);
                mPrefCatNotifDrawerStyle.removePreference(mPrefNotifImageLandscape);
                String option = mPrefs.getString(PREF_KEY_NOTIF_BACKGROUND, NOTIF_BG_DEFAULT);
                if (option.equals(NOTIF_BG_COLOR)) {
                    mPrefCatNotifDrawerStyle.addPreference(mPrefNotifColor);
                    mPrefCatNotifDrawerStyle.addPreference(mPrefNotifColorMode);
                } else if (option.equals(NOTIF_BG_IMAGE)) {
                    mPrefCatNotifDrawerStyle.addPreference(mPrefNotifImagePortrait);
                    mPrefCatNotifDrawerStyle.addPreference(mPrefNotifImageLandscape);
                    mPrefCatNotifDrawerStyle.addPreference(mPrefNotifColorMode);
                }
            }

            if (key == null || key.equals(PREF_KEY_NOTIF_COLOR_MODE)) {
                mPrefNotifColorMode.setSummary(mPrefNotifColorMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_BUTTON_BACKLIGHT_MODE)) {
                mPrefButtonBacklightMode.setSummary(mPrefButtonBacklightMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_PIE_CONTROL_ENABLE)) {
                final int pieMode = 
                        Integer.valueOf(mPrefs.getString(PREF_KEY_PIE_CONTROL_ENABLE, "0"));
                if (pieMode == 0) {
                    if (mPrefPieHwKeysDisabled.isChecked()) {
                        Editor e = mPrefs.edit();
                        e.putBoolean(PREF_KEY_HWKEYS_DISABLE, false);
                        e.commit();
                        mPrefPieHwKeysDisabled.setChecked(false);
                    }
                    mPrefPieHwKeysDisabled.setEnabled(false);
                } else {
                    mPrefPieHwKeysDisabled.setEnabled(true);
                }
                mPrefPieEnabled.setSummary(mPrefPieEnabled.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_RECENTS_CLEAR_ALL)) {
                mPrefRecentClear.setSummary(mPrefRecentClear.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_CLEAR_RECENTS_MODE)) {
                mPrefClearRecentMode.setSummary(mPrefClearRecentMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_RAMBAR)) {
                mPrefRambar.setSummary(mPrefRambar.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_EXPANDED_DESKTOP)) {
                mPrefExpandedDesktop.setSummary(mPrefExpandedDesktop.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_NAVBAR_OVERRIDE)
                    || key.equals(PREF_KEY_NAVBAR_ENABLE)) {
                final boolean override = mPrefs.getBoolean(PREF_KEY_NAVBAR_OVERRIDE, false);
                mPrefNavbarEnable.setEnabled(override);
                mPrefCatNavbarKeys.setEnabled(override && mPrefNavbarEnable.isChecked());
                mPrefCatNavbarRing.setEnabled(override && mPrefNavbarEnable.isChecked());
                mPrefCatNavbarColor.setEnabled(override && mPrefNavbarEnable.isChecked());
                mPrefCatNavbarDimen.setEnabled(override && mPrefNavbarEnable.isChecked());
            }

            if (key == null || key.equals(PREF_KEY_LOCKSCREEN_TARGETS_ENABLE)) {
                final boolean enabled = mPrefs.getBoolean(PREF_KEY_LOCKSCREEN_TARGETS_ENABLE, false);
                for(Preference p : mPrefLockscreenTargetsApp) {
                    p.setEnabled(enabled);
                }
            }

            if (key == null || key.equals(PREF_KEY_NETWORK_MODE_TILE_MODE)) {
                mPrefNetworkModeTileMode.setSummary(mPrefNetworkModeTileMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_QS_NETWORK_MODE_SIM_SLOT)) {
                mPrefQsNetworkModeSimSlot.setSummary(
                        String.format(getString(R.string.pref_qs_network_mode_sim_slot_summary),
                                mPrefQsNetworkModeSimSlot.getEntry()));
            }

            if (Utils.isMtkDevice()) {
                final boolean mtkBatteryPercent = Settings.Secure.getInt(getActivity().getContentResolver(), 
                        ModBatteryStyle.SETTING_MTK_BATTERY_PERCENTAGE, 0) == 1;
                if (mtkBatteryPercent) {
                    mPrefs.edit().putBoolean(PREF_KEY_BATTERY_PERCENT_TEXT, false).commit();
                    mPrefBatteryPercent.setChecked(false);
                    Intent intent = new Intent();
                    intent.setAction(ACTION_PREF_BATTERY_PERCENT_TEXT_CHANGED);
                    intent.putExtra(EXTRA_BATTERY_PERCENT_TEXT, false);
                    getActivity().sendBroadcast(intent);
                }
                mPrefBatteryPercent.setEnabled(!mtkBatteryPercent);
            }

            if (key == null || key.equals(PREF_KEY_STATUSBAR_CLOCK_DOW)) {
                mPrefSbClockDow.setSummary(mPrefSbClockDow.getEntry());
                mPrefSbClockDowSize.setEnabled(Integer.valueOf(
                        mPrefSbClockDow.getValue()) != 0);
            }

            if (key == null || key.equals(PREF_KEY_STATUSBAR_LOCK_POLICY)) {
                mPrefSbLockPolicy.setSummary(mPrefSbLockPolicy.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_DATA_TRAFFIC_POSITION)) {
                mPrefDataTrafficPosition.setSummary(mPrefDataTrafficPosition.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_DATA_TRAFFIC_SIZE)) {
                mPrefDataTrafficSize.setSummary(mPrefDataTrafficSize.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_VOLUME_PANEL_EXPANDABLE)) {
                mPrefVolumePanelAutoexpand.setEnabled(mPrefVolumePanelExpandable.isChecked());
                mPrefVolumePanelFullyExpandable.setEnabled(mPrefVolumePanelExpandable.isChecked());
            }

            if (key == null || key.equals(PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE)) {
                mPrefSbSignalColorMode.setSummary(mPrefSbSignalColorMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_PIE_CONTROL_CUSTOM_KEY)) {
                mPrefPieCustomKey.setSummary(mPrefPieCustomKey.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_CALLER_UNKNOWN_PHOTO_ENABLE)) {
                mPrefCallerUnknownPhoto.setEnabled(mPrefCallerUnknownPhotoEnable.isChecked());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_LOCKSCREEN_TORCH)) {
                mPrefHwKeyLockscreenTorch.setSummary(mPrefHwKeyLockscreenTorch.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_VOLUME_PANEL_TIMEOUT)) {
                mPrefVolumePanelTimeout.setSummary(mPrefVolumePanelTimeout.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_CRT_OFF_EFFECT)) {
                mPrefCrtOff.setSummary(mPrefCrtOff.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_TRANSLUCENT_DECOR)) {
                mPrefTranclucentDecor.setSummary(mPrefTranclucentDecor.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_LAUNCHER_DESKTOP_GRID_ROWS)) {
                mPrefLauncherDesktopGridRows.setSummary(mPrefLauncherDesktopGridRows.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_LAUNCHER_DESKTOP_GRID_COLS)) {
                mPrefLauncherDesktopGridCols.setSummary(mPrefLauncherDesktopGridCols.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_CALLER_FULLSCREEN_PHOTO)) {
                mPrefCallerFullscreenPhoto.setSummary(mPrefCallerFullscreenPhoto.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_VOLUME_ROCKER_WAKE)) {
                mPrefVolumeRockerWake.setSummary(mPrefVolumeRockerWake.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_DATA_TRAFFIC_OMNI_MODE)) {
                mPrefDataTrafficOmniMode.setSummary(mPrefDataTrafficOmniMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_DATA_TRAFFIC_INACTIVITY_MODE)) {
                mPrefDataTrafficInactivityMode.setSummary(mPrefDataTrafficInactivityMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_NAVBAR_CUSTOM_KEY_SINGLETAP)) {
                mPrefNavbarCustomKeySingletap.setSummary(mPrefNavbarCustomKeySingletap.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_NAVBAR_CUSTOM_KEY_LONGPRESS)) {
                mPrefNavbarCustomKeyLongpress.setSummary(mPrefNavbarCustomKeyLongpress.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_NAVBAR_CUSTOM_KEY_DOUBLETAP)) {
                mPrefNavbarCustomKeyDoubletap.setSummary(mPrefNavbarCustomKeyDoubletap.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_NAVBAR_RING_TARGETS_ENABLE)) {
                final boolean enabled = mPrefNavbarRingTargetsEnable.isChecked();
                for (int i = 0; i < mPrefNavbarRingTarget.length; i++) {
                    mPrefNavbarRingTarget[i].setEnabled(enabled);
                }
            }

            if (key == null || key.equals(PREF_KEY_NAVBAR_RING_TARGETS_BG_STYLE)) {
                mPrefNavbarRingTargetsBgStyle.setSummary(mPrefNavbarRingTargetsBgStyle.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_NAVBAR_RING_HAPTIC_FEEDBACK)) {
                mPrefNavbarRingHapticFeedback.setSummary(mPrefNavbarRingHapticFeedback.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING)) {
                mPrefBatteryPercentCharging.setSummary(mPrefBatteryPercentCharging.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_HOME_DOUBLETAP)) {
                mPrefHwKeyHomeDoubletap.setSummary(mPrefHwKeyHomeDoubletap.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_LOCKSCREEN_CARRIER_TEXT)) {
                String carrierText = mPrefLockscreenCarrierText.getText();
                if (carrierText == null || carrierText.isEmpty()) {
                    carrierText = getString(R.string.carrier_text_default);
                } else if (carrierText.trim().isEmpty()) {
                    carrierText = getString(R.string.carrier_text_empty);
                }
                mPrefLockscreenCarrierText.setSummary(carrierText);
            }

            if (key == null || key.equals(PREF_KEY_NOTIF_CARRIER_TEXT)) {
                String carrierText = mPrefNotifCarrierText.getText();
                if (carrierText == null || carrierText.isEmpty()) {
                    carrierText = getString(R.string.carrier_text_default);
                } else if (carrierText.trim().isEmpty()) {
                    carrierText = getString(R.string.carrier_text_empty);
                }
                mPrefNotifCarrierText.setSummary(carrierText);
            }

            if (key == null || key.equals(PREF_KEY_PIE_BACK_LONGPRESS)) {
                mPrefPieBackLongpress.setSummary(mPrefPieBackLongpress.getEntry());
            }
            if (key == null || key.equals(PREF_KEY_PIE_HOME_LONGPRESS)) {
                mPrefPieHomeLongpress.setSummary(mPrefPieHomeLongpress.getEntry());
            }
            if (key == null || key.equals(PREF_KEY_PIE_RECENTS_LONGPRESS)) {
                mPrefPieRecentsLongpress.setSummary(mPrefPieRecentsLongpress.getEntry());
            }
            if (key == null || key.equals(PREF_KEY_PIE_SEARCH_LONGPRESS)) {
                mPrefPieSearchLongpress.setSummary(mPrefPieSearchLongpress.getEntry());
            }
            if (key == null || key.equals(PREF_KEY_PIE_MENU_LONGPRESS)) {
                mPrefPieMenuLongpress.setSummary(mPrefPieMenuLongpress.getEntry());
            }
            if (key == null || key.equals(PREF_KEY_PIE_APP_LONGPRESS)) {
                mPrefPieAppLongpress.setSummary(mPrefPieAppLongpress.getEntry());
            }
            if (key == null || key.equals(PREF_KEY_PIE_LONGPRESS_DELAY)) {
                mPrefPieLongpressDelay.setSummary(mPrefPieLongpressDelay.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_QUICK_SETTINGS_TILE_LABEL_STYLE)) {
                mPrefQsTileLabelStyle.setSummary(mPrefQsTileLabelStyle.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_QUICK_PULLDOWN)) {
                mPrefQuickPulldownSize.setEnabled(!"0".equals(mPrefQuickPulldown.getValue()));
            }

            if (key == null || key.equals(PREF_KEY_SCREENRECORD_SIZE)) {
                mPrefScreenrecordSize.setSummary(mPrefScreenrecordSize.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_DATA_TRAFFIC_MODE)) {
                mPrefDataTrafficMode.setSummary(mPrefDataTrafficMode.getEntry());
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficPosition);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficSize);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficInactivityMode);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficOmniMode);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficOmniShowIcon);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficActiveDlOnly);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficOmniAutohide);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficOmniAutohideTh);
                String mode = mPrefDataTrafficMode.getValue();
                if (mode.equals("SIMPLE")) {
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficActiveDlOnly);
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficPosition);
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficSize);
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficInactivityMode);
                } else if (mode.equals("OMNI")) {
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficActiveDlOnly);
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficPosition);
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficSize);
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficOmniMode);
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficOmniShowIcon);
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficOmniAutohide);
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficOmniAutohideTh);
                }
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            updatePreferences(key);

            Intent intent = new Intent();
            if (key.equals(PREF_KEY_BATTERY_STYLE)) {
                intent.setAction(ACTION_PREF_BATTERY_STYLE_CHANGED);
                int batteryStyle = Integer.valueOf(prefs.getString(PREF_KEY_BATTERY_STYLE, "1"));
                intent.putExtra("batteryStyle", batteryStyle);
            } else if (key.equals(PREF_KEY_BATTERY_PERCENT_TEXT)) {
                intent.setAction(ACTION_PREF_BATTERY_PERCENT_TEXT_CHANGED);
                intent.putExtra(EXTRA_BATTERY_PERCENT_TEXT, prefs.getBoolean(PREF_KEY_BATTERY_PERCENT_TEXT, false));
            } else if (key.equals(PREF_KEY_BATTERY_PERCENT_TEXT_SIZE)) {
                intent.setAction(ACTION_PREF_BATTERY_PERCENT_TEXT_SIZE_CHANGED);
                intent.putExtra(EXTRA_BATTERY_PERCENT_TEXT_SIZE, Integer.valueOf(
                        prefs.getString(PREF_KEY_BATTERY_PERCENT_TEXT_SIZE, "16")));
            } else if (key.equals(PREF_KEY_BATTERY_PERCENT_TEXT_STYLE)) {
                intent.setAction(ACTION_PREF_BATTERY_PERCENT_TEXT_STYLE_CHANGED);
                intent.putExtra(EXTRA_BATTERY_PERCENT_TEXT_STYLE,
                        prefs.getString(PREF_KEY_BATTERY_PERCENT_TEXT_STYLE, "%"));
            } else if (key.equals(PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING)) {
                intent.setAction(ACTION_PREF_BATTERY_PERCENT_TEXT_STYLE_CHANGED);
                intent.putExtra(EXTRA_BATTERY_PERCENT_TEXT_CHARGING, Integer.valueOf(
                        prefs.getString(PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING, "0")));
            } else if (key.equals(PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING_COLOR)) {
                intent.setAction(ACTION_PREF_BATTERY_PERCENT_TEXT_STYLE_CHANGED);
                intent.putExtra(EXTRA_BATTERY_PERCENT_TEXT_CHARGING_COLOR,
                        prefs.getInt(PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING_COLOR, Color.GREEN));
            } else if (key.equals(PREF_KEY_QUICK_SETTINGS)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_PREFS, TileOrderActivity.updateTileList(prefs));
            } else if (key.equals(PREF_KEY_QUICK_SETTINGS_TILES_PER_ROW)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_COLS, Integer.valueOf(
                        prefs.getString(PREF_KEY_QUICK_SETTINGS_TILES_PER_ROW, "3")));
            } else if (key.equals(PREF_KEY_QUICK_SETTINGS_TILE_LABEL_STYLE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_TILE_LABEL_STYLE,
                        prefs.getString(PREF_KEY_QUICK_SETTINGS_TILE_LABEL_STYLE, "DEFAULT"));
            } else if (key.equals(PREF_KEY_QUICK_SETTINGS_HIDE_ON_CHANGE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_HIDE_ON_CHANGE,
                        prefs.getBoolean(PREF_KEY_QUICK_SETTINGS_HIDE_ON_CHANGE, false));
            } else if (key.equals(PREF_KEY_QUICK_SETTINGS_AUTOSWITCH)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_AUTOSWITCH, Integer.valueOf(
                        prefs.getString(PREF_KEY_QUICK_SETTINGS_AUTOSWITCH, "0")));
            } else if (key.equals(PREF_KEY_QUICK_PULLDOWN)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QUICK_PULLDOWN, Integer.valueOf(
                        prefs.getString(PREF_KEY_QUICK_PULLDOWN, "0")));
            } else if (key.equals(PREF_KEY_QUICK_PULLDOWN_SIZE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QUICK_PULLDOWN_SIZE,
                        prefs.getInt(PREF_KEY_QUICK_PULLDOWN_SIZE, 15));
            } else if (key.equals(PREF_KEY_STATUSBAR_BGCOLOR)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_BG_COLOR, prefs.getInt(PREF_KEY_STATUSBAR_BGCOLOR, Color.BLACK));
            } else if (key.equals(PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_ICON_COLOR_ENABLE,
                        prefs.getBoolean(PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_ICON_COLOR)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_ICON_COLOR, prefs.getInt(PREF_KEY_STATUSBAR_ICON_COLOR, 
                        getResources().getInteger(R.integer.COLOR_HOLO_BLUE_LIGHT)));
            } else if (key.equals(PREF_KEY_STATUS_ICON_STYLE)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_ICON_STYLE, Integer.valueOf(
                        prefs.getString(PREF_KEY_STATUS_ICON_STYLE, "1"))); 
            } else if (key.equals(PREF_KEY_STATUSBAR_ICON_COLOR_SECONDARY)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_ICON_COLOR_SECONDARY, 
                        prefs.getInt(PREF_KEY_STATUSBAR_ICON_COLOR_SECONDARY, 
                        getResources().getInteger(R.integer.COLOR_HOLO_BLUE_LIGHT)));
            } else if (key.equals(PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_DATA_ACTIVITY_COLOR,
                        prefs.getInt(PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR, 
                        getResources().getInteger(R.integer.signal_cluster_data_activity_icon_color)));
            } else if (key.equals(PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR_SECONDARY)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_DATA_ACTIVITY_COLOR_SECONDARY,
                        prefs.getInt(PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR_SECONDARY, 
                        getResources().getInteger(R.integer.signal_cluster_data_activity_icon_color)));
            } else if (key.equals(PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_SIGNAL_COLOR_MODE,
                        Integer.valueOf(prefs.getString(PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE, "1")));
            } else if (key.equals(PREF_KEY_TM_STATUSBAR_LAUNCHER)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_TM_SB_LAUNCHER, prefs.getInt(PREF_KEY_TM_STATUSBAR_LAUNCHER, 0));
            } else if (key.equals(PREF_KEY_TM_STATUSBAR_LOCKSCREEN)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_TM_SB_LOCKSCREEN, prefs.getInt(PREF_KEY_TM_STATUSBAR_LOCKSCREEN, 0));
            } else if (key.equals(PREF_KEY_TM_NAVBAR_LAUNCHER)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_TM_NB_LAUNCHER, prefs.getInt(PREF_KEY_TM_NAVBAR_LAUNCHER, 0));
            } else if (key.equals(PREF_KEY_TM_NAVBAR_LOCKSCREEN)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_TM_NB_LOCKSCREEN, prefs.getInt(PREF_KEY_TM_NAVBAR_LOCKSCREEN, 0));
            } else if (key.equals(PREF_KEY_STATUSBAR_CENTER_CLOCK)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CENTER_CLOCK, 
                        prefs.getBoolean(PREF_KEY_STATUSBAR_CENTER_CLOCK, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_DOW)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CLOCK_DOW, Integer.valueOf(
                        prefs.getString(PREF_KEY_STATUSBAR_CLOCK_DOW, "0")));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_DOW_SIZE)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CLOCK_DOW_SIZE, 
                        prefs.getInt(PREF_KEY_STATUSBAR_CLOCK_DOW_SIZE, 70));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_AMPM_HIDE)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_AMPM_HIDE, prefs.getBoolean(
                        PREF_KEY_STATUSBAR_CLOCK_AMPM_HIDE, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_AMPM_SIZE)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_AMPM_SIZE, prefs.getInt(
                        PREF_KEY_STATUSBAR_CLOCK_AMPM_SIZE, 70));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_HIDE)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CLOCK_HIDE, prefs.getBoolean(PREF_KEY_STATUSBAR_CLOCK_HIDE, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_LINK)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CLOCK_LINK, prefs.getString(PREF_KEY_STATUSBAR_CLOCK_LINK, null));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_LONGPRESS_LINK)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CLOCK_LONGPRESS_LINK,
                        prefs.getString(PREF_KEY_STATUSBAR_CLOCK_LONGPRESS_LINK, null));
            } else if (key.equals(PREF_KEY_ALARM_ICON_HIDE)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_ALARM_HIDE, prefs.getBoolean(PREF_KEY_ALARM_ICON_HIDE, false));
            } else if (key.equals(PREF_KEY_VOL_FORCE_MUSIC_CONTROL)) {
                intent.setAction(ACTION_PREF_VOL_FORCE_MUSIC_CONTROL_CHANGED);
                intent.putExtra(EXTRA_VOL_FORCE_MUSIC_CONTROL,
                        prefs.getBoolean(PREF_KEY_VOL_FORCE_MUSIC_CONTROL, false));
            } else if (key.equals(PREF_KEY_VOL_SWAP_KEYS)) {
                intent.setAction(ACTION_PREF_VOL_SWAP_KEYS_CHANGED);
                intent.putExtra(EXTRA_VOL_SWAP_KEYS,
                        prefs.getBoolean(PREF_KEY_VOL_SWAP_KEYS, false));
            } else if (key.equals(PREF_KEY_SAFE_MEDIA_VOLUME)) {
                intent.setAction(ACTION_PREF_SAFE_MEDIA_VOLUME_CHANGED);
                intent.putExtra(EXTRA_SAFE_MEDIA_VOLUME_ENABLED,
                        prefs.getBoolean(PREF_KEY_SAFE_MEDIA_VOLUME, false));
            } else if (key.equals(PREF_KEY_HWKEY_MENU_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_MENU_LONGPRESS_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_MENU_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_MENU_DOUBLETAP)) {
                intent.setAction(ACTION_PREF_HWKEY_MENU_DOUBLETAP_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_MENU_DOUBLETAP, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_HOME_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_HOME_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_HOME_LONGPRESS_KEYGUARD)) {
                intent.setAction(ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED);
                intent.putExtra(EXTRA_HWKEY_HOME_LONGPRESS_KG, prefs.getBoolean(
                        GravityBoxSettings.PREF_KEY_HWKEY_HOME_LONGPRESS_KEYGUARD, false));
            } else if (key.equals(PREF_KEY_HWKEY_HOME_DOUBLETAP_DISABLE)) {
                intent.setAction(ACTION_PREF_HWKEY_HOME_DOUBLETAP_CHANGED);
                intent.putExtra(EXTRA_HWKEY_HOME_DOUBLETAP_DISABLE,
                        prefs.getBoolean(PREF_KEY_HWKEY_HOME_DOUBLETAP_DISABLE, false));
            } else if (key.equals(PREF_KEY_HWKEY_HOME_DOUBLETAP)) {
                intent.setAction(ACTION_PREF_HWKEY_HOME_DOUBLETAP_CHANGED);
                intent.putExtra(EXTRA_HWKEY_HOME_DOUBLETAP, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_HOME_DOUBLETAP, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_BACK_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_BACK_LONGPRESS_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_BACK_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_BACK_DOUBLETAP)) {
                intent.setAction(ACTION_PREF_HWKEY_BACK_DOUBLETAP_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_BACK_DOUBLETAP, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_RECENTS_SINGLETAP)) {
                intent.setAction(ACTION_PREF_HWKEY_RECENTS_SINGLETAP_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_RECENTS_SINGLETAP, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_RECENTS_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_RECENTS_LONGPRESS_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_RECENTS_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_DOUBLETAP_SPEED)) {
                intent.setAction(ACTION_PREF_HWKEY_DOUBLETAP_SPEED_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_DOUBLETAP_SPEED, "400")));
            } else if (key.equals(PREF_KEY_HWKEY_KILL_DELAY)) {
                intent.setAction(ACTION_PREF_HWKEY_KILL_DELAY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_KILL_DELAY, "1000")));
            } else if (key.equals(PREF_KEY_VOLUME_ROCKER_WAKE)) {
                intent.setAction(ACTION_PREF_VOLUME_ROCKER_WAKE_CHANGED);
                intent.putExtra(EXTRA_VOLUME_ROCKER_WAKE,
                        prefs.getString(PREF_KEY_VOLUME_ROCKER_WAKE, "default"));
            } else if (key.equals(PREF_KEY_HWKEY_LOCKSCREEN_TORCH)) {
                intent.setAction(ACTION_PREF_HWKEY_LOCKSCREEN_TORCH_CHANGED);
                intent.putExtra(EXTRA_HWKEY_TORCH, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_LOCKSCREEN_TORCH, "0")));
            } else if (key.equals(PREF_KEY_VOLUME_PANEL_EXPANDABLE)) {
                intent.setAction(ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                intent.putExtra(EXTRA_EXPANDABLE,
                        prefs.getBoolean(PREF_KEY_VOLUME_PANEL_EXPANDABLE, false));
            } else if (key.equals(PREF_KEY_VOLUME_PANEL_FULLY_EXPANDABLE)) {
                intent.setAction(ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                intent.putExtra(EXTRA_EXPANDABLE_FULLY,
                        prefs.getBoolean(PREF_KEY_VOLUME_PANEL_FULLY_EXPANDABLE, false));
            } else if (key.equals(PREF_KEY_VOLUME_PANEL_AUTOEXPAND)) {
                intent.setAction(ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                intent.putExtra(EXTRA_AUTOEXPAND, 
                        prefs.getBoolean(PREF_KEY_VOLUME_PANEL_AUTOEXPAND, false));
            } else if (key.equals(PREF_KEY_VOLUME_ADJUST_MUTE)) {
                intent.setAction(ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                intent.putExtra(EXTRA_MUTED, prefs.getBoolean(PREF_KEY_VOLUME_ADJUST_MUTE, false));
            } else if (key.equals(PREF_KEY_VOLUME_ADJUST_VIBRATE_MUTE)) {
                intent.setAction(ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                intent.putExtra(EXTRA_VIBRATE_MUTED, prefs.getBoolean(PREF_KEY_VOLUME_ADJUST_VIBRATE_MUTE, false));
            } else if (key.equals(PREF_KEY_VOLUME_PANEL_TIMEOUT)) {
                intent.setAction(ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                intent.putExtra(EXTRA_TIMEOUT, Integer.valueOf(
                        prefs.getString(PREF_KEY_VOLUME_PANEL_TIMEOUT, "3000")));
            } else if (key.equals(PREF_KEY_VOLUME_PANEL_TRANSPARENCY)) {
                intent.setAction(ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                intent.putExtra(EXTRA_TRANSPARENCY,
                        prefs.getInt(PREF_KEY_VOLUME_PANEL_TRANSPARENCY, 0));
            } else if (key.equals(PREF_KEY_LINK_VOLUMES)) {
                intent.setAction(ACTION_PREF_LINK_VOLUMES_CHANGED);
                intent.putExtra(EXTRA_LINKED,
                        prefs.getBoolean(PREF_KEY_LINK_VOLUMES, true));
            } else if (key.equals(PREF_KEY_NOTIF_BACKGROUND)) {
                intent.setAction(ACTION_NOTIF_BACKGROUND_CHANGED);
                intent.putExtra(EXTRA_BG_TYPE, prefs.getString(
                        PREF_KEY_NOTIF_BACKGROUND, NOTIF_BG_DEFAULT));
            } else if (key.equals(PREF_KEY_NOTIF_COLOR)) {
                intent.setAction(ACTION_NOTIF_BACKGROUND_CHANGED);
                intent.putExtra(EXTRA_BG_COLOR, prefs.getInt(PREF_KEY_NOTIF_COLOR, Color.BLACK));
            } else if (key.equals(PREF_KEY_NOTIF_COLOR_MODE)) {
                intent.setAction(ACTION_NOTIF_BACKGROUND_CHANGED);
                intent.putExtra(EXTRA_BG_COLOR_MODE, prefs.getString(
                        PREF_KEY_NOTIF_COLOR_MODE, NOTIF_BG_COLOR_MODE_OVERLAY));
            } else if (key.equals(PREF_KEY_NOTIF_BACKGROUND_ALPHA)) {
                intent.setAction(ACTION_NOTIF_BACKGROUND_CHANGED);
                intent.putExtra(EXTRA_BG_ALPHA, prefs.getInt(PREF_KEY_NOTIF_BACKGROUND_ALPHA, 0));
            } else if (key.equals(PREF_KEY_DISABLE_DATA_NETWORK_TYPE_ICONS)) {
                intent.setAction(ACTION_DISABLE_DATA_NETWORK_TYPE_ICONS_CHANGED);
                intent.putExtra(EXTRA_DATA_NETWORK_TYPE_ICONS_DISABLED,
                        prefs.getBoolean(PREF_KEY_DISABLE_DATA_NETWORK_TYPE_ICONS, false));
            } else if (key.equals(PREF_KEY_NOTIF_CARRIER_TEXT)) {
                intent.setAction(ACTION_NOTIF_CARRIER_TEXT_CHANGED);
                intent.putExtra(EXTRA_NOTIF_CARRIER_TEXT,
                        prefs.getString(PREF_KEY_NOTIF_CARRIER_TEXT, null));
            } else if (key.equals(PREF_KEY_NOTIF_EXPAND_ALL)) {
                intent.setAction(ACTION_NOTIF_EXPAND_ALL_CHANGED);
                intent.putExtra(EXTRA_NOTIF_EXPAND_ALL,
                        prefs.getBoolean(PREF_KEY_NOTIF_EXPAND_ALL, false));
            } else if (key.equals(PREF_KEY_DISABLE_ROAMING_INDICATORS)) {
                intent.setAction(ACTION_DISABLE_ROAMING_INDICATORS_CHANGED);
                intent.putExtra(EXTRA_INDICATORS_DISABLED,
                        prefs.getBoolean(PREF_KEY_DISABLE_ROAMING_INDICATORS, false));
            } else if (key.equals(PREF_KEY_PIE_CONTROL_ENABLE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                int mode = Integer.valueOf(prefs.getString(PREF_KEY_PIE_CONTROL_ENABLE, "0"));
                intent.putExtra(EXTRA_PIE_ENABLE, mode);
                if (mode == 0) {
                    intent.putExtra(EXTRA_PIE_HWKEYS_DISABLE, false);
                }
            } else if (key.equals(PREF_KEY_PIE_CONTROL_CUSTOM_KEY)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_CUSTOM_KEY_MODE, Integer.valueOf( 
                        prefs.getString(PREF_KEY_PIE_CONTROL_CUSTOM_KEY, "0")));
            } else if (key.equals(PREF_KEY_PIE_CONTROL_MENU)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_MENU, prefs.getBoolean(PREF_KEY_PIE_CONTROL_MENU, false));
            } else if (key.equals(PREF_KEY_PIE_CONTROL_TRIGGERS)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                String[] triggers = prefs.getStringSet(
                        PREF_KEY_PIE_CONTROL_TRIGGERS, new HashSet<String>()).toArray(new String[0]);
                intent.putExtra(EXTRA_PIE_TRIGGERS, triggers);
            } else if (key.equals(PREF_KEY_PIE_CONTROL_TRIGGER_SIZE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_TRIGGER_SIZE, 
                        prefs.getInt(PREF_KEY_PIE_CONTROL_TRIGGER_SIZE, 5));
            } else if (key.equals(PREF_KEY_PIE_CONTROL_SIZE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_SIZE, prefs.getInt(PREF_KEY_PIE_CONTROL_SIZE, 1000));
            } else if (key.equals(PREF_KEY_HWKEYS_DISABLE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_HWKEYS_DISABLE, prefs.getBoolean(PREF_KEY_HWKEYS_DISABLE, false));
            } else if (key.equals(PREF_KEY_PIE_COLOR_BG)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_COLOR_BG, prefs.getInt(PREF_KEY_PIE_COLOR_BG, 
                        getResources().getColor(R.color.pie_background_color)));
            } else if (key.equals(PREF_KEY_PIE_COLOR_FG)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_COLOR_FG, prefs.getInt(PREF_KEY_PIE_COLOR_FG, 
                        getResources().getColor(R.color.pie_foreground_color)));
            } else if (key.equals(PREF_KEY_PIE_COLOR_OUTLINE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_COLOR_OUTLINE, prefs.getInt(PREF_KEY_PIE_COLOR_OUTLINE, 
                        getResources().getColor(R.color.pie_outline_color)));
            } else if (key.equals(PREF_KEY_PIE_COLOR_SELECTED)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_COLOR_SELECTED, prefs.getInt(PREF_KEY_PIE_COLOR_SELECTED, 
                        getResources().getColor(R.color.pie_selected_color)));
            } else if (key.equals(PREF_KEY_PIE_COLOR_TEXT)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_COLOR_TEXT, prefs.getInt(PREF_KEY_PIE_COLOR_TEXT, 
                        getResources().getColor(R.color.pie_text_color)));
            } else if (key.equals(PREF_KEY_PIE_BACK_LONGPRESS)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_BUTTON, "BACK");
                intent.putExtra(EXTRA_PIE_LONGPRESS_ACTION, Integer.valueOf(
                        prefs.getString(PREF_KEY_PIE_BACK_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_PIE_HOME_LONGPRESS)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_BUTTON, "HOME");
                intent.putExtra(EXTRA_PIE_LONGPRESS_ACTION, Integer.valueOf(
                        prefs.getString(PREF_KEY_PIE_HOME_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_PIE_RECENTS_LONGPRESS)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_BUTTON, "RECENT");
                intent.putExtra(EXTRA_PIE_LONGPRESS_ACTION, Integer.valueOf(
                        prefs.getString(PREF_KEY_PIE_RECENTS_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_PIE_SEARCH_LONGPRESS)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_BUTTON, "SEARCH");
                intent.putExtra(EXTRA_PIE_LONGPRESS_ACTION, Integer.valueOf(
                        prefs.getString(PREF_KEY_PIE_SEARCH_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_PIE_MENU_LONGPRESS)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_BUTTON, "MENU");
                intent.putExtra(EXTRA_PIE_LONGPRESS_ACTION, Integer.valueOf(
                        prefs.getString(PREF_KEY_PIE_MENU_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_PIE_APP_LONGPRESS)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_BUTTON, "APP_LAUNCHER");
                intent.putExtra(EXTRA_PIE_LONGPRESS_ACTION, Integer.valueOf(
                        prefs.getString(PREF_KEY_PIE_APP_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_PIE_SYSINFO_DISABLE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_SYSINFO_DISABLE,
                        prefs.getBoolean(PREF_KEY_PIE_SYSINFO_DISABLE, false));
            } else if (key.equals(PREF_KEY_PIE_LONGPRESS_DELAY)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_LONGPRESS_DELAY, Integer.valueOf(
                        prefs.getString(PREF_KEY_PIE_LONGPRESS_DELAY, "0")));
            } else if (key.equals(PREF_KEY_BUTTON_BACKLIGHT_MODE)) {
                intent.setAction(ACTION_PREF_BUTTON_BACKLIGHT_CHANGED);
                intent.putExtra(EXTRA_BB_MODE, prefs.getString(
                        PREF_KEY_BUTTON_BACKLIGHT_MODE, BB_MODE_DEFAULT));
            } else if (key.equals(PREF_KEY_BUTTON_BACKLIGHT_NOTIFICATIONS)) {
                intent.setAction(ACTION_PREF_BUTTON_BACKLIGHT_CHANGED);
                intent.putExtra(EXTRA_BB_NOTIF, prefs.getBoolean(
                        PREF_KEY_BUTTON_BACKLIGHT_NOTIFICATIONS, false));
            } else if (key.equals(PREF_KEY_QUICKAPP_DEFAULT)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED);
                intent.putExtra(EXTRA_QUICKAPP_DEFAULT, prefs.getString(PREF_KEY_QUICKAPP_DEFAULT, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT1)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED);
                intent.putExtra(EXTRA_QUICKAPP_SLOT1, prefs.getString(PREF_KEY_QUICKAPP_SLOT1, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT2)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED);
                intent.putExtra(EXTRA_QUICKAPP_SLOT2, prefs.getString(PREF_KEY_QUICKAPP_SLOT2, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT3)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED);
                intent.putExtra(EXTRA_QUICKAPP_SLOT3, prefs.getString(PREF_KEY_QUICKAPP_SLOT3, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT4)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED);
                intent.putExtra(EXTRA_QUICKAPP_SLOT4, prefs.getString(PREF_KEY_QUICKAPP_SLOT4, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_DEFAULT_2)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED_2);
                intent.putExtra(EXTRA_QUICKAPP_DEFAULT, prefs.getString(PREF_KEY_QUICKAPP_DEFAULT_2, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT1_2)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED_2);
                intent.putExtra(EXTRA_QUICKAPP_SLOT1, prefs.getString(PREF_KEY_QUICKAPP_SLOT1_2, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT2_2)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED_2);
                intent.putExtra(EXTRA_QUICKAPP_SLOT2, prefs.getString(PREF_KEY_QUICKAPP_SLOT2_2, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT3_2)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED_2);
                intent.putExtra(EXTRA_QUICKAPP_SLOT3, prefs.getString(PREF_KEY_QUICKAPP_SLOT3_2, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT4_2)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED_2);
                intent.putExtra(EXTRA_QUICKAPP_SLOT4, prefs.getString(PREF_KEY_QUICKAPP_SLOT4_2, null));
            } else if (key.equals(PREF_KEY_EXPANDED_DESKTOP)) {
                intent.setAction(ACTION_PREF_EXPANDED_DESKTOP_MODE_CHANGED);
                intent.putExtra(EXTRA_ED_MODE, Integer.valueOf(
                        prefs.getString(PREF_KEY_EXPANDED_DESKTOP, "0")));
            } else if (key.equals(PREF_KEY_NAVBAR_HEIGHT)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_HEIGHT, prefs.getInt(PREF_KEY_NAVBAR_HEIGHT, 100));
            } else if (key.equals(PREF_KEY_NAVBAR_HEIGHT_LANDSCAPE)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_HEIGHT_LANDSCAPE, 
                        prefs.getInt(PREF_KEY_NAVBAR_HEIGHT_LANDSCAPE, 100));
            } else if (key.equals(PREF_KEY_NAVBAR_WIDTH)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_WIDTH, prefs.getInt(PREF_KEY_NAVBAR_WIDTH, 100));
            } else if (key.equals(PREF_KEY_NAVBAR_MENUKEY)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_MENUKEY, prefs.getBoolean(PREF_KEY_NAVBAR_MENUKEY, false));
            } else if (key.equals(PREF_KEY_NAVBAR_CUSTOM_KEY_ENABLE)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                boolean enable = prefs.getBoolean(PREF_KEY_NAVBAR_CUSTOM_KEY_ENABLE, false);
                intent.putExtra(EXTRA_NAVBAR_CUSTOM_KEY_ENABLE, enable);
                if (!enable) {
                    prefs.edit().putBoolean(PREF_KEY_NAVBAR_CUSTOM_KEY_SWAP, false);
                    ((CheckBoxPreference)getPreferenceScreen().findPreference(
                            PREF_KEY_NAVBAR_CUSTOM_KEY_SWAP)).setChecked(false);
                }
            } else if (key.equals(PREF_KEY_NAVBAR_CUSTOM_KEY_SINGLETAP)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_CUSTOM_KEY_SINGLETAP,
                        Integer.valueOf(prefs.getString(PREF_KEY_NAVBAR_CUSTOM_KEY_SINGLETAP, "12")));
            } else if (key.equals(PREF_KEY_NAVBAR_CUSTOM_KEY_LONGPRESS)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_CUSTOM_KEY_LONGPRESS,
                        Integer.valueOf(prefs.getString(PREF_KEY_NAVBAR_CUSTOM_KEY_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_NAVBAR_CUSTOM_KEY_DOUBLETAP)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_CUSTOM_KEY_DOUBLETAP,
                        Integer.valueOf(prefs.getString(PREF_KEY_NAVBAR_CUSTOM_KEY_DOUBLETAP, "0")));
            } else if (key.equals(PREF_KEY_NAVBAR_CUSTOM_KEY_SWAP)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_CUSTOM_KEY_SWAP,
                        prefs.getBoolean(PREF_KEY_NAVBAR_CUSTOM_KEY_SWAP, false));
            } else if (key.equals(PREF_KEY_NAVBAR_SWAP_KEYS)) {
                intent.setAction(ACTION_PREF_NAVBAR_SWAP_KEYS);
            } else if (key.equals(PREF_KEY_NAVBAR_CURSOR_CONTROL)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_CURSOR_CONTROL,
                        prefs.getBoolean(PREF_KEY_NAVBAR_CURSOR_CONTROL, false));
            } else if (key.equals(PREF_KEY_NAVBAR_RING_DISABLE)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_RING_DISABLE,
                        prefs.getBoolean(PREF_KEY_NAVBAR_RING_DISABLE, false));
            } else if (PREF_KEY_NAVBAR_RING_TARGET.contains(key)) {
                intent.setAction(ACTION_PREF_NAVBAR_RING_TARGET_CHANGED);
                intent.putExtra(EXTRA_RING_TARGET_INDEX,
                        PREF_KEY_NAVBAR_RING_TARGET.indexOf(key));
                intent.putExtra(EXTRA_RING_TARGET_APP, prefs.getString(key, null));
            } else if (key.equals(PREF_KEY_NAVBAR_RING_TARGETS_BG_STYLE)) {
                intent.setAction(ACTION_PREF_NAVBAR_RING_TARGET_CHANGED);
                intent.putExtra(EXTRA_RING_TARGET_BG_STYLE,
                        prefs.getString(PREF_KEY_NAVBAR_RING_TARGETS_BG_STYLE, "NONE"));
            } else if (key.equals(PREF_KEY_NAVBAR_RING_HAPTIC_FEEDBACK)) {
                intent.setAction(ACTION_PREF_NAVBAR_RING_TARGET_CHANGED);
                intent.putExtra(EXTRA_RING_HAPTIC_FEEDBACK,
                        prefs.getString(PREF_KEY_NAVBAR_RING_HAPTIC_FEEDBACK, "DEFAULT"));
            } else if (key.equals(PREF_KEY_NAVBAR_CAMERA_KEY_DISABLE)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_CAMERA_KEY_DISABLE,
                        prefs.getBoolean(PREF_KEY_NAVBAR_CAMERA_KEY_DISABLE, false));
            } else if (key.equals(PREF_KEY_NAVBAR_COLOR_ENABLE)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_COLOR_ENABLE,
                        prefs.getBoolean(PREF_KEY_NAVBAR_COLOR_ENABLE, false)); 
            } else if (key.equals(PREF_KEY_NAVBAR_KEY_COLOR)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_KEY_COLOR,
                        prefs.getInt(PREF_KEY_NAVBAR_KEY_COLOR, 
                                getResources().getColor(R.color.navbar_key_color)));
            } else if (key.equals(PREF_KEY_NAVBAR_KEY_GLOW_COLOR)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_KEY_GLOW_COLOR,
                        prefs.getInt(PREF_KEY_NAVBAR_KEY_GLOW_COLOR, 
                                getResources().getColor(R.color.navbar_key_glow_color)));
            } else if (key.equals(PREF_KEY_NAVBAR_BG_COLOR)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_BG_COLOR,
                        prefs.getInt(PREF_KEY_NAVBAR_BG_COLOR, 
                                getResources().getColor(R.color.navbar_bg_color)));
            } else if (PREF_KEY_APP_LAUNCHER_SLOT.contains(key)) {
                intent.setAction(ACTION_PREF_APP_LAUNCHER_CHANGED);
                intent.putExtra(EXTRA_APP_LAUNCHER_SLOT,
                        PREF_KEY_APP_LAUNCHER_SLOT.indexOf(key));
                intent.putExtra(EXTRA_APP_LAUNCHER_APP, prefs.getString(key, null));
            } else if (key.equals(PREF_KEY_STATUSBAR_BRIGHTNESS)) {
                intent.setAction(ACTION_PREF_STATUSBAR_BRIGHTNESS_CHANGED);
                intent.putExtra(EXTRA_SB_BRIGHTNESS, prefs.getBoolean(PREF_KEY_STATUSBAR_BRIGHTNESS, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_DT2S)) {
                intent.setAction(ACTION_PREF_STATUSBAR_DT2S_CHANGED);
                intent.putExtra(EXTRA_SB_DT2S, prefs.getBoolean(PREF_KEY_STATUSBAR_DT2S, false));
            } else if (key.equals(PREF_KEY_NETWORK_MODE_TILE_MODE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_NMT_MODE, Integer.valueOf(
                        prefs.getString(PREF_KEY_NETWORK_MODE_TILE_MODE, "0")));
            } else if (key.equals(PREF_KEY_NETWORK_MODE_TILE_LTE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_NMT_LTE, prefs.getBoolean(PREF_KEY_NETWORK_MODE_TILE_LTE, false));
            } else if (key.equals(PREF_KEY_NETWORK_MODE_TILE_CDMA)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_NMT_CDMA, prefs.getBoolean(PREF_KEY_NETWORK_MODE_TILE_CDMA, false));
            } else if (key.equals(PREF_KEY_RINGER_MODE_TILE_MODE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                Set<String> modes = prefs.getStringSet(PREF_KEY_RINGER_MODE_TILE_MODE,
                        new HashSet<String>(Arrays.asList(new String[] { "0", "1", "2", "3" })));
                List<String> lmodes = new ArrayList<String>(modes);
                Collections.sort(lmodes);
                int[] imodes = new int[lmodes.size()];
                for (int i = 0; i < lmodes.size(); i++) {
                    imodes[i] = Integer.valueOf(lmodes.get(i));
                }
                intent.putExtra(EXTRA_RMT_MODE, imodes);
            } else if (key.equals(PREF_STAY_AWAKE_TILE_MODE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                Set<String> sAModes = prefs.getStringSet(PREF_STAY_AWAKE_TILE_MODE,
                        new HashSet<String>(Arrays.asList(new String[] { "0", "1", "2", "3", "4", "5", "6", "7" })));
                List<String> sALmodes = new ArrayList<String>(sAModes);
                Collections.sort(sALmodes);
                int[] sAImodes = new int[sALmodes.size()];
                for (int i = 0; i < sALmodes.size(); i++) {
                    sAImodes[i] = Integer.valueOf(sALmodes.get(i));
                }
                intent.putExtra(EXTRA_SA_MODE, sAImodes);
            } else if (key.equals(PREF_KEY_QS_TILE_SPAN_DISABLE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_TILE_SPAN_DISABLE,
                        prefs.getBoolean(PREF_KEY_QS_TILE_SPAN_DISABLE, false));
            } else if (key.equals(PREF_KEY_QS_ALARM_SINGLETAP_APP)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_ALARM_SINGLETAP_APP,
                        prefs.getString(PREF_KEY_QS_ALARM_SINGLETAP_APP, null));
            } else if (key.equals(PREF_KEY_QS_ALARM_LONGPRESS_APP)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_ALARM_LONGPRESS_APP,
                        prefs.getString(PREF_KEY_QS_ALARM_LONGPRESS_APP, null));
            } else if (key.equals(PREF_KEY_DISPLAY_ALLOW_ALL_ROTATIONS)) {
                intent.setAction(ACTION_PREF_DISPLAY_ALLOW_ALL_ROTATIONS_CHANGED);
                intent.putExtra(EXTRA_ALLOW_ALL_ROTATIONS, 
                        prefs.getBoolean(PREF_KEY_DISPLAY_ALLOW_ALL_ROTATIONS, false));
            } else if (key.equals(PREF_KEY_QS_NETWORK_MODE_SIM_SLOT)) {
                intent.setAction(ACTION_PREF_QS_NETWORK_MODE_SIM_SLOT_CHANGED);
                intent.putExtra(EXTRA_SIM_SLOT, Integer.valueOf(
                        prefs.getString(PREF_KEY_QS_NETWORK_MODE_SIM_SLOT, "0")));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_MODE)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_MODE, prefs.getString(PREF_KEY_DATA_TRAFFIC_MODE, "OFF"));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_OMNI_MODE)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_OMNI_MODE, prefs.getString(PREF_KEY_DATA_TRAFFIC_OMNI_MODE, "IN_OUT"));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_OMNI_SHOW_ICON)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_OMNI_SHOW_ICON, 
                        prefs.getBoolean(PREF_KEY_DATA_TRAFFIC_OMNI_SHOW_ICON, true));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_OMNI_AUTOHIDE)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_OMNI_AUTOHIDE, 
                        prefs.getBoolean(PREF_KEY_DATA_TRAFFIC_OMNI_AUTOHIDE, false));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_OMNI_AUTOHIDE_TH)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_OMNI_AUTOHIDE_TH, 
                        prefs.getInt(PREF_KEY_DATA_TRAFFIC_OMNI_AUTOHIDE_TH, 10));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_POSITION)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_POSITION, Integer.valueOf(
                        prefs.getString(PREF_KEY_DATA_TRAFFIC_POSITION, "0")));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_SIZE)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_SIZE, Integer.valueOf(
                        prefs.getString(PREF_KEY_DATA_TRAFFIC_SIZE, "14")));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_INACTIVITY_MODE)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_INACTIVITY_MODE, Integer.valueOf(
                        prefs.getString(PREF_KEY_DATA_TRAFFIC_INACTIVITY_MODE, "0")));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_ACTIVE_DL_ONLY)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_ACTIVE_DL_ONLY,
                        prefs.getBoolean(PREF_KEY_DATA_TRAFFIC_ACTIVE_DL_ONLY, false));
            } else if (key.equals(PREF_KEY_SMART_RADIO_NORMAL_MODE)) {
                intent.setAction(ACTION_PREF_SMART_RADIO_CHANGED);
                intent.putExtra(EXTRA_SR_NORMAL_MODE,
                        prefs.getInt(PREF_KEY_SMART_RADIO_NORMAL_MODE, -1));
            } else if (key.equals(PREF_KEY_SMART_RADIO_POWER_SAVING_MODE)) {
                intent.setAction(ACTION_PREF_SMART_RADIO_CHANGED);
                intent.putExtra(EXTRA_SR_POWER_SAVING_MODE,
                        prefs.getInt(PREF_KEY_SMART_RADIO_POWER_SAVING_MODE, -1));
            } else if (key.equals(PREF_KEY_SMART_RADIO_SCREEN_OFF)) {
                intent.setAction(ACTION_PREF_SMART_RADIO_CHANGED);
                intent.putExtra(EXTRA_SR_SCREEN_OFF,
                        prefs.getBoolean(PREF_KEY_SMART_RADIO_SCREEN_OFF, false));
            } else if (key.equals(PREF_KEY_SMART_RADIO_SCREEN_OFF_DELAY)) {
                intent.setAction(ACTION_PREF_SMART_RADIO_CHANGED);
                intent.putExtra(EXTRA_SR_SCREEN_OFF_DELAY,
                        prefs.getInt(PREF_KEY_SMART_RADIO_SCREEN_OFF_DELAY, 0));
            } else if (key.equals(PREF_KEY_SMART_RADIO_IGNORE_LOCKED)) {
                intent.setAction(ACTION_PREF_SMART_RADIO_CHANGED);
                intent.putExtra(EXTRA_SR_IGNORE_LOCKED,
                        prefs.getBoolean(PREF_KEY_SMART_RADIO_IGNORE_LOCKED, true));
            } else if (key.equals(PREF_KEY_SMART_RADIO_MODE_CHANGE_DELAY)) {
                intent.setAction(ACTION_PREF_SMART_RADIO_CHANGED);
                intent.putExtra(EXTRA_SR_MODE_CHANGE_DELAY,
                        prefs.getInt(PREF_KEY_SMART_RADIO_MODE_CHANGE_DELAY, 5));
            } else if (key.equals(PREF_KEY_LOCKSCREEN_BACKGROUND)) {
                intent.setAction(ACTION_PREF_LOCKSCREEN_BG_CHANGED);
                intent.putExtra(EXTRA_LOCKSCREEN_BG,
                        prefs.getString(PREF_KEY_LOCKSCREEN_BACKGROUND, LOCKSCREEN_BG_DEFAULT));
            } else if (key.equals(PREF_KEY_BATTERY_CHARGED_SOUND)) {
                intent.setAction(ACTION_PREF_BATTERY_CHARGED_SOUND_CHANGED);
                intent.putExtra(EXTRA_BATTERY_CHARGED_SOUND,
                        prefs.getBoolean(PREF_KEY_BATTERY_CHARGED_SOUND, false));
            } else if (key.equals(PREF_KEY_CHARGER_PLUGGED_SOUND)) {
                intent.setAction(ACTION_PREF_BATTERY_CHARGED_SOUND_CHANGED);
                intent.putExtra(EXTRA_CHARGER_PLUGGED_SOUND,
                        prefs.getBoolean(PREF_KEY_CHARGER_PLUGGED_SOUND, false));
            } else if (key.equals(PREF_KEY_TRANS_VERIFICATION)) {
                String transId = prefs.getString(key, null);
                if (transId != null && !transId.trim().isEmpty()) {
                    checkTransaction(transId.toUpperCase(Locale.US));
                }
            } else if (key.equals(PREF_KEY_NATIONAL_ROAMING)) {
                intent.setAction(ACTION_PREF_TELEPHONY_CHANGED);
                intent.putExtra(EXTRA_TELEPHONY_NATIONAL_ROAMING,
                        prefs.getBoolean(PREF_KEY_NATIONAL_ROAMING, false));
            } else if (key.equals(PREF_KEY_VK_VIBRATE_PATTERN)) {
                intent.setAction(ACTION_PREF_VK_VIBRATE_PATTERN_CHANGED);
                intent.putExtra(EXTRA_VK_VIBRATE_PATTERN,
                        prefs.getString(PREF_KEY_VK_VIBRATE_PATTERN, null));
            } else if (key.equals(PREF_KEY_FORCE_ENGLISH_LOCALE)) {
                mPrefs.edit().commit();
                intent = new Intent(getActivity(), GravityBoxSettings.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intent);
                System.exit(0);
                return;
            }
            if (intent.getAction() != null) {
                mPrefs.edit().commit();
                getActivity().sendBroadcast(intent);
            }

            if (key.equals(PREF_KEY_BRIGHTNESS_MIN) &&
                    prefs.getInt(PREF_KEY_BRIGHTNESS_MIN, 20) < 20) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.important);
                builder.setMessage(R.string.screen_brightness_min_warning);
                builder.setPositiveButton(android.R.string.ok, null);
                mDialog = builder.create();
                mDialog.show();
            }

            if (rebootKeys.contains(key))
                Toast.makeText(getActivity(), getString(R.string.reboot_required), Toast.LENGTH_SHORT).show();
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            if (pref == mPrefVkVibratePattern) {
                if (newValue == null || ((String)newValue).isEmpty()) return true;
                try {
                    Utils.csvToLongArray((String)newValue);
                } catch (Exception e) {
                    Toast.makeText(getActivity(), getString(R.string.lc_vibrate_pattern_invalid),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen prefScreen, Preference pref) {
            Intent intent = null;

            if (pref == mPrefAboutGb) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_gravitybox)));
            } else if (pref == mPrefAboutGplus) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_gplus)));
            } else if (pref == mPrefAboutXposed) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_xposed)));
            } else if (pref == mPrefAboutDonate) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_donate)));
            } else if (pref == mPrefEngMode) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName(APP_ENGINEERING_MODE, APP_ENGINEERING_MODE_CLASS);
            } else if (pref == mPrefDualSimRinger) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName(APP_DUAL_SIM_RINGER, APP_DUAL_SIM_RINGER_CLASS);
            } else if (pref == mPrefLockscreenBgImage) {
                setCustomLockscreenImage();
                return true;
            } else if (pref == mPrefNotifImagePortrait) {
                setCustomNotifBgPortrait();
                return true;
            } else if (pref == mPrefNotifImageLandscape) {
                setCustomNotifBgLandscape();
                return true;
            } else if (pref == mPrefGbThemeDark) {
                File file = new File(getActivity().getFilesDir() + "/" + FILE_THEME_DARK_FLAG);
                if (mPrefGbThemeDark.isChecked()) {
                    if (!file.exists()) {
                        try {
                            file.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (file.exists()) {
                        file.delete();
                    }
                }
                getActivity().recreate();
            } else if (pref == mPrefQsTileOrder) {
                intent = new Intent(getActivity(), TileOrderActivity.class);
            } else if (pref == mPrefPieColorReset) {
                final Resources res = getResources();
                final int bgColor = res.getColor(R.color.pie_background_color);
                final int fgColor = res.getColor(R.color.pie_foreground_color);
                final int outlineColor = res.getColor(R.color.pie_outline_color);
                final int selectedColor = res.getColor(R.color.pie_selected_color);
                final int textColor = res.getColor(R.color.pie_text_color);
                mPrefPieColorBg.setValue(bgColor);
                mPrefPieColorFg.setValue(fgColor);
                mPrefPieColorOutline.setValue(outlineColor);
                mPrefPieColorSelected.setValue(selectedColor);
                mPrefPieColorText.setValue(textColor);
                Intent pieIntent = new Intent(ACTION_PREF_PIE_CHANGED);
                pieIntent.putExtra(EXTRA_PIE_COLOR_BG, bgColor);
                pieIntent.putExtra(EXTRA_PIE_COLOR_FG, fgColor);
                pieIntent.putExtra(EXTRA_PIE_COLOR_OUTLINE, outlineColor);
                pieIntent.putExtra(EXTRA_PIE_COLOR_SELECTED, selectedColor);
                pieIntent.putExtra(EXTRA_PIE_COLOR_TEXT, textColor);
                getActivity().sendBroadcast(pieIntent);
            } else if (pref == mPrefCallerUnknownPhoto) {
                setCustomCallerImage();
                return true;
            } else if (PREF_CAT_HWKEY_ACTIONS.equals(pref.getKey()) &&
                    !mPrefs.getBoolean(PREF_KEY_NAVBAR_OVERRIDE, false) &&
                    !mPrefs.getBoolean("hw_keys_navbar_warning_shown", false)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.app_name)
                .setMessage(R.string.hwkey_navbar_warning)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mPrefs.edit().putBoolean("hw_keys_navbar_warning_shown", true).commit();
                    }
                });
                mDialog = builder.create();
                mDialog.show();
            } else if (PREF_KEY_SETTINGS_BACKUP.equals(pref.getKey())) {
                SettingsManager.getInstance(getActivity()).backupSettings();
            } else if (PREF_KEY_SETTINGS_RESTORE.equals(pref.getKey())) {
                final SettingsManager sm = SettingsManager.getInstance(getActivity());
                if (sm.isBackupAvailable()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.settings_restore_confirm)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            if (sm.restoreSettings()) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.app_name)
                                .setMessage(R.string.settings_restore_reboot)
                                .setCancelable(false)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        getActivity().finish();
                                    }
                                });
                                mDialog = builder.create();
                                mDialog.show();
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    mDialog = builder.create();
                    mDialog.show();
                } else {
                    Toast.makeText(getActivity(), R.string.settings_restore_no_backup, Toast.LENGTH_SHORT).show();
                }
            } else if (PREF_LED_CONTROL.equals(pref.getKey())) {
                intent = new Intent(getActivity(), LedControlActivity.class);
                intent.putExtra(LedControlActivity.EXTRA_UUID_REGISTERED, sSystemProperties.uuidRegistered);
                intent.putExtra(LedControlActivity.EXTRA_TRIAL_COUNTDOWN, sSystemProperties.uncTrialCountdown);
            }

            if (intent != null) {
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
                return true;
            }

            return super.onPreferenceTreeClick(prefScreen, pref);
        }

        @SuppressWarnings("deprecation")
        private void setCustomLockscreenImage() {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", false);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point displaySize = new Point();
            display.getRealSize(displaySize);
            // Lock screen for tablets visible section are different in landscape/portrait,
            // image need to be cropped correctly, like wallpaper setup for scrolling in background in home screen
            // other wise it does not scale correctly
            if (Utils.isTabletUI(getActivity())) {
                WallpaperManager wpManager = WallpaperManager.getInstance(getActivity());
                int wpWidth = wpManager.getDesiredMinimumWidth();
                int wpHeight = wpManager.getDesiredMinimumHeight();
                float spotlightX = (float) displaySize.x / wpWidth;
                float spotlightY = (float) displaySize.y / wpHeight;
                intent.putExtra("aspectX", wpWidth);
                intent.putExtra("aspectY", wpHeight);
                intent.putExtra("outputX", wpWidth);
                intent.putExtra("outputY", wpHeight);
                intent.putExtra("spotlightX", spotlightX);
                intent.putExtra("spotlightY", spotlightY);
            } else {
                boolean isPortrait = getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_PORTRAIT;
                intent.putExtra("aspectX", isPortrait ? displaySize.x : displaySize.y);
                intent.putExtra("aspectY", isPortrait ? displaySize.y : displaySize.x);
            }
            try {
                wallpaperTemporary.createNewFile();
                wallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(wallpaperTemporary));
                intent.putExtra("return-data", false);
                getActivity().startActivityFromFragment(this, intent, REQ_LOCKSCREEN_BACKGROUND);
            } catch (Exception e) {
                Toast.makeText(getActivity(), getString(
                        R.string.lockscreen_background_result_not_successful),
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        @SuppressWarnings("deprecation")
        private void setCustomNotifBgPortrait() {
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            Rect rect = new Rect();
            Window window = getActivity().getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rect);
            int statusBarHeight = rect.top;
            int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            int titleBarHeight = contentViewTop - statusBarHeight;
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            boolean isPortrait = getResources()
                    .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            intent.putExtra("aspectX", isPortrait ? width : height - titleBarHeight);
            intent.putExtra("aspectY", isPortrait ? height - titleBarHeight : width);
            intent.putExtra("outputX", isPortrait ? width : height);
            intent.putExtra("outputY", isPortrait ? height : width);
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            try {
                wallpaperTemporary.createNewFile();
                wallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(wallpaperTemporary));
                startActivityForResult(intent, REQ_NOTIF_BG_IMAGE_PORTRAIT);
            } catch (Exception e) {
                Toast.makeText(getActivity(), getString(
                        R.string.lockscreen_background_result_not_successful),
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        @SuppressWarnings("deprecation")
        private void setCustomNotifBgLandscape() {
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            Rect rect = new Rect();
            Window window = getActivity().getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rect);
            int statusBarHeight = rect.top;
            int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            int titleBarHeight = contentViewTop - statusBarHeight;
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            boolean isPortrait = getResources()
                  .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            intent.putExtra("aspectX", isPortrait ? height - titleBarHeight : width);
            intent.putExtra("aspectY", isPortrait ? width : height - titleBarHeight);
            intent.putExtra("outputX", isPortrait ? height : width);
            intent.putExtra("outputY", isPortrait ? width : height);
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            try {
                wallpaperTemporary.createNewFile();
                wallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(wallpaperTemporary));
                startActivityForResult(intent, REQ_NOTIF_BG_IMAGE_LANDSCAPE);
            } catch (Exception e) {
                Toast.makeText(getActivity(), getString(
                        R.string.lockscreen_background_result_not_successful),
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        private void setCustomCallerImage() {
            int width = getResources().getDimensionPixelSize(R.dimen.caller_id_photo_width);
            int height = getResources().getDimensionPixelSize(R.dimen.caller_id_photo_height);
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            boolean isPortrait = getResources()
                    .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            intent.putExtra("aspectX", isPortrait ? width : height);
            intent.putExtra("aspectY", isPortrait ? height : width);
            intent.putExtra("outputX", isPortrait ? width : height);
            intent.putExtra("outputY", isPortrait ? height : width);
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            try {
                wallpaperTemporary.createNewFile();
                wallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(wallpaperTemporary));
                startActivityForResult(intent, REQ_CALLER_PHOTO);
            } catch (Exception e) {
                Toast.makeText(getActivity(), getString(
                        R.string.caller_unkown_photo_result_not_successful),
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        public interface ShortcutHandler {
            Intent getCreateShortcutIntent();
            void onHandleShortcut(Intent intent, String name, Bitmap icon);
            void onShortcutCancelled();
        }

        private ShortcutHandler mShortcutHandler;
        public void obtainShortcut(ShortcutHandler handler) {
            if (handler == null) return;

            mShortcutHandler = handler;
            startActivityForResult(mShortcutHandler.getCreateShortcutIntent(), REQ_OBTAIN_SHORTCUT);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQ_LOCKSCREEN_BACKGROUND) {
                if (resultCode == Activity.RESULT_OK) {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.renameTo(wallpaperImage);
                    }
                    wallpaperImage.setReadable(true, false);
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_successful), 
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.delete();
                    }
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_not_successful),
                            Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQ_NOTIF_BG_IMAGE_PORTRAIT) {
                if (resultCode == Activity.RESULT_OK) {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.renameTo(notifBgImagePortrait);
                    }
                    notifBgImagePortrait.setReadable(true, false);
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_successful), 
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.delete();
                    }
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_not_successful),
                            Toast.LENGTH_SHORT).show();
                }
                Intent intent = new Intent(ACTION_NOTIF_BACKGROUND_CHANGED);
                getActivity().sendBroadcast(intent);
            } else if (requestCode == REQ_NOTIF_BG_IMAGE_LANDSCAPE) {
                if (resultCode == Activity.RESULT_OK) {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.renameTo(notifBgImageLandscape);
                    }
                    notifBgImageLandscape.setReadable(true, false);
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_successful), 
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.delete();
                    }
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_not_successful),
                            Toast.LENGTH_SHORT).show();
                }
                Intent intent = new Intent(ACTION_NOTIF_BACKGROUND_CHANGED);
                getActivity().sendBroadcast(intent);
            } else if (requestCode == REQ_CALLER_PHOTO) {
                if (resultCode == Activity.RESULT_OK) {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.renameTo(callerPhotoFile);
                    }
                    callerPhotoFile.setReadable(true, false);
                    Toast.makeText(getActivity(), getString(
                            R.string.caller_unknown_photo_result_successful), 
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.delete();
                    }
                    Toast.makeText(getActivity(), getString(
                            R.string.caller_unkown_photo_result_not_successful),
                            Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQ_OBTAIN_SHORTCUT && mShortcutHandler != null) {
                if (resultCode == Activity.RESULT_OK) {
                    Bitmap b = null;
                    Intent.ShortcutIconResource siRes = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                    if (siRes != null) {
                        try {
                            final Context extContext = getActivity().createPackageContext(
                                    siRes.packageName, Context.CONTEXT_IGNORE_SECURITY);
                            final Resources extRes = extContext.getResources();
                            final int drawableResId = extRes.getIdentifier(siRes.resourceName, "drawable", siRes.packageName);
                            b = BitmapFactory.decodeResource(extRes, drawableResId);
                        } catch (NameNotFoundException e) {
                            //
                        }
                    }
                    if (b == null) {
                        b = (Bitmap)data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
                    }

                    mShortcutHandler.onHandleShortcut(
                            (Intent)data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT),
                            data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME), b);
                } else {
                    mShortcutHandler.onShortcutCancelled();
                }
            }
        }

        private void checkTransaction(String transactionId) {
            mTransWebServiceClient = new WebServiceClient<TransactionResult>(getActivity(),
                    new WebServiceTaskListener<TransactionResult>() {
                        @Override
                        public void onWebServiceTaskCompleted(final TransactionResult result) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.app_name)
                            .setMessage(result.getTransactionStatusMessage())
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    if (result.getTransactionStatus() == TransactionStatus.TRANSACTION_VALID) {
                                        Intent intent = new Intent(SystemPropertyProvider.ACTION_REGISTER_UUID);
                                        intent.putExtra(SystemPropertyProvider.EXTRA_UUID,
                                                SettingsManager.getInstance(getActivity()).getOrCreateUuid());
                                        getActivity().sendBroadcast(intent);
                                        getActivity().finish();
                                    }
                                }
                            });
                            mDialog = builder.create();
                            mDialog.show();
                        }

                        @Override
                        public void onWebServiceTaskCancelled() { 
                            Toast.makeText(getActivity(), R.string.wsc_task_cancelled, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public TransactionResult obtainWebServiceResultInstance() {
                            return new TransactionResult(getActivity());
                        }

                        @Override
                        public void onWebServiceTaskError(TransactionResult result) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.app_name)
                            .setMessage(result.getMessage())
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            mDialog = builder.create();
                            mDialog.show();
                        }
                    });
            RequestParams params = new RequestParams(getActivity());
            params.setAction("checkTransaction");
            params.addParam("transactionId", transactionId);
            mTransWebServiceClient.execute(params);
        }
    }
}
