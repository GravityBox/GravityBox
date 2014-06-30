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

package com.ceco.kitkat.gravitybox.shortcuts;

import java.util.ArrayList;
import java.util.List;

import com.ceco.kitkat.gravitybox.ModHwKeys;
import com.ceco.kitkat.gravitybox.R;
import com.ceco.kitkat.gravitybox.adapters.IIconListAdapterItem;
import com.ceco.kitkat.gravitybox.adapters.IconListAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.graphics.drawable.Drawable;

public class RingerModeShortcut extends AShortcut {
    protected static final String ACTION =  ModHwKeys.ACTION_SET_RINGER_MODE;

    public static final int MODE_RING = 0;
    public static final int MODE_RING_VIBRATE = 1;
    public static final int MODE_SILENT = 2;
    public static final int MODE_VIBRATE = 3;

    public RingerModeShortcut(Context context) {
        super(context);
    }

    @Override
    public String getText() {
        return mContext.getString(R.string.shortcut_ringer_mode);
    }

    @Override
    public Drawable getIconLeft() {
        return mResources.getDrawable(R.drawable.shortcut_ringer_ring_vibrate);
    }

    @Override
    protected String getAction() {
        return ACTION;
    }

    @Override
    protected String getShortcutName() {
        return getText();
    }

    @Override
    protected ShortcutIconResource getIconResource() {
        return null;
    }

    @Override
    protected void createShortcut(final CreateShortcutListener listener) {
        final List<IIconListAdapterItem> list = new ArrayList<IIconListAdapterItem>();
        list.add(new RingerModeItem(mContext.getString(R.string.ringer_mode_sound), 
                R.drawable.shortcut_ringer_ring, MODE_RING));
        list.add(new RingerModeItem(mContext.getString(R.string.ringer_mode_sound_vibrate),
                R.drawable.shortcut_ringer_ring_vibrate, MODE_RING_VIBRATE));
        list.add(new RingerModeItem(mContext.getString(R.string.ringer_mode_silent),
                R.drawable.shortcut_ringer_silent, MODE_SILENT));
        list.add(new RingerModeItem(mContext.getString(R.string.ringer_mode_vibrate),
                R.drawable.shortcut_ringer_vibrate, MODE_VIBRATE));

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
            .setTitle(getText())
            .setAdapter(new IconListAdapter(mContext, list), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    RingerModeItem item = (RingerModeItem) list.get(which);

                    Intent launchIntent = new Intent(mContext, ShortcutActivity.class);
                    launchIntent.setAction(ShortcutActivity.ACTION_LAUNCH_ACTION);
                    launchIntent.putExtra(ShortcutActivity.EXTRA_ACTION, getAction());
                    launchIntent.putExtra(ShortcutActivity.EXTRA_ACTION_TYPE, getActionType());
                    launchIntent.putExtra(ModHwKeys.EXTRA_RINGER_MODE, item.getRingerMode());
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    Intent intent = new Intent();
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, item.getText());
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, item.getIconResource());

                    RingerModeShortcut.this.onShortcutCreated(intent, listener);
                    dialog.dismiss();
                }
            })
            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void launchAction(final Context context, Intent intent) {
        Intent launchIntent = new Intent(ACTION);
        launchIntent.putExtra(ModHwKeys.EXTRA_RINGER_MODE,
                intent.getIntExtra(ModHwKeys.EXTRA_RINGER_MODE,
                        MODE_RING_VIBRATE));
        context.sendBroadcast(launchIntent);
    }

    class RingerModeItem implements IIconListAdapterItem {
        private String mLabel;
        private int mIconResId;
        private int mRingerMode;

        public RingerModeItem(String text, int iconResId, int ringerMode) {
            mLabel = text;
            mIconResId = iconResId;
            mRingerMode = ringerMode;
        }

        @Override
        public String getText() {
            return mLabel;
        }

        @Override
        public String getSubText() {
            return null;
        }

        @Override
        public Drawable getIconLeft() {
            return mResources.getDrawable(mIconResId);
        }

        @Override
        public Drawable getIconRight() {
            return null;
        }

        public ShortcutIconResource getIconResource() {
            return ShortcutIconResource.fromContext(mContext, mIconResId);
        }

        public int getRingerMode() {
            return mRingerMode;
        }
    }
}
