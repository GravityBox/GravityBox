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

package com.ceco.kitkat.gravitybox;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.widget.Toast;

public class SettingsManager {
    private static final String BACKUP_PATH = Environment.getExternalStorageDirectory() + "/GravityBox/backup";
    private static final String BACKUP_OK_FLAG = BACKUP_PATH + "/.backup_ok";
    private static final String BACKUP_NO_MEDIA = BACKUP_PATH + "/.nomedia";

    private static Context mContext;
    private static SettingsManager mInstance;

    private SettingsManager(Context context) {
        mContext = context;
    }

    public static SettingsManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SettingsManager(context);
        }
        return mInstance;
    }

    public boolean backupSettings() {
        File targetDir = new File(BACKUP_PATH);
        if (!(targetDir.exists() && targetDir.isDirectory())) {
            if (!targetDir.mkdirs()) {
                Toast.makeText(mContext, R.string.settings_backup_failed, Toast.LENGTH_LONG).show();
                return false;
            }
        }

        // create .nomedia file to disable media scanning on backup folder
        File noMediaFile = new File(BACKUP_NO_MEDIA);
        if (!noMediaFile.exists()) {
            try {
                noMediaFile.createNewFile();
            } catch (IOException ioe) { }
        }

        // delete backup OK flag file first (if exists)
        File backupOkFlagFile = new File(BACKUP_OK_FLAG);
        if (backupOkFlagFile.exists()) {
            backupOkFlagFile.delete();
        }

        // preferences
        String[] prefsFileNames = new String[] { 
                mContext.getPackageName() + "_preferences.xml",
                "ledcontrol.xml"
        };
        for (String prefsFileName : prefsFileNames) {
            File prefsFile = new File(mContext.getFilesDir() + "/../shared_prefs/" + prefsFileName);
            if (prefsFile.exists()) {
                File prefsDestFile = new File(BACKUP_PATH + "/" + prefsFileName);
                try {
                    Utils.copyFile(prefsFile, prefsDestFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, R.string.settings_backup_failed, Toast.LENGTH_LONG).show();
                    return false;
                }
            } else if (prefsFileName.equals(prefsFileNames[0])) {
                // normally, this should never happen
                Toast.makeText(mContext, R.string.settings_backup_no_prefs, Toast.LENGTH_LONG).show();
                return false;
            }
        }

        // other files
        String targetFilesDirPath = BACKUP_PATH + "/files";
        File targetFilesDir = new File(targetFilesDirPath);
        if (!(targetFilesDir.exists() && targetFilesDir.isDirectory())) {
            if (!targetFilesDir.mkdirs()) {
                Toast.makeText(mContext, R.string.settings_backup_failed, Toast.LENGTH_LONG).show();
                return false;
            }
        }
        File[] fileList = mContext.getFilesDir().listFiles();
        if (fileList != null) {
            for (File f : fileList) {
                if (f.isFile() && !f.getName().equals("kis_image.png")) {
                    File outFile = new File(targetFilesDirPath + "/" + f.getName());
                    try {
                        Utils.copyFile(f, outFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(mContext, R.string.settings_backup_failed, Toast.LENGTH_LONG).show();
                        return false;
                    }
                } else if (f.isDirectory() && f.getName().equals("app_picker")) {
                    String appPickerFilesDirPath = targetFilesDirPath + "/app_picker";
                    File appPickerFilesDir = new File(appPickerFilesDirPath);
                    if (!(appPickerFilesDir.exists() && appPickerFilesDir.isDirectory())) {
                        if (!appPickerFilesDir.mkdirs()) {
                            Toast.makeText(mContext, R.string.settings_backup_failed, Toast.LENGTH_LONG).show();
                            return false;
                        }
                    }
                    File[] appPickerfileList = f.listFiles();
                    if (appPickerfileList != null) {
                        for (File apf : appPickerfileList) {
                            File outFile = new File(appPickerFilesDirPath + "/" + apf.getName());
                            try {
                                Utils.copyFile(apf, outFile);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(mContext, R.string.settings_backup_failed, Toast.LENGTH_LONG).show();
                                return false;
                            }
                        }
                    }
                }
            }
        }

        try {
            backupOkFlagFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mContext, R.string.settings_backup_failed, Toast.LENGTH_LONG).show();
            return false;
        }
       
        Toast.makeText(mContext, R.string.settings_backup_success, Toast.LENGTH_SHORT).show();
        return true;
    }

    public boolean isBackupAvailable() {
        File backupOkFlagFile = new File(BACKUP_OK_FLAG);
        return backupOkFlagFile.exists();
    }

    public boolean restoreSettings() {
        if (!isBackupAvailable()) {
            Toast.makeText(mContext, R.string.settings_restore_no_backup, Toast.LENGTH_SHORT).show();
            return false;
        }

        // preferences
        String[] prefsFileNames = new String[] { 
                mContext.getPackageName() + "_preferences.xml",
                "ledcontrol.xml"
        };
        for (String prefsFileName : prefsFileNames) {
            File prefsFile = new File(BACKUP_PATH + "/" + prefsFileName);
            if (prefsFile.exists()) {
                File prefsDestFile = new File(mContext.getFilesDir() + "/../shared_prefs/" + prefsFileName);
                try {
                    Utils.copyFile(prefsFile, prefsDestFile);
                    prefsDestFile.setReadable(true, false);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, R.string.settings_restore_failed, Toast.LENGTH_LONG).show();
                    return false;
                }
            } else if (prefsFileName.equals(prefsFileNames[0])) {
                Toast.makeText(mContext, R.string.settings_restore_no_backup, Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // other files
        String targetFilesDirPath = mContext.getFilesDir().getAbsolutePath();
        File targetFilesDir = new File(targetFilesDirPath);
        if (!(targetFilesDir.exists() && targetFilesDir.isDirectory())) {
            if (targetFilesDir.mkdirs()) {
                targetFilesDir.setExecutable(true, false);
                targetFilesDir.setReadable(true, false);
            }
        }
        File[] fileList = new File(BACKUP_PATH + "/files").listFiles();
        if (fileList != null) {
            for (File f : fileList) {
                if (f.isFile()) {
                    File outFile = new File(targetFilesDirPath + "/" + f.getName());
                    try {
                        Utils.copyFile(f, outFile);
                        outFile.setReadable(true, false);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(mContext, R.string.settings_restore_failed, Toast.LENGTH_LONG).show();
                        return false;
                    }
                } else if (f.isDirectory() && f.getName().equals("app_picker")) {
                    String appPickerFilesDirPath = targetFilesDirPath + "/app_picker";
                    File appPickerFilesDir = new File(appPickerFilesDirPath);
                    if (!(appPickerFilesDir.exists() && appPickerFilesDir.isDirectory())) {
                        if (appPickerFilesDir.mkdirs()) {
                            appPickerFilesDir.setExecutable(true, false);
                            appPickerFilesDir.setReadable(true, false);
                        }
                    }
                    File[] appPickerfileList = f.listFiles();
                    if (appPickerfileList != null) {
                        for (File apf : appPickerfileList) {
                            File outFile = new File(appPickerFilesDirPath + "/" + apf.getName());
                            try {
                                Utils.copyFile(apf, outFile);
                                outFile.setReadable(true, false);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(mContext, R.string.settings_restore_failed, Toast.LENGTH_LONG).show();
                                return true;
                            }
                        }
                    }
                }
            }
        }

        Toast.makeText(mContext, R.string.settings_restore_success, Toast.LENGTH_SHORT).show();
        return true;
    }

    public String getOrCreateUuid() {
        final String prefsName = mContext.getPackageName() + "_preferences";
        SharedPreferences prefs = mContext.getSharedPreferences(prefsName, Context.MODE_WORLD_READABLE);
        String uuid = prefs.getString("settings_uuid", null);
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            prefs.edit().putString("settings_uuid", uuid).commit();
        }
        return uuid;
    }

    public void resetUuid() {
        String prefsName = mContext.getPackageName() + "_preferences";
        SharedPreferences prefs = mContext.getSharedPreferences(prefsName, Context.MODE_WORLD_READABLE);
        prefs.edit().putString("settings_uuid", null).commit();
    }

    public long getUnlockerTimestamp() {
        String prefsName = mContext.getPackageName() + "_preferences";
        SharedPreferences prefs = mContext.getSharedPreferences(prefsName, Context.MODE_WORLD_READABLE);
        return prefs.getLong("gb_unlocker_tstamp", 0);
    }

    public void setUnlockerTimestamp(long ms) {
        String prefsName = mContext.getPackageName() + "_preferences";
        SharedPreferences prefs = mContext.getSharedPreferences(prefsName, Context.MODE_WORLD_READABLE);
        prefs.edit().putLong("gb_unlocker_tstamp", ms).commit();
    }
}
