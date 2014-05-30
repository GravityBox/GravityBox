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

import java.io.File;

import com.ceco.kitkat.gravitybox.GravityBoxSettings;
import com.ceco.kitkat.gravitybox.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

public class ActiveScreenActivity extends Activity {
    private SharedPreferences mPrefs;
    private Switch mMasterSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        File file = new File(getFilesDir() + "/" + GravityBoxSettings.FILE_THEME_DARK_FLAG);
        if (file.exists()) {
            setTheme(android.R.style.Theme_Holo);
        }

        super.onCreate(savedInstanceState);

        mPrefs = getSharedPreferences("ledcontrol", Context.MODE_WORLD_READABLE);

        setContentView(R.layout.active_screen_activity);

        mMasterSwitch = (Switch) findViewById(R.id.as_switch);
        mMasterSwitch.setChecked(mPrefs.getBoolean(LedSettings.PREF_KEY_ACTIVE_SCREEN_ENABLED, false));
        mMasterSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPrefs.edit().putBoolean(LedSettings.PREF_KEY_ACTIVE_SCREEN_ENABLED, isChecked).commit();
                Intent intent = new Intent(LedSettings.ACTION_UNC_SETTINGS_CHANGED);
                intent.putExtra(LedSettings.EXTRA_UNC_AS_ENABLED, isChecked);
                sendBroadcast(intent);
            }
        });
    }
}
