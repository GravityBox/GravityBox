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
import com.ceco.kitkat.gravitybox.ledcontrol.QuietHours;
import com.ceco.kitkat.gravitybox.ledcontrol.QuietHoursActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.graphics.drawable.Drawable;

public class QuietHoursShortcut extends AShortcut {
    protected static final String ACTION =  ModHwKeys.ACTION_TOGGLE_QUIET_HOURS;

    public QuietHoursShortcut(Context context) {
        super(context);
    }

    @Override
    public String getText() {
        return mContext.getString(R.string.lc_quiet_hours);
    }

    @Override
    public Drawable getIconLeft() {
        return mResources.getDrawable(R.drawable.shortcut_quiet_hours_auto);
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
        list.add(new QhModeItem(mContext.getString(R.string.shortcut_quiet_hours_toggle), 
                R.drawable.shortcut_quiet_hours, null));
        list.add(new QhModeItem(mContext.getString(R.string.quick_settings_quiet_hours_auto), 
                R.drawable.shortcut_quiet_hours_auto, QuietHours.Mode.AUTO));

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
            .setTitle(getText())
            .setAdapter(new IconListAdapter(mContext, list), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    QhModeItem item = (QhModeItem) list.get(which);

                    Intent launchIntent = new Intent(mContext, ShortcutActivity.class);
                    launchIntent.setAction(ShortcutActivity.ACTION_LAUNCH_ACTION);
                    launchIntent.putExtra(ShortcutActivity.EXTRA_ACTION, getAction());
                    launchIntent.putExtra(ShortcutActivity.EXTRA_ACTION_TYPE, getActionType());
                    if (item.getQhMode() != null) {
                        launchIntent.putExtra(QuietHoursActivity.EXTRA_QH_MODE, item.getQhMode().toString());
                    }
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    Intent intent = new Intent();
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, item.getText());
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, item.getIconResource());

                    QuietHoursShortcut.this.onShortcutCreated(intent, listener);
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
        if (intent.hasExtra(QuietHoursActivity.EXTRA_QH_MODE)) {
            launchIntent.putExtra(QuietHoursActivity.EXTRA_QH_MODE,
                    intent.getStringExtra(QuietHoursActivity.EXTRA_QH_MODE));
        }
        context.sendBroadcast(launchIntent);
    }

    class QhModeItem implements IIconListAdapterItem {
        private String mLabel;
        private int mIconResId;
        private QuietHours.Mode mQhMode;

        public QhModeItem(String text, int iconResId, QuietHours.Mode mode) {
            mLabel = text;
            mIconResId = iconResId;
            mQhMode = mode;
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

        public QuietHours.Mode getQhMode() {
            return mQhMode;
        }
    }
}
