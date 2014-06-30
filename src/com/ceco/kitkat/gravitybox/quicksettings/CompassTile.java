/*
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.ceco.kitkat.gravitybox.quicksettings;

import com.ceco.kitkat.gravitybox.R;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

public class CompassTile extends BasicTile implements SensorEventListener {

    private final static float ALPHA = 0.97f;
    private final static int MSG_UPDATE_COMPASS = 1;
    private final static int COMPASS_TILE_UPDATE_INTERVAL = 100;

    private boolean mActive = false;
    private float mDegree = 0f;
    private float mCurrentAnimationDegree = 0f;
    private WindowManager mWindowManager;
    private SensorManager mSensorManager;
    private Sensor mAccelerationSensor;
    private Sensor mGeomagneticFieldSensor;
    private float[] mAcceleration;
    private float[] mGeomagnetic;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_COMPASS) {
                updateCompassTile();
            }
        }
    };

    public CompassTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActive = !mActive;
                updateResources();
            }
        };

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mAccelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGeomagneticFieldSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mSupportsHideOnChange = false;
    }

    @Override
    protected int onGetLayoutId() {
        return R.layout.quick_settings_tile_compass;
    }

    @Override
    protected synchronized void updateTile() {
        if (mActive) {
            mDrawableId = R.drawable.ic_qs_compass_on;
            mLabel = mGbContext.getString(R.string.quick_settings_compass_init);

            // Register listeners
            mSensorManager.registerListener(
                    this, mAccelerationSensor, SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(
                    this, mGeomagneticFieldSensor, SensorManager.SENSOR_DELAY_GAME);
        } else {
            mDrawableId = R.drawable.ic_qs_compass_off;
            mLabel = mGbContext.getString(R.string.quick_settings_compass_off);

            // Reset rotation of the ImageView
            mCurrentAnimationDegree = 0;
            mImageView.setAnimation(null);
            mImageView.setRotation(0);

            // Remove listeners
            mSensorManager.unregisterListener(this);
            mHandler.removeMessages(MSG_UPDATE_COMPASS);
        }

        super.updateTile();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (mAcceleration == null) {
                mAcceleration = event.values.clone();
            }
            values = mAcceleration;
        } else {
            // Magnetic field sensor
            if (mGeomagnetic == null) {
                mGeomagnetic = event.values.clone();
            }
            values = mGeomagnetic;
        }

        for (int i = 0; i < 3; i++) {
            values[i] = ALPHA * values[i] + (1 - ALPHA) * event.values[i];
        }

        if (!mActive || mAcceleration == null || mGeomagnetic == null) {
            // Nothing to do at this moment
            return;
        }

        float R[] = new float[9];
        float I[] = new float[9];
        if (!SensorManager.getRotationMatrix(R, I, mAcceleration, mGeomagnetic)) {
            // Rotation matrix couldn't be calculated
            return;
        }

        // Get the current orientation
        float[] orientation = new float[3];
        SensorManager.getOrientation(R, orientation);

        // Convert azimuth to degrees
        float newDegree = (float) Math.toDegrees(orientation[0]);
        newDegree = (newDegree + 360) % 360;
        if (mDegree != newDegree && !mHandler.hasMessages(MSG_UPDATE_COMPASS)) {
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_COMPASS, COMPASS_TILE_UPDATE_INTERVAL);
        }

        mDegree = newDegree;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void updateCompassTile() {
        // Set rotation in degrees as tile title
        mTextView.setText(formatValueWithCardinalDirection(mDegree));

        // Make arrow always point to north
        float animationDegrees = getBaseDegree() - mDegree;

        // Use the shortest animation path between last and new angle
        if (animationDegrees - mCurrentAnimationDegree > 180) {
            animationDegrees -= 360f;
        } else if (animationDegrees - mCurrentAnimationDegree < -180) {
            animationDegrees += 360f;
        }

        // Create a rotation animation
        RotateAnimation rotateAnimation = new RotateAnimation(mCurrentAnimationDegree,
                animationDegrees, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

        // Set animation properties
        float duration = (Math.abs(animationDegrees - mCurrentAnimationDegree) % 360) * 2;
        rotateAnimation.setDuration((int) duration);
        rotateAnimation.setFillAfter(true);

        // Start the animation
        mImageView.startAnimation(rotateAnimation);
        mCurrentAnimationDegree = animationDegrees;
    }

    private float getBaseDegree() {
        switch (mWindowManager.getDefaultDisplay().getRotation()) {
            default:
            case Surface.ROTATION_0: return 360f;
            case Surface.ROTATION_90: return 270f;
            case Surface.ROTATION_180: return 180f;
            case Surface.ROTATION_270: return 90f;
        }
    }

    private String formatValueWithCardinalDirection(float degree) {
        int cardinalDirectionIndex = (int) (Math.floor(((degree - 22.5) % 360) / 45) + 1) % 8;
        String[] cardinalDirections = mGbResources.getStringArray(
                R.array.cardinal_directions);

        return mGbContext.getString(R.string.quick_settings_compass_value, degree,
                cardinalDirections[cardinalDirectionIndex]);
    }
}
