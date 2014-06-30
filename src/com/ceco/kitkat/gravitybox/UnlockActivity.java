package com.ceco.kitkat.gravitybox;
import com.ceco.kitkat.gravitybox.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.ContextThemeWrapper;

public class UnlockActivity extends Activity implements GravityBoxResultReceiver.Receiver {
    private static final String PKG_UNLOCKER = "com.ceco.gravitybox.unlocker";

    private GravityBoxResultReceiver mReceiver;
    private Handler mHandler;
    private Dialog mAlertDialog;
    private ProgressDialog mProgressDialog;

    private Runnable mGetSystemPropertiesTimeout = new Runnable() {
        @Override
        public void run() {
            dismissProgressDialog();
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    new ContextThemeWrapper(UnlockActivity.this, android.R.style.Theme_Holo_Dialog))
                .setTitle(R.string.app_name)
                .setMessage(R.string.gb_startup_error)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
            mAlertDialog = builder.create();
            mAlertDialog.show();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();
        mReceiver = new GravityBoxResultReceiver(mHandler);
        mReceiver.setReceiver(this);
        Intent intent = new Intent();
        intent.setAction(SystemPropertyProvider.ACTION_GET_SYSTEM_PROPERTIES);
        intent.putExtra("receiver", mReceiver);
        intent.putExtra("settings_uuid", SettingsManager.getInstance(this).getOrCreateUuid());
        mProgressDialog = new ProgressDialog(
                new ContextThemeWrapper(UnlockActivity.this, android.R.style.Theme_Holo_Dialog));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle(R.string.app_name);
        mProgressDialog.setMessage(getString(R.string.gb_startup_progress));
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
        mHandler.postDelayed(mGetSystemPropertiesTimeout, 5000);
        sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mGetSystemPropertiesTimeout);
            mHandler = null;
        }
        mReceiver = null;
        dismissProgressDialog();
        dismissAlertDialog();

        super.onDestroy();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (mHandler != null) {
            mHandler.removeCallbacks(mGetSystemPropertiesTimeout);
            mHandler = null;
        }
        dismissProgressDialog();
        Log.d("GravityBox", "result received: resultCode=" + resultCode);
        if (resultCode == SystemPropertyProvider.RESULT_SYSTEM_PROPERTIES) {
            dismissProgressDialog();
            Intent intent = new Intent(SystemPropertyProvider.ACTION_REGISTER_UUID);
            intent.putExtra(SystemPropertyProvider.EXTRA_UUID,
                    SettingsManager.getInstance(this).getOrCreateUuid());
            sendBroadcast(intent);
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    new ContextThemeWrapper(UnlockActivity.this, android.R.style.Theme_Holo_Dialog))
                .setTitle(R.string.app_name)
                .setMessage(R.string.premium_unlocked_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
            mAlertDialog = builder.create();
            mAlertDialog.show();
        } else {
            finish();
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;
    }

    private void dismissAlertDialog() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        mAlertDialog = null;
    }

    public static class UnlockReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getParcelableExtra("receiver") instanceof ResultReceiver) {
                ((ResultReceiver)intent.getParcelableExtra("receiver")).send(0, null);
            }
            Intent i = new Intent(context, UnlockActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

    public static class PkgManagerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri data = intent.getData();
            String pkgName = data == null ? null : data.getSchemeSpecificPart();
            if (!PKG_UNLOCKER.equals(pkgName)) return;

            if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED) &&
                    !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                SettingsManager.getInstance(context).setUnlockerTimestamp(System.currentTimeMillis());
                maybeRunUnlocker(context);
            }

            if (intent.getAction().equals(Intent.ACTION_PACKAGE_FULLY_REMOVED)) {
                checkPolicyOk(context);
            }
        }
    }

    protected static void maybeRunUnlocker(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setPackage("com.ceco.gravitybox.unlocker");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) { 
            //e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    protected static boolean checkPolicyOk(Context context) {
        boolean unlockerInstalled = true;
        try {
            PackageInfo pkgInfo = context.getPackageManager()
                    .getPackageInfo(PKG_UNLOCKER, 0);
        } catch (NameNotFoundException e) {
            unlockerInstalled = false;
        }

        if (!unlockerInstalled) {
            long addedMs = SettingsManager.getInstance(context).getUnlockerTimestamp();
            long removedMs = System.currentTimeMillis();
            if ((removedMs - addedMs) < 900000) {
                Log.d("GravityBox", "Unlocker uninstalled too early");
                SettingsManager.getInstance(context).resetUuid();
                return false;
            }
        }

        return true;
    }
}
