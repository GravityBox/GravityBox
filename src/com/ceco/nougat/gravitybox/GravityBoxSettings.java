/*
 * Copyright (C) 2017 Peter Gregus for GravityBox Project (C3C076@xda)
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.ceco.nougat.gravitybox.R;
import com.ceco.nougat.gravitybox.ledcontrol.LedMainActivity;
import com.ceco.nougat.gravitybox.ledcontrol.LedSettings;
import com.ceco.nougat.gravitybox.managers.BatteryInfoManager;
import com.ceco.nougat.gravitybox.preference.AppPickerPreference;
import com.ceco.nougat.gravitybox.preference.SeekBarPreference;
import com.ceco.nougat.gravitybox.quicksettings.TileOrderActivity;
import com.ceco.nougat.gravitybox.shortcuts.GoHomeShortcut;
import com.ceco.nougat.gravitybox.shortcuts.ShortcutActivity;
import com.ceco.nougat.gravitybox.webserviceclient.RequestParams;
import com.ceco.nougat.gravitybox.webserviceclient.TransactionResult;
import com.ceco.nougat.gravitybox.webserviceclient.WebServiceClient;
import com.ceco.nougat.gravitybox.webserviceclient.TransactionResult.TransactionStatus;
import com.ceco.nougat.gravitybox.webserviceclient.WebServiceClient.WebServiceTaskListener;

import android.media.Ringtone;
import android.media.RingtoneManager;
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
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest.permission;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Bitmap.CompressFormat;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class GravityBoxSettings extends GravityBoxActivity implements GravityBoxResultReceiver.Receiver {
    public static final String PREF_KEY_QUICK_SETTINGS_ENABLE = "pref_qs_management_enable";
    public static final String PREF_KEY_QUICK_SETTINGS_TILES_PER_ROW = "pref_qs_tiles_per_row2";
    public static final String PREF_KEY_QUICK_SETTINGS_TILE_LABEL_STYLE = "pref_qs_tile_label_style";
    public static final String PREF_KEY_QUICK_SETTINGS_HIDE_ON_CHANGE = "pref_qs_hide_on_change";
    public static final String PREF_KEY_QUICK_SETTINGS_AUTOSWITCH = "pref_auto_switch_qs2";
    public static final String PREF_KEY_QUICK_PULLDOWN = "pref_quick_pulldown";
    public static final String PREF_KEY_QUICK_PULLDOWN_SIZE = "pref_quick_pulldown_size";
    public static final String PREF_KEY_QUICK_SETTINGS_HIDE_BRIGHTNESS = "pref_qs_hide_brightness";
    public static final String PREF_KEY_QS_BRIGHTNESS_ICON = "pref_qs_brightness_icon";
    public static final String PREF_KEY_QS_SCALE_CORRECTION = "pref_qs_scale_correction";
    public static final int QUICK_PULLDOWN_OFF = 0;
    public static final int QUICK_PULLDOWN_RIGHT = 1;
    public static final int QUICK_PULLDOWN_LEFT = 2;

    public static final String PREF_CAT_KEY_BATTERY_SETTINGS = "pref_cat_battery_settings";
    public static final String PREF_CAT_KEY_BATTERY_PERCENT_TEXT = "pref_cat_battery_percent_text";
    public static final String PREF_KEY_BATTERY_STYLE = "pref_battery_style";
    public static final String PREF_KEY_BATTERY_PERCENT_TEXT_STATUSBAR = "pref_battery_percent_text_statusbar";
    public static final String PREF_KEY_BATTERY_PERCENT_TEXT_POSITION = "pref_battery_percent_text_position";
    public static final String PREF_KEY_BATTERY_PERCENT_TEXT_KEYGUARD = "pref_battery_percent_text_keyguard";
    public static final String PREF_KEY_BATTERY_PERCENT_TEXT_SIZE = "pref_battery_percent_text_size";
    public static final String PREF_KEY_BATTERY_PERCENT_TEXT_STYLE = "pref_battery_percent_text_style";
    public static final String PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING = "battery_percent_text_charging";
    public static final String PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING_COLOR = "pref_battery_percent_text_charging_color";
    public static final String PREF_KEY_BATTERY_HIDE_DASH_ICON = "pref_battery_hide_dash_icon";
    public static final int BATTERY_STYLE_STOCK = 1;
    public static final int BATTERY_STYLE_STOCK_PERCENT = 4;
    public static final int BATTERY_STYLE_CIRCLE = 2;
    public static final int BATTERY_STYLE_CIRCLE_PERCENT = 3;
    public static final int BATTERY_STYLE_CIRCLE_DASHED = 6;
    public static final int BATTERY_STYLE_CIRCLE_DASHED_PERCENT = 7;
    public static final int BATTERY_STYLE_NONE = 0;

    public static final String PREF_KEY_LOW_BATTERY_WARNING_POLICY = "pref_low_battery_warning_policy";
    public static final String PREF_KEY_BATTERY_CHARGED_SOUND = "pref_battery_charged_sound2";
    public static final String PREF_KEY_CHARGER_PLUGGED_SOUND = "pref_charger_plugged_sound2";
    public static final String PREF_KEY_CHARGER_PLUGGED_SOUND_WIRELESS = "pref_charger_plugged_sound_wireless";
    public static final String PREF_KEY_CHARGER_UNPLUGGED_SOUND = "pref_charger_unplugged_sound";
    public static final String ACTION_PREF_BATTERY_SOUND_CHANGED = 
            "gravitybox.intent.action.BATTERY_SOUND_CHANGED";
    public static final String EXTRA_BATTERY_SOUND_TYPE = "batterySoundType";
    public static final String EXTRA_BATTERY_SOUND_URI = "batterySoundUri";
    public static final String ACTION_PREF_LOW_BATTERY_WARNING_POLICY_CHANGED = 
            "gravitybox.intent.action.LOW_BATTERY_WARNING_POLICY_CHANGED";
    public static final String EXTRA_LOW_BATTERY_WARNING_POLICY = "lowBatteryWarningPolicy";

    public static final String PREF_KEY_DISABLE_ROAMING_INDICATORS = "pref_disable_roaming_indicators";
    public static final String ACTION_DISABLE_ROAMING_INDICATORS_CHANGED = "gravitybox.intent.action.DISABLE_ROAMING_INDICATORS_CHANGED";
    public static final String EXTRA_INDICATORS_DISABLED = "indicatorsDisabled";
    public static final String PREF_KEY_POWEROFF_ADVANCED = "pref_poweroff_advanced";
    public static final String PREF_KEY_REBOOT_ALLOW_ON_LOCKSCREEN = "pref_reboot_allow_on_lockscreen";
    public static final String PREF_KEY_REBOOT_ALLOW_SOFTREBOOT = "pref_reboot_allow_softreboot";
    public static final String PREF_KEY_REBOOT_CONFIRM_REQUIRED = "pref_reboot_confirm_required";
    public static final String PREF_KEY_POWERMENU_SCREENSHOT = "pref_powermenu_screenshot";
    public static final String PREF_KEY_POWERMENU_SCREENRECORD = "pref_powermenu_screenrecord";
    public static final String PREF_KEY_POWERMENU_DISABLE_ON_LOCKSCREEN = "pref_powermenu_disable_on_lockscreen";
    public static final String PREF_KEY_POWERMENU_EXPANDED_DESKTOP = "pref_powermenu_expanded_desktop";

    public static final String PREF_KEY_VOL_KEY_CURSOR_CONTROL = "pref_vol_key_cursor_control";
    public static final int VOL_KEY_CURSOR_CONTROL_OFF = 0;
    public static final int VOL_KEY_CURSOR_CONTROL_ON = 1;
    public static final int VOL_KEY_CURSOR_CONTROL_ON_REVERSE = 2;

    public static final String PREF_KEY_RAMBAR = "pref_rambar";
    public static final String PREF_KEY_RECENTS_CLEAR_MARGIN_TOP = "pref_recent_clear_margin_top";
    public static final String PREF_KEY_RECENTS_CLEAR_MARGIN_BOTTOM = "pref_recent_clear_margin_bottom";
    public static final int RECENT_CLEAR_OFF = 0;
    public static final int RECENT_CLEAR_TOP_LEFT = 51;
    public static final int RECENT_CLEAR_TOP_RIGHT = 53;
    public static final int RECENT_CLEAR_BOTTOM_LEFT = 83;
    public static final int RECENT_CLEAR_BOTTOM_RIGHT = 85;
    public static final String ACTION_PREF_RECENTS_CHANGED = "gravitybox.intent.action.RECENTS_CHANGED";
    public static final String EXTRA_RECENTS_RAMBAR = "recentsRambar";
    public static final String EXTRA_RECENTS_MARGIN_TOP = "recentsMarginTop";
    public static final String EXTRA_RECENTS_MARGIN_BOTTOM = "recentsMarginBottom";

    public static final String PREF_CAT_KEY_PHONE = "pref_cat_phone";
    public static final String PREF_KEY_CALLER_FULLSCREEN_PHOTO = "pref_caller_fullscreen_photo3";
    public static final String PREF_KEY_CALLER_UNKNOWN_PHOTO_ENABLE = "pref_caller_unknown_photo_enable";
    public static final String PREF_KEY_CALLER_UNKNOWN_PHOTO = "pref_caller_unknown_photo";
    public static final String PREF_KEY_DIALER_SHOW_DIALPAD = "pref_dialer_show_dialpad";
    public static final String PREF_KEY_NATIONAL_ROAMING = "pref_national_roaming";
    public static final String PREF_CAT_KEY_STATUSBAR = "pref_cat_statusbar";
    public static final String PREF_CAT_KEY_STATUSBAR_QS = "pref_cat_statusbar_qs";
    public static final String PREF_CAT_KEY_QS_TILE_SETTINGS = "pref_cat_qs_tile_settings";
    public static final String PREF_CAT_KEY_QS_NM_TILE_SETTINGS = "pref_cat_qs_nm_tile_settings";
    public static final String PREF_CAT_KEY_QS_DND_TILE_SETTINGS = "pref_cat_qs_dnd_tile";
    public static final String PREF_CAT_KEY_QS_LOCATION_TILE_SETTINGS = "pref_cat_qs_location_tile";
    public static final String PREF_CAT_KEY_QS_RM_TILE_SETTINGS = "pref_cat_qs_rm_tile";
    public static final String PREF_CAT_KEY_QS_SA_TILE_SETTINGS = "pref_cat_qs_sa_tile";
    public static final String PREF_CAT_KEY_STATUSBAR_COLORS = "pref_cat_statusbar_colors";
    public static final String PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE = "pref_statusbar_icon_color_enable";
    public static final String PREF_KEY_STATUSBAR_ICON_COLOR = "pref_statusbar_icon_color";
    public static final String PREF_KEY_STATUS_ICON_STYLE = "pref_status_icon_style";
    public static final String PREF_KEY_STATUSBAR_ICON_COLOR_SECONDARY = "pref_statusbar_icon_color_secondary";
    public static final String PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR = "pref_signal_cluster_data_activity_color";
    public static final String PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR_SECONDARY = 
            "pref_signal_cluster_data_activity_color_secondary";
    public static final String PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE = "pref_statusbar_signal_color_mode";
    public static final String PREF_KEY_STATUSBAR_CENTER_CLOCK = "pref_statusbar_center_clock";
    public static final String PREF_KEY_STATUSBAR_CLOCK_SHOW_SECONDS = "pref_statusbar_clock_show_seconds";
    public static final String PREF_KEY_STATUSBAR_CLOCK_DOW = "pref_statusbar_clock_dow2";
    public static final String PREF_KEY_STATUSBAR_CLOCK_DATE = "pref_statusbar_clock_date2";
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
    public static final String PREF_CAT_KEY_CLOCK_SETTINGS = "pref_cat_clock_settings";
    public static final String PREF_CAT_KEY_NOTIF_PANEL_CLOCK = "pref_cat_notif_panel_clock";

    public static final String PREF_CAT_KEY_ABOUT = "pref_cat_about";
    public static final String PREF_KEY_ABOUT_GRAVITYBOX = "pref_about_gb";
    public static final String PREF_KEY_ABOUT_GPLUS = "pref_about_gplus";
    public static final String PREF_KEY_ABOUT_XPOSED = "pref_about_xposed";
    public static final String PREF_KEY_ABOUT_DONATE = "pref_about_donate";
    public static final String PREF_KEY_ABOUT_UNLOCKER = "pref_about_get_unlocker";
    public static final String PREF_KEY_UNPLUG_TURNS_ON_SCREEN = "pref_unplug_turns_on_screen";
    public static final String PREF_KEY_ENGINEERING_MODE = "pref_engineering_mode";
    public static final String APP_MESSAGING = "com.android.mms";
    public static final String APP_STOCK_LAUNCHER = "com.android.launcher3";
    public static final String APP_GOOGLE_HOME = "com.google.android.launcher";
    public static final String APP_GOOGLE_NOW = "com.google.android.googlequicksearchbox";
    public static final String APP_ENGINEERING_MODE = "com.mediatek.engineermode";
    public static final String APP_ENGINEERING_MODE_CLASS = "com.mediatek.engineermode.EngineerMode";
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

    public static final String PREF_CAT_KEY_LOCKSCREEN_OTHER = "pref_cat_lockscreen_other";
    public static final String PREF_KEY_LOCKSCREEN_ROTATION = "pref_lockscreen_rotation2";
    public static final String PREF_KEY_LOCKSCREEN_SHOW_PATTERN_ERROR = "pref_lockscreen_show_pattern_error";
    public static final String PREF_KEY_LOCKSCREEN_MENU_KEY = "pref_lockscreen_menu_key2";
    public static final String PREF_KEY_LOCKSCREEN_QUICK_UNLOCK = "pref_lockscreen_quick_unlock";
    public static final String PREF_KEY_LOCKSCREEN_PIN_LENGTH = "pref_lockscreen_pin_length";
    public static final String PREF_KEY_LOCKSCREEN_DIRECT_UNLOCK = "pref_lockscreen_direct_unlock2";
    public static final String PREF_KEY_LOCKSCREEN_DIRECT_UNLOCK_TRANS_LEVEL = "pref_lockscreen_direct_unlock_trans_level";
    public static final String PREF_KEY_LOCKSCREEN_DIRECT_UNLOCK_POLICY = "pref_lockscreen_direct_unlock_policy";
    public static final String PREF_KEY_LOCKSCREEN_SMART_UNLOCK = "pref_lockscreen_smart_unlock";
    public static final String PREF_KEY_LOCKSCREEN_SMART_UNLOCK_POLICY = "pref_lockscreen_smart_unlock_policy";
    public static final String PREF_KEY_LOCKSCREEN_IMPRINT_MODE = "pref_lockscreen_imprint_mode";
    public static final String PREF_KEY_IMPRINT_VIBE_DISABLE = "pref_imprint_vibe_disable";
    public static final String PREF_KEY_LOCKSCREEN_D2TS = "pref_lockscreen_dt2s";
    public static final String PREF_KEY_LOCKSCREEN_CARRIER_TEXT = "pref_lockscreen_carrier_text";
    public static final String PREF_KEY_LOCKSCREEN_BLEFT_ACTION_CUSTOM = "pref_lockscreen_bleft_action_custom";
    public static final String PREF_KEY_LOCKSCREEN_BRIGHT_ACTION_CUSTOM = "pref_lockscreen_bright_action_custom";
    public static final String PREF_KEY_LOCKSCREEN_BOTTOM_ACTIONS_HIDE = "pref_lockscreen_bottom_actions_hide";
    public static final String PREF_KEY_LOCKSCREEN_PIN_SCRAMBLE = "pref_lockscreen_pin_sramble";
    public static final String ACTION_LOCKSCREEN_SETTINGS_CHANGED = "gravitybox.intent.action.LOCKSCREEN_SETTINGS_CHANGED";

    public static final String PREF_CAT_KEY_LOCKSCREEN_SHORTCUTS = "pref_cat_lockscreen_shortcuts";
    public static final List<String> PREF_KEY_LOCKSCREEN_SHORTCUT = new ArrayList<String>(Arrays.asList(
            "pref_lockscreen_shortcut0", "pref_lockscreen_shortcut1", "pref_lockscreen_shortcut2",
            "pref_lockscreen_shortcut3", "pref_lockscreen_shortcut4", "pref_lockscreen_shortcut5"));
    public static final String PREF_KEY_LOCKSCREEN_SHORTCUT_SAFE_LAUNCH = "pref_lockscreen_shortcuts_safe_launch";
    public static final String PREF_KEY_LOCKSCREEN_SHORTCUT_SHOW_BADGES = "pref_lockscreen_shortcuts_show_badges";
    public static final String ACTION_PREF_LOCKSCREEN_SHORTCUT_CHANGED = "gravitybox.intent.action.LOCKSCREEN_SHORTCUT_CHANGED";
    public static final String EXTRA_LS_SHORTCUT_SLOT = "lockscreenShortcutSlot";
    public static final String EXTRA_LS_SHORTCUT_VALUE = "lockscreenShortcutValue";
    public static final String EXTRA_LS_SAFE_LAUNCH = "lockscreenShortcutSafeLaunch";
    public static final String EXTRA_LS_SHOW_BADGES = "lockscreenShortcutShowBadges";

    public static final String PREF_CAT_KEY_POWER = "pref_cat_power";
    public static final String PREF_CAT_KEY_POWER_MENU = "pref_cat_power_menu";
    public static final String PREF_CAT_KEY_POWER_OTHER = "pref_cat_power_other";
    public static final String PREF_KEY_FLASHING_LED_DISABLE = "pref_flashing_led_disable";
    public static final String PREF_KEY_CHARGING_LED = "pref_charging_led";
    public static final String ACTION_BATTERY_LED_CHANGED = "gravitybox.intent.action.BATTERY_LED_CHANGED";
    public static final String EXTRA_BLED_FLASHING_DISABLED = "batteryLedFlashingDisabled";
    public static final String EXTRA_BLED_CHARGING = "batteryLedCharging";

    public static final String PREF_KEY_BATTERY_SAVER_INDICATION_DISABLE = "pref_battery_saver_indication_disable";
    public static final String ACTION_BATTERY_SAVER_CHANGED = "gravitybox.intent.action.BATTERY_SAVER_CHANGED";
    public static final String EXTRA_BS_INDICATION_DISABLE = "batterySaverIndicationDisable";

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
    public static final String PREF_KEY_TRANSLUCENT_DECOR = "pref_translucent_decor";

    public static final String PREF_CAT_KEY_MEDIA = "pref_cat_media";
    public static final String PREF_KEY_VOL_MUSIC_CONTROLS = "pref_vol_music_controls";
    public static final String PREF_KEY_MUSIC_VOLUME_STEPS = "pref_music_volume_steps";
    public static final String PREF_KEY_MUSIC_VOLUME_STEPS_VALUE = "pref_music_volume_steps_value";
    public static final String PREF_KEY_VOL_FORCE_MUSIC_CONTROL = "pref_vol_force_music_control";
    public static final String PREF_KEY_SAFE_MEDIA_VOLUME = "pref_safe_media_volume2";
    public static final String PREF_KEY_VOL_SWAP_KEYS = "pref_vol_swap_keys";
    public static final String PREF_KEY_VOLUME_PANEL_AUTOEXPAND = "pref_volume_panel_autoexpand";
    public static final String PREF_KEY_VOLUME_ADJUST_VIBRATE_MUTE = "pref_volume_adjust_vibrate_mute";
    public static final String PREF_KEY_VOLUME_PANEL_TIMEOUT = "pref_volume_panel_timeout2";
    public static final String ACTION_PREF_VOLUME_PANEL_MODE_CHANGED = "gravitybox.intent.action.VOLUME_PANEL_MODE_CHANGED";
    public static final String EXTRA_AUTOEXPAND = "autoExpand";
    public static final String EXTRA_VIBRATE_MUTED = "vibrate_muted";
    public static final String EXTRA_TIMEOUT = "timeout";
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
    public static final String PREF_KEY_HWKEY_MENU_SINGLETAP = "pref_hwkey_menu_singletap";
    public static final String PREF_KEY_HWKEY_MENU_LONGPRESS = "pref_hwkey_menu_longpress";
    public static final String PREF_KEY_HWKEY_MENU_DOUBLETAP = "pref_hwkey_menu_doubletap";
    public static final String PREF_CAT_HWKEY_HOME = "pref_cat_hwkey_home";
    public static final String PREF_KEY_HWKEY_HOME_LONGPRESS = "pref_hwkey_home_longpress";
    public static final String PREF_KEY_HWKEY_HOME_DOUBLETAP_DISABLE = "pref_hwkey_home_doubletap_disable";
    public static final String PREF_KEY_HWKEY_HOME_DOUBLETAP = "pref_hwkey_home_doubletap";
    public static final String PREF_KEY_HWKEY_HOME_LONGPRESS_KEYGUARD = "pref_hwkey_home_longpress_keyguard";
    public static final String PREF_CAT_HWKEY_BACK = "pref_cat_hwkey_back";
    public static final String PREF_KEY_HWKEY_BACK_SINGLETAP = "pref_hwkey_back_singletap";
    public static final String PREF_KEY_HWKEY_BACK_LONGPRESS = "pref_hwkey_back_longpress";
    public static final String PREF_KEY_HWKEY_BACK_DOUBLETAP = "pref_hwkey_back_doubletap";
    public static final String PREF_CAT_HWKEY_RECENTS = "pref_cat_hwkey_recents";
    public static final String PREF_KEY_HWKEY_RECENTS_SINGLETAP = "pref_hwkey_recents_singletap";
    public static final String PREF_KEY_HWKEY_RECENTS_LONGPRESS = "pref_hwkey_recents_longpress";
    public static final String PREF_KEY_HWKEY_RECENTS_DOUBLETAP = "pref_hwkey_recents_doubletap";
    public static final String PREF_KEY_HWKEY_CUSTOM_APP = "pref_hwkey_custom_app";
    public static final String PREF_KEY_HWKEY_DOUBLETAP_SPEED = "pref_hwkey_doubletap_speed";
    public static final String PREF_KEY_HWKEY_KILL_DELAY = "pref_hwkey_kill_delay";
    public static final String PREF_CAT_HWKEY_VOLUME = "pref_cat_hwkey_volume";
    public static final String PREF_KEY_VOLUME_ROCKER_WAKE = "pref_volume_rocker_wake";
    public static final String PREF_KEY_VOLUME_ROCKER_WAKE_ALLOW_MUSIC = "pref_volume_rocker_wake_allow_music";
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
    public static final int HWKEY_ACTION_INAPP_SEARCH = 24;
    public static final int HWKEY_ACTION_CUSTOM_APP = 25;
    public static final int HWKEY_DOUBLETAP_SPEED_DEFAULT = 400;
    public static final int HWKEY_KILL_DELAY_DEFAULT = 1000;
    public static final int HWKEY_TORCH_DISABLED = 0;
    public static final int HWKEY_TORCH_HOME_LONGPRESS = 1;
    public static final int HWKEY_TORCH_POWER_LONGPRESS = 2;
    public static final String ACTION_PREF_HWKEY_CHANGED = "gravitybox.intent.action.HWKEY_CHANGED";
    public static final String ACTION_PREF_HWKEY_DOUBLETAP_SPEED_CHANGED = "gravitybox.intent.action.HWKEY_DOUBLETAP_SPEED_CHANGED";
    public static final String ACTION_PREF_HWKEY_KILL_DELAY_CHANGED = "gravitybox.intent.action.HWKEY_KILL_DELAY_CHANGED";
    public static final String ACTION_PREF_VOLUME_ROCKER_WAKE_CHANGED = "gravitybox.intent.action.VOLUME_ROCKER_WAKE_CHANGED";
    public static final String ACTION_PREF_HWKEY_LOCKSCREEN_TORCH_CHANGED = "gravitybox.intent.action.HWKEY_LOCKSCREEN_TORCH_CHANGED";
    public static final String ACTION_PREF_VK_VIBRATE_PATTERN_CHANGED = "gravitybox.intent.action.VK_VIBRATE_PATTERN_CHANGED";
    public static final String EXTRA_HWKEY_KEY = "hwKeyKey";
    public static final String EXTRA_HWKEY_VALUE = "hwKeyValue";
    public static final String EXTRA_HWKEY_CUSTOM_APP = "hwKeyCustomApp";
    public static final String EXTRA_HWKEY_HOME_DOUBLETAP_DISABLE = "hwKeyHomeDoubletapDisable";
    public static final String EXTRA_HWKEY_HOME_LONGPRESS_KG = "hwKeyHomeLongpressKeyguard";
    public static final String EXTRA_VOLUME_ROCKER_WAKE = "volumeRockerWake";
    public static final String EXTRA_VOLUME_ROCKER_WAKE_ALLOW_MUSIC = "volumeRockerWakeAllowMusic";
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
    public static final String PREF_KEY_NOTIF_IMAGE_PORTRAIT = "pref_notif_image_portrait";
    public static final String PREF_KEY_NOTIF_IMAGE_LANDSCAPE = "pref_notif_image_landscape";
    public static final String PREF_KEY_NOTIF_BACKGROUND_ALPHA = "pref_notif_background_alpha";
    public static final String PREF_KEY_NOTIF_EXPAND_ALL = "pref_notif_expand_all";
    public static final String NOTIF_BG_DEFAULT = "default";
    public static final String NOTIF_BG_COLOR = "color";
    public static final String NOTIF_BG_IMAGE = "image";
    public static final String ACTION_NOTIF_BACKGROUND_CHANGED = "gravitybox.intent.action.NOTIF_BACKGROUND_CHANGED";
    public static final String ACTION_NOTIF_EXPAND_ALL_CHANGED = "gravitybox.intent.action.NOTIF_EXPAND_ALL_CHANGED";
    public static final String EXTRA_BG_TYPE = "bgType";
    public static final String EXTRA_BG_COLOR = "bgColor";
    public static final String EXTRA_BG_ALPHA = "bgAlpha";
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
    public static final String PREF_KEY_PIE_MIRRORED_KEYS = "pref_pie_control_mirrored_keys";
    public static final String PREF_KEY_PIE_CENTER_TRIGGER = "pref_pie_control_center_trigger";
    public static final String PREF_KEY_PIE_TRIGIND = "pref_pie_control_trigind";
    public static final String PREF_KEY_PIE_TRIGIND_COLOR = "pref_pie_control_trigind_color";
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
    public static final String EXTRA_PIE_SYSINFO_DISABLE = "pieSysinfoDisable";
    public static final String EXTRA_PIE_LONGPRESS_DELAY = "pieLongpressDelay";
    public static final String EXTRA_PIE_MIRRORED_KEYS = "pieMirroredKeys";
    public static final String EXTRA_PIE_CENTER_TRIGGER = "pieCenterTrigger";
    public static final String EXTRA_PIE_TRIGIND = "pieTrigind";
    public static final String EXTRA_PIE_TRIGIND_COLOR = "pieTrigindColor";

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
    public static final String PREF_KEY_QUICKAPP_DEFAULT_3 = "pref_quickapp_default_3";
    public static final String PREF_KEY_QUICKAPP_SLOT1_3 = "pref_quickapp_slot1_3";
    public static final String PREF_KEY_QUICKAPP_SLOT2_3 = "pref_quickapp_slot2_3";
    public static final String PREF_KEY_QUICKAPP_SLOT3_3 = "pref_quickapp_slot3_3";
    public static final String PREF_KEY_QUICKAPP_SLOT4_3 = "pref_quickapp_slot4_3";
    public static final String PREF_KEY_QUICKAPP_DEFAULT_4 = "pref_quickapp_default_4";
    public static final String PREF_KEY_QUICKAPP_SLOT1_4 = "pref_quickapp_slot1_4";
    public static final String PREF_KEY_QUICKAPP_SLOT2_4 = "pref_quickapp_slot2_4";
    public static final String PREF_KEY_QUICKAPP_SLOT3_4 = "pref_quickapp_slot3_4";
    public static final String PREF_KEY_QUICKAPP_SLOT4_4 = "pref_quickapp_slot4_4";
    public static final String ACTION_PREF_QUICKAPP_CHANGED = "gravitybox.intent.action.QUICKAPP_CHANGED";
    public static final String ACTION_PREF_QUICKAPP_CHANGED_2 = "gravitybox.intent.action.QUICKAPP_CHANGED_2";
    public static final String ACTION_PREF_QUICKAPP_CHANGED_3 = "gravitybox.intent.action.QUICKAPP_CHANGED_3";
    public static final String ACTION_PREF_QUICKAPP_CHANGED_4 = "gravitybox.intent.action.QUICKAPP_CHANGED_4";
    public static final String EXTRA_QUICKAPP_DEFAULT = "quickAppDefault";
    public static final String EXTRA_QUICKAPP_SLOT1 = "quickAppSlot1";
    public static final String EXTRA_QUICKAPP_SLOT2 = "quickAppSlot2";
    public static final String EXTRA_QUICKAPP_SLOT3 = "quickAppSlot3";
    public static final String EXTRA_QUICKAPP_SLOT4 = "quickAppSlot4";

    public static final String PREF_KEY_GB_THEME_DARK = "pref_gb_theme_dark";
    public static final String FILE_THEME_DARK_FLAG = "theme_dark";

    public static final String ACTION_PREF_BATTERY_STYLE_CHANGED = "gravitybox.intent.action.BATTERY_STYLE_CHANGED";
    public static final String EXTRA_BATTERY_STYLE = "batteryStyle";
    public static final String EXTRA_HIDE_DASH = "hideDash";
    public static final String ACTION_PREF_BATTERY_PERCENT_TEXT_CHANGED =
            "gravitybox.intent.action.BATTERY_PERCENT_TEXT_CHANGED";
    public static final String EXTRA_BATTERY_PERCENT_TEXT_STATUSBAR = "batteryPercentTextSb";
    public static final String EXTRA_BATTERY_PERCENT_TEXT_KEYGUARD = "batteryPercentTextKg";
    public static final String ACTION_PREF_BATTERY_PERCENT_TEXT_SIZE_CHANGED =
            "gravitybox.intent.action.BATTERY_PERCENT_TEXT_SIZE_CHANGED";
    public static final String EXTRA_BATTERY_PERCENT_TEXT_SIZE = "batteryPercentTextSize";
    public static final String ACTION_PREF_BATTERY_PERCENT_TEXT_STYLE_CHANGED =
            "gravitybox.intent.action.BATTERY_PERCENT_TEXT_SIZE_CHANGED";
    public static final String EXTRA_BATTERY_PERCENT_TEXT_STYLE = "batteryPercentTextStyle";
    public static final String EXTRA_BATTERY_PERCENT_TEXT_CHARGING = "batteryPercentTextCharging";
    public static final String EXTRA_BATTERY_PERCENT_TEXT_CHARGING_COLOR = "batteryPercentTextChargingColor";

    public static final String ACTION_PREF_STATUSBAR_COLOR_CHANGED = "gravitybox.intent.action.STATUSBAR_COLOR_CHANGED";
    public static final String EXTRA_SB_ICON_COLOR_ENABLE = "iconColorEnable";
    public static final String EXTRA_SB_ICON_COLOR = "iconColor";
    public static final String EXTRA_SB_ICON_STYLE = "iconStyle";
    public static final String EXTRA_SB_ICON_COLOR_SECONDARY = "iconColorSecondary";
    public static final String EXTRA_SB_DATA_ACTIVITY_COLOR = "dataActivityColor";
    public static final String EXTRA_SB_DATA_ACTIVITY_COLOR_SECONDARY = "dataActivityColorSecondary";
    public static final String EXTRA_SB_SIGNAL_COLOR_MODE = "signalColorMode";

    public static final String ACTION_PREF_QUICKSETTINGS_CHANGED = "gravitybox.intent.action.QUICKSETTINGS_CHANGED";
    public static final String EXTRA_QS_COLS = "qsCols";
    public static final String EXTRA_QS_AUTOSWITCH = "qsAutoSwitch";
    public static final String EXTRA_QUICK_PULLDOWN = "quickPulldown";
    public static final String EXTRA_QUICK_PULLDOWN_SIZE = "quickPulldownSize";
    public static final String EXTRA_QS_TILE_STYLE = "qsTileStyle";
    public static final String EXTRA_QS_HIDE_ON_CHANGE = "qsHideOnChange";
    public static final String EXTRA_QS_TILE_LABEL_STYLE = "qsTileLabelStyle";
    public static final String EXTRA_QS_HIDE_BRIGHTNESS = "qsHideBrightness";
    public static final String EXTRA_QS_BRIGHTNESS_ICON = "qsBrightnessIcon";
    public static final String EXTRA_QS_SCALE_CORRECTION = "qsScaleCorrection";

    public static final String ACTION_PREF_CLOCK_CHANGED = "gravitybox.intent.action.CENTER_CLOCK_CHANGED";
    public static final String EXTRA_CENTER_CLOCK = "centerClock";
    public static final String EXTRA_CLOCK_SHOW_SECONDS = "clockShowSeconds";
    public static final String EXTRA_CLOCK_DOW = "clockDow";
    public static final String EXTRA_CLOCK_DOW_SIZE = "clockDowSize";
    public static final String EXTRA_CLOCK_DATE = "clockDate";
    public static final String EXTRA_AMPM_HIDE = "ampmHide";
    public static final String EXTRA_AMPM_SIZE = "ampmSize";
    public static final String EXTRA_CLOCK_HIDE = "clockHide";
    public static final String EXTRA_CLOCK_LINK = "clockLink";
    public static final String EXTRA_CLOCK_LONGPRESS_LINK = "clockLongpressLink";
    public static final String EXTRA_ALARM_HIDE = "alarmHide";

    public static final String PREF_CAT_KEY_NAVBAR_KEYS = "pref_cat_navbar_keys";
    public static final String PREF_CAT_KEY_NAVBAR_COLOR = "pref_cat_navbar_color";
    public static final String PREF_CAT_KEY_NAVBAR_DIMEN = "pref_cat_navbar_dimen";
    public static final String PREF_KEY_NAVBAR_OVERRIDE = "pref_navbar_override";
    public static final String PREF_KEY_NAVBAR_ENABLE = "pref_navbar_enable";
    public static final String PREF_KEY_NAVBAR_HEIGHT = "pref_navbar_height";
    public static final String PREF_KEY_NAVBAR_HEIGHT_LANDSCAPE = "pref_navbar_height_landscape";
    public static final String PREF_KEY_NAVBAR_WIDTH = "pref_navbar_width";
    public static final String PREF_KEY_NAVBAR_MENUKEY = "pref_navbar_menukey";
    public static final String PREF_KEY_NAVBAR_HIDE_IME = "pref_navbar_hide_ime";
    public static final String PREF_CAT_KEY_NAVBAR_CUSTOM_KEY = "pref_cat_navbar_custom_key";
    public static final String PREF_KEY_NAVBAR_CUSTOM_KEY_ENABLE = "pref_navbar_custom_key_enable";
    public static final String PREF_KEY_NAVBAR_CUSTOM_KEY_SINGLETAP = "pref_navbar_custom_key_singletap";
    public static final String PREF_KEY_NAVBAR_CUSTOM_KEY_LONGPRESS = "pref_navbar_custom_key_longpress";
    public static final String PREF_KEY_NAVBAR_CUSTOM_KEY_DOUBLETAP = "pref_navbar_custom_key_doubletap";
    public static final String PREF_KEY_NAVBAR_CUSTOM_KEY_SWAP = "pref_navbar_custom_key_swap";
    public static final String PREF_KEY_NAVBAR_CUSTOM_KEY_ICON_STYLE = "pref_navbar_custom_key_icon_style";
    public static final String PREF_KEY_NAVBAR_CUSTOM_KEY_IMAGE = "pref_navbar_custom_key_image";
    public static final String PREF_KEY_NAVBAR_SWAP_KEYS = "pref_navbar_swap_keys";
    public static final String PREF_KEY_NAVBAR_CURSOR_CONTROL = "pref_navbar_cursor_control";
    public static final String PREF_KEY_NAVBAR_COLOR_ENABLE = "pref_navbar_color_enable";
    public static final String PREF_KEY_NAVBAR_KEY_COLOR = "pref_navbar_key_color";
    public static final String PREF_KEY_NAVBAR_KEY_GLOW_COLOR = "pref_navbar_key_glow_color";
    public static final String PREF_KEY_NAVBAR_AUTOFADE_KEYS = "pref_navbar_autofade_keys";
    public static final String PREF_KEY_NAVBAR_AUTOFADE_SHOW_KEYS = "pref_navbar_autofade_show_keys";
    public static final String ACTION_PREF_NAVBAR_CHANGED = "gravitybox.intent.action.ACTION_NAVBAR_CHANGED";
    public static final String ACTION_PREF_NAVBAR_SWAP_KEYS = "gravitybox.intent.action.ACTION_NAVBAR_SWAP_KEYS";
    public static final String EXTRA_NAVBAR_HEIGHT = "navbarHeight";
    public static final String EXTRA_NAVBAR_HEIGHT_LANDSCAPE = "navbarHeightLandscape";
    public static final String EXTRA_NAVBAR_WIDTH = "navbarWidth";
    public static final String EXTRA_NAVBAR_MENUKEY = "navbarMenukey";
    public static final String EXTRA_NAVBAR_HIDE_IME = "navbarHideIme";
    public static final String EXTRA_NAVBAR_CUSTOM_KEY_ENABLE = "navbarCustomKeyEnable";
    public static final String EXTRA_NAVBAR_COLOR_ENABLE = "navbarColorEnable";
    public static final String EXTRA_NAVBAR_KEY_COLOR = "navbarKeyColor";
    public static final String EXTRA_NAVBAR_KEY_GLOW_COLOR = "navbarKeyGlowColor";
    public static final String EXTRA_NAVBAR_CURSOR_CONTROL = "navbarCursorControl";
    public static final String EXTRA_NAVBAR_CUSTOM_KEY_SWAP = "navbarCustomKeySwap";
    public static final String EXTRA_NAVBAR_CUSTOM_KEY_ICON_STYLE = "navbarCustomKeyIconStyle";
    public static final String EXTRA_NAVBAR_AUTOFADE_KEYS = "navbarAutofadeKeys";
    public static final String EXTRA_NAVBAR_AUTOFADE_SHOW_KEYS = "navbarAutofadeShowKeys";

    public static final String PREF_KEY_STATUSBAR_BRIGHTNESS = "pref_statusbar_brightness";
    public static final String PREF_KEY_STATUSBAR_DISABLE_PEEK = "pref_statusbar_disable_peek";
    public static final String PREF_KEY_STATUSBAR_DT2S = "pref_statusbar_dt2s";
    public static final String ACTION_PREF_STATUSBAR_CHANGED = "gravitybox.intent.action.STATUSBAR_CHANGED";
    public static final String EXTRA_SB_BRIGHTNESS = "sbBrightness";
    public static final String EXTRA_SB_DISABLE_PEEK = "sbDisablePeek";
    public static final String EXTRA_SB_DT2S = "sbDt2s";

    public static final String PREF_CAT_KEY_PHONE_TELEPHONY = "pref_cat_phone_telephony";
    public static final String PREF_CAT_KEY_PHONE_DIALER = "pref_cat_phone_dialer";
    public static final String PREF_CAT_KEY_PHONE_MOBILE_DATA = "pref_cat_phone_mobile_data";

    public static final String PREF_KEY_RINGER_MODE_TILE_MODE = "pref_qs_ringer_mode2";
    public static final String PREF_KEY_RINGER_MODE_TILE_QUICK_MODE = "pref_rm_tile_quick_mode";
    public static final String PREF_STAY_AWAKE_TILE_MODE = "pref_qs_stay_awake";
    public static final String PREF_KEY_STAY_AWAKE_TILE_QUICK_MODE = "pref_sa_tile_quick_mode";
    public static final String PREF_KEY_STAY_AWAKE_TILE_AUTO_RESET = "pref_sa_tile_auto_reset";
    public static final String EXTRA_RMT_MODE = "ringerModeTileMode";
    public static final String EXTRA_RMT_QUICK_MODE = "ringerModeTileQuickMode";
    public static final String EXTRA_SA_MODE = "stayAwakeTileMode";
    public static final String EXTRA_SA_QUICK_MODE = "stayAwakeTileQuickMode";
    public static final String EXTRA_SA_AUTO_RESET = "stayAwakeTileAutoReset";

    public static final String PREF_KEY_DISPLAY_ALLOW_ALL_ROTATIONS = "pref_display_allow_all_rotations";
    public static final String ACTION_PREF_DISPLAY_ALLOW_ALL_ROTATIONS_CHANGED = 
            "gravitybox.intent.action.DISPLAY_ALLOW_ALL_ROTATIONS_CHANGED";
    public static final String EXTRA_ALLOW_ALL_ROTATIONS = "allowAllRotations";

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
    public static final String PREF_KEY_DATA_TRAFFIC_ACTIVE_MOBILE_ONLY = "pref_data_traffic_active_mobile_only";
    public static final String PREF_KEY_DATA_TRAFFIC_DISPLAY_MODE = "pref_data_traffic_display_mode";
    public static final String PREF_KEY_DATA_TRAFFIC_POSITION = "pref_data_traffic_position";
    public static final String PREF_KEY_DATA_TRAFFIC_LOCKSCREEN = "pref_data_traffic_lockscreen";
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
    public static final String EXTRA_DT_ACTIVE_MOBILE_ONLY = "dtActiveMobileOnly";
    public static final String EXTRA_DT_DISPLAY_MODE = "dtDisplayMode";
    public static final String EXTRA_DT_POSITION = "dtPosition";
    public static final String EXTRA_DT_LOCKSCREEN = "dtLockscreen";
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
            "pref_app_launcher_slot6", "pref_app_launcher_slot7", "pref_app_launcher_slot8",
            "pref_app_launcher_slot9", "pref_app_launcher_slot10", "pref_app_launcher_slot11"));
    public static final String ACTION_PREF_APP_LAUNCHER_CHANGED = "gravitybox.intent.action.APP_LAUNCHER_CHANGED";
    public static final String EXTRA_APP_LAUNCHER_SLOT = "appLauncherSlot";
    public static final String EXTRA_APP_LAUNCHER_APP = "appLauncherApp";

    public static final String PREF_CAT_LAUNCHER_TWEAKS = "pref_cat_launcher_tweaks";
    public static final String PREF_KEY_LAUNCHER_DESKTOP_GRID_ROWS = "pref_launcher_desktop_grid_rows";
    public static final String PREF_KEY_LAUNCHER_DESKTOP_GRID_COLS = "pref_launcher_desktop_grid_cols";
    public static final String PREF_KEY_LAUNCHER_RESIZE_WIDGET = "pref_launcher_resize_widget";

    public static final String PREF_CAT_KEY_SIGNAL_CLUSTER = "pref_cat_signal_cluster";
    public static final String PREF_KEY_SIGNAL_CLUSTER_DATA_ACTIVITY = "pref_signal_cluster_data_activity";
    public static final String PREF_KEY_SIGNAL_CLUSTER_HPLUS = "pref_signal_cluster_hplus";
    public static final String PREF_KEY_SIGNAL_CLUSTER_LTE_STYLE = "pref_signal_cluster_lte_style";
    public static final String PREF_KEY_SIGNAL_CLUSTER_HIDE_SIM_LABELS = "pref_signal_cluster_hide_sim_labels";
    public static final String PREF_KEY_SIGNAL_CLUSTER_NARROW = "pref_signal_cluster_narrow";
    public static final String PREF_KEY_SIGNAL_CLUSTER_DEM = "pref_signal_cluster_dem";
    public static final String PREF_KEY_SIGNAL_CLUSTER_DNTI = "pref_signal_cluster_dnti";
    public static final String PREF_KEY_SIGNAL_CLUSTER_NOSIM = "pref_signal_cluster_nosim";
    public static final String ACTION_PREF_SIGNAL_CLUSTER_CHANGED = "gravitybox.intent.action.SIGNAL_CLUSTER_CHANGED";
    public static final String EXTRA_SC_NARROW = "scNarrow";

    public static final String PREF_KEY_SMART_RADIO_ENABLE = "pref_smart_radio_enable";
    public static final String PREF_KEY_SMART_RADIO_NORMAL_MODE = "pref_smart_radio_normal_mode";
    public static final String PREF_KEY_SMART_RADIO_POWER_SAVING_MODE = "pref_smart_radio_power_saving_mode";
    public static final String PREF_KEY_SMART_RADIO_SCREEN_OFF = "pref_smart_radio_screen_off";
    public static final String PREF_KEY_SMART_RADIO_SCREEN_OFF_DELAY = "pref_smart_radio_screen_off_delay";
    public static final String PREF_KEY_SMART_RADIO_ADAPTIVE_DELAY = "pref_smart_radio_adaptive_delay";
    public static final String PREF_KEY_SMART_RADIO_IGNORE_LOCKED = "pref_smart_radio_ignore_locked";
    public static final String PREF_KEY_SMART_RADIO_MODE_CHANGE_DELAY = "pref_smart_radio_mode_change_delay";
    public static final String PREF_KEY_SMART_RADIO_MDA_IGNORE = "pref_smart_radio_mda_ignore";
    public static final String ACTION_PREF_SMART_RADIO_CHANGED = "gravitybox.intent.action.SMART_RADIO_CHANGED";
    public static final String EXTRA_SR_NORMAL_MODE = "smartRadioNormalMode";
    public static final String EXTRA_SR_POWER_SAVING_MODE = "smartRadioPowerSavingMode";
    public static final String EXTRA_SR_SCREEN_OFF = "smartRadioScreenOff";
    public static final String EXTRA_SR_SCREEN_OFF_DELAY = "smartRadioScreenOffDelay";
    public static final String EXTRA_SR_IGNORE_LOCKED = "smartRadioIgnoreLocked";
    public static final String EXTRA_SR_MODE_CHANGE_DELAY = "smartRadioModeChangeDelay";
    public static final String EXTRA_SR_MDA_IGNORE = "smartRadioMdaIgnore";
    public static final String EXTRA_SR_ADAPTIVE_DELAY = "smartRadioAdaptiveDelay";

    public static final String PREF_KEY_IME_FULLSCREEN_DISABLE = "pref_ime_fullscreen_disable";
    public static final String PREF_KEY_TORCH_AUTO_OFF = "pref_torch_auto_off";
    public static final String PREF_KEY_FORCE_OVERFLOW_MENU_BUTTON = "pref_force_overflow_menu_button2";
    public static final String PREF_KEY_FORCE_LTR_DIRECTION = "pref_force_ltr_direction";

    public static final String PREF_CAT_KEY_MISC_OTHER = "pref_cat_misc_other";
    public static final String PREF_KEY_PULSE_NOTIFICATION_DELAY = "pref_pulse_notification_delay2";

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

    public static final String PREF_KEY_STATUSBAR_BT_VISIBILITY = "pref_sb_bt_visibility";
    public static final String PREF_KEY_STATUSBAR_HIDE_VIBRATE_ICON = "pref_sb_hide_vibrate_icon";
    public static final String ACTION_PREF_SYSTEM_ICON_CHANGED = 
            "gravitybox.intent.action.SYSTEM_ICON_CHANGED";
    public static final String EXTRA_SB_BT_VISIBILITY = "sbBtVisibility";
    public static final String EXTRA_SB_HIDE_VIBRATE_ICON = "sbHideVibrateIcon";

    public static final String PREF_KEY_INCREASING_RING = "pref_increasing_ring";

    public static final String PREF_KEY_HEADS_UP_MASTER_SWITCH = "pref_heads_up_master_switch";
    public static final String PREF_KEY_HEADS_UP_TIMEOUT = "pref_heads_up_timeout";
    public static final String ACTION_HEADS_UP_SETTINGS_CHANGED = "gravitybox.intent.action.HEADS_UP_SETTINGS_CHANGED";

    public static final String PREF_KEY_HEADSET_ACTION_PLUG = "pref_headset_action_plug";
    public static final String PREF_KEY_HEADSET_ACTION_UNPLUG = "pref_headset_action_unplug";
    public static final String ACTION_PREF_HEADSET_ACTION_CHANGED = "gravitybox.intent.action.HEADSET_ACTION_CHANGED";
    public static final String EXTRA_HSA_STATE = "headsetState"; // 1 = plugged, 0 = unplugged
    public static final String EXTRA_HSA_URI = "headsetActionUri";

    public static final String PREF_KEY_POWER_PROXIMITY_WAKE = "pref_power_proximity_wake";
    public static final String PREF_KEY_POWER_PROXIMITY_WAKE_IGNORE_CALL = "pref_power_proximity_wake_ignore_call";
    public static final String PREF_KEY_POWER_CAMERA_VP = "pref_power_camera_vibrate_pattern";
    public static final String ACTION_PREF_POWER_CHANGED = "gravitybox.intent.action.POWER_CHANGED";
    public static final String EXTRA_POWER_PROXIMITY_WAKE = "powerProximityWake";
    public static final String EXTRA_POWER_PROXIMITY_WAKE_IGNORE_CALL = "powerProximityWakeIgnoreCall";
    public static final String EXTRA_POWER_CAMERA_VP = "powerCameraVibratePattern";

    public static final String PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS = "pref_statusbar_download_progress";
    public static final String PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS_ANIMATED = "pref_statusbar_download_progress_animated";
    public static final String PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS_CENTERED = "pref_statusbar_download_progress_centered";
    public static final String PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS_THICKNESS = "pref_statusbar_download_progress_thickness";
    public static final String PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS_MARGIN = "pref_statusbar_download_progress_margin";
    public static final String PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS_SOUND_ENABLE = "pref_statusbar_download_progress_sound_enable";
    public static final String PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS_SOUND = "pref_statusbar_download_progress_sound";
    public static final String PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS_SOUND_SCREEN_OFF = "pref_statusbar_download_progress_sound_screen_off";
    public static final String ACTION_PREF_STATUSBAR_DOWNLOAD_PROGRESS_CHANGED = "gravitybox.intent.action.STATUSBAR_DOWNLOAD_PROGRESS_CHANGED";
    public static final String EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_ENABLED = "sbDownloadProgressEnabled";
    public static final String EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_ANIMATED = "sbDownloadProgressAnimated";
    public static final String EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_CENTERED = "sbDownloadProgressCentered";
    public static final String EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_THICKNESS = "sbDownloadProgressThickness";
    public static final String EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_MARGIN = "sbDownloadProgressMargin";
    public static final String EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_SOUND_ENABLE = "sbDownloadProgressSoundEnable";
    public static final String EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_SOUND = "sbDownloadProgressSound";
    public static final String EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_SOUND_SCREEN_OFF = "sbDownloadProgressSoundScreenOff";

    public static final String PREF_KEY_QUICKRECORD_QUALITY = "pref_quickrecord_quality";
    public static final String PREF_KEY_QUICKRECORD_AUTOSTOP = "pref_quickrecord_autostop";
    public static final String EXTRA_QR_QUALITY = "quickRecordQuality";
    public static final String EXTRA_QR_AUTOSTOP = "quickRecordAutostop";

    public static final String PREF_KEY_HIDE_LAUNCHER_ICON = "pref_hide_launcher_icon";

    public static final String PREF_KEY_BATTERY_BAR_SHOW = "pref_battery_bar_show";
    public static final String PREF_KEY_BATTERY_BAR_POSITION = "pref_battery_bar_position";
    public static final String PREF_KEY_BATTERY_BAR_MARGIN = "pref_battery_bar_margin";
    public static final String PREF_KEY_BATTERY_BAR_THICKNESS = "pref_battery_bar_thickness";
    public static final String PREF_KEY_BATTERY_BAR_CHARGE_ANIM = "pref_battery_bar_charge_anim";
    public static final String PREF_KEY_BATTERY_BAR_DYNACOLOR = "pref_battery_bar_dynacolor";
    public static final String PREF_KEY_BATTERY_BAR_COLOR = "pref_battery_bar_color";
    public static final String PREF_KEY_BATTERY_BAR_COLOR_LOW = "pref_battery_bar_color_low";
    public static final String PREF_KEY_BATTERY_BAR_COLOR_CRITICAL = "pref_battery_bar_color_critical";
    public static final String PREF_KEY_BATTERY_BAR_CENTERED = "pref_battery_bar_centered";
    public static final String PREF_KEY_BATTERY_BAR_COLOR_CHARGING = "pref_battery_bar_color_charging";
    public static final String ACTION_PREF_BATTERY_BAR_CHANGED = "gravitybox.intent.action.BATTERY_BAR_CHANGED";
    public static final String EXTRA_BBAR_SHOW = "batteryBarShow";
    public static final String EXTRA_BBAR_POSITION = "batteryBarPosition";
    public static final String EXTRA_BBAR_MARGIN = "batteryBarMargin";
    public static final String EXTRA_BBAR_THICKNESS = "batteryBarThickness";
    public static final String EXTRA_BBAR_CHARGE_ANIM = "batteryBarChargeAnim";
    public static final String EXTRA_BBAR_DYNACOLOR = "batteryBarDynaColor";
    public static final String EXTRA_BBAR_COLOR = "batteryBarColor";
    public static final String EXTRA_BBAR_COLOR_LOW = "batteryBarColorLow";
    public static final String EXTRA_BBAR_COLOR_CRITICAL = "batteryBarColorCritical";
    public static final String EXTRA_BBAR_CENTERED = "batteryBarCentered";
    public static final String EXTRA_BBAR_COLOR_CHARGING = "batteryBarColorCharging";

    public static final String PREF_CAT_KEY_CELL_TILE = "pref_cat_qs_cell_tile";
    public static final String PREF_KEY_CELL_TILE_DATA_TOGGLE = "pref_cell_tile_data_toggle";
    public static final String EXTRA_CELL_TILE_DATA_TOGGLE = "cellTileDataToggle";

    public static final String PREF_CAT_KEY_BATTERY_TILE = "pref_cat_qs_battery_tile";
    public static final String PREF_KEY_BATTERY_TILE_TEMP = "pref_battery_tile_temp";
    public static final String PREF_KEY_BATTERY_TILE_TEMP_UNIT = "pref_battery_tile_temp_unit";
    public static final String PREF_KEY_BATTERY_TILE_VOLTAGE = "pref_battery_tile_voltage";
    public static final String EXTRA_BATTERY_TILE_TEMP = "batteryTileTemp";
    public static final String EXTRA_BATTERY_TILE_TEMP_UNIT = "batteryTileTempUnit";
    public static final String EXTRA_BATTERY_TILE_VOLTAGE = "batteryTileVoltage";

    public static final String PREF_KEY_DND_TILE_QUICK_MODE = "pref_dnd_tile_quick_mode";
    public static final String PREF_KEY_DND_TILE_ENABLED_MODES = "pref_dnd_tile_enabled_modes";
    public static final String PREF_KEY_DND_TILE_DURATION_MODE = "pref_dnd_tile_duration_mode";
    public static final String PREF_KEY_DND_TILE_DURATION = "pref_dnd_tile_duration";
    public static final String EXTRA_DND_TILE_QUICK_MODE = "dndTileQuickMode";
    public static final String EXTRA_DND_TILE_ENABLED_MODES = "dndTileEnabledModes";
    public static final String EXTRA_DND_TILE_DURATION_MODE = "dndTileDurationMode";
    public static final String EXTRA_DND_TILE_DURATION = "dndTileDuration";

    public static final String PREF_KEY_LOCATION_TILE_QUICK_MODE = "pref_location_tile_quick_mode";
    public static final String EXTRA_LOCATION_TILE_QUICK_MODE = "locationTileQuickMode";

    public static final String PREF_KEY_NM_TILE_ENABLED_MODES = "pref_nm_tile_enabled_modes";
    public static final String PREF_KEY_NM_TILE_QUICK_MODE = "pref_nm_tile_quick_mode";
    public static final String EXTRA_NM_TILE_ENABLED_MODES = "nmTileEnabledModes";
    public static final String EXTRA_NM_TILE_QUICK_MODE = "nmTileQuickMode";

    public static final String PREF_KEY_FORCE_AOSP = "pref_force_aosp";

    public static final String PREF_CAT_KEY_FINGERPRINT_LAUNCHER = "pref_cat_fingerprint_launcher";
    public static final String PREF_KEY_FINGERPRINT_LAUNCHER_ENABLE = "pref_fingerprint_launcher_enable";
    public static final String PREF_KEY_FINGERPRINT_LAUNCHER_PAUSE = "pref_fingerprint_launcher_pause";
    public static final String PREF_KEY_FINGERPRINT_LAUNCHER_SHOW_TOAST = "pref_fingerprint_launcher_show_toast";
    public static final String PREF_KEY_FINGERPRINT_LAUNCHER_APP = "pref_fingerprint_launcher_app";
    public static final String PREF_CAT_KEY_FINGERPRINT_LAUNCHER_FINGERS = "pref_cat_fingerprint_launcher_fingers";
    public static final String PREF_KEY_FINGERPRINT_LAUNCHER_FINGER = "pref_fingerprint_launcher_finger";
    public static final String ACTION_FPL_SETTINGS_CHANGED = "gravitybox.intent.action.FPL_SETTINGS_CHANGED";
    public static final String EXTRA_FPL_FINGER_ID = "fplFingerId";
    public static final String EXTRA_FPL_APP = "fplApp";
    public static final String EXTRA_FPL_PAUSE = "fplPause";
    public static final String EXTRA_FPL_SHOW_TOAST = "fplShowToast";

    private static final int REQ_LOCKSCREEN_BACKGROUND = 1024;
    private static final int REQ_NOTIF_BG_IMAGE_PORTRAIT = 1025;
    private static final int REQ_NOTIF_BG_IMAGE_LANDSCAPE = 1026;
    private static final int REQ_CALLER_PHOTO = 1027;
    private static final int REQ_OBTAIN_SHORTCUT = 1028;
    private static final int REQ_ICON_PICK = 1029;

    private static final List<String> rebootKeys = new ArrayList<String>(Arrays.asList(
            PREF_KEY_BRIGHTNESS_MIN,
            PREF_KEY_LOCKSCREEN_MENU_KEY,
            PREF_KEY_LOCKSCREEN_ROTATION,
            PREF_KEY_MUSIC_VOLUME_STEPS,
            PREF_KEY_MUSIC_VOLUME_STEPS_VALUE,
            PREF_KEY_TRANSLUCENT_DECOR,
            PREF_KEY_SCREEN_DIM_LEVEL,
            PREF_KEY_BRIGHTNESS_MASTER_SWITCH,
            PREF_KEY_NAVBAR_OVERRIDE,
            PREF_KEY_NAVBAR_ENABLE,
            PREF_KEY_UNPLUG_TURNS_ON_SCREEN,
            PREF_KEY_QUICK_SETTINGS_ENABLE,
            PREF_KEY_SIGNAL_CLUSTER_DATA_ACTIVITY,
            PREF_KEY_FORCE_OVERFLOW_MENU_BUTTON,
            PREF_KEY_SMART_RADIO_ENABLE,
            PREF_KEY_PULSE_NOTIFICATION_DELAY,
            PREF_KEY_STATUSBAR_CLOCK_MASTER_SWITCH,
            PREF_KEY_SIGNAL_CLUSTER_HPLUS,
            PREF_KEY_SIGNAL_CLUSTER_LTE_STYLE,
            PREF_KEY_FORCE_LTR_DIRECTION,
            PREF_KEY_HEADS_UP_MASTER_SWITCH,
            PREF_KEY_SIGNAL_CLUSTER_HIDE_SIM_LABELS,
            PREF_KEY_SAFE_MEDIA_VOLUME,
            PREF_KEY_SIGNAL_CLUSTER_DEM,
            PREF_KEY_SIGNAL_CLUSTER_DNTI,
            PREF_KEY_SIGNAL_CLUSTER_NOSIM,
            PREF_KEY_BATTERY_PERCENT_TEXT_POSITION,
            PREF_KEY_FINGERPRINT_LAUNCHER_ENABLE
    ));

    // TODO: recents key long-press
    private static final List<String> customAppKeys = new ArrayList<String>(Arrays.asList(
            PREF_KEY_HWKEY_MENU_SINGLETAP,
            PREF_KEY_HWKEY_MENU_LONGPRESS,
            PREF_KEY_HWKEY_MENU_DOUBLETAP,
            PREF_KEY_HWKEY_HOME_LONGPRESS,
            PREF_KEY_HWKEY_HOME_DOUBLETAP,
            PREF_KEY_HWKEY_BACK_SINGLETAP,
            PREF_KEY_HWKEY_BACK_LONGPRESS,
            PREF_KEY_HWKEY_BACK_DOUBLETAP,
            PREF_KEY_HWKEY_RECENTS_SINGLETAP,
            //PREF_KEY_HWKEY_RECENTS_LONGPRESS,
            PREF_KEY_HWKEY_RECENTS_DOUBLETAP,
            PREF_KEY_NAVBAR_CUSTOM_KEY_SINGLETAP,
            PREF_KEY_NAVBAR_CUSTOM_KEY_LONGPRESS,
            PREF_KEY_NAVBAR_CUSTOM_KEY_DOUBLETAP,
            PREF_KEY_PIE_APP_LONGPRESS,
            PREF_KEY_PIE_BACK_LONGPRESS,
            PREF_KEY_PIE_HOME_LONGPRESS,
            PREF_KEY_PIE_MENU_LONGPRESS,
            PREF_KEY_PIE_RECENTS_LONGPRESS,
            PREF_KEY_PIE_SEARCH_LONGPRESS
    ));

    private static final List<String> ringToneKeys = new ArrayList<String>(Arrays.asList(
            PREF_KEY_BATTERY_CHARGED_SOUND,
            PREF_KEY_CHARGER_PLUGGED_SOUND,
            PREF_KEY_CHARGER_UNPLUGGED_SOUND,
            PREF_KEY_CHARGER_PLUGGED_SOUND_WIRELESS,
            PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS_SOUND
    ));

    private static final List<String> lockscreenKeys = new ArrayList<String>(Arrays.asList(
            PREF_KEY_LOCKSCREEN_BACKGROUND,
            PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR,
            PREF_KEY_LOCKSCREEN_BACKGROUND_BLUR_EFFECT,
            PREF_KEY_LOCKSCREEN_BACKGROUND_BLUR_INTENSITY,
            PREF_KEY_LOCKSCREEN_BACKGROUND_OPACITY,
            PREF_KEY_LOCKSCREEN_QUICK_UNLOCK,
            PREF_KEY_LOCKSCREEN_PIN_LENGTH,
            PREF_KEY_LOCKSCREEN_DIRECT_UNLOCK,
            PREF_KEY_LOCKSCREEN_DIRECT_UNLOCK_TRANS_LEVEL,
            PREF_KEY_LOCKSCREEN_DIRECT_UNLOCK_POLICY,
            PREF_KEY_LOCKSCREEN_SMART_UNLOCK,
            PREF_KEY_LOCKSCREEN_SMART_UNLOCK_POLICY,
            PREF_KEY_LOCKSCREEN_IMPRINT_MODE,
            PREF_KEY_LOCKSCREEN_D2TS,
            PREF_KEY_LOCKSCREEN_CARRIER_TEXT,
            PREF_KEY_LOCKSCREEN_SHOW_PATTERN_ERROR,
            PREF_KEY_LOCKSCREEN_BLEFT_ACTION_CUSTOM,
            PREF_KEY_LOCKSCREEN_BRIGHT_ACTION_CUSTOM,
            PREF_KEY_LOCKSCREEN_BOTTOM_ACTIONS_HIDE,
            PREF_KEY_IMPRINT_VIBE_DISABLE,
            PREF_KEY_LOCKSCREEN_PIN_SCRAMBLE
    ));

    private static final List<String> headsUpKeys = new ArrayList<String>(Arrays.asList(
            PREF_KEY_HEADS_UP_TIMEOUT
//            PREF_KEY_HEADS_UP_ALPHA,
//            PREF_KEY_HEADS_UP_SNOOZE,
//            PREF_KEY_HEADS_UP_SNOOZE_TIMER
    ));

    public static final class SystemProperties {
        public boolean hasGeminiSupport;
        public boolean isTablet;
        public boolean hasNavigationBar;
        public boolean unplugTurnsOnScreen;
        public int defaultNotificationLedOff;
        public boolean uuidRegistered;
        public String uuidType;
        public int uncTrialCountdown;
        public boolean hasMsimSupport;
        public boolean supportsFingerprint;
        public int[] fingerprintIds;
        public boolean isOxygenOsRom;

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
            if (data.containsKey("uuidType")) {
                uuidType = data.getString("uuidType");
            }
            if (data.containsKey("uncTrialCountdown")) {
                uncTrialCountdown = data.getInt("uncTrialCountdown");
            }
            if (data.containsKey("hasMsimSupport")) {
                hasMsimSupport = data.getBoolean("hasMsimSupport");
            }
            if (data.containsKey("supportsFingerprint")) {
                supportsFingerprint = data.getBoolean("supportsFingerprint");
            }
            if (data.containsKey("fingerprintIds")) {
                fingerprintIds = data.getIntArray("fingerprintIds");
            }
            if (data.containsKey("isOxygenOsRom")) {
                isOxygenOsRom = data.getBoolean("isOxygenOsRom");
            }
        }
    }

    private GravityBoxResultReceiver mReceiver;
    private Handler mHandler;
    public static SystemProperties sSystemProperties;
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
        super.onCreate(savedInstanceState);

        // fix folder permissions
        SettingsManager.getInstance(this).fixFolderPermissionsAsync();

        // refuse to run if there's GB with old package name still installed
        // prompt to uninstall previous package and finish
        if (Utils.isAppInstalled(this, "com.ceco.marshmallow.gravitybox")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle(R.string.gb_new_package_dialog_title)
            .setMessage(R.string.gb_new_package_dialog_message)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    // try to uninstall old package
                    Uri oldGbUri = Uri.parse("package:com.ceco.marshmallow.gravitybox");
                    Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, oldGbUri);
                    startActivity(uninstallIntent);
                    finish();
                }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
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
        private ListPreference mPrefBatteryPercentSize;
        private ListPreference mPrefBatteryPercentStyle;
        private ListPreference mPrefBatteryPercentCharging;
        private ListPreference mLowBatteryWarning;
        private WorldReadablePrefs mPrefs;
        private AlertDialog mDialog;
        private PreferenceScreen mPrefCatAbout;
        private Preference mPrefAboutGb;
        private Preference mPrefAboutGplus;
        private Preference mPrefAboutXposed;
        private Preference mPrefAboutDonate;
        private Preference mPrefAboutUnlocker;
        private Preference mPrefEngMode;
        private PreferenceCategory mPrefCatLockscreenBg;
        private ListPreference mPrefLockscreenBg;
        private ColorPickerPreference mPrefLockscreenBgColor;
        private Preference mPrefLockscreenBgImage;
        private CheckBoxPreference mPrefLockscreenBgBlurEffect;
        private SeekBarPreference mPrefLockscreenBlurIntensity;
        private EditTextPreference mPrefLockscreenCarrierText;
        private File wallpaperImage;
        private File notifBgImagePortrait;
        private File notifBgImageLandscape;
        private ListPreference mPrefHwKeyMenuSingletap;
        private ListPreference mPrefHwKeyMenuLongpress;
        private ListPreference mPrefHwKeyMenuDoubletap;
        private ListPreference mPrefHwKeyHomeLongpress;
        private ListPreference mPrefHwKeyHomeDoubletap;
        private ListPreference mPrefHwKeyBackSingletap;
        private ListPreference mPrefHwKeyBackLongpress;
        private ListPreference mPrefHwKeyBackDoubletap;
        private ListPreference mPrefHwKeyRecentsSingletap;
        // TODO: recents key long-press
        //private ListPreference mPrefHwKeyRecentsLongpress;
        private ListPreference mPrefHwKeyRecentsDoubletap;
        private ListPreference mPrefHwKeyDoubletapSpeed;
        private ListPreference mPrefHwKeyKillDelay;
        private ListPreference mPrefPhoneFlip;
        private SwitchPreference mPrefSbIconColorEnable;
        private ColorPickerPreference mPrefSbIconColor;
        private ColorPickerPreference mPrefSbDaColor;
        private PreferenceScreen mPrefCatStatusbar;
        private PreferenceScreen mPrefCatStatusbarQs;
        //private ListPreference mPrefAutoSwitchQs;
        private ListPreference mPrefQuickPulldown;
        private SeekBarPreference mPrefQuickPulldownSize;
        private PreferenceScreen mPrefCatNotifDrawerStyle;
        private ListPreference mPrefNotifBackground;
        private ColorPickerPreference mPrefNotifColor;
        private SeekBarPreference mPrefNotifAlpha;
        private Preference mPrefNotifImagePortrait;
        private Preference mPrefNotifImageLandscape;
        private CheckBoxPreference mPrefNotifExpandAll;
        private CheckBoxPreference mPrefDisableRoamingIndicators;
        private ListPreference mPrefButtonBacklightMode;
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
        private ListPreference mPrefRambar;
        private PreferenceScreen mPrefCatPhone;
        private SeekBarPreference mPrefBrightnessMin;
        private SeekBarPreference mPrefScreenDimLevel;
        private PreferenceScreen mPrefCatLockscreen;
        private PreferenceScreen mPrefCatPower;
        private PreferenceCategory mPrefCatPowerMenu;
        private PreferenceCategory mPrefCatPowerOther;
        private CheckBoxPreference mPrefPowerProximityWake;
        private PreferenceScreen mPrefCatDisplay;
        private ListPreference mPrefTranclucentDecor;
        private PreferenceScreen mPrefCatMedia;
        private ListPreference mPrefExpandedDesktop;
        private PreferenceCategory mPrefCatNavbarKeys;
        private PreferenceCategory mPrefCatNavbarColor;
        private PreferenceCategory mPrefCatNavbarDimen;
        private CheckBoxPreference mPrefNavbarEnable;
        private CheckBoxPreference mPrefMusicVolumeSteps;
        private SeekBarPreference mPrefMusicVolumeStepsValue;
        private PreferenceCategory mPrefCatPhoneTelephony;
        private PreferenceCategory mPrefCatPhoneDialer;
        private ListPreference mPrefQsNetworkModeSimSlot;
        private ListPreference mPrefSbSignalColorMode;
        private CheckBoxPreference mPrefUnplugTurnsOnScreen;
        private MultiSelectListPreference mPrefCallVibrations;
        //private ListPreference mPrefQsTileLabelStyle;
        private ListPreference mPrefSbClockDate;
        private ListPreference mPrefSbClockDow;
        private SeekBarPreference mPrefSbClockDowSize;
        private PreferenceScreen mPrefCatDataTraffic;
        private ListPreference mPrefDataTrafficPosition;
        private CheckBoxPreference mPrefDataTrafficLs;
        private ListPreference mPrefDataTrafficSize;
        private ListPreference mPrefDataTrafficMode;
        private ListPreference mPrefDataTrafficInactivityMode;
        private ListPreference mPrefDataTrafficOmniMode;
        private CheckBoxPreference mPrefDataTrafficOmniShowIcon;
        private CheckBoxPreference mPrefDataTrafficOmniAutohide;
        private SeekBarPreference mPrefDataTrafficOmniAutohideTh;
        private CheckBoxPreference mPrefDataTrafficActiveMobileOnly;
        private ListPreference mPrefDataTrafficDisplayMode;
        private CheckBoxPreference mPrefLinkVolumes;
        private PreferenceScreen mPrefCatAppLauncher;
        private AppPickerPreference[] mPrefAppLauncherSlot;
        private File callerPhotoFile;
        private CheckBoxPreference mPrefCallerUnknownPhotoEnable;
        private Preference mPrefCallerUnknownPhoto;
        private PreferenceScreen mPrefCatStatusbarColors;
        private ColorPickerPreference mPrefSbIconColorSecondary;
        private ColorPickerPreference mPrefSbDaColorSecondary;
        private ListPreference mPrefHwKeyLockscreenTorch;
        private PreferenceCategory mPrefCatHwKeyOthers;
        private PreferenceCategory mPrefCatLsOther;
        private PreferenceScreen mPrefCatLsShortcuts;
        private ListPreference mPrefLsRotation;
        private PreferenceScreen mPrefCatLauncherTweaks;
        private ListPreference mPrefLauncherDesktopGridRows;
        private ListPreference mPrefLauncherDesktopGridCols;
        private ListPreference mPrefVolumeRockerWake;
        private ListPreference mPrefNavbarCustomKeySingletap;
        private ListPreference mPrefNavbarCustomKeyLongpress;
        private ListPreference mPrefNavbarCustomKeyDoubletap;
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
        private ListPreference mPrefScLteStyle;
        private ListPreference mPrefSbBtVisibility;
        private AppPickerPreference mPrefCustomApp;
        private ListPreference mPrefChargingLed;
        private CheckBoxPreference mPrefProximityWakeIgnoreCall;
        private CheckBoxPreference mPrefScHideSimLabels;
        private CheckBoxPreference mPrefScNarrow;
        private ListPreference mPrefQrQuality;
        private SeekBarPreference mPrefSrAdaptiveDelay;
        private ListPreference mPrefBbarPosition;
        private ListPreference mPrefSbdpMode;
        private PreferenceScreen mPrefCatCellTile;
        private ListPreference mPrefBatteryTileTempUnit;
        private EditTextPreference mPrefPowerCameraVp;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (sSystemProperties == null) {
                Intent restartIntent = new Intent(getActivity(), GravityBoxSettings.class);
                restartIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(restartIntent);
                System.exit(0);
                return;
            }
            
            mPrefs = SettingsManager.getInstance(getActivity()).getMainPrefs();
            if (Utils.USE_DEVICE_PROTECTED_STORAGE) {
                getPreferenceManager().setStorageDeviceProtected();
            }
            addPreferencesFromResource(R.xml.gravitybox);

            AppPickerPreference.sPrefsFragment = this;
            AppPickerPreference.cleanupAsync(getActivity());

            // Force update tile service components
            TileOrderActivity.updateServiceComponents(getActivity());

            mBatteryStyle = (ListPreference) findPreference(PREF_KEY_BATTERY_STYLE);
            mPrefBatteryPercentSize = (ListPreference) findPreference(PREF_KEY_BATTERY_PERCENT_TEXT_SIZE);
            mPrefBatteryPercentStyle = (ListPreference) findPreference(PREF_KEY_BATTERY_PERCENT_TEXT_STYLE);
            mPrefBatteryPercentCharging = (ListPreference) findPreference(PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING);
            mLowBatteryWarning = (ListPreference) findPreference(PREF_KEY_LOW_BATTERY_WARNING_POLICY);

            mPrefCatAbout = (PreferenceScreen) findPreference(PREF_CAT_KEY_ABOUT);
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
            mPrefAboutUnlocker = (Preference) findPreference(PREF_KEY_ABOUT_UNLOCKER);

            mPrefEngMode = (Preference) findPreference(PREF_KEY_ENGINEERING_MODE);
            if (!Utils.isAppInstalled(getActivity(), APP_ENGINEERING_MODE)) {
                getPreferenceScreen().removePreference(mPrefEngMode);
            }

            mPrefCatLockscreenBg = 
                    (PreferenceCategory) findPreference(PREF_CAT_KEY_LOCKSCREEN_BACKGROUND);
            mPrefLockscreenBg = (ListPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND);
            mPrefLockscreenBgColor = 
                    (ColorPickerPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR);
            mPrefLockscreenBgImage = 
                    (Preference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_IMAGE);
            mPrefLockscreenBgBlurEffect =
                    (CheckBoxPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_BLUR_EFFECT);
            mPrefLockscreenBlurIntensity =
                    (SeekBarPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_BLUR_INTENSITY);
            mPrefLockscreenCarrierText = 
                    (EditTextPreference) findPreference(PREF_KEY_LOCKSCREEN_CARRIER_TEXT);

            wallpaperImage = new File(Utils.getFilesDir(getActivity()) + "/lockwallpaper"); 
            notifBgImagePortrait = new File(Utils.getFilesDir(getActivity()) + "/notifwallpaper");
            notifBgImageLandscape = new File(Utils.getFilesDir(getActivity()) + "/notifwallpaper_landscape");
            callerPhotoFile = new File(Utils.getFilesDir(getActivity()) + "/caller_photo");

            mPrefHwKeyMenuSingletap = (ListPreference) findPreference(PREF_KEY_HWKEY_MENU_SINGLETAP);
            mPrefHwKeyMenuLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_MENU_LONGPRESS);
            mPrefHwKeyMenuDoubletap = (ListPreference) findPreference(PREF_KEY_HWKEY_MENU_DOUBLETAP);
            mPrefHwKeyHomeLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_HOME_LONGPRESS);
            //mPrefHwKeyHomeLongpressKeyguard = (CheckBoxPreference) findPreference(PREF_KEY_HWKEY_HOME_LONGPRESS_KEYGUARD);
            mPrefHwKeyHomeDoubletap = (ListPreference) findPreference(PREF_KEY_HWKEY_HOME_DOUBLETAP);
            mPrefHwKeyBackSingletap = (ListPreference) findPreference(PREF_KEY_HWKEY_BACK_SINGLETAP);
            mPrefHwKeyBackLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_BACK_LONGPRESS);
            mPrefHwKeyBackDoubletap = (ListPreference) findPreference(PREF_KEY_HWKEY_BACK_DOUBLETAP);
            mPrefHwKeyRecentsSingletap = (ListPreference) findPreference(PREF_KEY_HWKEY_RECENTS_SINGLETAP);
            // TODO: recents key long-press
            //mPrefHwKeyRecentsLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_RECENTS_LONGPRESS);
            mPrefHwKeyRecentsDoubletap = (ListPreference) findPreference(PREF_KEY_HWKEY_RECENTS_DOUBLETAP);
            mPrefHwKeyDoubletapSpeed = (ListPreference) findPreference(PREF_KEY_HWKEY_DOUBLETAP_SPEED);
            mPrefHwKeyKillDelay = (ListPreference) findPreference(PREF_KEY_HWKEY_KILL_DELAY);
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
            //mPrefAutoSwitchQs = (ListPreference) findPreference(PREF_KEY_QUICK_SETTINGS_AUTOSWITCH);
            mPrefQuickPulldown = (ListPreference) findPreference(PREF_KEY_QUICK_PULLDOWN);
            mPrefQuickPulldownSize = (SeekBarPreference) findPreference(PREF_KEY_QUICK_PULLDOWN_SIZE);

            mPrefCatNotifDrawerStyle = (PreferenceScreen) findPreference(PREF_CAT_KEY_NOTIF_DRAWER_STYLE);
            mPrefNotifBackground = (ListPreference) findPreference(PREF_KEY_NOTIF_BACKGROUND);
            mPrefNotifColor = (ColorPickerPreference) findPreference(PREF_KEY_NOTIF_COLOR);
            mPrefNotifImagePortrait = (Preference) findPreference(PREF_KEY_NOTIF_IMAGE_PORTRAIT);
            mPrefNotifImageLandscape = (Preference) findPreference(PREF_KEY_NOTIF_IMAGE_LANDSCAPE);
            mPrefNotifExpandAll = (CheckBoxPreference) findPreference(PREF_KEY_NOTIF_EXPAND_ALL);
            mPrefNotifAlpha = (SeekBarPreference) findPreference(PREF_KEY_NOTIF_BACKGROUND_ALPHA);

            mPrefDisableRoamingIndicators = (CheckBoxPreference) findPreference(PREF_KEY_DISABLE_ROAMING_INDICATORS);
            mPrefButtonBacklightMode = (ListPreference) findPreference(PREF_KEY_BUTTON_BACKLIGHT_MODE);

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
            File file = new File(Utils.getFilesDir(getActivity()) + "/" + FILE_THEME_DARK_FLAG);
            mPrefGbThemeDark.setChecked(file.exists());

            mPrefRambar = (ListPreference) findPreference(PREF_KEY_RAMBAR);

            mPrefCatPhone = (PreferenceScreen) findPreference(PREF_CAT_KEY_PHONE);

            mPrefBrightnessMin = (SeekBarPreference) findPreference(PREF_KEY_BRIGHTNESS_MIN);
            mPrefBrightnessMin.setMinimum(getResources().getInteger(R.integer.screen_brightness_min));
            mPrefScreenDimLevel = (SeekBarPreference) findPreference(PREF_KEY_SCREEN_DIM_LEVEL);
            mPrefScreenDimLevel.setMinimum(getResources().getInteger(R.integer.screen_brightness_dim_min));

            mPrefCatLockscreen = (PreferenceScreen) findPreference(PREF_CAT_KEY_LOCKSCREEN);
            mPrefCatPower = (PreferenceScreen) findPreference(PREF_CAT_KEY_POWER);
            mPrefCatPowerMenu = (PreferenceCategory) findPreference(PREF_CAT_KEY_POWER_MENU);
            mPrefCatPowerOther = (PreferenceCategory) findPreference(PREF_CAT_KEY_POWER_OTHER);
            mPrefPowerProximityWake = (CheckBoxPreference) findPreference(PREF_KEY_POWER_PROXIMITY_WAKE);
            mPrefCatDisplay = (PreferenceScreen) findPreference(PREF_CAT_KEY_DISPLAY);
            mPrefUnplugTurnsOnScreen = (CheckBoxPreference) findPreference(PREF_KEY_UNPLUG_TURNS_ON_SCREEN);
            mPrefCatMedia = (PreferenceScreen) findPreference(PREF_CAT_KEY_MEDIA);
            mPrefMusicVolumeSteps = (CheckBoxPreference) findPreference(PREF_KEY_MUSIC_VOLUME_STEPS);
            mPrefMusicVolumeStepsValue = (SeekBarPreference) findPreference(PREF_KEY_MUSIC_VOLUME_STEPS_VALUE);
            mPrefLinkVolumes = (CheckBoxPreference) findPreference(PREF_KEY_LINK_VOLUMES);
            mPrefTranclucentDecor =  (ListPreference) findPreference(PREF_KEY_TRANSLUCENT_DECOR);

            mPrefExpandedDesktop = (ListPreference) findPreference(PREF_KEY_EXPANDED_DESKTOP);

            mPrefCatNavbarKeys = (PreferenceCategory) findPreference(PREF_CAT_KEY_NAVBAR_KEYS);
            mPrefCatNavbarColor = (PreferenceCategory) findPreference(PREF_CAT_KEY_NAVBAR_COLOR);
            mPrefCatNavbarDimen = (PreferenceCategory) findPreference(PREF_CAT_KEY_NAVBAR_DIMEN);
            mPrefNavbarEnable = (CheckBoxPreference) findPreference(PREF_KEY_NAVBAR_ENABLE);
            mPrefNavbarCustomKeySingletap = (ListPreference) findPreference(PREF_KEY_NAVBAR_CUSTOM_KEY_SINGLETAP);
            mPrefNavbarCustomKeyLongpress = (ListPreference) findPreference(PREF_KEY_NAVBAR_CUSTOM_KEY_LONGPRESS);
            mPrefNavbarCustomKeyDoubletap = (ListPreference) findPreference(PREF_KEY_NAVBAR_CUSTOM_KEY_DOUBLETAP);

            mPrefCatPhoneTelephony = (PreferenceCategory) findPreference(PREF_CAT_KEY_PHONE_TELEPHONY);
            mPrefCatPhoneDialer = (PreferenceCategory) findPreference(PREF_CAT_KEY_PHONE_DIALER);
            mPrefCallVibrations = (MultiSelectListPreference) findPreference(PREF_KEY_CALL_VIBRATIONS);
            mPrefCallerUnknownPhotoEnable = (CheckBoxPreference) findPreference(PREF_KEY_CALLER_UNKNOWN_PHOTO_ENABLE);
            mPrefCallerUnknownPhoto = (Preference) findPreference(PREF_KEY_CALLER_UNKNOWN_PHOTO);

            mPrefQsNetworkModeSimSlot = (ListPreference) findPreference(PREF_KEY_QS_NETWORK_MODE_SIM_SLOT);
            //mPrefQsTileLabelStyle = (ListPreference) findPreference(PREF_KEY_QUICK_SETTINGS_TILE_LABEL_STYLE);

            mPrefSbClockDate = (ListPreference) findPreference(PREF_KEY_STATUSBAR_CLOCK_DATE);
            mPrefSbClockDow = (ListPreference) findPreference(PREF_KEY_STATUSBAR_CLOCK_DOW);
            mPrefSbClockDowSize = (SeekBarPreference) findPreference(PREF_KEY_STATUSBAR_CLOCK_DOW_SIZE);

            mPrefCatDataTraffic = (PreferenceScreen) findPreference(PREF_CAT_KEY_DATA_TRAFFIC);
            mPrefDataTrafficPosition = (ListPreference) findPreference(PREF_KEY_DATA_TRAFFIC_POSITION);
            mPrefDataTrafficLs = (CheckBoxPreference) findPreference(PREF_KEY_DATA_TRAFFIC_LOCKSCREEN);
            mPrefDataTrafficSize = (ListPreference) findPreference(PREF_KEY_DATA_TRAFFIC_SIZE);
            mPrefDataTrafficMode = (ListPreference) findPreference(PREF_KEY_DATA_TRAFFIC_MODE);
            mPrefDataTrafficInactivityMode = (ListPreference) findPreference(PREF_KEY_DATA_TRAFFIC_INACTIVITY_MODE);
            mPrefDataTrafficOmniMode = (ListPreference) findPreference(PREF_KEY_DATA_TRAFFIC_OMNI_MODE);
            mPrefDataTrafficOmniShowIcon = (CheckBoxPreference) findPreference(PREF_KEY_DATA_TRAFFIC_OMNI_SHOW_ICON);
            mPrefDataTrafficActiveMobileOnly = (CheckBoxPreference) findPreference(PREF_KEY_DATA_TRAFFIC_ACTIVE_MOBILE_ONLY);
            mPrefDataTrafficDisplayMode = (ListPreference) findPreference(PREF_KEY_DATA_TRAFFIC_DISPLAY_MODE);
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
                if (mPrefs.getString(appPref.getKey(), null) == null) {
                    mPrefs.edit().putString(appPref.getKey(), null).commit();
                }
            }

            mPrefSbIconColorSecondary = (ColorPickerPreference) findPreference(PREF_KEY_STATUSBAR_ICON_COLOR_SECONDARY);
            mPrefSbDaColorSecondary = (ColorPickerPreference) findPreference(PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR_SECONDARY);

            mPrefCatLsOther = (PreferenceCategory) findPreference(PREF_CAT_KEY_LOCKSCREEN_OTHER);
            mPrefLsRotation = (ListPreference) findPreference(PREF_KEY_LOCKSCREEN_ROTATION);

            mPrefCatLsShortcuts = (PreferenceScreen) findPreference(PREF_CAT_KEY_LOCKSCREEN_SHORTCUTS);
            for (int i = 0; i < PREF_KEY_LOCKSCREEN_SHORTCUT.size(); i++) {
                AppPickerPreference appPref = new AppPickerPreference(getActivity(), null);
                appPref.setKey(PREF_KEY_LOCKSCREEN_SHORTCUT.get(i));
                appPref.setTitle(String.format(
                        getActivity().getString(R.string.pref_app_launcher_slot_title), i + 1));
                appPref.setDialogTitle(appPref.getTitle());
                appPref.setDefaultSummary(getActivity().getString(R.string.app_picker_none));
                appPref.setSummary(getActivity().getString(R.string.app_picker_none));
                appPref.setAllowUnlockAction(true);
                appPref.setLaunchesFromLockscreen(true);
                mPrefCatLsShortcuts.addPreference(appPref);
                if (mPrefs.getString(appPref.getKey(), null) == null) {
                    mPrefs.edit().putString(appPref.getKey(), null).commit();
                }
            }

            mPrefCatLauncherTweaks = (PreferenceScreen) findPreference(PREF_CAT_LAUNCHER_TWEAKS);
            mPrefLauncherDesktopGridRows = (ListPreference) findPreference(PREF_KEY_LAUNCHER_DESKTOP_GRID_ROWS);
            mPrefLauncherDesktopGridCols = (ListPreference) findPreference(PREF_KEY_LAUNCHER_DESKTOP_GRID_COLS);

            mPrefVolumeRockerWake = (ListPreference) findPreference(PREF_KEY_VOLUME_ROCKER_WAKE);

            mPrefPulseNotificationDelay = (SeekBarPreference) findPreference(PREF_KEY_PULSE_NOTIFICATION_DELAY);

            mPrefCatMiscOther = (PreferenceCategory) findPreference(PREF_CAT_KEY_MISC_OTHER);
            mPrefTorchAutoOff = (SeekBarPreference) findPreference(PREF_KEY_TORCH_AUTO_OFF);

            mPrefBackup = findPreference(PREF_KEY_SETTINGS_BACKUP);
            mPrefRestore = findPreference(PREF_KEY_SETTINGS_RESTORE);

            mPrefTransVerification = (EditTextPreference) findPreference(PREF_KEY_TRANS_VERIFICATION);

            mPrefScreenrecordSize = (ListPreference) findPreference(PREF_KEY_SCREENRECORD_SIZE);

            mPrefCatSignalCluster = (PreferenceScreen) findPreference(PREF_CAT_KEY_SIGNAL_CLUSTER);
            mPrefScHideSimLabels = (CheckBoxPreference) findPreference(PREF_KEY_SIGNAL_CLUSTER_HIDE_SIM_LABELS);
            mPrefScNarrow = (CheckBoxPreference) findPreference(PREF_KEY_SIGNAL_CLUSTER_NARROW);

            mPrefLedControl = findPreference(PREF_LED_CONTROL);

            mPrefVkVibratePattern = (EditTextPreference) findPreference(PREF_KEY_VK_VIBRATE_PATTERN);
            mPrefVkVibratePattern.setOnPreferenceChangeListener(this);

            mPrefScLteStyle = (ListPreference) findPreference(PREF_KEY_SIGNAL_CLUSTER_LTE_STYLE);

            mPrefSbBtVisibility = (ListPreference) findPreference(PREF_KEY_STATUSBAR_BT_VISIBILITY);

            mPrefCustomApp = (AppPickerPreference) findPreference(PREF_KEY_HWKEY_CUSTOM_APP);
            getPreferenceScreen().removePreference(mPrefCustomApp);

            mPrefChargingLed = (ListPreference) findPreference(PREF_KEY_CHARGING_LED);
            mPrefProximityWakeIgnoreCall = (CheckBoxPreference) findPreference(PREF_KEY_POWER_PROXIMITY_WAKE_IGNORE_CALL); 

            mPrefQrQuality = (ListPreference) findPreference(PREF_KEY_QUICKRECORD_QUALITY);

            mPrefSrAdaptiveDelay = (SeekBarPreference) findPreference(PREF_KEY_SMART_RADIO_ADAPTIVE_DELAY);

            mPrefBbarPosition = (ListPreference) findPreference(PREF_KEY_BATTERY_BAR_POSITION);

            mPrefSbdpMode = (ListPreference) findPreference(PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS);

            mPrefCatCellTile = (PreferenceScreen) findPreference(PREF_CAT_KEY_CELL_TILE);

            mPrefBatteryTileTempUnit = (ListPreference) findPreference(PREF_KEY_BATTERY_TILE_TEMP_UNIT); 

            mPrefPowerCameraVp = (EditTextPreference) findPreference(PREF_KEY_POWER_CAMERA_VP);

            // Filter preferences according to feature availability 
            if (!Utils.hasFlash(getActivity())) {
                mPrefCatHwKeyOthers.removePreference(mPrefHwKeyLockscreenTorch);
                mPrefCatMiscOther.removePreference(mPrefTorchAutoOff);
            }
            if (!Utils.hasVibrator(getActivity())) {
                mPrefCatPhoneTelephony.removePreference(mPrefCallVibrations);
            }
            if (!Utils.hasProximitySensor(getActivity())) {
                mPrefCatPowerOther.removePreference(mPrefPowerProximityWake);
                mPrefCatPowerOther.removePreference(mPrefProximityWakeIgnoreCall);
            }
            if (!Utils.hasTelephonySupport(getActivity())) {
                mPrefCatPhone.removePreference(mPrefCatPhoneTelephony);
                mPrefCatMedia.removePreference(mPrefLinkVolumes);
            }
            // TODO: launcher tweaks? probably not...
            //if (!(Utils.isAppInstalled(getActivity(), APP_GOOGLE_NOW) &&
            //        Utils.isAppInstalled(getActivity(), APP_GOOGLE_HOME) ||
            //        Utils.isAppInstalled(getActivity(), APP_STOCK_LAUNCHER))) {
                getPreferenceScreen().removePreference(mPrefCatLauncherTweaks);
            //}
            if (Utils.isWifiOnly(getActivity())) {
                // Remove preferences that don't apply to wifi-only devices
                getPreferenceScreen().removePreference(mPrefCatPhone);
                mPrefCatQsTileSettings.removePreference(mPrefCatCellTile);
                mPrefCatQsTileSettings.removePreference(mPrefCatQsNmTileSettings);
                mPrefCatStatusbar.removePreference(mPrefDisableRoamingIndicators);
                mPrefCatQsNmTileSettings.removePreference(mPrefQsNetworkModeSimSlot);
                mPrefCatPowerOther.removePreference(mPrefProximityWakeIgnoreCall);
            }

            // remove Dialer features if Dialer package unavailable
            PackageInfo pi = null;
            for (String pkg : ModDialer.PACKAGE_NAMES) {
                pi = Utils.getPackageInfo(getActivity(), pkg);
                if (pi != null) break;
            }
            if (pi == null) {
                mPrefCatPhone.removePreference(mPrefCatPhoneDialer);
            } else if (pi.applicationInfo.targetSdkVersion >= 25) {
                Preference p = findPreference(PREF_KEY_CALLER_FULLSCREEN_PHOTO);
                if (p != null) mPrefCatPhoneDialer.removePreference(p);
                mPrefCatPhoneDialer.removePreference(mPrefCallerUnknownPhotoEnable);
                mPrefCatPhoneDialer.removePreference(mPrefCallerUnknownPhoto);
            }

            // Remove MTK specific preferences for non-MTK devices
            if (!Utils.isMtkDevice()) {
                mPrefCatStatusbar.removePreference(mPrefDisableRoamingIndicators);
                if (!sSystemProperties.hasMsimSupport) {
                    mPrefCatStatusbarColors.removePreference(mPrefSbIconColorSecondary);
                }
                mPrefCatSignalCluster.removePreference(mPrefSbDaColorSecondary);
                Preference p = findPreference(PREF_KEY_SIGNAL_CLUSTER_DNTI);
                if (p != null) {
                    mPrefCatSignalCluster.removePreference(p);
                }
            } else {
                Preference hPlusPref = findPreference(PREF_KEY_SIGNAL_CLUSTER_HPLUS);
                if (hPlusPref != null) {
                    mPrefCatSignalCluster.removePreference(hPlusPref);
                }
                mPrefCatLsOther.removePreference(mPrefLsRotation);
                Preference p = findPreference(PREF_KEY_SIGNAL_CLUSTER_DEM);
                if (p != null) {
                    mPrefCatSignalCluster.removePreference(p);
                }
                // Remove Gemini specific preferences for non-Gemini MTK devices
                if (!sSystemProperties.hasGeminiSupport) {
                    mPrefCatStatusbar.removePreference(mPrefDisableRoamingIndicators);
                }
                if (!sSystemProperties.hasGeminiSupport && !sSystemProperties.hasMsimSupport) {
                    mPrefCatStatusbarColors.removePreference(mPrefSbIconColorSecondary);
                }
            }

            // Remove preferences not compatible with Lenovo VibeUI ROMs
            if (Utils.hasLenovoVibeUI()) {
                getPreferenceScreen().removePreference(mPrefCatLockscreen);
                mPrefCatStatusbar.removePreference(mPrefCatStatusbarQs);
                mPrefCatNotifDrawerStyle.removePreference(mPrefNotifExpandAll);
            }

            // Remove Moto XT preferences
            if (Utils.isMotoXtDevice()) {
                Preference p = findPreference(PREF_KEY_SIGNAL_CLUSTER_NOSIM);
                if (p != null) mPrefCatSignalCluster.removePreference(p);
            }

            // Remove Moto G DS (falcon_asia_ds) preferences
            if (Utils.isFalconAsiaDs()) {
                Preference p = findPreference(PREF_KEY_SIGNAL_CLUSTER_HPLUS);
                if (p != null) mPrefCatSignalCluster.removePreference(p);
                p = findPreference(PREF_KEY_SIGNAL_CLUSTER_DEM);
                if (p != null) mPrefCatSignalCluster.removePreference(p);
            }

            // Remove MSIM preferences for non-MSIM devices
            if (!sSystemProperties.hasMsimSupport) {
                mPrefCatSignalCluster.removePreference(mPrefScHideSimLabels);
                mPrefCatSignalCluster.removePreference(mPrefScNarrow);
                mPrefCatQsNmTileSettings.removePreference(mPrefQsNetworkModeSimSlot);
            } else {
                mPrefCatSignalCluster.removePreference(mPrefScHideSimLabels);
                mPrefCatSignalCluster.removePreference(mPrefScNarrow);
            }

            // Remove Xperia preferences
            if (Utils.isXperiaDevice()) {
                mPrefCatLsOther.removePreference(mPrefLockscreenCarrierText);
                Preference p = findPreference(PREF_KEY_SIGNAL_CLUSTER_NOSIM);
                if (p != null) mPrefCatSignalCluster.removePreference(p);
            }

            // Features not relevant for KitKat but keep them for potential future use
            mPrefCatSignalCluster.removePreference(mPrefSbDaColorSecondary);

            // Remove more music volume steps option if necessary
            if (!Utils.shouldAllowMoreVolumeSteps()) {
                mPrefs.edit().putBoolean(PREF_KEY_MUSIC_VOLUME_STEPS, false).commit();
                mPrefCatMedia.removePreference(mPrefMusicVolumeSteps);
                mPrefCatMedia.removePreference(mPrefMusicVolumeStepsValue);
            }

            // Remove Lenovo preferences
            if (Utils.hasLenovoCustomUI()) {
                PreferenceScreen ps = (PreferenceScreen) findPreference(PREF_CAT_KEY_BATTERY_SETTINGS);
                PreferenceScreen ps2 = (PreferenceScreen) findPreference(PREF_CAT_KEY_BATTERY_PERCENT_TEXT);
                ps.removePreference(ps2);
            }

            // Remove Vernee Apollo Lite preferences
            if (Utils.isVerneeApolloDevice()) {
                mPrefCatDisplay.removePreference(mPrefPulseNotificationDelay);
            }

            // Remove OxygenOS preferences
            if (sSystemProperties.isOxygenOsRom) {
                Preference p = findPreference(PREF_KEY_LOCKSCREEN_IMPRINT_MODE);
                if (p != null) mPrefCatLsOther.removePreference(p);
                p = findPreference(PREF_KEY_SIGNAL_CLUSTER_DATA_ACTIVITY);
                if (p != null) mPrefCatSignalCluster.removePreference(p);
                p = findPreference(PREF_KEY_SIGNAL_CLUSTER_LTE_STYLE);
                if (p != null) mPrefCatSignalCluster.removePreference(p);
                mPrefCatSignalCluster.removePreference(mPrefSbDaColor);
                p = findPreference(PREF_KEY_SIGNAL_CLUSTER_HPLUS);
                if (p != null) mPrefCatSignalCluster.removePreference(p);
                mPrefCatPower.removePreference(mPrefCatPowerMenu);
                p = findPreference(PREF_CAT_KEY_BATTERY_TILE);
                if (p != null) mPrefCatQsTileSettings.removePreference(p);
                p = findPreference(PREF_CAT_KEY_QS_DND_TILE_SETTINGS);
                if (p != null) mPrefCatQsTileSettings.removePreference(p);
                mPrefCatPhone.removePreference(mPrefCatPhoneDialer);
                mBatteryStyle.setEntries(R.array.battery_style_entries_oos);
                mBatteryStyle.setEntryValues(R.array.battery_style_values_oos);
            } else {
                PreferenceScreen ps = (PreferenceScreen) findPreference(PREF_CAT_KEY_BATTERY_SETTINGS);
                Preference p = findPreference(PREF_KEY_BATTERY_HIDE_DASH_ICON);
                if (ps != null && p != null) ps.removePreference(p);
            }

            // Remove fingerprint related preferences
            if (!sSystemProperties.supportsFingerprint) {
                Preference p = findPreference(PREF_KEY_LOCKSCREEN_IMPRINT_MODE);
                if (p != null) mPrefCatLsOther.removePreference(p);
                p = findPreference(PREF_KEY_IMPRINT_VIBE_DISABLE);
                if (p != null) mPrefCatLsOther.removePreference(p);
                p = findPreference(PREF_CAT_KEY_FINGERPRINT_LAUNCHER);
                if (p != null) getPreferenceScreen().removePreference(p);
            }

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
            mPrefHwKeyMenuSingletap.setEntries(actionEntries);
            mPrefHwKeyMenuSingletap.setEntryValues(actionEntryValues);
            mPrefHwKeyMenuLongpress.setEntries(actionEntries);
            mPrefHwKeyMenuLongpress.setEntryValues(actionEntryValues);
            // other preferences have the exact same entries and entry values
            mPrefHwKeyMenuDoubletap.setEntries(actionEntries);
            mPrefHwKeyMenuDoubletap.setEntryValues(actionEntryValues);
            mPrefHwKeyHomeLongpress.setEntries(actionEntries);
            mPrefHwKeyHomeLongpress.setEntryValues(actionEntryValues);
            mPrefHwKeyHomeDoubletap.setEntries(actionEntries);
            mPrefHwKeyHomeDoubletap.setEntryValues(actionEntryValues);
            mPrefHwKeyBackSingletap.setEntries(actionEntries);
            mPrefHwKeyBackSingletap.setEntryValues(actionEntryValues);
            mPrefHwKeyBackLongpress.setEntries(actionEntries);
            mPrefHwKeyBackLongpress.setEntryValues(actionEntryValues);
            mPrefHwKeyBackDoubletap.setEntries(actionEntries);
            mPrefHwKeyBackDoubletap.setEntryValues(actionEntryValues);
            mPrefHwKeyRecentsSingletap.setEntries(actionEntries);
            mPrefHwKeyRecentsSingletap.setEntryValues(actionEntryValues);
            // TODO: recents key long-press
            //mPrefHwKeyRecentsLongpress.setEntries(actionEntries);
            //mPrefHwKeyRecentsLongpress.setEntryValues(actionEntryValues);
            mPrefHwKeyRecentsDoubletap.setEntries(actionEntries);
            mPrefHwKeyRecentsDoubletap.setEntryValues(actionEntryValues);
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

            if (sSystemProperties.fingerprintIds != null) {
                initFingerprintLauncher();
            }
            setDefaultValues();
            checkPermissions();
        }

        private void initFingerprintLauncher() {
            PreferenceCategory catFingers = (PreferenceCategory) findPreference(
                    PREF_CAT_KEY_FINGERPRINT_LAUNCHER_FINGERS);
            for (int i = 0; i < sSystemProperties.fingerprintIds.length; i++) {
                AppPickerPreference appPref = new AppPickerPreference(getActivity(), null);
                String key = PREF_KEY_FINGERPRINT_LAUNCHER_FINGER + String.valueOf(i);
                appPref.setKey(key);
                appPref.setTitle(String.format("%s %d",
                        getActivity().getString(R.string.finger), i + 1));
                appPref.setDialogTitle(appPref.getTitle());
                appPref.setDefaultSummary(getActivity().getString(R.string.app_picker_none));
                appPref.setSummary(getActivity().getString(R.string.app_picker_none));
                appPref.setPersistent(false);
                appPref.setIconPickerEnabled(false);
                String fingerId = String.valueOf(sSystemProperties.fingerprintIds[i]);
                appPref.getExtraData().putString("fingerId", fingerId);
                catFingers.addPreference(appPref);
                Set<String> currentSet = mPrefs.getStringSet(key, null);
                if (currentSet != null) {
                    String currentFingerId = null, currentApp = null;
                    for (String val : currentSet) {
                        String[] data = val.split(":", 2);
                        if ("fingerId".equals(data[0])) {
                            currentFingerId = data[1];
                        } else if ("app".equals(data[0])) {
                            currentApp = data[1];
                        }
                    }
                    if (!fingerId.equals(currentFingerId)) {
                        Set<String> newSet = new HashSet<>();
                        newSet.add("fingerId:" + fingerId);
                        if (currentApp != null) {
                            newSet.add("app:" + currentApp);
                        }
                        mPrefs.edit().putStringSet(key, newSet).commit();
                        Intent intent = new Intent(ACTION_FPL_SETTINGS_CHANGED);
                        intent.putExtra(EXTRA_FPL_FINGER_ID, fingerId);
                        intent.putExtra(EXTRA_FPL_APP, currentApp);
                        getActivity().sendBroadcast(intent);
                    }
                    if (currentApp != null) {
                        appPref.setValue(currentApp);
                    }
                }
                appPref.setOnPreferenceChangeListener(this);
            }

            // set default quick tap as Go Home for OOS 4.1
            if (sSystemProperties.isOxygenOsRom &&
                    mPrefs.getString(PREF_KEY_FINGERPRINT_LAUNCHER_APP, null) == null) {
                AppPickerPreference appp = (AppPickerPreference)
                        findPreference(PREF_KEY_FINGERPRINT_LAUNCHER_APP);
                appp.setValue(GoHomeShortcut.ACTION_DEFAULT_URI);
                Intent intent = new Intent(ACTION_FPL_SETTINGS_CHANGED);
                intent.putExtra(EXTRA_FPL_APP, appp.getValue());
                getActivity().sendBroadcast(intent);
            }
        }

        private void checkPermissions() {
            String[] perms = new String[] { permission.READ_EXTERNAL_STORAGE,
                    permission.WRITE_EXTERNAL_STORAGE, permission.RECORD_AUDIO };

            List<String> reqPerms = new ArrayList<>();
            for (String perm : perms) {
                if (getActivity().checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                    reqPerms.add(perm);
                }
            }
            if (!reqPerms.isEmpty()) {
                requestPermissions(reqPerms.toArray(new String[]{}), 0);
            } else {
                maybeShowCompatWarningDialog();
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            boolean showWarning = false;
            for (int res : grantResults) {
                showWarning = res != PackageManager.PERMISSION_GRANTED;
            }
            showWarning &= !mPrefs.getBoolean("permission_denied_hide", false);
            if (showWarning) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.important)
                .setMessage(R.string.permission_denied_msg)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(R.string.permission_denied_hide_title, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPrefs.edit().putBoolean("permission_denied_hide", true).commit();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        maybeShowCompatWarningDialog();
                    }
                });
                mDialog = builder.create();
                mDialog.show();
            } else {
                maybeShowCompatWarningDialog();
            }
        }

        @Override
        public void onPause() {
            if (mTransWebServiceClient != null) {
                mTransWebServiceClient.abortTaskIfRunning();
            }

            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
                mDialog = null;
            }

            super.onPause();
        }

        @Override
        public void onStart() {
            super.onStart();
            mPrefs.registerOnSharedPreferenceChangeListener(this);
            updatePreferences(null);
        }

        @Override
        public void onStop() {
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);
            super.onStop();
        }

        private void maybeShowCompatWarningDialog() {
            final int stage = mPrefs.getInt("compat_warning_stage", 0);
            if (stage < 2) {
                final TextView msgView = new TextView(getActivity());
                msgView.setText(R.string.compat_warning_message);
                msgView.setMovementMethod(LinkMovementMethod.getInstance());
                final int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                        getResources().getDisplayMetrics());
                msgView.setPadding(padding, padding, padding, padding);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.compat_warning_title);
                builder.setView(msgView);
                builder.setPositiveButton(stage == 0 ? R.string.compat_warning_ok_stage1 :
                    R.string.compat_warning_ok_stage2, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mPrefs.edit().putInt("compat_warning_stage", (stage+1)).commit();
                        }
                    });
                builder.setNegativeButton(android.R.string.cancel, null);
                mDialog = builder.create();
                mDialog.show();
            }
        }

        private void setDefaultValues() {
            if (!mPrefs.getBoolean(PREF_KEY_NAVBAR_ENABLE + "_set", false)) {
                mPrefs.edit().putBoolean(PREF_KEY_NAVBAR_ENABLE, sSystemProperties.hasNavigationBar).commit();
                mPrefNavbarEnable.setChecked(sSystemProperties.hasNavigationBar);
            }

            if (!mPrefs.getBoolean(PREF_KEY_UNPLUG_TURNS_ON_SCREEN + "_set", false)) {
                mPrefs.edit().putBoolean(PREF_KEY_UNPLUG_TURNS_ON_SCREEN, 
                        sSystemProperties.unplugTurnsOnScreen).commit();
                mPrefUnplugTurnsOnScreen.setChecked(sSystemProperties.unplugTurnsOnScreen);
            }

            if (!mPrefs.getBoolean(PREF_KEY_PULSE_NOTIFICATION_DELAY + "_set", false)) {
                int delay = Math.min(Math.max(sSystemProperties.defaultNotificationLedOff, 500), 20000);
                mPrefs.edit()
                    .putInt(PREF_KEY_PULSE_NOTIFICATION_DELAY, delay)
                    .putBoolean(PREF_KEY_PULSE_NOTIFICATION_DELAY + "_set", true)
                    .commit();
                mPrefPulseNotificationDelay.setDefaultValue(delay);
                mPrefPulseNotificationDelay.setValue(delay);
            }

            restrictFeatures();
            if (sSystemProperties.uuidRegistered) {
                if ("PayPal".equals(sSystemProperties.uuidType)) {
                    unrestrictFeatures();
                } else {
                    UnlockActivity.checkPolicyOk(getContext(), new UnlockActivity.CheckPolicyHandler() {
                        @Override
                        public void onPolicyResult(boolean ok) {
                            if (ok) {
                                unrestrictFeatures();
                                mPrefs.edit().putInt("policy_counter", 0).commit();
                            } else {
                                int cnt = mPrefs.getInt("policy_counter", 0) + 1;
                                if (cnt > 3) {
                                    SettingsManager.getInstance(getContext()).resetUuid();
                                } else {
                                    mPrefs.edit().putInt("policy_counter", cnt).commit();
                                    unrestrictFeatures();
                                }
                            }
                        }
                    });
                }
            } else {
                UnlockActivity.maybeRunUnlocker(getContext());
            }

            WebServiceClient.getAppSignatureHash(getActivity());
        }

        private void restrictFeatures() {
            mPrefBackup.setEnabled(false);
            mPrefBackup.setSummary(R.string.wsc_trans_required_summary);
            mPrefRestore.setEnabled(false);
            mPrefRestore.setSummary(R.string.wsc_trans_required_summary);
            if (sSystemProperties.uncTrialCountdown == 0) {
                mPrefLedControl.setEnabled(false);
                mPrefLedControl.setSummary(String.format("%s (%s)", mPrefLedControl.getSummary(),
                    getString(R.string.wsc_trans_required_summary)));
                LedSettings.lockUnc(getActivity(), true);
            }
            mPrefs.edit().putString(PREF_KEY_TRANS_VERIFICATION, null).commit();
            mPrefTransVerification.setText(null);
            mPrefTransVerification.getEditText().setText(null);
        }

        private void unrestrictFeatures() {
            mPrefBackup.setEnabled(true);
            mPrefBackup.setSummary(null);
            mPrefRestore.setEnabled(true);
            mPrefRestore.setSummary(null);
            mPrefLedControl.setEnabled(true);
            mPrefLedControl.setSummary(R.string.pref_led_control_summary);
            LedSettings.lockUnc(getActivity(), false);
            mPrefCatAbout.removePreference(mPrefTransVerification);
            mPrefCatAbout.removePreference(mPrefAboutUnlocker);
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
                String option = mPrefs.getString(PREF_KEY_NOTIF_BACKGROUND, NOTIF_BG_DEFAULT);
                mPrefNotifColor.setEnabled(option.equals(NOTIF_BG_COLOR));
                mPrefNotifImagePortrait.setEnabled(option.equals(NOTIF_BG_IMAGE));
                mPrefNotifImageLandscape.setEnabled(option.equals(NOTIF_BG_IMAGE));
                mPrefNotifAlpha.setEnabled(!option.equals(NOTIF_BG_DEFAULT));
            }

            if (key == null || key.equals(PREF_KEY_BUTTON_BACKLIGHT_MODE)) {
                mPrefButtonBacklightMode.setSummary(mPrefButtonBacklightMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_PIE_CONTROL_ENABLE)) {
                final int pieMode = 
                        Integer.valueOf(mPrefs.getString(PREF_KEY_PIE_CONTROL_ENABLE, "0"));
                if (pieMode == 0) {
                    if (mPrefPieHwKeysDisabled.isChecked()) {
                        mPrefs.edit().putBoolean(PREF_KEY_HWKEYS_DISABLE, false).commit();
                        mPrefPieHwKeysDisabled.setChecked(false);
                    }
                    mPrefPieHwKeysDisabled.setEnabled(false);
                } else {
                    mPrefPieHwKeysDisabled.setEnabled(true);
                }
                mPrefPieEnabled.setSummary(mPrefPieEnabled.getEntry());
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
                mPrefCatNavbarColor.setEnabled(override && mPrefNavbarEnable.isChecked());
                mPrefCatNavbarDimen.setEnabled(override && mPrefNavbarEnable.isChecked());
            }

            if (key == null || key.equals(PREF_KEY_QS_NETWORK_MODE_SIM_SLOT)) {
                mPrefQsNetworkModeSimSlot.setSummary(
                        String.format(getString(R.string.pref_qs_network_mode_sim_slot_summary),
                                mPrefQsNetworkModeSimSlot.getEntry()));
            }

            if (key == null || key.equals(PREF_KEY_STATUSBAR_CLOCK_DATE)) {
                mPrefSbClockDate.setSummary(mPrefSbClockDate.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_STATUSBAR_CLOCK_DOW)) {
                mPrefSbClockDow.setSummary(mPrefSbClockDow.getEntry());
                mPrefSbClockDowSize.setEnabled(Integer.valueOf(
                        mPrefSbClockDow.getValue()) != 0);
            }

            if (key == null || key.equals(PREF_KEY_DATA_TRAFFIC_POSITION)) {
                mPrefDataTrafficPosition.setSummary(mPrefDataTrafficPosition.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_DATA_TRAFFIC_SIZE)) {
                mPrefDataTrafficSize.setSummary(mPrefDataTrafficSize.getEntry());
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

            if (key == null || key.equals(PREF_KEY_TRANSLUCENT_DECOR)) {
                mPrefTranclucentDecor.setSummary(mPrefTranclucentDecor.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_LAUNCHER_DESKTOP_GRID_ROWS)) {
                mPrefLauncherDesktopGridRows.setSummary(mPrefLauncherDesktopGridRows.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_LAUNCHER_DESKTOP_GRID_COLS)) {
                mPrefLauncherDesktopGridCols.setSummary(mPrefLauncherDesktopGridCols.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_VOLUME_ROCKER_WAKE)) {
                mPrefVolumeRockerWake.setSummary(mPrefVolumeRockerWake.getEntry());
                Preference p = findPreference(PREF_KEY_VOLUME_ROCKER_WAKE_ALLOW_MUSIC);
                p.setEnabled("enabled".equals(mPrefVolumeRockerWake.getValue()));
            }

            if (key == null || key.equals(PREF_KEY_DATA_TRAFFIC_OMNI_MODE)) {
                mPrefDataTrafficOmniMode.setSummary(mPrefDataTrafficOmniMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_DATA_TRAFFIC_INACTIVITY_MODE)) {
                mPrefDataTrafficInactivityMode.setSummary(mPrefDataTrafficInactivityMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_BATTERY_PERCENT_TEXT_SIZE)) {
                mPrefBatteryPercentSize.setSummary(mPrefBatteryPercentSize.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_BATTERY_PERCENT_TEXT_STYLE)) {
                mPrefBatteryPercentStyle.setSummary(mPrefBatteryPercentStyle.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING)) {
                mPrefBatteryPercentCharging.setSummary(mPrefBatteryPercentCharging.getEntry());
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

//            if (key == null || key.equals(PREF_KEY_QUICK_SETTINGS_TILE_LABEL_STYLE)) {
//                mPrefQsTileLabelStyle.setSummary(mPrefQsTileLabelStyle.getEntry());
//            }

            if (key == null || key.equals(PREF_KEY_QUICK_PULLDOWN)) {
                mPrefQuickPulldownSize.setEnabled(!"0".equals(mPrefQuickPulldown.getValue()) &&
                        mPrefs.getBoolean(GravityBoxSettings.PREF_KEY_QUICK_SETTINGS_ENABLE, false));
            }

            if (key == null || key.equals(PREF_KEY_SCREENRECORD_SIZE)) {
                mPrefScreenrecordSize.setSummary(mPrefScreenrecordSize.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_DATA_TRAFFIC_MODE)) {
                mPrefDataTrafficMode.setSummary(mPrefDataTrafficMode.getEntry());
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficPosition);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficLs);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficSize);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficInactivityMode);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficOmniMode);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficOmniShowIcon);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficActiveMobileOnly);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficDisplayMode);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficOmniAutohide);
                mPrefCatDataTraffic.removePreference(mPrefDataTrafficOmniAutohideTh);
                String mode = mPrefDataTrafficMode.getValue();
                if (!mode.equals("OFF")) {
                    if (!Utils.isWifiOnly(getActivity())) {
                        mPrefCatDataTraffic.addPreference(mPrefDataTrafficActiveMobileOnly);
                    }
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficDisplayMode);
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficPosition);
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficLs);
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficSize);
                }
                if (mode.equals("SIMPLE")) {
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficInactivityMode);
                } else if (mode.equals("OMNI")) {
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficOmniMode);
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficOmniShowIcon);
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficOmniAutohide);
                    mPrefCatDataTraffic.addPreference(mPrefDataTrafficOmniAutohideTh);
                }
            }

            if (key == null || key.equals(PREF_KEY_DATA_TRAFFIC_DISPLAY_MODE)) {
                mPrefDataTrafficDisplayMode.setSummary(mPrefDataTrafficDisplayMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_SIGNAL_CLUSTER_LTE_STYLE)) {
                mPrefScLteStyle.setSummary(mPrefScLteStyle.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_STATUSBAR_BT_VISIBILITY)) {
                mPrefSbBtVisibility.setSummary(mPrefSbBtVisibility.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_CHARGING_LED)) {
                mPrefChargingLed.setSummary(mPrefChargingLed.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_QUICKRECORD_QUALITY)) {
                mPrefQrQuality.setSummary(mPrefQrQuality.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_SMART_RADIO_SCREEN_OFF_DELAY)) {
                mPrefSrAdaptiveDelay.setEnabled(
                        mPrefs.getBoolean(PREF_KEY_SMART_RADIO_ENABLE, false) &&
                        mPrefs.getBoolean(PREF_KEY_SMART_RADIO_SCREEN_OFF, false) &&
                        mPrefs.getInt(PREF_KEY_SMART_RADIO_SCREEN_OFF_DELAY, 0) > 0);
            }

            if (key == null || key.equals(PREF_KEY_BATTERY_BAR_POSITION)) {
                mPrefBbarPosition.setSummary(mPrefBbarPosition.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS)) {
                mPrefSbdpMode.setSummary(mPrefSbdpMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_BATTERY_TILE_TEMP_UNIT)) {
                mPrefBatteryTileTempUnit.setSummary(mPrefBatteryTileTempUnit.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_LOCKSCREEN_DIRECT_UNLOCK) ||
                    key.equals(PREF_KEY_LOCKSCREEN_DIRECT_UNLOCK_POLICY)) {
                ListPreference du = (ListPreference) findPreference(PREF_KEY_LOCKSCREEN_DIRECT_UNLOCK);
                ListPreference dup = (ListPreference) findPreference(PREF_KEY_LOCKSCREEN_DIRECT_UNLOCK_POLICY);
                dup.setEnabled(!"OFF".equals(du.getValue()));
                dup.setSummary(dup.getEntry());
                Preference p = findPreference(PREF_KEY_LOCKSCREEN_DIRECT_UNLOCK_TRANS_LEVEL);
                if (p != null) p.setEnabled("SEE_THROUGH".equals(du.getValue()));
            }

            if (key == null || key.equals(PREF_KEY_LOCKSCREEN_SMART_UNLOCK_POLICY)) {
                ListPreference sup = (ListPreference) findPreference(PREF_KEY_LOCKSCREEN_SMART_UNLOCK_POLICY);
                sup.setSummary(sup.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_CELL_TILE_DATA_TOGGLE)) {
                ListPreference p = (ListPreference) findPreference(PREF_KEY_CELL_TILE_DATA_TOGGLE);
                if (p != null) p.setSummary(p.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_LOCKSCREEN_IMPRINT_MODE)) {
                ListPreference p = (ListPreference) findPreference(PREF_KEY_LOCKSCREEN_IMPRINT_MODE);
                if (p != null) p.setSummary(p.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_DND_TILE_DURATION_MODE)) {
                ListPreference p = (ListPreference) findPreference(PREF_KEY_DND_TILE_DURATION_MODE);
                if (p != null) {
                    p.setSummary(p.getEntry());
                    Preference dp = findPreference(PREF_KEY_DND_TILE_DURATION);
                    dp.setEnabled(p.isEnabled() && "CUSTOM".equals(p.getValue()));
                }
            }

            if (key == null || key.equals(PREF_KEY_IMPRINT_VIBE_DISABLE)) {
                MultiSelectListPreference p = (MultiSelectListPreference) findPreference(PREF_KEY_IMPRINT_VIBE_DISABLE);
                if (p != null) {
                    String summary = "";
                    Set<String> values = p.getValues() == null ? new HashSet<String>() : p.getValues();
                    if (values.contains("SUCCESS")) summary += getString(R.string.imprint_vibe_disable_success);
                    if (values.contains("ERROR")) {
                        if (!summary.isEmpty()) summary += ", ";
                        summary += getString(R.string.imprint_vibe_disable_error);
                    }
                    p.setSummary(summary);
                }
            }

            if (key == null || key.equals(PREF_KEY_NAVBAR_AUTOFADE_KEYS)) {
                Preference p = findPreference(PREF_KEY_NAVBAR_AUTOFADE_SHOW_KEYS);
                if (p != null) p.setEnabled(mPrefs.getInt(PREF_KEY_NAVBAR_AUTOFADE_KEYS, 0) != 0);
            }

            if (key == null || key.equals(PREF_KEY_NAVBAR_AUTOFADE_SHOW_KEYS)) {
                ListPreference p = (ListPreference) findPreference(PREF_KEY_NAVBAR_AUTOFADE_SHOW_KEYS);
                p.setSummary(p.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_NAVBAR_CUSTOM_KEY_ICON_STYLE) ||
                    key.equals(PREF_KEY_NAVBAR_CUSTOM_KEY_ENABLE)) {
                ListPreference p = (ListPreference) findPreference(PREF_KEY_NAVBAR_CUSTOM_KEY_ICON_STYLE);
                p.setSummary(p.getEntry());
                Preference p2 = findPreference(PREF_KEY_NAVBAR_CUSTOM_KEY_IMAGE);
                p2.setEnabled(mPrefs.getBoolean(PREF_KEY_NAVBAR_CUSTOM_KEY_ENABLE, false) &&
                        "CUSTOM".equals(p.getValue()));
            }

            if (key == null || key.equals(PREF_KEY_FORCE_AOSP)) {
                CheckBoxPreference p = (CheckBoxPreference) findPreference(PREF_KEY_FORCE_AOSP);
                p.setChecked(Utils.isAospForced());
            }

            if (key == null || key.equals(PREF_KEY_LOCKSCREEN_BOTTOM_ACTIONS_HIDE)) {
                MultiSelectListPreference p = (MultiSelectListPreference) findPreference(PREF_KEY_LOCKSCREEN_BOTTOM_ACTIONS_HIDE);
                if (p != null) {
                    String summary = "";
                    Set<String> values = p.getValues() == null ? new HashSet<String>() : p.getValues();
                    if (values.contains("LEFT")) summary += getString(R.string.bottom_action_left);
                    if (values.contains("RIGHT")) {
                        if (!summary.isEmpty()) summary += ", ";
                        summary += getString(R.string.bottom_action_right);
                    }
                    p.setSummary(summary);
                    Preference p2 = findPreference(PREF_KEY_LOCKSCREEN_BLEFT_ACTION_CUSTOM);
                    if (p2 != null) p2.setEnabled(!values.contains("LEFT"));
                    p2 = findPreference(PREF_KEY_LOCKSCREEN_BRIGHT_ACTION_CUSTOM);
                    if (p2 != null) p2.setEnabled(!values.contains("RIGHT"));
                }
            }

            for (String caKey : customAppKeys) {
                ListPreference caPref = (ListPreference) findPreference(caKey);
                if ((caKey + "_custom").equals(key) && mPrefCustomApp.getValue() != null) {
                    caPref.setSummary(mPrefCustomApp.getSummary());
                    Intent intent = new Intent(ACTION_PREF_HWKEY_CHANGED);
                    intent.putExtra(EXTRA_HWKEY_KEY, caKey);
                    intent.putExtra(EXTRA_HWKEY_VALUE, HWKEY_ACTION_CUSTOM_APP);
                    intent.putExtra(EXTRA_HWKEY_CUSTOM_APP, mPrefCustomApp.getValue());
                    mPrefs.edit().commit();
                    getActivity().sendBroadcast(intent);
                } else if (key == null || customAppKeys.contains(key)) {
                    String value = caPref.getValue();
                    if (value != null && Integer.valueOf(value) == HWKEY_ACTION_CUSTOM_APP) {
                        mPrefCustomApp.setKey(caKey + "_custom");
                        mPrefCustomApp.setValue(
                                mPrefs.getString(caKey + "_custom", null));
                        caPref.setSummary(mPrefCustomApp.getSummary());
                    } else {
                        caPref.setSummary(caPref.getEntry());
                    }
                }
            }

            for (String rtKey : ringToneKeys) {
                RingtonePreference rtPref = (RingtonePreference) findPreference(rtKey);
                String val = mPrefs.getString(rtKey, null);
                if (val != null && !val.isEmpty()) {
                    if (rtKey.equals(PREF_KEY_CHARGER_PLUGGED_SOUND_WIRELESS) &&
                            val.equals("content://settings/system/notification_sound")) {
                        rtPref.setSummary(R.string.stock_wireless_sound);
                    } else {
                        Uri uri = Uri.parse(val);
                        Ringtone r = RingtoneManager.getRingtone(getActivity(), uri);
                        if (r != null) {
                            rtPref.setSummary(r.getTitle(getActivity()));
                        }
                    }
                } else {
                    rtPref.setSummary(R.string.lc_notif_sound_none);
                }
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (customAppKeys.contains(key)) { 
                if (Integer.valueOf(prefs.getString(key, "0")) == HWKEY_ACTION_CUSTOM_APP) {
                    Intent intent = new Intent(ACTION_PREF_HWKEY_CHANGED);
                    intent.putExtra(EXTRA_HWKEY_KEY, key);
                    intent.putExtra(EXTRA_HWKEY_VALUE, HWKEY_ACTION_CUSTOM_APP);
                    mPrefs.edit().commit();
                    getActivity().sendBroadcast(intent);
                    findPreference(key).setSummary(R.string.app_picker_none);
                    mPrefCustomApp.setKey(key + "_custom");
                    mPrefCustomApp.show();
                    return;
                } else {
                    mPrefs.edit().putString(key + "_custom", null).commit();
                }
            }
            updatePreferences(key);

            boolean delayedBroadcast = false;
            final Intent intent = new Intent();
            if (key.equals(PREF_KEY_BATTERY_STYLE)) {
                intent.setAction(ACTION_PREF_BATTERY_STYLE_CHANGED);
                int batteryStyle = Integer.valueOf(prefs.getString(PREF_KEY_BATTERY_STYLE, "1"));
                intent.putExtra("batteryStyle", batteryStyle);
            } else if (key.equals(PREF_KEY_BATTERY_HIDE_DASH_ICON)) {
                intent.setAction(ACTION_PREF_BATTERY_STYLE_CHANGED);
                intent.putExtra(EXTRA_HIDE_DASH, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_BATTERY_PERCENT_TEXT_STATUSBAR)) {
                intent.setAction(ACTION_PREF_BATTERY_PERCENT_TEXT_CHANGED);
                intent.putExtra(EXTRA_BATTERY_PERCENT_TEXT_STATUSBAR, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_BATTERY_PERCENT_TEXT_KEYGUARD)) {
                intent.setAction(ACTION_PREF_BATTERY_PERCENT_TEXT_CHANGED);
                intent.putExtra(EXTRA_BATTERY_PERCENT_TEXT_KEYGUARD, prefs.getString(key, "DEFAULT"));
            } else if (key.equals(PREF_KEY_BATTERY_PERCENT_TEXT_SIZE)) {
                intent.setAction(ACTION_PREF_BATTERY_PERCENT_TEXT_SIZE_CHANGED);
                intent.putExtra(EXTRA_BATTERY_PERCENT_TEXT_SIZE, Integer.valueOf(
                        prefs.getString(PREF_KEY_BATTERY_PERCENT_TEXT_SIZE, "0")));
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
            } else if (key.equals(PREF_KEY_QUICK_SETTINGS_TILES_PER_ROW)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_COLS, Integer.valueOf(
                        prefs.getString(PREF_KEY_QUICK_SETTINGS_TILES_PER_ROW, "0")));
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
            } else if (key.equals(PREF_KEY_QUICK_SETTINGS_HIDE_BRIGHTNESS)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_HIDE_BRIGHTNESS, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_QS_BRIGHTNESS_ICON)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_BRIGHTNESS_ICON, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_ICON_COLOR_ENABLE,
                        prefs.getBoolean(PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_ICON_COLOR)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_ICON_COLOR, prefs.getInt(PREF_KEY_STATUSBAR_ICON_COLOR, Color.WHITE));
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
            } else if (key.equals(PREF_KEY_STATUSBAR_CENTER_CLOCK)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CENTER_CLOCK, 
                        prefs.getBoolean(PREF_KEY_STATUSBAR_CENTER_CLOCK, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_SHOW_SECONDS)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CLOCK_SHOW_SECONDS, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_DOW)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CLOCK_DOW, Integer.valueOf(
                        prefs.getString(PREF_KEY_STATUSBAR_CLOCK_DOW, "0")));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_DOW_SIZE)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CLOCK_DOW_SIZE, 
                        prefs.getInt(PREF_KEY_STATUSBAR_CLOCK_DOW_SIZE, 70));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_DATE)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CLOCK_DATE, prefs.getString(PREF_KEY_STATUSBAR_CLOCK_DATE, null));
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
            } else if (key.equals(PREF_KEY_HWKEY_MENU_SINGLETAP)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_MENU_SINGLETAP, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_MENU_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_MENU_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_MENU_DOUBLETAP)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_MENU_DOUBLETAP, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_HOME_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_HOME_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_HOME_LONGPRESS_KEYGUARD)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_HOME_LONGPRESS_KG, prefs.getBoolean(
                        GravityBoxSettings.PREF_KEY_HWKEY_HOME_LONGPRESS_KEYGUARD, false));
            } else if (key.equals(PREF_KEY_HWKEY_HOME_DOUBLETAP_DISABLE)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_HOME_DOUBLETAP_DISABLE,
                        prefs.getBoolean(PREF_KEY_HWKEY_HOME_DOUBLETAP_DISABLE, false));
            } else if (key.equals(PREF_KEY_HWKEY_HOME_DOUBLETAP)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_HOME_DOUBLETAP, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_BACK_SINGLETAP)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_BACK_SINGLETAP, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_BACK_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_BACK_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_BACK_DOUBLETAP)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_BACK_DOUBLETAP, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_RECENTS_SINGLETAP)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_RECENTS_SINGLETAP, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_RECENTS_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_RECENTS_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_RECENTS_DOUBLETAP)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_RECENTS_DOUBLETAP, "0")));
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
            } else if (key.equals(PREF_KEY_VOLUME_ROCKER_WAKE_ALLOW_MUSIC)) {
                intent.setAction(ACTION_PREF_VOLUME_ROCKER_WAKE_CHANGED);
                intent.putExtra(EXTRA_VOLUME_ROCKER_WAKE_ALLOW_MUSIC,
                        prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_HWKEY_LOCKSCREEN_TORCH)) {
                intent.setAction(ACTION_PREF_HWKEY_LOCKSCREEN_TORCH_CHANGED);
                intent.putExtra(EXTRA_HWKEY_TORCH, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_LOCKSCREEN_TORCH, "0")));
            } else if (key.equals(PREF_KEY_VOLUME_PANEL_AUTOEXPAND)) {
                intent.setAction(ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                intent.putExtra(EXTRA_AUTOEXPAND, 
                        prefs.getBoolean(PREF_KEY_VOLUME_PANEL_AUTOEXPAND, false));
            } else if (key.equals(PREF_KEY_VOLUME_ADJUST_VIBRATE_MUTE)) {
                intent.setAction(ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                intent.putExtra(EXTRA_VIBRATE_MUTED, prefs.getBoolean(PREF_KEY_VOLUME_ADJUST_VIBRATE_MUTE, false));
            } else if (key.equals(PREF_KEY_VOLUME_PANEL_TIMEOUT)) {
                intent.setAction(ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                intent.putExtra(EXTRA_TIMEOUT, prefs.getInt(key, 0));
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
            } else if (key.equals(PREF_KEY_NOTIF_BACKGROUND_ALPHA)) {
                intent.setAction(ACTION_NOTIF_BACKGROUND_CHANGED);
                intent.putExtra(EXTRA_BG_ALPHA, prefs.getInt(PREF_KEY_NOTIF_BACKGROUND_ALPHA, 0));
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
                        getActivity().getColor(R.color.pie_background_color)));
            } else if (key.equals(PREF_KEY_PIE_COLOR_FG)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_COLOR_FG, prefs.getInt(PREF_KEY_PIE_COLOR_FG, 
                        getActivity().getColor(R.color.pie_foreground_color)));
            } else if (key.equals(PREF_KEY_PIE_COLOR_OUTLINE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_COLOR_OUTLINE, prefs.getInt(PREF_KEY_PIE_COLOR_OUTLINE, 
                        getActivity().getColor(R.color.pie_outline_color)));
            } else if (key.equals(PREF_KEY_PIE_COLOR_SELECTED)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_COLOR_SELECTED, prefs.getInt(PREF_KEY_PIE_COLOR_SELECTED, 
                        getActivity().getColor(R.color.pie_selected_color)));
            } else if (key.equals(PREF_KEY_PIE_COLOR_TEXT)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_COLOR_TEXT, prefs.getInt(PREF_KEY_PIE_COLOR_TEXT, 
                        getActivity().getColor(R.color.pie_text_color)));
            } else if (key.equals(PREF_KEY_PIE_BACK_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_PIE_BACK_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_PIE_HOME_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_PIE_HOME_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_PIE_RECENTS_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_PIE_RECENTS_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_PIE_SEARCH_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_PIE_SEARCH_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_PIE_MENU_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_PIE_MENU_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_PIE_APP_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_PIE_APP_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_PIE_SYSINFO_DISABLE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_SYSINFO_DISABLE,
                        prefs.getBoolean(PREF_KEY_PIE_SYSINFO_DISABLE, false));
            } else if (key.equals(PREF_KEY_PIE_LONGPRESS_DELAY)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_LONGPRESS_DELAY, Integer.valueOf(
                        prefs.getString(PREF_KEY_PIE_LONGPRESS_DELAY, "0")));
            } else if (key.equals(PREF_KEY_PIE_MIRRORED_KEYS)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_MIRRORED_KEYS,
                        prefs.getBoolean(PREF_KEY_PIE_MIRRORED_KEYS, false));
            } else if (key.equals(PREF_KEY_PIE_CENTER_TRIGGER)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_CENTER_TRIGGER,
                        prefs.getBoolean(PREF_KEY_PIE_CENTER_TRIGGER, false));
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
            } else if (key.equals(PREF_KEY_QUICKAPP_DEFAULT_3)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED_3);
                intent.putExtra(EXTRA_QUICKAPP_DEFAULT, prefs.getString(PREF_KEY_QUICKAPP_DEFAULT_3, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT1_3)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED_3);
                intent.putExtra(EXTRA_QUICKAPP_SLOT1, prefs.getString(PREF_KEY_QUICKAPP_SLOT1_3, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT2_3)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED_3);
                intent.putExtra(EXTRA_QUICKAPP_SLOT2, prefs.getString(PREF_KEY_QUICKAPP_SLOT2_3, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT3_3)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED_3);
                intent.putExtra(EXTRA_QUICKAPP_SLOT3, prefs.getString(PREF_KEY_QUICKAPP_SLOT3_3, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT4_3)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED_3);
                intent.putExtra(EXTRA_QUICKAPP_SLOT4, prefs.getString(PREF_KEY_QUICKAPP_SLOT4_3, null));                
            } else if (key.equals(PREF_KEY_QUICKAPP_DEFAULT_4)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED_4);
                intent.putExtra(EXTRA_QUICKAPP_DEFAULT, prefs.getString(PREF_KEY_QUICKAPP_DEFAULT_4, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT1_4)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED_4);
                intent.putExtra(EXTRA_QUICKAPP_SLOT1, prefs.getString(PREF_KEY_QUICKAPP_SLOT1_4, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT2_4)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED_4);
                intent.putExtra(EXTRA_QUICKAPP_SLOT2, prefs.getString(PREF_KEY_QUICKAPP_SLOT2_4, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT3_4)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED_4);
                intent.putExtra(EXTRA_QUICKAPP_SLOT3, prefs.getString(PREF_KEY_QUICKAPP_SLOT3_4, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT4_4)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED_4);
                intent.putExtra(EXTRA_QUICKAPP_SLOT4, prefs.getString(PREF_KEY_QUICKAPP_SLOT4_4, null));
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
            } else if (key.equals(PREF_KEY_NAVBAR_HIDE_IME)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_HIDE_IME, prefs.getBoolean(key, false));
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
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE,
                        Integer.valueOf(prefs.getString(PREF_KEY_NAVBAR_CUSTOM_KEY_SINGLETAP, "12")));
            } else if (key.equals(PREF_KEY_NAVBAR_CUSTOM_KEY_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE,
                        Integer.valueOf(prefs.getString(PREF_KEY_NAVBAR_CUSTOM_KEY_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_NAVBAR_CUSTOM_KEY_DOUBLETAP)) {
                intent.setAction(ACTION_PREF_HWKEY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_KEY, key);
                intent.putExtra(EXTRA_HWKEY_VALUE,
                        Integer.valueOf(prefs.getString(PREF_KEY_NAVBAR_CUSTOM_KEY_DOUBLETAP, "0")));
            } else if (key.equals(PREF_KEY_NAVBAR_CUSTOM_KEY_SWAP)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_CUSTOM_KEY_SWAP,
                        prefs.getBoolean(PREF_KEY_NAVBAR_CUSTOM_KEY_SWAP, false));
            } else if (key.equals(PREF_KEY_NAVBAR_CUSTOM_KEY_ICON_STYLE)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_CUSTOM_KEY_ICON_STYLE,
                        prefs.getString(PREF_KEY_NAVBAR_CUSTOM_KEY_ICON_STYLE, "SIX_DOT"));
            } else if (key.equals(PREF_KEY_NAVBAR_SWAP_KEYS)) {
                intent.setAction(ACTION_PREF_NAVBAR_SWAP_KEYS);
            } else if (key.equals(PREF_KEY_NAVBAR_CURSOR_CONTROL)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_CURSOR_CONTROL,
                        prefs.getBoolean(PREF_KEY_NAVBAR_CURSOR_CONTROL, false));
            } else if (key.equals(PREF_KEY_NAVBAR_COLOR_ENABLE)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_COLOR_ENABLE,
                        prefs.getBoolean(PREF_KEY_NAVBAR_COLOR_ENABLE, false));
            } else if (key.equals(PREF_KEY_NAVBAR_AUTOFADE_KEYS)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_AUTOFADE_KEYS, prefs.getInt(key, 0));
            } else if (key.equals(PREF_KEY_NAVBAR_AUTOFADE_SHOW_KEYS)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_AUTOFADE_SHOW_KEYS, prefs.getString(key, "NAVBAR"));
            } else if (key.equals(PREF_KEY_NAVBAR_KEY_COLOR)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_KEY_COLOR,
                        prefs.getInt(PREF_KEY_NAVBAR_KEY_COLOR, 
                                getActivity().getColor(R.color.navbar_key_color)));
            } else if (key.equals(PREF_KEY_NAVBAR_KEY_GLOW_COLOR)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_KEY_GLOW_COLOR,
                        prefs.getInt(PREF_KEY_NAVBAR_KEY_GLOW_COLOR, 
                                getActivity().getColor(R.color.navbar_key_glow_color)));
            } else if (PREF_KEY_APP_LAUNCHER_SLOT.contains(key)) {
                intent.setAction(ACTION_PREF_APP_LAUNCHER_CHANGED);
                intent.putExtra(EXTRA_APP_LAUNCHER_SLOT,
                        PREF_KEY_APP_LAUNCHER_SLOT.indexOf(key));
                intent.putExtra(EXTRA_APP_LAUNCHER_APP, prefs.getString(key, null));
            } else if (key.equals(PREF_KEY_STATUSBAR_BRIGHTNESS)) {
                intent.setAction(ACTION_PREF_STATUSBAR_CHANGED);
                intent.putExtra(EXTRA_SB_BRIGHTNESS, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_DISABLE_PEEK)) {
                intent.setAction(ACTION_PREF_STATUSBAR_CHANGED);
                intent.putExtra(EXTRA_SB_DISABLE_PEEK, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_DT2S)) {
                intent.setAction(ACTION_PREF_STATUSBAR_CHANGED);
                intent.putExtra(EXTRA_SB_DT2S, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_RINGER_MODE_TILE_MODE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                Set<String> modes = prefs.getStringSet(PREF_KEY_RINGER_MODE_TILE_MODE,
                        new HashSet<String>(Arrays.asList(new String[] { "1", "2", "3" })));
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
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_LOCKSCREEN)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_LOCKSCREEN, prefs.getBoolean(key, true));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_SIZE)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_SIZE, Integer.valueOf(
                        prefs.getString(PREF_KEY_DATA_TRAFFIC_SIZE, "14")));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_INACTIVITY_MODE)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_INACTIVITY_MODE, Integer.valueOf(
                        prefs.getString(PREF_KEY_DATA_TRAFFIC_INACTIVITY_MODE, "0")));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_ACTIVE_MOBILE_ONLY)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_ACTIVE_MOBILE_ONLY,
                        prefs.getBoolean(PREF_KEY_DATA_TRAFFIC_ACTIVE_MOBILE_ONLY, false));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_DISPLAY_MODE)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_DISPLAY_MODE, prefs.getString(key, "ALWAYS"));
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
            } else if (key.equals(PREF_KEY_SMART_RADIO_ADAPTIVE_DELAY)) {
                intent.setAction(ACTION_PREF_SMART_RADIO_CHANGED);
                intent.putExtra(EXTRA_SR_ADAPTIVE_DELAY,
                        prefs.getInt(PREF_KEY_SMART_RADIO_ADAPTIVE_DELAY, 0));
            } else if (key.equals(PREF_KEY_SMART_RADIO_IGNORE_LOCKED)) {
                intent.setAction(ACTION_PREF_SMART_RADIO_CHANGED);
                intent.putExtra(EXTRA_SR_IGNORE_LOCKED,
                        prefs.getBoolean(PREF_KEY_SMART_RADIO_IGNORE_LOCKED, true));
            } else if (key.equals(PREF_KEY_SMART_RADIO_MODE_CHANGE_DELAY)) {
                intent.setAction(ACTION_PREF_SMART_RADIO_CHANGED);
                intent.putExtra(EXTRA_SR_MODE_CHANGE_DELAY,
                        prefs.getInt(PREF_KEY_SMART_RADIO_MODE_CHANGE_DELAY, 5));
            } else if (key.equals(PREF_KEY_SMART_RADIO_MDA_IGNORE)) {
                intent.setAction(ACTION_PREF_SMART_RADIO_CHANGED);
                intent.putExtra(EXTRA_SR_MDA_IGNORE,
                        prefs.getBoolean(PREF_KEY_SMART_RADIO_MDA_IGNORE, false));
            } else if (key.equals(PREF_KEY_LOCKSCREEN_BACKGROUND)) {
                intent.setAction(ACTION_PREF_LOCKSCREEN_BG_CHANGED);
                intent.putExtra(EXTRA_LOCKSCREEN_BG,
                        prefs.getString(PREF_KEY_LOCKSCREEN_BACKGROUND, LOCKSCREEN_BG_DEFAULT));
            } else if (key.equals(PREF_KEY_BATTERY_CHARGED_SOUND)) {
                intent.setAction(ACTION_PREF_BATTERY_SOUND_CHANGED);
                intent.putExtra(EXTRA_BATTERY_SOUND_TYPE, BatteryInfoManager.SOUND_CHARGED);
                intent.putExtra(EXTRA_BATTERY_SOUND_URI,
                        prefs.getString(PREF_KEY_BATTERY_CHARGED_SOUND, ""));
            } else if (key.equals(PREF_KEY_CHARGER_PLUGGED_SOUND_WIRELESS)) {
                intent.setAction(ACTION_PREF_BATTERY_SOUND_CHANGED);
                intent.putExtra(EXTRA_BATTERY_SOUND_TYPE, BatteryInfoManager.SOUND_WIRELESS);
                intent.putExtra(EXTRA_BATTERY_SOUND_URI, prefs.getString(key, ""));
            } else if (key.equals(PREF_KEY_CHARGER_PLUGGED_SOUND)) {
                intent.setAction(ACTION_PREF_BATTERY_SOUND_CHANGED);
                intent.putExtra(EXTRA_BATTERY_SOUND_TYPE, BatteryInfoManager.SOUND_PLUGGED);
                intent.putExtra(EXTRA_BATTERY_SOUND_URI,
                        prefs.getString(PREF_KEY_CHARGER_PLUGGED_SOUND, ""));
            } else if (key.equals(PREF_KEY_CHARGER_UNPLUGGED_SOUND)) {
                intent.setAction(ACTION_PREF_BATTERY_SOUND_CHANGED);
                intent.putExtra(EXTRA_BATTERY_SOUND_TYPE, BatteryInfoManager.SOUND_UNPLUGGED);
                intent.putExtra(EXTRA_BATTERY_SOUND_URI,
                        prefs.getString(PREF_KEY_CHARGER_UNPLUGGED_SOUND, ""));
            } else if (key.equals(PREF_KEY_LOW_BATTERY_WARNING_POLICY)) {
                intent.setAction(ACTION_PREF_LOW_BATTERY_WARNING_POLICY_CHANGED);
                intent.putExtra(EXTRA_LOW_BATTERY_WARNING_POLICY, prefs.getString(key, "DEFAULT"));
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
                Intent restartIntent = new Intent(getActivity(), GravityBoxSettings.class);
                restartIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(restartIntent);
                System.exit(0);
                return;
            } else if (key.equals(PREF_KEY_STATUSBAR_BT_VISIBILITY)) {
                intent.setAction(ACTION_PREF_SYSTEM_ICON_CHANGED);
                intent.putExtra(EXTRA_SB_BT_VISIBILITY,
                        prefs.getString(PREF_KEY_STATUSBAR_BT_VISIBILITY, "DEFAULT"));
            } else if (key.equals(PREF_KEY_STATUSBAR_HIDE_VIBRATE_ICON)) {
                intent.setAction(ACTION_PREF_SYSTEM_ICON_CHANGED);
                intent.putExtra(EXTRA_SB_HIDE_VIBRATE_ICON, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_FLASHING_LED_DISABLE)) {
                intent.setAction(ACTION_BATTERY_LED_CHANGED);
                intent.putExtra(EXTRA_BLED_FLASHING_DISABLED,
                        prefs.getBoolean(PREF_KEY_FLASHING_LED_DISABLE, false));
            } else if (key.equals(PREF_KEY_CHARGING_LED)) {
                intent.setAction(ACTION_BATTERY_LED_CHANGED);
                intent.putExtra(EXTRA_BLED_CHARGING,
                        prefs.getString(PREF_KEY_CHARGING_LED, "DEFAULT"));
            } else if (key.equals(PREF_KEY_HEADSET_ACTION_PLUG) ||
                    key.equals(PREF_KEY_HEADSET_ACTION_UNPLUG)) {
                intent.setAction(ACTION_PREF_HEADSET_ACTION_CHANGED);
                intent.putExtra(EXTRA_HSA_STATE,
                        key.equals(PREF_KEY_HEADSET_ACTION_PLUG) ? 1 : 0);
                intent.putExtra(EXTRA_HSA_URI, prefs.getString(key, null));
            } else if (key.equals(PREF_KEY_POWER_PROXIMITY_WAKE)) {
                intent.setAction(ACTION_PREF_POWER_CHANGED);
                intent.putExtra(EXTRA_POWER_PROXIMITY_WAKE, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_POWER_PROXIMITY_WAKE_IGNORE_CALL)) {
                intent.setAction(ACTION_PREF_POWER_CHANGED);
                intent.putExtra(EXTRA_POWER_PROXIMITY_WAKE_IGNORE_CALL, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS)) {
                intent.setAction(ACTION_PREF_STATUSBAR_DOWNLOAD_PROGRESS_CHANGED);
                intent.putExtra(EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_ENABLED, prefs.getString(key, "OFF"));
            } else if (key.equals(PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS_ANIMATED)) {
                intent.setAction(ACTION_PREF_STATUSBAR_DOWNLOAD_PROGRESS_CHANGED);
                intent.putExtra(EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_ANIMATED, prefs.getBoolean(key, true));
            } else if (key.equals(PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS_CENTERED)) {
                intent.setAction(ACTION_PREF_STATUSBAR_DOWNLOAD_PROGRESS_CHANGED);
                intent.putExtra(EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_CENTERED, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS_THICKNESS)) {
                intent.setAction(ACTION_PREF_STATUSBAR_DOWNLOAD_PROGRESS_CHANGED);
                intent.putExtra(EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_THICKNESS, prefs.getInt(key, 1));
            } else if (key.equals(PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS_MARGIN)) {
                intent.setAction(ACTION_PREF_STATUSBAR_DOWNLOAD_PROGRESS_CHANGED);
                intent.putExtra(EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_MARGIN, prefs.getInt(key, 0));
            } else if (key.equals(PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS_SOUND_ENABLE)) {
                    intent.setAction(ACTION_PREF_STATUSBAR_DOWNLOAD_PROGRESS_CHANGED);
                    intent.putExtra(EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_SOUND_ENABLE, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS_SOUND)) {
                intent.setAction(ACTION_PREF_STATUSBAR_DOWNLOAD_PROGRESS_CHANGED);
                intent.putExtra(EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_SOUND,
                        prefs.getString(key, "content://settings/system/notification_sound"));
            } else if (key.equals(PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS_SOUND_SCREEN_OFF)) {
                intent.setAction(ACTION_PREF_STATUSBAR_DOWNLOAD_PROGRESS_CHANGED);
                intent.putExtra(EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_SOUND_SCREEN_OFF,
                        prefs.getBoolean(key, false));
            } else if (lockscreenKeys.contains(key)) {
                intent.setAction(ACTION_LOCKSCREEN_SETTINGS_CHANGED);
                delayedBroadcast = true;
            } else if (headsUpKeys.contains(key)) {
                intent.setAction(ACTION_HEADS_UP_SETTINGS_CHANGED);
            } else if (key.equals(PREF_KEY_QUICKRECORD_QUALITY)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QR_QUALITY, Integer.valueOf(prefs.getString(key, "22050")));
            } else if (key.equals(PREF_KEY_QUICKRECORD_AUTOSTOP)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QR_AUTOSTOP, prefs.getInt(key, 1));
            } else if (key.equals(PREF_KEY_HIDE_LAUNCHER_ICON)) {
                int mode = prefs.getBoolean(key, false) ?
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED :
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                getActivity().getPackageManager().setComponentEnabledSetting(
                        new ComponentName(getActivity(), "com.ceco.nougat.gravitybox.GravityBoxSettingsAlias"),
                        mode, PackageManager.DONT_KILL_APP);
            } else if (key.equals(PREF_KEY_BATTERY_BAR_SHOW)) {
                intent.setAction(ACTION_PREF_BATTERY_BAR_CHANGED);
                intent.putExtra(EXTRA_BBAR_SHOW, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_BATTERY_BAR_POSITION)) {
                intent.setAction(ACTION_PREF_BATTERY_BAR_CHANGED);
                intent.putExtra(EXTRA_BBAR_POSITION, prefs.getString(key, "TOP"));
            } else if (key.equals(PREF_KEY_BATTERY_BAR_MARGIN)) {
                intent.setAction(ACTION_PREF_BATTERY_BAR_CHANGED);
                intent.putExtra(EXTRA_BBAR_MARGIN, prefs.getInt(key, 0));
            } else if (key.equals(PREF_KEY_BATTERY_BAR_THICKNESS)) {
                intent.setAction(ACTION_PREF_BATTERY_BAR_CHANGED);
                intent.putExtra(EXTRA_BBAR_THICKNESS, prefs.getInt(key, 2));
            } else if (key.equals(PREF_KEY_BATTERY_BAR_DYNACOLOR)) {
                intent.setAction(ACTION_PREF_BATTERY_BAR_CHANGED);
                intent.putExtra(EXTRA_BBAR_DYNACOLOR, prefs.getBoolean(key, true));
            } else if (key.equals(PREF_KEY_BATTERY_BAR_COLOR)) {
                intent.setAction(ACTION_PREF_BATTERY_BAR_CHANGED);
                intent.putExtra(EXTRA_BBAR_COLOR, prefs.getInt(key, 
                        getResources().getInteger(R.integer.COLOR_WHITE)));
            } else if (key.equals(PREF_KEY_BATTERY_BAR_COLOR_LOW)) {
                intent.setAction(ACTION_PREF_BATTERY_BAR_CHANGED);
                intent.putExtra(EXTRA_BBAR_COLOR_LOW, prefs.getInt(key, 
                        getResources().getInteger(R.integer.COLOR_ORANGE)));
            } else if (key.equals(PREF_KEY_BATTERY_BAR_COLOR_CRITICAL)) {
                intent.setAction(ACTION_PREF_BATTERY_BAR_CHANGED);
                intent.putExtra(EXTRA_BBAR_COLOR_CRITICAL, prefs.getInt(key, 
                        getResources().getInteger(R.integer.COLOR_RED)));
            } else if (key.equals(PREF_KEY_BATTERY_BAR_CHARGE_ANIM)) {
                intent.setAction(ACTION_PREF_BATTERY_BAR_CHANGED);
                intent.putExtra(EXTRA_BBAR_CHARGE_ANIM, prefs.getBoolean(key, true));
            } else if (key.equals(PREF_KEY_BATTERY_BAR_CENTERED)) {
                intent.setAction(ACTION_PREF_BATTERY_BAR_CHANGED);
                intent.putExtra(EXTRA_BBAR_CENTERED, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_BATTERY_BAR_COLOR_CHARGING)) {
                intent.setAction(ACTION_PREF_BATTERY_BAR_CHANGED);
                intent.putExtra(EXTRA_BBAR_COLOR_CHARGING, prefs.getInt(key, 
                        getResources().getInteger(R.integer.COLOR_GREEN)));
            } else if (key.equals(PREF_KEY_RAMBAR)) {
                intent.setAction(ACTION_PREF_RECENTS_CHANGED);
                intent.putExtra(EXTRA_RECENTS_RAMBAR, 
                        Integer.valueOf(prefs.getString(key, "0")));
            } else if (key.equals(PREF_KEY_RECENTS_CLEAR_MARGIN_TOP)) {
                intent.setAction(ACTION_PREF_RECENTS_CHANGED);
                intent.putExtra(EXTRA_RECENTS_MARGIN_TOP, prefs.getInt(key, 77));
            } else if (key.equals(PREF_KEY_RECENTS_CLEAR_MARGIN_BOTTOM)) {
                intent.setAction(ACTION_PREF_RECENTS_CHANGED);
                intent.putExtra(EXTRA_RECENTS_MARGIN_BOTTOM, prefs.getInt(key, 50));
            } else if (key.equals(PREF_KEY_SIGNAL_CLUSTER_NARROW)) {
                intent.setAction(ACTION_PREF_SIGNAL_CLUSTER_CHANGED);
                intent.putExtra(EXTRA_SC_NARROW, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_CELL_TILE_DATA_TOGGLE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_CELL_TILE_DATA_TOGGLE, prefs.getString(key, "DISABLED"));
            } else if (PREF_KEY_LOCKSCREEN_SHORTCUT.contains(key)) {
                intent.setAction(ACTION_PREF_LOCKSCREEN_SHORTCUT_CHANGED);
                intent.putExtra(EXTRA_LS_SHORTCUT_SLOT,
                        PREF_KEY_LOCKSCREEN_SHORTCUT.indexOf(key));
                intent.putExtra(EXTRA_LS_SHORTCUT_VALUE, prefs.getString(key, null));
            } else if (key.equals(PREF_KEY_LOCKSCREEN_SHORTCUT_SAFE_LAUNCH)) {
                intent.setAction(ACTION_PREF_LOCKSCREEN_SHORTCUT_CHANGED);
                intent.putExtra(EXTRA_LS_SAFE_LAUNCH, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_LOCKSCREEN_SHORTCUT_SHOW_BADGES)) {
                intent.setAction(ACTION_PREF_LOCKSCREEN_SHORTCUT_CHANGED);
                intent.putExtra(EXTRA_LS_SHOW_BADGES, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_BATTERY_TILE_TEMP)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_BATTERY_TILE_TEMP, prefs.getBoolean(key, true));
            } else if (key.equals(PREF_KEY_BATTERY_TILE_TEMP_UNIT)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_BATTERY_TILE_TEMP_UNIT, prefs.getString(key, "C"));
            } else if (key.equals(PREF_KEY_BATTERY_TILE_VOLTAGE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_BATTERY_TILE_VOLTAGE, prefs.getBoolean(key, true));
            } else if (key.equals(PREF_KEY_BATTERY_SAVER_INDICATION_DISABLE)) {
                intent.setAction(ACTION_BATTERY_SAVER_CHANGED);
                intent.putExtra(EXTRA_BS_INDICATION_DISABLE, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_PIE_TRIGIND)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_TRIGIND, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_PIE_TRIGIND_COLOR)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_TRIGIND_COLOR, prefs.getInt(key, 
                        getActivity().getColor(R.color.pie_trigind_color)));
            } else if (key.equals(PREF_KEY_POWER_CAMERA_VP)) {
                intent.setAction(ACTION_PREF_POWER_CHANGED);
                intent.putExtra(EXTRA_POWER_CAMERA_VP, prefs.getString(key, null));
            } else if (key.equals(PREF_KEY_DND_TILE_QUICK_MODE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_DND_TILE_QUICK_MODE, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_DND_TILE_ENABLED_MODES)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                Set<String> modes = prefs.getStringSet(key,
                        new HashSet<String>(Arrays.asList(new String[] { "1", "2", "3" })));
                List<String> lmodes = new ArrayList<String>(modes);
                Collections.sort(lmodes);
                int[] imodes = new int[lmodes.size()];
                for (int i = 0; i < lmodes.size(); i++) {
                    imodes[i] = Integer.valueOf(lmodes.get(i));
                }
                intent.putExtra(EXTRA_DND_TILE_ENABLED_MODES, imodes);
            } else if (key.equals(PREF_KEY_DND_TILE_DURATION_MODE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_DND_TILE_DURATION_MODE, prefs.getString(key, "MANUAL"));
            } else if (key.equals(PREF_KEY_DND_TILE_DURATION)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_DND_TILE_DURATION, prefs.getInt(key, 60));
            } else if (key.equals(PREF_KEY_LOCATION_TILE_QUICK_MODE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_LOCATION_TILE_QUICK_MODE, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_NM_TILE_ENABLED_MODES)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                Set<String> modes = prefs.getStringSet(key,
                        new HashSet<String>(Arrays.asList(new String[] { "0", "1", "2", "10" })));
                List<String> lmodes = new ArrayList<String>(modes);
                Collections.sort(lmodes);
                int[] imodes = new int[lmodes.size()];
                for (int i = 0; i < lmodes.size(); i++) {
                    imodes[i] = Integer.valueOf(lmodes.get(i));
                }
                intent.putExtra(EXTRA_NM_TILE_ENABLED_MODES, imodes);
            } else if (key.equals(PREF_KEY_NM_TILE_QUICK_MODE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_NM_TILE_QUICK_MODE, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_RINGER_MODE_TILE_QUICK_MODE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_RMT_QUICK_MODE, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_STAY_AWAKE_TILE_QUICK_MODE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_SA_QUICK_MODE, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_STAY_AWAKE_TILE_AUTO_RESET)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_SA_AUTO_RESET, prefs.getBoolean(key, false));
            } else if (key.equals(PREF_KEY_QS_SCALE_CORRECTION)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_SCALE_CORRECTION, prefs.getInt(key, 0));
            } else if (key.equals(PREF_KEY_FINGERPRINT_LAUNCHER_APP)) {
                intent.setAction(ACTION_FPL_SETTINGS_CHANGED);
                intent.putExtra(EXTRA_FPL_APP, prefs.getString(key, null));
            } else if (key.equals(PREF_KEY_FINGERPRINT_LAUNCHER_SHOW_TOAST)) {
                intent.setAction(ACTION_FPL_SETTINGS_CHANGED);
                intent.putExtra(EXTRA_FPL_SHOW_TOAST, prefs.getBoolean(key, true));
            }

            if (intent.getAction() != null) {
                if (delayedBroadcast) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            getActivity().sendBroadcast(intent);
                        }
                    }, 200);
                } else {
                    getActivity().sendBroadcast(intent);
                }
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
            if (pref == mPrefVkVibratePattern || pref == mPrefPowerCameraVp) {
                if (newValue == null || ((String)newValue).isEmpty()) return true;
                try {
                    Utils.csvToLongArray((String)newValue);
                } catch (Exception e) {
                    Toast.makeText(getActivity(), getString(R.string.lc_vibrate_pattern_invalid),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else if (pref.getKey().startsWith(PREF_KEY_FINGERPRINT_LAUNCHER_FINGER)) {
                AppPickerPreference appPref = (AppPickerPreference) pref;
                Set<String> newSet = new HashSet<>();
                String fingerId = appPref.getExtraData().getString("fingerId");
                newSet.add("fingerId:" + fingerId);
                if (newValue != null) {
                    newSet.add("app:" + newValue);
                }
                mPrefs.edit().putStringSet(pref.getKey(), newSet).commit();
                Intent intent = new Intent(ACTION_FPL_SETTINGS_CHANGED);
                intent.putExtra(EXTRA_FPL_FINGER_ID, fingerId);
                intent.putExtra(EXTRA_FPL_APP, String.valueOf(newValue));
                getActivity().sendBroadcast(intent);
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
            } else if (pref == mPrefAboutUnlocker) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_gravitybox_unlocker)));
            } else if (pref == mPrefEngMode) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName(APP_ENGINEERING_MODE, APP_ENGINEERING_MODE_CLASS);
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
                File file = new File(Utils.getFilesDir(getActivity()) + "/" + FILE_THEME_DARK_FLAG);
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
            } else if (pref == mPrefPieColorReset) {
                final int bgColor = getActivity().getColor(R.color.pie_background_color);
                final int fgColor = getActivity().getColor(R.color.pie_foreground_color);
                final int outlineColor = getActivity().getColor(R.color.pie_outline_color);
                final int selectedColor = getActivity().getColor(R.color.pie_selected_color);
                final int textColor = getActivity().getColor(R.color.pie_text_color);
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
                if (sm.isBackupObsolete()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.settings_restore_backup_obsolete)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, null);
                    mDialog = builder.create();
                    mDialog.show();
                } else if (sm.isBackupAvailable()) {
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
                intent = new Intent(getActivity(), LedMainActivity.class);
                intent.putExtra(LedMainActivity.EXTRA_UUID_REGISTERED, sSystemProperties.uuidRegistered);
                intent.putExtra(LedMainActivity.EXTRA_TRIAL_COUNTDOWN, sSystemProperties.uncTrialCountdown);
            } else if (PREF_KEY_NAVBAR_CUSTOM_KEY_IMAGE.equals(pref.getKey())) {
                setNavbarCustomKeyImage();
            } else if (PREF_KEY_FORCE_AOSP.equals(pref.getKey())) {
                File file = new File(Utils.AOSP_FORCED_FILE_PATH);
                if (((CheckBoxPreference)pref).isChecked()) {
                    if (!file.exists()) {
                        try {
                            file.createNewFile();
                            file.setReadable(true, false);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (file.exists()) {
                    file.delete();
                }
            } else if (PREF_KEY_FINGERPRINT_LAUNCHER_PAUSE.equals(pref.getKey())) {
                Intent fplPauseIntent = new Intent(ACTION_FPL_SETTINGS_CHANGED);
                fplPauseIntent.putExtra(EXTRA_FPL_PAUSE, true);
                getActivity().sendBroadcast(fplPauseIntent);
            }

//            else if (PREF_KEY_HEADS_UP_SNOOZE_RESET.equals(pref.getKey())) {
//                intent = new Intent(ACTION_HEADS_UP_SNOOZE_RESET);
//                getActivity().sendBroadcast(intent);
//                return true;
//            }

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

        private void setCustomLockscreenImage() {
            Intent intent = new Intent(getActivity(), PickImageActivity.class);
            intent.putExtra(PickImageActivity.EXTRA_CROP, true);
            intent.putExtra(PickImageActivity.EXTRA_SCALE, true);
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
                intent.putExtra(PickImageActivity.EXTRA_ASPECT_X, wpWidth);
                intent.putExtra(PickImageActivity.EXTRA_ASPECT_Y, wpHeight);
                intent.putExtra(PickImageActivity.EXTRA_OUTPUT_X, wpWidth);
                intent.putExtra(PickImageActivity.EXTRA_OUTPUT_Y, wpHeight);
                intent.putExtra(PickImageActivity.EXTRA_SPOTLIGHT_X, spotlightX);
                intent.putExtra(PickImageActivity.EXTRA_SPOTLIGHT_Y, spotlightY);
            } else {
                boolean isPortrait = getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_PORTRAIT;
                intent.putExtra(PickImageActivity.EXTRA_ASPECT_X, isPortrait ? displaySize.x : displaySize.y);
                intent.putExtra(PickImageActivity.EXTRA_ASPECT_Y, isPortrait ? displaySize.y : displaySize.x);
            }
            getActivity().startActivityFromFragment(this, intent, REQ_LOCKSCREEN_BACKGROUND);
        }

        private void setCustomNotifBgPortrait() {
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point displaySize = new Point();
            display.getRealSize(displaySize);
            Intent intent = new Intent(getActivity(), PickImageActivity.class);
            intent.putExtra(PickImageActivity.EXTRA_CROP, true);
            boolean isPortrait = getResources()
                    .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            intent.putExtra(PickImageActivity.EXTRA_ASPECT_X, isPortrait ? displaySize.x : displaySize.y);
            intent.putExtra(PickImageActivity.EXTRA_ASPECT_Y, isPortrait ? displaySize.y : displaySize.x);
            intent.putExtra(PickImageActivity.EXTRA_OUTPUT_X, isPortrait ? displaySize.x : displaySize.y);
            intent.putExtra(PickImageActivity.EXTRA_OUTPUT_Y, isPortrait ? displaySize.y : displaySize.x);
            intent.putExtra(PickImageActivity.EXTRA_SCALE, true);
            intent.putExtra(PickImageActivity.EXTRA_SCALE_UP, true);
            startActivityForResult(intent, REQ_NOTIF_BG_IMAGE_PORTRAIT);
        }

        private void setCustomNotifBgLandscape() {
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point displaySize = new Point();
            display.getRealSize(displaySize);
            Intent intent = new Intent(getActivity(), PickImageActivity.class);
            intent.putExtra(PickImageActivity.EXTRA_CROP, true);
            boolean isPortrait = getResources()
                  .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            intent.putExtra(PickImageActivity.EXTRA_ASPECT_X, isPortrait ? displaySize.y : displaySize.x);
            intent.putExtra(PickImageActivity.EXTRA_ASPECT_Y, isPortrait ? displaySize.x : displaySize.y);
            intent.putExtra(PickImageActivity.EXTRA_OUTPUT_X, isPortrait ? displaySize.y : displaySize.x);
            intent.putExtra(PickImageActivity.EXTRA_OUTPUT_Y, isPortrait ? displaySize.x : displaySize.y);
            intent.putExtra(PickImageActivity.EXTRA_SCALE, true);
            intent.putExtra(PickImageActivity.EXTRA_SCALE_UP, true);
            startActivityForResult(intent, REQ_NOTIF_BG_IMAGE_LANDSCAPE);
        }

        private void setCustomCallerImage() {
            int width = getResources().getDimensionPixelSize(R.dimen.caller_id_photo_width);
            int height = getResources().getDimensionPixelSize(R.dimen.caller_id_photo_height);
            Intent intent = new Intent(getActivity(), PickImageActivity.class);
            intent.putExtra(PickImageActivity.EXTRA_CROP, true);
            boolean isPortrait = getResources()
                    .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            intent.putExtra(PickImageActivity.EXTRA_ASPECT_X, isPortrait ? width : height);
            intent.putExtra(PickImageActivity.EXTRA_ASPECT_Y, isPortrait ? height : width);
            intent.putExtra(PickImageActivity.EXTRA_OUTPUT_X, isPortrait ? width : height);
            intent.putExtra(PickImageActivity.EXTRA_OUTPUT_Y, isPortrait ? height : width);
            intent.putExtra(PickImageActivity.EXTRA_SCALE, true);
            intent.putExtra(PickImageActivity.EXTRA_SCALE_UP, true);
            startActivityForResult(intent, REQ_CALLER_PHOTO);
        }

        private void setNavbarCustomKeyImage() {
            int width = getResources().getDimensionPixelSize(R.dimen.navbar_custom_key_image_width);
            int height = getResources().getDimensionPixelSize(R.dimen.navbar_custom_key_image_height);
            pickIcon(width, height, new IconPickHandler() {
                @Override
                public void onIconPicked(Bitmap icon) {
                    try {
                        File target = new File(Utils.getFilesDir(getActivity()) + "/navbar_custom_key_image");
                        FileOutputStream fos = new FileOutputStream(target);
                        if (icon.compress(CompressFormat.PNG, 100, fos)) {
                            target.setReadable(true, false);
                        }
                        fos.close();
                        Intent intent = new Intent(ACTION_PREF_NAVBAR_CHANGED);
                        intent.putExtra(EXTRA_NAVBAR_CUSTOM_KEY_ICON_STYLE, "CUSTOM");
                        getActivity().sendBroadcast(intent);
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }

                @Override
                public void onIconPickCancelled() {
                    Toast.makeText(getActivity(), R.string.app_picker_icon_pick_cancelled,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        public interface ShortcutHandler {
            Intent getCreateShortcutIntent();
            void onHandleShortcut(Intent intent, String name,
                    String localIconResName, Bitmap icon);
            void onShortcutCancelled();
        }

        private ShortcutHandler mShortcutHandler;
        public void obtainShortcut(ShortcutHandler handler) {
            if (handler == null) return;

            mShortcutHandler = handler;
            startActivityForResult(mShortcutHandler.getCreateShortcutIntent(), REQ_OBTAIN_SHORTCUT);
        }

        public interface IconPickHandler {
            void onIconPicked(Bitmap icon);
            void onIconPickCancelled();
        }

        private IconPickHandler mIconPickHandler;
        public void pickIcon(int sizePx, IconPickHandler handler) {
            pickIcon(sizePx, sizePx, handler);
        }
        public void pickIcon(int width, int height, IconPickHandler handler) {
            if (handler == null) return;

            mIconPickHandler = handler;

            Intent intent = new Intent(getActivity(), PickImageActivity.class);
            intent.putExtra(PickImageActivity.EXTRA_CROP, true);
            intent.putExtra(PickImageActivity.EXTRA_ASPECT_X, width);
            intent.putExtra(PickImageActivity.EXTRA_ASPECT_Y, height);
            intent.putExtra(PickImageActivity.EXTRA_OUTPUT_X, width);
            intent.putExtra(PickImageActivity.EXTRA_OUTPUT_Y, height);
            intent.putExtra(PickImageActivity.EXTRA_SCALE, true);
            intent.putExtra(PickImageActivity.EXTRA_SCALE_UP, true);
            startActivityForResult(intent, REQ_ICON_PICK);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQ_LOCKSCREEN_BACKGROUND) {
                if (resultCode == Activity.RESULT_OK) {
                    File f = new File(data.getStringExtra(PickImageActivity.EXTRA_FILE_PATH));
                    if (f.exists()) {
                        f.renameTo(wallpaperImage);
                    }
                    wallpaperImage.setReadable(true, false);
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_successful), 
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_not_successful),
                            Toast.LENGTH_SHORT).show();
                }
                Intent intent = new Intent(ACTION_PREF_LOCKSCREEN_BG_CHANGED);
                getActivity().sendBroadcast(intent);
            } else if (requestCode == REQ_NOTIF_BG_IMAGE_PORTRAIT) {
                if (resultCode == Activity.RESULT_OK) {
                    File f = new File(data.getStringExtra(PickImageActivity.EXTRA_FILE_PATH));
                    if (f.exists()) {
                        f.renameTo(notifBgImagePortrait);
                    }
                    notifBgImagePortrait.setReadable(true, false);
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_successful), 
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_not_successful),
                            Toast.LENGTH_SHORT).show();
                }
                Intent intent = new Intent(ACTION_NOTIF_BACKGROUND_CHANGED);
                getActivity().sendBroadcast(intent);
            } else if (requestCode == REQ_NOTIF_BG_IMAGE_LANDSCAPE) {
                if (resultCode == Activity.RESULT_OK) {
                    File f = new File(data.getStringExtra(PickImageActivity.EXTRA_FILE_PATH));
                    if (f.exists()) {
                        f.renameTo(notifBgImageLandscape);
                    }
                    notifBgImageLandscape.setReadable(true, false);
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_successful), 
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_not_successful),
                            Toast.LENGTH_SHORT).show();
                }
                Intent intent = new Intent(ACTION_NOTIF_BACKGROUND_CHANGED);
                getActivity().sendBroadcast(intent);
            } else if (requestCode == REQ_CALLER_PHOTO) {
                if (resultCode == Activity.RESULT_OK) {
                    File f = new File(data.getStringExtra(PickImageActivity.EXTRA_FILE_PATH));
                    if (f.exists()) {
                        f.renameTo(callerPhotoFile);
                    }
                    callerPhotoFile.setReadable(true, false);
                    Toast.makeText(getActivity(), getString(
                            R.string.caller_unknown_photo_result_successful), 
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), getString(
                            R.string.caller_unkown_photo_result_not_successful),
                            Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQ_OBTAIN_SHORTCUT && mShortcutHandler != null) {
                if (resultCode == Activity.RESULT_OK) {
                    String localIconResName = null;
                    Bitmap b = null;
                    Intent.ShortcutIconResource siRes = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                    Intent shortcutIntent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    if (siRes != null) {
                        if (shortcutIntent != null &&
                                ShortcutActivity.ACTION_LAUNCH_ACTION.equals(
                                        shortcutIntent.getAction())) {
                            localIconResName = siRes.resourceName;
                        } else {
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
                    }
                    if (localIconResName == null && b == null) {
                        b = (Bitmap)data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
                    }

                    mShortcutHandler.onHandleShortcut(shortcutIntent,
                            data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME),
                            localIconResName, b);
                } else {
                    mShortcutHandler.onShortcutCancelled();
                }
            } else if (requestCode == REQ_ICON_PICK && mIconPickHandler != null) {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        File f = new File(data.getStringExtra(PickImageActivity.EXTRA_FILE_PATH));
                        Bitmap icon = BitmapFactory.decodeStream(new FileInputStream(f));
                        mIconPickHandler.onIconPicked(icon);
                        f.delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    mIconPickHandler.onIconPickCancelled();
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
                                        intent.putExtra(SystemPropertyProvider.EXTRA_UUID_TYPE, "PayPal");
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
