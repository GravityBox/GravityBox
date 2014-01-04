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

import com.ceco.kitkat.gravitybox.ModHwKeys;
import com.ceco.kitkat.gravitybox.R;

import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.graphics.drawable.Drawable;

public class ShowPowerMenuShortcut extends AShortcut {
    protected static final String ACTION =  ModHwKeys.ACTION_SHOW_POWER_MENU;

    public ShowPowerMenuShortcut(Context context) {
        super(context);
    }

    @Override
    public String getText() {
        return mContext.getString(R.string.hwkey_action_show_power_menu);
    }

    @Override
    public Drawable getIconLeft() {
        return mResources.getDrawable(R.drawable.shortcut_power_menu);
    }

    @Override
    public void createShortcut(CreateShortcutListener listener) {
        Intent launchIntent = new Intent(mContext, ShortcutActivity.class);
        launchIntent.setAction(ShortcutActivity.ACTION_LAUNCH_ACTION);
        launchIntent.putExtra(ShortcutActivity.EXTRA_ACTION, ACTION);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getText());
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                ShortcutIconResource.fromContext(mContext, R.drawable.shortcut_power_menu));

        if (listener != null) {
            listener.onShortcutCreated(intent);
        }
    }

    public static void launchAction(Context context, Intent intent) {
        Intent launchIntent = new Intent(ACTION);
        context.sendBroadcast(launchIntent);
    }
}
