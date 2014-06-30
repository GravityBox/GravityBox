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

package com.ceco.kitkat.gravitybox.preference;

import java.util.ArrayList;


import com.ceco.kitkat.gravitybox.GravityBoxSettings;
import com.ceco.kitkat.gravitybox.ModStatusBar;
import com.ceco.kitkat.gravitybox.R;
import com.ceco.kitkat.gravitybox.Utils;
import com.ceco.kitkat.gravitybox.adapters.IIconCheckListAdapterItem;
import com.ceco.kitkat.gravitybox.adapters.IconCheckListAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class OngoingNotifPreference extends DialogPreference 
                implements OnItemClickListener, OnClickListener {

    private Context mContext;
    private Resources mResources;
    private ListView mListView;
    private Button mBtnResetList;
    private ArrayList<IIconCheckListAdapterItem> mListData;
    private AlertDialog mAlertDialog;
    private TextView mDescription;
    private View mDivider;
    private int mIconSizePx;

    public OngoingNotifPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mResources = context.getResources();
        mIconSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25,
                mResources.getDisplayMetrics());
        setDialogLayoutResource(R.layout.ongoing_notif_preference);
    }

    @Override
    protected void onBindDialogView(View view) {
        mListView = (ListView) view.findViewById(R.id.icon_list);
        mListView.setOnItemClickListener(this);
        mListView.setEmptyView(view.findViewById(R.id.info_list_empty));

        mBtnResetList = (Button) view.findViewById(R.id.btnReset);
        mBtnResetList.setOnClickListener(this);

        mDescription = (TextView) view.findViewById(R.id.description);
        mDivider = (View) view.findViewById(R.id.divider);

        super.onBindView(view);

        setData();
    }

    @Override
    public void onActivityDestroy() {
        if (mAlertDialog != null) {
            if (mAlertDialog.isShowing()) {
                mAlertDialog.dismiss();
            }
            mAlertDialog = null;
        }
        super.onActivityDestroy();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String buf = "";
            for(int i = 0; i < mListData.size(); i++) {
                OngoingNotif on = (OngoingNotif) mListData.get(i);
                if (on.isChecked()) {
                    if (!buf.isEmpty()) buf += "#C3C0#";
                    buf += on.getKey();
                }
            }
            persistString(buf);
            Intent intent = new Intent();
            intent.setAction(GravityBoxSettings.ACTION_PREF_ONGOING_NOTIFICATIONS_CHANGED);
            intent.putExtra(GravityBoxSettings.EXTRA_ONGOING_NOTIF, buf);
            mContext.sendBroadcast(intent);
        }
    }

    private void setData() {
        mListView.setAdapter(null);
        mListData = new ArrayList<IIconCheckListAdapterItem>();
        mBtnResetList.setVisibility(View.GONE);
        final String prefData = getPersistedString(null);

        final String notifData = Settings.Secure.getString(mContext.getContentResolver(),
                ModStatusBar.SETTING_ONGOING_NOTIFICATIONS);
        if (notifData != null && !notifData.isEmpty()) {
            final String[] notifications = notifData.split("#C3C0#");
            mListData = new ArrayList<IIconCheckListAdapterItem>();
            for (String n : notifications) {
                final String[] nd = n.split(",");
                if (nd.length == 2) {
                    OngoingNotif on = new OngoingNotif(nd[0], Integer.valueOf(nd[1]));
                    on.setChecked(prefData != null && prefData.contains(on.getKey()));
                    mListData.add(on);
                }
            }
            IconCheckListAdapter adapter = new IconCheckListAdapter(mContext, mListData);
            adapter.setSubtextEnabled(false);
            mListView.setAdapter(adapter);
            ((IconCheckListAdapter)mListView.getAdapter()).notifyDataSetChanged();
            mBtnResetList.setVisibility(View.VISIBLE);
        }

        if (notifData == null || notifData.isEmpty() || prefData == null) {
            mDescription.setVisibility(View.VISIBLE);
            mDivider.setVisibility(View.VISIBLE);
        } else {
            mDescription.setVisibility(View.GONE);
            mDivider.setVisibility(View.GONE);
        }
    }

    class OngoingNotif implements IIconCheckListAdapterItem {
        private String mPackage;
        private int mIconId;
        private boolean mChecked;
        private String mName;
        private Drawable mIcon;

        public OngoingNotif(String pkg, int iconId) {
            mPackage = pkg;
            mIconId = iconId;
        }

        public String getKey() {
            if (mPackage == null) return null;

            return mPackage + "," + mIconId;
        }

        @Override
        public Drawable getIconLeft() {
            if (mPackage == null || mIconId == 0) return null;

            if (mIcon == null) {
                Resources res = mResources;
                if (!mPackage.equals("android") &&
                        !mPackage.equals(mContext.getPackageName())) {
                    try {
                       res = mContext.createPackageContext(mPackage, 
                                    Context.CONTEXT_IGNORE_SECURITY).getResources();
                    } catch (NameNotFoundException e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                try {
                    Drawable d = res.getDrawable(mIconId);
                    if (d != null) {
                        Bitmap b = Utils.drawableToBitmap(d.mutate());
                        b = Bitmap.createScaledBitmap(b, mIconSizePx, mIconSizePx, true);
                        mIcon = new BitmapDrawable(mResources, b);
                    }
                } catch (Resources.NotFoundException nfe) {
                    //
                }
            }

            return mIcon;
        }

        @Override
        public Drawable getIconRight() {
            return null;
        }

        @Override
        public String getText() {
            if (mName == null) {
                try {
                    PackageManager pm = mContext.getPackageManager();
                    PackageInfo pi = pm.getPackageInfo(mPackage, 0);
                    mName = (String) pi.applicationInfo.loadLabel(pm);
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                    mName = mPackage;
                }
            }
            return mName; 
        }

        @Override
        public String getSubText() {
            return null;
        }

        @Override
        public void setChecked(boolean checked) {
            mChecked = checked;
        }

        @Override
        public boolean isChecked() {
            return mChecked;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        IIconCheckListAdapterItem item = 
                (IIconCheckListAdapterItem) parent.getItemAtPosition(position);
        item.setChecked(!item.isChecked());
        mListView.invalidateViews();
    }

    @Override
    public void onClick(View v) {
        if (v == mBtnResetList) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(R.string.ongoing_notif_reset_alert);
            builder.setNegativeButton(android.R.string.no, null);
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    mAlertDialog = null;
                    Intent intent = new Intent();
                    intent.setAction(GravityBoxSettings.ACTION_PREF_ONGOING_NOTIFICATIONS_CHANGED);
                    intent.putExtra(GravityBoxSettings.EXTRA_ONGOING_NOTIF_RESET, true);
                    mContext.sendBroadcast(intent);
                    persistString("");
                    mListData.clear();
                    if (mListView.getAdapter() != null) {
                        ((IconCheckListAdapter) mListView.getAdapter()).notifyDataSetChanged();
                    }
                    mBtnResetList.setVisibility(View.GONE);
                    mDescription.setVisibility(View.VISIBLE);
                    mDivider.setVisibility(View.VISIBLE);
                }
            });
            mAlertDialog = builder.create();
            mAlertDialog.show();
        }
    }
}
