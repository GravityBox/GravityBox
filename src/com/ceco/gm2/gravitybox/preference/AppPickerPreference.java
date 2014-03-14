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

package com.ceco.gm2.gravitybox.preference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.ceco.gm2.gravitybox.GravityBoxSettings.PrefsFragment;
import com.ceco.gm2.gravitybox.GravityBoxSettings.PrefsFragment.ShortcutHandler;
import com.ceco.gm2.gravitybox.R;
import com.ceco.gm2.gravitybox.Utils;
import com.ceco.gm2.gravitybox.adapters.BasicIconListItem;
import com.ceco.gm2.gravitybox.adapters.IIconListAdapterItem;
import com.ceco.gm2.gravitybox.adapters.IconListAdapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

public class AppPickerPreference extends DialogPreference 
                                 implements OnItemClickListener, 
                                            OnItemSelectedListener,
                                            View.OnClickListener {
    private static final String TAG = "GB:AppPickerPreference";
    public static final String SEPARATOR = "#C3C0#";

    public static final int MODE_APP = 0;
    public static final int MODE_SHORTCUT = 1;

    public static PrefsFragment sPrefsFragment;
    private static IconListAdapter sIconPickerAdapter;

    private Context mContext;
    private ListView mListView;
    private EditText mSearch;
    private ProgressBar mProgressBar;
    private AsyncTask<Void, Void, ArrayList<IIconListAdapterItem>> mAsyncTask;
    private String mDefaultSummaryText;
    private int mAppIconSizePx;
    private PackageManager mPackageManager;
    private Resources mResources;
    private int mMode;
    private Spinner mModeSpinner;
    private ImageButton mBtnAppIcon;
    private AppInfo mAppInfo;
    private int mAppIconPreviewSizePx;
    private Dialog mIconPickerDialog;
    private boolean mIconPickerEnabled = true;

    private static LruCache<String, BitmapDrawable> sAppIconCache;
    static {
        final int cacheSize = Math.min((int)Runtime.getRuntime().maxMemory() / 6, 4194304);
        sAppIconCache = new LruCache<String, BitmapDrawable>(cacheSize) {
            @Override
            protected int sizeOf(String key, BitmapDrawable d) {
                return d.getBitmap().getByteCount();
            }
        };
    }

    class AppInfo {
        String name;
        Drawable icon;
        public AppInfo() {
            name = mDefaultSummaryText;
        }
    }

    public AppPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mResources = mContext.getResources();
        mDefaultSummaryText = (String) getSummary();
        mAppIconSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, 
                mResources.getDisplayMetrics());
        mAppIconPreviewSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, 
                mResources.getDisplayMetrics());
        mPackageManager = mContext.getPackageManager();
        mMode = MODE_APP;
        mAppInfo = new AppInfo();

        if (attrs != null) {
            mIconPickerEnabled = attrs.getAttributeBooleanValue(null, "iconPickerEnabled", true);
        }

        setDialogLayoutResource(R.layout.app_picker_preference);
        setPositiveButtonText(null);

        if (sIconPickerAdapter == null) {
            initializeIconPickerAdapter();
        }
    }

    private void initializeIconPickerAdapter() {
        String[] labels = mResources.getStringArray(R.array.shortcut_icon_picker_labels);
        TypedArray icons = mResources.obtainTypedArray(R.array.shortcut_icon_picker_icons);
        if (labels.length != icons.length()) {
            icons.recycle();
            return;
        }

        ArrayList<IIconListAdapterItem> list = new ArrayList<IIconListAdapterItem>(labels.length);
        for (int i = 0; i < labels.length; i++) {
            BasicIconListItem item = new BasicIconListItem(labels[i], null, 
                    icons.getResourceId(i, 0), 0, mResources);
            list.add(item);
        }
        sIconPickerAdapter = new IconListAdapter(mContext, list);
        icons.recycle();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        LinearLayout widgetFrameView = ((LinearLayout)view.findViewById(android.R.id.widget_frame));
        mBtnAppIcon = new ImageButton(mContext);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mAppIconPreviewSizePx, mAppIconPreviewSizePx);
        lp.gravity = Gravity.CENTER;
        mBtnAppIcon.setLayoutParams(lp);
        mBtnAppIcon.setScaleType(ScaleType.CENTER_CROP);
        mBtnAppIcon.setImageDrawable(mAppInfo.icon);
        mBtnAppIcon.setFocusable(false);
        if (mIconPickerEnabled) {
            mBtnAppIcon.setOnClickListener(this);
        } else {
            mBtnAppIcon.setEnabled(false);
        }
        widgetFrameView.addView(mBtnAppIcon);
        widgetFrameView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mListView = (ListView) view.findViewById(R.id.icon_list);
        mListView.setOnItemClickListener(this);

        mSearch = (EditText) view.findViewById(R.id.input_search);
        mSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) { }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                    int arg2, int arg3) { }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                    int arg3) 
            {
                if(mListView.getAdapter() == null)
                    return;
                
                ((IconListAdapter)mListView.getAdapter()).getFilter().filter(arg0);
            }
        });

        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

        mModeSpinner = (Spinner) view.findViewById(R.id.mode_spinner);
        ArrayAdapter<String> mModeSpinnerAdapter = new ArrayAdapter<String>(
                getContext(), android.R.layout.simple_spinner_item,
                        new ArrayList<String>(Arrays.asList(
                                mContext.getString(R.string.app_picker_applications), 
                                mContext.getString(R.string.app_picker_shortcuts))));
        mModeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mModeSpinner.setAdapter(mModeSpinnerAdapter);
        mModeSpinner.setOnItemSelectedListener(this);
        mMode = mModeSpinner.getSelectedItemPosition();

        setData();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mAsyncTask != null && mAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            mAsyncTask.cancel(true);
        }
        mAsyncTask = null;

        if (mIconPickerDialog != null && mIconPickerDialog.isShowing()) {
            mIconPickerDialog.dismiss();
        }
        mIconPickerDialog = null;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            String value = getPersistedString(null);
            if (value != null && value.contains(SEPARATOR)) {
                value = convertOldValueFormat(value);
            }
            mAppInfo = getAppInfoFromValue(value);
            setSummary(mAppInfo.name);
        } else {
            setValue(null);
            setSummary(mDefaultSummaryText);
        }
    } 

    @Override
    public void onClick(View v) {
        if (v != mBtnAppIcon || 
                sIconPickerAdapter == null ||
                getPersistedString(null) == null) return;

        if (mIconPickerDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(R.string.icon_picker_choose_icon_title)
                .setAdapter(sIconPickerAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        try {
                            BasicIconListItem item = (BasicIconListItem) sIconPickerAdapter.getItem(which);
                            Intent intent = Intent.parseUri(getPersistedString(null), 0);
                            if (intent.hasExtra("icon")) {
                                intent.removeExtra("icon");
                            }
                            intent.putExtra("iconResName", mResources.getResourceEntryName(
                                    item.getIconLeftId()));
                            setValue(intent.toUri(0));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
            mIconPickerDialog = builder.create();
        }

        mIconPickerDialog.show();
    }

    private String convertOldValueFormat(String oldValue) {
        try {
            String[] splitValue = oldValue.split(SEPARATOR);
            ComponentName cn = new ComponentName(splitValue[0], splitValue[1]);
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(cn);
            intent.putExtra("mode", MODE_APP);
            String newValue = intent.toUri(0);
            setValue(newValue);
            Log.d(TAG, "Converted old AppPickerPreference value: " + newValue);
            return newValue;
        } catch(Exception e) {
            Log.e(TAG, "Error converting old AppPickerPreference value: " + e.getMessage());
            return null;
        }
    }

    public void setDefaultSummary(String summary) {
        mDefaultSummaryText = summary;
    }

    private void setData() {
        mAsyncTask = new AsyncTask<Void,Void,ArrayList<IIconListAdapterItem>>() {
            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();

                mListView.setVisibility(View.INVISIBLE);
                mSearch.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected ArrayList<IIconListAdapterItem> doInBackground(Void... arg0) {
                ArrayList<IIconListAdapterItem> itemList = new ArrayList<IIconListAdapterItem>();
                List<ResolveInfo> appList = new ArrayList<ResolveInfo>();

                List<PackageInfo> packages = mPackageManager.getInstalledPackages(0);
                Intent mainIntent = new Intent();
                if (mMode == MODE_APP) {
                    mainIntent.setAction(Intent.ACTION_MAIN);
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                } else if (mMode == MODE_SHORTCUT) {
                    mainIntent.setAction(Intent.ACTION_CREATE_SHORTCUT);
                }
                for(PackageInfo pi : packages) {
                    if (this.isCancelled()) break;
                    mainIntent.setPackage(pi.packageName);
                    List<ResolveInfo> activityList = mPackageManager.queryIntentActivities(mainIntent, 0);
                    for(ResolveInfo ri : activityList) {
                        appList.add(ri);
                    }
                }

                Collections.sort(appList, new ResolveInfo.DisplayNameComparator(mPackageManager));
                itemList.add(mMode == MODE_SHORTCUT ? 
                        new ShortcutItem(mContext.getString(R.string.app_picker_none), null) :
                        new AppItem(mContext.getString(R.string.app_picker_none), null));
                for (ResolveInfo ri : appList) {
                    if (this.isCancelled()) break;
                    String appName = ri.loadLabel(mPackageManager).toString();
                    IIconListAdapterItem ai = mMode == MODE_SHORTCUT ?
                            new ShortcutItem(appName, ri) : new AppItem(appName, ri);
                    itemList.add(ai);
                }

                return itemList;
            }

            @Override
            protected void onPostExecute(ArrayList<IIconListAdapterItem> result)
            {
                mProgressBar.setVisibility(View.GONE);
                mSearch.setVisibility(View.VISIBLE);
                mListView.setAdapter(new IconListAdapter(mContext, result));
                ((IconListAdapter)mListView.getAdapter()).notifyDataSetChanged();
                mListView.setVisibility(View.VISIBLE);
            }
        }.execute();
    }

    private void setValue(String value){
        persistString(value);
        mAppInfo = getAppInfoFromValue(value);
        setSummary(mAppInfo.name);
        if (mBtnAppIcon != null) {
            mBtnAppIcon.setImageDrawable(mAppInfo.icon);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        IIconListAdapterItem item = (IIconListAdapterItem) parent.getItemAtPosition(position);
        if (mMode == MODE_APP) {
            AppItem ai = (AppItem) item;
            setValue(ai.getValue());
            getDialog().dismiss();
        } else if (mMode == MODE_SHORTCUT) {
            ShortcutItem si = (ShortcutItem) item;
            if (si.getCreateShortcutIntent() == null) {
                setValue(null);
                getDialog().dismiss();
            } else {
                si.setShortcutCreatedListener(new ShortcutCreatedListener() {
                    @Override
                    public void onShortcutCreated(ShortcutItem sir) {
                        setValue(sir.getValue());
                        // we have to call this explicitly for some yet unknown reason...
                        sPrefsFragment.onSharedPreferenceChanged(getSharedPreferences(), getKey());
                        getDialog().dismiss();
                    }
                });
                sPrefsFragment.obtainShortcut((ShortcutItem) item);
            }
        }
    }

    private AppInfo getAppInfoFromValue(String value) {
        AppInfo appInfo = new AppInfo();
        if (value == null) return appInfo;

        try {
            Intent intent = Intent.parseUri(value, 0);
            int iconResId = intent.getStringExtra("iconResName") != null ?
                    mResources.getIdentifier(intent.getStringExtra("iconResName"),
                    "drawable", mContext.getPackageName()) : 0;
            int mode = intent.getIntExtra("mode", MODE_APP);
            if (mode == MODE_APP) {
                ComponentName cn = intent.getComponent();
                ActivityInfo ai = mPackageManager.getActivityInfo(cn, 0);
                appInfo.name = (ai.loadLabel(mPackageManager).toString());
                if (iconResId != 0) {
                    appInfo.icon = mResources.getDrawable(iconResId);
                } else {
                    appInfo.icon = ai.loadIcon(mPackageManager);
                }
            } else if (mode == MODE_SHORTCUT) {
                appInfo.name = intent.getStringExtra("prefLabel");
                if (iconResId != 0) {
                    appInfo.icon = mResources.getDrawable(iconResId);
                } else if (intent.hasExtra("icon")) {
                    final String appIconPath = intent.getStringExtra("icon");
                    if (appIconPath != null) {
                        File f = new File(appIconPath);
                        FileInputStream fis = new FileInputStream(f);
                        appInfo.icon = new BitmapDrawable(mResources, BitmapFactory.decodeStream(fis));
                        fis.close();
                    }
                }
            }
            return appInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return appInfo;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mMode = position;
        setData();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }

    class AppItem implements IIconListAdapterItem {
        protected String mAppName;
        protected BitmapDrawable mAppIcon;
        protected ResolveInfo mResolveInfo;
        protected Intent mIntent;

        private AppItem() { }

        public AppItem(String appName, ResolveInfo ri) {
            mAppName = appName;
            mResolveInfo = ri;
            if (mResolveInfo != null) {
                mIntent = new Intent(Intent.ACTION_MAIN);
                mIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                ComponentName cn = new ComponentName(mResolveInfo.activityInfo.packageName,
                        mResolveInfo.activityInfo.name);
                mIntent.setComponent(cn);
                mIntent.putExtra("mode", MODE_APP);
            }
        }

        public String getAppName() {
            return mAppName;
        }

        public String getValue() {
            return (mIntent == null ? null : mIntent.toUri(0));
        }

        public Intent getIntent() {
            return mIntent;
        }

        @Override
        public String getText() {
            return mAppName;
        }

        @Override
        public String getSubText() {
            return null;
        }

        protected String getKey() {
            return getValue();
        }

        @Override
        public Drawable getIconLeft() {
            if (mResolveInfo == null) return null;

            if (mAppIcon == null) {
                final String key = getKey();
                mAppIcon = sAppIconCache.get(key);
                if (mAppIcon == null) {
                    Bitmap bitmap = Utils.drawableToBitmap(mResolveInfo.loadIcon(mPackageManager));
                    bitmap = Bitmap.createScaledBitmap(bitmap, mAppIconSizePx, mAppIconSizePx, false);
                    mAppIcon = new BitmapDrawable(mResources, bitmap);
                    sAppIconCache.put(key, mAppIcon);
                }
            }
            return mAppIcon;
        }

        @Override
        public Drawable getIconRight() {
            return null;
        }
    }

    interface ShortcutCreatedListener {
        void onShortcutCreated(ShortcutItem item);
    }

    class ShortcutItem extends AppItem implements ShortcutHandler {
        private Intent mCreateShortcutIntent;
        private ShortcutCreatedListener mShortcutCreatedListener;

        public ShortcutItem(String appName, ResolveInfo ri) {
            mAppName = appName;
            mResolveInfo = ri;
            if (mResolveInfo != null) {
                mCreateShortcutIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
                ComponentName cn = new ComponentName(mResolveInfo.activityInfo.packageName,
                        mResolveInfo.activityInfo.name);
                mCreateShortcutIntent.setComponent(cn);
                // mark intent so we can later identify it comes from GB
                mCreateShortcutIntent.putExtra("gravitybox", true);
            }
        }

        public void setShortcutCreatedListener(ShortcutCreatedListener listener) {
            mShortcutCreatedListener = listener;
        }

        @Override
        protected String getKey() {
            return mCreateShortcutIntent.toUri(0);
        }

        @Override
        public Intent getCreateShortcutIntent() {
            return mCreateShortcutIntent;
        }

        @Override
        public void onHandleShortcut(Intent intent, String name, Bitmap icon) {
            if (intent == null) {
                Toast.makeText(mContext, R.string.app_picker_shortcut_null_intent, Toast.LENGTH_LONG).show();
                return;
            }

            mIntent = intent;
            mIntent.putExtra("mode", MODE_SHORTCUT);

            // generate label
            if (name != null) {
                mAppName  += ": " + name;
                mIntent.putExtra("label", name);
                mIntent.putExtra("prefLabel", mAppName);
            } else {
                mIntent.putExtra("label", mAppName);
                mIntent.putExtra("prefLabel", mAppName);
            }

            if (icon != null) {
                mAppIcon = new BitmapDrawable(mResources, icon);
            }

            // process icon
            if (mAppIcon != null) {
                try {
                    final Context context = AppPickerPreference.this.mContext;
                    final String dir = context.getFilesDir() + "/app_picker";
                    final String fileName = dir + "/" + UUID.randomUUID().toString();
                    File d = new File(dir);
                    d.mkdirs();
                    d.setReadable(true, false);
                    d.setExecutable(true, false);
                    File f = new File(fileName);
                    FileOutputStream fos = new FileOutputStream(f);
                    final boolean iconSaved = icon == null ?
                            mAppIcon.getBitmap().compress(CompressFormat.PNG, 100, fos) :
                                icon.compress(CompressFormat.PNG, 100, fos);
                    if (iconSaved) {
                        mIntent.putExtra("icon", f.getAbsolutePath());
                        f.setReadable(true, false);
                    }
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // callback to shortcut created listener if set
            if (mShortcutCreatedListener != null) {
                mShortcutCreatedListener.onShortcutCreated(this);
            }
        }

        @Override
        public void onShortcutCancelled() {
            Toast.makeText(mContext, R.string.app_picker_shortcut_cancelled, Toast.LENGTH_SHORT).show();
        }
    }
}
