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

package com.ceco.kitkat.gravitybox.quicksettings;

import com.ceco.kitkat.gravitybox.ModSmartRadio;
import com.ceco.kitkat.gravitybox.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;

public class SmartRadioTile extends BasicTile {

    private boolean mSmartRadioEnabled;
    private ModSmartRadio.State mSmartRadioState;
    private SettingsObserver mSettingsObserver;

    public SmartRadioTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ModSmartRadio.ACTION_TOGGLE_SMART_RADIO);
                mContext.sendBroadcast(i);
            }
        };
    }

    @Override
    protected int onGetLayoutId() {
        return R.layout.quick_settings_tile_smart_radio;
    }

    @Override
    protected void onTilePostCreate() {
        mSettingsObserver = new SettingsObserver(new Handler());
        mSettingsObserver.observe();

        super.onTilePostCreate();
    }

    @Override
    protected synchronized void updateTile() {
        mSmartRadioEnabled = Settings.System.getInt(mContext.getContentResolver(),
                ModSmartRadio.SETTING_SMART_RADIO_ENABLED, 1) == 1;
        String state = Settings.System.getString(mContext.getContentResolver(), 
                ModSmartRadio.SETTING_SMART_RADIO_STATE);
        mSmartRadioState = ModSmartRadio.State.valueOf(state == null ? "UNKNOWN" : state);

        if (mSmartRadioEnabled) {
            mLabel = mGbContext.getString(R.string.quick_settings_smart_radio_on);
            mDrawableId = mSmartRadioState == ModSmartRadio.State.POWER_SAVING ?
                    R.drawable.ic_qs_smart_radio_on : R.drawable.ic_qs_smart_radio_on_normal;
        } else {
            mLabel = mGbContext.getString(R.string.quick_settings_smart_radio_off);
            mDrawableId = R.drawable.ic_qs_smart_radio_off;
        }

        super.updateTile();
    }

    class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(Settings.System.getUriFor(
                   ModSmartRadio.SETTING_SMART_RADIO_ENABLED), false, this);
            cr.registerContentObserver(Settings.System.getUriFor(
                   ModSmartRadio.SETTING_SMART_RADIO_STATE), false, this);
        }

        @Override 
        public void onChange(boolean selfChange) {
            updateResources();
        }
    } 
}
