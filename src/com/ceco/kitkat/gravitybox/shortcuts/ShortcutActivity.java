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

import com.ceco.kitkat.gravitybox.R;
import com.ceco.kitkat.gravitybox.adapters.IIconListAdapterItem;
import com.ceco.kitkat.gravitybox.adapters.IconListAdapter;
import com.ceco.kitkat.gravitybox.shortcuts.AShortcut.CreateShortcutListener;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class ShortcutActivity extends ListActivity {
    protected static final String ACTION_LAUNCH_ACTION = "gravitybox.intent.action.LAUNCH_ACTION";
    protected static final String EXTRA_ACTION = "action";
    protected static final String EXTRA_DATA = "actionData";

    private Context mContext;
    private IconListAdapter mListAdapter;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            finish();
            return;
        } else if (intent.getAction().equals(Intent.ACTION_CREATE_SHORTCUT)) {
            setContentView(R.layout.shortcut_activity);
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
