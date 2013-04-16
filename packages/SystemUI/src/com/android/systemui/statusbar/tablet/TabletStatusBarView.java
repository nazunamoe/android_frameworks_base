/*
 * Copyright (C) 2010 The Android Open Source Project
 * This code has been modified. Portions copyright (C) 2012, ParanoidAndroid Project.
 *
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

package com.android.systemui.statusbar.tablet;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.View;
import android.view.MotionEvent;
import com.android.systemui.statusbar.phone.PanelBar;

import com.android.internal.util.aokp.BackgroundAlphaColorDrawable;
import com.android.systemui.R;

import java.util.List;

public class TabletStatusBarView extends PanelBar {

    ActivityManager mActivityManager;
    KeyguardManager mKeyguardManager;

    private Handler mHandler;

    private final int MAX_PANELS = 5;
    private final View[] mIgnoreChildren = new View[MAX_PANELS];
    private final View[] mPanels = new View[MAX_PANELS];
    private final int[] mPos = new int[2];

    float mAlpha;
    int mAlphaMode;
    int mBarColor;

    public TabletStatusBarView(Context context) {
        this(context, null);

    mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
    mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
    SettingsObserver settingsObserver = new SettingsObserver(new Handler());
    settingsObserver.observe();
    Drawable bg = mContext.getResources().getDrawable(R.drawable.system_bar_background);
        if(bg instanceof ColorDrawable) {
            BackgroundAlphaColorDrawable bacd = new BackgroundAlphaColorDrawable(
                    mBarColor != -2 ? mBarColor : ((ColorDrawable) bg).getColor());
            setBackground(bacd);
        }
        updateSettings();
    }

    public TabletStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setDelegateView(View view) {
    }

    public void setBar(TabletStatusBar phoneStatusBar) {
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (TabletStatusBar.DEBUG) {
                Slog.d(TabletStatusBar.TAG, "TabletStatusBarView intercepting touch event: " + ev);
            }
            // do not close the recents panel here- the intended behavior is that recents is dismissed
            // on touch up when clicking on status bar buttons
            // TODO: should we be closing the notification panel and input methods panel?
            mHandler.removeMessages(TabletStatusBar.MSG_CLOSE_NOTIFICATION_PANEL);
            mHandler.sendEmptyMessage(TabletStatusBar.MSG_CLOSE_NOTIFICATION_PANEL);
            mHandler.removeMessages(TabletStatusBar.MSG_CLOSE_INPUT_METHODS_PANEL);
            mHandler.sendEmptyMessage(TabletStatusBar.MSG_CLOSE_INPUT_METHODS_PANEL);
            mHandler.removeMessages(TabletStatusBar.MSG_STOP_TICKER);
            mHandler.sendEmptyMessage(TabletStatusBar.MSG_STOP_TICKER);

            for (int i=0; i < mPanels.length; i++) {
                if (mPanels[i] != null && mPanels[i].getVisibility() == View.VISIBLE) {
                    if (eventInside(mIgnoreChildren[i], ev)) {
                        if (TabletStatusBar.DEBUG) {
                            Slog.d(TabletStatusBar.TAG,
                                    "TabletStatusBarView eating event for view: "
                                    + mIgnoreChildren[i]);
                        }
                        return true;
                    }
                }
            }
        }
        if (TabletStatusBar.DEBUG) {
            Slog.d(TabletStatusBar.TAG, "TabletStatusBarView not intercepting event");
        }
        return super.onInterceptTouchEvent(ev);
    }

    private boolean eventInside(View v, MotionEvent ev) {
        // assume that x and y are window coords because we are.
        final int x = (int)ev.getX();
        final int y = (int)ev.getY();

        final int[] p = mPos;
        v.getLocationInWindow(p);

        final int l = p[0];
        final int t = p[1];
        final int r = p[0] + v.getWidth();
        final int b = p[1] + v.getHeight();

        return x >= l && x < r && y >= t && y < b;
    }

    public void setHandler(Handler h) {
        mHandler = h;
    }

    /**
     * Let the status bar know that if you tap on ignore while panel is showing, don't do anything.
     *
     * Debounces taps on, say, a popup's trigger when the popup is already showing.
     */
    public void setIgnoreChildren(int index, View ignore, View panel) {
        mIgnoreChildren[index] = ignore;
        mPanels[index] = panel;
    }

    private boolean isKeyguardEnabled() {
        if(mKeyguardManager == null) return false;
        return mKeyguardManager.isKeyguardLocked();
    }

    /*
     * ]0 < alpha < 1[
     */
    protected void setBackgroundAlpha(float alpha) {
        Drawable bg = getBackground();
        if (bg == null)
            return;

        if(bg instanceof BackgroundAlphaColorDrawable) {
            ((BackgroundAlphaColorDrawable) bg).setBgColor(mBarColor);
        }
        int a = Math.round(alpha * 255);
        bg.setAlpha(a);
    }

    public void updateBackgroundAlpha() {
        if((isKeyguardEnabled() && mAlphaMode == 0)) {
            setBackgroundAlpha(1);
        } else if (isKeyguardEnabled() || mAlphaMode == 2) {
            setBackgroundAlpha(mAlpha);
        }
    }

    private boolean isCurrentHomeActivity(ComponentName component, ActivityInfo homeInfo) {
        if (homeInfo == null) {
            final PackageManager pm = mContext.getPackageManager();
            homeInfo = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .resolveActivityInfo(pm, 0);
        }
        return homeInfo != null
            && homeInfo.packageName.equals(component.getPackageName())
            && homeInfo.name.equals(component.getClassName());
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();

            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NAVIGATION_BAR_ALPHA), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUS_NAV_BAR_ALPHA_MODE), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NAVIGATION_BAR_COLOR), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    protected void updateSettings() {
        mAlpha = 1.0f - Settings.System.getFloat(mContext.getContentResolver(),
                       Settings.System.NAVIGATION_BAR_ALPHA,
                       0.0f);
        mAlphaMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_NAV_BAR_ALPHA_MODE, 1);
        mBarColor = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NAVIGATION_BAR_COLOR, -2);

        updateBackgroundAlpha();
    }
}
