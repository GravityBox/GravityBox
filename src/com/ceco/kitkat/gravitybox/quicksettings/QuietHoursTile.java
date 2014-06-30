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

import com.ceco.kitkat.gravitybox.GravityBox;
import com.ceco.kitkat.gravitybox.R;
import com.ceco.kitkat.gravitybox.StatusbarQuietHoursManager;
import com.ceco.kitkat.gravitybox.StatusbarQuietHoursManager.QuietHoursListener;
import com.ceco.kitkat.gravitybox.ledcontrol.QuietHours;
import com.ceco.kitkat.gravitybox.ledcontrol.QuietHoursActivity;

import android.content.Context;
import android.content.Intent;
import android.view.View;

public class QuietHoursTile extends BasicTile implements QuietHoursListener {

    private QuietHours mQh;
    private StatusbarQuietHoursManager mManager;

    public QuietHoursTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleState();
            }
        };

        mOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mQh != null) {
                    if (mQh.mode != QuietHours.Mode.AUTO) {
                        mManager.setMode(QuietHours.Mode.AUTO);
                    } else {
                        Intent i = new Intent();
                        i.setClassName(GravityBox.PACKAGE_NAME, QuietHoursActivity.class.getName());
                        startActivity(i);
                    }
                }
                return true;
            }
        };

        mManager = StatusbarQuietHoursManager.getInstance(mContext);
        mQh = mManager.getQuietHours();
    }

    @Override
    protected int onGetLayoutId() {
        return R.layout.quick_settings_tile_quiet_hours;
    }

    @Override
    protected void onTilePostCreate() {
        super.onTilePostCreate();
        mManager.registerListener(this);
    }

    @Override
    public void onQuietHoursChanged() {
        mQh = mManager.getQuietHours();
        updateResources();
    }

    @Override
    public void onTimeTick() {
        updateResources();
    }

    @Override
    protected synchronized void updateTile() {
        if (mQh == null) return;

        if (mQh.uncLocked || !mQh.enabled) {
            mTile.setVisibility(View.GONE);
            return;
        } else {
            mTile.setVisibility(View.VISIBLE);
        }

        switch (mQh.mode) {
            case ON: 
                mLabel = mGbContext.getString(R.string.quick_settings_quiet_hours_on);
                mDrawableId = R.drawable.ic_qs_quiet_hours_on;
                break;
            case OFF:
                mLabel = mGbContext.getString(R.string.quick_settings_quiet_hours_off);
                mDrawableId = R.drawable.ic_qs_quiet_hours_off;
                break;
            case AUTO:
                mLabel = mGbContext.getString(R.string.quick_settings_quiet_hours_auto);
                mDrawableId = mQh.quietHoursActive() ?
                        R.drawable.ic_qs_quiet_hours_auto_on : R.drawable.ic_qs_quiet_hours_auto_off;
                break;
        }

        super.updateTile();
    }

    private void toggleState() {
        if (mQh == null) return;

        switch (mQh.mode) {
            case ON:
                mManager.setMode(QuietHours.Mode.OFF);
                break;
            case AUTO:
                mManager.setMode(mQh.quietHoursActive() ? 
                        QuietHours.Mode.OFF : QuietHours.Mode.ON);
                break;
            case OFF:
                mManager.setMode(QuietHours.Mode.ON);
                break;
        }
    }
}
