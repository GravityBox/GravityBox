package com.ceco.kitkat.gravitybox;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class StatusbarSignalClusterMtk extends StatusbarSignalCluster {
    protected boolean mRoamingIndicatorsDisabled;
    protected static boolean[] mMobileVisible = null, mRoaming = null;
    protected static int[] mRoamingId = null;
    protected static ImageView[] mMobile = null, mMobileActivity = null, mMobileType = null, mMobileRoam = null;
    protected static Object[] mMobileActivityIds = null, mMobileTypeIds = null;
    protected static Object[][] mMobileIconIds = null;

    public StatusbarSignalClusterMtk(LinearLayout view, StatusBarIconManager iconManager) {
        super(view, iconManager);

        mRoamingIndicatorsDisabled = false;
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        super.onBroadcastReceived(context, intent);

        if (intent.getAction().equals(GravityBoxSettings.ACTION_DISABLE_ROAMING_INDICATORS_CHANGED)) {
            mRoamingIndicatorsDisabled = intent.getBooleanExtra(
                    GravityBoxSettings.EXTRA_INDICATORS_DISABLED, false);
        }
    }

    @Override
    public void initPreferences(XSharedPreferences prefs) {
        super.initPreferences(prefs);

        mRoamingIndicatorsDisabled = prefs.getBoolean(
                GravityBoxSettings.PREF_KEY_DISABLE_ROAMING_INDICATORS, false);
    }

    @Override
    protected void updateWiFiIcon() {
        try {
            Object wifiStrengthId = null, wifiActivityId = null;
            if (XposedHelpers.getBooleanField(mView, "mWifiVisible") &&
                    mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED) {
                ImageView wifiIcon = (ImageView) XposedHelpers.getObjectField(mView, "mWifi");
                if (wifiIcon != null) {
                    wifiStrengthId = (Object) XposedHelpers.getObjectField(mView, "mWifiStrengthId");
                    int resId = (wifiStrengthId instanceof Integer) ?
                            (Integer) wifiStrengthId :
                                (Integer) XposedHelpers.callMethod(wifiStrengthId, "getIconId");
                    Drawable d = mIconManager.getWifiIcon(resId, true);
                    if (d != null) wifiIcon.setImageDrawable(d);
                }
                ImageView wifiActivity = (ImageView) XposedHelpers.getObjectField(mView, "mWifiActivity");
                if (wifiActivity != null) {
                    try {
                        wifiActivityId = (Object) XposedHelpers.getObjectField(mView, "mWifiActivityId");
                        int resId = (wifiActivityId instanceof Integer) ?
                                (Integer) wifiActivityId :
                                    (Integer) XposedHelpers.callMethod(wifiActivityId, "getIconId");
                        Drawable d = mResources.getDrawable(resId).mutate();
                        d = mIconManager.applyDataActivityColorFilter(d);
                        wifiActivity.setImageDrawable(d);
                    } catch (Resources.NotFoundException e) {
                        wifiActivity.setImageDrawable(null);
                    }
                }
            }
        } catch (Throwable t) {
            logAndMute("updateWiFiIcon", t);
        }
    }

    @Override
    protected void updateMobileIcon() {
        try {
            Object mobile = XposedHelpers.getObjectField(mView, "mMobile");
            if (mMobile == null) {
                mMobile = (mobile instanceof ImageView) ?
                        new ImageView[] { (ImageView) mobile, Utils.hasGeminiSupport() ?
                                (ImageView) XposedHelpers.getObjectField(mView, "mMobileGemini") : null } :
                                    (ImageView[]) mobile;
            }
            Object mobileActivity = XposedHelpers.getObjectField(mView, "mMobileActivity");
            if (mMobileActivity == null) {
                mMobileActivity = (mobileActivity instanceof ImageView) ?
                        new ImageView[] { (ImageView) mobileActivity, Utils.hasGeminiSupport() ?
                                (ImageView) XposedHelpers.getObjectField(mView, "mMobileActivityGemini") : null } :
                                    (ImageView[]) mobileActivity;
            }
            Object mobileActivityIds = XposedHelpers.getObjectField(mView, "mMobileActivityId");
            mMobileActivityIds = (mobileActivityIds instanceof Object[]) ? (Object[]) mobileActivityIds :
                    new Object[] { (Object) mobileActivityIds, Utils.hasGeminiSupport() ?
                            (Object) XposedHelpers.getObjectField(mView, "mMobileActivityIdGemini") : null };
            Object mobileRoam = XposedHelpers.getObjectField(mView, "mMobileRoam");
            if (mMobileRoam == null) {
                mMobileRoam = (mobileRoam instanceof ImageView) ?
                        new ImageView[] { (ImageView) mobileRoam, Utils.hasGeminiSupport() ?
                                (ImageView) XposedHelpers.getObjectField(mView, "mMobileRoamGemini") : null } :
                                    (ImageView[]) mobileRoam;
            }
            if (Utils.hasGeminiSupport()) {
                Object mobileIconIds = XposedHelpers.getObjectField(mView, "mMobileStrengthId");
                mMobileIconIds = (mobileIconIds instanceof Object[][]) ? (Object[][]) mobileIconIds :
                        new Object[][] { (Object[]) mobileIconIds, Utils.hasGeminiSupport() ?
                                (Object[]) XposedHelpers.getObjectField(mView, "mMobileStrengthIdGemini") : null };
            }
            Object mobileType = XposedHelpers.getObjectField(mView, "mMobileType");
            if (mMobileType == null) {
                mMobileType = (mobileType instanceof ImageView) ?
                        new ImageView[] { (ImageView) mobileType, Utils.hasGeminiSupport() ?
                                (ImageView) XposedHelpers.getObjectField(mView, "mMobileTypeGemini") : null } :
                                    (ImageView[]) mobileType;
            }
            Object mobileTypeIds = XposedHelpers.getObjectField(mView, "mMobileTypeId");
            mMobileTypeIds = (mobileTypeIds instanceof Object[]) ? (Object[]) mobileTypeIds :
                    new Object[] { (Object) mobileTypeIds, Utils.hasGeminiSupport() ?
                            (Object) XposedHelpers.getObjectField(mView, "mMobileTypeIdGemini") : null };
            Object mobileVisible = XposedHelpers.getObjectField(mView, "mMobileVisible");
            mMobileVisible = (mobileVisible instanceof Boolean) ?
                    new boolean[] { (Boolean) mobileVisible, Utils.hasGeminiSupport() ?
                            (boolean) XposedHelpers.getBooleanField(mView, "mMobileVisibleGemini") : false } :
                                (boolean[]) mobileVisible;
            Object roaming = XposedHelpers.getObjectField(mView, "mRoaming");
            mRoaming = (roaming instanceof Boolean) ?
                    new boolean[] { (Boolean) roaming, Utils.hasGeminiSupport() ?
                            (boolean) XposedHelpers.getBooleanField(mView, "mRoamingGemini") : false } :
                                (boolean[]) roaming;
            Object roamingId = XposedHelpers.getObjectField(mView, "mRoamingId");
            if (mRoamingId == null) {
                mRoamingId = (roamingId instanceof Integer) ?
                        new int[] { (Integer) roamingId, Utils.hasGeminiSupport() ?
                                (int) XposedHelpers.getIntField(mView, "mRoamingGeminiId") : 0 } :
                                    (int[]) roamingId;
            }

            for (int slot = 0; slot < mMobile.length; slot++) {
                if (mMobileVisible != null && mMobileVisible[slot] &&
                        mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED) {
                    if (mMobile[slot] != null) {
                        int resId = (Integer) XposedHelpers.callMethod(Utils.hasGeminiSupport() ? 
                                mMobileIconIds[slot][0] : XposedHelpers.getObjectField(mView, "mMobileStrengthId"),
                                "getIconId");
                        Drawable d = mIconManager.getMobileIcon(slot, resId, true);
                        if (d != null) mMobile[slot].setImageDrawable(d);
                    }
                    if (mIconManager.isMobileIconChangeAllowed(slot)) {
                        if (mMobileActivity != null) {
                            try {
                                int resId = (Integer) XposedHelpers.callMethod(
                                        mMobileActivityIds[slot], "getIconId");
                                Drawable d = mResources.getDrawable(resId).mutate();
                                d = mIconManager.applyDataActivityColorFilter(slot, d);
                                mMobileActivity[slot].setImageDrawable(d);
                            } catch (Resources.NotFoundException e) {
                                mMobileActivity[slot].setImageDrawable(null);
                            }
                        }
                        if (mMobileType != null) {
                            try {
                                int resId = Utils.hasGeminiSupport() ?
                                        (Integer) XposedHelpers.callMethod(mMobileTypeIds[slot], "getIconId") :
                                            XposedHelpers.getIntField(mView, "mMobileTypeId");
                                Drawable d = mResources.getDrawable(resId).mutate();
                                d = mIconManager.applyColorFilter(slot, d);
                                mMobileType[slot].setImageDrawable(d);
                            } catch (Resources.NotFoundException e) {
                                mMobileType[slot].setImageDrawable(null);
                            }
                        }
                        if (mRoaming[slot]) {
                            if (mMobileRoam != null) {
                                try {
                                    int resId = mRoamingId[slot];
                                    Drawable d = mResources.getDrawable(resId).mutate();
                                    d = mIconManager.applyColorFilter(slot, d);
                                    mMobileRoam[slot].setImageDrawable(d);
                                } catch (Resources.NotFoundException e) {
                                    mMobileRoam[slot].setImageDrawable(null);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logAndMute("updateMobileIcon", t);
        }
    }

    @Override 
    protected void updateAirplaneModeIcon() {
        try {
            ImageView airplaneModeIcon = Utils.hasGeminiSupport() ?
                    (ImageView) XposedHelpers.getObjectField(mView, "mFlightMode") :
                        (ImageView) XposedHelpers.getObjectField(mView, "mAirplane");
            if (airplaneModeIcon != null) {
                Drawable d = airplaneModeIcon.getDrawable();
                if (mIconManager.isColoringEnabled()) {
                    d = mIconManager.applyColorFilter(d);
                } else if (d != null) {
                    d.setColorFilter(null);
                }
                airplaneModeIcon.setImageDrawable(d);
            }
        } catch (Throwable t) {
            logAndMute("updateAirplaneModeIcon", t);
        }
    }

    @Override
    public void apply() {
        super.apply();

        updateRoamingIndicator();
    }

    protected void updateRoamingIndicator() {
        try {
            if (mRoamingIndicatorsDisabled) {
                Object mobileRoam = XposedHelpers.getObjectField(mView, "mMobileRoam");
                if (mMobileRoam == null) {
                    mMobileRoam = (mobileRoam instanceof ImageView) ?
                            new ImageView[] { (ImageView) mobileRoam, Utils.hasGeminiSupport() ?
                                    (ImageView) XposedHelpers.getObjectField(mView, "mMobileRoamGemini") : null } :
                                        (ImageView[]) mobileRoam;
                } else {
                    for (ImageView iv : mMobileRoam) {
                        if (iv != null) {
                            iv.setVisibility(View.GONE);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logAndMute("updateRoamingIndicator", t);
        }
    }
}
