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

package com.ceco.kitkat.gravitybox.shortcuts;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import com.ceco.kitkat.gravitybox.adapters.IIconListAdapterItem;

public abstract class AShortcut implements IIconListAdapterItem {

    protected Context mContext;
    protected Resources mResources;

    public interface CreateShortcutListener {
        void onShortcutCreated(Intent intent);
    }

    public AShortcut(Context context) {
        mContext = context;
        mResources = mContext.getResources();
    }

    @Override
    public abstract String getText();

    @Override
    public String getSubText() {
        return null;
    }

    @Override
    public abstract Drawable getIconLeft();

    @Override
    public Drawable getIconRight() {
        return null;
    }

    public abstract void createShortcut(CreateShortcutListener listener);
}
