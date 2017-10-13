/*
 * Copyright (C) 2016 Peter Gregus for GravityBox Project (C3C076@xda)
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

import com.ceco.nougat.gravitybox.R;
import com.ceco.nougat.gravitybox.ledcontrol.QuietHoursActivity;
import com.ceco.nougat.gravitybox.shortcuts.AShortcut;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.widget.Toast;

public class GravityBoxService extends IntentService {
    public static final String ACTION_TOGGLE_SYNC = "gravitybox.intent.action.TOGGLE_SYNC";
    public static final String ACTION_GET_SYNC_STATUS = "gravitybox.intent.action.GET_SYNC_STATUS";

    public static final int RESULT_SYNC_STATUS = 0;
    public static final String KEY_SYNC_STATUS = "syncStatus";

    private Handler mHandler;

    public GravityBoxService() {
        super("GravityBoxService");
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(ACTION_TOGGLE_SYNC)) {
            final boolean newState;
            if (intent.hasExtra(AShortcut.EXTRA_ENABLE)) {
                newState = intent.getBooleanExtra(AShortcut.EXTRA_ENABLE, false);
            } else {
                newState = !ContentResolver.getMasterSyncAutomatically();
            }
            ContentResolver.setMasterSyncAutomatically(newState);
            if (intent.getBooleanExtra(AShortcut.EXTRA_SHOW_TOAST, false)) {
                showToast(newState ? 
                        R.string.quick_settings_sync_on :
                            R.string.quick_settings_sync_off);
            }
        } else if (intent.getAction().equals(ACTION_GET_SYNC_STATUS)) {
            boolean syncStatus = ContentResolver.getMasterSyncAutomatically();
            ResultReceiver receiver = intent.getParcelableExtra("receiver");
            Bundle data = new Bundle();
            data.putBoolean(KEY_SYNC_STATUS, syncStatus);
            receiver.send(RESULT_SYNC_STATUS, data);
        } else if (intent.getAction().equals(QuietHoursActivity.ACTION_SET_QUIET_HOURS_MODE)) {
            setQuietHoursMode(intent.getStringExtra(QuietHoursActivity.EXTRA_QH_MODE),
                    intent.getBooleanExtra(AShortcut.EXTRA_SHOW_TOAST, false));
        }
    }

    private void setQuietHoursMode(final String mode, final boolean showToast) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                QuietHoursActivity.setQuietHoursMode(getApplicationContext(), mode, showToast);
            }
        });
    }

    private void showToast(final int messageResId) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), messageResId, 
                            Toast.LENGTH_SHORT).show();
            }
        });
    }
}
