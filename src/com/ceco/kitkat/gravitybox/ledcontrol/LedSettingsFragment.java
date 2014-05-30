/*
 * Copyright (C) 2014 Peter Gregus for GravityBox Project (C3C076@xda)
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

package com.ceco.kitkat.gravitybox.ledcontrol;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import com.ceco.kitkat.gravitybox.R;
import com.ceco.kitkat.gravitybox.ledcontrol.LedSettings.LedMode;
import com.ceco.kitkat.gravitybox.preference.SeekBarPreference;

import android.app.Activity;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.widget.Toast;

public class LedSettingsFragment extends PreferenceFragment implements OnPreferenceChangeListener {
    private static final String PREF_KEY_LED_COLOR = "pref_lc_led_color";
    private static final String PREF_KEY_LED_TIME_ON = "pref_lc_led_time_on";
    private static final String PREF_KEY_LED_TIME_OFF = "pref_lc_led_time_off";
    private static final String PREF_KEY_ONGOING = "pref_lc_ongoing";
    private static final String PREF_KEY_NOTIF_SOUND_OVERRIDE = "pref_lc_notif_sound_override";
    private static final String PREF_KEY_NOTIF_SOUND = "pref_lc_notif_sound";
    private static final String PREF_KEY_NOTIF_SOUND_ONLY_ONCE = "pref_lc_notif_sound_only_once";
    private static final String PREF_KEY_NOTIF_SOUND_ONLY_ONCE_TIMEOUT = "pref_lc_notif_sound_only_once_timeout";
    private static final String PREF_KEY_NOTIF_INSISTENT = "pref_lc_notif_insistent";
    private static final String PREF_KEY_VIBRATE_OVERRIDE = "pref_lc_vibrate_override";
    private static final String PREF_KEY_VIBRATE_PATTERN = "pref_lc_vibrate_pattern";
    private static final String PREF_KEY_DEFAULT_SETTINGS = "pref_lc_default_settings";
    private static final String PREF_CAT_KEY_ACTIVE_SCREEN = "pref_cat_lc_active_screen";
    private static final String PREF_KEY_ACTIVE_SCREEN_ENABLED = "pref_lc_active_screen_enable";
    private static final String PREF_KEY_ACTIVE_SCREEN_EXPANDED = "pref_lc_active_screen_expand";
    private static final String PREF_KEY_LED_MODE = "pref_lc_led_mode";
    private static final String PREF_CAT_KEY_QH = "pref_cat_lc_quiet_hours";
    private static final String PREF_KEY_QH_IGNORE = "pref_lc_qh_ignore";
    private static final String PREF_KEY_QH_IGNORE_LIST = "pref_lc_qh_ignore_list";

    private static final int REQ_PICK_SOUND = 101;

    private ColorPickerPreference mColorPref;
    private SeekBarPreference mLedOnMsPref;
    private SeekBarPreference mLedOffMsPref;
    private CheckBoxPreference mOngoingPref;
    private Preference mNotifSoundPref;
    private CheckBoxPreference mNotifSoundOverridePref;
    private Uri mSoundUri;
    private CheckBoxPreference mNotifSoundOnlyOncePref;
    private SeekBarPreference mNotifSoundOnlyOnceTimeoutPref;
    private CheckBoxPreference mNotifInsistentPref;
    private CheckBoxPreference mVibratePatternOverridePref;
    private EditTextPreference mVibratePatternPref;
    private SwitchPreference mDefaultSettingsPref;
    private PreferenceCategory mActiveScreenCat;
    private CheckBoxPreference mActiveScreenEnabledPref;
    private CheckBoxPreference mActiveScreenExpandedPref;
    private ListPreference mLedModePref;
    private PreferenceCategory mQhCat;
    private CheckBoxPreference mQhIgnorePref;
    private EditTextPreference mQhIgnoreListPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.led_control_settings);

        mColorPref = (ColorPickerPreference) findPreference(PREF_KEY_LED_COLOR);
        mLedOnMsPref = (SeekBarPreference) findPreference(PREF_KEY_LED_TIME_ON);
        mLedOffMsPref = (SeekBarPreference) findPreference(PREF_KEY_LED_TIME_OFF);
        mOngoingPref = (CheckBoxPreference) findPreference(PREF_KEY_ONGOING);
        mNotifSoundOverridePref = (CheckBoxPreference) findPreference(PREF_KEY_NOTIF_SOUND_OVERRIDE);
        mNotifSoundPref = findPreference(PREF_KEY_NOTIF_SOUND);
        mNotifSoundOnlyOncePref = (CheckBoxPreference) findPreference(PREF_KEY_NOTIF_SOUND_ONLY_ONCE);
        mNotifSoundOnlyOnceTimeoutPref = (SeekBarPreference) findPreference(PREF_KEY_NOTIF_SOUND_ONLY_ONCE_TIMEOUT);
        mNotifInsistentPref = (CheckBoxPreference) findPreference(PREF_KEY_NOTIF_INSISTENT);
        mVibratePatternOverridePref = (CheckBoxPreference) findPreference(PREF_KEY_VIBRATE_OVERRIDE);
        mVibratePatternPref = (EditTextPreference) findPreference(PREF_KEY_VIBRATE_PATTERN);
        mVibratePatternPref.setOnPreferenceChangeListener(this);
        mDefaultSettingsPref = (SwitchPreference) findPreference(PREF_KEY_DEFAULT_SETTINGS);
        mActiveScreenCat = (PreferenceCategory) findPreference(PREF_CAT_KEY_ACTIVE_SCREEN);
        mActiveScreenEnabledPref = (CheckBoxPreference) findPreference(PREF_KEY_ACTIVE_SCREEN_ENABLED);
        mActiveScreenExpandedPref = (CheckBoxPreference) findPreference(PREF_KEY_ACTIVE_SCREEN_EXPANDED);
        mLedModePref = (ListPreference) findPreference(PREF_KEY_LED_MODE);
        mLedModePref.setOnPreferenceChangeListener(this);
        mQhCat = (PreferenceCategory) findPreference(PREF_CAT_KEY_QH);
        mQhIgnorePref = (CheckBoxPreference) findPreference(PREF_KEY_QH_IGNORE);
        mQhIgnoreListPref = (EditTextPreference) findPreference(PREF_KEY_QH_IGNORE_LIST);
    }

    protected void initialize(LedSettings ledSettings) {
        mColorPref.setValue(ledSettings.getColor());
        mLedOnMsPref.setValue(ledSettings.getLedOnMs());
        mLedOffMsPref.setValue(ledSettings.getLedOffMs());
        mOngoingPref.setChecked(ledSettings.getOngoing());
        mNotifSoundOverridePref.setChecked(ledSettings.getSoundOverride());
        mSoundUri = ledSettings.getSoundUri();
        mNotifSoundOnlyOncePref.setChecked(ledSettings.getSoundOnlyOnce());
        mNotifSoundOnlyOnceTimeoutPref.setValue((int)(ledSettings.getSoundOnlyOnceTimeout() / 60000));
        mNotifInsistentPref.setChecked(ledSettings.getInsistent());
        mVibratePatternOverridePref.setChecked(ledSettings.getVibrateOverride());
        if (ledSettings.getVibratePatternAsString() != null) {
            mVibratePatternPref.setText(ledSettings.getVibratePatternAsString());
        }
        updateSoundPrefSummary();
        if (ledSettings.getPackageName().equals("default")) {
            mDefaultSettingsPref.setChecked(ledSettings.getEnabled());
        } else {
            mDefaultSettingsPref.setChecked(false);
            getPreferenceScreen().removePreference(mDefaultSettingsPref);
        }
        if (!LedSettings.isActiveScreenMasterEnabled(getActivity())) {
            getPreferenceScreen().removePreference(mActiveScreenCat);
        } else {
            mActiveScreenEnabledPref.setChecked(ledSettings.getActiveScreenEnabled());
            mActiveScreenExpandedPref.setChecked(ledSettings.getActiveScreenExpanded());
        }
        mLedModePref.setValue(ledSettings.getLedMode().toString());
        updateLedModeDependentState();
        if (!LedSettings.isQuietHoursEnabled(getActivity())) {
            getPreferenceScreen().removePreference(mQhCat);
        } else {
            mQhIgnorePref.setChecked(ledSettings.getQhIgnore());
            mQhIgnoreListPref.setText(ledSettings.getQhIgnoreList());
        }
    }

    private void updateSoundPrefSummary() {
        mNotifSoundPref.setSummary(getString(R.string.lc_notif_sound_none));
        if (mSoundUri != null) {
            Ringtone r = RingtoneManager.getRingtone(getActivity(), mSoundUri);
            if (r != null) {
                mNotifSoundPref.setSummary(r.getTitle(getActivity()));
            } else {
                mSoundUri = null;
            }
        }
    }

    private void updateLedModeDependentState() {
        mLedModePref.setSummary(mLedModePref.getEntry());
        LedMode lm = LedMode.valueOf(mLedModePref.getValue());
        mColorPref.setEnabled(lm == LedMode.OVERRIDE);
        mLedOnMsPref.setEnabled(lm == LedMode.OVERRIDE);
        mLedOffMsPref.setEnabled(lm == LedMode.OVERRIDE);
    }

    protected int getColor() {
        return mColorPref.getValue();
    }

    protected int getLedOnMs() {
        return mLedOnMsPref.getValue();
    }

    protected int getLedOffMs() {
        return mLedOffMsPref.getValue();
    }

    protected boolean getOngoing() {
        return mOngoingPref.isChecked();
    }

    protected boolean getSoundOverride() {
        return mNotifSoundOverridePref.isChecked();
    }

    protected Uri getSoundUri() {
        return mSoundUri;
    }

    protected boolean getSoundOnlyOnce() {
        return mNotifSoundOnlyOncePref.isChecked();
    }

    protected long getSoundOnlyOnceTimeout() {
        return (mNotifSoundOnlyOnceTimeoutPref.getValue() * 60000);
    }

    protected boolean getInsistent() {
        return mNotifInsistentPref.isChecked();
    }

    protected boolean getVibrateOverride() {
        return mVibratePatternOverridePref.isChecked();
    }

    protected String getVibratePatternAsString() {
        return mVibratePatternPref.getText();
    }

    protected boolean getDefaultSettingsEnabled() {
        return mDefaultSettingsPref.isChecked();
    }

    protected boolean getActiveScreenEnabled() {
        return mActiveScreenEnabledPref.isChecked();
    }

    protected boolean getActiveScreenExpanded() {
        return mActiveScreenExpandedPref.isChecked();
    }

    protected LedMode getLedMode() {
        return LedMode.valueOf(mLedModePref.getValue());
    }

    protected boolean getQhIgnore() {
        return mQhIgnorePref.isChecked();
    }

    protected String getQhIgnoreList() {
        return mQhIgnoreListPref.getText();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen prefScreen, Preference pref) {
        if (pref == mNotifSoundPref) {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, mSoundUri);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, 
                    Settings.System.DEFAULT_NOTIFICATION_URI);
            startActivityForResult(intent, REQ_PICK_SOUND);
            return true;
        }
        return super.onPreferenceTreeClick(prefScreen, pref);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PICK_SOUND && resultCode == Activity.RESULT_OK && data != null) {
            mSoundUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            updateSoundPrefSummary();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVibratePatternPref) {
            try {
                String val = (String)newValue;
                LedSettings.parseVibratePatternString(val);
                return true;
            } catch (Exception e) {
                Toast.makeText(getActivity(), getString(R.string.lc_vibrate_pattern_invalid),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        } else if (preference == mLedModePref) {
            mLedModePref.setValue((String)newValue);
            updateLedModeDependentState();
        }
        return true;
    }
}
