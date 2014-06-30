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

package com.ceco.kitkat.gravitybox.shortcuts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ceco.kitkat.gravitybox.R;
import com.ceco.kitkat.gravitybox.Utils;
import com.ceco.kitkat.gravitybox.adapters.IIconListAdapterItem;
import com.ceco.kitkat.gravitybox.adapters.IconListAdapter;
import com.ceco.kitkat.gravitybox.ledcontrol.LedSettings;
import com.ceco.kitkat.gravitybox.shortcuts.AShortcut.CreateShortcutListener;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

public class ShortcutActivity extends ListActivity {
    public static final String ACTION_LAUNCH_ACTION = "gravitybox.intent.action.LAUNCH_ACTION";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_ACTION_TYPE = "actionType";

    private Context mContext;
    private IconListAdapter mListAdapter;
    private Button mBtnCancel;
    private boolean mInvokedFromGb;

    private static List<String> UNSAFE_ACTIONS = new ArrayList<String>(Arrays.asList(
            ExpandNotificationsShortcut.ACTION,
            ExpandQuicksettingsShortcut.ACTION,
            NetworkModeShortcut.ACTION,
            RecentAppsShortcut.ACTION,
            MobileDataShortcut.ACTION,
            WifiShortcut.ACTION,
            BluetoothShortcut.ACTION,
            WifiApShortcut.ACTION,
            NfcShortcut.ACTION,
            LocationModeShortcut.ACTION,
            SmartRadioShortcut.ACTION,
            AirplaneModeShortcut.ACTION
    ));

    public static boolean isActionSafe(String action) {
        return (!UNSAFE_ACTIONS.contains(action));
    }

    public static boolean isGbBroadcastShortcut(Intent intent) {
        return (intent != null && intent.getAction() != null &&
                intent.getAction().equals(ShortcutActivity.ACTION_LAUNCH_ACTION) &&
                intent.hasExtra(ShortcutActivity.EXTRA_ACTION_TYPE) &&
                intent.getStringExtra(ShortcutActivity.EXTRA_ACTION_TYPE).equals("broadcast"));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            finish();
            return;
        } else if (intent.getAction().equals(Intent.ACTION_CREATE_SHORTCUT)) {
            setContentView(R.layout.shortcut_activity);
            mBtnCancel = (Button) findViewById(R.id.btnCancel);
            mBtnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            mInvokedFromGb = intent.hasExtra("gravitybox");
            return;
        } else if (intent.getAction().equals(ACTION_LAUNCH_ACTION) &&
                intent.hasExtra(EXTRA_ACTION)) {
            launchAction(intent);
            finish();
            return;
        } else {
            finish();
            return;
        }
    }
    
    private void launchAction(Intent intent) {
        final String action = intent.getStringExtra(EXTRA_ACTION);

        if (action.equals(ShowPowerMenuShortcut.ACTION)) {
            ShowPowerMenuShortcut.launchAction(mContext, intent);
        } else if (action.equals(ExpandNotificationsShortcut.ACTION)) {
            ExpandNotificationsShortcut.launchAction(mContext, intent);
        } else if (action.equals(ExpandQuicksettingsShortcut.ACTION)) {
            ExpandQuicksettingsShortcut.launchAction(mContext, intent);
        } else if (action.equals(ExpandedDesktopShortcut.ACTION)) {
            ExpandedDesktopShortcut.launchAction(mContext, intent);
        } else if (action.equals(ScreenshotShortcut.ACTION)) {
            ScreenshotShortcut.launchAction(mContext, intent);
        } else if (action.equals(ScreenrecordShortcut.ACTION)) {
            ScreenrecordShortcut.launchAction(mContext, intent);
        } else if (action.equals(TorchShortcut.ACTION)) {
            TorchShortcut.launchAction(mContext, intent);
        } else if (action.equals(NetworkModeShortcut.ACTION)) {
            NetworkModeShortcut.launchAction(mContext, intent);
        } else if (action.equals(RecentAppsShortcut.ACTION)) {
            RecentAppsShortcut.launchAction(mContext, intent);
        } else if (action.equals(AppLauncherShortcut.ACTION)) {
            AppLauncherShortcut.launchAction(mContext, intent);
        } else if (action.equals(RotationLockShortcut.ACTION)) {
            RotationLockShortcut.launchAction(mContext, intent);
        } else if (action.equals(SleepShortcut.ACTION)) {
            SleepShortcut.launchAction(mContext, intent);
        } else if (action.equals(MobileDataShortcut.ACTION)) {
            MobileDataShortcut.launchAction(mContext, intent);
        } else if (action.equals(WifiShortcut.ACTION)) {
            WifiShortcut.launchAction(mContext, intent);
        } else if (action.equals(BluetoothShortcut.ACTION)) {
            BluetoothShortcut.launchAction(mContext, intent);
        } else if (action.equals(WifiApShortcut.ACTION)) {
            WifiApShortcut.launchAction(mContext, intent);
        } else if (action.equals(LocationModeShortcut.ACTION)) {
            LocationModeShortcut.launchAction(mContext, intent);
        } else if (action.equals(NfcShortcut.ACTION)) {
            NfcShortcut.launchAction(mContext, intent);
        } else if (action.equals(GoogleNowShortcut.ACTION)) {
            GoogleNowShortcut.launchAction(mContext, intent);
        } else if (action.equals(VolumePanelShortcut.ACTION)) {
            VolumePanelShortcut.launchAction(mContext, intent);
        } else if (action.equals(LauncherDrawerShortcut.ACTION)) {
            LauncherDrawerShortcut.launchAction(mContext, intent);
        } else if (action.equals(BrightnessDialogShortcut.ACTION)) {
            BrightnessDialogShortcut.launchAction(mContext, intent);
        } else if (action.equals(SmartRadioShortcut.ACTION)) {
            SmartRadioShortcut.launchAction(mContext, intent);
        } else if (action.equals(QuietHoursShortcut.ACTION)) {
            QuietHoursShortcut.launchAction(mContext, intent);
        } else if (action.equals(AirplaneModeShortcut.ACTION)) {
            AirplaneModeShortcut.launchAction(mContext, intent);
        } else if (action.equals(RingerModeShortcut.ACTION)) {
            RingerModeShortcut.launchAction(mContext, intent);
        } else if (action.equals(SyncShortcut.ACTION)) {
            SyncShortcut.launchAction(mContext, intent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setData() {
        ArrayList<IIconListAdapterItem> list = new ArrayList<IIconListAdapterItem>();
        list.add(new ShowPowerMenuShortcut(mContext));
        list.add(new ExpandNotificationsShortcut(mContext));
        list.add(new ExpandQuicksettingsShortcut(mContext));
        list.add(new ExpandedDesktopShortcut(mContext));
        list.add(new GoogleNowShortcut(mContext));
        list.add(new ScreenshotShortcut(mContext));
        list.add(new ScreenrecordShortcut(mContext));
        if (Utils.hasFlash(mContext)) {
            list.add(new TorchShortcut(mContext));
        }
        list.add(new LocationModeShortcut(mContext));
        list.add(new WifiShortcut(mContext));
        list.add(new WifiApShortcut(mContext));
        if (!Utils.isWifiOnly(mContext)) {
            list.add(new MobileDataShortcut(mContext));
            list.add(new NetworkModeShortcut(mContext));
            list.add(new SmartRadioShortcut(mContext));
        }
        list.add(new AirplaneModeShortcut(mContext));
        list.add(new BluetoothShortcut(mContext));
        if (Utils.hasNfc(mContext)) {
            list.add(new NfcShortcut(mContext));
        }
        list.add(new SyncShortcut(mContext));
        if (mInvokedFromGb) {
            list.add(new MediaControlShortcut(mContext));
        }
        list.add(new VolumePanelShortcut(mContext));
        list.add(new RingerModeShortcut(mContext));
        list.add(new BrightnessDialogShortcut(mContext));
        list.add(new RecentAppsShortcut(mContext));
        if (mInvokedFromGb) {
            list.add(new KillAppShortcut(mContext));
            list.add(new SwitchAppShortcut(mContext));
        }
        list.add(new AppLauncherShortcut(mContext));
        list.add(new LauncherDrawerShortcut(mContext));
        list.add(new RotationLockShortcut(mContext));
        list.add(new SleepShortcut(mContext));
        if (!LedSettings.isUncLocked(mContext)) {
            list.add(new QuietHoursShortcut(mContext));
        }

        mListAdapter = new IconListAdapter(mContext, list);
        setListAdapter(mListAdapter);
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        AShortcut s = (AShortcut) mListAdapter.getItem(position);
        s.createShortcut(new CreateShortcutListener() {
            @Override
            public void onShortcutCreated(Intent intent) {
                ShortcutActivity.this.setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
}
