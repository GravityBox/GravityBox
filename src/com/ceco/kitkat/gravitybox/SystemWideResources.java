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

package com.ceco.kitkat.gravitybox;

import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import com.ceco.kitkat.gravitybox.R;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public class SystemWideResources {

    public static void initResources(final XSharedPreferences prefs) {
        try {
            Resources systemRes = XResources.getSystem();

            XModuleResources modRes = XModuleResources.createInstance(GravityBox.MODULE_PATH, null);

            int translucentDecor = Integer.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_TRANSLUCENT_DECOR, "0"));
            if (translucentDecor != 0) {
                XResources.setSystemWideReplacement("android", "bool", "config_enableTranslucentDecor", translucentDecor == 1);
            }

            boolean holoBgDither = prefs.getBoolean(GravityBoxSettings.PREF_KEY_HOLO_BG_DITHER, false);
            if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_HOLO_BG_SOLID_BLACK, false)) {
                XResources.setSystemWideReplacement(
                    "android", "drawable", "background_holo_dark", modRes.fwd(R.drawable.background_holo_dark_solid));
            } else if (holoBgDither) {
                XResources.setSystemWideReplacement(
                        "android", "drawable", "background_holo_dark", modRes.fwd(R.drawable.background_holo_dark));
            }
            if (holoBgDither) {
                XResources.setSystemWideReplacement(
                        "android", "drawable", "background_holo_light", modRes.fwd(R.drawable.background_holo_light));
            }

            if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_OVERRIDE, false)) {
                XResources.setSystemWideReplacement("android", "bool", "config_showNavigationBar",
                        prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_ENABLE,
                                SystemPropertyProvider.getSystemConfigBool(systemRes,
                                        "config_showNavigationBar")));
            }

            XResources.setSystemWideReplacement("android", "bool", "config_unplugTurnsOnScreen",
                    prefs.getBoolean(GravityBoxSettings.PREF_KEY_UNPLUG_TURNS_ON_SCREEN,
                            SystemPropertyProvider.getSystemConfigBool(systemRes,
                                    "config_unplugTurnsOnScreen")));

            int pulseNotificationDelay = prefs.getInt(GravityBoxSettings.PREF_KEY_PULSE_NOTIFICATION_DELAY, -1);
            if (pulseNotificationDelay != -1) {
                XResources.setSystemWideReplacement("android", "integer", "config_defaultNotificationLedOff",
                        (pulseNotificationDelay));;
            }

            XResources.setSystemWideReplacement("android", "bool", "config_sip_wifi_only", false);

        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

}
