/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2014 Peter Gregus for GravityBox Project (C3C076@xda)
 *
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
package com.ceco.kitkat.gravitybox.preference;

import com.ceco.kitkat.gravitybox.R;
import com.ceco.kitkat.gravitybox.Utils;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

public class IncreasingRingPreference extends VolumePreference implements
        CheckBox.OnCheckedChangeListener {
    private static final String TAG = "IncreasingRingPreference";
    private static final String SEPARATOR = "#C3C0#";

    public static final String ACTION_INCREASING_RING_CHANGED =
            "gravitybox.intent.action.INCREASING_RING_CHANGED";
    public static final String EXTRA_STREAM_TYPE = "streamType";
    public static final String EXTRA_ENABLED = "enabled";
    public static final String EXTRA_MIN_VOLUME = "minVolume";
    public static final String EXTRA_INTERVAL = "interval";

    private CheckBox mEnabledCheckbox;
    private TextView mMinVolumeTitle;
    private SeekBar mMinVolumeSeekBar;
    private TextView mRingVolumeNotice;
    private TextView mIntervalTitle;
    private Spinner mInterval;
    private int[] mIntervalValues;
    private ConfigStore mConfigStore;

    public static final class ConfigStore {
        public boolean enabled = false;
        public int minVolume = 1;
        public int interval = 0;

        public ConfigStore(String value) {
            if (value == null) return;

            try {
                String[] data = value.split(SEPARATOR);
                enabled = Boolean.valueOf(data[0]);
                minVolume = Integer.valueOf(data[1]);
                interval = Integer.valueOf(data[2]);
            } catch (Throwable t) {
                enabled = false;
                t.printStackTrace();
            }
        }

        public String getValue() {
            String[] data = new String[] {
                    String.valueOf(enabled),
                    String.valueOf(minVolume),
                    String.valueOf(interval)
            };
            return Utils.join(data, SEPARATOR);
        }

        public String toString() {
            return "[enabled=" + enabled + "; minVolume=" + minVolume + "; interval=" + interval + "]";
        }
    }

    public IncreasingRingPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.preference_dialog_increasing_ring);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mEnabledCheckbox = (CheckBox) view.findViewById(R.id.increasing_ring);
        mEnabledCheckbox.setOnCheckedChangeListener(this);

        mMinVolumeTitle = (TextView) view.findViewById(R.id.increasing_ring_min_volume_title);

        mMinVolumeSeekBar = (SeekBar) view.findViewById(R.id.seekbar);
        AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mMinVolumeSeekBar.setSecondaryProgress(am.getStreamVolume(AudioManager.STREAM_RING));

        mRingVolumeNotice = (TextView) view.findViewById(R.id.increasing_ring_volume_notice);

        mIntervalTitle = (TextView) view.findViewById(R.id.increasing_ring_interval_title);
        mInterval = (Spinner) view.findViewById(R.id.increasing_ring_interval);
        mIntervalValues = getContext().getResources().getIntArray(R.array.increasing_ring_interval_values);

        getConfig();
        updateVolumeNoticeVisibility(mMinVolumeSeekBar.getProgress());
        updateEnabledStates();
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) { } 

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(false);

        if (positiveResult) {
            saveConfig();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        updateVolumeNoticeVisibility(mMinVolumeSeekBar.getProgress());
        updateEnabledStates();
    }

    @Override
    public boolean onVolumeChange(SeekBarVolumizer volumizer, int value) {
        boolean result = super.onVolumeChange(volumizer, value);
        if (result) {
            updateVolumeNoticeVisibility(value);
        }
        return result;
    }

    private void getConfig() {
        mConfigStore = new ConfigStore(getPersistedString(null));
        mEnabledCheckbox.setChecked(mConfigStore.enabled);
        mMinVolumeSeekBar.setProgress(mConfigStore.minVolume);

        int index = 0;
        for (int i = 0; i < mIntervalValues.length; i++) {
            if (mIntervalValues[i] == mConfigStore.interval) {
                index = i;
                break;
            }
        }
        mInterval.setSelection(index);
    }

    private void saveConfig() {
        if (mConfigStore != null) {
            mConfigStore.enabled = mEnabledCheckbox.isChecked();
            mConfigStore.minVolume = mMinVolumeSeekBar.getProgress();
            mConfigStore.interval = mIntervalValues[mInterval.getSelectedItemPosition()];
            persistString(mConfigStore.getValue());

            Intent intent = new Intent(ACTION_INCREASING_RING_CHANGED);
            intent.putExtra(EXTRA_STREAM_TYPE, mStreamType);
            intent.putExtra(EXTRA_ENABLED, mConfigStore.enabled);
            intent.putExtra(EXTRA_MIN_VOLUME, mConfigStore.minVolume);
            intent.putExtra(EXTRA_INTERVAL, mConfigStore.interval);
            getContext().sendBroadcast(intent);
        }
    }

    private void updateVolumeNoticeVisibility(int value) {
        boolean visible = value > mMinVolumeSeekBar.getSecondaryProgress();
        if (!mEnabledCheckbox.isChecked()) {
            visible = false;
        }
        mRingVolumeNotice.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateEnabledStates() {
        boolean enable = mEnabledCheckbox.isChecked();
        mMinVolumeTitle.setEnabled(enable);
        mMinVolumeSeekBar.setEnabled(enable);
        mRingVolumeNotice.setEnabled(enable);
        mIntervalTitle.setEnabled(enable);
        mInterval.setEnabled(enable);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        if (mEnabledCheckbox != null) {
            myState.mEnabled = mEnabledCheckbox.isChecked();
        }
        if (mInterval != null) {
            myState.mIntervalSelection = mInterval.getSelectedItemPosition();
        }
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        if (mEnabledCheckbox != null) {
            mEnabledCheckbox.setChecked(myState.mEnabled);
        }
        if (mInterval != null) {
            mInterval.setSelection(myState.mIntervalSelection);
        }
    }

    private static class SavedState extends BaseSavedState {
        boolean mEnabled;
        int mIntervalSelection;

        public SavedState(Parcel source) {
            super(source);
            mEnabled = source.readInt() != 0;
            mIntervalSelection = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mEnabled ? 1 : 0);
            dest.writeInt(mIntervalSelection);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
