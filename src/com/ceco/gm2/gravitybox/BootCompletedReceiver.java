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

package com.ceco.gm2.gravitybox;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            prepareAssets(context);
        }
    }

    // copies required files from assets to file system
    private void prepareAssets(Context context) {
        File f;

        // prepare battery sound files
        final String[] bSounds = new String[] { "battery_charged.ogg", "charger_plugged.ogg", "charger_unplugged.ogg" };
        for (String bSound : bSounds) {
            f = new File(context.getFilesDir() + "/" + bSound);
            if (!f.exists()) {
                Utils.writeAssetToFile(context, bSound, f);
            }
            if (f.exists()) {
                f.setReadable(true, false);
            }
        }
    }
}
