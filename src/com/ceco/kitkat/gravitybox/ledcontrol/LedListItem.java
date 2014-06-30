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

import java.util.Locale;

import com.ceco.kitkat.gravitybox.R;
import com.ceco.kitkat.gravitybox.adapters.IBaseListAdapterItem;
import com.ceco.kitkat.gravitybox.ledcontrol.LedSettings.LedMode;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;

public class LedListItem implements IBaseListAdapterItem {

    private Context mContext;
    private ApplicationInfo mAppInfo;
    private String mAppName;
    private Drawable mAppIcon;
    private LedSettings mLedSettings;

    protected LedListItem(Context context, ApplicationInfo appInfo) {
        mContext = context;
        mAppInfo = appInfo;
        PackageManager pm = mContext.getPackageManager();
        mAppName = (String) mAppInfo.loadLabel(pm);
        mAppIcon = mAppInfo.loadIcon(pm);
        mLedSettings = LedSettings.deserialize(mContext, appInfo.packageName);
    }

    protected ApplicationInfo getAppInfo() {
        return mAppInfo;
    }

    protected String getAppName() {
        return mAppName;
     }

    protected String getAppDesc() {
        if (!isEnabled()) {
            LedSettings defLs = LedSettings.getDefault(mContext);
            return defLs.getEnabled() ? mContext.getString(R.string.lc_defaults_apply) :
                    mContext.getString(R.string.lc_disabled);
        } else {
            String buf = "LED: ";
            if (mLedSettings.getLedMode() == LedMode.OFF) {
                buf += mContext.getString(R.string.lc_led_mode_off);
            } else if (mLedSettings.getLedMode() == LedMode.ORIGINAL) {
                buf += mContext.getString(R.string.lc_led_mode_original);
            } else {
                buf += String.format(Locale.getDefault(),
                    "%s=%dms", mContext.getString(R.string.lc_item_summary_on),
                        mLedSettings.getLedOnMs());
                buf += String.format(Locale.getDefault(),
                    "; %s=%dms", mContext.getString(R.string.lc_item_summary_off),
                    mLedSettings.getLedOffMs());
            }
            if (mLedSettings.getSoundOverride()) {
                if (mLedSettings.getSoundUri() == null) {
                    buf += "; " + mContext.getString(R.string.lc_notif_sound_none);
                } else {
                    Ringtone r = RingtoneManager.getRingtone(mContext, mLedSettings.getSoundUri());
                    if (r != null) {
                        buf += "; " + r.getTitle(mContext);
                    }
                }
            }
            if (mLedSettings.getSoundOnlyOnce()) {
                buf += "; " + mContext.getString(R.string.pref_lc_notif_sound_only_once_title);
            }
            if (mLedSettings.getInsistent()) {
                buf += "; " + mContext.getString(R.string.pref_lc_notif_insistent_title);
            }
            if (mLedSettings.getVibrateOverride()) {
                buf += "; " + mContext.getString(R.string.pref_lc_vibrate_override_title);
            }
            if (LedSettings.isActiveScreenMasterEnabled(mContext) &&
                    mLedSettings.getActiveScreenEnabled()) {
                buf += "; " + mContext.getString(R.string.lc_active_screen);
            }
            if (LedSettings.isQuietHoursEnabled(mContext) &&
                    mLedSettings.getQhIgnore()) {
                buf += "; " + mContext.getString(R.string.pref_lc_qh_ignore_title);
            }
            if (mLedSettings.getOngoing()) {
                buf += "; " + mContext.getString(R.string.lc_item_summary_ongoing);
            }
            return buf;
        }
    }

    protected Drawable getAppIcon() {
        return mAppIcon;
    }

    protected LedSettings getLedSettings() {
        return mLedSettings;
    }

    protected void refreshLedSettings() {
        mLedSettings = LedSettings.deserialize(mContext, mAppInfo.packageName);
    }

    protected boolean isEnabled() {
        return mLedSettings.getEnabled();
    }

    protected void setEnabled(boolean enabled) {
        mLedSettings.setEnabled(enabled);
        mLedSettings.serialize();
    }

    @Override
    public String getText() {
        return getAppName();
    }

    @Override
    public String getSubText() {
        return getAppDesc();
    }
}
